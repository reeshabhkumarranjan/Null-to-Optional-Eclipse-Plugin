/**
 * 
 */
package edu.cuny.hunter.optionalrefactoring.ui.tests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Entity;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings.CHOICES;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;
import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;

import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.*;
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
	 * The name of the directory containing resources under the project
	 * directory.
	 */
	private static final String RESOURCE_PATH = "resources";

	private static final Logger LOGGER = Logger.getLogger(clazz.getName());

	public static Test setUpTest(Test test) {
		return new Java18Setup(test);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(clazz));
	}

	public ConvertNullToOptionalRefactoringTest(String name) {
		super(name);
	}

	@Override
	public String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.ui.tests.refactoring.RefactoringTest#getFileContents(java
	 * .lang.String) Had to override this method because, since this plug-in is
	 * a fragment (at least I think that this is the reason), it doesn't have an
	 * activator and the bundle is resolving to the eclipse refactoring test
	 * bundle.
	 */
	@Override
	public String getFileContents(String fileName) throws IOException {
		Path absolutePath = getAbsolutionPath(fileName);
		byte[] encoded = Files.readAllBytes(absolutePath);
		return new String(encoded, Charset.defaultCharset());
	}

	private Path getAbsolutionPath(String fileName) {
		Path path = Paths.get(RESOURCE_PATH, fileName);
		Path absolutePath = path.toAbsolutePath();
		return absolutePath;
	}

	public void setFileContents(String fileName, String contents) throws IOException {
		Path absolutePath = getAbsolutionPath(fileName);
		Files.write(absolutePath, contents.getBytes());
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

		if (!unit.isStructureKnown())
			throw new IllegalArgumentException(cuName + " has structural errors.");

		// full path of where the CU exists.
		Path directory = Paths.get(unit.getParent().getParent().getParent().getResource().getLocation().toString());

		// compile it to make and store the class file.
		assertTrue("Input should compile", compiles(unit.getSource(), directory));

		return unit;
	}

	@SuppressWarnings("unused")
	private static boolean compiles(String source) throws IOException {
		return compiles(source, Files.createTempDirectory(null));
	}

	private static boolean compiles(String source, Path directory) throws IOException {
		// Save source in .java file.
		File sourceFile = new File(directory.toFile(), "bin/p/A.java");
		sourceFile.getParentFile().mkdirs();
		Files.write(sourceFile.toPath(), source.getBytes());

		// Compile source file.
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		boolean compileSuccess = compiler.run(null, null, null, sourceFile.getPath()) == 0;

		sourceFile.delete();
		return compileSuccess;
	}

	private void helper(Set<Set<String>> expectedPassingSets, Set<String> expectedFailingSet, 
			CHOICES turnOff, RefactoringStatus expectedStatus) throws Exception {

		System.out.println();
		// compute the actual results.
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");

		ConvertNullToOptionalRefactoringProcessor refactoring = Util.createNullToOptionalRefactoringProcessor(
				new IJavaElement[] { icu }, Optional.empty());
		if (turnOff != null) refactoring.settings().set(false, turnOff);

		RefactoringStatus status = refactoring.checkFinalConditions(new NullProgressMonitor(), null);

		System.out.println(refactoring.settings().toString());

		assertTrue("The refactoring status matches the expected refactoring status "+expectedStatus.getSeverity()+".", 
				status.getSeverity() == expectedStatus.getSeverity());

		// Here we are getting all the sets of type dependent entities
		Set<Entity> passingSets = refactoring.getPassingEntities();
		Set<Entity> failingSet = refactoring.getFailingEntities();

		// print to console
		System.out.println(this.getName()+" - SEVERITY: "+status.getSeverity());
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
		Set<Set<String>> actualPassingSets = passingSets.stream().map(
				entity -> entity.element().stream().map(
						element -> element.getElementName())
				.collect(Collectors.toSet())).collect(Collectors.toSet());

		Set<Set<String>> actualFailingSet = failingSet.stream().map(
				entity -> entity.element().stream().map(
						element -> element.getElementName())
				.collect(Collectors.toSet())).collect(Collectors.toSet());

		assertNotNull(actualPassingSets);
		assertNotNull(actualFailingSet);

		assertTrue("Expected passing sets contain "+expectedPassingSets.toString()+" and are the same.", 
				expectedPassingSets.containsAll(actualPassingSets) && 
				actualPassingSets.containsAll(expectedPassingSets));
		assertTrue("Expected failing set contains "+expectedFailingSet.toString()+" and are the same.", 
				expectedFailingSet.containsAll(actualFailingSet) && 
				actualFailingSet.containsAll(expectedFailingSet));
	}

	public void testSettingsMethodReturnOn() throws Exception {
		this.helper(setOf(setOf("m")), 
				setOf(), CHOICES.FIELDS, 
				new RefactoringStatus());
	}

	public void testSettingsMethodReturnOff() throws Exception {
		this.helper(setOf(setOf("a")), 
				setOf(), CHOICES.METHOD_RETURNS, 
				new RefactoringStatus());
	}

	public void testSettingsParametersOn() throws Exception {
		this.helper(setOf(setOf("x")), 
				setOf(), null, 
				new RefactoringStatus());
	}

	public void testSettingsParametersOff() throws Exception {
		this.helper(setOf(setOf("o")), 
				setOf(), CHOICES.METHOD_PARAMS, 
				new RefactoringStatus());
	}

	public void testSettingsLocalVarsOn() throws Exception {
		this.helper(setOf(setOf("x")), 
				setOf(), CHOICES.METHOD_RETURNS, 
				new RefactoringStatus());
	}

	public void testSettingsLocalVarsOff() throws Exception {
		this.helper(setOf(setOf("m")), 
				setOf(), CHOICES.LOCAL_VARS, 
				new RefactoringStatus());
	}

	public void testSettingsFieldsOn() throws Exception {
		this.helper(setOf(setOf("x")), 
				setOf(), CHOICES.METHOD_RETURNS, 
				new RefactoringStatus());
	}

	public void testSettingsFieldsOff() throws Exception {
		this.helper(setOf(), 
				setOf(), CHOICES.FIELDS, 
				RefactoringStatus.createErrorStatus(""));
	}

	public void testSettingsImplicitOn() throws Exception {
		this.helper(setOf(setOf("x")), 
				setOf(), null, 
				new RefactoringStatus());
	}

	public void testSettingsImplicitOff() throws Exception {
		this.helper(setOf(), 
				setOf(), CHOICES.IMPLICIT_FIELDS, 
				RefactoringStatus.createErrorStatus(""));
	}

	public void testAnonymousClassDeclaration() throws Exception {
		this.helper(setOf(setOf("o")), 
				setOf(), 
				null,
				new RefactoringStatus());
	}

	public void testCastExpressionFailNoSeed() throws Exception {
		this.helper(setOf(),
				setOf(),
				null,
				RefactoringStatus.createErrorStatus(""));
	}

	public void testCastExpressionFailureVariable() throws Exception {
		this.helper(setOf(),
				setOf("a","b"),
				null,
				RefactoringStatus.createErrorStatus(""));
	}

	public void testCastExpressionFailureMethod() throws Exception {
		this.helper(setOf(),
				setOf("x","m"),
				null,
				RefactoringStatus.createErrorStatus(""));
	}

	public void testImplicitlyNullVariableDecl() throws Exception {
		this.helper(setOf(setOf("a","b")),
				setOf(),
				null,
				new RefactoringStatus());
	}

	public void testAssignmentFieldSimpleName() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,
				new RefactoringStatus());
	}

	public void testAssignmentFieldThis() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,
				new RefactoringStatus());
	}

	public void testAssignmentFieldThisQualified() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testAssignmentFieldSuper() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testAssignmentFieldSuperQualified() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testAssignmentFieldStaticSimpleName() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testAssignmentFieldStaticQualified() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testAssignmentFieldArray() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testAssignmentFieldTransitive() throws Exception {
		this.helper(setOf(setOf("a","b","c"),
				setOf("controlNullDependent"),
				setOf("d","e")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testAssignmentLocalVariable() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testAssignmentLocalVariableArray() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testAssignmentLocalVariableTransitive() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("c","d","e","f","g"),
				setOf("control")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testDeclarationField() throws Exception {
		this.helper(setOf(setOf("e"),
				setOf("earray"),
				setOf("einitializedarray"),
				setOf("f"),
				setOf("farray"),
				setOf("finitializedarray")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testDeclarationFieldArray() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testDeclarationFieldTransitive() throws Exception {
		this.helper(setOf(setOf("a","b"),
				setOf("c","d","e"),
				setOf("controlNullDependent")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testDeclarationLocalVariable() throws Exception {
		this.helper(setOf(setOf("a")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testDeclarationLocalVariableArray() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				null,				
				new RefactoringStatus());
	}


	public void testDeclarationLocalVariableTransitive() throws Exception {
		this.helper(
				setOf(setOf("a","b","c"), 
						setOf("d","e"),
						setOf("control")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testInvocationConstructor() throws Exception {
		this.helper(
				setOf(setOf("a","b","d","g","k"),
						setOf("f","i","m"),
						setOf("o")),
				setOf(),
				null,				
				new RefactoringStatus());
	}

	public void testInvocationMethod() throws Exception {
		this.helper(
				setOf(setOf("o"),
						setOf("m","i","f"),
						setOf("k","g","d","b","a")),
				setOf(),
				null,				
				new RefactoringStatus());	
	}

	public void testInvocationSuperConstructor() throws Exception {
		this.helper(
				setOf(setOf("g","d","b","a"),
						setOf("i","f")),
				setOf(),
				null,				
				new RefactoringStatus());	
	}

	public void testNewStatement() throws Exception {
		this.helper(
				setOf(setOf("k","g","d","b","a"),
						setOf("m","i","f"),
						setOf("o")),
				setOf(),
				null,				
				new RefactoringStatus());	
	}

	public void testReturnStatement() throws Exception {
		this.helper(
				setOf(setOf("nullReturner", "extendedNullReturner", "composedNullReturner"),
						setOf("controlNullReturner")),
				setOf(),
				null,				
				new RefactoringStatus());	
	}
}
