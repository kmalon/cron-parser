import cronparser.ConsolePrinter
import cronparser.Printer

class TestConsolePrinter(
    private val printer: Printer = ConsolePrinter()
) : Printer {

    lateinit var printedContent: String
    override fun print(content: String) {
        printedContent = content
        printer.print(content)
    }
}