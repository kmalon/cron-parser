# RUN
## using gradle
>./gradlew run --args="\"set_args\""

sample call:

`./gradlew run --args="\"*/20 */6 */7 * * ./command\""`

## using jar
build project:

`./gradlew build`

and then run:
>java -jar ./build/libs/cron-parser-1.0-SNAPSHOT-standalone.jar "set_args"

sample call:

`java -jar ./build/libs/cron-parser-1.0-SNAPSHOT-standalone.jar "*/20 */6 */7 * * ./command"`