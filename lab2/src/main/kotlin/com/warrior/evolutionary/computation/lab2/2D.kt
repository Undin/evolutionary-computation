package com.warrior.evolutionary.computation.lab2

import com.warrior.evolutionary.computation.core.DoubleRange
import com.warrior.evolutionary.computation.core.IterationPredicate
import org.jzy3d.analysis.AnalysisLauncher
import org.jzy3d.chart.factories.AWTChartComponentFactory
import org.jzy3d.colors.Color
import org.jzy3d.maths.Coord3d
import org.jzy3d.plot3d.primitives.Scatter
import org.jzy3d.plot3d.rendering.canvas.Quality

/**
 * Created by warrior on 20.04.16.
 */

fun main(args: Array<String>) {

    var populations: List<List<GeneticAlgorithm.Individual>>? = null
    for (i in 1..20) {
        val algorithm = GeneticAlgorithm(FN_2D, arrayOf(DoubleRange(X_LEFT, X_RIGHT), DoubleRange(Y_LEFT, Y_RIGHT)))
        val newPopulations = algorithm.search(50, IterationPredicate(100))
        if (populations == null || populations.last()[0] < newPopulations.last()[0]) {
            populations = newPopulations
        }
    }

    populations!!

    val points = populations.map {
        it.map { Coord3d(it.chromosome[0], it.chromosome[1], it.value) }
    }

    val chart = AWTChartComponentFactory.chart(Quality.Advanced)
    val surface = Surface("x-y", chart, { x, y -> FN_2D(doubleArrayOf(x, y)) },
            range(X_LEFT, X_RIGHT), range(Y_LEFT, Y_RIGHT))
    AnalysisLauncher.open(surface)
    val minPoint = Scatter(arrayOf(Coord3d(MIN_X, MIN_Y, MIN)), Color.RED, 5f)
    chart.add(minPoint)

    println(points.last()[0])
    chart.addController(KeyListener(chart, points))
}
