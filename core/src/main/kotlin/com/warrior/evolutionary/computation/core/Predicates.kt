package com.warrior.evolutionary.computation.core

/**
 * Created by warrior on 18.04.16.
 */
open class IterationPredicate(val iteration: Int) : Predicate {

    var iterationPass: Int = 0

    override fun test(value: Double): Boolean {
        if (iterationPass < iteration) {
            iterationPass++
            return false
        }
        return true
    }
}

class PrecisionPredicate(val realValue: Double, val precision: Double) : IterationPredicate(200) {
    override fun test(value: Double): Boolean {
        if (Math.abs(realValue - value) < precision) {
            return true
        }
        return super.test(value)
    }
}
