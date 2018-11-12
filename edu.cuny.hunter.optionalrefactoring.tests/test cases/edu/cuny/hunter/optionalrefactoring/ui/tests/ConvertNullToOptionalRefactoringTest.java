/**
 *
 */
package edu.cuny.hunter.optionalrefactoring.ui.tests;

import static edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure.CAST_EXPRESSION;
import static edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure.ENHANCED_FOR;
import static edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure.MAIN_METHOD;
import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.setOf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import edu.cuny.citytech.refactoring.common.tests.RefactoringTest;
import edu.cuny.hunter.optionalrefactoring.core.analysis.N2ORefactoringStatusContext;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.descriptors.ConvertNullToOptionalRefactoringDescriptor;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.Entities;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings.Choice;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 */
@SuppressWarnings("restriction")
public class ConvertNullToOptionalRefactoringTest extends RefactoringTest {

	private static class MockEntryData {
		Integer severity;
		String message;
		Optional<PreconditionFailure> pf;
		Integer code;

		/**
		 * @param s  RefactoringStatus severity
		 * @param f PreconditionFailure
		 */
		MockEntryData(final Integer s, final PreconditionFailure f) {
			/* we take the precondition failure code with the lowest integer value */
			this.severity = s;
			this.message = f.getMessage();
			this.pf = Optional.ofNullable(f);
			this.code = f.getCode();
		}
		
		MockEntryData(final Integer s, final String msg) {
			this.severity = s;
			this.message = msg;
			this.pf = Optional.empty();
			this.code = RefactoringStatusEntry.NO_CODE;
		}
	}

	private static final Class<ConvertNullToOptionalRefactoringTest> clazz = ConvertNullToOptionalRefactoringTest.class;

	private static final String REFACTORING_PATH = "ConvertNullToOptional/";

	/**
	 * The name of the directory containing resources under the project directory.
	 */
	private static final String RESOURCE_PATH = "resources";

	private static final Logger LOGGER = Logger.getLogger(clazz.getName());

	private static boolean compiles(final String source) throws IOException {
		return compiles(source, Files.createTempDirectory(null));
	}

