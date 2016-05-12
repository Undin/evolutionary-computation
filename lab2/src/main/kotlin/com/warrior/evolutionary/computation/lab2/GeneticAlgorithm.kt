package com.warrior.evolutionary.computation.lab2

import com.warrior.evolutionary.computation.core.Predicate
import java.util.*

/**
 * Created by warrior on 22.04.16.
 */
class GeneticAlgorithm(val ranges: Array<ClosedRange<Double>>, val function: (DoubleArray) -> Double) {

    private val random = Random()

    var populationSize = 50
    var mutationProbability = 0.1
    var crossoverProbability = 0.9

    var alpha = 0.4

    val a = 1.5;

    fun search(predicate: Predicate): List<List<Individual>> {
        val pointsList = ArrayList<List<Individual>>()

        var population = (1..populationSize).map {
            DoubleArray(ranges.size) {
                val r = ranges[it]
                random.nextDouble() * (r.endInclusive - r.start) + r.start
            }
        }
        .map { Individual(it, function(it)) }
        .sortedDescending()


        pointsList.add(ArrayList(population))

        val probabilities = ArrayList<Double>(populationSize)
        probabilities.add(probability(0, populationSize))
        for (i in 1..populationSize - 1) {
            probabilities.add(probabilities.last() + probability(i, populationSize))
        }

        while (!predicate.test(population[0].value)) {

            val parents = ArrayList<Pair<Individual, Individual>>()
            for (j in 1..2 * populationSize) {
                val rand1 = random.nextDouble()
                val rand2 = random.nextDouble()

                val firstParentIndex = findParent(probabilities, rand1)
                val secondParentIndex = findParent(probabilities, rand2)

                parents.add(Pair(population[firstParentIndex], population[secondParentIndex]))
            }

            val children = parents.filter { random.nextDouble() < crossoverProbability }
                    .map { p -> crossover(p.first.chromosome, p.second.chromosome) }
                    .map { if (random.nextDouble() < mutationProbability) { mutation(it) } else { it } }
                    .map { Individual(it, function(it)) }

            population = population.toMutableList()
                    .plus(children)
                    .sortedDescending()
                    .take(populationSize)

            pointsList.add(ArrayList(population))
        }

        return pointsList
    }

    private fun probability(i: Int, populationSize: Int): Double =
            (a - (a - (2 - a)) * i / ((populationSize - 1))) / populationSize

    private fun findParent(probabilities: List<Double>, key: Double): Int {
        val index = Collections.binarySearch(probabilities, key, { o1, o2 -> o1.compareTo(o2) })
        return if (index >= 0) { index } else { -index - 1 }
    }

    private fun crossover(first: DoubleArray, second: DoubleArray): DoubleArray {
        return DoubleArray(first.size) { index ->
            val min = Math.min(first[index], second[index])
            val max = Math.max(first[index], second[index])
            val width = max - min
            val newValue = random.nextDouble() * width * (1 + 2 * alpha)
            Math.max(ranges[index].start, Math.min(ranges[index].endInclusive, newValue))
        }
    }

    private fun mutation(chromosome: DoubleArray): DoubleArray {
        val index = random.nextInt(chromosome.size)
        val range = ranges[index]
        val newChromosome = chromosome.clone()
        newChromosome[index] = random.nextDouble() * (range.endInclusive - range.start) + range.start
        return newChromosome
    }

    data class Individual(val chromosome: DoubleArray, val value: Double): Comparable<Individual> {
        override fun compareTo(other: Individual): Int = -value.compareTo(other.value)
        override fun toString(): String = "${Arrays.toString(chromosome)} -> $value"
    }
}
