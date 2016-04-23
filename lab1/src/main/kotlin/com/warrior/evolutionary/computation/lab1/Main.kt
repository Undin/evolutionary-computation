package com.warrior.evolutionary.computation.lab1

import com.warrior.evolutionary.computation.core.IterationPredicate
import org.math.plot.Plot2DPanel
import java.awt.Color
import java.awt.Rectangle
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.lang.Math.cos
import java.lang.Math.exp
import javax.swing.JFrame

/**
 * Created by warrior on 11.04.16.
 */

const val LEFT = 0.5
const val RIGHT = 10.0
val FN: (Double) -> Double = { x -> cos(x) / (1 + exp(-x)) }
const val MAX_X = 6.28505
const val MAX_Y = 0.998128

fun main(args: Array<String>) {

    val points = 1000
    val x = DoubleArray(points) { LEFT + (RIGHT - LEFT) / points * it }
    val y = DoubleArray(points) { FN(x[it]) }

    val plot = Plot2DPanel();
    plot.addLinePlot("function", Color.BLACK, x, y)

    val frame = JFrame();
    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosed(e: WindowEvent?) {
            System.exit(0)
        }
    })
    frame.bounds = Rectangle(0, 0, 1200, 600)
    frame.contentPane = plot;
    frame.isVisible = true;

    val algorithm = GeneticAlgorithm(FN, LEFT, RIGHT)
    val pointList = algorithm.search(40, IterationPredicate(20))
    println("result: ${pointList.last().first.last()}, ${pointList.last().second.last()}")
    println("real: $MAX_X, $MAX_Y")

    val keyListener = KeyListener(plot)
    plot.addKeyListener(keyListener)
    plot.requestFocus()
    keyListener.points = pointList
}

class KeyListener(val plot: Plot2DPanel) : KeyAdapter() {

    private var plotIndex: Int = -1
    private var index: Int = 0

    var points: List<Pair<DoubleArray, DoubleArray>>? = null
        set(value) {
            field = value
            if (plotIndex != -1) {
                plot.removePlot(plotIndex)
            }

            index = 0
            if (value != null) {
                plotIndex = plot.addScatterPlot("population", Color.RED, value[index].first, value[index].second)
            }
        }

    override fun keyPressed(e: KeyEvent) {
        val p = points
        if (p != null && plotIndex != -1) {
            if (e.keyCode == KeyEvent.VK_LEFT && index > 0) {
                index--
                plot.removePlot(plotIndex)
                plotIndex = plot.addScatterPlot("population", Color.RED, p[index].first, p[index].second)
            }
            if (e.keyCode == KeyEvent.VK_RIGHT && index < p.lastIndex) {
                index++
                plot.removePlot(plotIndex)
                plotIndex = plot.addScatterPlot("population", Color.RED, p[index].first, p[index].second)
            }
        }
    }
}
