package com.warrior.evolutionary.computation.pcms

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * Created by warrior on 12/14/16.
 */
fun main(args: Array<String>) {
    val reader = BufferedReader(InputStreamReader(System.`in`))
    val n = reader.nextInt()
    val array = IntArray(n)
    val random = Random()

    var result: Int
    do {
        random.nextBinaryArray(array)
        println(array.toBitString())
        result = reader.nextInt()
    } while (result != n && result != n / 2)

    if (result == n) {
        return
    }

    array[0] = array[0] xor 1
    val b = IntArray(n)
    b[0] = 1
    for (i in 1..n - 1) {
        array[i] = array[i] xor 1
        println(array.toBitString())
        result = reader.nextInt()
        if (result == n) {
            return
        }
        b[i] = if (result == n / 2) 0 else 1
        array[i] = array[i] xor 1
    }
    array[0] = array[0] xor 1

    for (i in b.indices) {
        array[i] = array[i] xor b[i]
    }
    println(array.toBitString())
    result = reader.nextInt()
    if (result != n) {
        for (i in array.indices) {
            array[i] = array[i] xor 1
        }
        println(array.toBitString())
    }
}

private fun Random.nextBinaryArray(dst: IntArray) {
    for (i in dst.indices) {
        dst[i] = nextInt(2)
    }
}
private fun BufferedReader.nextInt(): Int = readLine().toInt()
private fun IntArray.toBitString(): String = joinToString("")
