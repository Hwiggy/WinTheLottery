import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS")
object WinTheLottery {
    private val games = listOf(
        Powerball(),
        MegaMillions()
    )

    @JvmStatic fun main(args: Array<String>) {
        val scanner = Scanner(System.`in`)
        println("Available Games:")
        games.forEachIndexed { ind, it -> println("$ind: ${it.name}") }
        print("Which game would you like to play? ")
        val gameIndex = kotlin.runCatching { scanner.nextInt() }.getOrNull() ?: run {
            println("Invalid game chosen, picking game 0")
            0
        }
        val chosenGame = games[gameIndex]
        println("Reading NYS Lottery information...")
        val raw = chosenGame.drawings()
        println("Lottery information received.")
        print("How many days should be considered for analysis? 0 for all. ")
        val probabilityModel = HashMap<Int, HashMap<Int, Int>>()
        val days = runCatching { scanner.nextInt() }.getOrNull() ?: 0
        val data = if (days <= 0) raw else {
            val instant = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
            raw.filter { it.date().toInstant().isAfter(instant) }
        }
        data.forEach {
            val numbers = it.numbers()
            numbers.forEachIndexed { position, number ->
                probabilityModel.computeIfAbsent(position, ::HashMap).compute(number) {
                    _, weight -> weight?.plus(1) ?: 1
                }
            }
        }
        val topNumbers = HashMap<Int, List<Int>>()
        probabilityModel.forEach { (position, model) ->
            topNumbers[position] = model.entries.sortedByDescending { it.value }.map { it.key }
        }

        print("How many games do you want to generate? ")
        val games = scanner.nextInt()
        val length = games.toString().length
        val max = topNumbers.minOf { (_, v) -> v.size }
        repeat(games) { game ->
            print("Game ${"%0${length}d".format(game + 1)}: ")
            val set = StringBuilder().let {
                repeat(6) { position ->
                    it.append(
                        "%02d ".format(
                            topNumbers[position]!![if (game >= max) game % max else game]
                        )
                    )
                }
                it.trim()
            }
            print(set)
            val setNumbers = set.split(" ").map(String::toInt)
            val match = raw.find {
                it.numbers() == setNumbers
            }
            if (match != null) { print(" Previous Winner!") }
            print("\n")
        }
    }
}