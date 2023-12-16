interface CronArgumentsParser {
    fun parseFor(argument: RawParserArgument): ParsedArguments
}

class StandardCronFormat {
    companion object {
        val standardFormatRegex = "(.*) (.*) (.*) (.*) (.*) (.*)".toRegex()
    }

    fun isCorrectFormat(argument: RawParserArgument): Boolean =
        standardFormatRegex.matches(argument.value)

    fun get(argument: RawParserArgument): List<String> =
        standardFormatRegex.find(argument.value)?.groupValues?.drop(1) ?: throw NotFoundArgumentsException()
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

        fun isEligibleFor(argument: String, minValue: Int, maxValue: Int): Boolean {
            val arguments: List<String> = argument.split(COMMA)
            return argument.contains(COMMA) && arguments[0].toInt() in minValue..maxValue && arguments[1].toInt() in minValue..maxValue
        }
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

        fun isEligibleFor(argument: String, minValue: Int, maxValue: Int): Boolean {
            val arguments: List<String> = argument.split(DASH)
            return argument.contains(DASH)
                    && hasExactlyOneDash(arguments)
                    && argumentsAreNumbers(arguments)
                    && secondNumberIsBigger(arguments)
                    && arguments[0].toInt() in minValue..maxValue
                    && arguments[1].toInt() in minValue..maxValue
        }

        private fun hasExactlyOneDash(arguments: List<String>) = arguments.size == 2

        private fun argumentsAreNumbers(arguments: List<String>) =
            arguments.filter { it.toIntOrNull() != null }.size == 2

        private fun secondNumberIsBigger(arguments: List<String>): Boolean {
            val numbersArguments = arguments.map { it.toInt() }
            return numbersArguments[0] < numbersArguments[1]
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
    private val values: List<String> = step
        .let { IntRange(minValue, (maxValue / it)) }
        //todo
        .mapIndexed { index, argument ->
            index * step
        }
        .map { it.toString() }


    override fun getValues(): List<String> =
        values

    companion object {
        const val SLASH: String = "/"

        fun isEligibleFor(argument: String, minValue: Int, maxValue: Int): Boolean {
            val arguments: List<String> = argument.split(SLASH)
            return argument.contains(SLASH)
                    && hasExactlyOneSlash(arguments)
                    && secondArgumentIsNumber(arguments[1])
                    && firstArgumentIsStar(arguments[0])
                    && arguments[1].toInt() in minValue..maxValue
        }

        private fun hasExactlyOneSlash(arguments: List<String>): Boolean = arguments.size == 2

        private fun firstArgumentIsStar(argument: String): Boolean = argument == "*"

        private fun secondArgumentIsNumber(argument: String): Boolean = argument.toIntOrNull() != null
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