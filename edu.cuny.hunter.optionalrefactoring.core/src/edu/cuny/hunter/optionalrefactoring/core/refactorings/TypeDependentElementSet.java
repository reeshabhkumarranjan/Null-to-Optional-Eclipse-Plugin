package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

/**
 * 
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 * 
 * The purpose of this Class is the abstraction of the data structures produced in the RefactorableHarvester class.
 * It behaves as the internal API boundary between the propagation and seeding done in the RefactorableHarvester and
 * other AST processing components of the project, and the evaluation and refactoring components, so that type
 * dependent elements and their graph of type dependency can be more easily represented.
 * 
 * Some of the inherited Set interface methods are not implemented.
 * 
 */
public class TypeDependentElementSet implements Set<IJavaElement> {
	
	private final Set<IJavaElement> elements;
	private final IJavaElement seed;
	private final boolean implicit;
	private final RefactoringStatus status;
	
	/**
	 * 
	 * @param elements			the type dependent set of IJavaElements discovered by the RefactorableHarvester
	 * @param seeds				an existing set of TypeDependentElementSets, one of which the elements belong in
	 * @return					the augmented TypeDependentElementSet
	 * @see						RefactorableHarvester
	 */
	public static TypeDependentElementSet of (Set<IJavaElement> elements,
			Set<TypeDependentElementSet> seeds) {
		
		/*This operation finds the corresponding TDES 
		to the set of IJavaElements passed in from the Harvester*/
		TypeDependentElementSet theSet = seeds.stream()
				.filter(set -> elements
						.contains(set.seed())).findFirst().get();
		theSet.elements.addAll(elements);
		return theSet;
	}

	/**
	 * @param javaElement the IJavaElement that is locally null-type dependent (the seed)
	 * @param implicit Whether or not this locally null-type dependent entity comes from an uninitialized Field.
	 * @return A singleton set for the purpose of finding additional type-dependent entities.
	 */
	public static TypeDependentElementSet createSeed(IJavaElement javaElement, Boolean implicit) {
		return new TypeDependentElementSet(Util.setOf(javaElement), javaElement, implicit, new RefactoringStatus());
	}
	
	/**
	 * @param javaElement The IJavaElement that is locally null-type dependent (the seed)
	 * @param implicit Whether or not this locally null-type dependent entity comes from an uninitialized Field.
	 * @param refactoringStatus A RefactoringStatus that indicates the degree to which this set may be non-refactorable.
	 * @return A singleton set for the purpose of additional analysis.
	 */
	public static TypeDependentElementSet createBadSeed(IJavaElement javaElement, Boolean implicit, RefactoringStatus refactoringStatus) {
		return new TypeDependentElementSet(Util.setOf(javaElement), javaElement, implicit, 
				refactoringStatus);
	}

	private TypeDependentElementSet(Set<IJavaElement> elements, 
			IJavaElement seed, Boolean implicit, RefactoringStatus status) {
		this.elements = elements;
		this.seed = seed;
		this.implicit = implicit;
		this.status = status;
	}

	public IJavaElement seed() {
		return this.seed;
	}
	

	public boolean seedImplicit() {
		return this.implicit;
	}
	
	@Override
	public boolean contains(Object o) {
		return (this.elements.contains(o));
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.elements.containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return this.elements.isEmpty();
	}

	@Override
	public Iterator<IJavaElement> iterator() {
		return this.elements.iterator();
	}

	@Override
	public int size() {
		return this.elements.size();
	}

	public RefactoringStatus getStatus() {
		return status;
	}

	/**
	 * Methods below are stubs, not intended for use.
	 */
	
	public void testStubs() {
		this.toArray();
		this.toArray(null);
		this.clear();
		this.remove(null);
		this.removeAll(null);
		this.retainAll(null);
		this.add(null);
		this.addAll(null);
	}
	
	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {	}	

	@Override
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean add(IJavaElement e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends IJavaElement> c) {
		// TODO Auto-generated method stub
		return false;
	}
}
