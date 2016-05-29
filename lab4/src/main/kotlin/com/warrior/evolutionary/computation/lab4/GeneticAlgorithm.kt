package com.warrior.evolutionary.computation.lab4

import com.github.jabbalaci.graphviz.GraphViz
import com.warrior.evolutionary.computation.core.DoubleRange
import com.warrior.evolutionary.computation.core.Predicate
import com.warrior.evolutionary.computation.lab4.Expr.*
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.StreamSupport

/**
 * Created by warrior on 24.04.16.
 */
class GeneticAlgorithm(val fn: Fn, val fitnessFunction: (Expr) -> Double) {

    private val random = Random()
    private val hasAllVariablesMask: Int
    private val dateFormat = SimpleDateFormat("HH-mm-ss_dd-MM-YY")

    private val variablesNumber: Int
    private val variables: List<Terminal.Variable>
    private val consts: List<Terminal.Const>
    private val unary: List<(Expr) -> Unary>
    private val binary: List<(Expr, Expr) -> Binary>

    var maxDepth = 15

    var survivedPart = 0.05
    var mutationProbability = 0.3
    var subtreeMutationProbability = 0.5
    var tournamentProbability = 0.8

    var a = 1.5

    private var iteration = 0
    private lateinit var logDir: File
    private lateinit var logger: PrintWriter

    init {
        variablesNumber = fn.variableNumber()
        variables = fn.variables()
        consts = fn.consts()
        unary = fn.unaryGenerators()
        binary = fn.binaryGenerators()
        hasAllVariablesMask = (0 until variablesNumber).map { 1 shl it }.sum()
    }

    fun search(populationSize: Int, predicate: Predicate): List<Individual> {
        iteration = 0
        logDir = File("lab4/logs/${dateFormat.format(Date())}")
        logDir.mkdirs()
        logger = PrintWriter(File(logDir, "log.log"))

        val pointsList = ArrayList<Individual>(populationSize + 1)

        // calculate probabilities for selection
        val probabilities = ArrayList<Double>(populationSize)
        probabilities.add(probability(0, populationSize))
        for (i in 1..populationSize - 1) {
            probabilities.add(probabilities.last() + probability(i, populationSize))
        }

        var population: List<Individual> = IntStream.range(0, populationSize)
                .parallel()
                .mapToObj {
                    val depth = random.nextInt(maxDepth - variablesNumber) + variablesNumber + 1
                    val chromosome = generate(depth, true)
                    Individual(chromosome, fitnessFunction(chromosome))
                }
                .sorted()
                .collect(Collectors.toList<Individual>())

        printCurrentState(population)
        pointsList.add(population[0])

        val survivedNumber = Math.max(1, (survivedPart * populationSize + 0.5).toInt())
        val childrenNumber = ((populationSize - survivedNumber) / 2) * 2

        while (!predicate.test(population[0].value)) {
            iteration++
            val childrenChromosomes = ArrayList<Expr>(childrenNumber)

            for (i in 1..childrenNumber / 2) {
                val (firstParent, secondParent) = getParents(population, probabilities)

                val (firstChromosome, secondChromosome) = crossover(firstParent.chromosome, secondParent.chromosome)
                val firstChild = withProbability(mutationProbability) { firstChromosome.mutation() } ?: firstChromosome
                val secondChild = withProbability(mutationProbability) { secondChromosome.mutation() } ?: secondChromosome
                childrenChromosomes.add(firstChild)
                childrenChromosomes.add(secondChild)
            }

            for (i in 0 until survivedNumber) {
                val child = population[i].chromosome.mutation()
                childrenChromosomes.add(child)
            }

            val children = StreamSupport.stream(childrenChromosomes.spliterator(), true)
                    .map { Individual(it, fitnessFunction(it)) }
                    .collect(Collectors.toList<Individual>())


            population = selection(population, children, survivedNumber)

            printCurrentState(population)
            pointsList.add(population[0])
        }

        return pointsList
    }

