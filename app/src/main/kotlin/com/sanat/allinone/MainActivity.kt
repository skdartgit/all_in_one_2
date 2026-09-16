package com.sanat.allinone

import android.app.*
import android.content.*
import android.graphics.*
import android.graphics.drawable.*
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.text.*
import android.text.style.ForegroundColorSpan
import android.view.*
import android.text.method.TransformationMethod
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.*
import org.json.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

private const val PREF = "all_in_one_data"
private const val PIN_KEY = "pin"
private const val DATE_FMT = "dd-MM-yyyy"

private data class Note(
    var id: Long,
    var title: String,
    var date: String,
    var desc: String
)

private data class CodeItem(
    var id: Long,
    var title: String,
    var desc: String,
    var lang: String,
    var code: String
)

private data class Account(
    var id: Long,
    var name: String,
    var user: String,
    var pass: String,
    var other1: String,
    var other2: String
)

private data class SavedFile(
    var id: Long,
    var title: String,
    var fileName: String,
    var path: String,
    var mime: String
)

private data class Asset(
    var id: Long,
    var name: String,
    var category: String
)

private data class Investment(
    var id: Long,
    var assetId: Long,
    var date: String,
    var amount: Double
)

private data class Angel(
    var id: Long,
    var date: String,
    var amount: Double
)

private class Store(
    private val c: Context
) {

    private val p =
        c.getSharedPreferences(
            PREF,
            Context.MODE_PRIVATE
        )

    private fun arr(k: String): JSONArray =
        try {
            JSONArray(
                p.getString(
                    k,
                    "[]"
                )
            )
        } catch (_: Exception) {
            JSONArray()
        }

    private fun put(
        k: String,
        a: JSONArray
    ) {
        p.edit()
            .putString(k, a.toString())
            .apply()
    }

    /*
     * ID generation must never reuse an ID that already exists in the
     * restored/current data. This is especially important after restoring
     * a backup because the old version did not restore the next-ID counters.
     */
    private fun next(k: String): Long {

        val a =
            arr(k)

        var maxId = 0L

        for (i in 0 until a.length()) {
            val id =
                a.optJSONObject(i)
                    ?.optLong("id", 0L)
                    ?: 0L

            if (id > maxId) {
                maxId = id
            }
        }

        val storedNext =
            p.getLong(
                "next_$k",
                1L
            )

        val n =
            maxOf(
                storedNext,
                maxId + 1L
            )

        p.edit()
            .putLong(
                "next_$k",
                n + 1L
            )
            .apply()

        return n
    }

    /*
     * Older versions could create duplicate IDs after a restore because
     * next-ID counters were not restored. Duplicate IDs can make an Edit
     * operation update a different item and can make a newly-created Asset
     * inherit the investments of an older Asset with the same ID.
     *
     * Keep the first occurrence of an ID unchanged and give every later
     * duplicate a fresh ID. For Assets, investments remain attached to the
     * first/original occurrence, preventing a newly-created duplicate Asset
     * from displaying those investments.
     */
    fun repairIds() {

        val keys =
            arrayOf(
                "notes",
                "codes",
                "accounts",
                "files",
                "assets",
                "investments",
                "angels"
            )

        keys.forEach { k ->

            val a =
                arr(k)

            val used =
                HashSet<Long>()

            var nextId = 1L

            for (i in 0 until a.length()) {

                val o =
                    a.optJSONObject(i)
                        ?: continue

                var id =
                    o.optLong(
                        "id",
                        0L
                    )

                if (id <= 0L || used.contains(id)) {

                    while (used.contains(nextId)) {
                        nextId++
                    }

                    id = nextId

                    o.put(
                        "id",
                        id
                    )
                }

                used.add(id)

                if (id >= nextId) {
                    nextId = id + 1L
                }
            }

            if (a.length() > 0) {
                put(k, a)
            }

            p.edit()
                .putLong(
                    "next_$k",
                    nextId
                )
                .apply()
        }
    }

    fun pin(): String =
        p.getString(
            PIN_KEY,
            "123456"
        ) ?: "123456"

    fun setPin(v: String) {
        p.edit()
            .putString(
                PIN_KEY,
                v
            )
            .apply()
    }

    fun notes(): MutableList<Note> {
        val a = arr("notes")

        return MutableList(a.length()) {
            val o = a.getJSONObject(it)

            Note(
                o.getLong("id"),
                o.getString("title"),
                o.getString("date"),
                o.optString("desc")
            )
        }
    }

    fun saveNotes(
        x: List<Note>
    ) {
        val a = JSONArray()

        x.forEach {
            a.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("date", it.date)
                    put("desc", it.desc)
                }
            )
        }

        put("notes", a)
    }

    fun codes(): MutableList<CodeItem> {
        val a = arr("codes")

        return MutableList(a.length()) {
            val o = a.getJSONObject(it)

            CodeItem(
                o.getLong("id"),
                o.getString("title"),
                o.optString("desc"),
                o.optString("lang", "Kotlin"),
                o.optString("code")
            )
        }
    }

    fun saveCodes(
        x: List<CodeItem>
    ) {
        val a = JSONArray()

        x.forEach {
            a.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("desc", it.desc)
                    put("lang", it.lang)
                    put("code", it.code)
                }
            )
        }

        put("codes", a)
    }

    fun accounts(): MutableList<Account> {
        val a = arr("accounts")

        return MutableList(a.length()) {
            val o = a.getJSONObject(it)

            Account(
                o.getLong("id"),
                o.getString("name"),
                o.optString("user"),
                o.optString("pass"),
                o.optString("other1"),
                o.optString("other2")
            )
        }
    }

    fun saveAccounts(
        x: List<Account>
    ) {
        val a = JSONArray()

        x.forEach {
            a.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("user", it.user)
                    put("pass", it.pass)
                    put("other1", it.other1)
                    put("other2", it.other2)
                }
            )
        }

        put("accounts", a)
    }

    fun files(): MutableList<SavedFile> {
        val a = arr("files")

        return MutableList(a.length()) {
            val o = a.getJSONObject(it)

            SavedFile(
                o.getLong("id"),
                o.getString("title"),
                o.getString("fileName"),
                o.getString("path"),
                o.optString(
                    "mime",
                    "application/octet-stream"
                )
            )
        }
    }

    fun saveFiles(
        x: List<SavedFile>
    ) {
        val a = JSONArray()

        x.forEach {
            a.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("title", it.title)
                    put("fileName", it.fileName)
                    put("path", it.path)
                    put("mime", it.mime)
                }
            )
        }

        put("files", a)
    }

    fun assets(): MutableList<Asset> {
        val a = arr("assets")

        return MutableList(a.length()) {
            val o = a.getJSONObject(it)

            Asset(
                o.getLong("id"),
                o.getString("name"),
                o.getString("category")
            )
        }
    }

    fun saveAssets(
        x: List<Asset>
    ) {
        val a = JSONArray()

        x.forEach {
            a.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("category", it.category)
                }
            )
        }

        put("assets", a)
    }

    fun investments(): MutableList<Investment> {
        val a = arr("investments")

        return MutableList(a.length()) {
            val o = a.getJSONObject(it)

            Investment(
                o.getLong("id"),
                o.getLong("assetId"),
                o.getString("date"),
                o.getDouble("amount")
            )
        }
    }

    fun saveInvestments(
        x: List<Investment>
    ) {
        val a = JSONArray()

        x.forEach {
            a.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("assetId", it.assetId)
                    put("date", it.date)
                    put("amount", it.amount)
                }
            )
        }

        put("investments", a)
    }

    fun angels(): MutableList<Angel> {
        val a = arr("angels")

        return MutableList(a.length()) {
            val o = a.getJSONObject(it)

            Angel(
                o.getLong("id"),
                o.getString("date"),
                o.getDouble("amount")
            )
        }
    }

    fun saveAngels(
        x: List<Angel>
    ) {
        val a = JSONArray()

        x.forEach {
            a.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("date", it.date)
                    put("amount", it.amount)
                }
            )
        }

        put("angels", a)
    }

    fun id(k: String): Long =
        next(k)

    fun raw(): JSONObject =
        JSONObject().apply {

            put(
                "version",
                2
            )

            put(
                "pin",
                pin()
            )

            put(
                "notes",
                arr("notes")
            )

            put(
                "codes",
                arr("codes")
            )

            put(
                "accounts",
                arr("accounts")
            )

            val fa = JSONArray()

            files().forEach { f ->

                fa.put(
                    JSONObject().apply {

                        put(
                            "id",
                            f.id
                        )

                        put(
                            "title",
                            f.title
                        )

                        put(
                            "fileName",
                            f.fileName
                        )

                        put(
                            "mime",
                            f.mime
                        )

                        put(
                            "dataBase64",
                            try {
                                Base64.encodeToString(
                                    File(f.path).readBytes(),
                                    Base64.NO_WRAP
                                )
                            } catch (_: Exception) {
                                ""
                            }
                        )
                    }
                )
            }

            put(
                "files",
                fa
            )

            put(
                "assets",
                arr("assets")
            )

            put(
                "investments",
                arr("investments")
            )

            put(
                "angels",
                arr("angels")
            )
        }

    fun restore(
        o: JSONObject
    ) {

        p.edit()
            .putString(
                PIN_KEY,
                o.optString(
                    "pin",
                    "123456"
                )
            )
            .apply()

        listOf(
            "notes",
            "codes",
            "accounts",
            "assets",
            "investments",
            "angels"
        ).forEach {
            put(
                it,
                o.optJSONArray(it)
                    ?: JSONArray()
            )
        }

        val fa =
            o.optJSONArray("files")
                ?: JSONArray()

        val dir =
            File(
                c.filesDir,
                "user_files"
            ).apply {
                mkdirs()
            }

        val out = JSONArray()

        for (i in 0 until fa.length()) {

            val x =
                fa.getJSONObject(i)

            val name =
                "${System.currentTimeMillis()}_${i}_${x.optString("fileName", "file")}"

            val f =
                File(
                    dir,
                    name
                )

            try {
                f.writeBytes(
                    Base64.decode(
                        x.optString(
                            "dataBase64",
                            ""
                        ),
                        Base64.DEFAULT
                    )
                )
            } catch (_: Exception) {
            }

            out.put(
                JSONObject().apply {

                    put(
                        "id",
                        x.optLong(
                            "id",
                            i.toLong() + 1
                        )
                    )

                    put(
                        "title",
                        x.optString(
                            "title",
                            x.optString(
                                "fileName",
                                "File"
                            )
                        )
                    )

                    put(
                        "fileName",
                        x.optString(
                            "fileName",
                            "file"
                        )
                    )

                    put(
                        "mime",
                        x.optString(
                            "mime",
                            "application/octet-stream"
                        )
                    )

                    put(
                        "path",
                        f.absolutePath
                    )
                }
            )
        }

        put(
            "files",
            out
        )
    }
}

