package com.warrior.evolutionary.computation.pcms

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * Created by warrior on 12/16/16.
 */
private const val DEBUG = false

private const val ITERATION = 3
private const val K = 10

private val TEST_N = 10
private val MUTATION_PROBABILITY = doubleArrayOf(1.0, 0.5, 0.4, 0.3, 0.28, 0.265, 0.25, 0.24, 0.23, 0.22)

fun main(args: Array<String>) {
    val algorithm = GeneticAlgorithm()
    algorithm.run()
}

private fun DoubleArray.format(): String = joinToString(" ")

private class GeneticAlgorithm {

    private val interactor = FiveMinimaInteractor()

    private var mutationProbability: Double = 0.0

    fun run(): Int {
        try {
            val n = interactor.initValues()
            mutationProbability = MUTATION_PROBABILITY[n - 1]

            val random = Random()
            val populationSize = n * n

            var population = (1..populationSize).mapTo(ArrayList(populationSize))  {
                val xs = DoubleArray(n) { random.nextDouble() * 20 - 10 }
                val value = getValue(interactor, xs)
                Individual(xs, value)
            }

            while (true) {
                val newPopulation = ArrayList<Individual>(populationSize)
                for (individual in population) {
                    val xss = (0 until K).mapTo(ArrayList()) { mutation(individual.xs, random) }
                    val values = (0 until K).mapTo(ArrayList()) { i ->
                        val xs = xss[i]
                        getValue(interactor, xs, 1)
                    }
                    val minValue = values.min()!!
                    val children = xss.zip(values)
                            .filter { it.second - minValue <= 2 }
                            .map { p ->
                                val (xs, value) = p
                                val secondValue = getValue(interactor, xs, ITERATION - 1)
                                Individual(xs, (value + (ITERATION - 1) * secondValue) / ITERATION)
                            }
                    val bestChild = children.min()!!

                    if (bestChild < individual) {
                        newPopulation.add(bestChild)
                    } else {
                        newPopulation.add(individual)
                    }
                }

                population = newPopulation
            }
        } catch (e: BingoException) {
        }

        return interactor.requestCount
    }

    private fun getValue(interactor: FiveMinimaInteractor, xs: DoubleArray, iterations: Int = ITERATION): Double {
        val request = xs.format()
        return (1..iterations).map {
            interactor.request(request)
            val response = interactor.response()
            if (response == "Bingo") {
                if (DEBUG) {
                    throw BingoException()
                } else {
                    System.exit(0)
                }
            }
            response.toDouble()
        }.average()
    }

    private fun mutation(xs: DoubleArray, random: Random): DoubleArray {
        return DoubleArray(xs.size) { i ->
            if (random.nextDouble() < mutationProbability) {
                Math.min(10.0, Math.max(-10.0, random.nextGaussian() * 5 + xs[i]))
            } else {
                xs[i]
            }
        }
    }

    private data class Individual(val xs: DoubleArray, val value: Double) : Comparable<Individual> {
        override operator fun compareTo(other: Individual): Int = value.compareTo(other.value)
    }
}

private class FiveMinimaInteractor {

    private val random = Random()
    private val reader = BufferedReader(InputStreamReader(System.`in`))

    private val n: Int = TEST_N
    private val b: DoubleArray = DoubleArray(5) { 20 * random.nextDouble() - 10 }
    private val y: DoubleArray = DoubleArray(5) { random.nextDouble() + 1 }
    private val a: Array<DoubleArray> = Array(n) {
        DoubleArray(5) { 20 * random.nextDouble() - 10 }
    }
    private val secondMinima = b.sorted()[1]
    private val epsilon = 0.01

    private val f: (DoubleArray) -> Double = { xs ->
        (0..4).map { i ->
            val sum = (0 until n).map { j ->
                val q = xs[j] - a[j][i]
                q * q
            }.sum()
            b[i] + y[i] * sum
        }.min()!!
    }

    private var lastRequest: DoubleArray = doubleArrayOf()
    var requestCount = 0
        private set

    fun initValues(): Int {
        return if (DEBUG) {
            println("--> $n")
            n
        } else {
            reader.readLine().toInt()
        }
    }

    fun response(): String {
        return if (DEBUG) {
            val realValue = f(lastRequest)
            val response = if (realValue <= secondMinima + epsilon) {
                "Bingo"
            } else {
                val distortedValue = realValue + random.nextDouble() - 0.5
                distortedValue.toString()
            }
            println("--> $response")
            response
        } else {
            reader.readLine()
        }
    }

    fun request(action: String) {
        if (DEBUG) {
            requestCount++
            if (requestCount > n * n * 10000) {
                throw RequestCountExceededException()
            }
            println("<-- $action")
            lastRequest = action.split(" ").map(String::toDouble).toDoubleArray()
        } else {
            println(action)
        }
    }
}

private class BingoException : RuntimeException()
private class RequestCountExceededException : RuntimeException()
