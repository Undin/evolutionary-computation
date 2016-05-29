package com.warrior.evolutionary.computation.lab4

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.warrior.evolutionary.computation.lab4.Expr.Binary
import com.warrior.evolutionary.computation.lab4.Expr.Terminal.Const
import com.warrior.evolutionary.computation.lab4.Expr.Terminal.Variable
import com.warrior.evolutionary.computation.lab4.Expr.Unary
import com.warrior.evolutionary.computation.lab4.Expr.Terminal
import java.util.*

/**
 * Created by warrior on 14/05/16.
 */
sealed class Expr(val size: Int, val depth: Int) {

    companion object {
        private val mapper: ObjectMapper = ObjectMapper()
    }

    sealed class Binary(val lhs: Expr, val rhs: Expr) : Expr(lhs.size + rhs.size + 1, Math.max(lhs.depth, rhs.depth) + 1) {
        class Add(lhs: Expr, rhs: Expr) : Binary(lhs, rhs) {
            override fun invoke(values: DoubleArray): Double = lhs(values) + rhs(values)
        }

        class Sub(lhs: Expr, rhs: Expr) : Binary(lhs, rhs) {
            override fun invoke(values: DoubleArray): Double = lhs(values) - rhs(values)
        }

        class Mul(lhs: Expr, rhs: Expr) : Binary(lhs, rhs) {
            override fun invoke(values: DoubleArray): Double = lhs(values) * rhs(values)
        }

        class Div(lhs: Expr, rhs: Expr) : Binary(lhs, rhs) {
            override fun invoke(values: DoubleArray): Double = lhs(values) / rhs(values)
        }

        class Pow(lhs: Expr, rhs: Expr) : Binary(lhs, rhs) {
            override fun invoke(values: DoubleArray): Double = Math.pow(lhs(values), rhs(values))
        }

        override fun invoke(): Expr {
            return if (lhs is Const && rhs is Const) {
                val v = when (this) {
                    is Add -> lhs.value + rhs.value
                    is Sub -> lhs.value - rhs.value
                    is Mul -> lhs.value * rhs.value
                    is Div -> lhs.value / rhs.value
                    is Pow -> Math.pow(lhs.value, rhs.value)
                }
                Const(v)
            } else {
                this
            }
        }

        override fun doMinimize(): Expr {
            val lhs = lhs.minimize()
            val rhs = rhs.minimize()
            val isLhsZero = lhs is Const && lhs.value == 0.0
            val isLhsOne = lhs is Const && lhs.value == 1.0
            val isRhsZero = rhs is Const && rhs.value == 0.0
            val isRhsOne = rhs is Const && rhs.value == 1.0
            val isSameVariable = lhs is Variable && rhs is Variable && lhs.index == rhs.index
            return when (this) {
                is Add -> {
                    if (isLhsZero) {
                        rhs
                    } else if (isRhsZero) {
                        lhs
                    } else {
                        Add(lhs, rhs)
                    }
                }
                is Sub -> {
                    if (isRhsZero) {
                        lhs
                    } else if (isSameVariable) {
                        Const.ZERO
                    } else {
                        Sub(lhs, rhs)
                    }
                }
                is Mul -> {
                    if (isLhsZero || isRhsZero) {
                        Const.ZERO
                    } else if (isLhsOne) {
                        rhs
                    } else if (isRhsOne) {
                        lhs
                    } else {
                        Mul(lhs, rhs)
                    }
                }
                is Div -> {
                    if (isLhsZero) {
                        Const.ZERO
                    } else if (isRhsOne) {
                        lhs
                    } else {
                        Div(lhs, rhs)
                    }
                }
                is Pow -> {
                    if (isLhsZero) {
                        Const.ZERO
                    } else if (isLhsOne || isRhsZero) {
                        Const.ONE
                    } else if (isRhsOne) {
                        lhs
                    } else {
                        Pow(lhs, rhs)
                    }
                }
            }
        }
        
