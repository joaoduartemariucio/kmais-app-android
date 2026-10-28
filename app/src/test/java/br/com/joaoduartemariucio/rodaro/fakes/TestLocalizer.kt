package br.com.joaoduartemariucio.rodaro.fakes

import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.presentation.common.Localizer
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class TestLocalizer(
    folder: String = "values",
    override val locale: Locale = Locale.US,
) : Localizer {
    private val strings: Map<String, String>
    private val plurals: Map<String, Map<String, String>>

    init {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(File(resourceRoot(), "$folder/strings.xml"))
        strings =
            document.getElementsByTagName("string").elements().associate { it.getAttribute("name") to unescape(it.textContent) }
        plurals =
            document.getElementsByTagName("plurals").elements().associate { plural ->
                plural.getAttribute("name") to
                    plural.getElementsByTagName("item").elements().associate { it.getAttribute("quantity") to unescape(it.textContent) }
            }
    }

    override fun string(
        id: Int,
        vararg args: Any,
    ): String {
        val template = strings[name(R.string::class.java, id)] ?: error("Missing string $id")
        return if (args.isEmpty()) template else String.format(locale, template, *args)
    }

    override fun plural(
        id: Int,
        count: Int,
        vararg args: Any,
    ): String {
        val forms = plurals[name(R.plurals::class.java, id)] ?: error("Missing plural $id")
        val template = (if (count == 1) forms["one"] else null) ?: forms.getValue("other")
        return String.format(locale, template, *args)
    }

    companion object {
        fun resourceRoot(): File = listOf(File("src/main/res"), File("app/src/main/res")).first { it.exists() }

        private fun name(
            type: Class<*>,
            id: Int,
        ): String = type.fields.first { it.getInt(null) == id }.name

        private fun unescape(text: String): String = text.replace("\\'", "'").replace("\\\"", "\"")

        private fun org.w3c.dom.NodeList.elements(): List<Element> = (0 until length).map { item(it) as Element }
    }
}
