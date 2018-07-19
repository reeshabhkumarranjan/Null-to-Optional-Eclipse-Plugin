package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;

public class TypeDependentElementTree implements Set<IJavaElement> {

	private final Set<IJavaElement> elements;	
	private final Map<IJavaElement, Set<IJavaElement>> dependents, dependencies;
	
	public static TypeDependentElementTree of (Set<IJavaElement> elements,
			Map<IJavaElement, Set<IJavaElement>> dependents, Map<IJavaElement, Set<IJavaElement>> dependencies) {
		return new TypeDependentElementTree(elements, dependents, dependencies);
	}
	
	private TypeDependentElementTree(Set<IJavaElement> elements,
			Map<IJavaElement, Set<IJavaElement>> dependents, Map<IJavaElement, Set<IJavaElement>> dependencies) {
		this.elements = elements;
		this.dependencies = dependencies;
		this.dependents = dependents;
	}

	public Set<IJavaElement> getDependents(IJavaElement element) {
		return this.dependents.get(element);
	}
	
	public Set<IJavaElement> getDependency(IJavaElement element) {
		return this.dependencies.get(element);
	}
	
	@Override
	public boolean addAll(Collection<? extends IJavaElement> c) {
		if (c instanceof TypeDependentElementTree) {
			this.merge((TypeDependentElementTree)c);
			return true;
		}
		else return false;
	}

	private void merge(TypeDependentElementTree c) {
		this.dependencies.putAll(c.dependencies);
		this.dependents.putAll(c.dependents);
		this.elements.addAll(c.elements);		
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
