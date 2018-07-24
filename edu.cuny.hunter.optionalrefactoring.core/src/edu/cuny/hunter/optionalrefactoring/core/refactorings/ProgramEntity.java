package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.IBinding;

/**
 * @author <a href="mailto:ofriedman@acm.org">Oren Friedman</a>
 *
 */
public class ProgramEntity {

	private final IJavaElement element;
	private final IBinding binding;
	private final boolean seed;
	private final boolean implicit;
	
	public ProgramEntity(IJavaElement element, IBinding binding, 
			boolean isSeed, boolean implicit) {
		this.element = element;
		this.binding = binding;
		this.seed = isSeed;
		this.implicit = implicit;
	}
	
	public ProgramEntity(IJavaElement element, IBinding binding) {
		this.element = element;
		this.binding = binding;
		this.seed = false;
		this.implicit = false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return this.element().equals(obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.element.getElementName();
	}

	public IJavaElement element() {
		return this.element;
	}
	
	public IBinding binding() {
		return this.binding;
	}
	
	public boolean isSeed() {
		return this.seed;
	}
	
	public boolean implicitlyNull() {
		return this.implicit;
	}

}
