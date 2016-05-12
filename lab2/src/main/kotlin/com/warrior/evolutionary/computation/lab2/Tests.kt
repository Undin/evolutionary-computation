package com.warrior.evolutionary.computation.lab2

import com.warrior.evolutionary.computation.core.DoubleRange
import com.warrior.evolutionary.computation.core.IterationPredicate
import com.warrior.evolutionary.computation.core.PrecisionPredicate
import org.math.plot.Plot2DPanel
import java.awt.Color
import java.awt.Rectangle
import java.util.stream.Collectors
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import javax.swing.JFrame

/**
 * Created by warrior on 18.04.16.
 */
val PRECISION = 0.05

fun main(args: Array<String>) {
    val (populationSize, values1) = populationSizeToIterationsAndCalls()
    drawPlot("population size", "iterations", Pair(populationSize, values1.first))
    drawPlot("population size", "calls", Pair(populationSize, values1.second))

    val (crossoverProbability, values2) = crossoverProbabilityToIterationsAndCalls()
    drawPlot("crossover probability", "iterations", Pair(crossoverProbability, values2.first))
    drawPlot("crossover probability", "calls", Pair(crossoverProbability, values2.second))

    val (mutationProbability, values3) = mutationProbabilityToIterationsAndCalls()
    drawPlot("mutation probability", "iterations", Pair(mutationProbability, values3.first))
    drawPlot("mutation probability", "calls", Pair(mutationProbability, values3.second))

    drawPlot("population size", "precision", populationSizeToPrecision())
    drawPlot("crossover probability", "precision", crossoverProbabilityToPrecision())
    drawPlot("mutation probability", "precision", mutationProbabilityToPrecision())

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

private fun populationSizeToIterationsAndCalls(): Pair<DoubleArray, Pair<DoubleArray, DoubleArray>> {
    val populationSizes = DoubleArray(19) { (it + 2) * 5.0 }
    val result = measureIterationsAndCalls(populationSizes, 500) {
        populationSize = it.toInt()
    }
    return Pair(populationSizes, result)
}

private fun crossoverProbabilityToIterationsAndCalls(): Pair<DoubleArray, Pair<DoubleArray, DoubleArray>> {
    val crossoverProbabilities = DoubleArray(10) { (it + 1) * 0.1 }
    val result = measureIterationsAndCalls(crossoverProbabilities, 500) {
        crossoverProbability = it
    }
    return Pair(crossoverProbabilities, result)
}

private fun mutationProbabilityToIterationsAndCalls(): Pair<DoubleArray, Pair<DoubleArray, DoubleArray>> {
    val mutationProbabilities = DoubleArray(25) { 0.001 + it * 0.004 }
    val result = measureIterationsAndCalls(mutationProbabilities, 500) {
        mutationProbability = it
    }
    return Pair(mutationProbabilities, result)
}

private fun measureIterationsAndCalls(params: DoubleArray, iterations: Int, block: GeneticAlgorithm.(Double) -> Unit): Pair<DoubleArray, DoubleArray> {
    val values = DoubleStream.of(*params)
            .parallel()
            .mapToObj { param ->
                println(param)

                var iterationsSum = 0
                var callsSum = 0
                for (i in 1..iterations) {
                    var calls = 0
                    val algorithm = GeneticAlgorithm(arrayOf(DoubleRange(0.0, 10.0), DoubleRange(0.0, 10.0))) {
                        calls++
                        FN_2D(it)
                    }
                    algorithm.block(param)

                    val predicate = PrecisionPredicate(MIN, PRECISION, 200)
                    algorithm.search(predicate)
                    iterationsSum += predicate.iterationPass
                    callsSum += calls
                }
                Pair(iterationsSum.toDouble() / iterations, callsSum.toDouble() / iterations)
            }
            .collect(Collectors.toList<Pair<Double, Double>>())

    val iterationsArray = DoubleArray(values.size) { values[it].first }
    val callsArray = DoubleArray(values.size) { values[it].second }
    return Pair(iterationsArray, callsArray)
}

private fun populationSizeToPrecision(): Pair<DoubleArray, DoubleArray> {
    val populationSizes = DoubleArray(19) { (it + 2) * 5.0 }
    val result = measurePrecision(populationSizes, 100) {
        populationSize = it.toInt()
    }
    return Pair(populationSizes, result)
}

private fun crossoverProbabilityToPrecision(): Pair<DoubleArray, DoubleArray> {
    val crossoverProbabilities = DoubleArray(10) { (it + 1) * 0.1 }
    val result = measurePrecision(crossoverProbabilities, 500) {
        crossoverProbability = it
    }
    return Pair(crossoverProbabilities, result)
}

private fun mutationProbabilityToPrecision(): Pair<DoubleArray, DoubleArray> {
    val mutationProbabilities = DoubleArray(25) { 0.001 + it * 0.004 }
    val result = measurePrecision(mutationProbabilities, 1000) {
        mutationProbability = it
    }
    return Pair(mutationProbabilities, result)
}

private fun measurePrecision(params: DoubleArray, iterations: Int, block: GeneticAlgorithm.(Double) -> Unit): DoubleArray {
    return DoubleStream.of(*params)
            .parallel()
            .map { param ->
                println(param)
                val algorithm = GeneticAlgorithm(arrayOf(DoubleRange(0.0, 10.0), DoubleRange(0.0, 10.0)), FN_2D)
                algorithm.block(param)
                IntStream.range(0, iterations).mapToDouble {
                    val predicate = IterationPredicate(100)
                    val populations = algorithm.search(predicate)
                    Math.abs(populations.last()[0].value - MIN)
                }.average().asDouble
            }
            .toArray()
}
