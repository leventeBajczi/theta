package hu.bme.mit.theta.analysis.expl;

import hu.bme.mit.theta.analysis.Lattice;
import hu.bme.mit.theta.analysis.PartialOrd;
import hu.bme.mit.theta.core.decl.Decl;
import hu.bme.mit.theta.core.model.ImmutableValuation;
import hu.bme.mit.theta.core.type.LitExpr;

import java.util.Map;

public final class ExplLattice implements Lattice<ExplState> {

    private final PartialOrd<ExplState> partialOrd;

    private static final ExplLattice INSTANCE = new ExplLattice();

    private ExplLattice() {
        partialOrd = ExplOrd.getInstance();
    }

    public static ExplLattice getInstance() {
        return INSTANCE;
    }

    @Override
    public ExplState top() {
        return ExplState.top();
    }

    @Override
    public ExplState bottom() {
        return ExplState.bottom();
    }

    @Override
    public ExplState meet(final ExplState state1, final ExplState state2) {
        ImmutableValuation.Builder valBuilder = ImmutableValuation.builder();
        final Map<Decl<?>, LitExpr<?>> declToExpr1 = state1.toMap();
        final Map<Decl<?>, LitExpr<?>> declToExpr2 = state2.toMap();

        declToExpr1.forEach(valBuilder::put);

        for(Decl<?> decl : declToExpr2.keySet()){
            LitExpr<?> value = declToExpr2.get(decl);
            if(!declToExpr1.containsKey(decl)){
                valBuilder.put(decl, value);
            } else if(!declToExpr1.get(decl).equals(value)){
                return ExplState.bottom();
            }
        }
        return ExplState.of(valBuilder.build());
    }

    @Override
    public ExplState join(final ExplState state1, final ExplState state2) {
        ImmutableValuation.Builder valBuilder = ImmutableValuation.builder();
        final Map<Decl<?>, LitExpr<?>> declToExpr1 = state1.toMap();
        final Map<Decl<?>, LitExpr<?>> declToExpr2 = state2.toMap();

        for(Decl<?> decl : declToExpr1.keySet()){
            LitExpr<?> value = declToExpr1.get(decl);
            if(declToExpr2.containsKey(decl) && declToExpr2.get(decl).equals(value)){
                valBuilder.put(decl, value);
            }
        }
        return ExplState.of(valBuilder.build());
    }

    @Override
    public boolean isLeq(final ExplState state1, final ExplState state2) {
        return partialOrd.isLeq(state1, state2);
    }
}