private class CodeEditText(
    c: Context
) : EditText(c) {

    private var busy = false

    /*
     * Once the user manually scrolls vertically, keep that vertical
     * position while typing. Horizontal cursor movement is still allowed.
     */
    private var userHasScrolled = false
    private var touchDownY = 0f
    private var dragging = false
    private val touchSlop =
        ViewConfiguration.get(c).scaledTouchSlop

    var language: String = "Kotlin"

    init {

        setTextSize(14f)

        setTypeface(
            Typeface.MONOSPACE
        )

        setPadding(
            18,
            18,
            18,
            18
        )

        gravity =
            Gravity.TOP or Gravity.START

        inputType =
            android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        setHorizontallyScrolling(true)

        setTextIsSelectable(true)

        addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    st: Int,
                    c: Int,
                    a: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    st: Int,
                    b: Int,
                    c: Int
                ) {
                    if (!busy) {
                        highlight()
                    }
                }

                override fun afterTextChanged(
                    e: Editable?
                ) {

                    if (busy || e == null) {
                        return
                    }

                    if (
                        e.isNotEmpty() &&
                        e[e.length - 1] == '\n'
                    ) {

                        busy = true

                        val before =
                            e.toString()
                                .dropLast(1)
                                .substringAfterLast('\n')

                        val n =
                            Regex(
                                "^[ \\t]*"
                            )
                                .find(before)
                                ?.value
                                ?.length
                                ?: 0

                        val extra =
                            if (
                                before
                                    .trimEnd()
                                    .endsWith("{") ||
                                before
                                    .trimEnd()
                                    .endsWith(":")
                            ) {
                                4
                            } else {
                                0
                            }

                        e.insert(
                            e.length,
                            " ".repeat(
                                n + extra
                            )
                        )

                        busy = false
                    }
                }
            }
        )
    }

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {
                touchDownY = event.y
                dragging = false

                parent?.requestDisallowInterceptTouchEvent(
                    true
                )
            }

            MotionEvent.ACTION_MOVE -> {

                if (
                    !dragging &&
                    kotlin.math.abs(
                        event.y - touchDownY
                    ) > touchSlop
                ) {
                    dragging = true
                    userHasScrolled = true
                }

                parent?.requestDisallowInterceptTouchEvent(
                    true
                )
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(
                    false
                )
            }
        }

        return super.onTouchEvent(event)
    }

    /*
     * Preserve the manually chosen vertical position, but let Android
     * move the cursor horizontally so long lines remain visible.
     */
    override fun bringPointIntoView(
        offset: Int
    ): Boolean {

        if (userHasScrolled) {

            val lockedY = scrollY

            val result =
                super.bringPointIntoView(
                    offset
                )

            scrollTo(
                scrollX,
                lockedY
            )

            post {
                scrollTo(
                    scrollX,
                    lockedY
                )
            }

            return result
        }

        return super.bringPointIntoView(
            offset
        )
    }

    fun highlight() {

        if (busy) {
            return
        }

        val s =
            text.toString()

        val sp =
            highlightCode(
                s,
                language
            )

        val pos =
            selectionStart

        val oldScrollX =
            scrollX

        val oldScrollY =
            scrollY

        busy = true

        setText(
            sp,
            TextView.BufferType.SPANNABLE
        )

        setSelection(
            pos.coerceAtMost(
                length()
            )
        )

        busy = false

        post {
            scrollTo(
                oldScrollX,
                oldScrollY
            )
        }
    }
}

