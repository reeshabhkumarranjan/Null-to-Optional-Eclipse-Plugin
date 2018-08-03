/**
 * 
 */
package edu.cuny.hunter.optionalrefactoring.ui.tests;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.ui.tests.refactoring.Java18Setup;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import edu.cuny.citytech.refactoring.common.tests.RefactoringTest;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.TypeDependentElementSet;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

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

	public void testTypeDependentElementSet() throws Exception {
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		CompilationUnit c = (CompilationUnit)parser.createAST(null);
		Set<IJavaElement> elements = new LinkedHashSet<>();
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(SimpleName node) {
				elements.add(node.resolveBinding().getJavaElement());
				return super.visit(node);
			}
		};
		c.accept(visitor);
		// grab a random element from the stream and make it a seed
		IJavaElement seed = elements.stream().findFirst().get();
		// get a random element from the remaining and make it a bad seed
		Set<IJavaElement> others = elements.stream().filter(element -> !element.equals(seed)).collect(Collectors.toSet());
		IJavaElement badSeed = others.stream().findFirst().get();
		// make a singleton seeded set with the chosen seed element
		TypeDependentElementSet seeded = TypeDependentElementSet.createSeed(seed, Boolean.FALSE);
		// make a singleton failing seeded set
		TypeDependentElementSet badTdes = TypeDependentElementSet.createBadSeed(badSeed, 
				Boolean.FALSE, RefactoringStatus.createErrorStatus(""));
		// make a set of seeded sets to mimic the set of seeds generated by the seeding stage
		Set<TypeDependentElementSet> mockedSeedSet = Util.setOf(seeded);
		// merge the elements from the visitor with the mockedSeedSet to simulate the merging of found elements in the propagation stage
		TypeDependentElementSet tdes = TypeDependentElementSet.of(elements, mockedSeedSet);
		// for coverage of the default interface methods
		tdes.testStubs();
		// test the accessor and predicate methods
		assertTrue("TDES has the right seed.", tdes.seed().equals(seed));
		assertTrue("The seed is not implicit", !tdes.seedImplicit());
		assertTrue("TDES contains the each element produced by the visitor", elements.stream().allMatch(tdes::contains));
		assertTrue("TDES contains all the visitor-produced elements", tdes.containsAll(elements));
		assertTrue("BadSeed tdes is not OK status.", !badTdes.getStatus().isOK());
		assertTrue("TDES has an OK status.", tdes.getStatus().isOK());
		assertTrue("TDES is not empty.", !tdes.isEmpty());
		assertTrue("TDES has "+elements.size()+" elements and visitor produced "+elements.size()+"elements.", elements.size() == tdes.size());
	}

	private void helper(Set<Set<String>> expectedPassingSets, Set<Set<String>> expectedFailingSets,
			RefactoringStatus expectedStatus) throws Exception {
		System.out.println();
		// compute the actual results.
		ICompilationUnit icu = this.createCUfromTestFile(this.getPackageP(), "A");
		
		// we know it's a ProcessorBasedRefactoring since we overriding getRefactoring() in this class.
		ProcessorBasedRefactoring refactoring = (ProcessorBasedRefactoring) this.getRefactoring(icu);
		
		// we know it's a ConvertNullToOptionalRefactoringProcessor since we overriding getRefactoring() in this class.
		ConvertNullToOptionalRefactoringProcessor processor = (ConvertNullToOptionalRefactoringProcessor) refactoring.getProcessor();
		
		RefactoringStatus status = refactoring.checkFinalConditions(new NullProgressMonitor());
		assertTrue("The refactoring status matches the expected refactoring status "+expectedStatus.getSeverity()+".", 
				status.getSeverity() == expectedStatus.getSeverity());

		// Here we are getting all the sets of type dependent entities
		Set<TypeDependentElementSet> passingSets = processor.getPassingEntities();
		Set<TypeDependentElementSet> failingSets = processor.getFailingEntities();
		
		// print to console
		System.out.println(this.getName()+" - SEVERITY: "+status.getSeverity());
		System.out.println("Passing sets:");
		System.out.print("{");
		passingSets.forEach(set -> {
			Util.candidatePrinter(set);
			System.out.print(", ");
		});
		System.out.println("}");
		System.out.println("Failing sets:");
		System.out.print("{");
		failingSets.forEach(set -> {
			Util.candidatePrinter(set);
			System.out.print(", ");
		});
		System.out.println("}");
		
		// convert to sets of strings
		Set<Set<String>> actualPassingSets = passingSets.stream()
				.map(set -> set.stream().map(element -> element.getElementName().toString()).collect(Collectors.toSet()))
				.collect(Collectors.toSet());
		
		Set<Set<String>> actualFailingSets = failingSets.stream().map(
				set -> set.stream().map(element -> element.getElementName().toString()).collect(Collectors.toSet()))
				.collect(Collectors.toSet());

		assertNotNull(actualPassingSets);
		assertNotNull(actualFailingSets);

		assertTrue("Expected passing sets contain "+expectedPassingSets.toString()+" and are the same.", 
				expectedPassingSets.containsAll(actualPassingSets) && 
				actualPassingSets.containsAll(expectedPassingSets));
		assertTrue("Expected failing sets contain "+expectedFailingSets.toString()+" and are the same.", 
				expectedFailingSets.containsAll(actualFailingSets) && 
				actualFailingSets.containsAll(expectedFailingSets));
	}
	
	public void testAnonymousClassDeclaration() throws Exception {
		this.helper(setOf(setOf("o")), setOf(), new RefactoringStatus());
	}
	
	public void testCastExpressionFailNoSeed() throws Exception {
		this.helper(
				setOf(),
				setOf(),
				RefactoringStatus.createErrorStatus(""));
	}
	
	public void testCastExpressionFailureVariable() throws Exception {
		this.helper(
				setOf(),
				setOf(setOf("a"),setOf("b")),
				RefactoringStatus.createErrorStatus(""));
	}
	
	public void testCastExpressionFailureMethod() throws Exception {
		this.helper(
				setOf(),
				setOf(setOf("x"),setOf("m")),
				RefactoringStatus.createErrorStatus(""));
	}
	
	public void testImplicitlyNullVariableDecl() throws Exception {
		this.helper( 
				setOf(setOf("a","b")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentFieldSimpleName() throws Exception {
		this.helper( 
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentFieldThis() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentFieldThisQualified() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentFieldSuper() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentFieldSuperQualified() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentFieldStaticSimpleName() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentFieldStaticQualified() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentFieldArray() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentFieldTransitive() throws Exception {
		this.helper(
				setOf(setOf("a","b","c"),
						setOf("controlNullDependent"),
						setOf("d","e")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentLocalVariable() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentLocalVariableArray() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testAssignmentLocalVariableTransitive() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("c","d","e","f","g"),
						setOf("control")),
				setOf(),
				new RefactoringStatus());
	}

	public void testDeclarationField() throws Exception {
		this.helper(
				setOf(setOf("e"),
						setOf("earray"),
						setOf("einitializedarray"),
						setOf("f"),
						setOf("farray"),
						setOf("finitializedarray")),
				setOf(),
				new RefactoringStatus());
	}

	public void testDeclarationFieldArray() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}

	public void testDeclarationFieldTransitive() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("c","d","e"),
						setOf("controlNullDependent")),
				setOf(),
				new RefactoringStatus());
	}

	public void testDeclarationLocalVariable() throws Exception {
		this.helper(setOf(setOf("a")),
				setOf(),
				new RefactoringStatus());
	}

	public void testDeclarationLocalVariableArray() throws Exception {
		this.helper(
				setOf(setOf("a","b"),
						setOf("nullControl")),
				setOf(),
				new RefactoringStatus());
	}


	public void testDeclarationLocalVariableTransitive() throws Exception {
		this.helper(
				setOf(setOf("a","b","c"), 
						setOf("d","e"),
						setOf("control")),
				setOf(),
				new RefactoringStatus());
	}

	public void testInvocationConstructor() throws Exception {
		this.helper(
				setOf(setOf("a","b","d","g","k"),
						setOf("f","i","m"),
						setOf("o")),
				setOf(),
				new RefactoringStatus());
	}

	public void testInvocationMethod() throws Exception {
		this.helper(
				setOf(setOf("o"),
						setOf("m","i","f"),
						setOf("k","g","d","b","a")),
				setOf(),
				new RefactoringStatus());	
	}

	public void testInvocationSuperConstructor() throws Exception {
		this.helper(
				setOf(setOf("g","d","b","a"),
						setOf("i","f")),
				setOf(),
				new RefactoringStatus());	
	}

	public void testNewStatement() throws Exception {
		this.helper(
				setOf(setOf("k","g","d","b","a"),
						setOf("m","i","f"),
						setOf("o")),
				setOf(),
				new RefactoringStatus());	
	}

	public void testReturnStatement() throws Exception {
		this.helper(
				setOf(setOf("nullReturner", "extendedNullReturner", "composedNullReturner"),
						setOf("controlNullReturner")),
				setOf(),
				new RefactoringStatus());	
	}

	@Override
	protected Logger getLogger() {
		return LOGGER;
	}

	@Override
	protected Refactoring getRefactoring(IJavaElement... elements) throws JavaModelException {
		ConvertNullToOptionalRefactoringProcessor processor = 
				Util.createNullToOptionalRefactoringProcessor(elements, Optional.empty());
		return new ProcessorBasedRefactoring(processor);
	}
}
