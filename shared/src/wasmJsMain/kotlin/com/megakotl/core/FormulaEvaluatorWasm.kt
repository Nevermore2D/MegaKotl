package com.megakotl.core

import kotlin.math.*

internal actual fun formulaSyntaxError(expression: String): String? =
    try {
        WasmFormulaParser(expression).parse()
        null
    } catch (error: IllegalArgumentException) {
        error.message ?: "Некорректная формула"
    }

internal actual fun createFormulaEvaluator(expression: String): (Double) -> Double =
    WasmFormulaParser(expression).parse()

/**
 * Small expression parser with no Java dependencies. Supports numbers (including 1e-3),
 * x, pi, e, + - * / ^, parentheses and common one-argument elementary functions.
 * Deliberately rejects unsupported mXparser syntax instead of silently miscalculating.
 */
internal class WasmFormulaParser(private val source: String) {
    private var pos = 0

    fun parse(): (Double) -> Double {
        val result = expression()
        spaces()
        require(pos == source.length) { "Неожиданный символ '${source[pos]}' в позиции $pos" }
        return result
    }

    private fun expression(): (Double) -> Double {
        var left = term()
        while (true) {
            spaces()
            val op = when {
                take('+') -> '+'
                take('-') -> '-'
                else -> return left
            }
            val previous = left
            val right = term()
            left = when (op) {
                '+' -> { x -> previous(x) + right(x) }
                else -> { x -> previous(x) - right(x) }
            }
        }
    }

    private fun term(): (Double) -> Double {
        var left = unary()
        while (true) {
            spaces()
            val op = when {
                take('*') -> '*'
                take('/') -> '/'
                // mXparser accepts 2x and 2(x+1); offer the same familiar shorthand.
                pos < source.length && (source[pos] == '(' || source[pos].isLetter() || source[pos] == 'π') -> '*'
                else -> return left
            }
            val previous = left
            val right = unary()
            left = when (op) {
                '*' -> { x -> previous(x) * right(x) }
                else -> { x -> previous(x) / right(x) }
            }
        }
    }

    private fun unary(): (Double) -> Double {
        spaces()
        return when {
            take('+') -> unary()
            take('-') -> {
                val value = unary()
                val negated: (Double) -> Double = { x -> -value(x) }
                negated
            }
            else -> power()
        }
    }

    // Right-associative exponentiation: 2^3^2 = 2^(3^2).
    private fun power(): (Double) -> Double {
        val left = atom()
        spaces()
        if (!take('^')) return left
        val right = unary()
        return { x -> left(x).pow(right(x)) }
    }

    private fun atom(): (Double) -> Double {
        spaces()
        if (take('(')) {
            val expression = expression()
            spaces()
            require(take(')')) { "Не закрыта скобка в позиции $pos" }
            return expression
        }
        require(pos < source.length) { "Неожиданный конец формулы" }
        if (source[pos].isDigit() || source[pos] == '.') {
            val number = number()
            return { _ -> number }
        }
        val start = pos
        while (pos < source.length && (source[pos].isLetter() || source[pos] == 'π')) pos++
        require(pos > start) { "Неизвестный символ '${source[pos]}' в позиции $pos" }
        val identifier = source.substring(start, pos).lowercase()
        when (identifier) {
            "x" -> return { x -> x }
            "pi", "π" -> return { _ -> PI }
            "e" -> return { _ -> E }
        }
        spaces()
        require(take('(')) { "После $identifier ожидается (" }
        val operand = expression()
        spaces()
        require(take(')')) { "Не закрыта функция $identifier" }
        return when (identifier) {
            "sin" -> { x -> sin(operand(x)) }
            "cos" -> { x -> cos(operand(x)) }
            "tan", "tg" -> { x -> tan(operand(x)) }
            "asin", "arcsin" -> { x -> asin(operand(x)) }
            "acos", "arccos" -> { x -> acos(operand(x)) }
            "atan", "arctan" -> { x -> atan(operand(x)) }
            "sinh" -> { x -> sinh(operand(x)) }
            "cosh" -> { x -> cosh(operand(x)) }
            "tanh" -> { x -> tanh(operand(x)) }
            "sqrt" -> { x -> sqrt(operand(x)) }
            "abs" -> { x -> abs(operand(x)) }
            "exp" -> { x -> exp(operand(x)) }
            "ln" -> { x -> ln(operand(x)) }
            "log", "log10" -> { x -> log10(operand(x)) }
            "floor" -> { x -> floor(operand(x)) }
            "ceil" -> { x -> ceil(operand(x)) }
            "sign", "sgn" -> { x -> sign(operand(x)) }
            else -> error("Функция $identifier не поддерживается в браузере")
        }
    }

    private fun number(): Double {
        val start = pos
        while (pos < source.length && source[pos].isDigit()) pos++
        if (take('.')) {
            while (pos < source.length && source[pos].isDigit()) pos++
        }
        if (pos < source.length && (source[pos] == 'e' || source[pos] == 'E')) {
            val exponentStart = pos
            pos++
            if (pos < source.length && (source[pos] == '-' || source[pos] == '+')) pos++
            val digits = pos
            while (pos < source.length && source[pos].isDigit()) pos++
            if (pos == digits) pos = exponentStart
        }
        return source.substring(start, pos).toDoubleOrNull()
            ?: error("Некорректное число в позиции $start")
    }

    private fun spaces() {
        while (pos < source.length && source[pos].isWhitespace()) pos++
    }

    private fun take(ch: Char): Boolean {
        if (pos >= source.length || source[pos] != ch) return false
        pos++
        return true
    }
}
