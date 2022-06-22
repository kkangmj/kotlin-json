package deserialization

import ru.yole.jkid.JsonName
import ru.yole.jkid.deserialization.deserialize
import java.io.BufferedReader
import java.io.InputStreamReader

data class BirthOfDate(val month: String, val year: String)
data class Author(
    @JsonName("nameOfAuthor") val name: String,
    val birthOfDate: BirthOfDate,
    val publishedCountry: List<String>
)

data class Book(val title: String, val publisher: String, val author: Author)

fun main(args: Array<String>) {
    val json =
        """{"publisher": "Acon", "title": "Catch-22", "author": {"nameOfAuthor": "J. Heller", "publishedCountry": ["Korea", "US"], "birthOfDate": {"month": "4", "year": "1998"}}}"""
    println(deserialize<Book>(json))
}
