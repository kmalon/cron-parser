package cronparser.standardformat

import cronparser.ParserException
import cronparser.RawParserArgument

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
        STANDARD_FORMAT_REGEX.find(argument.value)?.groupValues?.drop(1)
            ?: throw NotFoundArgumentsException(argument.value)
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
            CommandArgument.isEligibleFor() -> CommandArgument(argument)
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
    fun getFor(timeArgumentType: TimeArgumentType): Argument =
        timeArguments.value[timeArgumentType] ?: throw NotFoundTimeArgumentException(timeArgumentType)

    fun getFor(otherArgumentType: OtherArgumentType): Argument =
        otherArguments.value[otherArgumentType] ?: throw NotFoundOtherArgumentException(otherArgumentType)
}

class UnrecognizedArgumentException(
    argument: String,
    message: String = "Can not parse argument: $argument"
) :
    ParserException(message)

class NotFoundArgumentsException(
    argument: String,
    message: String = "Can not parse argument: $argument"
) :
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