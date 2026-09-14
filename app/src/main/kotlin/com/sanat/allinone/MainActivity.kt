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

    private fun next(k: String): Long {
        val n =
            p.getLong(
                "next_$k",
                1L
            )

        p.edit()
            .putLong(
                "next_$k",
                n + 1
            )
            .apply()

        return n
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
     * IMPORTANT:
     * Once the user manually scrolls the code editor, Android must NOT
     * automatically move the editor back to the cursor position while
     * syntax highlighting or typing is happening.
     */
    private var userHasScrolled = false
    private var touchDownY = 0f
    private var dragging = false
    private val touchSlop =
        ViewConfiguration.get(c).scaledTouchSlop

    private val kw =
        Regex(
            "\\b(fun|val|var|class|object|interface|if|else|when|for|while|return|import|package|public|private|protected|static|void|new|try|catch|finally|throw|throws|extends|implements|def|lambda|True|False|None|print|function|const|let|async|await|return|SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|CREATE)\\b"
        )

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

    /*
     * Let this editor own vertical/horizontal scrolling while the finger
     * is inside it. This prevents the dialog's ScrollView from fighting
     * with the code editor.
     */
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
     * Android normally tries to bring the cursor into view after every
     * text change. That is what causes the annoying jump to the bottom.
     *
     * Before the user manually scrolls, normal Android behaviour is kept.
     * After a manual scroll, the current viewport is locked until the user
     * changes it manually again.
     */
    override fun bringPointIntoView(
        offset: Int
    ): Boolean {

        if (userHasScrolled) {
            return true
        }

        return super.bringPointIntoView(
            offset
        )
    }

    override fun requestRectangleOnScreen(
        rectangle: Rect,
        immediate: Boolean
    ): Boolean {

        if (userHasScrolled) {
            return true
        }

        return super.requestRectangleOnScreen(
            rectangle,
            immediate
        )
    }

    fun highlight() {

        if (busy) {
            return
        }

        val s =
            text.toString()

        val sp =
            android.text.SpannableString(s)

        fun col(
            r: Regex,
            color: Int
        ) {

            r.findAll(s).forEach {

                sp.setSpan(
                    ForegroundColorSpan(color),
                    it.range.first,
                    it.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        col(
            kw,
            Color.rgb(
                75,
                55,
                180
            )
        )

        col(
            Regex(
                "(//.*$|#.*$)",
                RegexOption.MULTILINE
            ),
            Color.rgb(
                90,
                90,
                90
            )
        )

        col(
            Regex(
                "(\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*')"
            ),
            Color.rgb(
                170,
                65,
                40
            )
        )

        col(
            Regex("<[^>]+>"),
            Color.rgb(
                0,
                110,
                130
            )
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

        /*
         * setText() can reset the internal EditText scroll position.
         * Restore exactly the position the user had chosen.
         */
        post {
            scrollTo(
                oldScrollX,
                oldScrollY
            )
        }
    }
}

class MainActivity : Activity() {

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
                18f,
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
            RecyclerView(this)

        rv.layoutManager =
            LinearLayoutManager(this)

        rv.adapter =
            NoteAdapter(list)

        // Let the RecyclerView use only the height required by its content.
        // The page's outer ScrollView handles vertical scrolling.
        rv.isNestedScrollingEnabled = false

        content.addView(
            rv,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

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
                    n.desc

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
                n.desc

            h.detail.visibility =
                View.GONE
        }
    }

    private fun row(): LinearLayout {

        val r =
            LinearLayout(this)

        r.orientation =
            LinearLayout.VERTICAL

        bgView(
            r,
            Color.WHITE,
            12f
        )

        val top =
            LinearLayout(this)

        top.gravity =
            Gravity.CENTER_VERTICAL

        val t =
            tv("").apply {
                id = 1001
            }

        top.addView(
            t,
            LinearLayout.LayoutParams(
                0,
                dp(46),
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
                dp(40)
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
            }
    
        bgView(
            r,
            Color.WHITE,
            12f
        )
    
        // --------------------------------------------------------
        // CODE TITLE
        // --------------------------------------------------------
    
        val top =
            LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
    
        val t =
            tv("").apply {
                id = 1001
            }
    
        top.addView(
            t,
            LinearLayout.LayoutParams(
                0,
                dp(46),
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
                dp(40)
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

        val desc =
            edit(
                "Write Description",
                old?.desc ?: ""
            )

        desc.minLines = 6

        box.addView(t)
        box.addView(d)
        box.addView(desc)

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
                            desc.text.toString()
                        )
                    )

                } else {

                    old.title =
                        t.text.toString()

                    old.date =
                        d.text.toString()

                    old.desc =
                        desc.text.toString()

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
            RecyclerView(this)

        rv.layoutManager =
            LinearLayoutManager(this)

        val ad =
            CodeAdapter(list)

        rv.adapter = ad

        // Let the RecyclerView use only the height required by its content.
        // The page's outer ScrollView handles vertical scrolling.
        rv.isNestedScrollingEnabled = false

        content.addView(
            rv,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        attachDrag(
            rv,
            list
        ) {
            store.saveCodes(list)
            ad.notifyDataSetChanged()
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
                    c.desc
            
                h.code.text =
                    highlightText(
                        c.code
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

        val desc =
            edit(
                "Code Description",
                old?.desc ?: ""
            )

        desc.minLines = 3

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

        code.setText(
            old?.code ?: ""
        )

        code.minLines = 18

        code.layoutParams =
            LinearLayout.LayoutParams(
                -1,
                dp(360)
            ).apply {
                topMargin = dp(6)
            }

        box.addView(title)
        box.addView(desc)
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
                            desc.text.toString()
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
                                desc.text.toString(),
                                spinner.selectedItem.toString(),
                                code.text.toString()
                            )
                        )

                    } else {

                        old.title = name

                        old.desc =
                            desc.text.toString()

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
            RecyclerView(this)

        rv.layoutManager =
            LinearLayoutManager(this)

        val ad =
            AccountAdapter(list)

        rv.adapter = ad

        // Let the RecyclerView use only the height required by its content.
        // The page's outer ScrollView handles vertical scrolling.
        rv.isNestedScrollingEnabled = false

        content.addView(
            rv,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        attachDrag(
            rv,
            list
        ) {
            store.saveAccounts(list)
            ad.notifyDataSetChanged()
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

            h.itemView.setOnClickListener {

                h.detail.text =
                    "UserID: ${a.user}\n" +
                    "Password: ${a.pass}\n" +
                    "Other-1: ${a.other1}\n" +
                    "Other-2: ${a.other2}"

                h.detail.setTextIsSelectable(
                    true
                )

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
            RecyclerView(this)

        rv.layoutManager =
            LinearLayoutManager(this)

        val ad =
            FileAdapter(list)

        rv.adapter = ad

        // Let the RecyclerView use only the height required by its content.
        // The page's outer ScrollView handles vertical scrolling.
        rv.isNestedScrollingEnabled = false

        content.addView(
            rv,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        attachDrag(
            rv,
            list
        ) {
            store.saveFiles(list)
            ad.notifyDataSetChanged()
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
            "Add Investment"
        ) {
            investmentDialog(null)
        }

        sectionButton(
            "Edit & Delete Investment"
        ) {
            investmentManage()
        }

        val assets =
            store.assets()

        val inv =
            store.investments()

        assets.forEach { a ->

            val head =
                LinearLayout(this)

            bgView(
                head,
                Color.WHITE,
                12f
            )

            val b =
                tv(
                    "▣ ${a.name}  •  ${a.category}     ₹" +
                        inv.filter {
                            it.assetId == a.id
                        }.sumOf {
                            it.amount
                        },
                    15f,
                    true
                )

            head.addView(
                b,
                LinearLayout.LayoutParams(
                    -1,
                    dp(46)
                )
            )

            val detail =
                tv("")

            detail.setTextIsSelectable(
                true
            )

            detail.visibility =
                View.GONE

            head.setOnClickListener {

                detail.text =
                    inv.filter {
                        it.assetId == a.id
                    }
                        .sortedByDescending {
                            it.date
                        }
                        .joinToString("\n") {
                            "${it.date}   ₹${it.amount}"
                        }
                        .ifBlank {
                            "No investments yet"
                        }

                detail.visibility =
                    if (
                        detail.visibility ==
                        View.VISIBLE
                    )
                        View.GONE
                    else
                        View.VISIBLE
            }

            content.addView(head)
            content.addView(detail)
        }
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
        old: Investment?
    ) {

        val assets =
            store.assets()

        if (assets.isEmpty()) {

            toast(
                "Add an asset first"
            )

            return
        }

        val box =
            LinearLayout(this)

        box.orientation =
            LinearLayout.VERTICAL

        val s =
            Spinner(this)

        s.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                assets.map {
                    it.name
                }
            )

        old?.let {

            val ix =
                assets.indexOfFirst {
                    it.id == old.assetId
                }

            if (ix >= 0) {
                s.setSelection(ix)
            }
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

        box.addView(s)
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

                val a =
                    assets[
                        s.selectedItemPosition
                    ]

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
                            a.id,
                            d.text.toString()
                                .ifBlank {
                                    dateNow()
                                },
                            value
                        )
                    )

                } else {

                    old.assetId =
                        a.id

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

    private fun investmentManage() {

        val l =
            store.investments()

        if (l.isEmpty()) {

            toast(
                "No investments"
            )

            return
        }

        val assets =
            store.assets()

        val names =
            l.map {

                val a =
                    assets.find {
                        x ->
                        x.id == it.assetId
                    }

                "${a?.name ?: "Deleted asset"} • " +
                    "${it.date} • ₹${it.amount}"

            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(
                "Edit / Delete Investment"
            )
            .setItems(names) { _, i ->

                AlertDialog.Builder(this)
                    .setItems(
                        arrayOf(
                            "Edit",
                            "Delete"
                        )
                    ) { _, x ->

                        if (x == 0) {

                            investmentDialog(
                                l[i]
                            )

                        } else {

                            l.removeAt(i)

                            store.saveInvestments(
                                l
                            )

                            investmentPage()
                        }
                    }
                    .show()
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
        s: String
    ): CharSequence {

        val sp =
            android.text.SpannableString(
                s
            )

        Regex(
            "\\b(fun|val|var|class|object|interface|if|else|when|for|while|return|import|package|public|private|static|void|new|def|print|function|const|let|async|await|SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|CREATE)\\b"
        )
            .findAll(s)
            .forEach {

                sp.setSpan(
                    ForegroundColorSpan(
                        Color.rgb(
                            75,
                            55,
                            180
                        )
                    ),
                    it.range.first,
                    it.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        Regex(
            "(//.*$|#.*$)",
            RegexOption.MULTILINE
        )
            .findAll(s)
            .forEach {

                sp.setSpan(
                    ForegroundColorSpan(
                        Color.DKGRAY
                    ),
                    it.range.first,
                    it.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        Regex(
            "\"[^\"]*\"|\'[^\']*\'"
        )
            .findAll(s)
            .forEach {

                sp.setSpan(
                    ForegroundColorSpan(
                        Color.rgb(
                            170,
                            65,
                            40
                        )
                    ),
                    it.range.first,
                    it.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        Regex(
            "<[^>]+>"
        )
            .findAll(s)
            .forEach {

                sp.setSpan(
                    ForegroundColorSpan(
                        Color.rgb(
                            0,
                            110,
                            130
                        )
                    ),
                    it.range.first,
                    it.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

        return sp
    }

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
