interface CronArgumentsParser {
    fun parseFor(argument: RawParserArgument): ParsedArguments
}

class StandardCronFormat {
    companion object {
        val STANDARD_FORMAT_REGEX = "^([^\\s]+) ([^\\s]+) ([^\\s]+) ([^\\s]+) ([^\\s]+) ([^\\s]+)\$".toRegex()
    }

    fun isCorrectFormat(argument: RawParserArgument): Boolean =
        STANDARD_FORMAT_REGEX.matches(argument.value)

    fun get(argument: RawParserArgument): List<String> =
        STANDARD_FORMAT_REGEX.find(argument.value)?.groupValues?.drop(1) ?: throw NotFoundArgumentsException()
}

class StandardCronParser(
    private val standardCronFormat: StandardCronFormat,
    private val argumentFactory: ArgumentFactory = ArgumentFactory(),
) : CronArgumentsParser {

    override fun parseFor(argument: RawParserArgument): ParsedArguments {
        val arguments = standardCronFormat.get(argument)
        return argumentFactory.createFrom(arguments)
    }
}

enum class TimeArgumentType(val minValue: Int, val maxValue: Int, val occurrenceIndex: Int, val fullName: String) {
    MINUTE(0, 59, 0, "minute"),
    HOUR(0, 23, 1, "hour"),
    DAY_OF_MONTH(1, 31, 2, "day of month"),
    MONTH(1, 12, 3, "month"),
    DAY_OF_WEEK(0, 6, 4, "day of week");
}

enum class OtherArgumentType(val occurrenceIndex: Int, val fullName: String) {
    COMMAND(5, "command");
}


class ArgumentFactory {
    fun createFrom(
        arguments: List<String>
    ): ParsedArguments {
        val timeArguments =
            TimeArgumentType
                .entries
                .associateWith { argumentType ->
                    createTimeArgument(arguments[argumentType.occurrenceIndex], argumentType)
                }
                .let(::ParsedTimeArguments)

        val otherArguments =
            OtherArgumentType
                .entries
                .associateWith { argumentType ->
                    createOtherArgument(arguments[argumentType.occurrenceIndex])
                }
                .let(::ParsedOtherArguments)

        return ParsedArguments(timeArguments, otherArguments)
    }

    private fun createTimeArgument(argument: String, timeArgumentType: TimeArgumentType): Argument =
        when {
            NumberArgument.isEligibleFor(argument, timeArgumentType.minValue, timeArgumentType.maxValue) ->
                NumberArgument(argument)

            StarArgument.isEligibleFor(argument) ->
                StarArgument(timeArgumentType.minValue, timeArgumentType.maxValue)

            CommaArgument.isEligibleFor(argument, timeArgumentType.minValue, timeArgumentType.maxValue) ->
                CommaArgument(argument)

            SlashArgument.isEligibleFor(argument, timeArgumentType.minValue, timeArgumentType.maxValue) ->
                SlashArgument(argument, timeArgumentType.minValue, timeArgumentType.maxValue)

            DashNumberArgument.isEligibleFor(argument, timeArgumentType.minValue, timeArgumentType.maxValue) ->
                DashNumberArgument(argument)

            else ->
                throw UnrecognizedArgumentException(argument)

        }

    private fun createOtherArgument(argument: String): Argument =
        when {
            CommandArgument.isEligibleFor(argument) -> CommandArgument(argument)
            else -> throw UnrecognizedArgumentException(argument)
        }
}

data class ParsedTimeArguments(
    val value: Map<TimeArgumentType, Argument>
)

data class ParsedOtherArguments(
    val value: Map<OtherArgumentType, Argument>
)

data class ParsedArguments(
    val timeArguments: ParsedTimeArguments,
    val otherArguments: ParsedOtherArguments,
) {
    //TODO exception test
    fun getFor(timeArgumentType: TimeArgumentType): Argument =
        timeArguments.value[timeArgumentType] ?: throw NotFoundTimeArgumentException(timeArgumentType)

    //TODO exception test
    fun getFor(otherArgumentType: OtherArgumentType): Argument =
        otherArguments.value[otherArgumentType] ?: throw NotFoundOtherArgumentException(otherArgumentType)
}

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
        private val DASH_REGEX = "^[0-9]+${DASH}[0-9]+$".toRegex()

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
        private val SLASH_REGEX = "^\\*${SLASH}[0-9]+$".toRegex()

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
        fun isEligibleFor(argument: String): Boolean =
            true
    }
}

class UnrecognizedArgumentException(
    argument: String,
    message: String = "Can not parse argument: $argument"
) :
    ParserException(message)

class NotFoundArgumentsException(message: String = "Can not found any arguments in provided application invitation") :
    ParserException(message)

class NotFoundTimeArgumentException(
    argumentType: TimeArgumentType,
    message: String = "Can not found parsed argument for: ${argumentType.fullName}"
) :
    ParserException(message)

class NotFoundOtherArgumentException(
    otherArgumentType: OtherArgumentType,
    message: String = "Can not found parsed argument for: ${otherArgumentType.fullName}"
) :
    ParserException(message)