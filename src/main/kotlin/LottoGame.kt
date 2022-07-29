import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Date

abstract class LottoGame<Drawing : LottoGame.Drawing>(val name: String, drawingType: Class<out Drawing>) {
    /**
     * The OpenData API link for this dataset
     */
    abstract val database: String

    /**
     * The most recent date the lottery format changed
     */
    abstract val dateFilter: Date

    /**
     * Certain lotteries have specific drawing data available; we can handle that correctly here
     */
    private val typeToken = TypeToken.getParameterized(List::class.java, drawingType)

    /**
     * Traditionally speaking, lottery games have a specific date at which their format changes to decrease the probability of winning.
     * Using the date filter, we can filter out data that used to be valid but now is not.
     */
    private val query by lazy {
        "?\$where=draw_date%3E%22${DATE_FORMAT.format(dateFilter)}%22"
    }

    /**
     * Connects to the OpenData JSON document with the correct date filter and parses the drawing information
     */
    fun drawings(): List<Drawing> {
        val httpClient: HttpClient = HttpClient.newHttpClient()
        val req = HttpRequest.newBuilder(URI("$database$query")).GET().build()
        val json = httpClient.send(req, HttpResponse.BodyHandlers.ofString())
        return Gson().fromJson(json.body(), typeToken.type)
    }

    /**
     * Data imperative to a drawing record; represents a set of winning numbers and a draw date
     */
    interface Drawing {
        fun date(): Date
        fun numbers(): List<Int>
    }
}

/**
 * Represents the Powerball game.
 * Powerball format most recently changed on 10/4/2015.
 */
class Powerball : LottoGame<Powerball.Drawing>("Powerball", Drawing::class.java) {
    override val database = "https://data.ny.gov/resource/d6yy-54nr.json"
    override val dateFilter = Date(115, 10, 4)

    data class Drawing(
        private val draw_date: String,
        private val winning_numbers: String,
        private val multiplier: Int
    ) : LottoGame.Drawing {
        override fun date(): Date = DATE_FORMAT.parse(draw_date)
        override fun numbers() = winning_numbers.split(" ").map(String::toInt)
    }
}

/**
 * Represents the Mega Millions game.
 * Mega Millions format most recently changed on 10/28/2017.
 */
class MegaMillions : LottoGame<MegaMillions.Drawing>("Mega Millions", Drawing::class.java) {
    override val database = "https://data.ny.gov/resource/5xaw-6ayf.json"
    override val dateFilter = Date(117, 10, 28)

    data class Drawing(
        private val draw_date: String,
        private val winning_numbers: String,
        private val mega_ball: String,
        private val multiplier: Int
    ) : LottoGame.Drawing {
        override fun date(): Date = DATE_FORMAT.parse(draw_date)
        override fun numbers() = ("$winning_numbers $mega_ball").split(" ").map(String::toInt)
    }
}