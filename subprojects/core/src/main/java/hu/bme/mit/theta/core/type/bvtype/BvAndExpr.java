package hu.bme.mit.theta.core.type.bvtype;

import hu.bme.mit.theta.core.model.Valuation;
import hu.bme.mit.theta.core.type.Expr;
import hu.bme.mit.theta.core.type.MultiaryExpr;
import hu.bme.mit.theta.core.utils.TypeUtils;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static hu.bme.mit.theta.core.utils.TypeUtils.*;

public class BvAndExpr extends MultiaryExpr<BvType, BvType> {
    private static final int HASH_SEED = 9125;
    private static final String OPERATOR_LABEL = "&";

    private BvAndExpr(final Iterable<? extends Expr<BvType>> ops) {
        super(ops);
        checkAllTypesEqual(ops);
    }

    public static BvAndExpr of(final Iterable<? extends Expr<BvType>> ops) {
        return new BvAndExpr(ops);
    }

    public static BvAndExpr create(final List<? extends Expr<?>> ops) {
        checkNotNull(ops);
        return BvAndExpr.of(ops.stream().map(TypeUtils::castBv).collect(toImmutableList()));
    }

    @Override
    public BvType getType() {
        return getOps().get(0).getType();
    }

    @Override
    public BvLitExpr eval(final Valuation val) {
        return getOps().stream().skip(1).reduce(
            (BvLitExpr) getOps().get(0).eval(val),
            (op1, op2) -> (op1.and((BvLitExpr) op2.eval(val))),
            BvLitExpr::and
        );
    }

    @Override
    public BvAndExpr with(final Iterable<? extends Expr<BvType>> ops) {
        if (ops == getOps()) {
            return this;
        } else {
            return BvAndExpr.of(ops);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof BvAndExpr) {
            final BvAndExpr that = (BvAndExpr) obj;
            return this.getOps().equals(that.getOps());
        } else {
            return false;
        }
    }

    @Override
    protected int getHashSeed() {
        return HASH_SEED;
    }

    @Override
    public String getOperatorLabel() {
        return OPERATOR_LABEL;
    }
}