    private fun getParents(population: List<Individual>, probabilities: ArrayList<Double>): Pair<Individual, Individual> {
        val rand1 = random.nextDouble()
        val rand2 = random.nextDouble()

        val firstParentIndex = findParent(probabilities, rand1)
        val secondParentIndex = findParent(probabilities, rand2)

        val firstParent = population[firstParentIndex]
        val secondParent = population[secondParentIndex]
        return Pair(firstParent, secondParent)
    }

    private fun probability(i: Int, populationSize: Int): Double =
            (a - (a - (2 - a)) * i / ((populationSize - 1))) / populationSize

    private fun findParent(probabilities: List<Double>, key: Double): Int {
        val index = Collections.binarySearch(probabilities, key, { o1, o2 -> o1.compareTo(o2) })
        return if (index >= 0) { index } else { -index - 1 }
    }

    private fun selection(currentPopulation: List<Individual>, children: List<Individual>, survivedNumber: Int): List<Individual> {
        val populationSize = currentPopulation.size
        val newPopulation = ArrayList<Individual>(populationSize)
        val other = ArrayList<Individual>(children.size + populationSize - survivedNumber)
        for ((i, ch) in currentPopulation.withIndex()) {
            if (i < survivedNumber) {
                if (newPopulation.isEmpty() || newPopulation.last().value != ch.value) {
                    newPopulation.add(ch)
                }
            } else {
                other.add(ch)
            }
        }
        other += children

        val places = populationSize - newPopulation.size
        for (j in 1..places) {
            val first = random.nextInt(other.size)
            val second = random.nextInt(other.size)
            val index = if (random.nextDouble() < tournamentProbability) { first } else { second }
            newPopulation.add(other.removeAt(index))
        }
        newPopulation.sort()
        return newPopulation
    }

    private fun crossover(first: Expr, second: Expr): Pair<Expr, Expr> = subtreeCrossover(first, second)

    private fun subtreeCrossover(first: Expr, second: Expr): Pair<Expr, Expr> {
        while (true) {
            val firstIndex = random.nextInt(first.size)
            val (firstSubexpr, depth) = first.getSubexpr(firstIndex)
            if (depth == 0 && first.depth > 1) {
                continue
            }
            val suitableExprs = second.getAllSuitableSubExprs(firstSubexpr, maxDepth - depth)
            if (suitableExprs.isEmpty()) {
                continue
            }
            val i = random.nextInt(suitableExprs.size)
            val (secondSubexpr, secondIndex) = suitableExprs[i]
            val firstChild = first.replaceSubtree(firstIndex, secondSubexpr)
            checkConstraints(firstChild)
            val secondChild = second.replaceSubtree(secondIndex, firstSubexpr)
            checkConstraints(secondChild)
            return Pair(firstChild, secondChild)
        }
    }

    private fun Expr.mutation(): Expr {
        return withProbability(subtreeMutationProbability) { subtreeMutation(this) } ?: pointMutation(this)
    }

    private fun <T> withProbability(probability: Double, block: () -> T): T? {
        return if (random.nextDouble() < probability) { block() } else { null }
    }

    private fun pointMutation(chromosome: Expr): Expr {
        val index = random.nextInt(chromosome.size)
        val (expr, depth) = chromosome.getSubexpr(index)
        return when (expr) {
            is Terminal -> chromosome.replaceTerminalNode(index) { randomVariable() }
            is Unary -> chromosome.replaceUnaryNode(index) { e -> randomUnary(e) }
            is Binary -> chromosome.replaceBinaryNode(index) { lhs, rhs -> randomBinary(lhs, rhs) }
        }
    }

    private fun subtreeMutation(chromosome: Expr): Expr {
        if (chromosome.size == 1) {
            return generate()
        }

        var index: Int
        var expr: Expr
        var depth: Int
        do {
            index = random.nextInt(chromosome.size)
            val (e, d) = chromosome.getSubexpr(index)
            expr = e
            depth = d
        } while (expr == chromosome)

        // TODO: wat??
        if (depth >= maxDepth) {
            return generate()
        }

        val newExpr = generate(maxDepth - depth)
        return chromosome.replaceSubtree(index, newExpr)
    }

