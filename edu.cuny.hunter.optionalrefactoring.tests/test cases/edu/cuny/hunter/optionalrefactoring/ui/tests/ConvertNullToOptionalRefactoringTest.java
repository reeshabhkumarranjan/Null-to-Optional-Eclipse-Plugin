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
import static edu.cuny.hunter.optionalrefactoring.core.utils.Util.*;
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

	private void helper(Set<String> expectedElements, Set<Set<String>> expectedSets) throws Exception {

		// compute the actual results.
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit c = (CompilationUnit)parser.createAST(null);
		RefactorableHarvester harvester = RefactorableHarvester.of(icu, c, 
				SearchEngine.createJavaSearchScope(new ICompilationUnit[] { icu }), new NullProgressMonitor());

		// Here we are getting just the seeds without transitive dependencies
		Set<IJavaElement> seeds = harvester.getSeeds();
		
		Set<String> actualElements = seeds.stream()
				.map(element -> element.getElementName().toString())
				.collect(Collectors.toSet());
		
		assertNotNull(actualElements);		
		assertTrue("Expected elements are "+expectedElements.toString()+" and are the same in both sets.", 
				expectedElements.containsAll(actualElements));
		
		// Here we are getting all the sets of type dependent entities
		Set<Set<IJavaElement>> sets = harvester.harvestRefactorableContexts();

		Set<Set<String>> actualSets = sets.stream()
				.map(set -> set.stream().map(element -> element.getElementName().toString()).collect(Collectors.toSet()))
				.collect(Collectors.toSet());
		
		assertNotNull(actualSets);		
		
		assertTrue("Expected sets contain "+expectedSets.toString()+" and are the same.", 
				expectedSets.containsAll(actualSets));
	}

	public void testAssignmentLocalVariable() throws Exception {
		this.helper(setOf("a"), setOf(setOf("a")));
	}

	public void testAssignmentLocalVariableArray() throws Exception {
		this.helper(setOf("a"), setOf(setOf("a")));
	}

	public void testDeclarationLocalVariable() throws Exception {
		this.helper(setOf("a"), setOf(setOf("a")));
	}

	public void testDeclarationLocalVariableArray() throws Exception {
		this.helper(setOf("a"), setOf(setOf("a")));
	}

	public void testInvocationConstructor() throws Exception {
		this.helper(setOf("a","f","o"), 
				setOf(setOf("a","b","d","g","k"),
						setOf("f","i","m"),
						setOf("o")));
	}

	public void testInvocationMethod() throws Exception {
		this.helper(setOf("k","m","o"), 
				setOf(setOf("o"),
						setOf("m","i","f"),
						setOf("k","g","d","b","a")));	
	}

	public void testInvocationSuperConstructor() throws Exception {
		this.helper(setOf("g","i"), 
				setOf(setOf("g","d","b","a"),
						setOf("i","f")));	
	}

	public void testNewStatement() throws Exception {
		this.helper(setOf("o"), setOf(setOf("a")));	
	}

	public void testReturnStatement() throws Exception {
		this.helper(setOf("nullReturner", "extendedNullReturner", "composedNullReturner", "controlNullReturner"), 
				setOf(setOf("nullReturner", "extendedNullReturner", "composedNullReturner"),
						setOf("controlNullReturner")));	
	}

	public void testAssignmentLocalVariableTransitive() throws Exception {
		this.helper(setOf("a","e","control"), 
				setOf(setOf("a","b"),
						setOf("c","d","e","f","g"),
						setOf("control")));
	}
	
	public void testDeclarationLocalVariableTransitive() throws Exception {
		this.helper(setOf("a","d","control"), 
				setOf(setOf("a","b","c"), 
						setOf("d","e"),
						setOf("control")));
	}
	
	public void testAssignmentFieldTransitive() throws Exception {
		this.helper(setOf("a","controlNullDependent","d"), 
				setOf(setOf("a","b","c"),
						setOf("controlNullDependent"),
						setOf("d","e")));
	}
	
	public void testAssignmentField() throws Exception {
		this.helper(setOf("thisQualifiedName", "thisSimpleName", "simpleName", 
				"superQualifiedName", "superSimpleName", "staticSimpleName", "staticQualifiedName"), 
				setOf(setOf("superSimpleName"), 
						setOf("thisQualifiedName"), 
						setOf("staticQualifiedName"), 
						setOf("simpleName"), 
						setOf("thisSimpleName"), 
						setOf("staticSimpleName"), 
						setOf("superQualifiedName")));
	}
	
	public void testAssignmentFieldArray() throws Exception {
		this.helper(setOf("thisQualifiedName", "thisSimpleName", "simpleName", 
				"thisQualifiedNameInitialized", "thisSimpleNameInitialized", "simpleNameInitialized", "fieldAccess"), 
				setOf(setOf("thisQualifiedName"),
						setOf("thisSimpleName"),
						setOf("simpleName"),
						setOf("thisQualifiedNameInitialized"),
						setOf("thisSimpleNameInitialized"),
						setOf("simpleNameInitialized"),
						setOf("fieldAccess")));
	}
	
	public void testDeclarationField() throws Exception {
		this.helper(setOf("e", "earray", "einitializedarray", "f", "farray", "finitializedarray"), 
				setOf(setOf("e"),
						setOf("earray"),
						setOf("einitializedarray"),
						setOf("f"),
						setOf("farray"),
						setOf("finitializedarray")));
	}
	
	public void testDeclarationFieldTransitive() throws Exception {
		this.helper(setOf("a","controlNullDependent","c"), 
				setOf(setOf("a","b"),
						setOf("c","d","e"),
						setOf("controlNullDependent")));
	}
}
