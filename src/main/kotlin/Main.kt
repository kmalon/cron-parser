fun main(args: Array<String>) {

    println("Program arguments: ${args.joinToString()}")

    CronParser().parseAndPrint(args)
}