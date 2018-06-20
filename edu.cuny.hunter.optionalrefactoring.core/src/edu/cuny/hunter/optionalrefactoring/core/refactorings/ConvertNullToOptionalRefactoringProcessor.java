package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import static org.eclipse.jdt.ui.JavaElementLabels.ALL_FULLY_QUALIFIED;
import static org.eclipse.jdt.ui.JavaElementLabels.getElementLabel;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditBasedChangeManager;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.TimeCollector;

/**
 * The activator class controls the plug-in life cycle
 * 
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class ConvertNullToOptionalRefactoringProcessor extends RefactoringProcessor {

	@SuppressWarnings("unused")
	private static final GroupCategorySet SET_CONVERT_NULL_TO_OPTIONAL = new GroupCategorySet(
			new GroupCategory("edu.cuny.hunter.optionalrefactoring", //$NON-NLS-1$
					Messages.CategoryName, Messages.CategoryDescription));

	private Map<ICompilationUnit, CompilationUnitRewrite> compilationUnitToCompilationUnitRewriteMap = new HashMap<>();

	/**
	 * For excluding AST parse time.
	 */
	private TimeCollector excludedTimeCollector = new TimeCollector();

	/** Does the refactoring use a working copy layer? */
	private final boolean layer;

	/** The code generation settings, or <code>null</code> */
	private CodeGenerationSettings settings;

	private Map<ITypeRoot, CompilationUnit> typeRootToCompilationUnitMap = new HashMap<>();

	private Map<IType, ITypeHierarchy> typeToTypeHierarchyMap = new HashMap<>();

	private IJavaElement[] javaElements;

	public ConvertNullToOptionalRefactoringProcessor() throws JavaModelException {
		this(null, null, false, Optional.empty());
	}
	
	public ConvertNullToOptionalRefactoringProcessor(final CodeGenerationSettings settings,
			Optional<IProgressMonitor> monitor) throws JavaModelException {
		this(null, settings, false, monitor);
	}

	public ConvertNullToOptionalRefactoringProcessor(IJavaElement[] javaElements, final CodeGenerationSettings settings,
			boolean layer, Optional<IProgressMonitor> monitor) throws JavaModelException {
		try {
			this.javaElements = javaElements;
			this.settings = settings;
			this.layer = layer;

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

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		try {
			SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.CheckingPreconditions,
					this.getJavaElements().length * 1000);
			final RefactoringStatus status = new RefactoringStatus();

			for (IJavaElement elem : this.getJavaElements()) {
				switch (elem.getElementType()) {
				case IJavaElement.JAVA_PROJECT:
					process((IJavaProject) elem, subMonitor);
					break;
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
					process((IPackageFragmentRoot) elem, subMonitor);
					break;
				case IJavaElement.PACKAGE_FRAGMENT:
					process((IPackageFragment) elem, subMonitor);
					break;
				case IJavaElement.COMPILATION_UNIT:
					process((ICompilationUnit) elem, subMonitor);
					break;
				case IJavaElement.TYPE:
					process((IType) elem, subMonitor);
					break;
				case IJavaElement.FIELD:
					process((IField) elem, subMonitor);
					break;
				case IJavaElement.METHOD:
					process((IMethod) elem, subMonitor);
					break;
				case IJavaElement.INITIALIZER:
					process((IInitializer) elem, subMonitor);
					break;
				}
			}

			// if there are no fatal errors.
			if (!status.hasFatalError()) {
				// these are the nulls passing preconditions.
				// Set<NullLiteral> passingNullSet = null; //TODO: this.getRefactorableNulls();

				// add a fatal error if there are no passing nulls.
				// if (passingNullSet.isEmpty())
				// status.addFatalError(Messages.NoNullsHavePassedThePreconditions);
				// else {
				// TODO:
				// Checks.addModifiedFilesToChecker(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()),
				// context);
				// }
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

	private void process(IJavaProject project, SubMonitor subMonitor) throws JavaModelException {
		IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
		for (IPackageFragmentRoot root : roots) {
			process(root, subMonitor);
		}
	}

	private void process(IPackageFragmentRoot root, SubMonitor subMonitor)
			throws JavaModelException {
		IJavaElement[] children = root.getChildren();
		for (IJavaElement child : children) {
			if (child.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
				process((IPackageFragment) child, subMonitor);
		}
	}

	private void process(IPackageFragment fragment, SubMonitor subMonitor) throws JavaModelException {
		ICompilationUnit[] units = fragment.getCompilationUnits();
		for (ICompilationUnit unit : units)
			process(unit, subMonitor);
	}

	private void process(ICompilationUnit icu, SubMonitor subMonitor) throws JavaModelException {
		CompilationUnit compilationUnit = getCompilationUnit(icu, subMonitor.split(1));
		NullExprHarvester harvester = NullExprHarvester.of(icu, compilationUnit);
		Set<IJavaElement> workList = harvester.getCandidates();
		candidatePrinter(workList);
	}
	
	private void process(IType elem, SubMonitor subMonitor) throws JavaModelException {
		CompilationUnit compilationUnit = getCompilationUnit(elem.getTypeRoot(), subMonitor.split(1));
		NullExprHarvester harvester = NullExprHarvester.of(elem, compilationUnit);
		Set<IJavaElement> workList = harvester.getCandidates();
		candidatePrinter(workList);
	}

	private void process(IInitializer elem, SubMonitor subMonitor) throws JavaModelException {
		CompilationUnit compilationUnit = getCompilationUnit(elem.getTypeRoot(), subMonitor.split(1));
		NullExprHarvester harvester = NullExprHarvester.of(elem, compilationUnit);
		Set<IJavaElement> workList = harvester.getCandidates();
		candidatePrinter(workList);
	}

	private void process(IMethod elem, SubMonitor subMonitor) throws JavaModelException {
		CompilationUnit compilationUnit = getCompilationUnit(elem.getTypeRoot(), subMonitor.split(1));
		NullExprHarvester harvester = NullExprHarvester.of(elem, compilationUnit);
		Set<IJavaElement> workList = harvester.getCandidates();
		candidatePrinter(workList);
	}

	private void process(IField elem, SubMonitor subMonitor) throws JavaModelException {
		CompilationUnit compilationUnit = getCompilationUnit(elem.getTypeRoot(), subMonitor.split(1));
		NullExprHarvester harvester = NullExprHarvester.of(elem, compilationUnit);
		Set<IJavaElement> workList = harvester.getCandidates();
		candidatePrinter(workList);	
	}
	
	private void candidatePrinter(Set<? extends IJavaElement> candidates) {
		Logger logger = Logger.getLogger(this.toString());
		candidates.forEach(x ->
				logger.info(x.toString()));
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

	private RefactoringStatus checkWritabilitiy(IMember member, PreconditionFailure failure) {
		// if (member.isBinary() || member.isReadOnly()) {
		// return createError(failure, member);
		// }
		return new RefactoringStatus();
	}

	private void clearCaches() {
		getTypeToTypeHierarchyMap().clear();
		getCompilationUnitToCompilationUnitRewriteMap().clear();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		try {
			pm.beginTask(Messages.CreatingChange, 1);

			final TextEditBasedChangeManager manager = new TextEditBasedChangeManager();

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

	private CompilationUnit getCompilationUnit(ITypeRoot root, IProgressMonitor pm) {
		CompilationUnit compilationUnit = this.typeRootToCompilationUnitMap.get(root);
		if (compilationUnit == null) {
			this.getExcludedTimeCollector().start();
			compilationUnit = RefactoringASTParser.parseWithASTProvider(root, true, pm);
			this.getExcludedTimeCollector().stop();
			this.typeRootToCompilationUnitMap.put(root, compilationUnit);
		}
		return compilationUnit;
	}

	private CompilationUnitRewrite getCompilationUnitRewrite(ICompilationUnit unit, CompilationUnit root) {
		CompilationUnitRewrite rewrite = this.getCompilationUnitToCompilationUnitRewriteMap().get(unit);
		if (rewrite == null) {
			rewrite = new CompilationUnitRewrite(unit, root);
			this.getCompilationUnitToCompilationUnitRewriteMap().put(unit, rewrite);
		}
		return rewrite;
	}

	protected Map<ICompilationUnit, CompilationUnitRewrite> getCompilationUnitToCompilationUnitRewriteMap() {
		return this.compilationUnitToCompilationUnitRewriteMap;
	}

	/**
	 * {@inheritDoc}
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

	private ITypeHierarchy getTypeHierarchy(IType type, Optional<IProgressMonitor> monitor) throws JavaModelException {
		try {
			ITypeHierarchy ret = this.getTypeToTypeHierarchyMap().get(type);

			if (ret == null) {
				ret = type.newTypeHierarchy(monitor.orElseGet(NullProgressMonitor::new));
				this.getTypeToTypeHierarchyMap().put(type, ret);
			}

			return ret;
		} finally {
			monitor.ifPresent(IProgressMonitor::done);
		}
	}

	private Map<IType, ITypeHierarchy> getTypeToTypeHierarchyMap() {
		return typeToTypeHierarchyMap;
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

	private void manageCompilationUnit(final TextEditBasedChangeManager manager, CompilationUnitRewrite rewrite,
			Optional<IProgressMonitor> monitor) throws CoreException {
		monitor.ifPresent(m -> m.beginTask("Creating change ...", IProgressMonitor.UNKNOWN));
		CompilationUnitChange change = rewrite.createChange(false, monitor.orElseGet(NullProgressMonitor::new));

		if (change != null)
			change.setTextType("java");

		manager.manage(rewrite.getCu(), change);
	}
}
