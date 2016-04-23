package com.warrior.evolutionary.computation.lab1

import com.warrior.evolutionary.computation.core.Predicate
import java.util.*

/**
 * Created by warrior on 27.03.16.
 */

class GeneticAlgorithm(val function: (Double) -> Double, val left: Double, val right: Double) {

    private val random = Random()

    var numberOfBits = 20
    var mutationProbability = 0.1
    var crossoverProbability = 0.5

    val a = 1.5;

    fun search(populationSize: Int, predicate: Predicate): List<Pair<DoubleArray, DoubleArray>> {
        val pointsList = ArrayList<Pair<DoubleArray, DoubleArray>>()

        // calculate probabilities for selection
        val probabilities = ArrayList<Double>(populationSize)
        probabilities.add(probability(0, populationSize))
        for (i in 1..populationSize - 1) {
            probabilities.add(probabilities.last() + probability(i, populationSize))
        }

        // initialize initial generation
        var population = random.longs(populationSize.toLong(), 0L, 1L shl numberOfBits)
                .toArray()
                .map { Individual(it, function(toDouble(it))) }
                .sortedDescending()
        pointsList.add(toPointList(population))

        while (!predicate.test(population[0].value)) {

            // select parents for crossover
            val parents = ArrayList<Pair<Individual, Individual>>()
            for (j in 1..populationSize) {
                val rand1 = random.nextDouble()
                val rand2 = random.nextDouble()

                val firstParentIndex = findParent(probabilities, rand1)
                val secondParentIndex = findParent(probabilities, rand2)

                parents.add(Pair(population[firstParentIndex], population[secondParentIndex]))
            }

            val children =
                    // select parents for crossover according with crossover probability
                    parents.filter { random.nextDouble() < crossoverProbability }
                    // make crossover
                    .flatMap { p -> crossover(p.first.chromosome, p.second.chromosome).toList() }
                    // make mutation according with mutation probability
                    .map { if (random.nextDouble() < mutationProbability) { mutation(it) } else { it } }
                    // calculate fitness funtion
                    .map { Individual(it, function(toDouble(it))) }

            population = population.toMutableList()
                    .plus(children) // union current generation and children
                    .sortedDescending() // sort according with fitness function
                    .take(populationSize) // take best individuals

            // add new population to result list
            pointsList.add(toPointList(population))
        }

        return pointsList
    }

    private fun crossover(first: Long, second: Long): Pair<Long, Long> {
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

    private fun mutation(chromosome: Long): Long {
        val point = random.nextInt(numberOfBits)
        return chromosome xor (1L shl point)
    }

    private fun toPointList(population: List<Individual>): Pair<DoubleArray, DoubleArray> {
        val x = population.map { toDouble(it.chromosome) }.toDoubleArray()
        val y = population.map { it.value }.toDoubleArray()
        return x to y
    }

    private fun findParent(probabilities: List<Double>, key: Double): Int {
        val index = Collections.binarySearch(probabilities, key, { o1, o2 -> o1.compareTo(o2) })
        return if (index >= 0) { index } else { -index - 1 }
    }

    private fun probability(i: Int, populationSize: Int): Double =
            (a - (a - (2 - a)) * i / ((populationSize - 1))) / populationSize

    private fun toDouble(point: Long): Double = left + point * (right - left) / ((1L shl numberOfBits) - 1)

    data class Individual(val chromosome: Long, val value: Double): Comparable<Individual> {
        override fun compareTo(other: Individual): Int = value.compareTo(other.value)
    }
}
