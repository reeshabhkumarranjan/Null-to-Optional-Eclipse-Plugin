package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import com.google.common.collect.Streams;

import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

@SuppressWarnings("restriction")
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

		RefactoringStatus status = instances
				.stream()
				.filter(instance -> elements.contains(instance.element()))
				.flatMap(instance -> instance.failures()
						.stream()
						.map(failure -> Util.createStatusEntry(
								settings, 
								failure,
								instance.element(),
								instance.node(),
								instance.action())))
				.collect(RefactoringStatus::new, RefactoringStatus::addEntry, RefactoringStatus::merge);
		
		return new Entities(status, mappedInstances);
	}

	private final Map<IJavaElement, Set<Instance<? extends ASTNode>>> elementToInstancesMap;

	private final RefactoringStatus status;
	
	private final Map<ICompilationUnit, Set<IJavaElement>> icuMap = new LinkedHashMap<>();

	private final Map<ICompilationUnit, CompilationUnitRewrite> rewrites = new LinkedHashMap<>();

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
	
	public void transform() throws CoreException {
		IProgressMonitor monitor = new NullProgressMonitor();
		for (final ICompilationUnit icu : this.icuMap.keySet()) {
			final CompilationUnit cu = Util.getCompilationUnit(icu, monitor);
			final Set<IJavaElement> elements = this.icuMap.get(icu);
			final N2ONodeTransformer n2ont = new N2ONodeTransformer(icu, cu, elements, this.elementToInstancesMap);
			final Document doc = (Document) n2ont.process();
			String name = icu.getElementName();
			icu.rename("old_"+name, true, monitor);
			ICompilationUnit rwIcu = ((IPackageFragment)icu.getParent())
					.createCompilationUnit(name, doc.get(), true, monitor);
			CompilationUnitRewrite cur = new CompilationUnitRewrite(rwIcu);
			final ImportRewrite ir = cur.getImportRewrite();
			ir.addImport("java.util.Optional");
			this.rewrites.put(rwIcu, cur);
		}
	}

	public void addIcu(ICompilationUnit icu, IJavaElement element) {
		if (this.icuMap.containsKey(icu))
			this.icuMap.get(icu).add(element);
		else
			this.icuMap.put(icu, Util.setOf(element));	}

	public Map<ICompilationUnit, CompilationUnitRewrite> getRewrites() {
		return this.rewrites;
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
