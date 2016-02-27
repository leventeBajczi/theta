package hu.bme.mit.inf.ttmc.program.expr;

import hu.bme.mit.inf.ttmc.constraint.expr.RefExpr;
import hu.bme.mit.inf.ttmc.constraint.type.Type;
import hu.bme.mit.inf.ttmc.constraint.utils.ExprVisitor;
import hu.bme.mit.inf.ttmc.program.decl.VarDecl;
import hu.bme.mit.inf.ttmc.program.utils.ProgExprVisitor;

public interface VarRefExpr<DeclType extends Type> extends RefExpr<DeclType, VarDecl<DeclType>> {
	
	@Override
	public default <P, R> R accept(ExprVisitor<? super P, ? extends R> visitor, P param) {
		if (visitor instanceof ProgExprVisitor<?, ?>) {
			final ProgExprVisitor<? super P, ? extends R> sVisitor = (ProgExprVisitor<? super P, ? extends R>) visitor;
			return sVisitor.visit(this, param);
		} else {
			throw new UnsupportedOperationException();
		}
	}
}
