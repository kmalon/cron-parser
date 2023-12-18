package cronparser.standardformat

interface Argument {
    fun getValues(): List<String>
}

class NumberArgument(
    value: String
) : Argument {
    private val values: List<String> = listOf(value)

    override fun getValues(): List<String> =
        values

    companion object {
        fun isEligibleFor(argument: String, minValue: Int, maxValue: Int): Boolean =
            argument.toIntOrNull() != null && argument.toInt() in minValue..maxValue
    }
}

class StarArgument(
    minValue: Int,
    maxValue: Int,
) : Argument {
    private val values: List<String> = IntRange(minValue, maxValue).map { it.toString() }
    override fun getValues(): List<String> =
        values

    companion object {
        const val STAR: String = "*"

        fun isEligibleFor(argument: String): Boolean =
            argument == STAR
    }
}

class CommaArgument(
    value: String
) : Argument {
    private val values: List<String> = value.split(COMMA)
    override fun getValues(): List<String> =
        values

    companion object {
        const val COMMA: String = ","
        private val ONLY_COMMA_REGEX = "[^0-9,\\s]".toRegex()
        private val START_WITH_COMMA_REGEX = "^[^,.*]".toRegex()
        private val END_WITH_COMMA_REGEX = "[^.*,]\$".toRegex()

        fun isEligibleFor(argument: String, minValue: Int, maxValue: Int): Boolean {
            val arguments: List<String> = argument.split(COMMA)
            return argument.hasOnlyCommaAndDigits()
                    && arguments[0].toInt() in minValue..maxValue
                    && arguments[1].toInt() in minValue..maxValue
        }

        private fun String.hasOnlyCommaAndDigits() =
            !this.contains(ONLY_COMMA_REGEX)
                .or(!this.contains(START_WITH_COMMA_REGEX))
                .or(!this.contains(END_WITH_COMMA_REGEX))
    }
}

class DashNumberArgument(
    value: String
) : Argument {
    private val values: List<String> = value
        .split(DASH)
        .map { it.toInt() }
        .let {
            IntRange(it[0], it[1])
                .map { it.toString() }
        }

    override fun getValues(): List<String> =
        values

    companion object {
        const val DASH: String = "-"
        private val DASH_REGEX = "^[0-9]+$DASH[0-9]+$".toRegex()

        fun isEligibleFor(argument: String, minValue: Int, maxValue: Int): Boolean {
            val splitArguments: List<String> = argument.split(DASH)
            return argument.hasOnlyDashAndDigits()
                    && isRangeCorrect(splitArguments[0], splitArguments[1], minValue, maxValue)
        }

        private fun String.hasOnlyDashAndDigits(): Boolean =
            this.contains(DASH_REGEX)

        private fun isRangeCorrect(
            beforeDash: String,
            afterDash: String,
            minValue: Int,
            maxValue: Int
        ): Boolean {
            val beforeDashNumber = beforeDash.toInt()
            val afterDashNumber = afterDash.toInt()
            return beforeDashNumber <= afterDashNumber
                    && beforeDashNumber in minValue..maxValue
                    && afterDashNumber in minValue..maxValue
        }
    }
}

class SlashArgument(
    value: String,
    minValue: Int,
    maxValue: Int,
) : Argument {
    private val step: Int = value
        .split(SLASH)[1]
        .toInt()
    private val values: List<String> =
        IntRange(minValue, maxValue / step)
            .runningFold(minValue) { acc, _ -> acc + step }
            .takeWhile { it <= maxValue }
            .map { it.toString() }


    override fun getValues(): List<String> =
        values

    companion object {
        const val SLASH: String = "/"
        private val SLASH_REGEX = "^\\*$SLASH[0-9]+$".toRegex()

        fun isEligibleFor(argument: String, minValue: Int, maxValue: Int): Boolean {
            return argument
                .hasOnlySlashAndDigits()
                    && argument.split(SLASH)[1].toInt() in minValue..maxValue
        }

        private fun String.hasOnlySlashAndDigits(): Boolean =
            this.contains(SLASH_REGEX)
    }
}

class CommandArgument(
    value: String
) : Argument {

    private val values = listOf(value)

    override fun getValues(): List<String> =
        values

    companion object {
        fun isEligibleFor(): Boolean =
            true
    }
}