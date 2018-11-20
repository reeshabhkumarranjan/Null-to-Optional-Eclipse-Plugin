package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import static org.eclipse.jdt.ui.JavaElementLabels.ALL_FULLY_QUALIFIED;
import static org.eclipse.jdt.ui.JavaElementLabels.getElementLabel;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import edu.cuny.citytech.refactoring.common.core.RefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactorableHarvester;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.TimeCollector;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 * The activator class controls the plug-in life cycle
 *
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class ConvertNullToOptionalRefactoringProcessor extends RefactoringProcessor {

	@SuppressWarnings("unused")
	private static final GroupCategorySet SET_CONVERT_NULL_TO_OPTIONAL = new GroupCategorySet(
			new GroupCategory("edu.cuny.hunter.optionalrefactoring", //$NON-NLS-1$
					Messages.CategoryName, Messages.CategoryDescription));

	/**
	 * For excluding AST parse time.
	 */
	private final TimeCollector excludedTimeCollector = new TimeCollector();

	private final IJavaElement[] javaElements; // the input java model elements

	private final IJavaSearchScope refactoringScope;

	private final RefactoringSettings settings;

	private final Set<Entities> entities = new LinkedHashSet<>();

	private final Set<IJavaElement> seeds = new LinkedHashSet<>();

	private int countNotRefactorable;

	public ConvertNullToOptionalRefactoringProcessor() throws JavaModelException {
		this(null, null, false, null, Optional.empty());
	}

	public ConvertNullToOptionalRefactoringProcessor(final CodeGenerationSettings settings,
			final Optional<IProgressMonitor> monitor) throws JavaModelException {
		this(null, settings, false, null, monitor);
	}

	public ConvertNullToOptionalRefactoringProcessor(final IJavaElement[] javaElements,
			final CodeGenerationSettings settings, final boolean layer, final RefactoringSettings refactoringSettings,
			final Optional<IProgressMonitor> monitor) throws JavaModelException {
		super(settings);
		try {
			this.javaElements = javaElements;
			this.refactoringScope = SearchEngine.createJavaSearchScope(javaElements);
			this.settings = refactoringSettings;
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	public ConvertNullToOptionalRefactoringProcessor(final IJavaElement[] javaElements,
			final CodeGenerationSettings settings, final RefactoringSettings refactoringSettings,
			final Optional<IProgressMonitor> monitor) throws JavaModelException {
		this(javaElements, settings, false, refactoringSettings, monitor);
	}

	public ConvertNullToOptionalRefactoringProcessor(final Optional<IProgressMonitor> monitor)
			throws JavaModelException {
		this(null, null, false, null, monitor);
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		try {
			final SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.CheckingPreconditions,
					this.getJavaElements().length * 1000);
			final RefactoringStatus status = new RefactoringStatus();

			for (final IJavaElement elem : this.getJavaElements())
				// here we merge the resulting RefactoringStatus from the process method with
				// status
				switch (elem.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
					status.merge(this.process((IJavaProject) elem, subMonitor));
					break;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					status.merge(this.process((IPackageFragmentRoot) elem, subMonitor));
					break;
				case IJavaElement.PACKAGE_FRAGMENT:
					status.merge(this.process((IPackageFragment) elem, subMonitor));
					break;
				case IJavaElement.COMPILATION_UNIT:
					status.merge(this.process((ICompilationUnit) elem, subMonitor));
					break;
				case IJavaElement.TYPE:
					status.merge(this.process((IType) elem, subMonitor));
					break;
				case IJavaElement.FIELD:
					status.merge(this.process((IField) elem, subMonitor));
					break;
				case IJavaElement.METHOD:
					status.merge(this.process((IMethod) elem, subMonitor));
					break;
				case IJavaElement.INITIALIZER:
					status.merge(this.process((IInitializer) elem, subMonitor));
					break;
				}

			if (!status.hasError()) {
			}
			return status;
		} catch (

		final Exception e) {
			JavaPlugin.log(e);
			throw e;
		} finally {
			monitor.done();
		}
	}

	@Override
	public RefactoringStatus checkInitialConditions(final IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			this.clearCaches();
			this.getExcludedTimeCollector().clear();

			final RefactoringStatus status = new RefactoringStatus();
			pm.beginTask(Messages.CheckingPreconditions, 1);
			return status;
			// }
		} catch (final Exception e) {
			JavaPlugin.log(e);
			throw e;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkProjectCompliance(final IJavaProject destinationProject) throws JavaModelException {
		final RefactoringStatus status = new RefactoringStatus();

		return status;
	}

	private RefactoringStatus checkStructure(final IMember member) throws JavaModelException {
		if (!member.isStructureKnown())
			return RefactoringStatus.createErrorStatus(
					MessageFormat.format(Messages.CUContainsCompileErrors, getElementLabel(member, ALL_FULLY_QUALIFIED),
							getElementLabel(member.getCompilationUnit(), ALL_FULLY_QUALIFIED)),
					JavaStatusContext.create(member.getCompilationUnit()));
		return new RefactoringStatus();
	}

	private RefactoringStatus checkWritability(final IMember member, final PreconditionFailure failure) {
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {

			final TextEditBasedChangeManager manager = new TextEditBasedChangeManager();

			if (this.entities.stream().filter(entity -> !entity.status().hasError()).collect(Collectors.toSet())
					.isEmpty())
				return new NullChange(Messages.NoNullsHaveBeenFound);

			final int count = this.entities.stream().map(Entities::size).reduce(Integer::sum).orElse(0);

			pm.beginTask(Messages.CreatingChange, count);
			
			final Map<ICompilationUnit, CompilationUnit> icuCuMap = new LinkedHashMap<>();
			final Function<ICompilationUnit,CompilationUnit> cuGet = icu -> {
				CompilationUnit _cu = icuCuMap.get(icu); 
				if (_cu == null) {
					_cu = Util.getCompilationUnit(icu, pm);
					_cu.recordModifications();
					icuCuMap.put(icu, _cu);
				};
				return _cu;
			};

			for (final Entities entity : this.entities) {
				for (final Map.Entry<IJavaElement, Set<Instance<? extends ASTNode>>> pair : entity) {
					IJavaElement element = pair.getKey();
					final ICompilationUnit icu = (ICompilationUnit) element.getAncestor(IJavaElement.COMPILATION_UNIT);
					CompilationUnit cu = cuGet.apply(icu);
					entity.addIcu(cu, element);
					pm.worked(1);
				}
				entity.transform();
			}
			
			for (Map.Entry<ICompilationUnit, CompilationUnit> entry : icuCuMap.entrySet()) {
				IProgressMonitor m = new NullProgressMonitor();
				ICompilationUnit icu = entry.getKey();
				CompilationUnit cu = entry.getValue();
				Document doc = new Document(icu.getSource());
				TextEdit edits = cu.rewrite(doc, icu.getJavaProject().getOptions(true));
				try {
					edits.apply(doc);
				} catch (MalformedTreeException | BadLocationException e) {
					throw new CoreException(new Status(Status.ERROR, ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID, 
							RefactoringStatus.FATAL, Messages.Transformer_FailedToWriteDocument, e));
				}
				String name = icu.getElementName();
				icu.rename("old_"+name, true, m);
				ICompilationUnit rwIcu = ((IPackageFragment)icu.getParent())
						.createCompilationUnit(name, doc.get(), true, m);
				CompilationUnitRewrite cur = new CompilationUnitRewrite(rwIcu);
				final ImportRewrite ir = cur.getImportRewrite();
				ir.addImport("java.util.Optional");
				this.compilationUnitToCompilationUnitRewriteMap.put(rwIcu, cur);
			}
			
			// save the source changes.
			final ICompilationUnit[] units = this.getCompilationUnitToCompilationUnitRewriteMap().keySet().stream()
					.filter(cu -> !manager.containsChangesIn(cu)).toArray(ICompilationUnit[]::new);

			for (final ICompilationUnit cu : units) {
				final CompilationUnit compilationUnit = this.getCompilationUnit(cu, pm);
				this.manageCompilationUnit(manager, this.getCompilationUnitRewrite(cu, compilationUnit),
						Optional.of(new SubProgressMonitor(pm, IProgressMonitor.UNKNOWN)));
			}

			final Map<String, String> arguments = new HashMap<>();
			final int flags = RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;

			final ConvertNullToOptionalRefactoringDescriptor descriptor = new ConvertNullToOptionalRefactoringDescriptor(
					null, "TODO", null, arguments, flags);

			return new DynamicValidationRefactoringChange(descriptor, this.getProcessorName(), manager.getAllChanges());
		} finally {
			pm.done();
			this.clearCaches();
		}
	}

	/**
	 * Creates a working copy layer if necessary.
	 *
	 * @param monitor the progress monitor to use
	 * @return a status describing the outcome of the operation
	 */
	private RefactoringStatus createWorkingCopyLayer(final IProgressMonitor monitor) {
		try {
			monitor.beginTask(Messages.CheckingPreconditions, 1);
			return new RefactoringStatus();
		} finally {
			monitor.done();
		}
	}

	@Override
	protected Map<ICompilationUnit, CompilationUnitRewrite> getCompilationUnitToCompilationUnitRewriteMap() {
		return this.compilationUnitToCompilationUnitRewriteMap;
	}

	/**
	 * {@inheritDoc} Don't use this!
	 */
	@Override
	public Object[] getElements() {
		return null;
	}

	public Set<Entities> getEntities() {
		return this.entities;
	}
	
	public Set<IJavaElement> getSeeds() {
		return this.seeds;
	}
	
	public int countNotRefactorable() {
		return this.countNotRefactorable;
	}

	public TimeCollector getExcludedTimeCollector() {
		return this.excludedTimeCollector;
	}

	@Override
	public String getIdentifier() {
		return ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID;
	}

	protected IJavaElement[] getJavaElements() {
		return this.javaElements;
	}

	@Override
	public String getProcessorName() {
		return Messages.Name;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		// return
		// RefactoringAvailabilityTester.isInterfaceMigrationAvailable(getSourceMethods().parallelStream()
		// .filter(m ->
		// !this.getUnmigratableMethods().contains(m)).toArray(IMethod[]::new),
		// Optional.empty());
		return true;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(final RefactoringStatus status,
			final SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	/**
	 * @param icu        an ICompilationUnit
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(final ICompilationUnit icu, final SubMonitor subMonitor) throws CoreException {
		final CompilationUnit compilationUnit = this.getCompilationUnit(icu, subMonitor.split(1));
		final RefactorableHarvester harvester = new RefactorableHarvester(icu, compilationUnit, this.refactoringScope,
				this.settings, subMonitor);
		final RefactoringStatus status = harvester.process();
		this.seeds .addAll(harvester.getSeeds());
		this.countNotRefactorable = harvester.countNotRefactorable();
		this.entities.addAll(harvester.getEntities());

		return status;
	}

	/**
	 * @param field
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(final IField field, final SubMonitor subMonitor) throws CoreException {
		final CompilationUnit compilationUnit = this.getCompilationUnit(field.getTypeRoot(), subMonitor.split(1));
		final RefactorableHarvester harvester = new RefactorableHarvester(field, compilationUnit, this.refactoringScope,
				this.settings, subMonitor);
		final RefactoringStatus status = harvester.process();
		this.seeds .addAll(harvester.getSeeds());
		this.countNotRefactorable = harvester.countNotRefactorable();
		this.entities.addAll(harvester.getEntities());
		return status;
	}

	/**
	 * @param initializer
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(final IInitializer initializer, final SubMonitor subMonitor)
			throws CoreException {
		final CompilationUnit compilationUnit = this.getCompilationUnit(initializer.getTypeRoot(), subMonitor.split(1));
		final RefactorableHarvester harvester = new RefactorableHarvester(initializer, compilationUnit,
				this.refactoringScope, this.settings, subMonitor);
		final RefactoringStatus status = harvester.process();
		this.seeds .addAll(harvester.getSeeds());
		this.countNotRefactorable = harvester.countNotRefactorable();
		this.entities.addAll(harvester.getEntities());
		return status;
	}

	/**
	 * @param project    An IJavaProject.
	 * @param subMonitor
	 * @return A failing RefactoringStatus, unless any of the potentiallyGoodStatus
	 *         instances are OK
	 * @throws CoreException
	 */
	private RefactoringStatus process(final IJavaProject project, final SubMonitor subMonitor) throws CoreException {
		final IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
		final RefactoringStatus status = new RefactoringStatus();
		for (final IPackageFragmentRoot root : roots)
			status.merge(this.process(root, subMonitor));
		return status;
	}

	/**
	 * @param method
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(final IMethod method, final SubMonitor subMonitor) throws CoreException {
		final CompilationUnit compilationUnit = this.getCompilationUnit(method.getTypeRoot(), subMonitor.split(1));
		final RefactorableHarvester harvester = new RefactorableHarvester(method, compilationUnit, this.refactoringScope,
				this.settings, subMonitor);
		final RefactoringStatus status = harvester.process();
		this.seeds .addAll(harvester.getSeeds());
		this.countNotRefactorable = harvester.countNotRefactorable();
		this.entities.addAll(harvester.getEntities());
		return status;
	}

	/**
	 * @param fragment   A package.
	 * @param subMonitor
	 * @return A RefactoringStatus
	 * @throws CoreException
	 */
	private RefactoringStatus process(final IPackageFragment fragment, final SubMonitor subMonitor)
			throws CoreException {
		final ICompilationUnit[] units = fragment.getCompilationUnits();
		final RefactoringStatus status = new RefactoringStatus();
		for (final ICompilationUnit unit : units)
			status.merge(this.process(unit, subMonitor));
		return status;
	}

	/**
	 * @param root       A folder or jar.
	 * @param subMonitor
	 * @return A failing RefactoringStatus, unless any of the potentiallyGoodStatus
	 *         instances are OK
	 * @throws CoreException
	 */
	private RefactoringStatus process(final IPackageFragmentRoot root, final SubMonitor subMonitor)
			throws CoreException {
		final RefactoringStatus status = new RefactoringStatus();
		final IJavaElement[] children = root.getChildren();
		for (final IJavaElement child : children)
			status.merge(this.process((IPackageFragment) child, subMonitor));
		return status;
	}

	/**
	 * @param type       an IType
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(final IType type, final SubMonitor subMonitor) throws CoreException {
		final CompilationUnit compilationUnit = this.getCompilationUnit(type.getTypeRoot(), subMonitor.split(1));
		final RefactorableHarvester harvester = new RefactorableHarvester(type, compilationUnit, this.refactoringScope,
				this.settings, subMonitor);
		final RefactoringStatus status = harvester.process();
		this.seeds .addAll(harvester.getSeeds());
		this.countNotRefactorable = harvester.countNotRefactorable();
		this.entities.addAll(harvester.getEntities());
		return status;
	}

	public RefactoringSettings settings() {
		return this.settings;
	}
}
