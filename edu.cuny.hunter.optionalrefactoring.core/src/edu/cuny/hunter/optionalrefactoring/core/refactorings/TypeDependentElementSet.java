package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;

public class TypeDependentElementSet implements Set<IJavaElement> {
	
	private static enum Relations {
		DEPENDENTS,
		DEPENDENCIES;
	}

	private final Map<IJavaElement, Map<Relations, Set<IJavaElement>>> elements;
	
	public static TypeDependentElementSet of (Set<IJavaElement> unmappedElements,
			Map<IJavaElement, Set<IJavaElement>> _dependents, Map<IJavaElement, Set<IJavaElement>> _dependencies) {
		
		Map<IJavaElement, Set<IJavaElement>> dependents = new LinkedHashMap<>(_dependents);
		dependents.keySet().retainAll(unmappedElements);
		Map<IJavaElement, Set<IJavaElement>> dependencies = new LinkedHashMap<>(_dependencies);
		dependencies.keySet().retainAll(unmappedElements);
		
		if (dependents.size()!= dependencies.size()) throw new IllegalArgumentException("Dependencies and dependents don't match.");
		
		Map<IJavaElement, Map<Relations,Set<IJavaElement>>> mappedElements = new LinkedHashMap<>();
		
		unmappedElements.forEach(element -> {
			Map<Relations,Set<IJavaElement>> relations = new LinkedHashMap<>();
			relations.put(Relations.DEPENDENTS, dependents.get(element));
			relations.put(Relations.DEPENDENCIES, dependencies.get(element));
			mappedElements.put(element, relations);
		});
		
		return new TypeDependentElementSet(mappedElements);
	}
	
	private TypeDependentElementSet(Map<IJavaElement, Map<Relations, Set<IJavaElement>>> mappedElements) {
		this.elements = mappedElements;
	}

	public Set<IJavaElement> getDependents(IJavaElement element) {
		return this.elements.get(element).get(Relations.DEPENDENTS);
	}
	
	public Set<IJavaElement> getDependencies(IJavaElement element) {
		return this.elements.get(element).get(Relations.DEPENDENCIES);
	}
	
	@Override
	public boolean addAll(Collection<? extends IJavaElement> c) {
		if (c instanceof TypeDependentElementSet) {
			this.merge((TypeDependentElementSet)c);
			return true;
		}
		else throw new IllegalArgumentException("Tried to add a collection that is not a TypeDependentElementSet.");
	}

	private void merge(TypeDependentElementSet c) {
		c.forEach(element -> this.elements.putIfAbsent(element, c.elements.get(element)));
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
