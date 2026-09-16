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

private data class Account(
    var id: Long,
    var name: String,
    var user: String,
    var pass: String,
    var other1: String,
    var other2: String
)

private data class SavedFolder(
    var id: Long,
    var name: String
)

private data class SavedFile(
    var id: Long,
    var title: String,
    var fileName: String,
    var path: String,
    var mime: String,
    var folderId: Long = 0L
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

    init {
        // Clear its old database and ID counter from previous versions.
        p.edit()
            .remove("codes")
            .remove("next_codes")
            .apply()
    }

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
                "accounts",
                "files",
                "folders",
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
                ),
                o.optLong("folderId", 0L)
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
                    put("folderId", it.folderId)
                }
            )
        }

        put("files", a)
    }

    fun folders(): MutableList<SavedFolder> {
        val a = arr("folders")

        return MutableList(a.length()) {
            val o = a.getJSONObject(it)

            SavedFolder(
                o.getLong("id"),
                o.optString("name", "Untitled Folder")
            )
        }
    }

    fun saveFolders(
        x: List<SavedFolder>
    ) {
        val a = JSONArray()

        x.forEach {
            a.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                }
            )
        }

        put("folders", a)
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

    /*
     * ID namespaces use the exact same keys as the stored JSON arrays.
     * This prevents ID reuse after restore or app upgrades.
     */
    private fun idKey(k: String): String =
        when (k) {
            "note" -> "notes"
            "account" -> "accounts"
            "file" -> "files"
            "folder" -> "folders"
            "asset" -> "assets"
            "investment" -> "investments"
            "angel" -> "angels"
            else -> k
        }

    fun id(k: String): Long =
        next(idKey(k))

    fun raw(): JSONObject =
        JSONObject().apply {

            put(
                "version",
                3
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
                "accounts",
                arr("accounts")
            )

            put(
                "folders",
                arr("folders")
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
                            "folderId",
                            f.folderId
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
            "accounts",
            "assets",
            "investments",
            "angels",
            "folders"
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
                        "folderId",
                        x.optLong(
                            "folderId",
                            0L
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
                        if (s.length >= 16)
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
            1 -> accountsPage()
            2 -> filesPage()
            3 -> investmentPage()
            4 -> backupPage()
            5 -> controlPage()
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

    private fun accountRow(): LinearLayout =
        compactTitleRow()

    private fun fileRow(): LinearLayout =
        compactTitleRow().apply {
            // My Files uses the same fixed 2-line title container as
            // My Accounts. The shared compact row also contains the
            // detail TextView for Accounts; hide it for Files so it
            // cannot add extra blank height below the title section.
            findViewById<TextView>(1003)?.visibility =
                View.GONE
        }

    private fun compactTitleRow(): LinearLayout {
        val r =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    RecyclerView.LayoutParams(
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

        bgView(r, Color.WHITE, 12f)
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
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            }

        top.addView(
            t,
            LinearLayout.LayoutParams(
                0,
                dp(64),
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
        r.addView(
            d,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        return r
    }

    private inner class AccountAdapter(
        val data: MutableList<Account>
    ) : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(
            p: ViewGroup,
            t: Int
        ) =
            VH(accountRow())

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

    private var pendingFolderId =
        0L

    private val pendingFileUris =
        mutableListOf<Uri>()

    private var pendingFileIndex =
        0

    private fun filesPage() {

        clear("My Files")

        sectionButton(
            "Create Folder"
        ) {
            folderDialog()
        }

        sectionButton(
            "Upload File"
        ) {
            chooseUploadFolder()
        }

        val folders =
            store.folders()

        folders.forEach { folder ->

            val folderBox =
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
                folderBox,
                Color.WHITE,
                12f
            )

            (folderBox.background as? GradientDrawable)
                ?.setStroke(
                    dp(1),
                    Color.BLACK
                )

            val folderTitle =
                tv(
                    "📁  ${folder.name}",
                    16f,
                    true
                ).apply {
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(
                        dp(12),
                        0,
                        dp(12),
                        0
                    )
                    gravity =
                        Gravity.CENTER_VERTICAL
                }

            folderBox.addView(
                folderTitle,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(64)
                )
            )

            val folderFiles =
                store.files()
                    .filter {
                        it.folderId == folder.id
                    }
                    .toMutableList()

            val folderContent =
                LinearLayout(this).apply {
                    orientation =
                        LinearLayout.VERTICAL
                }

            if (folderFiles.isEmpty()) {

                val empty =
                    tv(
                        "No files in this folder",
                        14f,
                        false
                    ).apply {
                        setPadding(
                            dp(14),
                            dp(4),
                            dp(14),
                            dp(14)
                        )
                    }

                folderContent.addView(
                    empty
                )

            } else {

                val rv =
                    FullHeightRecyclerView(this)

                rv.layoutManager =
                    LinearLayoutManager(this)

                rv.adapter =
                    FileAdapter(folderFiles)

                rv.isNestedScrollingEnabled =
                    false

                rv.setPadding(
                    0,
                    0,
                    0,
                    dp(8)
                )

                rv.clipToPadding =
                    false

                folderContent.addView(
                    rv,
                    LinearLayout.LayoutParams(
                        -1,
                        -2
                    )
                )

                attachDrag(
                    rv,
                    folderFiles
                ) {
                    val all =
                        store.files()

                    val ids =
                        folderFiles.map {
                            it.id
                        }.toHashSet()

                    val reordered =
                        folderFiles.toList()

                    var index =
                        0

                    for (i in all.indices) {
                        if (ids.contains(all[i].id)) {
                            all[i] =
                                reordered[index++]
                        }
                    }

                    store.saveFiles(all)
                }
            }

            // Folders are collapsed by default. The user can tap the
            // folder title to expand and see its files.
            folderContent.visibility =
                View.GONE

            folderBox.addView(
                folderContent
            )

            folderTitle.setOnClickListener {

                folderContent.visibility =
                    if (
                        folderContent.visibility ==
                        View.VISIBLE
                    )
                        View.GONE
                    else
                        View.VISIBLE
            }

            content.addView(
                folderBox
            )
        }

        val rootFiles =
            store.files()
                .filter {
                    it.folderId == 0L ||
                    folders.none { f ->
                        f.id == it.folderId
                    }
                }
                .toMutableList()

        if (rootFiles.isNotEmpty()) {

            val heading =
                tv(
                    "Single Files",
                    17f,
                    true
                ).apply {
                    setPadding(
                        dp(10),
                        dp(12),
                        dp(10),
                        dp(4)
                    )
                }

            content.addView(
                heading
            )

            val rv =
                FullHeightRecyclerView(this)

            rv.layoutManager =
                LinearLayoutManager(this)

            rv.adapter =
                FileAdapter(rootFiles)

            rv.isNestedScrollingEnabled =
                false

            rv.setPadding(
                0,
                0,
                0,
                dp(12)
            )

            rv.clipToPadding =
                false

            addList(rv)

            attachDrag(
                rv,
                rootFiles
            ) {
                val all =
                    store.files()

                val ids =
                    rootFiles.map {
                        it.id
                    }.toHashSet()

                val reordered =
                    rootFiles.toList()

                var index =
                    0

                for (i in all.indices) {
                    if (ids.contains(all[i].id)) {
                        all[i] =
                            reordered[index++]
                    }
                }

                store.saveFiles(all)
            }
        }
    }

    private inner class FileAdapter(
        val data: MutableList<SavedFile>
    ) : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(
            p: ViewGroup,
            t: Int
        ) =
            VH(fileRow())

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

    private fun folderDialog() {

        val e =
            edit(
                "Folder Name"
            )

        AlertDialog.Builder(this)
            .setTitle(
                "Create Folder"
            )
            .setView(e)
            .setPositiveButton(
                "Create"
            ) { _, _ ->

                val name =
                    e.text.toString()
                        .trim()

                if (name.isBlank()) {
                    toast(
                        "Folder name cannot be empty"
                    )
                    return@setPositiveButton
                }

                val folders =
                    store.folders()

                if (
                    folders.any {
                        it.name.equals(
                            name,
                            ignoreCase = true
                        )
                    }
                ) {
                    toast(
                        "Folder already exists"
                    )
                    return@setPositiveButton
                }

                folders.add(
                    SavedFolder(
                        store.id("folder"),
                        name
                    )
                )

                store.saveFolders(
                    folders
                )

                filesPage()
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun chooseUploadFolder() {

        val folders =
            store.folders()

        if (folders.isEmpty()) {
            startFilePicker(0L)
            return
        }

        val names =
            mutableListOf(
                "No Folder (Single File)"
            )

        names.addAll(
            folders.map {
                "📁  ${it.name}"
            }
        )

        AlertDialog.Builder(this)
            .setTitle(
                "Upload File"
            )
            .setItems(
                names.toTypedArray()
            ) { _, which ->

                val folderId =
                    if (which == 0)
                        0L
                    else
                        folders[which - 1].id

                startFilePicker(
                    folderId
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    private fun startFilePicker(
        folderId: Long
    ) {

        pendingFolderId =
            folderId

        pendingFileUris.clear()
        pendingFileIndex = 0

        startActivityForResult(
            Intent(
                Intent.ACTION_OPEN_DOCUMENT
            ).apply {

                addCategory(
                    Intent.CATEGORY_OPENABLE
                )

                type = "*/*"

                putExtra(
                    Intent.EXTRA_ALLOW_MULTIPLE,
                    true
                )
            },
            77
        )
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
            d?.data == null &&
            d?.clipData == null
        ) {
            return
        }

        try {

            when (r) {

                77 -> {

                    pendingFileUris.clear()

                    d?.clipData?.let {
                        for (
                            i in 0 until it.itemCount
                        ) {
                            pendingFileUris.add(
                                it.getItemAt(i).uri
                            )
                        }
                    }

                    if (
                        pendingFileUris.isEmpty()
                    ) {
                        d?.data?.let {
                            pendingFileUris.add(
                                it
                            )
                        }
                    }

                    pendingFileIndex = 0

                    promptNextFileTitle()
                }

                88 ->
                    restoreData(
                        d!!.data!!
                    )

                89 ->
                    exportData(
                        d!!.data!!
                    )
            }

        } catch (e: Exception) {

            toast(
                "Operation failed: ${e.message}"
            )
        }
    }

    private fun promptNextFileTitle() {

        if (
            pendingFileIndex >=
            pendingFileUris.size
        ) {

            val count =
                pendingFileUris.size

            pendingFileUris.clear()
            pendingFileIndex = 0

            filesPage()

            toast(
                if (count == 1)
                    "File uploaded"
                else
                    "$count files uploaded"
            )

            return
        }

        val uri =
            pendingFileUris[
                pendingFileIndex
            ]

        val displayName =
            uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.ifBlank {
                    "File"
                }
                ?: "File"

        val e =
            edit(
                "File Title"
            )

        AlertDialog.Builder(this)
            .setTitle(
                "File Title " +
                    "(${pendingFileIndex + 1}/" +
                    "${pendingFileUris.size})"
            )
            .setView(e)
            .setPositiveButton(
                "Upload"
            ) { _, _ ->

                val title =
                    e.text.toString()
                        .trim()
                        .ifBlank {
                            displayName
                        }

                savePickedFile(
                    uri,
                    pendingFolderId,
                    title
                )

                pendingFileIndex++

                promptNextFileTitle()
            }
            .setNegativeButton(
                "Cancel"
            ) { _, _ ->

                pendingFileUris.clear()
                pendingFileIndex = 0
                filesPage()
            }
            .show()
    }

    private fun savePickedFile(
        uri: Uri,
        folderId: Long,
        title: String
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

                if (input == null) {
                    throw IOException(
                        "Unable to read selected file"
                    )
                }

                FileOutputStream(out)
                    .use { output ->

                        input.copyTo(
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
                title,
                name,
                out.absolutePath,
                mime,
                folderId
            )
        )

        store.saveFiles(l)
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



    }
}