        fun replaceChildren(l: Expr, r: Expr): Binary = when (this) {
            is Add -> Add(l, r)
            is Sub -> Sub(l, r)
            is Mul -> Mul(l, r)
            is Div -> Div(l, r)
            is Pow -> Pow(l, r)
        }

        override fun toJson(): JsonNode {
            val type = when (this) {
                is Add -> "add"
                is Sub -> "sub"
                is Mul -> "mul"
                is Div -> "div"
                is Pow -> "pow"
            }
            val node =  mapper.createObjectNode()
                    .put("type", type)
            node.set("lhs", lhs.toJson())
            node.set("rhs", rhs.toJson())
            return node
        }

        override fun toString(): String = when (this) {
            is Add -> "($lhs) + ($rhs)"
            is Sub -> "($lhs) - ($rhs)"
            is Mul -> "($lhs) * ($rhs)"
            is Div -> "($lhs) / ($rhs)"
            is Pow -> "($lhs) ^ ($rhs)"
        }
    }

    sealed class Unary(val expr: Expr) : Expr(expr.size + 1, expr.depth + 1) {
        class UnaryMinus(expr: Expr) : Unary(expr) {
            override fun invoke(values: DoubleArray): Double = -expr(values)
        }

        class Abs(expr: Expr) : Unary(expr) {
            override fun invoke(values: DoubleArray): Double = Math.abs(expr(values))
        }

        class Sin(expr: Expr) : Unary(expr) {
            override fun invoke(values: DoubleArray): Double = Math.sin(expr(values))
        }

        class Cos(expr: Expr) : Unary(expr) {
            override fun invoke(values: DoubleArray): Double = Math.cos(expr(values))
        }

        class Exp(expr: Expr) : Unary(expr) {
            override fun invoke(values: DoubleArray): Double = Math.exp(expr(values))
        }

        override fun invoke(): Expr {
            return if (expr is Const) {
                val v = when (this) {
                    is UnaryMinus -> -expr.value
                    is Abs -> Math.abs(expr.value)
                    is Sin -> Math.sin(expr.value)
                    is Cos -> Math.cos(expr.value)
                    is Exp -> Math.exp(expr.value)
                }
                Const(v)
            } else {
                this
            }
        }

        override fun doMinimize(): Expr {
            val childExpr = expr.minimize()
            return when (this) {
                is UnaryMinus -> if (childExpr is UnaryMinus) { childExpr } else { UnaryMinus(childExpr) }
                is Abs -> if (childExpr is Abs) { childExpr } else { Abs(childExpr) }
                is Sin -> if (childExpr is UnaryMinus) { UnaryMinus(Sin(childExpr.expr)) } else { Sin(childExpr) }
                is Cos -> {
                    var e = childExpr
                    if (childExpr is UnaryMinus) {
                        e = childExpr.expr
                    } else if (childExpr is Abs) {
                        e = childExpr.expr
                    }
                    Cos(e)
                }
                is Exp -> Exp(childExpr)
            }
        }
        
        fun replaceChild(e: Expr): Unary = when (this) {
            is UnaryMinus -> UnaryMinus(e)
            is Abs -> Abs(e)
            is Sin -> Sin(e)
            is Cos -> Cos(e)
            is Exp -> Exp(e)
        }

        override fun toJson(): JsonNode {
            val type = when (this) {
                is UnaryMinus -> "unaryMinus"
                is Abs -> "abs"
                is Sin -> "sin"
                is Cos -> "cos"
                is Exp -> "exp"
            }
            return mapper.createObjectNode()
                    .put("type", type)
                    .set("expr", expr.toJson())
        }

        override fun toString(): String {
            return when (this) {
                is UnaryMinus -> "-($expr)"
                is Abs -> "abs($expr)"
                is Sin -> "sin($expr)"
                is Cos -> "cos($expr)"
                is Exp -> "exp($expr)"
            }
        }
    }

