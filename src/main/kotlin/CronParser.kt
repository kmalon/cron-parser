class CronParser(
    private val cronFormatRecognizer: CronFormatRecognizer,
    private val cronPrinter: CronPrinter,
) {
    fun parseAndPrint(args: Array<String>) {
        val arguments = args.let(::RawParserArgument)
        val parsedArguments = cronFormatRecognizer.getParser(arguments).parseFor(arguments)
        cronPrinter.print(parsedArguments)
    }
}

class RawParserArgument(
    args: Array<String>
) {
    val value: String = args.validateAndGet()

    private fun Array<String>.validateAndGet(): String =
        when {
            this.size != 1 -> throw WrongArgumentSizeException()
            this[0].isBlank() || this[0].isEmpty() -> throw EmptyOrBlankArgumentException()
            else -> this[0]
        }
}

class EmptyOrBlankArgumentException(message: String = "Application argument should not be blank or empty.") :
    ParserException(message)

class WrongArgumentSizeException(message: String = "Application argument should be passed as one argument.") :
    ParserException(message)

open class ParserException(message: String) : RuntimeException(message)