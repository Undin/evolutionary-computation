package com.warrior.evolutionary.computation.lab3

import com.warrior.evolutionary.computation.core.Predicate
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.StreamSupport

/**
 * Created by warrior on 24.04.16.
 */
class GeneticAlgorithm(val points: List<Pair<Int, Int>>, val function: (List<Pair<Int, Int>>) -> Double) {

    private val random = Random()

    private val minX: Int
    private val maxX: Int
    private val minY: Int
    private val maxY: Int

    init {
        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE

        for ((x, y) in points) {
            if (x < minX) {
                minX = x
            }
            if (x > maxX) {
                maxX = x
            }
            if (y < minY) {
                minY = y
            }
            if (y < maxY) {
                maxY = y
            }
        }
        this.minX = minX
        this.maxX = maxX
        this.minY = minY
        this.maxY = maxY
    }

    var survivedPart = 0.01
    var mutationProbability = 0.5
    var tournamentProbability = 0.8

    fun search(populationSize: Int, predicate: Predicate): List<Pair<List<Int>, Double>> {
        val pointsList = ArrayList<Pair<List<Int>, Double>>(populationSize + 1)

        var population = IntStream.range(0, populationSize)
                .mapToObj { randomChromosome() }
                .parallel()
                .map { Individual(it, dist(it)) }
                .sorted()
                .collect(Collectors.toList<Individual>())

        printBests(population)
        pointsList.add(Pair(population[0].chromosome.toAbsoluteOrder(0..points.lastIndex), population[0].value))

        val childrenNumber = (Math.min(populationSize - 1, (survivedPart * populationSize + 0.5).toInt()) / 2) * 2

        while (!predicate.test(population[0].value)) {
            val childrenChromosomes = ArrayList<List<Int>>(childrenNumber)

            for (i in 1..childrenNumber / 2) {
                val firstParent = population[random.nextInt(populationSize)]
                val secondParent = population[random.nextInt(populationSize)]
                val (firstChromosome, secondChromosome) = crossover(firstParent.chromosome, secondParent.chromosome)
                val firstChild = mutation(firstChromosome)
                val secondChild = mutation(secondChromosome)
                childrenChromosomes.add(firstChild)
                childrenChromosomes.add(secondChild)
            }

            val children = StreamSupport.stream(childrenChromosomes.spliterator(), true)
                    .map { Individual(it, dist(it)) }
                    .collect(Collectors.toList<Individual>())

            population = selection(population, children)

            printBests(population)
            pointsList.add(Pair(population[0].chromosome.toAbsoluteOrder(0..points.lastIndex), population[0].value))
        }

        return pointsList
    }

    private fun printBests(population: List<Individual>) {
        for (i in 0..4) {
            println(population[i].value)
        }
        println("---------------------------")
    }

    private fun selection(currentPopulation: List<Individual>, children: List<Individual>): List<Individual> {
        val populationSize = currentPopulation.size
        val survivedNumber = populationSize - children.size
        val newPopulation = ArrayList<Individual>(populationSize)
        val other = ArrayList<Individual>(2 * children.size)
        for ((i, ch) in currentPopulation.withIndex()) {
            if (i < survivedNumber) {
                newPopulation.add(ch)
            } else {
                other.add(ch)
            }
        }
        other += children
        for (j in 1..populationSize - survivedNumber) {
            val first = random.nextInt(other.size)
            val second = random.nextInt(other.size)
            val index = if (random.nextDouble() < tournamentProbability) { first } else { second }
            newPopulation.add(other.removeAt(index))
        }
        newPopulation.sort()
        return newPopulation
    }

    private fun dist(relativeOrder: List<Int>): Double {
        val absoluteOrder = relativeOrder.toAbsoluteOrder(points)
        return function(absoluteOrder)
    }

    private fun randomChromosome(): List<Int> {
        val x = random.nextInt(maxX - minX) + minX
        val y = random.nextInt(maxY - minY) + minY
        val comparator = PointComparator(points, x, y)

        val (left, right) = (0..points.lastIndex).partition { i -> points[i].first < x }
        val leftSorted = left.sortedWith(comparator)
        val rightSorted = right.sortedWith(comparator.reversed())
        val sortedPoints = leftSorted + rightSorted
        return sortedPoints.toRelativeOrder()
    }

    private fun crossover(first: List<Int>, second: List<Int>): Pair<List<Int>, List<Int>> {
        val k = random.nextInt(points.size)
        val firstChild = first.subList(0, k) + second.subList(k, second.size)
        val secondChild = second.subList(0, k) + first.subList(k, first.size)
        return Pair(firstChild, secondChild)
    }

    private fun mutation(chromosome: List<Int>): List<Int> {
        if (random.nextDouble() > mutationProbability) {
            return chromosome
        }

        val absoluteOrder = chromosome.toAbsoluteOrder(0..points.lastIndex).toMutableList()
        val i = random.nextInt(points.lastIndex)
        val j = random.nextInt(points.lastIndex)
        val c = absoluteOrder[i]
        absoluteOrder[i] = absoluteOrder[j]
        absoluteOrder[j] = c
        return absoluteOrder.toRelativeOrder()
    }

    private fun <T> List<Int>.toAbsoluteOrder(src: Iterable<T>): List<T> {
        val points = src.toMutableList()
        return map { points.removeAt(it) }
    }

    private fun List<Int>.toRelativeOrder(): List<Int> {
        val indices = (0..points.lastIndex).toMutableList()
        return map {
            val i = indices.indexOf(it)
            indices.removeAt(i)
            i
        }
    }

    data class Individual(val chromosome: List<Int>, val value: Double): Comparable<Individual> {
        override fun compareTo(other: Individual): Int = value.compareTo(other.value)
    }

    private class PointComparator(val points: List<Pair<Int, Int>>, val x: Int, val y: Int) : Comparator<Int> {
        override fun compare(o1: Int, o2: Int): Int {
            val (x1, y1) = points[o1]
            val (x2, y2) = points[o2]
            return cos(x1, y1).compareTo(cos(x2, y2))
        }

        private fun cos(x1: Int, y1: Int): Double =
                (x1 - x) / Math.sqrt(((x1 - x) * (x1 - x) + (y1 - y) * (y1 - y)).toDouble())
    }
}
