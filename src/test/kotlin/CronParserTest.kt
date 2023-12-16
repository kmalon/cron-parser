import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test


class CronParserTest {

    @Test
    fun `should parse correctly all arguments with star (*)`() {
        //given
        val inputArgs: Array<String> = arrayOf("* * * * * /path/command")
        val testConsolePrinter = TestConsolePrinter()
        val cronParser = CronParserFactory.create(printer = testConsolePrinter)

        //when
        cronParser.parseAndPrint(inputArgs)

        //then
        Assertions.assertEquals(
            """
            minute         0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59
            hour           0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23
            day of month   1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31
            month          1 2 3 4 5 6 7 8 9 10 11 12
            day of week    0 1 2 3 4 5 6
            command        /path/command
        """.trimIndent(),
            testConsolePrinter.printedContent
        )
    }

    @Test
    fun `should parse correctly all arguments with comma (,)`() {
        //given
        val inputArgs: Array<String> =
            arrayOf("0,3,5,6,8,14,25,46,59 1,4,6,9,11,13,20 3,5,8,9,30 1,5,6,12 3,5 /path/command")
        val testConsolePrinter = TestConsolePrinter()
        val cronParser = CronParserFactory.create(printer = testConsolePrinter)

        //when
        cronParser.parseAndPrint(inputArgs)

        //then
        Assertions.assertEquals(
            """
            minute         0 3 5 6 8 14 25 46 59
            hour           1 4 6 9 11 13 20
            day of month   3 5 8 9 30
            month          1 5 6 12
            day of week    3 5
            command        /path/command
        """.trimIndent(),
            testConsolePrinter.printedContent
        )
    }

    @Test
    fun `should parse correctly all arguments with dash (-)`() {
        //given
        val inputArgs: Array<String> = arrayOf("13-27 0-10 7-27 1-5 5-6 /path/command")
        val testConsolePrinter = TestConsolePrinter()
        val cronParser = CronParserFactory.create(printer = testConsolePrinter)

        //when
        cronParser.parseAndPrint(inputArgs)

        //then
        Assertions.assertEquals(
            """
            minute         13 14 15 16 17 18 19 20 21 22 23 24 25 26 27
            hour           0 1 2 3 4 5 6 7 8 9 10
            day of month   7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27
            month          1 2 3 4 5
            day of week    5 6
            command        /path/command
        """.trimIndent(),
            testConsolePrinter.printedContent
        )
    }

    @Test
    fun `should parse correctly all arguments with slash`() {
        //given
        val inputArgs: Array<String> = arrayOf("*/15 */3 */10 */2 */1 /path/command")
        val testConsolePrinter = TestConsolePrinter()
        val cronParser = CronParserFactory.create(printer = testConsolePrinter)

        //when
        cronParser.parseAndPrint(inputArgs)

        //then
        Assertions.assertEquals(
            """
            minute         0 15 30 45
            hour           0 3 6 9 12 15 18 21
            day of month   1 11 21 31
            month          1 3 5 7 9 11
            day of week    0 1 2 3 4 5 6
            command        /path/command
        """.trimIndent(),
            testConsolePrinter.printedContent
        )
    }

    @Test
    fun `should parse correctly all arguments with number`() {
        //given
        val inputArgs: Array<String> = arrayOf("1 3 8 12 2 /path/command")
        val testConsolePrinter = TestConsolePrinter()
        val cronParser = CronParserFactory.create(printer = testConsolePrinter)

        //when
        cronParser.parseAndPrint(inputArgs)

        //then
        Assertions.assertEquals(
            """
            minute         1
            hour           3
            day of month   8
            month          12
            day of week    2
            command        /path/command
        """.trimIndent(),
            testConsolePrinter.printedContent
        )
    }
}