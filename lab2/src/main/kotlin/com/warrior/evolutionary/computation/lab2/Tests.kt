package com.warrior.evolutionary.computation.lab2

import com.warrior.evolutionary.computation.core.DoubleRange
import com.warrior.evolutionary.computation.core.IterationPredicate
import com.warrior.evolutionary.computation.core.PrecisionPredicate
import org.math.plot.Plot2DPanel
import java.awt.Color
import java.awt.Rectangle
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import javax.swing.JFrame

/**
 * Created by warrior on 18.04.16.
 */
val PRECISION = 0.05

fun main(args: Array<String>) {
    drawPlot("population size", "iterations", ::populationSizeToIterations)
    drawPlot("crossover probability", "iterations", ::crossoverProbabilityToIterations)
    drawPlot("mutation probability", "iterations", ::mutationProbabilityToIterations)
    drawPlot("population size", "precision", ::populationSizeToPrecision)
    drawPlot("crossover probability", "precision", ::crossoverProbabilityToPrecision)
    drawPlot("mutation probability", "precision", ::mutationProbabilityToPrecision)

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

private fun populationSizeToIterations(): Pair<DoubleArray, DoubleArray> {
    val populationSize = DoubleArray(19) { (it + 2) * 5.0 }
    val algorithm = GeneticAlgorithm(FN_2D, arrayOf(DoubleRange(0.0, 10.0), DoubleRange(0.0, 10.0)))

    val values = DoubleStream.of(*populationSize)
            .parallel()
            .map { size ->
                println(size)
                IntStream.range(0, 500).map {
                    val predicate = PrecisionPredicate(MIN, PRECISION, 200)
                    algorithm.search(size.toInt(), predicate)
                    println("$size[$it]: ${predicate.iterationPass}")
                    predicate.iterationPass
                }.average().asDouble
            }
            .toArray()
    return populationSize to values
}

private fun crossoverProbabilityToIterations(): Pair<DoubleArray, DoubleArray> {
    val crossoverProbability = DoubleArray(10) { (it + 1) * 0.1 }

    val values = DoubleStream.of(*crossoverProbability)
            .parallel()
            .map { probability ->
                println(probability)
                val algorithm = GeneticAlgorithm(FN_2D, arrayOf(DoubleRange(0.0, 10.0), DoubleRange(0.0, 10.0)))
                algorithm.crossoverProbability = probability
                IntStream.range(0, 500).map {
                    val predicate = PrecisionPredicate(MIN, PRECISION, 200)
                    algorithm.search(50, predicate)
                    println("$probability[$it]: ${predicate.iterationPass}")
                    predicate.iterationPass
                }.average().asDouble
            }
            .toArray()
    return crossoverProbability to values
}

private fun mutationProbabilityToIterations(): Pair<DoubleArray, DoubleArray> {
    val mutationProbability = DoubleArray(25) { 0.001 + it * 0.004 }

    val values = DoubleStream.of(*mutationProbability)
            .parallel()
            .map { probability ->
                println(probability)
                val algorithm = GeneticAlgorithm(FN_2D, arrayOf(DoubleRange(0.0, 10.0), DoubleRange(0.0, 10.0)))
                algorithm.mutationProbability = probability
                IntStream.range(0, 500).map {
                    val predicate = PrecisionPredicate(MIN, PRECISION, 200)
                    algorithm.search(50, predicate)
                    println("$probability[$it]: ${predicate.iterationPass}")
                    predicate.iterationPass
                }.average().asDouble
            }
            .toArray()
    return mutationProbability to values
}

private fun populationSizeToPrecision(): Pair<DoubleArray, DoubleArray> {
    val populationSize = DoubleArray(19) { (it + 2) * 5.0 }
    val algorithm = GeneticAlgorithm(FN_2D, arrayOf(DoubleRange(0.0, 10.0), DoubleRange(0.0, 10.0)))

    val values = DoubleStream.of(*populationSize)
            .parallel()
            .map { size ->
                println(size)
                IntStream.range(0, 100).mapToDouble {
                    val predicate = IterationPredicate(100)
                    val populations = algorithm.search(size.toInt(), predicate)
                    val result = Math.abs(populations.last()[0].value - MIN)
                    println("$size[$it]: $result")
                    result
                }.average().asDouble
            }
            .toArray()
    return populationSize to values
}

private fun crossoverProbabilityToPrecision(): Pair<DoubleArray, DoubleArray> {
    val crossoverProbability = DoubleArray(10) { (it + 1) * 0.1 }

    val values = DoubleStream.of(*crossoverProbability)
            .parallel()
            .map { probability ->
                println(probability)
                val algorithm = GeneticAlgorithm(FN_2D, arrayOf(DoubleRange(0.0, 10.0), DoubleRange(0.0, 10.0)))
                algorithm.crossoverProbability = probability
                IntStream.range(0, 500).mapToDouble {
                    val predicate = IterationPredicate(100)
                    val populations = algorithm.search(50, predicate)
                    val result = Math.abs(populations.last()[0].value - MIN)
                    println("$probability[$it]: $result")
                    result
                }.average().asDouble
            }
            .toArray()
    return crossoverProbability to values
}

private fun mutationProbabilityToPrecision(): Pair<DoubleArray, DoubleArray> {
    val mutationProbability = DoubleArray(25) { 0.001 + it * 0.004 }

    val values = DoubleStream.of(*mutationProbability)
            .parallel()
            .map { probability ->
                println(probability)
                val algorithm = GeneticAlgorithm(FN_2D, arrayOf(DoubleRange(0.0, 10.0), DoubleRange(0.0, 10.0)))
                algorithm.mutationProbability = probability
                IntStream.range(0, 1000).mapToDouble {
                    val predicate = IterationPredicate(100)
                    val populations = algorithm.search(50, predicate)
                    val result = Math.abs(populations.last()[0].value - MIN)
                    println("$probability[$it]: $result")
                    result
                }.average().asDouble
            }
            .toArray()
    return mutationProbability to values
}
