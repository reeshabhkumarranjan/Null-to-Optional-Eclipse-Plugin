package edu.cuny.hunter.optionalrefactoring.core.refactorings;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class TypeDeclarationPrinter extends ASTVisitor {

	@Override
	public boolean visit(TypeDeclaration node) {
		System.out.println(node.getName());
		return super.visit(node);
	}
	
	

}