    sealed class Terminal() : Expr(1, 1) {
        class Const(val value: Double) : Terminal() {
            companion object {
                val ZERO = Const(0.0)
                val ONE = Const(1.0)
            }
            override fun invoke(values: DoubleArray): Double = value
            override fun toJson(): JsonNode = mapper.createObjectNode()
                    .put("type", "const")
                    .put("value", value)
        }

        class Variable(val index: Int) : Terminal() {
            override fun invoke(values: DoubleArray): Double = values[index]
            override fun toJson(): JsonNode = mapper.createObjectNode()
                    .put("type", "var")
                    .put("index", index)
        }

        override fun invoke(): Expr = this
        override fun doMinimize(): Expr = this

        override fun toString(): String = when (this) {
            is Const -> value.toString()
            is Variable -> "x[$index]"
        }
    }

    fun minimize(): Expr {
        val expr = invoke()
        return expr.doMinimize()
    }

    fun toJsonString(): String = mapper.writeValueAsString(toJson())

    abstract operator fun invoke(values: DoubleArray): Double
    abstract fun toJson(): JsonNode

    protected abstract operator fun invoke(): Expr
    protected abstract fun doMinimize(): Expr
}

data class ExprInfo(val expr: Expr, val index: Int, val depth: Int)

fun Expr.allSubExprs(): List<ExprInfo> {
    fun Expr.subExprs(out: MutableList<ExprInfo>, startIndex: Int, currentDepth: Int) {
        when (this) {
            is Terminal -> out.add(ExprInfo(this, startIndex, currentDepth))
            is Unary -> {
                out.add(ExprInfo(this, startIndex + expr.size, currentDepth))
                expr.subExprs(out, startIndex, currentDepth + 1)
            }
            is Binary -> {
                out.add(ExprInfo(this, startIndex + lhs.size, currentDepth))
                lhs.subExprs(out, startIndex, currentDepth + 1)
                rhs.subExprs(out, startIndex + lhs.size + 1, currentDepth + 1)
            }
        }
    }

    val out = ArrayList<ExprInfo>(size)
    subExprs(out, 0, 0)
    return out
}

fun Expr.replaceSubtree(index: Int, other: Expr): Expr = when (this) {
    is Expr.Terminal -> {
        if (index != 0) {
            throw IllegalStateException()
        }
        other
    }
    is Expr.Unary -> {
        if (index == expr.size) {
            other
        } else {
            val newExpr = expr.replaceSubtree(index, other)
            replaceChild(newExpr)
        }
    }
    is Expr.Binary -> {
        if (index == lhs.size) {
            other
        } else {
            var newLhs = lhs
            var newRhs = rhs
            if (index < lhs.size) {
                newLhs = lhs.replaceSubtree(index, other)
            } else {
                newRhs = rhs.replaceSubtree(index - lhs.size - 1, other)
            }
            replaceChildren(newLhs, newRhs)
        }
    }
}

fun Expr.replaceTerminalNode(index: Int, gen: () -> Terminal): Expr = when (this) {
    is Expr.Terminal -> gen()
    is Expr.Unary -> {
        if (expr.size == index) {
            throw IllegalStateException()
        } else {
            replaceChild(expr.replaceTerminalNode(index, gen))
        }
    }
    is Expr.Binary -> {
        if (index == lhs.size) {
            throw IllegalStateException()
        }
        if (index < lhs.size) {
            replaceChildren(lhs.replaceTerminalNode(index, gen), rhs)
        } else {
            replaceChildren(lhs, rhs.replaceTerminalNode(index - lhs.size - 1, gen))
        }
    }
}

