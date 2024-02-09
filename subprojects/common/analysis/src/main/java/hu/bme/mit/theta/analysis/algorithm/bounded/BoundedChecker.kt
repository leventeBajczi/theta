/*
 *  Copyright 2024 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hu.bme.mit.theta.analysis.algorithm.bounded

import hu.bme.mit.theta.analysis.Trace
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker
import hu.bme.mit.theta.analysis.algorithm.SafetyResult
import hu.bme.mit.theta.analysis.expr.ExprState
import hu.bme.mit.theta.analysis.expr.StmtAction
import hu.bme.mit.theta.analysis.unit.UnitPrec
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.core.model.Valuation
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.abstracttype.AbstractExprs.Eq
import hu.bme.mit.theta.core.type.booltype.BoolExprs.*
import hu.bme.mit.theta.core.type.booltype.BoolType
import hu.bme.mit.theta.core.utils.PathUtils
import hu.bme.mit.theta.core.utils.indexings.VarIndexing
import hu.bme.mit.theta.core.utils.indexings.VarIndexingFactory
import hu.bme.mit.theta.solver.ItpSolver
import hu.bme.mit.theta.solver.Solver
import java.util.*

class BoundedChecker<S : ExprState, A : StmtAction> @JvmOverloads constructor(
    private val monolithicExpr: MonolithicExpr,
    private val shouldGiveUp: (Int) -> Boolean = { false },
    private val bmcSolver: Solver? = null,
    private val bmcEnabled: (Int) -> Boolean = { true },
    private val lfPathOnly: () -> Boolean = { true },
    private val itpSolver: ItpSolver? = null,
    private val imcEnabled: (Int) -> Boolean = { true },
    private val indSolver: Solver? = null,
    private val kindEnabled: (Int) -> Boolean = { true },
    private val valToState: (Valuation) -> S,
    private val biValToAction: (Valuation, Valuation) -> A,
    private val logger: Logger,
) : SafetyChecker<S, A, UnitPrec> {

    private val vars = monolithicExpr.vars()
    private val unfoldedInitExpr = PathUtils.unfold(monolithicExpr.initExpr, 0)
    private val unfoldedPropExpr = { i: VarIndexing -> PathUtils.unfold(monolithicExpr.propExpr, i) }
    private val indices = mutableListOf(VarIndexingFactory.indexing(0))
    private val exprs = mutableListOf<Expr<BoolType>>()
    private var lastIterLookup = Triple(-1, -1, -1)

    init {
        check(bmcSolver != itpSolver || bmcSolver == null) { "Use distinct solvers for BMC and IMC!" }
        check(bmcSolver != indSolver || bmcSolver == null) { "Use distinct solvers for BMC and KInd!" }
        check(itpSolver != indSolver || itpSolver == null) { "Use distinct solvers for IMC and KInd!" }
    }

    override fun check(prec: UnitPrec?): SafetyResult<S, A> {
        var iteration = 0;

        bmcSolver?.add(unfoldedInitExpr)

        while (!shouldGiveUp(iteration)) {
            iteration++
            logger.write(Logger.Level.MAINSTEP, "Starting iteration $iteration\n")

            exprs.add(PathUtils.unfold(monolithicExpr.transExpr, indices.last()))

            indices.add(indices.last().add(monolithicExpr.offsetIndex))

            if (bmcEnabled(iteration)) {
                bmc()?.let { return it }
                lastIterLookup = lastIterLookup.copy(first = iteration)
            }

            if (kindEnabled(iteration)) {
                if (!bmcEnabled(iteration)) {
                    error("Bad configuration: induction check should always be preceded by a BMC/SAT check")
                }
                kind()?.let { return it }
                lastIterLookup = lastIterLookup.copy(second = iteration)
            }

            if (imcEnabled(iteration)) {
                itp()?.let { return it }
                lastIterLookup = lastIterLookup.copy(third = iteration)
            }
        }
        return SafetyResult.unknown() as SafetyResult<S, A>
    }

    private fun bmc(): SafetyResult<S, A>? {
        val bmcSolver = this.bmcSolver!!
        logger.write(Logger.Level.MAINSTEP, "\tStarting BMC\n")

        exprs.subList(lastIterLookup.first + 1, exprs.size).forEach { bmcSolver.add(it) }

        if (lfPathOnly()) { // indices contains currIndex as last()
            for (indexing in indices) {
                if (indexing != indices.last()) {
                    val allVarsSame = And(vars.map {
                        Eq(PathUtils.unfold(it.ref, indexing), PathUtils.unfold(it.ref, indices.last()))
                    })
                    bmcSolver.add(Not(allVarsSame))
                }
            }

            if (bmcSolver.check().isUnsat) {
                bmcSolver.pop()
                logger.write(Logger.Level.MAINSTEP, "Safety proven in BMC step\n")
                return SafetyResult.safe<S, A>()
            }
        }

        bmcSolver.push()
        bmcSolver.add(Not(unfoldedPropExpr(indices.last())))

        val ret = if (bmcSolver.check().isSat) {
            val trace = getTrace(bmcSolver.model)
            logger.write(Logger.Level.MAINSTEP, "CeX found in BMC step (length ${trace.length()})\n")
            SafetyResult.unsafe(trace)
        } else null

        bmcSolver.pop()
        return ret
    }

    private fun kind(): SafetyResult<S, A>? {
        val indSolver = this.indSolver!!

        logger.write(Logger.Level.MAINSTEP, "\tStarting k-induction\n")

        exprs.subList(lastIterLookup.first + 1, exprs.size).forEach { indSolver.add(it) }

        indSolver.push()
        indSolver.add(Not(unfoldedPropExpr(indices.last())))

        val ret = if (indSolver.check().isUnsat) {
            logger.write(Logger.Level.MAINSTEP, "Safety proven in k-induction step\n")
            SafetyResult.safe<S, A>()
        } else null

        indSolver.pop()
        return ret
    }

    private fun itp(): SafetyResult<S, A>? {
        val itpSolver = this.itpSolver!!
        logger.write(Logger.Level.MAINSTEP, "\tStarting IMC\n")

        itpSolver.push()

        val a = itpSolver.createMarker()
        val b = itpSolver.createMarker()
        val pattern = itpSolver.createBinPattern(a, b)

        itpSolver.push()

        itpSolver.add(a, unfoldedInitExpr)
        itpSolver.add(a, exprs[0])
        itpSolver.add(b, exprs.subList(1, exprs.size))

        if (lfPathOnly()) { // indices contains currIndex as last()
            itpSolver.push()
            for (indexing in indices) {
                if (indexing != indices.last()) {
                    val allVarsSame = And(vars.map {
                        Eq(PathUtils.unfold(it.ref, indexing), PathUtils.unfold(it.ref, indices.last()))
                    })
                    itpSolver.add(a, Not(allVarsSame))
                }
            }

            if (itpSolver.check().isUnsat) {
                itpSolver.pop()
                itpSolver.pop()
                logger.write(Logger.Level.MAINSTEP, "Safety proven in IMC/BMC step\n")
                return SafetyResult.safe()
            }
            itpSolver.pop()
        }

        itpSolver.add(b, Not(unfoldedPropExpr(indices.last())))

        val status = itpSolver.check()

        if (status.isSat) {
            val trace = getTrace(itpSolver.model)
            logger.write(Logger.Level.MAINSTEP, "CeX found in IMC/BMC step (length ${trace.length()})\n")
            itpSolver.pop()
            itpSolver.pop()
            return SafetyResult.unsafe(trace)
        }

        var img = unfoldedInitExpr
        while (itpSolver.check().isUnsat) {
            val interpolant = itpSolver.getInterpolant(pattern)
            val itpFormula = PathUtils.unfold(PathUtils.foldin(interpolant.eval(a), indices[1]), indices[0])
            itpSolver.pop()

            itpSolver.push()
            itpSolver.add(a, itpFormula)
            itpSolver.add(a, Not(img))
            val itpStatus = itpSolver.check()
            if (itpStatus.isUnsat) {
                logger.write(Logger.Level.MAINSTEP, "Safety proven in IMC step\n")
                itpSolver.pop()
                itpSolver.pop()
                return SafetyResult.safe()
            }
            itpSolver.pop()
            img = Or(img, itpFormula)

            itpSolver.push()
            itpSolver.add(a, itpFormula)
            itpSolver.add(a, exprs[0])
            itpSolver.add(b, exprs.subList(1, exprs.size))
            itpSolver.add(b, Not(unfoldedPropExpr(indices.last())))
        }

        itpSolver.pop()
        itpSolver.pop()
        return null
    }


    private fun getTrace(model: Valuation): Trace<S, A> {
        val stateList = LinkedList<S>()
        val actionList = LinkedList<A>()
        var lastValuation: Valuation? = null
        for (i in indices) {
            val valuation = PathUtils.extractValuation(model, i, vars)
            stateList.add(valToState(valuation))
            if (lastValuation != null) {
                actionList.add(biValToAction(lastValuation, valuation))
            }
            lastValuation = valuation
        }
        return Trace.of(stateList, actionList)
    }

}