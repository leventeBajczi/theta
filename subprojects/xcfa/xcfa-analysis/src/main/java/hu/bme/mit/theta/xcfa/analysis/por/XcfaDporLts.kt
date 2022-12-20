package hu.bme.mit.theta.xcfa.analysis.por

import hu.bme.mit.theta.analysis.LTS
import hu.bme.mit.theta.analysis.algorithm.ArgNode
import hu.bme.mit.theta.analysis.waitlist.Waitlist
import hu.bme.mit.theta.xcfa.analysis.XcfaAction
import hu.bme.mit.theta.xcfa.analysis.XcfaState
import hu.bme.mit.theta.xcfa.analysis.getXcfaLts
import hu.bme.mit.theta.xcfa.model.XCFA
import java.util.*
import java.util.stream.Stream
import kotlin.NoSuchElementException
import kotlin.collections.LinkedHashMap
import kotlin.math.max
import kotlin.random.Random

private typealias S = XcfaState<*>
private typealias A = XcfaAction

class XcfaDporLts(
    private val xcfa: XCFA
) : LTS<S, A> {

    private val random = Random.Default // or use Random(seed) with a seed

    val waitlist = object : Waitlist<ArgNode<S, A>> {

        private fun max(map1: Map<Int, Int>, map2: Map<Int, Int>) =
            (map1.keys union map2.keys).associateWith { key -> max(map1[key] ?: -1, map2[key] ?: -1) }

        override fun add(item: ArgNode<S, A>) {
            val newaction = item.inEdge.get().action
            val process = newaction.pid
            val newProcessLastAction = LinkedHashMap(last.processLastAction).apply { this[process] = stack.size - 1 }
            var newLastDependents: Map<Int, Int> = LinkedHashMap(last.lastDependents[process])
            val relevantProcesses = (newProcessLastAction.keys - setOf(process)).toMutableSet()

            for (index in stack.size - 1 downTo 1) {
                if (relevantProcesses.isEmpty()) break
                val node = stack[index].node
                val action = node.inEdge.get().action
                if (relevantProcesses.contains(action.pid)) {
                    if (index <= (newLastDependents[action.pid] ?: -1)) {
                        // there is an action a' such that action -> a' -> newaction (->: happens-before)
                        relevantProcesses.remove(action.pid)
                    } else if (dependent(newaction, action)) {
                        // reversible race
                        stack[index - 1].backtrack!!.add(TODO("add an initial of 'v'"))
                        newLastDependents = max(newLastDependents, stack[index].lastDependents[action.pid]!!)
                        relevantProcesses.remove(action.pid)
                    }
                }
            }

            stack.push(
                StackItem(
                    node = item,
                    processLastAction = newProcessLastAction,
                    lastDependents = last.lastDependents.toMutableMap().apply { this[process] = newLastDependents }
                )
            )
        }

        override fun addAll(items: Collection<ArgNode<S, A>>) {
            assert(items.size == 1) // TODO <=
            add(items.first())
        }

        override fun addAll(items: Stream<out ArgNode<S, A>>) {
            val iterator = items.iterator()
            add(iterator.next()) // TODO if (iterator.hasNext())
            assert(!iterator.hasNext())
        }

        override fun isEmpty() = stack.isEmpty()

        override fun remove(): ArgNode<S, A> {
            if (isEmpty) throw NoSuchElementException("The search stack is empty.")
            return stack.peek().node
        }

        override fun size() = stack.count { it.backtrack == null || it.backtrack!!.isNotEmpty() }

        override fun clear() = stack.clear()
    }

    private data class StackItem(
        val node: ArgNode<S, A>,
        var backtrack: MutableSet<A>? = null,
        val processLastAction: Map<Int, Int>,
        val lastDependents: Map<Int, Map<Int, Int>>,
        var detectedDisabledRaces: Boolean = false,
    )

    private val simpleXcfaLts = getXcfaLts()

    private val stack: Stack<StackItem> = Stack()

    private val last get() = stack.peek()

    private fun getAllEnabledActionsFor(state: S): Set<A> = simpleXcfaLts.getEnabledActionsFor(state)

    override fun getEnabledActionsFor(state: S): Set<A> {
        assert(state == last.node.state)

        val enabledActions = getAllEnabledActionsFor(state)
        val enabledProcesses = enabledActions.map { it.pid }.toSet()

        if (!last.detectedDisabledRaces && state.processes.size != enabledProcesses.size) {
            TODO("disabled race detection")
            last.detectedDisabledRaces = true
        }

        if (enabledProcesses.isEmpty()) {
            do stack.pop() while (stack.isNotEmpty() && last.backtrack!!.isEmpty())
            return emptySet()
        }

        if (last.backtrack == null) {
            val randomProcess = enabledProcesses.random(random)
            last.backtrack = enabledActions.filter { it.pid == randomProcess }.toMutableSet()
        }

        val actionToExplore = last.backtrack!!.random()
        last.backtrack!!.remove(actionToExplore)

        return setOf(actionToExplore)
    }

    fun dependent(a: A, b: A): Boolean {
        TODO("implement dependency condition")
    }
}