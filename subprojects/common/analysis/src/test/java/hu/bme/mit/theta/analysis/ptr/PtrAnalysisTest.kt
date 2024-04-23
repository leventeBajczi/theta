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

package hu.bme.mit.theta.analysis.ptr

import hu.bme.mit.theta.analysis.expl.ExplAnalysis
import hu.bme.mit.theta.analysis.expl.ExplPrec
import hu.bme.mit.theta.analysis.expl.ExplState
import hu.bme.mit.theta.core.decl.Decls.Var
import hu.bme.mit.theta.core.stmt.Stmts.Assume
import hu.bme.mit.theta.core.stmt.Stmts.MemoryAssign
import hu.bme.mit.theta.core.type.anytype.Exprs.Dereference
import hu.bme.mit.theta.core.type.booltype.BoolExprs.True
import hu.bme.mit.theta.core.type.inttype.IntExprs.Eq
import hu.bme.mit.theta.core.type.inttype.IntExprs.Int
import hu.bme.mit.theta.solver.z3legacy.Z3LegacySolverFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class PtrAnalysisTest {

    companion object {
        private val x = Var("x", Int())

        private val explTop = PtrState(ExplState.top())

        private val emptyAction = PtrAction(listOf(), emptyMap())
        private val readLiteralOnly = PtrAction(listOf(Assume(Eq(Dereference(Int(0), Int(1), Int()), Int(0)))), emptyMap())
        private val writeLiteralOnly = PtrAction(listOf(MemoryAssign(Dereference(Int(0), Int(1), Int()), Int(0))), emptyMap())

        private val emptyPrec = PtrPrec(ExplPrec.empty())

        @JvmStatic
        fun testInputs(): Collection<Arguments> {
            return listOf(
                Arguments.of(explTop, emptyAction, emptyPrec,
                    listOf(explTop)),
                Arguments.of(explTop, readLiteralOnly, emptyPrec,
                    listOf(explTop)),
                Arguments.of(explTop, writeLiteralOnly, emptyPrec,
                    listOf(PtrState(ExplState.top(), writeLiteralOnly.getNextWriteTriples()))),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("testInputs")
    fun transFuncTest(state: PtrState<ExplState>, action: PtrAction, prec: PtrPrec<ExplPrec>, expectedResult: Collection<PtrState<ExplState>>) {
        val analysis =
            PtrAnalysis(ExplAnalysis.create(Z3LegacySolverFactory.getInstance().createSolver(), True()))
        val result = analysis.transFunc.getSuccStates(state, action, prec)
        Assertions.assertEquals(expectedResult.toSet(), result.toSet())
    }
}