	public static Test setUpTest(final Test test) {
		return new Java18Setup(test);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(clazz));
	}

	public ConvertNullToOptionalRefactoringTest(final String name) {
		super(name);
	}

	@Override
	protected void tearDown() throws Exception {

		if (this.getPackageP().exists()) {
			tryDeletingJavaFiles(this.getPackageP());
		}

		super.tearDown();
	}

	private static void tryDeletingJavaFiles(IPackageFragment pack) throws JavaModelException {
		File sourceFile = pack.getResource().getLocation().append("A.java").toFile();

		// delete the file.
		try {
			Files.delete(sourceFile.toPath());
		} catch (IOException e) {
			throw new IllegalArgumentException("Source file does not exist.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jdt.ui.tests.refactoring.RefactoringTest#createCUfromTestFile
	 * (org.eclipse.jdt.core.IPackageFragment, java.lang.String)
	 */
	@Override
	protected ICompilationUnit createCUfromTestFile(final IPackageFragment pack, final String cuName) throws Exception {
		final ICompilationUnit unit = super.createCUfromTestFile(pack, cuName);

		// full path of where the CU exists.
		final Path directory = Paths
				.get(unit.getParent().getParent().getParent().getResource().getLocation().toString());

		// compile it to make and store the class file.
		assertTrue("Input should compile", compiles(unit.getSource(), directory));

		return unit;
	}

	private RefactoringStatus createExpectedStatus(final MockEntryData[] list) {
		final RefactoringStatus rs = new RefactoringStatus();
		for (final MockEntryData tuple : list)
			rs.addEntry(new RefactoringStatusEntry(tuple.severity, tuple.message,
					tuple.pf.isPresent() ? new N2ORefactoringStatusContext(null, null, tuple.pf.get(), null) : null,
					ConvertNullToOptionalRefactoringDescriptor.REFACTORING_ID, tuple.code));
		return rs;
	}

	/**
	 * Checks if an expected RefactoringStatus mocked by test is equal to a
	 * RefactoringStatus from the Refactoring.
	 *
	 * @param actual
	 * @param expected
	 * @return
	 */
	private boolean equivalentRefactoringStatus(final RefactoringStatus actual, final RefactoringStatus expected) {
		if (actual.isOK() || actual.hasFatalError())
			return actual.getSeverity() == expected.getSeverity();
		else
			return actual.getSeverity() == expected.getSeverity() && Stream.of(actual.getEntries()).allMatch(left -> // check
																													// if
																													// all
																													// the
																													// first
																													// are
																													// present
																													// in
																													// second
			Stream.of(expected.getEntries()).anyMatch(right -> this.equivalentRefactoringStatusEntry(left, right)))
					&& Stream.of(expected.getEntries()).allMatch(left -> // check if all the second are present in first
					Stream.of(actual.getEntries())
							.anyMatch(right -> this.equivalentRefactoringStatusEntry(left, right)));
	}

	/**
	 * Checks if a RefactoringStatusEntry from a mocked RefactoringStatus matches
	 * one from the Refactoring. Checks severity, message, context, pluginId, and
	 * code for equality, but in the case of context it only checks that the set of
	 * PreconditionFailures is the same, and does not check the element or
	 * sourceRange.
	 *
	 * @param actual
	 * @param expected
	 * @return
	 */
	private boolean equivalentRefactoringStatusEntry(final RefactoringStatusEntry actual,
			final RefactoringStatusEntry expected) {
		return actual.getSeverity() == expected.getSeverity() && 
				actual.getMessage().equals(expected.getMessage()) && 
				actual.getSeverity() == RefactoringStatus.WARNING ? 
						actual.getContext() == expected.getContext() && actual.getContext() == null : // No context because no seeding occurred 
					((N2ORefactoringStatusContext) actual.getContext()).getPreconditionFailure()
						.equals(((N2ORefactoringStatusContext) expected.getContext()).getPreconditionFailure()) && 
				actual.getPluginId().equals(expected.getPluginId()) && 
				actual.getCode() == expected.getCode();
	}

	private Path getAbsolutionPath(final String fileName) {
		final Path path = Paths.get(RESOURCE_PATH, fileName);
		final Path absolutePath = path.toAbsolutePath();
		return absolutePath;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jdt.ui.tests.refactoring.RefactoringTest#getFileContents(java
	 * .lang.String) Had to override this method because, since this plug-in is a
	 * fragment (at least I think that this is the reason), it doesn't have an
	 * activator and the bundle is resolving to the eclipse refactoring test bundle.
	 */
	@Override
	public String getFileContents(final String fileName) throws IOException {
		final Path absolutePath = this.getAbsolutionPath(fileName);
		final byte[] encoded = Files.readAllBytes(absolutePath);
		return new String(encoded, Charset.defaultCharset());
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	@Override
	protected Refactoring getRefactoring(final IJavaElement... elements) throws JavaModelException {
		final EnumSet<RefactoringSettings.Choice> choices = EnumSet.allOf(RefactoringSettings.Choice.class);
		final ConvertNullToOptionalRefactoringProcessor processor = Util.createNullToOptionalRefactoringProcessor(
				elements, new RefactoringSettings(choices) /* here the test defaults are injected*/, Optional.empty());
		return new ProcessorBasedRefactoring(processor);
	}

	@Override
	public String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private ConvertNullToOptionalRefactoringProcessor getRefactoringProcessor(final ICompilationUnit icu)
			throws JavaModelException {
		// we know it's a ProcessorBasedRefactoring since we overriding
		// getRefactoring()
		// in this class.
		final ProcessorBasedRefactoring refactoring = (ProcessorBasedRefactoring) this.getRefactoring(icu);

		// we know it's a ConvertNullToOptionalRefactoringProcessor since we
		// overriding
		// getRefactoring() in this class.
		final ConvertNullToOptionalRefactoringProcessor refactoringProcessor = (ConvertNullToOptionalRefactoringProcessor) refactoring
				.getProcessor();
		return refactoringProcessor;
	}

	@Override
	public void setFileContents(final String fileName, final String contents) throws IOException {
		final Path absolutePath = this.getAbsolutionPath(fileName);
		Files.write(absolutePath, contents.getBytes());
	}

	public void testAnonymousClassDeclaration() throws Exception {
		this.propagationHelper(setOf(setOf("o")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testAssignmentField() throws Exception {
		this.transformationHelper(EnumSet.of(Choice.CONSIDER_IMPLICITLY_NULL_FIELDS), new RefactoringStatus());
	}

	public void testAssignmentFieldArray() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testAssignmentFieldSimpleName() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class),
				this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentFieldStaticQualified() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class),
				this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentFieldStaticSimpleName() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class), 
				this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentFieldSuper() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class), 
				this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentFieldSuperQualified() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class),
				this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentFieldThis() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class), 
				this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentFieldThisQualified() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class), 
				this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentFieldTransitive() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b", "c"), setOf("controlNullDependent"), setOf("d", "e")), setOf(),
				EnumSet.noneOf(Choice.class), this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE),
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentLocalVariable() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class), 
				this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentLocalVariable2() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testAssignmentLocalVariable3() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testAssignmentLocalVariableArray() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testAssignmentLocalVariableTransitive() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("c", "d", "e", "f", "g"), setOf("control")), setOf(), EnumSet.noneOf(Choice.class),
				this.createExpectedStatus(
						new MockEntryData[] {
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE),
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE), 
								new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
						}));
	}

	public void testAssignmentParameters() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testBridgeEnhancedForStatementExpression() throws Exception {
		this.transformationHelper(null, this.createExpectedStatus(new MockEntryData[] {
				new MockEntryData(RefactoringStatus.INFO, ENHANCED_FOR) }));
	}

	public void testCastExpressionBridgeOffMethod() throws Exception {
		this.propagationHelper(setOf(), setOf(), EnumSet.of(Choice.REFACTOR_THROUGH_JAVA_OPERATORS, Choice.BRIDGE_ENTITIES_EXCLUDED_BY_SETTINGS),
				this.createExpectedStatus(new MockEntryData[] { 
						new MockEntryData(RefactoringStatus.ERROR, CAST_EXPRESSION)
						}));
	}

	public void testCastExpressionBridgeOffVarDecl() throws Exception {
		this.propagationHelper(setOf(), setOf(), EnumSet.of(Choice.REFACTOR_THROUGH_JAVA_OPERATORS, Choice.BRIDGE_ENTITIES_EXCLUDED_BY_SETTINGS),
				this.createExpectedStatus(new MockEntryData[] {
						new MockEntryData(RefactoringStatus.ERROR, CAST_EXPRESSION)
						}));
	}

	public void testCastExpressionBridgeOnMethod() throws Exception {
		this.propagationHelper(setOf(setOf("a", "x", "b", "m")), setOf(), EnumSet.noneOf(Choice.class),
				this.createExpectedStatus(new MockEntryData[] {
						new MockEntryData(RefactoringStatus.INFO, CAST_EXPRESSION) 
					}));
	}
	
	public void testSettingsImplicitOnPrimitive() throws Exception {
		this.propagationHelper(setOf(), setOf(), EnumSet.noneOf(Choice.class), this.createExpectedStatus(
				new MockEntryData[] {
						new MockEntryData(RefactoringStatus.WARNING, Messages.NoNullsHaveBeenFound)
				}));
	}

	public void testCastExpressionBridgeOnVarDecl() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b")), setOf(), EnumSet.noneOf(Choice.class), 
				this.createExpectedStatus(new MockEntryData[] {
						new MockEntryData(RefactoringStatus.INFO, CAST_EXPRESSION) 
				}));
	}

	public void testComparisonLocalVariable() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testComparisonLocalVariable2() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testComparisonLocalVariable3() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testDeclarationField() throws Exception {
		this.propagationHelper(setOf(setOf("e"), setOf("earray"), setOf("einitializedarray"), setOf("f"),
				setOf("farray"), setOf("finitializedarray")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testDeclarationFieldArray() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}
	
	public void testVarDeclMultiFragment() throws Exception {
		this.transformationHelper(EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testDeclarationFieldTransitive() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("c", "d", "e"), setOf("controlNullDependent")), setOf(),
				EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testDeclarationLocalVariable() throws Exception {
		this.propagationHelper(setOf(setOf("a")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testDeclarationLocalVariableArray() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testDeclarationLocalVariableTransitive() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b", "c"), setOf("d", "e"), setOf("control")), setOf(), EnumSet.noneOf(Choice.class),
				new RefactoringStatus());
	}

	public void testImplicitlyNullFieldConstructorInit() throws Exception {
		this.transformationHelper(null,
				RefactoringStatus.createWarningStatus(Messages.NoNullsHaveBeenFound));
	}

	public void testImplicitlyNullFieldNoConstructorInit() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testImplicitlyNullFieldSomeConstructorInit() throws Exception {
		this.transformationHelper(null, this.createExpectedStatus(new MockEntryData[] {
				new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
		}));
	}

	public void testImplicitlyNullVariableDecl() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testInvocationConstructor() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b", "d", "g", "k"), setOf("f", "i", "m"), setOf("o")), setOf(), EnumSet.noneOf(Choice.class),
				new RefactoringStatus());
	}

	public void testInvocationMethod() throws Exception {
		this.propagationHelper(setOf(setOf("o"), setOf("m", "i", "f"), setOf("k", "g", "d", "b", "a")), setOf(), EnumSet.noneOf(Choice.class),
				new RefactoringStatus());
	}

	public void testInvocationSuperConstructor() throws Exception {
		this.propagationHelper(setOf(setOf("g", "d", "b", "a"), setOf("i", "f")), setOf(), EnumSet.noneOf(Choice.class),
				new RefactoringStatus());
	}

	public void testMainMethodNoBridge() throws Exception {
		this.propagationHelper(setOf(), setOf(), EnumSet.of(Choice.BRIDGE_EXTERNAL), 
				this.createExpectedStatus(new MockEntryData[] {
						new MockEntryData(RefactoringStatus.ERROR, MAIN_METHOD) 
				}));
	}

	public void testMainMethodBridge() throws Exception {
		this.propagationHelper(setOf(setOf("args")), setOf(), EnumSet.noneOf(Choice.class), 
				this.createExpectedStatus(new MockEntryData[] {
						new MockEntryData(RefactoringStatus.INFO, MAIN_METHOD) 
				}));
	}

	public void testNewStatement() throws Exception {
		this.propagationHelper(setOf(setOf("k", "g", "d", "b", "a"), setOf("m", "i", "f"), setOf("o")), setOf(), EnumSet.noneOf(Choice.class),
				new RefactoringStatus());
	}

	public void testOfNullable() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testOfNullable2() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testOfNullable3() throws Exception {
		this.transformationHelper(null, this.createExpectedStatus(new MockEntryData[] {
				new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.NON_SOURCE_CODE)
		}));
	}

	public void testReturnStatement() throws Exception {
		this.propagationHelper(setOf(setOf("nullReturner", "extendedNullReturner", "composedNullReturner"),
				setOf("controlNullReturner")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testSettingsFieldsOff() throws Exception {
		this.propagationHelper(setOf(), setOf(), EnumSet.of(Choice.REFACTOR_FIELDS, Choice.BRIDGE_ENTITIES_EXCLUDED_BY_SETTINGS),
				this.createExpectedStatus(new MockEntryData[] {
						new MockEntryData(RefactoringStatus.ERROR, PreconditionFailure.EXCLUDED_ENTITY)
				}));
	}

	public void testSettingsFieldsOn() throws Exception {
		this.propagationHelper(setOf(setOf("x")), setOf(), EnumSet.of(Choice.REFACTOR_METHOD_RETURN_TYPES),
				new RefactoringStatus());
	}

	public void testSettingsImplicitOff() throws Exception {
		this.propagationHelper(setOf(), setOf(), EnumSet.of(Choice.CONSIDER_IMPLICITLY_NULL_FIELDS),
				RefactoringStatus.createWarningStatus(Messages.NoNullsHaveBeenFound));
	}

	public void testSettingsImplicitOn() throws Exception {
		this.propagationHelper(setOf(setOf("x")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testSettingsLocalVarsOff() throws Exception {
		this.propagationHelper(setOf(setOf("m")), setOf(), EnumSet.of(Choice.REFACTOR_LOCAL_VARS), 
				this.createExpectedStatus(new MockEntryData[] {
						new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.EXCLUDED_ENTITY)
				}));
	}

	public void testSettingsLocalVarsOn() throws Exception {
		this.propagationHelper(setOf(setOf("x")), setOf(), EnumSet.of(Choice.REFACTOR_METHOD_RETURN_TYPES),
				new RefactoringStatus());
	}

	public void testSettingsMethodReturnOff() throws Exception {
		this.propagationHelper(setOf(setOf("a")), setOf(), EnumSet.of(Choice.REFACTOR_METHOD_RETURN_TYPES),
				new RefactoringStatus());
	}

	public void testSettingsMethodReturnOn() throws Exception {
		this.propagationHelper(setOf(setOf("m")), setOf(), EnumSet.of(Choice.REFACTOR_FIELDS), 
				this.createExpectedStatus(new MockEntryData[] {
						new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.EXCLUDED_ENTITY)
				}));
	}

	public void testSettingsParametersOff() throws Exception {
		this.propagationHelper(setOf(setOf("o")), setOf(), EnumSet.of(Choice.REFACTOR_METHOD_PARAMS), new RefactoringStatus());
	}

	public void testSettingsParametersOn() throws Exception {
		this.propagationHelper(setOf(setOf("x")), setOf(), EnumSet.noneOf(Choice.class), new RefactoringStatus());
	}

	public void testTransformationEnhancedForStatement() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationFieldAccessAssignment() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationFieldDeclLocal() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationLocalVarAssignment() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationLocalVarDeclLocal() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationMethDeclLocal() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationTwoFields() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationWithJavaDoc() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationWithLineComment() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationWithMultiLineComment() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testTransformationWithSeedComparison() throws Exception {
		this.transformationHelper(null, this.createExpectedStatus(new MockEntryData[] {
				new MockEntryData(RefactoringStatus.INFO, PreconditionFailure.REFERENCE_EQUALITY_OP)
		}));
	}

	private void transformationHelper(final EnumSet<Choice> turnOff, final RefactoringStatus expectedStatus) throws Exception {
		final ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");

		final ProcessorBasedRefactoring refactoring = (ProcessorBasedRefactoring) this.getRefactoring(icu);
		final ConvertNullToOptionalRefactoringProcessor processor = (ConvertNullToOptionalRefactoringProcessor) refactoring
				.getProcessor();

		if (turnOff != null)
			turnOff.forEach(choice -> processor.settings().set(false, choice));

		final RefactoringStatus finalStatus = refactoring.checkAllConditions(new NullProgressMonitor());
		LOGGER.info("Final status: " + finalStatus);

		assertTrue("Precondition checking returned the expected RefactoringStatus: " + expectedStatus + ".",
				this.equivalentRefactoringStatus(finalStatus, expectedStatus));

		if (!finalStatus.hasFatalError())
			this.performChange(refactoring, false);
		else
			this.performChange(new NullChange());

		final String outputTestFileName = this.getOutputTestFileName("A");
		final String actual = icu.getSource();
		LOGGER.info(actual);
		assertTrue("Actual output should compile.", compiles(actual));

		final String expected = this.getFileContents(outputTestFileName);
		assertEqualLines(expected, actual);
		
	}

	private void propagationHelper(final Set<Set<String>> expectedPassingSets,
			final Set<Set<String>> expectedFailingSet, final EnumSet<Choice> turnOff, final RefactoringStatus expectedStatus)
			throws Exception {

		// compute the actual results.
		final ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");

		final ConvertNullToOptionalRefactoringProcessor refactoring = this.getRefactoringProcessor(icu);

		if (!turnOff.isEmpty())
			turnOff.forEach(choice -> refactoring.settings().set(false, choice));

		final RefactoringStatus status = refactoring.checkFinalConditions(new NullProgressMonitor(), null);

		// assert that the actual severity matches that of the expected.
		assertTrue("The actual RefactoringStatus ("+status+") is equal to the expected: ("+expectedStatus+").",
				this.equivalentRefactoringStatus(status, expectedStatus));
		
		final Set<Entities> sets = refactoring.getEntities();

		// Here we are getting all the sets of type dependent entities
		final Set<Entities> passingSets = sets.stream().filter(entity -> !entity.status().hasError())
				.collect(Collectors.toSet());
		final Set<Entities> failingSet = sets.stream().filter(entity -> entity.status().hasError())
				.collect(Collectors.toSet());
		
		LOGGER.info("Settings:"+refactoring.settings()+"\n"
		+"Passing Sets:"+passingSets.stream().map(Entities::toString)
				.collect(Collectors.joining(", ", "{", "}"))+"\n"
		+"RefactoringStatusEntries:"+Arrays.stream(status.getEntries()).map(RefactoringStatusEntry::toString)
				.collect(Collectors.joining(", ", "\n", "\n")));

		// convert to sets of strings
		final Set<Set<String>> actualPassingSets = passingSets.stream()
				.map(entity -> entity.stream().map(pair -> pair.getKey())
						.map(element -> element.getElementName()).collect(Collectors.toSet()))
				.collect(Collectors.toSet());

		final Set<Set<String>> actualFailingSet = failingSet.stream()
				.map(entity -> entity.stream().map(pair -> pair.getKey())
						.map(element -> element.getElementName()).collect(Collectors.toSet()))
				.collect(Collectors.toSet());

		assertNotNull(actualPassingSets);
		assertNotNull(actualFailingSet);

		assertTrue("Expected passing sets contain " + expectedPassingSets.toString() + " and are the same.",
				expectedPassingSets.containsAll(actualPassingSets)
						&& actualPassingSets.containsAll(expectedPassingSets));
		assertTrue("Expected failing set contains " + expectedFailingSet.toString() + " and are the same.",
				expectedFailingSet.containsAll(actualFailingSet) && actualFailingSet.containsAll(expectedFailingSet));
	}
}
