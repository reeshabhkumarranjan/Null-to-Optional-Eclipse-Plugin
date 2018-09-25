/**
 *
 */
package edu.cuny.hunter.optionalrefactoring.ui.tests;

import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.setOf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import edu.cuny.citytech.refactoring.common.tests.RefactoringTest;
import edu.cuny.hunter.optionalrefactoring.core.analysis.Entity;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings.Choices;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
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

	private static final Class<ConvertNullToOptionalRefactoringTest> clazz = ConvertNullToOptionalRefactoringTest.class;

	private static final String REFACTORING_PATH = "ConvertNullToOptional/";

	/**
	 * The name of the directory containing resources under the project directory.
	 */
	private static final String RESOURCE_PATH = "resources";

	private static final Logger LOGGER = Logger.getLogger(clazz.getName());

	private static boolean compiles(String source) throws IOException {
		return compiles(source, Files.createTempDirectory(null));
	}

	public static Test setUpTest(Test test) {
		return new Java18Setup(test);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(clazz));
	}

	public ConvertNullToOptionalRefactoringTest(String name) {
		super(name);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jdt.ui.tests.refactoring.RefactoringTest#createCUfromTestFile
	 * (org.eclipse.jdt.core.IPackageFragment, java.lang.String)
	 */
	@Override
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, String cuName) throws Exception {
		ICompilationUnit unit = super.createCUfromTestFile(pack, cuName);

		// full path of where the CU exists.
		Path directory = Paths.get(unit.getParent().getParent().getParent().getResource().getLocation().toString());

		// compile it to make and store the class file.
		assertTrue("Input should compile", compiles(unit.getSource(), directory));

		return unit;
	}

	private Path getAbsolutionPath(String fileName) {
		Path path = Paths.get(RESOURCE_PATH, fileName);
		Path absolutePath = path.toAbsolutePath();
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
	public String getFileContents(String fileName) throws IOException {
		Path absolutePath = this.getAbsolutionPath(fileName);
		byte[] encoded = Files.readAllBytes(absolutePath);
		return new String(encoded, Charset.defaultCharset());
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	@Override
	protected Refactoring getRefactoring(IJavaElement... elements) throws JavaModelException {
		ConvertNullToOptionalRefactoringProcessor processor = Util.createNullToOptionalRefactoringProcessor(elements,
				RefactoringSettings.testDefaults() /* here the test defaults are injected */, Optional.empty());
		return new ProcessorBasedRefactoring(processor);
	}

	@Override
	public String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private ConvertNullToOptionalRefactoringProcessor getRefactoringProcessor(ICompilationUnit icu)
			throws JavaModelException {
		// we know it's a ProcessorBasedRefactoring since we overriding getRefactoring()
		// in this class.
		ProcessorBasedRefactoring refactoring = (ProcessorBasedRefactoring) this.getRefactoring(icu);

		// we know it's a ConvertNullToOptionalRefactoringProcessor since we overriding
		// getRefactoring() in this class.
		ConvertNullToOptionalRefactoringProcessor refactoringProcessor = (ConvertNullToOptionalRefactoringProcessor) refactoring
				.getProcessor();
		return refactoringProcessor;
	}

	private void propagationHelper(Set<Set<String>> expectedPassingSets, Set<Set<String>> expectedFailingSet,
			Choices turnOff, RefactoringStatus expectedStatus) throws Exception {

		System.out.println();
		// compute the actual results.
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");

		ConvertNullToOptionalRefactoringProcessor refactoring = this.getRefactoringProcessor(icu);

		if (turnOff != null)
			refactoring.settings().set(false, turnOff);

		RefactoringStatus status = refactoring.checkFinalConditions(new NullProgressMonitor(), null);

		System.out.println(refactoring.settings());

		assertTrue(
				"The refactoring status matches the expected refactoring status " + expectedStatus.getSeverity() + ".",
				status.getSeverity() == expectedStatus.getSeverity());

		// Here we are getting all the sets of type dependent entities
		Set<Entity> passingSets = refactoring.getPassingEntities();
		Set<Entity> failingSet = refactoring.getFailingEntities();

		// print to console
		System.out.println(this.getName() + " - SEVERITY: " + status.getSeverity());
		System.out.println("Passing sets:");
		System.out.print("{");
		passingSets.forEach(set -> {
			Util.candidatePrinter(set);
			System.out.print(", ");
		});
		System.out.println("}");
		System.out.println("Failing set:");
		System.out.print("{");
		failingSet.forEach(set -> {
			Util.candidatePrinter(set);
			System.out.print(", ");
		});
		System.out.println("}");

		// convert to sets of strings
		Set<Set<String>> actualPassingSets = passingSets.stream().map(entity -> entity.element().stream()
				.map(element -> element.getElementName()).collect(Collectors.toSet())).collect(Collectors.toSet());

		Set<Set<String>> actualFailingSet = failingSet.stream().map(entity -> entity.element().stream()
				.map(element -> element.getElementName()).collect(Collectors.toSet())).collect(Collectors.toSet());

		assertNotNull(actualPassingSets);
		assertNotNull(actualFailingSet);

		assertTrue("Expected passing sets contain " + expectedPassingSets.toString() + " and are the same.",
				expectedPassingSets.containsAll(actualPassingSets)
						&& actualPassingSets.containsAll(expectedPassingSets));
		assertTrue("Expected failing set contains " + expectedFailingSet.toString() + " and are the same.",
				expectedFailingSet.containsAll(actualFailingSet) && actualFailingSet.containsAll(expectedFailingSet));
	}

	@Override
	public void setFileContents(String fileName, String contents) throws IOException {
		Path absolutePath = this.getAbsolutionPath(fileName);
		Files.write(absolutePath, contents.getBytes());
	}
	
	public void testImplicitlyNullFieldConstructorInit() throws Exception {
		this.transformationHelper(null, RefactoringStatus.createErrorStatus("No nulls to refactor."));
	}

	public void testImplicitlyNullFieldNoConstructorInit() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	public void testImplicitlyNullFieldSomeConstructorInit() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}
	
	public void testAnonymousClassDeclaration() throws Exception {
		this.propagationHelper(setOf(setOf("o")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentFieldArray() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentFieldSimpleName() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentFieldStaticQualified() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentFieldStaticSimpleName() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentFieldSuper() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentFieldSuperQualified() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentFieldThis() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentFieldThisQualified() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentFieldTransitive() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b", "c"), setOf("controlNullDependent"), setOf("d", "e")), setOf(),
				null, new RefactoringStatus());
	}

	public void testAssignmentLocalVariable() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentLocalVariableArray() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testAssignmentLocalVariableTransitive() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("c", "d", "e", "f", "g"), setOf("control")), setOf(), null,
				new RefactoringStatus());
	}

	public void testCastExpressionFailNoSeed() throws Exception {
		this.propagationHelper(setOf(), setOf(), null, RefactoringStatus.createErrorStatus(""));
	}

	public void testCastExpressionFailureMethod() throws Exception {
		this.propagationHelper(setOf(), setOf(setOf("x"), setOf("m")), null, RefactoringStatus.createErrorStatus(""));
	}

	public void testCastExpressionFailureVariable() throws Exception {
		this.propagationHelper(setOf(), setOf(setOf("a"), setOf("b")), null, RefactoringStatus.createErrorStatus(""));
	}

	public void testDeclarationField() throws Exception {
		this.propagationHelper(setOf(setOf("e"), setOf("earray"), setOf("einitializedarray"), setOf("f"),
				setOf("farray"), setOf("finitializedarray")), setOf(), null, new RefactoringStatus());
	}

	public void testDeclarationFieldArray() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testDeclarationFieldTransitive() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("c", "d", "e"), setOf("controlNullDependent")), setOf(),
				null, new RefactoringStatus());
	}

	public void testDeclarationLocalVariable() throws Exception {
		this.propagationHelper(setOf(setOf("a")), setOf(), null, new RefactoringStatus());
	}

	public void testDeclarationLocalVariableArray() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b"), setOf("nullControl")), setOf(), null, new RefactoringStatus());
	}

	public void testDeclarationLocalVariableTransitive() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b", "c"), setOf("d", "e"), setOf("control")), setOf(), null,
				new RefactoringStatus());
	}

	public void testImplicitlyNullVariableDecl() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b")), setOf(), null, new RefactoringStatus());
	}

	public void testInvocationConstructor() throws Exception {
		this.propagationHelper(setOf(setOf("a", "b", "d", "g", "k"), setOf("f", "i", "m"), setOf("o")), setOf(), null,
				new RefactoringStatus());
	}

	public void testInvocationMethod() throws Exception {
		this.propagationHelper(setOf(setOf("o"), setOf("m", "i", "f"), setOf("k", "g", "d", "b", "a")), setOf(), null,
				new RefactoringStatus());
	}

	public void testInvocationSuperConstructor() throws Exception {
		this.propagationHelper(setOf(setOf("g", "d", "b", "a"), setOf("i", "f")), setOf(), null,
				new RefactoringStatus());
	}

	public void testNewStatement() throws Exception {
		this.propagationHelper(setOf(setOf("k", "g", "d", "b", "a"), setOf("m", "i", "f"), setOf("o")), setOf(), null,
				new RefactoringStatus());
	}

	public void testReturnStatement() throws Exception {
		this.propagationHelper(setOf(setOf("nullReturner", "extendedNullReturner", "composedNullReturner"),
				setOf("controlNullReturner")), setOf(), null, new RefactoringStatus());
	}

	public void testSettingsFieldsOff() throws Exception {
		this.propagationHelper(setOf(), setOf(), Choices.FIELDS, RefactoringStatus.createErrorStatus(""));
	}

	public void testSettingsFieldsOn() throws Exception {
		this.propagationHelper(setOf(setOf("x")), setOf(), Choices.METHOD_RETURNS, new RefactoringStatus());
	}

	public void testSettingsImplicitOff() throws Exception {
		this.propagationHelper(setOf(), setOf(), Choices.IMPLICIT_FIELDS, RefactoringStatus.createErrorStatus(""));
	}

	public void testSettingsImplicitOn() throws Exception {
		this.propagationHelper(setOf(setOf("x")), setOf(), null, new RefactoringStatus());
	}

	public void testSettingsLocalVarsOff() throws Exception {
		this.propagationHelper(setOf(setOf("m")), setOf(), Choices.LOCAL_VARS, new RefactoringStatus());
	}

	public void testSettingsLocalVarsOn() throws Exception {
		this.propagationHelper(setOf(setOf("x")), setOf(), Choices.METHOD_RETURNS, new RefactoringStatus());
	}

	public void testSettingsMethodReturnOff() throws Exception {
		this.propagationHelper(setOf(setOf("a")), setOf(), Choices.METHOD_RETURNS, new RefactoringStatus());
	}

	public void testSettingsMethodReturnOn() throws Exception {
		this.propagationHelper(setOf(setOf("m")), setOf(), Choices.FIELDS, new RefactoringStatus());
	}

	public void testSettingsParametersOff() throws Exception {
		this.propagationHelper(setOf(setOf("o")), setOf(), Choices.METHOD_PARAMS, new RefactoringStatus());
	}

	public void testSettingsParametersOn() throws Exception {
		this.propagationHelper(setOf(setOf("x")), setOf(), null, new RefactoringStatus());
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
	
	public void testTransformationWithLineComment() throws Exception {
		this.transformationHelper(null, new RefactoringStatus());
	}

	private void transformationHelper(Choices turnOff, RefactoringStatus expectedStatus) throws Exception {
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");

		ProcessorBasedRefactoring refactoring = (ProcessorBasedRefactoring) this.getRefactoring(icu);
		ConvertNullToOptionalRefactoringProcessor processor = (ConvertNullToOptionalRefactoringProcessor) refactoring
				.getProcessor();

		if (turnOff != null)
			processor.settings().set(false, turnOff);

		RefactoringStatus finalStatus = refactoring.checkFinalConditions(new NullProgressMonitor());
		this.getLogger().info("Final status: " + finalStatus);

		assertTrue("Precondition checking returned the expected RefactoringStatus: "+expectedStatus+".", finalStatus.getSeverity() == expectedStatus.getSeverity());
		this.performChange(refactoring, false);

		String outputTestFileName = this.getOutputTestFileName("A");
		String actual = icu.getSource();
		assertTrue("Actual output should compile.", compiles(actual));

		String expected = this.getFileContents(outputTestFileName);
		assertEqualLines(expected, actual);
	}
}
