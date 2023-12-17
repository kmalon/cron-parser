package cronparser

import LOGGER
import cronparser.standardformat.OtherArgumentType
import cronparser.standardformat.ParsedArguments
import cronparser.standardformat.TimeArgumentType

interface CronPrinter {
    fun print(arguments: ParsedArguments)
}

class StringStringPrinter(
    private val printer: Printer
) : CronPrinter {
    override fun print(arguments: ParsedArguments) =
        printer.print(
            """
            ${TimeArgumentType.MINUTE.fullName.prepareNameForPrint()}${
                arguments.getFor(TimeArgumentType.MINUTE).getValues().printArguments()
            }
            ${TimeArgumentType.HOUR.fullName.prepareNameForPrint()}${
                arguments.getFor(TimeArgumentType.HOUR).getValues().printArguments()
            }
            ${TimeArgumentType.DAY_OF_MONTH.fullName.prepareNameForPrint()}${
                arguments.getFor(TimeArgumentType.DAY_OF_MONTH).getValues().printArguments()
            }
            ${TimeArgumentType.MONTH.fullName.prepareNameForPrint()}${
                arguments.getFor(TimeArgumentType.MONTH).getValues().printArguments()
            }
            ${TimeArgumentType.DAY_OF_WEEK.fullName.prepareNameForPrint()}${
                arguments.getFor(TimeArgumentType.DAY_OF_WEEK).getValues().printArguments()
            }
            ${OtherArgumentType.COMMAND.fullName.prepareNameForPrint()}${
                arguments.getFor(OtherArgumentType.COMMAND).getValues().printArguments()
            }
            """.trimIndent()
        )


    private fun String.prepareNameForPrint(): String {
        if (this.length > MAX_LENGTH) {
            return this.dropLast(this.length - MAX_LENGTH)
        }
        return this + (MAX_LENGTH - this.length).provideSpacesIfNeeded()
    }

    private fun Int.provideSpacesIfNeeded(): String =
        if (this > 0)
            " ".repeat(this)
        else
            ""

    private fun List<String>.printArguments() =
        this.joinToString(" ")

    companion object {
        const val MAX_LENGTH = 15
    }
}

interface Printer {
    fun print(content: String)
}

class ConsolePrinter : Printer {
    override fun print(content: String) {
        LOGGER.logger.info { content }
    }

}