package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import com.google.common.collect.Streams;

import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

public class Entities implements Collection<Entry<IJavaElement, Set<Instance<? extends ASTNode>>>> {

	public static Entities create(
			final Set<IJavaElement> elements,
			final Set<Instance<? extends ASTNode>> instances, 
			final RefactoringSettings settings
	) {
		final Map<IJavaElement, Set<Instance<? extends ASTNode>>> mappedInstances = elements
				.stream()
				.collect(Collectors.toMap(
						element -> element,
						element -> instances
							.stream()
							.filter(instance -> elements.contains(instance.element()))
							.filter(instance -> instance.element().equals(element)).collect(Collectors.toSet()),
						(left, right) -> Streams.concat(left.stream(), right.stream()).collect(Collectors.toSet())));
		RefactoringStatus status = mappedInstances.values().stream().flatMap(Set::stream)
				.flatMap(instance -> instance.failures().stream()
						.map(failure -> Util.createStatusEntry(settings, 
								failure, instance.element(), instance.node(), instance.action(), false)))
				.collect(RefactoringStatus::new, RefactoringStatus::addEntry, RefactoringStatus::merge);
		
		return new Entities(status, mappedInstances);
	}

	private final Map<IJavaElement, Set<Instance<? extends ASTNode>>> elementToInstancesMap;

	private final RefactoringStatus status;
	
	private final Map<CompilationUnit, Set<IJavaElement>> cuMap = new LinkedHashMap<>();

	private Entities(final RefactoringStatus status, final Map<IJavaElement, Set<Instance<? extends ASTNode>>> mappedInstances) {
		this.status = status;
		this.elementToInstancesMap = mappedInstances;
	}

	@Override
	public String toString() {
		return this.elementToInstancesMap.entrySet().stream().map(Entry::getKey).map(IJavaElement::getElementName)
			.collect(Collectors.joining(", ", "{", "}"));
	}

	@Override
	public Iterator<Entry<IJavaElement, Set<Instance<? extends ASTNode>>>> iterator() {
		return this.elementToInstancesMap.entrySet().iterator();
	}

	public RefactoringStatus status() {
		return this.status;
	}
	
	public void transform() {
		for (final CompilationUnit cu : this.cuMap.keySet()) {
			final Set<IJavaElement> elements = this.cuMap.get(cu);
			final N2ONodeTransformer n2ont = new N2ONodeTransformer(cu, elements, this.elementToInstancesMap);
			n2ont.process();
		}
	}

	public void addIcu(CompilationUnit cu, IJavaElement element) {
		if (this.cuMap.containsKey(cu))
			this.cuMap.get(cu).add(element);
		else
			this.cuMap.put(cu, Util.setOf(element));	
	}

	@Override
	public boolean add(Entry<IJavaElement, Set<Instance<? extends ASTNode>>> e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Entry<IJavaElement, Set<Instance<? extends ASTNode>>>> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub	
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
		return this.size() == 0;
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
		return this.elementToInstancesMap.size();
	}

	@Override
	public Object[] toArray() {
		return this.elementToInstancesMap.entrySet().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.elementToInstancesMap.entrySet().toArray(a);
	}
}
