import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource


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

    @ParameterizedTest
    @EnumSource(WrongSeparatorsScenario::class)
    fun `should throw exception when arguments are incorrect`(scenario: WrongSeparatorsScenario) {
        //given
        val inputArgs: Array<String> =
            arrayOf("${scenario.minute} ${scenario.hour} ${scenario.dayOfMonth} ${scenario.month} ${scenario.dayOfWeek} /path/command")
        val testConsolePrinter = TestConsolePrinter()
        val cronParser = CronParserFactory.create(printer = testConsolePrinter)

        //when
        assertThrows<UnrecognizedArgumentException> {
            cronParser.parseAndPrint(inputArgs)
        }
    }
}

enum class WrongSeparatorsScenario(
    val minute: String,
    val hour: String,
    val dayOfMonth: String,
    val month: String,
    val dayOfWeek: String
) {
    MINUTE_WITH_DOUBLED_SLASH("*/1/2", "*", "*", "*", "*"),
    HOUR_WITH_DOUBLED_SLASH("*", "*/1/2", "*", "*", "*"),
    DAY_OF_MONTH_WITH_DOUBLED_SLASH("*", "*", "*/1/2", "*", "*"),
    MONTH_WITH_DOUBLED_SLASH("*", "*", "*", "*/1/2", "*"),
    DAY_OF_WEEK_WITH_DOUBLED_SLASH("*", "*", "*", "*", "*/1/2"),
    MINUTE_WITH_DOUBLED_DASH("1-3-5", "*", "*", "*", "*"),
    HOUR_WITH_DOUBLED_DASH("*", "1-3-5", "*", "*", "*"),
    DAY_OF_MONTH_WITH_DOUBLED_DASH("*", "*", "1-3-5", "*", "*"),
    MONTH_WITH_DOUBLED_DASH("*", "*", "*", "1-3-5", "*"),
    DAY_OF_WEEK_WITH_DOUBLED_DASH("*", "*", "*", "*", "1-3-5"),
    MINUTE_WITH_COMMA_DASH("1,3-5", "*", "*", "*", "*"),
    HOUR_WITH_COMMA_DASH("*", "1,3-5", "*", "*", "*"),
    DAY_OF_MONTH_WITH_COMMA_DASH("*", "*", "1,3-5", "*", "*"),
    MONTH_WITH_COMMA_DASH("*", "*", "*", "1,3-5", "*"),
    DAY_OF_WEEK_WITH_COMMA_DASH("*", "*", "*", "*", "1,3-5"),
    MINUTE_WITH_SLASH_DASH("1/3-5", "*", "*", "*", "*"),
    HOUR_WITH_SLASH_DASH("*", "1/3-5", "*", "*", "*"),
    DAY_OF_MONTH_WITH_SLASH_DASH("*", "*", "1/3-5", "*", "*"),
    MONTH_WITH_SLASH_DASH("*", "*", "*", "1/3-5", "*"),
    DAY_OF_WEEK_WITH_SLASH_DASH("*", "*", "*", "*", "1/3-5"),
    MINUTE_WITH_COMA_SLASH("1,3-5", "*", "*", "*", "*"),
    HOUR_WITH_COMA_SLASH("*", "1,3-5", "*", "*", "*"),
    DAY_OF_MONTH_WITH_COMA_SLASH("*", "*", "1,3-5", "*", "*"),
    MONTH_WITH_COMA_SLASH("*", "*", "*", "1,3-5", "*"),
    DAY_OF_WEEK_WITH_COMA_SLASH("*", "*", "*", "*", "1,3-5"),
    MINUTE_STARTED_FROM_SLASH("/1", "*", "*", "*", "*"),
    HOUR_STARTED_FROM_SLASH("*", "/1", "*", "*", "*"),
    DAY_OF_MONTH_STARTED_FROM_SLASH("*", "*", "/1", "*", "*"),
    MONTH_STARTED_FROM_SLASH("*", "*", "*", "/1", "*"),
    DAY_OF_WEEK_STARTED_FROM_SLASH("*", "*", "*", "*", "/1"),
    MINUTE_STARTED_FROM_COMMA(",1", "*", "*", "*", "*"),
    HOUR_STARTED_FROM_COMMA("*", ",1", "*", "*", "*"),
    DAY_OF_MONTH_STARTED_FROM_COMMA("*", "*", ",1", "*", "*"),
    MONTH_STARTED_FROM_COMMA("*", "*", "*", ",1", "*"),
    DAY_OF_WEEK_STARTED_FROM_COMMA("*", "*", "*", "*", ",1"),
    MINUTE_STARTED_FROM_DASH("-1", "*", "*", "*", "*"),
    HOUR_STARTED_FROM_DASH("*", "-1", "*", "*", "*"),
    DAY_OF_MONTH_STARTED_FROM_DASH("*", "*", "-1", "*", "*"),
    MONTH_STARTED_FROM_DASH("*", "*", "*", "-1", "*"),
    DAY_OF_WEEK_STARTED_FROM_DASH("*", "*", "*", "*", "-1"),
    MINUTE_ENDED_WITH_DASH("1-", "*", "*", "*", "*"),
    HOUR_ENDED_WITH_DASH("*", "1-", "*", "*", "*"),
    DAY_OF_MONTH_ENDED_WITH_DASH("*", "*", "1-", "*", "*"),
    MONTH_ENDED_WITH_DASH("*", "*", "*", "1-", "*"),
    DAY_OF_WEEK_ENDED_WITH_DASH("*", "*", "*", "*", "1-"),
    MINUTE_ENDED_WITH_COMMA("1,", "*", "*", "*", "*"),
    HOUR_ENDED_WITH_COMMA("*", "1,", "*", "*", "*"),
    DAY_OF_MONTH_ENDED_WITH_COMMA("*", "*", "1,", "*", "*"),
    MONTH_ENDED_WITH_COMMA("*", "*", "*", "1,", "*"),
    DAY_OF_WEEK_ENDED_WITH_COMMA("*", "*", "*", "*", "1,"),
    MINUTE_ENDED_WITH_SLASH("1/", "*", "*", "*", "*"),
    HOUR_ENDED_WITH_SLASH("*", "1/", "*", "*", "*"),
    DAY_OF_MONTH_ENDED_WITH_SLASH("*", "*", "1/", "*", "*"),
    MONTH_ENDED_WITH_SLASH("*", "*", "*", "1/", "*"),
    DAY_OF_WEEK_ENDED_WITH_SLASH("*", "*", "*", "*", "1/"),
}