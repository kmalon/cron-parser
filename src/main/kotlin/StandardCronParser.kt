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

enum class TimeArgumentType(val maxValue: Int, val occurrenceIndex: Int, val fullName: String) {
    MINUTE(60, 0, "minute"),
    HOUR(24, 1, "hour"),
    DAY_OF_MONTH(31, 2, "day of month"),
    MONTH(12, 3, "month"),
    DAY_OF_WEEK(7, 4, "day of week");
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
                .map { argumentType ->
                    val parsedArgument = createTimeArgument(arguments[argumentType.occurrenceIndex], argumentType)
                    argumentType to parsedArgument
                }
                .toMap()
                .let(::ParsedTimeArguments)

        val otherArguments =
            OtherArgumentType
                .entries
                .map { argumentType ->
                    val parsedArgument = createOtherArgument(arguments[argumentType.occurrenceIndex])
                    argumentType to parsedArgument
                }
                .toMap()
                .let(::ParsedOtherArguments)

        return ParsedArguments(timeArguments, otherArguments)
    }

    private fun createTimeArgument(argument: String, timeArgumentType: TimeArgumentType): Argument =
        when {
            NumberArgument.isEligibleFor(argument, timeArgumentType.maxValue) -> NumberArgument(argument)
            StarArgument.isEligibleFor(argument) -> StarArgument(timeArgumentType.maxValue)
            CommaArgument.isEligibleFor(argument, timeArgumentType.maxValue) -> CommaArgument(argument)
            SlashArgument.isEligibleFor(argument, timeArgumentType.maxValue) -> SlashArgument(
                argument,
                timeArgumentType.maxValue
            )

            DashNumberArgument.isEligibleFor(argument, timeArgumentType.maxValue) -> DashNumberArgument(argument)
            else -> throw UnrecognizedArgumentException(argument)

        }

    private fun createOtherArgument(argument: String): Argument =
        when {
            CommandArgument.isEligibleFor(argument) -> CommandArgument(argument)
            else -> throw UnrecognizedArgumentException(argument)
        }
}

data class ParsedArgument(
    val argument: Argument,
    val argumentName: String,
)

data class ParsedTimeArguments(
    val value: Map<TimeArgumentType, Argument>
)

data class ParsedOtherArguments(
    val value: Map<OtherArgumentType, Argument>
)

data class ParsedArguments(
    val timeArguments: ParsedTimeArguments,
    val otherArguments: ParsedOtherArguments,
)

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
        fun isEligibleFor(argument: String, maxValue: Int): Boolean =
            argument.toIntOrNull() != null && argument.toInt() <= maxValue
    }
}

class StarArgument(
    maxValue: Int,
) : Argument {
    private val values: List<String> = IntRange(0, maxValue).map { it.toString() }
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

        fun isEligibleFor(argument: String, maxValue: Int): Boolean {
            val arguments: List<String> = argument.split(COMMA)
            return argument.contains(COMMA) && hasExactlyOneComma(arguments) && arguments[0].toInt() <= maxValue && arguments[1].toInt() <= maxValue
        }

        private fun hasExactlyOneComma(arguments: List<String>) = arguments.size == 2
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

        fun isEligibleFor(argument: String, maxValue: Int): Boolean {
            val arguments: List<String> = argument.split(DASH)
            return argument.contains(DASH)
                    && hasExactlyOneDash(arguments)
                    && argumentsAreNumbers(arguments)
                    && secondNumberIsBigger(arguments)
                    && arguments[0].toInt() <= maxValue
                    && arguments[1].toInt() <= maxValue
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
    maxValue: Int
) : Argument {
    private val step: Int = value
        .split(SLASH)[1]
        .toInt()
    private val values: List<String> = step
        .let { IntRange(0, (maxValue / it) - 1) }
        .mapIndexed { index, argument ->
            index * step
        }
        .map { it.toString() }


    override fun getValues(): List<String> =
        values

    companion object {
        const val SLASH: String = "/"

        fun isEligibleFor(argument: String, maxValue: Int): Boolean {
            val arguments: List<String> = argument.split(SLASH)
            return argument.contains(SLASH)
                    && hasExactlyOneSlash(arguments)
                    && secondArgumentIsNumber(arguments[1])
                    && firstArgumentIsStar(arguments[0])
                    && arguments[1].toInt() <= maxValue
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