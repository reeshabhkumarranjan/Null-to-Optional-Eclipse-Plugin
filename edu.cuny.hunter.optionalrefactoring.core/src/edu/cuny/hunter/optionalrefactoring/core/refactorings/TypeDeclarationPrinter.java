package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class TypeDeclarationPrinter extends ASTVisitor {

	@Override
	public void endVisit(TypeDeclaration node) {
		System.out.println(node.getName());
		super.endVisit(node);
	}
	
	

}
