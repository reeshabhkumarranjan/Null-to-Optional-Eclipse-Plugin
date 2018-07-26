package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;

public class WorkList extends LinkedHashSet<IJavaElement> implements Iterable<IJavaElement> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5357933777896982471L;
	private static boolean isValidTypeSignature(String sig) {
		if (Signature.getTypeSignatureKind(sig) == Signature.BASE_TYPE_SIGNATURE)
			return true;
		else if (Signature.getTypeSignatureKind(sig) == Signature.ARRAY_TYPE_SIGNATURE)
			return isValidTypeSignature(Signature.getElementType(sig));
		else
			return false;
	}

	private final Set<ComputationNode> computationForest = new LinkedHashSet<>();

	private ValuedComputationNode currentNode;

	private final Map<IJavaElement, ValuedComputationNode> elemToNode = new HashMap<>();

	public boolean add(IJavaElement element) {
//		try {
//			this.sanityCheck(element);
//		} catch (final JavaModelException e) {
//			throw new RuntimeException(e);
//		}

		final ValuedComputationNode elemNode = this.elemToNode.get(element);

		if (elemNode != null) // its been seen before.
		{
			// get the roots.
			final ComputationNode elemNodeRoot = elemNode.getRoot();
			final ComputationNode currNodeRoot = this.currentNode.getRoot();

			if (elemNodeRoot != currNodeRoot) {
				// union the trees.
				final ComputationNode unionNode = this.union(elemNodeRoot, currNodeRoot);

				// remove the old trees from the forest.
				this.computationForest.remove(elemNodeRoot);
				this.computationForest.remove(currNodeRoot);

				// add the new tree to the forest.
				this.computationForest.add(unionNode);
			}

			return false;
		}

		else // it has not been seen before.
		{
			final ValuedComputationNode node = new ValuedComputationNode(element);
			this.elemToNode.put(element, node);
			if (this.currentNode == null)
				// seed the comp forest.
				this.computationForest.add(node);
			else
				// attach the new node.
				this.currentNode.makeParent(node);
			return super.add(element);
		}
	}

	public boolean addAll(Set<IJavaElement> c) {
		boolean changed = false;
		for (IJavaElement element : c) {
			changed |= this.add(element);
		}
		return changed;
	}

	public Set<ComputationNode> getComputationForest() {
		return this.computationForest;
	}

	public Set<IJavaElement> getCurrentComputationTreeElements() {
		// find the tree in the forest that contains the current node.
		final ComputationNode root = this.currentNode.getRoot();
		return root.getComputationTreeElements();
	}

	public ValuedComputationNode getCurrentNode() {
		return this.currentNode;
	}

	public Set<IJavaElement> getSeen() {
		return this.elemToNode.keySet();
	}

	public boolean hasNext() {
		return !this.isEmpty();
	}

	public IJavaElement next() {
		final Iterator<IJavaElement> it = this.iterator();
		final IJavaElement ret = it.next();
		this.currentNode = (ValuedComputationNode) this.elemToNode.get(ret);
		it.remove();
		return ret;
	}

	public void remove() {
		this.iterator().remove();
	}

//	private void sanityCheck(Object e) throws JavaModelException {
//		final IJavaElement o = (IJavaElement) e;
//		if (o.isReadOnly())
//			throw new IllegalArgumentException(Messages.Worklist_IllegalWorklistElement + o);
//
//		switch (o.getElementType()) {
//		case IJavaElement.LOCAL_VARIABLE: {
//			final ILocalVariable lv = (ILocalVariable) o;
//			final String sig = lv.getTypeSignature();
//			if (!isValidTypeSignature(sig))
////				throw new IllegalArgumentException(Messages.Worklist_IllegalWorklistElement
////						+ o);
////			break;
////		}
//
//		case IJavaElement.FIELD: {
//			final IField f = (IField) o;
//			final String sig = f.getTypeSignature();
//			if (!isValidTypeSignature(sig))
//				throw new IllegalArgumentException(Messages.Worklist_IllegalWorklistElement
//						+ o);
//			break;
//		}
//
//		case IJavaElement.METHOD: {
//			final IMethod m = (IMethod) o;
//			final String retType = m.getReturnType();
//			if (!isValidTypeSignature(retType))
//				throw new IllegalArgumentException(Messages.Worklist_IllegalWorklistElement
//						+ o);
//
//			break;
//		}
//
//		default: {
//			throw new IllegalArgumentException(Messages.Worklist_IllegalWorklistElement + o);
//		}
//		}
//	}

	private ComputationNode union(ComputationNode root1, ComputationNode root2) {
		final ComputationNode ret = new UnionComputationNode();
		ret.makeParent(root1);
		ret.makeParent(root2);
		return ret;
	}
	
}
