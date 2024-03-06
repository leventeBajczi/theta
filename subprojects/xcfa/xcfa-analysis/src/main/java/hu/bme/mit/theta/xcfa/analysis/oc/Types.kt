package hu.bme.mit.theta.xcfa.analysis.oc

import hu.bme.mit.theta.core.decl.*
import hu.bme.mit.theta.core.model.Valuation
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.LitExpr
import hu.bme.mit.theta.core.type.NullaryExpr
import hu.bme.mit.theta.core.type.Type
import hu.bme.mit.theta.core.type.anytype.RefExpr
import hu.bme.mit.theta.core.type.booltype.BoolExprs.And
import hu.bme.mit.theta.core.type.booltype.BoolExprs.Bool
import hu.bme.mit.theta.core.type.booltype.BoolLitExpr
import hu.bme.mit.theta.core.type.booltype.BoolType
import hu.bme.mit.theta.xcfa.model.XcfaEdge
import hu.bme.mit.theta.xcfa.model.XcfaLocation
import hu.bme.mit.theta.xcfa.model.XcfaProcedure

internal object OcType : Type

internal object OcLitExpr : LitExpr<OcType>, NullaryExpr<OcType>() {

    override fun getType() = OcType
    override fun eval(`val`: Valuation?) = error("This expression is not meant to be evaluated.")
}

internal enum class EventType { WRITE, READ }
internal data class Event(
    val const: IndexedConstDecl<*>,
    val type: EventType,
    val guard: List<Expr<BoolType>>,
    val pid: Int,
    val edge: XcfaEdge,
) {

    val clk: RefExpr<OcType> = RefExpr.of(Decls.Const("${const.name}\$clk", OcType))
    var assignment: Expr<BoolType>? = null

    fun enabled(valuation: Valuation): Boolean = try {
        And(guard).eval(valuation).value
    } catch (e: Exception) {
        false
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Event) return false
        return const == other.const && type == other.type
    }

    override fun hashCode(): Int {
        var result = const.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

internal enum class RelationType { PO, EPO, COI, COE, RFI, RFE }
internal data class Relation(
    val type: RelationType,
    val from: Event,
    val to: Event,
) : Expr<BoolType> {

    val decl: ConstDecl<BoolType> =
        Decls.Const("${type.toString().lowercase()}_${from.const.name}_${to.const.name}", Bool())
    val declRef: RefExpr<BoolType> = RefExpr.of(decl)

    override fun toString() = "Relation($type, ${from.const.name}[${from.type.toString()[0]}], ${to.const.name}[${to.type.toString()[0]}])"
    override fun getType(): BoolType = Bool()
    override fun getArity() = 2
    override fun getOps(): List<Expr<*>> = listOf(from.clk, to.clk)
    override fun eval(v: Valuation) = error("This expression is not meant to be evaluated.")
    override fun withOps(ops: List<Expr<*>>) = error("This expression is not meant to be modified.")
    fun enabled(valuation: Map<Decl<*>, LitExpr<*>>) = value(valuation) ?: false
    fun value(valuation: Map<Decl<*>, LitExpr<*>>): Boolean? =
        if (type == RelationType.PO || type == RelationType.EPO) true
        else valuation[decl]?.let { (it as BoolLitExpr).value }
}

internal data class Violation(
    val errorLoc: XcfaLocation,
    val guard: Expr<BoolType>,
    val lastEvents: List<Event>,
)

internal data class Mutex(
    val mutex: String,
    var entered: Boolean = false,
)

internal data class Thread(
    val procedure: XcfaProcedure,
    val guard: List<Expr<BoolType>> = listOf(),
    val mutex: Mutex? = null,
    val pidVar: VarDecl<*>? = null,
    val startEvent: Event? = null,
    val joinEvents: MutableSet<Event> = mutableSetOf(),
    val pid: Int = uniqueId(),
) {

    companion object {

        private var cnt: Int = 0
        fun uniqueId(): Int = cnt++
    }
}

internal data class SearchItem(val loc: XcfaLocation) {

    val guards: MutableList<List<Expr<BoolType>>> = mutableListOf()
    val mutexes: MutableList<Mutex?> = mutableListOf()
    val lastEvents: MutableList<Event> = mutableListOf()
    val lastWrites: MutableList<Map<VarDecl<*>, Set<Event>>> = mutableListOf()
    val pidLookups: MutableList<Map<VarDecl<*>, Set<Pair<List<Expr<BoolType>>, Int>>>> = mutableListOf()
    val incoming: MutableSet<XcfaEdge> = mutableSetOf()
}

internal data class StackItem(val event: Event) {

    var eventsToVisit: MutableList<Event>? = null
}