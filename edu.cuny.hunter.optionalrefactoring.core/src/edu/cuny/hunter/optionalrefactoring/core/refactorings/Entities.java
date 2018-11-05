package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.google.common.collect.Streams;

import edu.cuny.hunter.optionalrefactoring.core.analysis.Action;
import edu.cuny.hunter.optionalrefactoring.core.analysis.PreconditionFailure;
import edu.cuny.hunter.optionalrefactoring.core.analysis.RefactoringSettings;
import edu.cuny.hunter.optionalrefactoring.core.utils.Util;

@SuppressWarnings("restriction")
public class Entities implements Iterable<IJavaElement> {

	public static class Instance {
		final IJavaElement element;
		final ASTNode node;
		final EnumSet<PreconditionFailure> failures;
		final Action action;

		public Instance(final IJavaElement e, final ASTNode n, final EnumSet<PreconditionFailure> pf, final Action a) {
			this.element = e;
			this.node = n;
			this.failures = pf;
			this.action = a;
		}
		
		@Override
		public boolean equals(Object _other) {
			Instance other = (Instance)_other;
			return this.element.equals(other.element) &&
					Util.getSourceRange(this.node).equals(Util.getSourceRange(other.node)) &&
					this.failures.equals(other.failures) &&
					this.action.equals(other.action);
		}
	}

	public static Entities create(final Set<IJavaElement> elements, final Set<Instance> instances, 
			final RefactoringSettings settings) {
		final Map<IJavaElement, Set<Instance>> mappedInstances = elements.stream()
				.collect(Collectors.toMap(element -> element,
						element -> instances.stream().filter(instance -> elements.contains(instance.element))
								.filter(instance -> instance.element.equals(element)).collect(Collectors.toSet()),
						(left, right) -> Streams.concat(left.stream(), right.stream()).collect(Collectors.toSet())));
		RefactoringStatus status = instances.stream()
				.filter(instance -> elements.contains(instance.element))
				.flatMap(instance -> instance.failures.stream().map(failure -> Util.createStatusEntry(settings, failure, instance.element, instance.node, instance.action)))
				.collect(RefactoringStatus::new, RefactoringStatus::addEntry, RefactoringStatus::merge);
		return new Entities(status, elements, mappedInstances);
	}

	private final Set<IJavaElement> elements;

	private final Map<IJavaElement, Set<Instance>> instances;

	private final RefactoringStatus status;
	
	private final Map<ICompilationUnit, Set<IJavaElement>> icuMap = new LinkedHashMap<>();

	private final Map<CompilationUnitRewrite, Set<IJavaElement>> rewriteMap = new LinkedHashMap<>();

	private Entities(final RefactoringStatus status, final Set<IJavaElement> elements,
			final Map<IJavaElement, Set<Instance>> mappedInstances) {
		this.status = status;
		this.elements = elements;
		this.instances = mappedInstances;
	}

	@Override
	public String toString() {
		return this.elements.stream().map(IJavaElement::getElementName)
			.collect(Collectors.joining(", ", "{", "}"));
	}
	
	public void addRewrite(final CompilationUnitRewrite rewrite, final IJavaElement element) {
		if (this.rewriteMap.containsKey(rewrite))
			this.rewriteMap.get(rewrite).add(element);
		else
			this.rewriteMap.put(rewrite, Util.setOf(element));
	}

	public Set<IJavaElement> elements() {
		return this.elements;
	}

	@Override
	public Iterator<IJavaElement> iterator() {
		return this.elements.iterator();
	}

	public RefactoringStatus status() {
		return this.status;
	}
	
	public void transform() throws CoreException {
		for (final ICompilationUnit icu : this.icuMap.keySet()) {
			final CompilationUnit cu = Util.getCompilationUnit(icu, new NullProgressMonitor());
			Document doc = new Document(icu.getSource());
			Set<IJavaElement> elements = this.icuMap.get(icu);
			N2ONodeTransformer n2ont = new N2ONodeTransformer(cu, elements, this.instances);
			CompilationUnit tcu = (CompilationUnit)n2ont.process();
			ImportDeclaration id = tcu.getAST().newImportDeclaration();
			id.setName(tcu.getAST().newName(new String[] { "java", "util", "Optional" }));
			cu.imports().add(id);
			TextEdit te = tcu.rewrite(doc, icu.getJavaProject().getOptions(true));
			try {
				te.apply(doc);
			} catch (MalformedTreeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Util.LOGGER.info(doc.get());
		}
	}

	public void addIcu(ICompilationUnit icu, IJavaElement element) {
		if (this.icuMap.containsKey(icu))
			this.icuMap.get(icu).add(element);
		else
			this.icuMap.put(icu, Util.setOf(element));	}
}