    private fun Expr.getAllSuitableSubExprs(e: Expr, maxSubExprDepth: Int, currentDepth: Int = 0, startIndex: Int = 0): List<Pair<Expr, Int>> {
        val exprs = ArrayList<Pair<Expr, Int>>()
        if (currentDepth + e.depth <= maxDepth) {
            when (this) {
                is Terminal -> {
                    if (depth <= maxSubExprDepth) {
                        exprs.add(Pair(this, startIndex))
                    }
                }
                is Unary -> {
                    exprs.addAll(expr.getAllSuitableSubExprs(e, maxSubExprDepth, currentDepth + 1, startIndex))
                    if (depth <= maxSubExprDepth) {
                        exprs.add(Pair(this, startIndex + expr.size))
                    }
                }
                is Binary -> {
                    exprs.addAll(lhs.getAllSuitableSubExprs(e, maxSubExprDepth, currentDepth + 1, startIndex))
                    if (depth <= maxSubExprDepth) {
                        exprs.add(Pair(this, startIndex + lhs.size))
                    }
                    exprs.addAll(rhs.getAllSuitableSubExprs(e, maxSubExprDepth, currentDepth + 1, startIndex + lhs.size + 1))
                }
            }
        }
        return exprs
    }

    private fun Expr.getSubexpr(index: Int, currentDepth: Int = 0): Pair<Expr, Int> {
        return when (this) {
            is Binary -> {
                if (index < lhs.size) {
                    lhs.getSubexpr(index, currentDepth + 1)
                } else if (index == lhs.size) {
                    Pair(this, currentDepth)
                } else {
                    rhs.getSubexpr(index - lhs.size - 1, currentDepth + 1)
                }
            }
            is Unary -> {
                if (index + 1 == size) {
                    Pair(this, currentDepth)
                } else {
                    expr.getSubexpr(index, currentDepth + 1)
                }
            }
            is Terminal -> {
                if (index == 0) {
                    Pair(this, currentDepth)
                } else {
                    throw IllegalStateException()
                }
            }
        }
    }

    private fun generate(depth: Int = maxDepth, checkVariables: Boolean = false): Expr {
        var e: Expr
        do {
            e = if (random.nextBoolean()) { generateFull(depth) } else { generateGrow(depth) }
            checkConstraints(e)
        } while (checkVariables && e.variables() != hasAllVariablesMask)
        return e
    }

    private fun checkConstraints(e: Expr) {
        if (e.depth > maxDepth) {
            throw IllegalStateException("maxDepth: $maxDepth, size: ${e.depth}")
        }
    }

    private fun Expr.variables(): Int = when (this) {
        is Expr.Binary -> lhs.variables() or rhs.variables()
        is Expr.Unary -> expr.variables()
        is Expr.Terminal.Const -> 0
        is Expr.Terminal.Variable -> 1 shl index
        else -> 0
    }

    private fun generateFull(maxDepth: Int): Expr {
        if (maxDepth < 0) {
            throw IllegalArgumentException("maxDepth: $maxDepth")
        }
        return if (maxDepth == 1) {
            randomTerminal()
        } else {
            if (random.nextBoolean()) {
                val expr = generateFull(maxDepth - 1)
                randomUnary(expr)
            } else {
                val lhs = generateFull(maxDepth - 1)
                val rhs = generateFull(maxDepth - 1)
                randomBinary(lhs, rhs)
            }
        }
    }

    private fun generateGrow(maxDepth: Int): Expr {
        if (maxDepth < 0) {
            throw IllegalArgumentException("maxDepth: $maxDepth")
        }
        return if (maxDepth == 1) {
            randomVariable()
        } else {
            val x = random.nextInt(4)
            when (x) {
                0 -> randomVariable()
                1 -> randomConst()
                2 -> {
                    val expr = generateGrow(maxDepth - 1)
                    randomUnary(expr)
                }
                3 -> {
                    val lhs = generateGrow(maxDepth - 1)
                    val rhs = generateGrow(maxDepth - 1)
                    randomBinary(lhs, rhs)
                }
                else -> throw IllegalStateException("unreachable state")
            }
        }
    }

