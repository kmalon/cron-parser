class CronFormatRecognizer(
    private val standardCronFormat: StandardCronFormat = StandardCronFormat()
) {
    fun getParser(argument: RawParserArgument): CronArgumentsParser =
        if (standardCronFormat.isCorrectFormat(argument)) {
            StandardCronParser(standardCronFormat)
        } else {
            throw NotSupportedCronFormat()
        }
}

class NotSupportedCronFormat(message: String = "Not supported cron parser format.") :
    ParserException(message)