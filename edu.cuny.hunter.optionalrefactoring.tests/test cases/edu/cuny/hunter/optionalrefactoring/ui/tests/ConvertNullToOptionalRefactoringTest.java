/**
 * 
 */
package edu.cuny.hunter.optionalrefactoring.ui.tests;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactorableHarvester;
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

	private void helper(String... _expectedElements) throws Exception {

		Set<String> expectedElements = Arrays.asList(_expectedElements).stream().collect(Collectors.toSet());
		
		// compute the actual results.
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit c = (CompilationUnit)parser.createAST(null);
		RefactorableHarvester harvester = RefactorableHarvester.of(icu, c, 
				SearchEngine.createJavaSearchScope(new ICompilationUnit[] { icu }), null);

		Set<String> actualElements = harvester.harvestRefactorableContexts().keySet().stream().map(element -> element.getElementName().toString()).collect(Collectors.toSet());
		assertNotNull(actualElements);		

		// compare them with the expected results.
		assertTrue("Expected sets contain "+expectedElements.toString()+" and are the same.", 
				expectedElements.containsAll(actualElements));
	}

	public void testAssignmentLocalVariable() throws Exception {
		this.helper("a");
	}

	public void testAssignmentLocalVariableArrayAccess() throws Exception {
		this.helper("a");
	}
	
	public void testAssignmentLocalVariableFieldAccess() throws Exception {
		this.helper("a");
	}
	
	public void testAssignmentLocalVariableArrayAccessFieldAccess() throws Exception {
		this.helper("a");
	}
	
	public void testAssignmentLocalVariableFieldAccessArrayAccess() throws Exception {
		this.helper("a");
	}
	
	public void testDeclarationLocalVariable() throws Exception {
		this.helper("a");
	}
	
	public void testDeclarationLocalVariableArray() throws Exception {
		this.helper("a");
	}
}