private fun highlightCode(
    s: String,
    language: String
): SpannableString {

    val sp =
        SpannableString(s)

    fun col(
        regex: Regex,
        color: Int
    ) {
        regex.findAll(s).forEach {
            if (it.range.first <= it.range.last) {
                sp.setSpan(
                    ForegroundColorSpan(color),
                    it.range.first,
                    it.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    val keywordColor =
        Color.rgb(75, 55, 180)

    val commentColor =
        Color.rgb(90, 90, 90)

    val stringColor =
        Color.rgb(170, 65, 40)

    val tagColor =
        Color.rgb(0, 110, 130)

    val numberColor =
        Color.rgb(0, 125, 95)

    val annotationColor =
        Color.rgb(125, 70, 150)

    val lang =
        language.trim().lowercase(
            Locale.US
        )

    val keywords =
        when (lang) {

            "python" ->
                "\\b(and|as|assert|async|await|break|case|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|match|nonlocal|not|or|pass|raise|return|try|while|with|yield|True|False|None|self|print)\\b"

            "kotlin" ->
                "\\b(as|break|class|continue|data|do|else|false|for|fun|if|in|interface|is|lateinit|object|null|open|override|package|private|protected|public|return|sealed|super|this|throw|true|try|typealias|typeof|val|var|vararg|when|while|by|catch|constructor|delegate|dynamic|field|file|finally|get|import|init|param|property|receiver|set|setparam|where|actual|abstract|annotation|companion|const|crossinline|expect|external|final|infix|inline|inner|internal|noinline|out|operator|reified|suspend|tailrec|value|it)\\b"

            "java" ->
                "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|true|false|try|void|volatile|while|var|record|sealed|permits|yield)\\b"

            "javascript" ->
                "\\b(await|break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|false|finally|for|function|if|import|in|instanceof|let|new|null|return|static|super|switch|this|throw|true|try|typeof|var|void|while|with|yield|async|of|get|set)\\b"

            "c", "c++" ->
                "\\b(auto|bool|break|case|catch|char|class|const|constexpr|continue|default|delete|do|double|else|enum|explicit|extern|false|float|for|friend|if|inline|int|long|namespace|new|nullptr|operator|private|protected|public|register|return|short|signed|sizeof|static|struct|switch|template|this|throw|true|try|typedef|typename|union|unsigned|using|virtual|void|volatile|while)\\b"

            "sql" ->
                "\\b(SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|ALTER|DROP|TABLE|DATABASE|INDEX|JOIN|INNER|LEFT|RIGHT|FULL|OUTER|ON|AS|AND|OR|NOT|NULL|IS|IN|LIKE|BETWEEN|GROUP|BY|ORDER|HAVING|LIMIT|OFFSET|DISTINCT|UNION|ALL|PRIMARY|KEY|FOREIGN|REFERENCES|DEFAULT|CASE|WHEN|THEN|ELSE|END|COUNT|SUM|AVG|MIN|MAX)\\b"

            "shell" ->
                "\\b(if|then|else|elif|fi|for|while|in|do|done|case|esac|function|select|until|time|export|local|readonly|return|source|echo|printf|cd|pwd|exit|true|false)\\b"

            "json" ->
                "\\b(true|false|null)\\b"

            "css" ->
                "\\b(display|position|top|right|bottom|left|width|height|margin|padding|border|color|background|font|font-size|font-family|flex|grid|content|align-items|justify-content|overflow|opacity|z-index|transform|transition|animation)\\b"

            "html", "xml" ->
                ""

            else ->
                "\\b(fun|val|var|class|object|interface|if|else|when|for|while|return|import|package|public|private|protected|static|void|new|def|print|function|const|let|async|await)\\b"
        }

    if (keywords.isNotEmpty()) {
        col(
            Regex(
                keywords,
                if (lang == "sql")
                    setOf(RegexOption.IGNORE_CASE)
                else
                    emptySet()
            ),
            keywordColor
        )
    }

    when (lang) {

        "python", "shell" -> {
            col(
                Regex(
                    "#.*$",
                    RegexOption.MULTILINE
                ),
                commentColor
            )
        }

        "html", "xml" -> {
            col(
                Regex(
                    "<!--[\\s\\S]*?-->"
                ),
                commentColor
            )
        }

        "sql" -> {
            col(
                Regex(
                    "--.*$|/\\*[\\s\\S]*?\\*/",
                    RegexOption.MULTILINE
                ),
                commentColor
            )
        }

        "css" -> {
            col(
                Regex(
                    "/\\*[\\s\\S]*?\\*/"
                ),
                commentColor
            )
        }

        else -> {
            col(
                Regex(
                    "//.*$|/\\*[\\s\\S]*?\\*/",
                    RegexOption.MULTILINE
                ),
                commentColor
            )
        }
    }

    col(
        Regex(
            "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'"
        ),
        stringColor
    )

    if (
        lang == "html" ||
        lang == "xml"
    ) {
        col(
            Regex(
                "</?[A-Za-z][^>]*>|<!DOCTYPE[^>]*>",
                setOf(
                    RegexOption.IGNORE_CASE
                )
            ),
            tagColor
        )
    }

    if (lang == "css") {
        col(
            Regex(
                "#[0-9a-fA-F]{3,8}\\b"
            ),
            stringColor
        )
    }

    if (lang == "json") {
        col(
            Regex(
                "\"([^\"\\\\]|\\\\.)*\"(?=\\s*:)"
            ),
            tagColor
        )
    }

    if (lang == "shell") {
        col(
            Regex(
                "\\$[A-Za-z_][A-Za-z0-9_]*|\\$\\{[^}]+\\}"
            ),
            annotationColor
        )
    }

    if (
        lang == "python" ||
        lang == "kotlin" ||
        lang == "java" ||
        lang == "javascript"
    ) {
        col(
            Regex(
                "@[A-Za-z_][A-Za-z0-9_.]*"
            ),
            annotationColor
        )
    }

    col(
        Regex(
            "\\b(?:0x[0-9A-Fa-f]+|\\d+(?:\\.\\d+)?)\\b"
        ),
        numberColor
    )

    return sp
}


class MainActivity : Activity() {

    private class PinStarTransformation : TransformationMethod {

        override fun getTransformation(
            source: CharSequence,
            view: View
        ): CharSequence {

            return object : CharSequence {

                override val length: Int
                    get() = source.length

                override fun get(index: Int): Char = '*'

                override fun subSequence(
                    startIndex: Int,
                    endIndex: Int
                ): CharSequence =
                    "*".repeat(
                        (endIndex - startIndex).coerceAtLeast(0)
                    )

                override fun toString(): String =
                    "*".repeat(source.length)
            }
        }

        override fun onFocusChanged(
            view: View,
            sourceText: CharSequence,
            focused: Boolean,
            direction: Int,
            previouslyFocusedRect: Rect?
        ) {
        }
    }


    private lateinit var store: Store
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout

    private val text =
        Color.rgb(
            32,
            40,
            55
        )

    private val tabs =
        arrayOf(
            "My Notes",
            "My Coding",
            "My Accounts",
            "My Files",
            "Investment Details",
            "Export & Restore",
            "Control Panel"
        )

    private fun dp(
        n: Int
    ) =
        (
            n *
            resources.displayMetrics.density
        ).toInt()

    private fun tv(
        s: String,
        size: Float = 15f,
        bold: Boolean = false
    ): TextView =
        TextView(this).apply {

            setText(s)

            setTextSize(size)

            setTextColor(this@MainActivity.text)

            setTypeface(
                Typeface.DEFAULT,
                if (bold)
                    Typeface.BOLD
                else
                    Typeface.NORMAL
            )

            setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(5)
            )
        }

    private fun bgView(
        v: View,
        color: Int = Color.WHITE,
        r: Float = 14f
    ) {

        v.background =
            GradientDrawable().apply {

                setColor(color)

                cornerRadius =
                    dp(
                        r.toInt()
                    ).toFloat()

                setStroke(
                    dp(1),
                    Color.rgb(
                        225,
                        228,
                        235
                    )
                )
            }
    }

    private fun button(
        s: String,
        action: () -> Unit
    ): Button =
        Button(this).apply {

            setText(s)

            setTextSize(13f)

            setTextColor(Color.WHITE)

            setAllCaps(false)

            setPadding(
                dp(12),
                dp(5),
                dp(12),
                dp(5)
            )

            background =
                GradientDrawable().apply {
                    setColor(
                        Color.rgb(
                            63,
                            86,
                            211
                        )
                    )
                    cornerRadius =
                        dp(8).toFloat()
                }

            setOnClickListener {
                action()
            }
        }

    private fun edit(
        hint: String,
        value: String = "",
        password: Boolean = false
    ): EditText =
        EditText(this).apply {

            setHint(hint)

            setText(value)

            setTextSize(14f)

            setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )

            setSingleLine(false)

            bgView(
                this,
                Color.WHITE,
                10f
            )

            if (password) {
                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }

    private fun dateNow(): String =
        SimpleDateFormat(
            DATE_FMT,
            Locale.ENGLISH
        )
            .apply {
                timeZone =
                    TimeZone.getTimeZone(
                        "Asia/Kolkata"
                    )
            }
            .format(Date())

    override fun onCreate(
        b: Bundle?
    ) {

        super.onCreate(b)

        store =
            Store(this)

        // Repair any duplicate IDs left by an older backup/restore and
        // rebuild all next-ID counters before the user can edit/add data.
        store.repairIds()

        showPin()
    }

    private fun showPin() {

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        box.setPadding(
            dp(24),
            dp(20),
            dp(24),
            dp(20)
        )

        val title =
            tv(
                "All in One",
                25f,
                true
            )

        title.gravity =
            Gravity.CENTER

        box.addView(title)

        box.addView(
            tv(
                "Enter 6-digit PIN",
                15f
            )
        )

        val pin =
            edit(
                "PIN"
            )

        pin.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER or
            android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD

        pin.setSingleLine()

        // Always mask every PIN digit with a literal * character.
        pin.transformationMethod =
            PinStarTransformation()

        box.addView(
            pin,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        val enter =
            button("Unlock") {

                if (
                    pin.text.toString() ==
                    store.pin()
                ) {

                    buildShell()

                } else {

                    toast(
                        "Incorrect PIN"
                    )
                }
            }

        box.addView(
            enter,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            )
        )

        val lay =
            FrameLayout(this)

        lay.setBackgroundColor(
            Color.rgb(
                245,
                247,
                250
            )
        )

        lay.addView(
            box,
            FrameLayout.LayoutParams(
                -1,
                -2,
                Gravity.CENTER
            )
        )

        setContentView(lay)

        pin.requestFocus()
    }

    private fun buildShell() {

        root =
            LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setBackgroundColor(
            Color.rgb(
                245,
                247,
                250
            )
        )

        val header =
            LinearLayout(this)

        header.gravity =
            Gravity.CENTER_VERTICAL

        header.setPadding(
            dp(6),
            0,
            dp(6),
            0
        )

        header.background =
            GradientDrawable().apply {
                setColor(
                    Color.rgb(
                        63,
                        86,
                        211
                    )
                )
                cornerRadius =
                    dp(8).toFloat()
            }

        val title =
            tv(
                "All in One",
                22f,
                true
            )

        title.setTextColor(
            Color.WHITE
        )

        title.gravity =
            Gravity.CENTER

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            )
        )

        val exit =
            button("Exit") {
                finishAndRemoveTask()
            }
        
        exit.textSize = 17f

        header.addView(
            exit,
            LinearLayout.LayoutParams(
                dp(70),
                dp(40)
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            )
        )

        content =
            LinearLayout(this)

        content.orientation =
            LinearLayout.VERTICAL

        content.setPadding(
            dp(7),
            dp(5),
            dp(7),
            dp(4)
        )

        val scroll =
            ScrollView(this)

        scroll.addView(
            content
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val nav =
            HorizontalScrollView(this)

        val navrow =
            LinearLayout(this)

        tabs.forEachIndexed { i, s ->

            val b =
                button(s) {
                    showTab(i)
                }

            navrow.addView(
                b,
                LinearLayout.LayoutParams(
                    dp(
                        if (i == 4)
                            180
                        else
                            125
                    ),
                    dp(40)
                )
            )
        }

        nav.addView(
            navrow
        )

        root.addView(
            nav,
            LinearLayout.LayoutParams(
                -1,
                dp(44)
            )
        )

        val foot =
            tv(
                "@ 2026 Built & Developed by Sanat Dey",
                11f
            )

        foot.gravity =
            Gravity.CENTER

        foot.setTextColor(
            Color.WHITE
        )

        foot.background =
            GradientDrawable().apply {
                setColor(
                    Color.rgb(
                        63,
                        86,
                        211
                    )
                )
                cornerRadius =
                    dp(6).toFloat()
            }

        root.addView(
            foot,
            LinearLayout.LayoutParams(
                -1,
                dp(24)
            )
        )

        setContentView(root)

        showTab(0)
    }

    private fun clear(
        title: String
    ) {

        content.removeAllViews()

        val h =
            tv(
                title,
                18f,
                true
            )

        h.gravity =
            Gravity.CENTER

        content.addView(
            h,
            LinearLayout.LayoutParams(
                -1,
                dp(38)
            )
        )
    }

    private fun showTab(
        i: Int
    ) {

        when (i) {

            0 -> notesPage()
            1 -> codesPage()
            2 -> accountsPage()
            3 -> filesPage()
            4 -> investmentPage()
            5 -> backupPage()
            6 -> controlPage()
        }
    }

    private fun sectionButton(
        s: String,
        a: () -> Unit
    ) {

        val b =
            button(
                s,
                a
            )

        content.addView(
            b,
            LinearLayout.LayoutParams(
                -1,
                dp(44)
            ).apply {
                setMargins(
                    dp(3),
                    dp(4),
                    dp(3),
                    dp(4)
                )
            }
        )
    }

    // --------------------------------------------------------
    // FULL-HEIGHT LIST RECYCLERVIEW
    // --------------------------------------------------------

    /*
     * The four list pages live inside the main ScrollView.
     * A normal RecyclerView can report a clipped/limited height when it is
     * measured inside a ScrollView, which can make the first or last item
     * unreachable. This subclass explicitly asks RecyclerView to measure
     * all of its content when the parent gives it an unspecified height.
     */
    private class FullHeightRecyclerView(
        context: Context
    ) : RecyclerView(context) {

        override fun onMeasure(
            widthSpec: Int,
            heightSpec: Int
        ) {

            val mode =
                MeasureSpec.getMode(heightSpec)

            if (mode == MeasureSpec.UNSPECIFIED) {

                super.onMeasure(
                    widthSpec,
                    MeasureSpec.makeMeasureSpec(
                        Int.MAX_VALUE shr 2,
                        MeasureSpec.AT_MOST
                    )
                )

            } else {

                super.onMeasure(
                    widthSpec,
                    heightSpec
                )
            }
        }
    }

    // --------------------------------------------------------
    // LIST DISPLAY
    // --------------------------------------------------------

    private fun addList(
        rv: RecyclerView
    ) {
        content.addView(
            rv,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )
    }

    // --------------------------------------------------------
    // NOTES
    // --------------------------------------------------------

    private fun notesPage() {

        clear("My Notes")

        sectionButton(
            "Add Note"
        ) {
            noteDialog(null)
        }

        val list =
            store.notes()

        val rv =
            FullHeightRecyclerView(this)

        rv.layoutManager =
            LinearLayoutManager(this)

        rv.adapter =
            NoteAdapter(list)

        // Let the RecyclerView use only the height required by its content.
        // The page's outer ScrollView handles vertical scrolling.
        rv.isNestedScrollingEnabled = false
        rv.setPadding(0, 0, 0, dp(12))
        rv.clipToPadding = false

        addList(rv)

        // Long-press a saved note, drag it up/down, and release
        // to permanently save the new note order.
        attachDrag(
            rv,
            list
        ) {
            store.saveNotes(list)
        }
    }

    private inner class NoteAdapter(
        val data: MutableList<Note>
    ) : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(
            p: ViewGroup,
            t: Int
        ) =
            VH(row())

        override fun getItemCount() =
            data.size

        override fun onBindViewHolder(
            h: VH,
            pos: Int
        ) {

            val n =
                data[pos]

            h.title.text =
                "📓  ${n.title}  •  ${n.date}"

            h.edit.setOnClickListener {
                noteDialog(n)
            }

            h.itemView.setOnClickListener {

                h.detail.text =
                    descriptionToSpanned(n.desc)

                h.detail.visibility =
                    if (
                        h.detail.visibility ==
                        View.VISIBLE
                    )
                        View.GONE
                    else
                        View.VISIBLE
            }

            h.detail.text =
                descriptionToSpanned(n.desc)

            h.detail.visibility =
                View.GONE
        }
    }

    private fun row(): LinearLayout {

        val r =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        // Clear space between individual list cards,
                        // matching the Investment Details asset layout.
                        setMargins(
                            dp(3),
                            dp(5),
                            dp(3),
                            dp(5)
                        )
                    }
            }

        bgView(
            r,
            Color.WHITE,
            12f
        )

        // Thin black border around EACH list item.
        (r.background as? GradientDrawable)?.setStroke(
            dp(1),
            Color.BLACK
        )

        val top =
            LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            }

        val t =
            tv("").apply {
                id = 1001
                // Title section is tall enough to clearly display at least
                // three lines of a long title.
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            }

        top.addView(
            t,
            LinearLayout.LayoutParams(
                0,
                dp(96),
                1f
            )
        )

        val e =
            button("✎") {}.apply {
                id = 1002
            }

        top.addView(
            e,
            LinearLayout.LayoutParams(
                dp(65),
                dp(44)
            )
        )

        r.addView(top)

        val d =
            tv("").apply {
                id = 1003
            }

        d.setTextIsSelectable(true)

        d.setPadding(
            dp(14),
            0,
            dp(14),
            dp(12)
        )

        r.addView(d)

        return r
    }

    private fun codeRow(): LinearLayout {
    
        val r =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        // Clear space between individual list cards,
                        // matching the Investment Details asset layout.
                        setMargins(
                            dp(3),
                            dp(5),
                            dp(3),
                            dp(5)
                        )
                    }
            }
    
        bgView(
            r,
            Color.WHITE,
            12f
        )

        // Thin black border around EACH code item.
        (r.background as? GradientDrawable)?.setStroke(
            dp(1),
            Color.BLACK
        )
    
        // --------------------------------------------------------
        // CODE TITLE
        // --------------------------------------------------------
    
        val top =
            LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            }
    
        val t =
            tv("").apply {
                id = 1001
                // Title section is tall enough to clearly display at least
                // three lines of a long title.
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            }
    
        top.addView(
            t,
            LinearLayout.LayoutParams(
                0,
                dp(96),
                1f
            )
        )
    
        val e =
            button("✎") {}.apply {
                id = 1002
            }
    
        top.addView(
            e,
            LinearLayout.LayoutParams(
                dp(65),
                dp(44)
            )
        )
    
        r.addView(top)
    
        // --------------------------------------------------------
        // DETAILS CONTAINER
        // --------------------------------------------------------
    
        val details =
            LinearLayout(this).apply {
                id = 1007
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(
                    dp(14),
                    dp(4),
                    dp(14),
                    dp(12)
                )
            }
    
        // --------------------------------------------------------
        // CODE DESCRIPTION
        // --------------------------------------------------------
    
        val descLabel =
            tv(
                "Code Description",
                14f,
                true
            )
    
        details.addView(descLabel)
    
        val desc =
            tv("").apply {
                id = 1005
                typeface = Typeface.DEFAULT
                isSingleLine = false
                setHorizontallyScrolling(false)
                setPadding(
                    0,
                    dp(4),
                    0,
                    dp(14)
                )
            }
    
        details.addView(
            desc,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    
        // --------------------------------------------------------
        // ORIGINAL CODE
        // --------------------------------------------------------
    
        val codeLabel =
            tv(
                "Original Code",
                14f,
                true
            )
    
        details.addView(codeLabel)
    
        val hs =
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = true
                isFillViewport = false
                overScrollMode =
                    View.OVER_SCROLL_IF_CONTENT_SCROLLS
            }
    
        val code =
            tv("").apply {
                id = 1006
                typeface = Typeface.MONOSPACE
                setHorizontallyScrolling(true)
                isSingleLine = false
                setTextIsSelectable(true)
    
                setPadding(
                    0,
                    dp(4),
                    dp(14),
                    dp(12)
                )
    
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            }
    
        hs.addView(code)
    
        details.addView(
            hs,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    
        r.addView(
            details,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    
        return r
    }


    /*
     * Rich description support.
     *
     * Descriptions are still stored as String, so the existing data format
     * remains compatible. Bold and underline are stored as small inline
     * markers and converted back to Android spans when displayed/edited.
     *
     * The toolbar formats the complete current line, as requested.
     */
    /*
     * Rich description support.
     *
     * Text is still stored as String. Bold and underline are represented
     * by inline [b]/[/b] and [u]/[/u] markers so existing saved data remains
     * usable. Version 9 fixes toolbar focus/selection handling and formats
     * the exact selected text.
     *
     * B / U / B+U:
     *   - Single tap  -> apply formatting to the selected range.
     *   - No selection -> apply to the current line.
     *   - Double tap on the same toolbar button -> remove that formatting
     *     from the same selected range (or current line).
     *
     * The toolbar buttons never take focus. Their touch events are handled
     * directly, so tapping them cannot collapse the editor selection.
     */
    private fun descriptionToSpanned(
        value: String
    ): SpannableString {

        val clean = StringBuilder()
        val boldRanges = mutableListOf<Pair<Int, Int>>()
        val underlineRanges = mutableListOf<Pair<Int, Int>>()
        val boldStack = mutableListOf<Int>()
        val underlineStack = mutableListOf<Int>()

        var i = 0

        while (i < value.length) {
            when {
                value.startsWith("[b]", i) -> {
                    boldStack.add(clean.length)
                    i += 3
                }

                value.startsWith("[/b]", i) -> {
                    if (boldStack.isNotEmpty()) {
                        val a = boldStack.removeAt(boldStack.lastIndex)
                        if (a < clean.length) {
                            boldRanges.add(Pair(a, clean.length))
                        }
                    }
                    i += 4
                }

                value.startsWith("[u]", i) -> {
                    underlineStack.add(clean.length)
                    i += 3
                }

                value.startsWith("[/u]", i) -> {
                    if (underlineStack.isNotEmpty()) {
                        val a = underlineStack.removeAt(underlineStack.lastIndex)
                        if (a < clean.length) {
                            underlineRanges.add(Pair(a, clean.length))
                        }
                    }
                    i += 4
                }

                else -> {
                    clean.append(value[i])
                    i++
                }
            }
        }

        while (boldStack.isNotEmpty()) {
            val a = boldStack.removeAt(boldStack.lastIndex)
            if (a < clean.length) {
                boldRanges.add(Pair(a, clean.length))
            }
        }

        while (underlineStack.isNotEmpty()) {
            val a = underlineStack.removeAt(underlineStack.lastIndex)
            if (a < clean.length) {
                underlineRanges.add(Pair(a, clean.length))
            }
        }

        val sp = SpannableString(clean.toString())

        boldRanges.forEach { range ->
            val a = range.first.coerceIn(0, sp.length)
            val b = range.second.coerceIn(a, sp.length)
            if (a < b) {
                sp.setSpan(
                    android.text.style.StyleSpan(Typeface.BOLD),
                    a,
                    b,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        underlineRanges.forEach { range ->
            val a = range.first.coerceIn(0, sp.length)
            val b = range.second.coerceIn(a, sp.length)
            if (a < b) {
                sp.setSpan(
                    android.text.style.UnderlineSpan(),
                    a,
                    b,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return sp
    }

    private fun spannedToDescription(
        value: CharSequence
    ): String {

        if (value !is Spanned) {
            return value.toString()
        }

        val s = value.toString()
        if (s.isEmpty()) return ""

        val boldAt = BooleanArray(s.length)
        val underlineAt = BooleanArray(s.length)

        value.getSpans(
            0,
            value.length,
            android.text.style.StyleSpan::class.java
        ).forEach { span ->
            if (
                span.style == Typeface.BOLD ||
                span.style == Typeface.BOLD_ITALIC
            ) {
                val a = value.getSpanStart(span).coerceIn(0, s.length)
                val b = value.getSpanEnd(span).coerceIn(a, s.length)
                for (i in a until b) boldAt[i] = true
            }
        }

        value.getSpans(
            0,
            value.length,
            android.text.style.UnderlineSpan::class.java
        ).forEach { span ->
            val a = value.getSpanStart(span).coerceIn(0, s.length)
            val b = value.getSpanEnd(span).coerceIn(a, s.length)
            for (i in a until b) underlineAt[i] = true
        }

        val result = StringBuilder()
        var bold = false
        var underline = false

        for (i in s.indices) {
            val nextBold = boldAt[i]
            val nextUnderline = underlineAt[i]

            if (bold && !nextBold) result.append("[/b]")
            if (underline && !nextUnderline) result.append("[/u]")
            if (!bold && nextBold) result.append("[b]")
            if (!underline && nextUnderline) result.append("[u]")

            result.append(s[i])
            bold = nextBold
            underline = nextUnderline
        }

        if (underline) result.append("[/u]")
        if (bold) result.append("[/b]")

        return result.toString()
    }

    private fun richDescriptionEditor(
        initial: String,
        minLines: Int
    ): Pair<LinearLayout, EditText> {

        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(4)
                )
            }

        val toolbar =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

        val editor =
            EditText(this).apply {
                setText(
                    descriptionToSpanned(initial),
                    TextView.BufferType.SPANNABLE
                )
                setTextSize(14f)
                setTextColor(Color.DKGRAY)
                gravity = Gravity.TOP or Gravity.START
                this.minLines = minLines
                maxLines = 10
                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setPadding(
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
                )
            }

        var savedStart = -1
        var savedEnd = -1
        var lastButton: View? = null
        var lastTapTime = 0L

        fun rememberSelection() {
            val e = editor.text ?: return
            var a = editor.selectionStart
            var b = editor.selectionEnd

            if (a < 0 || b < 0) {
                a = 0
                b = 0
            }

            a = a.coerceIn(0, e.length)
            b = b.coerceIn(0, e.length)

            savedStart = minOf(a, b)
            savedEnd = maxOf(a, b)
        }

        fun getFormattingRange(): Pair<Int, Int>? {
            val e = editor.text ?: return null
            if (e.isEmpty()) return null

            var a = savedStart
            var b = savedEnd

            if (a < 0 || b < 0) {
                rememberSelection()
                a = savedStart
                b = savedEnd
            }

            a = a.coerceIn(0, e.length)
            b = b.coerceIn(0, e.length)

            if (a != b) {
                return Pair(a, b)
            }

            val text = e.toString()
            val cursor = a.coerceIn(0, text.length)

            val lineStart =
                text.lastIndexOf(
                    '\n',
                    (cursor - 1).coerceAtLeast(0)
                ).let { if (it < 0) 0 else it + 1 }

            val lineEndFound = text.indexOf('\n', cursor)
            val lineEnd =
                if (lineEndFound < 0) text.length else lineEndFound

            if (lineStart >= lineEnd) return null
            return Pair(lineStart, lineEnd)
        }

        fun restoreSelection() {
            val e = editor.text ?: return
            val a = savedStart.coerceIn(0, e.length)
            val b = savedEnd.coerceIn(0, e.length)
            try {
                editor.requestFocus()
                editor.setSelection(a, b)
            } catch (_: Exception) {
            }
        }

        fun removeBoldInRange(
            e: Spannable,
            start: Int,
            end: Int
        ) {
            val spans = e.getSpans(
                start,
                end,
                android.text.style.StyleSpan::class.java
            ).toList()

            spans.forEach { span ->
                if (
                    span.style != Typeface.BOLD &&
                    span.style != Typeface.BOLD_ITALIC
                ) return@forEach

                val sa = e.getSpanStart(span)
                val sb = e.getSpanEnd(span)
                e.removeSpan(span)

                if (sa < start) {
                    e.setSpan(
                        android.text.style.StyleSpan(Typeface.BOLD),
                        sa,
                        minOf(start, sb),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                if (sb > end) {
                    e.setSpan(
                        android.text.style.StyleSpan(Typeface.BOLD),
                        maxOf(end, sa),
                        sb,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }

        fun removeUnderlineInRange(
            e: Spannable,
            start: Int,
            end: Int
        ) {
            val spans = e.getSpans(
                start,
                end,
                android.text.style.UnderlineSpan::class.java
            ).toList()

            spans.forEach { span ->
                val sa = e.getSpanStart(span)
                val sb = e.getSpanEnd(span)
                e.removeSpan(span)

                if (sa < start) {
                    e.setSpan(
                        android.text.style.UnderlineSpan(),
                        sa,
                        minOf(start, sb),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                if (sb > end) {
                    e.setSpan(
                        android.text.style.UnderlineSpan(),
                        maxOf(end, sa),
                        sb,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }

        fun applyFormatting(
            bold: Boolean,
            underline: Boolean,
            remove: Boolean
        ) {
            val e = editor.text as? Spannable ?: return
            val range = getFormattingRange() ?: return
            val start = range.first
            val end = range.second

            if (remove) {
                if (bold) {
                    removeBoldInRange(e, start, end)
                }
                if (underline) {
                    removeUnderlineInRange(e, start, end)
                }
            } else {
                if (bold) {
                    e.setSpan(
                        android.text.style.StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                if (underline) {
                    e.setSpan(
                        android.text.style.UnderlineSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            editor.setSelection(
                savedStart.coerceIn(0, e.length),
                savedEnd.coerceIn(0, e.length)
            )
            editor.invalidate()
            editor.post {
                restoreSelection()
            }
        }

        fun tool(
            label: String,
            bold: Boolean,
            underline: Boolean
        ): Button {
            val button =
                Button(this).apply {
                    text = label
                    textSize = 12f
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isClickable = true
                    layoutParams =
                        LinearLayout.LayoutParams(
                            dp(64),
                            dp(42)
                        ).apply {
                            rightMargin = dp(6)
                        }
                }

            button.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        /*
                         * Capture the editor selection BEFORE the button can
                         * interfere with focus. Returning true also prevents
                         * Android's normal Button touch handling from moving
                         * focus to the toolbar.
                         */
                        rememberSelection()
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        val now =
                            android.os.SystemClock.uptimeMillis()

                        val isDoubleTap =
                            lastButton === v &&
                            now - lastTapTime <= 400L

                        if (isDoubleTap) {
                            applyFormatting(
                                bold = bold,
                                underline = underline,
                                remove = true
                            )
                            lastTapTime = 0L
                            lastButton = null
                        } else {
                            applyFormatting(
                                bold = bold,
                                underline = underline,
                                remove = false
                            )
                            lastTapTime = now
                            lastButton = v
                        }

                        true
                    }

                    MotionEvent.ACTION_CANCEL -> true
                    else -> true
                }
            }

            return button
        }

        toolbar.addView(
            tool(
                "B",
                bold = true,
                underline = false
            )
        )

        toolbar.addView(
            tool(
                "U",
                bold = false,
                underline = true
            )
        )

        toolbar.addView(
            tool(
                "B + U",
                bold = true,
                underline = true
            )
        )

        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        root.addView(
            editor,
            LinearLayout.LayoutParams(
                -1,
                dp(
                    if (minLines >= 6)
                        190
                    else
                        150
                )
            )
        )

        return Pair(
            root,
            editor
        )
    }

    private fun noteDialog(
        old: Note?
    ) {

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        box.setPadding(
            dp(8),
            0,
            dp(8),
            0
        )

        val t =
            edit(
                "Note Title",
                old?.title ?: ""
            )

        val d =
            edit(
                "Date",
                old?.date ?: dateNow()
            )

        val descEditor =
            richDescriptionEditor(
                old?.desc ?: "",
                6
            )

        box.addView(t)
        box.addView(d)
        box.addView(
            descEditor.first
        )

        AlertDialog.Builder(this)
            .setTitle(
                if (old == null)
                    "Add Note"
                else
                    "Edit Note"
            )
            .setView(box)
            .setPositiveButton("Save") { _, _ ->

                val l =
                    store.notes()

                if (old == null) {

                    l.add(
                        Note(
                            store.id("note"),
                            t.text.toString()
                                .ifBlank {
                                    "Untitled Note"
                                },
                            d.text.toString()
                                .ifBlank {
                                    dateNow()
                                },
                            spannedToDescription(descEditor.second.text)
                        )
                    )

                } else {

                    old.title =
                        t.text.toString()

                    old.date =
                        d.text.toString()

                    old.desc =
                        spannedToDescription(descEditor.second.text)

                    val idx =
                        l.indexOfFirst {
                            it.id == old.id
                        }

                    if (idx >= 0) {
                        l[idx] = old
                    }
                }

                store.saveNotes(l)

                notesPage()
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // --------------------------------------------------------
    // CODING
    // --------------------------------------------------------

    private fun codesPage() {

        clear("My Coding")

        sectionButton(
            "Write Code"
        ) {
            codeDialog(null)
        }

        val list =
            store.codes()

        val rv =
            FullHeightRecyclerView(this)

        rv.layoutManager =
            LinearLayoutManager(this)

        val ad =
            CodeAdapter(list)

        rv.adapter = ad

        // Let the RecyclerView use only the height required by its content.
        // The page's outer ScrollView handles vertical scrolling.
        rv.isNestedScrollingEnabled = false
        rv.setPadding(0, 0, 0, dp(12))
        rv.clipToPadding = false

        addList(rv)

        attachDrag(
            rv,
            list
        ) {
            store.saveCodes(list)
        }
    }

    private inner class CodeAdapter(
        val data: MutableList<CodeItem>
    ) : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(
            p: ViewGroup,
            t: Int
        ) =
            VH(codeRow())

        override fun getItemCount() =
            data.size

        override fun onBindViewHolder(
            h: VH,
            pos: Int
        ) {

            val c =
                data[pos]

            h.title.text =
                "▣  ${c.title}"

            h.edit.text =
                "✎ / 🗑"

            h.edit.setOnClickListener {

                val pop =
                    PopupMenu(
                        this@MainActivity,
                        h.edit
                    )

                pop.menu.add("Edit")
                pop.menu.add("Delete")

                pop.setOnMenuItemClickListener {

                    if (it.title == "Edit") {

                        codeDialog(c)

                    } else {

                        data.remove(c)

                        store.saveCodes(data)

                        notifyDataSetChanged()
                    }

                    true
                }

                pop.show()
            }

            h.itemView.setOnClickListener {
            
                h.description.text =
                    descriptionToSpanned(
                        c.desc
                    )
            
                h.code.text =
                    highlightCode(
                        c.code,
                        c.lang
                    )
            
                h.code.setTypeface(
                    Typeface.MONOSPACE
                )
            
                h.code.setTextIsSelectable(true)
            
                h.details.visibility =
                    if (
                        h.details.visibility ==
                        View.VISIBLE
                    )
                        View.GONE
                    else
                        View.VISIBLE
            }
        }
    }

    private fun codeDialog(
        old: CodeItem?
    ) {

        /*
         * One vertical ScrollView belongs to the dialog.
         *
         * Title + description + language can therefore become as long as
         * needed without hiding the Original Code field.
         *
         * The CodeEditText itself remains a fixed-height, independently
         * scrollable editor.
         */
        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
                overScrollMode =
                    View.OVER_SCROLL_IF_CONTENT_SCROLLS
            }

        val box =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(8),
                    0,
                    dp(8),
                    dp(8)
                )
            }

        val title =
            edit(
                "Code Title",
                old?.title ?: ""
            )

        val descEditor =
            richDescriptionEditor(
                old?.desc ?: "",
                3
            )

        val spinner =
            Spinner(this)

        val langs =
            arrayOf(
                "Kotlin",
                "Java",
                "Python",
                "HTML",
                "CSS",
                "JavaScript",
                "C",
                "C++",
                "SQL",
                "JSON",
                "XML",
                "Shell",
                "Other"
            )

        spinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                langs
            )

        old?.let {

            spinner.setSelection(
                langs.indexOf(
                    it.lang
                ).coerceAtLeast(0)
            )
        }

        val code =
            CodeEditText(this)

        code.language =
            old?.lang ?: "Kotlin"

        code.setText(
            old?.code ?: ""
        )

        code.highlight()

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    code.language =
                        langs[position]

                    code.highlight()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }

        code.minLines = 18

        code.layoutParams =
            LinearLayout.LayoutParams(
                -1,
                dp(360)
            ).apply {
                topMargin = dp(6)
            }

        box.addView(title)
        box.addView(
            descEditor.first
        )
        box.addView(spinner)
        box.addView(code)

        scroll.addView(
            box,
            ViewGroup.LayoutParams(
                -1,
                -2
            )
        )

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    if (old == null)
                        "Write Code"
                    else
                        "Edit Code"
                )
                .setView(scroll)
                .setPositiveButton("Save") { _, _ ->

                    var name =
                        title.text.toString()
                            .trim()

                    if (name.isBlank()) {

                        name =
                            descEditor.second.text
                                .toString()
                                .trim()
                                .split(
                                    Regex("\\s+")
                                )
                                .take(6)
                                .joinToString(" ")
                                .ifBlank {
                                    "Untitled Code"
                                }
                    }

                    val l =
                        store.codes()

                    if (old == null) {

                        l.add(
                            CodeItem(
                                store.id("code"),
                                name,
                                spannedToDescription(
                                    descEditor.second.text
                                ),
                                spinner.selectedItem.toString(),
                                code.text.toString()
                            )
                        )

                    } else {

                        old.title = name

                        old.desc =
                            spannedToDescription(
                                descEditor.second.text
                            )

                        old.lang =
                            spinner.selectedItem
                                .toString()

                        old.code =
                            code.text.toString()

                        val idx =
                            l.indexOfFirst {
                                it.id == old.id
                            }

                        if (idx >= 0) {
                            l[idx] = old
                        }
                    }

                    store.saveCodes(l)

                    codesPage()
                }
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .create()

        /*
         * ADJUST_RESIZE prevents the keyboard from panning the entire
         * dialog unpredictably. The dialog gets a smaller viewport and
         * its ScrollView handles the content.
         */
        dialog.setOnShowListener {

            dialog.window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }

        /*
         * When the user enters the Original Code field, make sure the
         * dialog's OUTER scroll reaches the code editor. After that,
         * CodeEditText controls its own scrolling.
         */
        code.setOnFocusChangeListener { _, hasFocus ->

            if (hasFocus) {

                scroll.post {
                    scroll.fullScroll(
                        View.FOCUS_DOWN
                    )
                }

                scroll.postDelayed({

                    scroll.fullScroll(
                        View.FOCUS_DOWN
                    )

                }, 200)
            }
        }

        dialog.show()
    }

    // --------------------------------------------------------
    // ACCOUNTS
    // --------------------------------------------------------

    private fun accountsPage() {

        clear("My Accounts")

        sectionButton(
            "Add Account"
        ) {
            accountDialog(null)
        }

        val list =
            store.accounts()

        val rv =
            FullHeightRecyclerView(this)

        rv.layoutManager =
            LinearLayoutManager(this)

        val ad =
            AccountAdapter(list)

        rv.adapter = ad

        // Let the RecyclerView use only the height required by its content.
        // The page's outer ScrollView handles vertical scrolling.
        rv.isNestedScrollingEnabled = false
        rv.setPadding(0, 0, 0, dp(12))
        rv.clipToPadding = false

        addList(rv)

        attachDrag(
            rv,
            list
        ) {
            store.saveAccounts(list)
        }
    }

    private inner class AccountAdapter(
        val data: MutableList<Account>
    ) : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(
            p: ViewGroup,
            t: Int
        ) =
            VH(row())

        override fun getItemCount() =
            data.size

        override fun onBindViewHolder(
            h: VH,
            pos: Int
        ) {

            val a =
                data[pos]

            h.title.text =
                "🔐  ${a.name}"

            h.edit.text =
                "✎"

            h.edit.setOnClickListener {
                accountDialog(a)
            }

            // RecyclerView reuses ViewHolders. Always start an account
            // row in its compact state so the first tap expands it,
            // rather than unexpectedly shrinking it.
            h.detail.text =
                "UserID: ${a.user}\n" +
                "Password: ${a.pass}\n" +
                "Other-1: ${a.other1}\n" +
                "Other-2: ${a.other2}"

            h.detail.setTextIsSelectable(
                true
            )

            h.detail.visibility =
                View.GONE

            h.itemView.setOnClickListener {

                h.detail.visibility =
                    if (
                        h.detail.visibility ==
                        View.VISIBLE
                    )
                        View.GONE
                    else
                        View.VISIBLE
            }
        }
    }

    private fun accountDialog(
        old: Account?
    ) {

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        val n =
            edit(
                "For What Account",
                old?.name ?: ""
            )

        val u =
            edit(
                "UserID",
                old?.user ?: ""
            )

        val p =
            edit(
                "Password",
                old?.pass ?: "",
                true
            )

        val o1 =
            edit(
                "Other-1",
                old?.other1 ?: ""
            )

        val o2 =
            edit(
                "Other-2",
                old?.other2 ?: ""
            )

        listOf(
            n,
            u,
            p,
            o1,
            o2
        ).forEach {
            box.addView(it)
        }

        AlertDialog.Builder(this)
            .setTitle(
                if (old == null)
                    "Add Account"
                else
                    "Edit Account"
            )
            .setView(box)
            .setPositiveButton("Save") { _, _ ->

                val l =
                    store.accounts()

                if (old == null) {

                    l.add(
                        Account(
                            store.id("account"),
                            n.text.toString()
                                .ifBlank {
                                    "Untitled Account"
                                },
                            u.text.toString(),
                            p.text.toString(),
                            o1.text.toString(),
                            o2.text.toString()
                        )
                    )

                } else {

                    old.name =
                        n.text.toString()

                    old.user =
                        u.text.toString()

                    old.pass =
                        p.text.toString()

                    old.other1 =
                        o1.text.toString()

                    old.other2 =
                        o2.text.toString()

                    val idx =
                        l.indexOfFirst {
                            it.id == old.id
                        }

                    if (idx >= 0) {
                        l[idx] = old
                    }
                }

                store.saveAccounts(l)

                accountsPage()
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // --------------------------------------------------------
    // FILES
    // --------------------------------------------------------

    private fun filesPage() {

        clear("My Files")

        sectionButton(
            "Add File"
        ) {
            fileTitleDialog()
        }

        val list =
            store.files()

        val rv =
            FullHeightRecyclerView(this)

        rv.layoutManager =
            LinearLayoutManager(this)

        val ad =
            FileAdapter(list)

        rv.adapter = ad

        // Let the RecyclerView use only the height required by its content.
        // The page's outer ScrollView handles vertical scrolling.
        rv.isNestedScrollingEnabled = false
        rv.setPadding(0, 0, 0, dp(12))
        rv.clipToPadding = false

        addList(rv)

        attachDrag(
            rv,
            list
        ) {
            store.saveFiles(list)
        }
    }

    private inner class FileAdapter(
        val data: MutableList<SavedFile>
    ) : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(
            p: ViewGroup,
            t: Int
        ) =
            VH(row())

        override fun getItemCount() =
            data.size

        override fun onBindViewHolder(
            h: VH,
            pos: Int
        ) {

            val f =
                data[pos]

            h.title.text =
                "📄  ${f.title}"

            h.edit.text =
                "OPEN"

            h.edit.setOnClickListener {
                openFile(f)
            }

            h.itemView.setOnClickListener {
                openFile(f)
            }
        }
    }

    private var pendingFileTitle =
        ""

    private fun fileTitleDialog() {

        val e =
            edit(
                "File Title"
            )

        AlertDialog.Builder(this)
            .setTitle("Add File")
            .setView(e)
            .setPositiveButton(
                "Upload File"
            ) { _, _ ->

                pendingFileTitle =
                    e.text.toString()

                startActivityForResult(
                    Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                    ).apply {

                        addCategory(
                            Intent.CATEGORY_OPENABLE
                        )

                        type = "*/*"

                    },
                    77
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    override fun onActivityResult(
        r: Int,
        c: Int,
        d: Intent?
    ) {

        super.onActivityResult(
            r,
            c,
            d
        )

        if (
            c != RESULT_OK ||
            d?.data == null
        ) {
            return
        }

        try {

            when (r) {

                77 ->
                    savePickedFile(
                        d.data!!
                    )

                88 ->
                    restoreData(
                        d.data!!
                    )

                89 ->
                    exportData(
                        d.data!!
                    )
            }

        } catch (e: Exception) {

            toast(
                "Operation failed: ${e.message}"
            )
        }
    }

    private fun savePickedFile(
        uri: Uri
    ) {

        val name =
            uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.replace(
                    Regex(
                        "[^A-Za-z0-9._-]"
                    ),
                    "_"
                )
                ?: "file_${System.currentTimeMillis()}"

        val safe =
            "${System.currentTimeMillis()}_$name"

        val dir =
            File(
                filesDir,
                "user_files"
            ).apply {
                mkdirs()
            }

        val out =
            dir.resolve(safe)

        contentResolver
            .openInputStream(uri)
            .use { input ->

                FileOutputStream(out)
                    .use { output ->

                        input?.copyTo(
                            output
                        )
                    }
            }

        val mime =
            contentResolver.getType(uri)
                ?: guessMime(name)

        val l =
            store.files()

        l.add(
            SavedFile(
                store.id("file"),
                pendingFileTitle
                    .ifBlank {
                        name
                    },
                name,
                out.absolutePath,
                mime
            )
        )

        store.saveFiles(l)

        filesPage()
    }

    private fun guessMime(
        n: String
    ): String =
        when (
            n.substringAfterLast(
                '.',
                ""
            ).lowercase(
                Locale.US
            )
        ) {

            "pdf" ->
                "application/pdf"

            "jpg",
            "jpeg" ->
                "image/jpeg"

            "png" ->
                "image/png"

            "txt",
            "kt",
            "java",
            "py",
            "js",
            "css",
            "html",
            "xml",
            "json" ->
                "text/plain"

            else ->
                "application/octet-stream"
        }

    private fun openFile(
        f: SavedFile
    ) {

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    File(f.path)
                )

            startActivity(
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    setDataAndType(
                        uri,
                        f.mime
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            )

        } catch (_: Exception) {

            toast(
                "No app can open this file"
            )
        }
    }

    // --------------------------------------------------------
    // INVESTMENTS
    // --------------------------------------------------------

    private fun investmentDate(
        value: String
    ): Date? {
        return try {
            SimpleDateFormat(
                DATE_FMT,
                Locale.US
            ).apply {
                isLenient = false
            }.parse(value.trim())
        } catch (_: Exception) {
            null
        }
    }

    private fun investmentPage() {

        clear(
            "Investment Details"
        )

        val summary =
            LinearLayout(this)

        summary.orientation =
            LinearLayout.HORIZONTAL

        val angel =
            button(
                "Angel One Invested Value\n₹" +
                    store.angels()
                        .sumOf {
                            it.amount
                        }
            ) {
                angelDialog()
            }

        val total =
            button(
                "Total Invested Value\n₹" +
                    store.investments()
                        .sumOf {
                            it.amount
                        }
            ) {
            }

        summary.addView(
            angel,
            LinearLayout.LayoutParams(
                0,
                dp(62),
                1f
            )
        )

        summary.addView(
            total,
            LinearLayout.LayoutParams(
                0,
                dp(62),
                1f
            )
        )

        content.addView(
            summary
        )

        val assetHeading =
            tv(
                "Asset List",
                17f,
                true
            )

        assetHeading.setPadding(
            dp(10),
            dp(14),
            dp(10),
            dp(6)
        )

        content.addView(
            assetHeading
        )

        val assets =
            store.assets()

        val inv =
            store.investments()

        assets.forEach { a ->

            val assetBox =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.VERTICAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(
                                dp(3),
                                dp(5),
                                dp(3),
                                dp(5)
                            )
                        }
                }

            bgView(
                assetBox,
                Color.WHITE,
                12f
            )

            // Thin black border around EACH asset.
            (assetBox.background as? GradientDrawable)?.setStroke(
                dp(1),
                Color.BLACK
            )

            val top =
                LinearLayout(this).apply {
                    gravity =
                        Gravity.CENTER_VERTICAL

                    orientation =
                        LinearLayout.HORIZONTAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                }

            val title =
                tv(
                    "▣ ${a.name}",
                    15f,
                    true
                )

            top.addView(
                title,
                LinearLayout.LayoutParams(
                    0,
                    dp(44),
                    1f
                )
            )

            val actions =
                button(
                    "+  🖊️  🗑️"
                ) {
                    assetInvestmentActions(a.id)
                }

            top.addView(
                actions,
                LinearLayout.LayoutParams(
                    dp(110),
                    dp(40)
                )
            )

            assetBox.addView(top)

            val category =
                tv(
                    "Asset Category: ${a.category}",
                    14f,
                    false
                )

            assetBox.addView(
                category,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            val value =
                inv.filter {
                    it.assetId == a.id
                }.sumOf {
                    it.amount
                }

            val valueView =
                tv(
                    "Value (₹. $value)",
                    14f,
                    true
                ).apply {
                    setTextColor(
                        Color.rgb(
                            34,
                            139,
                            34
                        )
                    )
                    setPadding(
                        dp(10),
                        dp(5),
                        dp(10),
                        dp(12)
                    )
                }

            assetBox.addView(
                valueView,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            val detail =
                tv("")

            detail.setTextIsSelectable(
                true
            )

            detail.visibility =
                View.GONE

            assetBox.setOnClickListener {

                val entries =
                    inv.filter {
                        it.assetId == a.id
                    }
                        .sortedWith(
                            compareByDescending {
                                investmentDate(it.date)
                                    ?: Date(0)
                            }
                        )

                if (entries.isEmpty()) {

                    detail.text =
                        "No investments yet"

                } else {

                    val sp =
                        android.text.SpannableStringBuilder()

                    entries.forEachIndexed { index, item ->

                        sp.append(
                            "${item.date}   "
                        )

                        val amountText =
                            "₹${item.amount}"

                        val start =
                            sp.length

                        sp.append(
                            amountText
                        )

                        sp.setSpan(
                            ForegroundColorSpan(
                                Color.rgb(
                                    34,
                                    139,
                                    34
                                )
                            ),
                            start,
                            sp.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        if (
                            index <
                            entries.lastIndex
                        ) {
                            sp.append("\n")
                        }
                    }

                    detail.text = sp
                }

                // Keep the action button responsive when the
                // asset row itself is tapped.
                detail.visibility =
                    if (
                        detail.visibility ==
                        View.VISIBLE
                    )
                        View.GONE
                    else
                        View.VISIBLE
            }

            assetBox.addView(detail)

            content.addView(
                assetBox
            )
        }
    }

    private fun assetInvestmentActions(
        assetId: Long
    ) {

        val asset =
            store.assets()
                .find {
                    it.id == assetId
                }

        if (asset == null) {
            toast("Asset not found")
            investmentPage()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Investment • ${asset.name}"
            )
            .setItems(
                arrayOf(
                    "Add Investment",
                    "Edit Investment",
                    "Delete Investment"
                )
            ) { _, which ->

                when (which) {

                    0 ->
                        investmentDialog(
                            old = null,
                            assetId = assetId
                        )

                    1 ->
                        investmentManage(
                            assetId = assetId,
                            deleteOnly = false
                        )

                    2 ->
                        investmentManage(
                            assetId = assetId,
                            deleteOnly = true
                        )
                }
            }
            .show()
    }

    private fun assetDialog(
        old: Asset?
    ) {

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        val n =
            edit(
                "Asset Name",
                old?.name ?: ""
            )

        val s =
            Spinner(this)

        val cats =
            arrayOf(
                "Mutual Fund",
                "Stock",
                "Fixed Deposit",
                "Recurring Deposit"
            )

        s.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                cats
            )

        old?.let {

            s.setSelection(
                cats.indexOf(
                    it.category
                ).coerceAtLeast(0)
            )
        }

        box.addView(n)
        box.addView(s)

        AlertDialog.Builder(this)
            .setTitle(
                if (old == null)
                    "Add Asset"
                else
                    "Edit Asset"
            )
            .setView(box)
            .setPositiveButton("Save") { _, _ ->

                val l =
                    store.assets()

                if (old == null) {

                    l.add(
                        Asset(
                            store.id("asset"),
                            n.text.toString()
                                .ifBlank {
                                    "Untitled Asset"
                                },
                            s.selectedItem.toString()
                        )
                    )

                } else {

                    old.name =
                        n.text.toString()

                    old.category =
                        s.selectedItem
                            .toString()

                    val idx =
                        l.indexOfFirst {
                            it.id == old.id
                        }

                    if (idx >= 0) {
                        l[idx] = old
                    }
                }

                store.saveAssets(l)

                investmentPage()
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun assetManage() {

        val l =
            store.assets()

        if (l.isEmpty()) {
            toast("No assets")
            return
        }

        val names =
            l.map {
                it.name
            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(
                "Edit / Delete Asset"
            )
            .setItems(names) { _, which ->

                val a =
                    l[which]

                AlertDialog.Builder(this)
                    .setItems(
                        arrayOf(
                            "Edit",
                            "Delete"
                        )
                    ) { _, x ->

                        if (x == 0) {

                            assetDialog(a)

                        } else {

                            store.saveAssets(
                                l.filter {
                                    it.id != a.id
                                }
                            )

                            store.saveInvestments(
                                store.investments()
                                    .filter {
                                        it.assetId != a.id
                                    }
                            )

                            investmentPage()
                        }
                    }
                    .show()
            }
            .show()
    }

    private fun investmentDialog(
        old: Investment?,
        assetId: Long
    ) {

        val asset =
            store.assets()
                .find {
                    it.id == assetId
                }

        if (asset == null) {
            toast(
                "Asset not found"
            )
            investmentPage()
            return
        }

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        val assetView =
            tv(
                "Asset: ${asset.name}",
                15f,
                true
            ).apply {
                setPadding(
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(6)
                )
            }

        val d =
            edit(
                "Date",
                old?.date ?: dateNow()
            )

        val am =
            edit(
                "Amount",
                old?.amount?.toString()
                    ?: ""
            )

        box.addView(assetView)
        box.addView(d)
        box.addView(am)

        AlertDialog.Builder(this)
            .setTitle(
                if (old == null)
                    "Add Investment"
                else
                    "Edit Investment"
            )
            .setView(box)
            .setPositiveButton("Save") { _, _ ->

                val l =
                    store.investments()

                val value =
                    am.text.toString()
                        .toDoubleOrNull()
                        ?: 0.0

                if (old == null) {

                    l.add(
                        Investment(
                            store.id(
                                "investment"
                            ),
                            assetId,
                            d.text.toString()
                                .ifBlank {
                                    dateNow()
                                },
                            value
                        )
                    )

                } else {

                    old.assetId =
                        assetId

                    old.date =
                        d.text.toString()

                    old.amount =
                        value

                    val idx =
                        l.indexOfFirst {
                            it.id == old.id
                        }

                    if (idx >= 0) {
                        l[idx] = old
                    }
                }

                store.saveInvestments(l)

                investmentPage()
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun investmentManage(
        assetId: Long,
        deleteOnly: Boolean = false
    ) {

        val l =
            store.investments()
                .filter {
                    it.assetId == assetId
                }
                .sortedWith(
                    compareByDescending {
                        investmentDate(it.date)
                            ?: Date(0)
                    }
                )
                .toMutableList()

        if (l.isEmpty()) {

            toast(
                if (deleteOnly)
                    "No investments for this asset"
                else
                    "No investments for this asset"
            )

            return
        }

        val assetName =
            store.assets()
                .find {
                    it.id == assetId
                }
                ?.name
                ?: "Asset"

        val names =
            l.map {
                "${it.date} • ₹${it.amount}"
            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(
                if (deleteOnly)
                    "Delete Investment • $assetName"
                else
                    "Edit / Delete Investment • $assetName"
            )
            .setItems(names) { _, i ->

                val selected = l[i]

                if (deleteOnly) {

                    AlertDialog.Builder(this)
                        .setMessage(
                            "Delete this investment permanently?"
                        )
                        .setPositiveButton(
                            "Delete"
                        ) { _, _ ->

                            val all =
                                store.investments()

                            all.removeAll {
                                it.id == selected.id
                            }

                            store.saveInvestments(
                                all
                            )

                            investmentPage()
                        }
                        .setNegativeButton(
                            "Cancel",
                            null
                        )
                        .show()

                } else {

                    AlertDialog.Builder(this)
                        .setItems(
                            arrayOf(
                                "Edit",
                                "Delete"
                            )
                        ) { _, x ->

                            if (x == 0) {

                                investmentDialog(
                                    old = selected,
                                    assetId = assetId
                                )

                            } else {

                                val all =
                                    store.investments()

                                all.removeAll {
                                    it.id == selected.id
                                }

                                store.saveInvestments(
                                    all
                                )

                                investmentPage()
                            }
                        }
                        .show()
                }
            }
            .show()
    }

    private fun angelDialog() {

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        val d =
            edit(
                "Date",
                dateNow()
            )

        val a =
            edit(
                "Angel One invested value"
            )

        box.addView(d)
        box.addView(a)

        AlertDialog.Builder(this)
            .setTitle(
                "Angel One Invested Value"
            )
            .setView(box)
            .setPositiveButton("Save") { _, _ ->

                val l =
                    store.angels()

                l.add(
                    Angel(
                        store.id("angel"),
                        d.text.toString(),
                        a.text.toString()
                            .toDoubleOrNull()
                            ?: 0.0
                    )
                )

                store.saveAngels(l)

                investmentPage()
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // --------------------------------------------------------
    // EXPORT / RESTORE
    // --------------------------------------------------------

    private fun backupPage() {

        clear(
            "Export / Restore All"
        )

        sectionButton(
            "Export All Data"
        ) {

            startActivityForResult(

                Intent(
                    Intent.ACTION_CREATE_DOCUMENT
                ).apply {

                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )

                    type =
                        "application/json"

                    putExtra(
                        Intent.EXTRA_TITLE,
                        "all_in_one_backup.json"
                    )
                },

                89
            )
        }

        sectionButton(
            "Restore All Data"
        ) {

            startActivityForResult(

                Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                ).apply {

                    addCategory(
                        Intent.CATEGORY_OPENABLE
                    )

                    type =
                        "application/json"
                },

                88
            )
        }
    }

    private fun exportData(
        uri: Uri
    ) {

        contentResolver
            .openOutputStream(uri)
            .use {

                it?.write(
                    store.raw()
                        .toString(2)
                        .toByteArray()
                )
            }

        toast(
            "All data exported"
        )
    }

    private fun restoreData(
        uri: Uri
    ) {

        val s =
            contentResolver
                .openInputStream(uri)
                .use {
                    it?.bufferedReader()
                        ?.readText()
                        ?: ""
                }

        val o =
            JSONObject(s)

        store.restore(o)

        // Rebuild unique IDs and next-ID counters from the restored data.
        store.repairIds()

        filesPage()

        toast(
            "All data restored"
        )
    }

    // --------------------------------------------------------
    // CONTROL PANEL
    // --------------------------------------------------------

    private fun controlPage() {

        clear(
            "Control Panel"
        )

        sectionButton(
            "Delete Notes"
        ) {

            deletePicker(
                "Notes",
                store.notes()
                    .map {
                        it.title
                    }
            ) { i ->

                val l =
                    store.notes()

                l.removeAt(i)

                store.saveNotes(l)

                controlPage()
            }
        }

        sectionButton(
            "Delete Accounts"
        ) {

            deletePicker(
                "Accounts",
                store.accounts()
                    .map {
                        it.name
                    }
            ) { i ->

                val l =
                    store.accounts()

                l.removeAt(i)

                store.saveAccounts(l)

                controlPage()
            }
        }

        sectionButton(
            "Delete Files"
        ) {

            deletePicker(
                "Files",
                store.files()
                    .map {
                        it.title
                    }
            ) { i ->

                val l =
                    store.files()

                val f =
                    l.removeAt(i)

                File(f.path).delete()

                store.saveFiles(l)

                controlPage()
            }
        }

        sectionButton(
            "Add Asset"
        ) {
            assetDialog(null)
        }

        sectionButton(
            "Edit & Delete Asset"
        ) {
            assetManage()
        }

        sectionButton(
            "Reset Your App PIN"
        ) {
            resetPin()
        }
    }

    private fun deletePicker(
        title: String,
        names: List<String>,
        done: (Int) -> Unit
    ) {

        if (names.isEmpty()) {

            toast(
                "No $title"
            )

            return
        }

        AlertDialog.Builder(this)
            .setTitle(
                "Select $title"
            )
            .setItems(
                names.toTypedArray()
            ) { _, i ->

                AlertDialog.Builder(this)
                    .setMessage(
                        "Delete ${names[i]} permanently?"
                    )
                    .setPositiveButton(
                        "Delete"
                    ) { _, _ ->

                        done(i)
                    }
                    .setNegativeButton(
                        "Cancel",
                        null
                    )
                    .show()
            }
            .show()
    }

    private fun resetPin() {

        val e =
            edit(
                "New 6-digit PIN"
            )

        e.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER

        e.setSingleLine()

        AlertDialog.Builder(this)
            .setTitle(
                "Reset PIN"
            )
            .setView(e)
            .setPositiveButton(
                "Save"
            ) { _, _ ->

                val v =
                    e.text.toString()

                if (
                    Regex(
                        "\\d{6}"
                    ).matches(v)
                ) {

                    store.setPin(v)

                    toast(
                        "PIN changed"
                    )

                } else {

                    toast(
                        "PIN must contain exactly 6 digits"
                    )
                }
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // --------------------------------------------------------
    // LONG-PRESS REORDER
    // --------------------------------------------------------

    private fun <T> attachDrag(
        rv: RecyclerView,
        l: MutableList<T>,
        save: () -> Unit
    ) {

        val cb =
            object :
                ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP or
                        ItemTouchHelper.DOWN,
                    0
                ) {

                override fun onMove(
                    r: RecyclerView,
                    v: RecyclerView.ViewHolder,
                    t: RecyclerView.ViewHolder
                ): Boolean {

                    val f =
                        v.bindingAdapterPosition

                    val to =
                        t.bindingAdapterPosition

                    if (
                        f < 0 ||
                        to < 0
                    ) {
                        return false
                    }

                    java.util.Collections.swap(
                        l,
                        f,
                        to
                    )

                    r.adapter
                        ?.notifyItemMoved(
                            f,
                            to
                        )

                    save()

                    return true
                }

                override fun onSwiped(
                    v: RecyclerView.ViewHolder,
                    d: Int
                ) {
                }
            }

        ItemTouchHelper(cb)
            .attachToRecyclerView(rv)
    }

    // --------------------------------------------------------
    // DISPLAY SYNTAX HIGHLIGHTING
    // --------------------------------------------------------

    private fun highlightText(
        s: String,
        language: String = "Other"
    ): CharSequence =
        highlightCode(
            s,
            language
        )

    private fun toast(
        s: String
    ) =
        Toast.makeText(
            this,
            s,
            Toast.LENGTH_SHORT
        ).show()

    private class VH(
        v: View
    ) : RecyclerView.ViewHolder(v) {
    
        val title =
            v.findViewById<TextView>(1001)
    
        val edit =
            v.findViewById<Button>(1002)
    
        // Used by Notes / Accounts
        val detail =
            v.findViewById<TextView>(1003)
    
        // Used by My Coding
        val details =
            v.findViewById<LinearLayout>(1007)
    
        val description =
            v.findViewById<TextView>(1005)
    
        val code =
            v.findViewById<TextView>(1006)
    }
}
