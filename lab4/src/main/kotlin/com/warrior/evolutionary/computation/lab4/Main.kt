package com.warrior.evolutionary.computation.lab4

import com.github.jabbalaci.graphviz.GraphViz
import com.warrior.evolutionary.computation.core.DoubleRange
import com.warrior.evolutionary.computation.core.PrecisionPredicate
import com.warrior.evolutionary.computation.lab4.Expr.Binary
import com.warrior.evolutionary.computation.lab4.Expr.Binary.*
import com.warrior.evolutionary.computation.lab4.Expr.Terminal.Const
import com.warrior.evolutionary.computation.lab4.Expr.Terminal.Variable
import com.warrior.evolutionary.computation.lab4.Expr.Unary
import com.warrior.evolutionary.computation.lab4.Expr.Unary.*
import com.warrior.evolutionary.computation.lab4.GeneticAlgorithm.Fn
import org.math.plot.Plot2DPanel
import java.awt.Color
import java.awt.Rectangle
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.lang.Math.*
import javax.swing.JFrame

/**
 * Created by warrior on 20/05/16.
 */

const val VARIABLE_NUMBER = 5
const val STEP_NUMBER = 5

fun main(args: Array<String>) {

    val f = F11(VARIABLE_NUMBER)

    val algorithm = GeneticAlgorithm(f) { e -> error(e, f) }
    algorithm.maxDepth = 13
    val results = algorithm.search(600, PrecisionPredicate(0.0, 0.0001, 500))
    val expr = results.last().chromosome
    println(expr.toJsonString())
    println("avg error: " + Math.sqrt(results.last().value / Math.pow(STEP_NUMBER.toDouble(), VARIABLE_NUMBER.toDouble())))
}

fun drawExprs(results: List<Expr>) {
    val outFolder = File("out")
    outFolder.mkdir()

    for (i in 0..results.size step 10) {
        toGraph(results[i], File(outFolder, "result$i.png"))
    }
}

fun buildPlot(title: String, fn: (Double) -> Double, realFn: (Double) -> Double, range: DoubleRange) {
    val points = 1000
    val x = DoubleArray(points) { range.start +  (range.endInclusive - range.start) / (points - 1) * it }
    val realY = DoubleArray(points) { realFn(x[it]) }
    val y = DoubleArray(points) { fn(x[it]) }

    val plot = Plot2DPanel();
    plot.addLinePlot("real fn", Color.BLACK, x, realY)
    plot.addLinePlot("created fn", Color.RED, x, y)

    val frame = JFrame(title);
    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosed(e: WindowEvent?) {
            System.exit(0)
        }
    })
    frame.bounds = Rectangle(0, 0, 1200, 600)
    frame.contentPane = plot;
    frame.isVisible = true;
}

private fun drawRealFn() {
    for (i in 0..4) {
        toGraph(gen(i), File("lab4/logs/real_f$i.png"))
    }
    toGraph(Add(gen(0), Add(gen(1), Add(gen(2), Add(gen(3), gen(4))))), File("lab4/logs/real_f.png"))
}

private fun toGraph(e: Expr, out: File) {

    fun Expr.toGraph(gv: GraphViz, startIndex: Int) {
        val nodeName = name(startIndex)
        when (this) {
            is Unary -> {
                gv.addln("$nodeName -> ${expr.name(startIndex)}")
                expr.toGraph(gv, startIndex)
            }
            is Binary -> {
                gv.addln("$nodeName -> ${lhs.name(startIndex)}")
                gv.addln("$nodeName -> ${rhs.name(startIndex + lhs.size + 1)}")
                lhs.toGraph(gv, startIndex)
                rhs.toGraph(gv, startIndex + lhs.size + 1)
            }
        }
    }

    val gv = GraphViz()
    gv.addln(gv.start_graph())
    e.toGraph(gv, 0)
    gv.addln(gv.end_graph())
    gv.writeGraphToFile(gv.getGraph(gv.dotSource, "png", "dot"), out)
}

private fun Expr.index(startIndex: Int = 0): Int {
    return when (this) {
        is Expr.Binary -> startIndex + lhs.size
        is Expr.Unary -> startIndex + expr.size
        is Expr.Terminal -> startIndex
    }
}

private fun Expr.name(startIndex: Int): String {
    val name = when (this) {
        is Add -> "+"
        is Sub -> "-"
        is Mul -> "*"
        is Div -> "/"
        is Pow -> "^"
        is UnaryMinus -> "-"
        is Sin -> "sin"
        is Cos -> "cos"
        is Exp -> "exp"
        is Abs -> "abs"
        is Const -> value.toString()
        is Variable -> "x${index + 1}"
        else -> throw IllegalStateException()
    }
    return "\"$name (${index(startIndex)})\""
}



private fun error(e: Expr, realFn: Fn): Double {
    val num = realFn.variableNumber()
    val steps = DoubleArray(num) { i ->
        val range = realFn.range(i)
        (range.endInclusive - range.start) / (STEP_NUMBER - 1)
    }

    var sum = 0.0
    for (i in 0 until pow(STEP_NUMBER.toDouble(), num.toDouble()).toInt()) {
        var v = i
        val xs = DoubleArray(num)
        for (j in 0 until num) {
            xs[j] = realFn.range(j).start + (v % STEP_NUMBER) * steps[j]
            v /= STEP_NUMBER
        }
        sum += pow(abs(e(xs) - realFn(xs)), 2.0)
    }
    if (!sum.isFinite()) {
        return Double.POSITIVE_INFINITY
    }
    return sum
}

