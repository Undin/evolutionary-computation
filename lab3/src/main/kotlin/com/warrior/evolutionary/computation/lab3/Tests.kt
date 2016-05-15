package com.warrior.evolutionary.computation.lab3

import com.warrior.evolutionary.computation.core.IterationPredicate
import org.math.plot.Plot2DPanel
import java.awt.Color
import java.awt.Rectangle
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import javax.swing.JFrame

/**
 * Created by warrior on 15/05/16.
 */

fun main(args: Array<String>) {
    drawPlot("survived part", "iterations", survivedPartMeasure())
    drawPlot("tournament probability", "iterations", tournamentProbabilityMeasure())
    drawPlot("mutation probability", "iterations", mutationProbabilityMeasure())
}

private fun drawPlot(xLabel: String, yLabel: String, result: Pair<DoubleArray, DoubleArray>) {
    val plot = Plot2DPanel()
    plot.setAxisLabels(xLabel, yLabel)
    plot.addLinePlot("", Color.BLACK, result.first, result.second)

    val frame = JFrame();
    frame.bounds = Rectangle(0, 0, 800, 500)
    frame.contentPane = plot
    frame.isVisible = true
}

private fun survivedPartMeasure(): Pair<DoubleArray, DoubleArray> {
    val survivedPart = DoubleArray(10) { 0.1 * (it + 1) }
    val result = measure(survivedPart, 200) {
        this.survivedPart = it
    }
    return Pair(survivedPart, result)
}

private fun mutationProbabilityMeasure(): Pair<DoubleArray, DoubleArray> {
    val mutationProbability = DoubleArray(10) { 0.1 * (it + 1) }
    val result = measure(mutationProbability, 200) {
        this.mutationProbability = it
    }
    return Pair(mutationProbability, result)
}

private fun tournamentProbabilityMeasure(): Pair<DoubleArray, DoubleArray> {
    val tournamentProbability = DoubleArray(10) { 0.1 * (it + 1) }
    val result = measure(tournamentProbability, 200) {
        this.tournamentProbability = it
    }
    return Pair(tournamentProbability, result)
}

private fun measure(params: DoubleArray, iterations: Int, block: GeneticAlgorithm.(Double) -> Unit): DoubleArray {
    return DoubleStream.of(*params)
            .parallel()
            .map { param ->
                println(param)
                val algorithm = GeneticAlgorithm(POINTS, ::dist)
                algorithm.block(param)
                IntStream.range(0, iterations).mapToDouble {
                    val predicate = IterationPredicate(1000)
                    algorithm.search(100, predicate).last().second
                }.average().asDouble
            }
            .toArray()
}
