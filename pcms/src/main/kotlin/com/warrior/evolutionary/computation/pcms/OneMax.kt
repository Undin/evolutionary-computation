package com.warrior.evolutionary.computation.pcms

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by warrior on 12/14/16.
 */
fun main(args: Array<String>) {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val n = reader.nextInt()

    val answer = IntArray(n)
    println(answer.toBitString())

    var currentResult = reader.nextInt()
    if (currentResult != n) {
        for (i in answer.indices) {
            answer[i] = 1
            println(answer.toBitString())
            val newResult = reader.nextInt()
            if (newResult < currentResult) {
                answer[i] = 0
            } else {
                currentResult = newResult
            }
            if (currentResult == n) {
                break
            }
        }
    }
}

private fun BufferedReader.nextInt(): Int = readLine().toInt()
private fun IntArray.toBitString(): String = joinToString("")
