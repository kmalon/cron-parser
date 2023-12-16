import mu.KotlinLogging

fun main(args: Array<String>) {
    LOGGER.logger.info("\nProgram arguments: ${args.joinToString()}\n\n")

    CronParserFactory
        .create()
        .parseAndPrint(args)
}

object LOGGER {
    val logger = KotlinLogging.logger {}
}