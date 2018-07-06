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
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a")));
	}

	public void testAssignmentLocalVariableArrayAccess() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a")));
	}

	public void testDeclarationLocalVariable() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a")));
	}

	public void testDeclarationLocalVariableArray() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a")));
	}

	public void testInvocationConstructor() throws Exception {
		this.helper(Util.setCons("o"), Util.setCons(Util.setCons("a")));
	}

	public void testInvocationMethod() throws Exception {
		this.helper(Util.setCons("o"), Util.setCons(Util.setCons("a")));	
	}

	public void testInvocationSuperConstructor() throws Exception {
		this.helper(Util.setCons("o"), Util.setCons(Util.setCons("a")));	
	}

	public void testNewStatement() throws Exception {
		this.helper(Util.setCons("o"), Util.setCons(Util.setCons("a")));	
	}

	public void testReturnStatement() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a")));	
	}

	public void testAssignmentLocalVariableTransitive2arity() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a","b","c")));
	}

	public void testAssignmentLocalVariableTransitive1arity() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a","b")));
	}

	public void testAssignmentLocalVariableTransitive2arityAnd1arity() throws Exception {
		this.helper(Util.setCons("a","d"), Util.setCons(Util.setCons("a","b","c"),Util.setCons("d","e")));
	}
	
	public void testDeclarationLocalVariableTransitive1arity() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a","b")));
	}
	
	public void testDeclarationLocalVariableTransitive2arity() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a","b","c")));
	}
	
	public void testDeclarationLocalVariableTransitive2arityAnd1arity() throws Exception {
		this.helper(Util.setCons("a","d"), Util.setCons(Util.setCons("a","b","c"), Util.setCons("d","e")));
	}
	
	public void testAssignmentFieldTransitive2arity() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a","b","c")));
	}
	
	public void testAssignmentFieldTransitive1arity() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a","b")));
	}
	
<<<<<<< HEAD
	public void testAssignmentField() throws Exception {
		this.helper(Util.setCons("thisQualifiedName", "thisSimpleName", "simpleName", 
				"superQualifiedName", "superSimpleName", "staticSimpleName", "staticQualifiedName"), 
				Util.setCons(Util.setCons("superSimpleName"), 
						Util.setCons("thisQualifiedName"), 
						Util.setCons("staticQualifiedName"), 
						Util.setCons("simpleName"), 
						Util.setCons("thisSimpleName"), 
						Util.setCons("staticSimpleName"), 
						Util.setCons("superQualifiedName")));
	}
	
	public void testAssignmentFieldArray() throws Exception {
		this.helper(Util.setCons("thisQualifiedName", "thisSimpleName", "simpleName", 
				"thisQualifiedNameInitialized", "thisSimpleNameInitialized", "simpleNameInitialized", "fieldAccess"), 
				Util.setCons(Util.setCons("thisQualifiedName"),
						Util.setCons("thisSimpleName"),
						Util.setCons("simpleName"),
						Util.setCons("thisQualifiedNameInitialized"),
						Util.setCons("thisSimpleNameInitialized"),
						Util.setCons("simpleNameInitialized"),
						Util.setCons("fieldAccess")));
	}
	
	public void testDeclarationField() throws Exception {
		this.helper(Util.setCons("e", "earray", "einitializedarray", "f", "farray", "finitializedarray"), 
				Util.setCons(Util.setCons("e"),
						Util.setCons("earray"),
						Util.setCons("einitializedarray"),
						Util.setCons("f"),
						Util.setCons("farray"),
						Util.setCons("finitializedarray")));
=======
	public void testAssignmentLocalVariableArrayAccessTransitive1arity() throws Exception {
		this.helper(Util.setCons("a"), Util.setCons(Util.setCons("a","b")));
>>>>>>> c7dac3a97d18d68c1d847c2cbef30b7b2fce4a2b
	}
}
