package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;

/**
 * 
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 * 
 * The purpose of this Class is the abstraction of the data structures produced in the RefactorableHarvester class.
 * It behaves as the internal API boundary between the propagation and seeding done in the RefactorableHarvester and
 * other AST processing components of the project, and the evaluation and refactoring components, so that type
 * dependent elements and their graph of type dependency can be more easily represented.
 * 
 * Some of the inherited Set interface methods are not implemented as this map is intended to be immutable.
 * 
 */
public class TypeDependentElementSet implements Set<IJavaElement> {
	
	private final Set<IJavaElement> elements;
	private final IJavaElement seed;
	private final boolean implicit;
	
	/**
	 * 
	 * @param elements			the type dependent set of IJavaElements discovered by the RefactorableHarvester
	 * @param seed				the element locally dependent on a null literal or implicitly null
	 * @return					an instance of the TypeDependentElementSet class
	 * @see						RefactorableHarvester
	 */
	public static TypeDependentElementSet of (Set<IJavaElement> elements,
			Map<IJavaElement,Boolean> seeds) {
		
		IJavaElement seed = elements.stream().filter(
				element -> seeds.keySet().contains(element)).findFirst().get();
				
		return new TypeDependentElementSet(elements, seed, seeds.get(seed));
	}
	
	private TypeDependentElementSet(Set<IJavaElement> elements, 
			IJavaElement seed, Boolean implicit) {
		this.elements = elements;
		this.seed = seed;
		this.implicit = implicit;
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

	/**
	 * Methods below are stubs, not intended for use.
	 */
	
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
	public boolean add(IJavaElement e) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean addAll(Collection<? extends IJavaElement> c) {
		/*if (c instanceof TypeDependentElementSet) {
			this.merge((TypeDependentElementSet)c);
			return true;
		}
		else throw new IllegalArgumentException("Tried to add a collection that is not a TypeDependentElementSet.");*/
		return false;
	}

/*	private void merge(TypeDependentElementSet c) {
		c.forEach(element -> this.elements.putIfAbsent(element, c.elements.get(element)));
	}*/

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
}
