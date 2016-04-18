package com.warrior.evolutionary.computation.lab1

import java.util.*

/**
 * Created by warrior on 27.03.16.
 */

class GeneticAlgorithm(val function: (Double) -> Double, val left: Double, val right: Double) {

    interface Predicate {
        fun test(value: Double): Boolean
    }

    private val random = Random()

    var numberOfBits = 20
    var mutationProbability = 0.1
    var crossoverProbability = 0.5

    val a = 1.5;

    fun search(populationSize: Int, predicate: Predicate): List<Pair<DoubleArray, DoubleArray>> {
        val pointsList = ArrayList<Pair<DoubleArray, DoubleArray>>()

        var population = random.longs(populationSize.toLong(), 0L, 1L shl numberOfBits)
                .toArray()
                .map { Individual(it, function(toDouble(it))) }
                .sortedDescending()
        pointsList.add(toPointList(population))

        val probabilities = ArrayList<Double>(populationSize)
        probabilities.add(probability(0, populationSize))
        for (i in 1..populationSize - 1) {
            probabilities.add(probabilities.last() + probability(i, populationSize))
        }

        while (!predicate.test(population[0].value)) {

            val parents = ArrayList<Pair<Individual, Individual>>()
            for (j in 1..populationSize) {
                val rand1 = random.nextDouble()
                val rand2 = random.nextDouble()

                val firstParentIndex = findParent(probabilities, rand1)
                val secondParentIndex = findParent(probabilities, rand2)

                parents.add(Pair(population[firstParentIndex], population[secondParentIndex]))
            }

            val children = parents.filter { random.nextDouble() < crossoverProbability }
                    .flatMap { p -> crossover(p.first.chromosome, p.second.chromosome).toList() }
                    .map { if (random.nextDouble() < mutationProbability) { mutation(it) } else { it } }
                    .map { Individual(it, function(toDouble(it))) }

            population = population.toMutableList()
                    .plus(children)
                    .sortedDescending()
                    .take(populationSize)

            pointsList.add(toPointList(population))
        }

        return pointsList
    }

    private fun toPointList(population: List<Individual>): Pair<DoubleArray, DoubleArray> {
        val x = population.map { toDouble(it.chromosome) }.toDoubleArray()
        val y = population.map { it.value }.toDoubleArray()
        return x to y
    }

    private fun findParent(probabilities: List<Double>, key: Double): Int {
        val index = Collections.binarySearch(probabilities, key, { o1, o2 -> o1.compareTo(o2) })
        return if (index >= 0) {
            index
        } else {
            -index - 1
        }
    }

    fun crossover(first: Long, second: Long): Pair<Long, Long> {
        val point = random.nextInt(numberOfBits)
        if (point == 0) {
            return Pair(first, second)
        }

        val mask = (1L shl point) - 1

        val firstLeft = first and mask
        val firstRight = first xor firstLeft
        val secondLeft = second and mask
        val secondRight = second xor secondLeft

        return Pair(firstLeft + secondRight, firstRight + secondLeft)
    }

    fun mutation(chromosome: Long): Long {
        val point = random.nextInt(numberOfBits)
        return chromosome xor (1L shl point)
    }

    private fun probability(i: Int, populationSize: Int): Double =
            (a - (a - (2 - a)) * i / ((populationSize - 1))) / populationSize

    fun toDouble(point: Long): Double = left + point * (right - left) / ((1L shl numberOfBits) - 1)

    data class Individual(val chromosome: Long, val value: Double): Comparable<Individual> {
        override fun compareTo(other: Individual): Int = value.compareTo(other.value)
    }
}
