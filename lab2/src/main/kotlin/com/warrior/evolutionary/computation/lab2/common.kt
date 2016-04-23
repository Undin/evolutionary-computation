package com.warrior.evolutionary.computation.lab2

import org.jzy3d.analysis.AbstractAnalysis
import org.jzy3d.chart.Chart
import org.jzy3d.chart.controllers.keyboard.camera.AWTCameraKeyController
import org.jzy3d.colors.Color
import org.jzy3d.colors.ColorMapper
import org.jzy3d.colors.colormaps.ColorMapRainbow
import org.jzy3d.maths.Coord3d
import org.jzy3d.maths.Range
import org.jzy3d.plot3d.builder.Builder
import org.jzy3d.plot3d.builder.Mapper
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid
import org.jzy3d.plot3d.primitives.Scatter
import java.awt.event.KeyEvent

/**
 * Created by warrior on 25.04.16.
 */
val C = intArrayOf(1, 2, 5, 2, 3)
val A = arrayOf(intArrayOf(3, 5, 2, 1, 7), intArrayOf(5, 2, 1, 4, 9), intArrayOf(1, 2, 3, 4, 5))
val FN_2D: (DoubleArray) -> Double = { xs -> fn(xs, 2) }
val FN_3D: (DoubleArray) -> Double = { xs -> fn(xs, 3) }

const val MIN_X = 2.7934
const val MIN_Y = 1.5972
const val MIN = -4.155809247082015

const val X_LEFT = 0.0
const val X_RIGHT = 10.0
const val Y_LEFT = 0.0
const val Y_RIGHT = 10.0
const val Z_LEFT = 0.0
const val Z_RIGHT = 10.0

fun fn(xs: DoubleArray, argsNumber: Int): Double {
    var result = 0.0
    for ((i, c) in C.withIndex()) {
        var sum = 0.0;
        for (j in 0 until argsNumber) {
            val x = xs[j]
            sum += (x - A[j][i]) * (x - A[j][i])
        }
        result += c * Math.exp(-sum / Math.PI) * Math.cos(Math.PI * sum)
    }
    return result
}

fun range(left: Double, right: Double): Range = Range(left.toFloat(), right.toFloat())

class Surface(val surfaceName: String, chart: Chart, val fn: (Double, Double) -> Double,
              val xrange: Range, val yrange: Range, val steps: Int = 1000) : AbstractAnalysis() {

    init {
        this.chart = chart
    }

    override fun init() {
        // Define a function to plot
        val mapper = object : Mapper() {
            override fun f(x: Double, y: Double): Double = fn(x, y)
        }

        // Create the object to represent the function over the given range.
        val surface = Builder.buildOrthonormalBig(OrthonormalGrid(xrange, steps, yrange, steps), mapper)
        surface.colorMapper = ColorMapper(ColorMapRainbow(), surface.bounds.zmin.toDouble(), surface.bounds.zmax.toDouble(), Color(1f, 1f, 1f, .5f))
        surface.faceDisplayed = true
        surface.wireframeDisplayed = false

        // Create a chart
        chart.add(surface)
    }

    override fun getName(): String = surfaceName
}

class KeyListener(val chart: Chart, val points: List<List<Coord3d>>) : AWTCameraKeyController() {

    var step: Int = 2

    private var index = 0
    private var scatter: Scatter

    init {
        scatter = newScatter(index)
        chart.add(scatter)
        println("$index: ${points[index][0]}")
    }

    override fun keyReleased(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_Q || e.keyCode == KeyEvent.VK_W) {
            chart.removeDrawable(scatter)

            index = if (e.keyCode == KeyEvent.VK_W) {
                Math.min(points.lastIndex, index + step)
            } else {
                Math.max(0, index - step)
            }
            println("$index: ${points[index][0]}")

            scatter = newScatter(index)
            chart.add(scatter)
        } else {
            super.keyReleased(e)
        }
    }

    private fun newScatter(index: Int): Scatter = Scatter(points[index].toTypedArray(), Color.BLACK, 4f)
}
