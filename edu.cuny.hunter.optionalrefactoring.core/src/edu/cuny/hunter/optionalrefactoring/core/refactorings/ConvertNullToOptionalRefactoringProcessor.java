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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
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
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import edu.cuny.citytech.refactoring.common.core.RefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.analysis.Entity;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.TimeCollector;

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
	private TimeCollector excludedTimeCollector = new TimeCollector();

	/** Does the refactoring use a working copy layer? */
	private final boolean layer;

	private final IJavaElement[] javaElements;	// the input java model elements

	private final IJavaSearchScope refactoringScope;
	
	private final RefactoringSettings settings;

	private final Set<Set<Entity>> passingEntities = new LinkedHashSet<>(); // the forest of refactorable type-dependent entities

	private final Set<Entity> failingEntities = new LinkedHashSet<>();

	private final Map<IJavaElement, Set<ISourceRange>> bridgeableSourceRanges = new LinkedHashMap<>();

	public ConvertNullToOptionalRefactoringProcessor() throws JavaModelException {
		this(null, null, false, Optional.empty());
	}

	public ConvertNullToOptionalRefactoringProcessor(final CodeGenerationSettings settings,
			Optional<IProgressMonitor> monitor) throws JavaModelException {
		this(null, settings, false, monitor);
	}

	public ConvertNullToOptionalRefactoringProcessor(IJavaElement[] javaElements, final CodeGenerationSettings settings,
			boolean layer, Optional<IProgressMonitor> monitor) throws JavaModelException {
		super(settings);
		try {
			this.javaElements = javaElements;
			this.layer = layer;
			this.refactoringScope = SearchEngine.createJavaSearchScope(javaElements);
			this.settings = RefactoringSettings.getDefault();

		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	public ConvertNullToOptionalRefactoringProcessor(IJavaElement[] javaElements, final CodeGenerationSettings settings,
			Optional<IProgressMonitor> monitor) throws JavaModelException {
		this(javaElements, settings, false, monitor);
	}

	public ConvertNullToOptionalRefactoringProcessor(Optional<IProgressMonitor> monitor) throws JavaModelException {
		this(null, null, false, monitor);
	}

	public Set<Set<Entity>> getPassingEntities() {
		return this.passingEntities;
	}

	public Set<Entity> getFailingEntities() {
		return this.failingEntities;
	}

	public Map<IJavaElement,Set<ISourceRange>> getBridgeableSourceRanges() {
		return this.bridgeableSourceRanges;
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.CheckingPreconditions,
					this.getJavaElements().length * 1000);
			final RefactoringStatus status = new RefactoringStatus();

			for (IJavaElement elem : this.getJavaElements()) {
				// here we merge the resulting RefactoringStatus from the process method with status
				switch (elem.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
					status.merge(process((IJavaProject) elem, subMonitor));
					break;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					status.merge(process((IPackageFragmentRoot) elem, subMonitor));
					break;
				case IJavaElement.PACKAGE_FRAGMENT:
					status.merge(process((IPackageFragment) elem, subMonitor));
					break;
				case IJavaElement.COMPILATION_UNIT:
					status.merge(process((ICompilationUnit) elem, subMonitor));
					break;
				case IJavaElement.TYPE:
					status.merge(process((IType) elem, subMonitor));
					break;
				case IJavaElement.FIELD:
					status.merge(process((IField) elem, subMonitor));
					break;
				case IJavaElement.METHOD:
					status.merge(process((IMethod) elem, subMonitor));
					break;
				case IJavaElement.INITIALIZER:
					status.merge(process((IInitializer) elem, subMonitor));
					break;
				}
			}

			// if there are no errors.
			if (!status.hasError()) {
				// offer to do the transformation
			}
			return status;
		} catch (

				Exception e) {
			JavaPlugin.log(e);
			throw e;
		} finally {
			monitor.done();
		}
	}
	/**
	 * @param project An IJavaProject.
	 * @param subMonitor
	 * @return A failing RefactoringStatus, unless any of the potentiallyGoodStatus instances are OK
	 * @throws CoreException
	 */
	private RefactoringStatus process(IJavaProject project, SubMonitor subMonitor) throws CoreException {
		IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
		RefactoringStatus initialStatus = RefactoringStatus.createErrorStatus(Messages.NoNullsHavePassedThePreconditions);
		for (IPackageFragmentRoot root : roots) {
			RefactoringStatus potentiallyGoodStatus = process(root, subMonitor);
			if (!potentiallyGoodStatus.hasError()) initialStatus = potentiallyGoodStatus;
		}
		return initialStatus;
	}
	/**
	 * @param root A folder or jar.
	 * @param subMonitor
	 * @return A failing RefactoringStatus, unless any of the potentiallyGoodStatus instances are OK
	 * @throws CoreException
	 */
	private RefactoringStatus process(IPackageFragmentRoot root, SubMonitor subMonitor)
			throws CoreException {
		RefactoringStatus initialStatus = RefactoringStatus.createErrorStatus(Messages.NoNullsHavePassedThePreconditions);
		IJavaElement[] children = root.getChildren();
		for (IJavaElement child : children) {
			if (child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
				RefactoringStatus potentiallyGoodStatus = process((IPackageFragment) child, subMonitor);
				if (!potentiallyGoodStatus.hasError()) initialStatus = potentiallyGoodStatus;
			}
		}
		return initialStatus;
	}
	/**
	 * @param fragment A package.
	 * @param subMonitor
	 * @return A failing RefactoringStatus, unless any of the potentiallyGoodStatus instances are OK
	 * @throws CoreException
	 */
	private RefactoringStatus process(IPackageFragment fragment, SubMonitor subMonitor) throws CoreException {
		ICompilationUnit[] units = fragment.getCompilationUnits();
		RefactoringStatus initialStatus = RefactoringStatus.createErrorStatus(Messages.NoNullsHavePassedThePreconditions);
		for (ICompilationUnit unit : units) {
			RefactoringStatus potentiallyGoodStatus = process(unit, subMonitor);
			if (!potentiallyGoodStatus.hasError()) initialStatus = potentiallyGoodStatus;
		}
		return initialStatus;
	}
	/**
	 * @param icu an ICompilationUnit
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(ICompilationUnit icu, SubMonitor subMonitor) throws CoreException {
		CompilationUnit compilationUnit = getCompilationUnit(icu, subMonitor.split(1));
		RefactorableHarvester harvester = RefactorableHarvester.of(icu, 
				compilationUnit, this.refactoringScope, this.settings, subMonitor);
		RefactoringStatus status = harvester.harvestRefactorableContexts();
		this.passingEntities.addAll(harvester.getPassing());
		this.failingEntities.addAll(harvester.getFailing());
		this.extractBridgeableSourceRanges(harvester);
		return status;
	}
	/**
	 * @param type an IType
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(IType type, SubMonitor subMonitor) throws CoreException {
		CompilationUnit compilationUnit = getCompilationUnit(type.getTypeRoot(), subMonitor.split(1));
		RefactorableHarvester harvester = RefactorableHarvester.of(type, 
				compilationUnit, this.refactoringScope, this.settings, subMonitor);
		RefactoringStatus status = harvester.harvestRefactorableContexts();
		this.passingEntities.addAll(harvester.getPassing());
		this.failingEntities.addAll(harvester.getFailing());
		this.extractBridgeableSourceRanges(harvester);
		return status;
	}
	/**
	 * @param initializer
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(IInitializer initializer, SubMonitor subMonitor) throws CoreException {
		CompilationUnit compilationUnit = getCompilationUnit(initializer.getTypeRoot(), subMonitor.split(1));
		RefactorableHarvester harvester = RefactorableHarvester.of(initializer, 
				compilationUnit, this.refactoringScope, this.settings, subMonitor);
		RefactoringStatus status = harvester.harvestRefactorableContexts();
		this.passingEntities.addAll(harvester.getPassing());
		this.failingEntities.addAll(harvester.getFailing());
		this.extractBridgeableSourceRanges(harvester);
		return status;
	}
	/**
	 * @param method
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(IMethod method, SubMonitor subMonitor) throws CoreException {
		CompilationUnit compilationUnit = getCompilationUnit(method.getTypeRoot(), subMonitor.split(1));
		RefactorableHarvester harvester = RefactorableHarvester.of(method, 
				compilationUnit, this.refactoringScope, this.settings, subMonitor);
		RefactoringStatus status = harvester.harvestRefactorableContexts();
		this.passingEntities.addAll(harvester.getPassing());
		this.failingEntities.addAll(harvester.getFailing());
		this.extractBridgeableSourceRanges(harvester);
		return status;
	}
	/**
	 * @param field
	 * @param subMonitor
	 * @return the RefactoringStatus from the harvester
	 * @throws CoreException
	 */
	private RefactoringStatus process(IField field, SubMonitor subMonitor) throws CoreException {
		CompilationUnit compilationUnit = getCompilationUnit(field.getTypeRoot(), subMonitor.split(1));
		RefactorableHarvester harvester = RefactorableHarvester.of(field, 
				compilationUnit, this.refactoringScope, this.settings, subMonitor);
		RefactoringStatus status = harvester.harvestRefactorableContexts();
		this.passingEntities.addAll(harvester.getPassing());
		this.failingEntities.addAll(harvester.getFailing());
		this.extractBridgeableSourceRanges(harvester);
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		try {
			this.clearCaches();
			this.getExcludedTimeCollector().clear();

			// TODO (later):
			// if (this.getSourceMethods().isEmpty())
			// return
			// RefactoringStatus.createFatalErrorStatus(Messages.NullssNotSpecified);
			// else {
			RefactoringStatus status = new RefactoringStatus();
			pm.beginTask(Messages.CheckingPreconditions, 1);
			return status;
			// }
		} catch (Exception e) {
			JavaPlugin.log(e);
			throw e;
		} finally {
			pm.done();
		}
	}

	private void extractBridgeableSourceRanges(RefactorableHarvester harvester) {
		harvester.getBridgeable().entrySet().stream()
		.forEach(entry -> {
			if (this.bridgeableSourceRanges
					.containsKey(entry.getKey()))
				this.bridgeableSourceRanges
				.get(entry.getKey()).addAll(entry.getValue());
			else this.bridgeableSourceRanges
			.put(entry.getKey(), entry.getValue());
		});
	}

	private RefactoringStatus checkProjectCompliance(IJavaProject destinationProject) throws JavaModelException {
		RefactoringStatus status = new RefactoringStatus();

		// if (!JavaModelUtil.is18OrHigher(destinationProject))
		// addErrorAndMark(status,
		// PreconditionFailure.DestinationProjectIncompatible, sourceMethod,
		// targetMethod);

		return status;
	}

	private RefactoringStatus checkStructure(IMember member) throws JavaModelException {
		if (!member.isStructureKnown()) {
			return RefactoringStatus.createErrorStatus(
					MessageFormat.format(Messages.CUContainsCompileErrors, getElementLabel(member, ALL_FULLY_QUALIFIED),
							getElementLabel(member.getCompilationUnit(), ALL_FULLY_QUALIFIED)),
					JavaStatusContext.create(member.getCompilationUnit()));
		}
		return new RefactoringStatus();
	}

	private RefactoringStatus checkWritability(IMember member, PreconditionFailure failure) {
		// if (member.isBinary() || member.isReadOnly()) {
		// return createError(failure, member);
		// }
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {

			final TextEditBasedChangeManager manager = new TextEditBasedChangeManager();

			if (this.passingEntities.isEmpty()) 
				return new NullChange(Messages.NoNullsHavePassedThePreconditions);

			Optional<Integer> count = this.passingEntities.stream()
					.flatMap(Set::stream).map(entity -> new Integer(1)).reduce(Integer::sum);

			pm.beginTask(Messages.CreatingChange, count.orElse(0));

			for (Set<Entity> set : this.passingEntities) {
				for (Entity entity : set) {
					CompilationUnitRewrite rewrite = this.getCompilationUnitRewrite(
							(ICompilationUnit)entity.element().getAncestor(IJavaElement.COMPILATION_UNIT), 
							this.getCompilationUnit((ICompilationUnit)entity.element()
									.getAncestor(IJavaElement.COMPILATION_UNIT), pm));
					entity.transform(rewrite);
					pm.worked(1);
				}
			}

			// save the source changes.
			ICompilationUnit[] units = this.getCompilationUnitToCompilationUnitRewriteMap().keySet().stream()
					.filter(cu -> !manager.containsChangesIn(cu)).toArray(ICompilationUnit[]::new);

			for (ICompilationUnit cu : units) {
				CompilationUnit compilationUnit = getCompilationUnit(cu, pm);
				manageCompilationUnit(manager, getCompilationUnitRewrite(cu, compilationUnit),
						Optional.of(new SubProgressMonitor(pm, IProgressMonitor.UNKNOWN)));
			}

			final Map<String, String> arguments = new HashMap<>();
			int flags = RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;

			// TODO: Fill in description.

			ConvertNullToOptionalRefactoringDescriptor descriptor = new ConvertNullToOptionalRefactoringDescriptor(null,
					"TODO", null, arguments, flags);

			return new DynamicValidationRefactoringChange(descriptor, getProcessorName(), manager.getAllChanges());
		} finally {
			pm.done();
			this.clearCaches();
		}
	}

	/**
	 * Creates a working copy layer if necessary.
	 *
	 * @param monitor
	 *            the progress monitor to use
	 * @return a status describing the outcome of the operation
	 */
	private RefactoringStatus createWorkingCopyLayer(IProgressMonitor monitor) {
		try {
			monitor.beginTask(Messages.CheckingPreconditions, 1);
			// ICompilationUnit unit =
			// getDeclaringType().getCompilationUnit();
			// if (fLayer)
			// unit = unit.findWorkingCopy(fOwner);
			// resetWorkingCopies(unit);
			return new RefactoringStatus();
		} finally {
			monitor.done();
		}
	}

	protected Map<ICompilationUnit, CompilationUnitRewrite> getCompilationUnitToCompilationUnitRewriteMap() {
		return this.compilationUnitToCompilationUnitRewriteMap;
	}

	/**
	 * {@inheritDoc}
	 * Don't use this!
	 */
	@Override
	public Object[] getElements() {
		return null;
	}

	public TimeCollector getExcludedTimeCollector() {
		return excludedTimeCollector;
	}

	@Override
	public String getIdentifier() {
		return ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID;
	}

	protected IJavaElement[] getJavaElements() {
		return javaElements;
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
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants)
			throws CoreException {
		return new RefactoringParticipant[0];
	}
}
