package com.warrior.evolutionary.computation.lab3

import com.warrior.evolutionary.computation.core.IterationPredicate
import java.awt.Color
import java.awt.Polygon
import java.awt.Rectangle
import javax.swing.JFrame

/**
 * Created by warrior on 24.04.16.
 */

val POINTS = listOf(
        Pair(6734, 1453),
        Pair(2233, 10),
        Pair(5530, 1424),
        Pair(401, 841),
        Pair(3082, 1644),
        Pair(7608, 4458),
        Pair(7573, 3716),
        Pair(7265, 1268),
        Pair(6898, 1885),
        Pair(1112, 2049),
        Pair(5468, 2606),
        Pair(5989, 2873),
        Pair(4706, 2674),
        Pair(4612, 2035),
        Pair(6347, 2683),
        Pair(6107, 669),
        Pair(7611, 5184),
        Pair(7462, 3590),
        Pair(7732, 4723),
        Pair(5900, 3561),
        Pair(4483, 3369),
        Pair(6101, 1110),
        Pair(5199, 2182),
        Pair(1633, 2809),
        Pair(4307, 2322),
        Pair(675, 1006),
        Pair(7555, 4819),
        Pair(7541, 3981),
        Pair(3177, 756),
        Pair(7352, 4506),
        Pair(7545, 2801),
        Pair(3245, 3305),
        Pair(6426, 3173),
        Pair(4608, 1198),
        Pair(23, 2216),
        Pair(7248, 3779),
        Pair(7762, 4595),
        Pair(7392, 2244),
        Pair(3484, 2829),
        Pair(6271, 2135),
        Pair(4985, 140),
        Pair(1916, 1569),
        Pair(7280, 4899),
        Pair(7509, 3239),
        Pair(10, 2676),
        Pair(6807, 2993),
        Pair(5185, 3258),
        Pair(3023, 1942)
)

val BEST_WAY = listOf(0, 7, 37, 30, 43, 17, 6, 27, 5, 36, 18, 26, 16, 42, 29, 35,
        45, 32, 19, 46, 20, 31, 38, 47, 4, 41, 23, 9, 44, 34, 3, 25, 1, 28,
        33, 40, 15, 21, 2, 22, 13, 24, 12, 10, 11, 14, 39, 8)

val BEST_RESULT = dist(BEST_WAY.map { POINTS[it] })

fun main(args: Array<String>) {
    val algorithm = GeneticAlgorithm(POINTS, ::dist)
    val results = algorithm.search(200, IterationPredicate(1000))
    println(BEST_RESULT)
    draw(results.last().first)
}

fun dist(points: List<Pair<Int, Int>>): Double {
    var dist = 0.0;
    for (i in 0..points.lastIndex) {
        val (x1, y1) = points[i]
        val (x2, y2) = points[(i + 1) % points.size]
        dist += Math.sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).toDouble())
    }
    return dist
}

private fun draw(wayOrder: List<Int>) {
    val frame = JFrame()
    frame.bounds = Rectangle(0, 0, 700, 700)
    frame.isVisible = true
    var minX = Int.MAX_VALUE
    var minY = Int.MAX_VALUE
    var maxX = 0
    var maxY = 0
    POINTS.forEach { p -> 
        if (p.first < minX) {
            minX = p.first
        }
        if (p.first > maxX) {
            maxX = p.first
        }
        if (p.second < minY) {
            minY = p.second
        }
        if (p.second > maxY) {
            maxY = p.second
        }
    }
    val normalizedPoints = POINTS.map { p ->
        val x = ((p.first - minX).toDouble() / (maxX - minX) * 600).toInt() + 50
        val y = ((p.second - minY).toDouble() / (maxY - minY) * 600).toInt() + 50
        Pair(x, y)
    }

    drawPoints(frame, normalizedPoints)
    val bestWay = BEST_WAY.map { normalizedPoints[it] }
    drawWay(frame, bestWay, Color.BLUE)
    val way = wayOrder.map { normalizedPoints[it] }
    drawWay(frame, way, Color.GREEN)
}

fun drawPoints(frame: JFrame, points: List<Pair<Int, Int>>) {
    val graphics = frame.graphics
    points.forEachIndexed { i, p ->
        graphics.drawOval(p.first, p.second, 3, 3)
        graphics.fillOval(p.first, p.second, 3, 3)
        graphics.drawString("$i", p.first + 2, p.second - 2)
    }
}

fun drawWay(frame: JFrame, points: List<Pair<Int, Int>>, color: Color) {
    val xs = IntArray(points.size) { points[it].first }
    val ys = IntArray(points.size) { points[it].second }
    val graphics = frame.graphics
    graphics.color = color
    graphics.drawPolygon(Polygon(xs, ys, points.size))
}
