package com.warrior.evolutionary.computation.pcms

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * Created by warrior on 12/15/16.
 */
private const val DEBUG = false

fun main(args: Array<String>) {
    val interactor = Interactor
    val (n, c, w) = interactor.initValues()
    interactor.action("accept")

    // exclude all items
    var currentWeight = w
    while (currentWeight > 0) {
        val weight = interactor.nextInt()
        if (weight < currentWeight) {
            interactor.action("accept")
            currentWeight = weight
        } else {
            interactor.action("decline")
        }
    }

    // collect all items' weights
    val weightsMap = HashMap<Int, Int>()
    var collectedItems = 0
    while (collectedItems < n) {
        val weight = interactor.nextInt()
        if (weight > currentWeight) {
            weightsMap.merge(weight - currentWeight, 1) { old, new -> old + new }
            currentWeight = weight
            collectedItems++
            if (collectedItems != n) {
                interactor.action("accept")
            }
        } else {
            interactor.action("decline")
        }
    }

    // find answer
    val weightsList = ArrayList<Int>()
    for ((k, v) in weightsMap) {
        for (i in 1..v) {
            weightsList.add(k)
        }
    }
    val bestAnswer = solveKnapsackProblem(weightsList, c)
    if (bestAnswer.value == currentWeight) {
        interactor.action("stop")
        return
    } else {
        interactor.action("accept")
    }

    val toRemove = ArrayList(weightsList)
    for (w in bestAnswer.weights) {
        toRemove.remove(w)
    }

    // exclude all items which don't belong to answer
    while (true) {
        val weight = interactor.nextInt()
        if (weight == bestAnswer.value) {
            interactor.action("stop")
            break
        }
        if (weight < currentWeight) {
            val w = currentWeight - weight
            if (toRemove.remove(w)) {
                interactor.action("accept")
                currentWeight = weight
            } else {
                interactor.action("decline")
            }
        } else {
            interactor.action("decline")
        }
    }
}

private fun solveKnapsackProblem(weightsList: List<Int>, maxWeight: Int): Answer {
    var bestWeight = 0
    var set = 0

    for (i in 1 until (1 shl weightsList.size)) {
        var weight = 0
        for (j in 0 until weightsList.size) {
            if (i and (1 shl j) != 0) {
                weight += weightsList[j]
            }
        }
        if (weight <= maxWeight && weight > bestWeight) {
            bestWeight = weight
            set = i
        }
    }

    val weights = weightsList
            .filterIndexedTo(ArrayList()) { i, w -> set and (1 shl i) != 0 }
    return Answer(bestWeight, weights)
}

private data class Answer(val value: Int, val weights: List<Int>)

private object Interactor {

    private val random = Random()
    private val reader = BufferedReader(InputStreamReader(System.`in`))

    private val n: Int = random.nextInt(20) + 1
    private val c: Int = random.nextInt(1000000000 + 1)
    private val weights = IntArray(n) { random.nextInt(100000000) + 1 }

    private val currentState = BooleanArray(n) { random.nextBoolean() }
    private var currentWeight = weights.withIndex().map { if (currentState[it.index]) it.value else 0 }.sum()
    private var nextAction = random.nextInt(n)

    private var actionCount = 0

    fun initValues(): Triple<Int, Int, Int> {
        return if (DEBUG) {
            val w = currentWeight + delta()
            println("--> $n $c $w")
            Triple(n, c, w)
        } else {
            val values = reader.readLine().split(" ").map(String::toInt)
            Triple(values[0], values[1], values[2])
        }
    }

    fun nextInt(): Int {
        return if (DEBUG) {
            val delta = delta()
            println("--> ${currentWeight + delta}")
            currentWeight + delta
        } else {
            reader.readLine().toInt()
        }
    }

    fun action(action: String) {
        if (DEBUG) {
            println("<-- $action")
            actionCount++
            when (action) {
                "accept" -> {
                    val delta = delta()
                    currentState[nextAction] = !currentState[nextAction]
                    currentWeight += delta
                }
                "decline" -> {}
                "stop" -> {
                    val delta = delta()
                    currentState[nextAction] = !currentState[nextAction]
                    currentWeight += delta

                    val point = solveKnapsackProblem(weights.toList(), c)
                    if (point.value != currentWeight) {
                        throw IllegalStateException("$currentWeight is not optimal answer. ${point.value}")
                    }
                    println("actions: $actionCount")
                    System.exit(0)
                }
                else -> throw IllegalArgumentException("$action is illegal")
            }
            nextAction = random.nextInt(n)
        } else {
            println(action)
        }
    }

    private fun delta(): Int = if (currentState[nextAction]) -weights[nextAction] else weights[nextAction]
}
