package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
 * The static factory method of() retains in copies of the maps of dependencies and dependents from the 
 * RefactorableHarvester only keys present in the Set<IJavaElement>, and then
 * for each element in the Set<IJavaElement> creates a new Map<Relation,Set<IJavaElement>> which consolidates that
 * elements dependents and dependencies and then places it in a master map for this class "elements".
 * 
 * The class uses the set interface because it behaves like a set, with unique keys, but also retains the transitive
 * type dependency mappings for every element which can be retrieved using an appropriately named lookup method with 
 * the element as the key.
 * Some of the inherited Set interface methods are not implemented as this map is intended to be immutable.
 * 
 */
public class TypeDependentElementSet implements Set<IJavaElement> {
	
	private static enum Relationship {
		DEPENDENTS,
		DEPENDENCIES;
	}

	private final Map<IJavaElement, Map<Relationship, Set<IJavaElement>>> elements;
	
	/**
	 * 
	 * @param unmappedElements	the type dependent set of IJavaElements discovered by the RefactorableHarvester
	 * @param _dependents		all IJavaElements encountered by the RefactorableHarvester with their dependent element mappings.
	 * @param _dependencies		all IJavaElements encountered by the RefactorableHarvester with their dependency element mappings.
	 * @return					an instance of the TypeDependentElementSet class
	 * @see						RefactorableHarvester
	 */
	public static TypeDependentElementSet of (Set<IJavaElement> unmappedElements,
			Map<IJavaElement, Set<IJavaElement>> _dependents, Map<IJavaElement, Set<IJavaElement>> _dependencies) {
		
		Map<IJavaElement, Set<IJavaElement>> dependents = new LinkedHashMap<>(_dependents);
		dependents.keySet().retainAll(unmappedElements);
		Map<IJavaElement, Set<IJavaElement>> dependencies = new LinkedHashMap<>(_dependencies);
		dependencies.keySet().retainAll(unmappedElements);
		
		if (dependents.size()!= dependencies.size()) throw new IllegalArgumentException("Dependencies and dependents don't match.");
		
		Map<IJavaElement, Map<Relationship,Set<IJavaElement>>> mappedElements = new LinkedHashMap<>();
		
		unmappedElements.forEach(element -> {
			Map<Relationship,Set<IJavaElement>> relations = new LinkedHashMap<>();
			relations.put(Relationship.DEPENDENTS, dependents.get(element));
			relations.put(Relationship.DEPENDENCIES, dependencies.get(element));
			mappedElements.put(element, relations);
		});
		
		return new TypeDependentElementSet(mappedElements);
	}
	
	private TypeDependentElementSet(Map<IJavaElement, Map<Relationship, Set<IJavaElement>>> mappedElements) {
		this.elements = mappedElements;
	}

	public Set<IJavaElement> getDependents(IJavaElement element) {
		return this.elements.get(element).get(Relationship.DEPENDENTS);
	}
	
	public Set<IJavaElement> getDependencies(IJavaElement element) {
		return this.elements.get(element).get(Relationship.DEPENDENCIES);
	}

	@Override
	public boolean contains(Object o) {
		return (this.elements.keySet().contains(o));
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.elements.keySet().containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return this.elements.keySet().isEmpty();
	}

	@Override
	public Iterator<IJavaElement> iterator() {
		return this.elements.keySet().iterator();
	}

	@Override
	public int size() {
		return this.elements.keySet().size();
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
