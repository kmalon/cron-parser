package cronparser

class CronParserFactory {
    companion object {
        fun create(
            cronFormatRecognizer: CronFormatRecognizer = CronFormatRecognizer(),
            printer: Printer = ConsolePrinter(),
            cronPrinter: CronPrinter = StringStringPrinter(printer),
        ): CronParser =
            CronParser(cronFormatRecognizer, cronPrinter)
    }
}