fun Expr.replaceUnaryNode(index: Int, gen: (e: Expr) -> Unary): Expr = when (this) {
    is Expr.Terminal -> throw IllegalStateException()
    is Expr.Unary -> {
        if (expr.size == index) {
            gen(expr)
        } else {
            replaceChild(expr.replaceUnaryNode(index, gen))
        }
    }
    is Expr.Binary -> {
        if (index == lhs.size) {
            throw IllegalStateException()
        }
        if (index < lhs.size) {
            replaceChildren(lhs.replaceUnaryNode(index, gen), rhs)
        } else {
            replaceChildren(lhs, rhs.replaceUnaryNode(index - lhs.size - 1, gen))
        }
    }
}

fun Expr.replaceBinaryNode(index: Int, gen: (l: Expr, r: Expr) -> Binary): Expr {
    try {
        return when (this) {
            is Expr.Terminal -> throw IllegalStateException()
            is Expr.Unary -> {
                if (expr.size == index) {
                    throw IllegalStateException()
                } else {
                    replaceChild(expr.replaceBinaryNode(index, gen))
                }
            }
            is Expr.Binary -> {
                if (index == lhs.size) {
                    gen(lhs, rhs)
                } else if (index < lhs.size) {
                    replaceChildren(lhs.replaceBinaryNode(index, gen), rhs)
                } else {
                    replaceChildren(lhs, rhs.replaceBinaryNode(index - lhs.size - 1, gen))
                }
            }
        }
    } catch (e: Exception) {
        println(this)
        throw e
    }
}

fun JsonNode.toExpr(): Expr {
    if (!isObject) {
        throw IllegalStateException()
    }
    val type = get("type")?.asText() ?: throw IllegalStateException("type is not specified")
    return when (type) {
        "const" -> {
            val value = get("value")?.asDouble() ?: throw IllegalStateException("value is not specified")
            Const(value)
        }
        "var" -> {
            val index = get("index")?.asInt() ?: throw IllegalStateException("index is not specified")
            Variable(index)
        }
        "unaryMinus" -> {
            val expr = get("expr") ?: throw IllegalStateException("expr is not specified")
            Unary.UnaryMinus(expr.toExpr())
        }
        "abs" -> {
            val expr = get("expr") ?: throw IllegalStateException("expr is not specified")
            Unary.Abs(expr.toExpr())
        }
        "sin" -> {
            val expr = get("expr") ?: throw IllegalStateException("expr is not specified")
            Unary.Sin(expr.toExpr())
        }
        "cos" -> {
            val expr = get("expr") ?: throw IllegalStateException("expr is not specified")
            Unary.Cos(expr.toExpr())
        }
        "exp" -> {
            val expr = get("expr") ?: throw IllegalStateException("expr is not specified")
            Unary.Exp(expr.toExpr())
        }
        "add" -> {
            val lhs = get("lhs") ?: throw IllegalStateException("lhs is not specified")
            val rhs = get("rhs") ?: throw IllegalStateException("rhs is not specified")
            Binary.Add(lhs.toExpr(), rhs.toExpr())
        }
        "sub" -> {
            val lhs = get("lhs") ?: throw IllegalStateException("lhs is not specified")
            val rhs = get("rhs") ?: throw IllegalStateException("rhs is not specified")
            Binary.Sub(lhs.toExpr(), rhs.toExpr())
        }
        "mul" -> {
            val lhs = get("lhs") ?: throw IllegalStateException("lhs is not specified")
            val rhs = get("rhs") ?: throw IllegalStateException("rhs is not specified")
            Binary.Mul(lhs.toExpr(), rhs.toExpr())
        }
        "div" -> {
            val lhs = get("lhs") ?: throw IllegalStateException("lhs is not specified")
            val rhs = get("rhs") ?: throw IllegalStateException("rhs is not specified")
            Binary.Div(lhs.toExpr(), rhs.toExpr())
        }
        "pow" -> {
            val lhs = get("lhs") ?: throw IllegalStateException("lhs is not specified")
            val rhs = get("rhs") ?: throw IllegalStateException("rhs is not specified")
            Binary.Pow(lhs.toExpr(), rhs.toExpr())
        }
        else -> throw IllegalStateException("unreachable state")
    }
}
