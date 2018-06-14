package edu.cuny.hunter.optionalrefactoring.core.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.IBinding;

public class BindingSet<B extends IBinding> implements Set<IBinding> {

	private final Map<Class, Set<Object>> map = new HashMap<>();
	
	public BindingSet(int x, int y) {
		this(x);
	}
	
	public BindingSet(int x) {
		super();
	}

	{
		Object o = null;
		Object k;
		k = null;
		add(null);
	}
	
	@Override
	public boolean add(IBinding b) {
		if (map.containsKey(b.getClass())) {
			map.get(b.getClass()).add(b);
			return true;
		}
		else {
			Set<IBinding> s = new HashSet<>();
			s.add(b);
			map.put(b.getClass(), (Set<Object>) b);
			return false;
		}
		
	}

	@Override
	public boolean addAll(Collection<? extends IBinding> c) {
		// TODO Auto-generated method stub                        
		return false;
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

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
	public int size() {
		// TODO Auto-generated method stub
		return 0;
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

}
