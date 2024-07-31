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

package hu.bme.mit.theta.xsts.analysis.util

import hu.bme.mit.theta.core.Relation
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibSymbolTable
import hu.bme.mit.theta.solver.smtlib.impl.generic.GenericSmtLibTransformationManager

fun List<Relation>.toSMT2(): String {
    val symbolTable = GenericSmtLibSymbolTable()
    val transformationManager = GenericSmtLibTransformationManager(symbolTable)
    val terms = flatMap { it.rules.map { "(assert " + transformationManager.toTerm(it.toExpr()) + ")" } }
    val decls = filter { symbolTable.definesConst(it.constDecl) }.map { symbolTable.getDeclaration(it.constDecl) }

    return """
; generated by Theta
; https://github.com/ftsrg/theta/

(set-logic HORN)

; declarations
${decls.joinToString("\n")}

; facts, rules, queries
${terms.joinToString("\n")}

(check-sat)
(exit)
""".trimIndent()
}

fun Relation.toSMT2() = listOf(this).toSMT2()