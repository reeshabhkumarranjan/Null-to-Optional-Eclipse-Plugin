/**
 * 
 */
package edu.cuny.hunter.optionalrefactoring.ui.tests;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactorableHarvester;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;
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
	
	private void helper1(Set<String> expectedElements) throws Exception {

		// compute the actual results.
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit c = (CompilationUnit)parser.createAST(null);
		RefactorableHarvester harvester = RefactorableHarvester.of(icu, c, 
				SearchEngine.createJavaSearchScope(new ICompilationUnit[] { icu }), new NullProgressMonitor());

		Set<IJavaElement> seeds = harvester.getSeeds();
		Util.candidatePrinter(seeds);
		Set<String> actualElements = seeds.stream()
				.map(element -> element.getElementName().toString())
				.collect(Collectors.toSet());
		assertNotNull(actualElements);		
		
		// compare them with the expected results.
		assertTrue("Expected sets contain "+expectedElements.toString()+" and are the same.", 
				expectedElements.containsAll(actualElements));
	}
	
	private void helper2(Set<Set<String>> expectedElements) throws Exception {

		// compute the actual results.
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit c = (CompilationUnit)parser.createAST(null);
		RefactorableHarvester harvester = RefactorableHarvester.of(icu, c, 
				SearchEngine.createJavaSearchScope(new ICompilationUnit[] { icu }), new NullProgressMonitor());

		Set<Set<IJavaElement>> sets = harvester.harvestRefactorableContexts();
		sets.forEach(set -> Util.candidatePrinter(set));
		
		Set<Set<String>> actualElements = sets.stream()
				.map(set -> set.stream().map(element -> element.getElementName().toString()).collect(Collectors.toSet()))
				.collect(Collectors.toSet());
		assertNotNull(actualElements);		

		// compare them with the expected results.
		assertTrue("Expected sets contain "+expectedElements.toString()+" and are the same.", 
				expectedElements.containsAll(actualElements));
	}

	public void testAssignmentFieldQualifiedNameSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testAssignmentFieldQualifiedNameHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}

	public void testAssignmentFieldSimpleNameSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testAssignmentFieldSimpleNameHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}
	
	public void testAssignmentFieldSuperSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testAssignmentFieldSuperHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));	
	}

	public void testAssignmentFieldThisQualifiedNameSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}

	public void testAssignmentFieldThisQualifiedNameHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));	
	}

	public void testAssignmentFieldThisSimpleNameSeed() throws Exception {
		this.helper1(Util.setCons("a"));	
	}

	public void testAssignmentFieldThisSimpleNameHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));	
	}

	public void testAssignmentLocalVariableSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}

	public void testAssignmentLocalVariableHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}

	public void testAssignmentLocalVariableArrayAccessSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testAssignmentLocalVariableArrayAccessHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}
	
	public void testAssignmentLocalVariableFieldAccessSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testAssignmentLocalVariableFieldAccessHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}
	
	public void testAssignmentLocalVariableArrayAccessFieldAccessSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testAssignmentLocalVariableArrayAccessFieldAccessHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}
	
	public void testAssignmentLocalVariableFieldAccessArrayAccessSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testAssignmentLocalVariableFieldAccessArrayAccessHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}
	
	public void testDeclarationLocalVariableSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testDeclarationLocalVariableHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}
	
	public void testDeclarationLocalVariableArraySeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testDeclarationLocalVariableArrayHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}
	
	public void testInvocationConstructorSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testInvocationConstructorHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));
	}
	
	public void testInvocationMethodSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testInvocationMethodHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));	
	}
	
	public void testInvocationSuperConstructorSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}

	public void testInvocationSuperConstructorHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));	
	}

	public void testNewStatementSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}
	
	public void testNewStatementHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));	
	}
	
	public void testReturnStatementSeed() throws Exception {
		this.helper1(Util.setCons("a"));
	}

	public void testReturnStatementHarvest() throws Exception {
		this.helper2(Util.setCons(Util.setCons("a")));	
	}

}