private fun errorWithPoints(e: Expr, realFn: Fn, points: List<DoubleArray>): Double {
    val sum = points.map { xs -> pow(abs(e(xs) - realFn(xs)), 2.0) }.sum()
    return if (sum.isFinite()) { sum } else { Double.POSITIVE_INFINITY }
}

private fun gen(i: Int): Expr {
    return UnaryMinus(Mul(Sin(Variable(i)), Pow(Sin(Div(Mul(Const(i + 1.0), Mul(Variable(i), Variable(i))), Const(PI))), Const(20.0))))
}

class F1(val variableNumber: Int) : Fn {
    override fun invoke(xs: DoubleArray): Double = xs.map { it * it }.sum()
    override fun variableNumber(): Int = variableNumber
    override fun range(i: Int): DoubleRange = DoubleRange(-5.12, 5.12)
    override fun consts(): List<Const> = listOf(Const(2.0))
    override fun unaryGenerators(): List<(Expr) -> Unary> = emptyList()
    override fun binaryGenerators(): List<(Expr, Expr) -> Binary> = listOf(::Add, ::Mul, ::Pow)
}

class F11(val variableNumber: Int) : Fn {

    override fun invoke(xs: DoubleArray): Double = xs.withIndex()
            .map {
                val (i, x) = it
                -sin(x) * pow(sin((i + 5) * x * x / PI), 20.0)
            }
            .sum()

    override fun variableNumber(): Int = variableNumber
    override fun range(i: Int): DoubleRange = DoubleRange(0.0, PI)
    override fun consts(): List<Const> = listOf(Const(20.0), Const(1 / PI), Const(1.0), Const(2.0), Const(3.0), Const(4.0), Const(5.0))
    override fun unaryGenerators(): List<(Expr) -> Unary> = listOf(::UnaryMinus, ::Sin)
    override fun binaryGenerators(): List<(Expr, Expr) -> Binary> = listOf(::Add, ::Sub, ::Mul, ::Pow)
}

object f12 : Fn {

    private val a = 1.0
    private val b = 5.1 / (4 * PI * PI)
    private val c = 5 / PI
    private val d = 6.0
    private val e = 10.0
    private val f = 1 / (8 * PI)

    override fun invoke(xs: DoubleArray): Double {
        return a * pow((xs[1] - b * pow(xs[0], 2.0) + c * xs[0] - d), 2.0) + e * (1 - f) * cos(xs[0]) + e;
    }

    override fun variableNumber(): Int = 2

    override fun range(i: Int): DoubleRange {
        return if (i == 0) {
            DoubleRange(-5.0, 10.0)
        } else {
            DoubleRange(0.0, 15.0)
        }
    }

    override fun consts(): List<Const> = listOf(Const(a), Const(b), Const(c), Const(d), Const(e), Const(f), Const(2.0))
    override fun unaryGenerators(): List<(Expr) -> Unary> = listOf(::Cos)
    override fun binaryGenerators(): List<(Expr, Expr) -> Binary> = listOf(::Add, ::Sub, ::Mul, ::Pow)
}

object f13 : Fn {
    override fun invoke(xs: DoubleArray): Double {
        return -cos(xs[0]) * cos(xs[1]) * exp(-pow(xs[0] - PI, 2.0) * pow(xs[1] - PI, 2.0))
    }

    override fun variableNumber(): Int = 2
    override fun range(i: Int): DoubleRange = DoubleRange(-100.0, 100.0)
    override fun consts(): List<Const> = listOf(Const(2.0), Const(PI))
    override fun unaryGenerators(): List<(Expr) -> Unary> = listOf(::Cos, ::Exp, ::UnaryMinus)
    override fun binaryGenerators(): List<(Expr, Expr) -> Binary> = listOf(::Add, ::Sub, ::Mul, ::Pow)
}

object f14 : Fn {
    override fun invoke(xs: DoubleArray): Double {
        return (1 + pow((xs[0] + xs[1] + 1), 2.0) * (19 -
                14 * xs[0] + 3 * pow(xs[0], 2.0) -
                14 * xs[1] + 6 * xs[0] * xs[1] + 3 * pow(xs[1], 2.0))) *
                (30 + pow((2 * xs[0] - 3 * xs[1]), 2.0) * (18 - 32 * xs[0] + 12 * pow(xs[0], 2.0) + 48 * xs[1] -
                36 * xs[0] * xs[1] + 27 * pow(xs[1], 2.0)))
    }

    override fun variableNumber(): Int = 2
    override fun range(i: Int): DoubleRange = DoubleRange(-2.0, 2.0)
    override fun consts(): List<Const> = listOf(Const(1.0), Const(2.0), Const(3.0), Const(6.0), Const(14.0), Const(18.0),
            Const(19.0), Const(27.0), Const(30.0), Const(32.0), Const(36.0), Const(48.0))
    override fun unaryGenerators(): List<(Expr) -> Unary> = listOf(::UnaryMinus)
    override fun binaryGenerators(): List<(Expr, Expr) -> Binary> = listOf(::Add, ::Sub, ::Mul, ::Pow)
}
