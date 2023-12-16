interface CronPrinter {
    fun print(arguments: ParsedArguments)
}

class ConsolePrinter : CronPrinter {
    override fun print(arguments: ParsedArguments) {
        TODO("Not yet implemented")
    }

    companion object {
        const val MAX_WIGHT = 15
    }
}