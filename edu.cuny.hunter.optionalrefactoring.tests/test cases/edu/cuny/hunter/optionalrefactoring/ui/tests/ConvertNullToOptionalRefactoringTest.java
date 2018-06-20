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
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import edu.cuny.hunter.optionalrefactoring.core.refactorings.NullExprHarvester;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author <a href="mailto:raffi.khatchadourian@hunter.cuny.edu">Raffi
 *         Khatchadourian</a>
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
		else
			return unit;
	}

	private static boolean compiles(String source) throws IOException {
		// Save source in .java file.
		Path root = Files.createTempDirectory(null);
		File sourceFile = new File(root.toFile(), "p/A.java");
		sourceFile.getParentFile().mkdirs();
		Files.write(sourceFile.toPath(), source.getBytes());

		// Compile source file.
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		return compiler.run(null, null, null, sourceFile.getPath()) == 0;
	}

	private void helper(NullToOptionalExpectedResult... expectedResults) throws Exception {

		// compute the actual results.
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");
		ASTParser parser = ASTParser.newParser(AST.JLS10);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit c = (CompilationUnit)parser.createAST(null);
		NullExprHarvester harvester = NullExprHarvester.of(icu, c);

		Set<IMethod> actualMethods = harvester.harvestMethods();
		assertNotNull(actualMethods);
		Set<IField> actualFields = harvester.harvestFields();
		assertNotNull(actualFields);
		Set<ILocalVariable> actualLocalVariables = harvester.harvestLocalVariables();
		assertNotNull(actualLocalVariables);

		// compare them with the expected results.
		// for each expected result.
		for (NullToOptionalExpectedResult result : expectedResults) {
			// find the corresponding stream in the actual results.
			Set<IField> expectedFields = result.getFields();
			Set<ILocalVariable> expectedLocalVariables = result.getLocalVariables();
			Set<IMethod> expectedMethods = result.getMethods();

			String errorMessage = "Can't find any JavaElements for seeding ";
			assertNotNull(errorMessage+"Fields.", expectedFields);
			assertFalse(errorMessage+"Fields.", expectedFields.isEmpty());
			assertNotNull(errorMessage+"Local Variables.", expectedLocalVariables);
			assertFalse(errorMessage+"Local Variables.", expectedFields.isEmpty());
			assertNotNull(errorMessage+"Methods.", expectedMethods);
			assertFalse(errorMessage+"Methods.", expectedMethods.isEmpty());

			assertEquals("Harvested Fields are the same: ", actualFields, expectedFields);
			assertEquals("Harvested Local Variables are the same: ", actualLocalVariables, expectedLocalVariables);
			assertEquals("Harvested Methods are the same: ", actualMethods, expectedMethods);
		}
	}
	
	public void testNoResult() throws Exception {
		this.helper(NullToOptionalExpectedResult.of(Optional.empty(), Optional.empty(), Optional.empty()));
	}
	
}