package com.warrior.evolutionary.computation.lab1

import org.math.plot.Plot2DPanel
import java.awt.Color
import java.awt.Rectangle
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import javax.swing.JFrame

/**
 * Created by warrior on 18.04.16.
 */
fun main(args: Array<String>) {
    drawPlot("population size", "iterations", ::populationSizeTest)
    drawPlot("crossover probability", "iterations", ::crossoverProbabilityTest)
    drawPlot("mutation probability", "iterations", ::mutationProbabilityTest)
}

private fun drawPlot(xLabel: String, yLabel: String, daraFactory: () -> Pair<DoubleArray, DoubleArray>) {
    val result = daraFactory()
    val plot = Plot2DPanel()
    plot.setAxisLabels(xLabel, yLabel)
    plot.addLinePlot("", Color.BLACK, result.first, result.second)

    val frame = JFrame();
    frame.bounds = Rectangle(0, 0, 800, 500)
    frame.contentPane = plot
    frame.isVisible = true
}

private fun populationSizeTest(): Pair<DoubleArray, DoubleArray> {
    val populationSize = DoubleArray(19) { (it + 2) * 5.0 }
    val algorithm = GeneticAlgorithm(FN, LEFT, RIGHT)

    val values = DoubleStream.of(*populationSize)
            .parallel()
            .map { size ->
                println(size)
                IntStream.range(0, 100).map {
                    val predicate = PrecisionPredicate(MAX_Y, 0.001)
                    algorithm.search(size.toInt(), predicate)
                    println("$size[$it]: ${predicate.iterationPass}")
                    predicate.iterationPass
                }.average().asDouble
            }
            .toArray()
    return populationSize to values
}

private fun crossoverProbabilityTest(): Pair<DoubleArray, DoubleArray> {
    val crossoverProbability = DoubleArray(10) { (it + 1) * 0.1 }

    val values = DoubleStream.of(*crossoverProbability)
            .parallel()
            .map { probability ->
                println(probability)
                val algorithm = GeneticAlgorithm(FN, LEFT, RIGHT)
                algorithm.crossoverProbability = probability
                IntStream.range(0, 1500).map {
                    val predicate = PrecisionPredicate(MAX_Y, 0.001)
                    algorithm.search(40, predicate)
                    println("$probability[$it]: ${predicate.iterationPass}")
                    predicate.iterationPass
                }.average().asDouble
            }
            .toArray()
    return crossoverProbability to values
}

private fun mutationProbabilityTest(): Pair<DoubleArray, DoubleArray> {
    val mutationProbability = DoubleArray(50) { 0.001 + it * 0.002 }

    val values = DoubleStream.of(*mutationProbability)
            .parallel()
            .map { probability ->
                println(probability)
                val algorithm = GeneticAlgorithm(FN, LEFT, RIGHT)
                algorithm.mutationProbability = probability
                IntStream.range(0, 1000).map {
                    val predicate = PrecisionPredicate(MAX_Y, 0.001)
                    algorithm.search(40, predicate)
                    println("$probability[$it]: ${predicate.iterationPass}")
                    predicate.iterationPass
                }.average().asDouble
            }
            .toArray()
    return mutationProbability to values
}