    private fun randomVariable(): Expr.Terminal.Variable = variables.randomElement()
    private fun randomConst(): Expr.Terminal.Const = consts.randomElement()
    private fun randomTerminal(): Expr.Terminal = if (random.nextBoolean()) { randomVariable() } else { randomConst() }
    private fun randomUnary(expr: Expr): Expr.Unary = unary.randomElement()(expr)
    private fun randomBinary(lhs: Expr, rhs: Expr): Expr.Binary = binary.randomElement()(lhs, rhs)

    private fun <T> List<T>.randomElement(): T = get(random.nextInt(size))

    private fun printCurrentState(population: List<Individual>) {
        println("iteration: $iteration")
        for (i in 0..4) {
            println(population[i])
        }
        println(".......")
        println(population.last())
        println("--------------------------------")

        logger.println("iteration: $iteration, value: ${population[0].value}, expr: ${population[0].chromosome.toJsonString()}")
        logger.flush()
        saveExpressionTree(population[0].chromosome, File(logDir, "$iteration.png"))
    }

    private fun saveExpressionTree(e: Expr, out: File) {

        fun Expr.toGraph(gv: GraphViz, startIndex: Int) {
            val nodeName = name(startIndex)
            when (this) {
                is Unary -> {
                    gv.addln("$nodeName -> ${expr.name(startIndex)}")
                    expr.toGraph(gv, startIndex)
                }
                is Binary -> {
                    gv.addln("$nodeName -> ${lhs.name(startIndex)}")
                    gv.addln("$nodeName -> ${rhs.name(startIndex + lhs.size + 1)}")
                    lhs.toGraph(gv, startIndex)
                    rhs.toGraph(gv, startIndex + lhs.size + 1)
                }
            }
        }

        val gv = GraphViz()
        gv.addln(gv.start_graph())
        e.toGraph(gv, 0)
        gv.addln(gv.end_graph())
        gv.writeGraphToFile(gv.getGraph(gv.dotSource, "png", "dot"), out)
    }

    private fun Expr.index(startIndex: Int = 0): Int {
        return when (this) {
            is Expr.Binary -> startIndex + lhs.size
            is Expr.Unary -> startIndex + expr.size
            is Expr.Terminal -> startIndex
        }
    }

    private fun Expr.name(startIndex: Int): String {
        val name = when (this) {
            is Binary.Add -> "+"
            is Binary.Sub -> "-"
            is Binary.Mul -> "*"
            is Binary.Div -> "/"
            is Binary.Pow -> "^"
            is Unary.UnaryMinus -> "-"
            is Unary.Sin -> "sin"
            is Unary.Cos -> "cos"
            is Unary.Exp -> "exp"
            is Unary.Abs -> "abs"
            is Terminal.Const -> value.toString()
            is Terminal.Variable -> "x${index + 1}"
            else -> throw IllegalStateException()
        }
        return "\"$name (${index(startIndex)})\""
    }

    data class Individual(val chromosome: Expr, val value: Double): Comparable<Individual> {
        override fun compareTo(other: Individual): Int = value.compareTo(other.value)
        override fun toString(): String = "$value, $chromosome"
    }

    interface Fn {
        operator fun invoke(xs: DoubleArray): Double
        fun variableNumber(): Int
        fun range(i: Int): DoubleRange
        fun consts(): List<Terminal.Const>
        fun unaryGenerators(): List<(Expr) -> Unary>
        fun binaryGenerators(): List<(Expr, Expr) -> Binary>
        fun variables(): List<Terminal.Variable> = (0 until variableNumber()).map { Terminal.Variable(it) }
    }
}
