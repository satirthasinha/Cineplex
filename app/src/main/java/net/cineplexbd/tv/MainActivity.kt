package net.cineplexbd.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val homeUrl = "http://103.136.200.66/"
    private val executor = Executors.newFixedThreadPool(3)
    private lateinit var root: LinearLayout
    private lateinit var sidebar: LinearLayout
    private lateinit var main: FrameLayout
    private lateinit var web: WebView
    private var selected = "Home"
    private var expanded = true
    private val movies = mutableListOf<Movie>()

    data class Movie(val title: String, val href: String, val image: String)

    private val bg = Color.rgb(10, 11, 15)
    private val panel = Color.rgb(23, 25, 32)
    private val accent = Color.rgb(105, 151, 255)
    private val muted = Color.rgb(160, 164, 175)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        web = WebView(this).apply {
            visibility = View.GONE
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = 0
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url == homeUrl || (url?.startsWith(homeUrl) == true && url.count { it == '/' } <= 3)) {
                        extractHomeContent()
                    }
                }
            }
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(bg)
        }
        sidebar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(12), dp(20))
            setBackgroundColor(Color.rgb(16, 17, 22))
        }
        main = FrameLayout(this)
        root.addView(sidebar, LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(main, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        // Keep a hidden WebView attached so it can read the live portal catalogue.
        root.addView(web, LinearLayout.LayoutParams(1, 1))
        setContentView(root)
        buildSidebar()
        showHome()
        web.loadUrl(homeUrl)
    }

    private fun buildSidebar() {
        sidebar.removeAllViews()
        val brand = TextView(this).apply {
            text = "CINEPLEX"
            textSize = 22f
            letterSpacing = 0.16f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(dp(8), dp(4), 0, dp(30))
        }
        sidebar.addView(brand)
        val items = listOf(
            "⌂" to "Home", "▣" to "Movies", "▤" to "TV Series",
            "⌕" to "Search", "♡" to "My List", "◷" to "Recently Watched",
            "⚙" to "Settings"
        )
        items.forEach { (icon, label) ->
            val active = selected == label
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12), dp(13), dp(8), dp(13))
                background = if (active) rounded(accent, 14) else rounded(Color.TRANSPARENT, 14)
                isFocusable = true
                isFocusableInTouchMode = true
                setOnClickListener { navigate(label) }
                setOnFocusChangeListener { v, has ->
                    if (has) v.background = rounded(if (active) accent else Color.rgb(49, 53, 65), 14)
                    else v.background = rounded(if (selected == label) accent else Color.TRANSPARENT, 14)
                }
            }
            val glyph = TextView(this).apply {
                text = icon
                textSize = 22f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            row.addView(glyph, LinearLayout.LayoutParams(dp(34), dp(32)))
            if (expanded) {
                val title = TextView(this).apply {
                    text = label
                    textSize = 15f
                    setTextColor(Color.WHITE)
                    setPadding(dp(12), 0, 0, 0)
                    typeface = if (active) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                }
                row.addView(title)
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54))
            lp.bottomMargin = dp(5)
            sidebar.addView(row, lp)
        }
        val spacer = View(this)
        sidebar.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))
        val toggle = TextView(this).apply {
            text = if (expanded) "‹  Collapse menu" else "›"
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.CENTER
            isFocusable = true
            setOnClickListener {
                expanded = !expanded
                val width = if (expanded) 220 else 76
                root.removeView(sidebar)
                root.addView(sidebar, 0, LinearLayout.LayoutParams(dp(width), ViewGroup.LayoutParams.MATCH_PARENT))
                buildSidebar()
            }
        }
        sidebar.addView(toggle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)))
    }

    private fun navigate(label: String) {
        selected = label
        buildSidebar()
        when (label) {
            "Home" -> showHome()
            "Movies" -> showPortal("Movies", "Browse movies from the CinePlex catalogue.")
            "TV Series" -> showPortal("TV Series", "Browse available TV series.")
            "Search" -> showSearch()
            "My List" -> showMessage("My List", "Your saved titles will appear here.")
            "Recently Watched" -> showMessage("Recently Watched", "Your viewing history will appear here.")
            "Settings" -> showMessage("Settings", "CinePlex TV · Version 2.0")
        }
    }

    private fun showHome() {
        main.removeAllViews()
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(30), dp(22), dp(30), dp(36))
        }
        scroll.addView(content)
        main.addView(scroll)
        val top = TextView(this).apply {
            text = "Discover"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        content.addView(top)
        val sub = TextView(this).apply {
            text = "Movies and series, all in one place."
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(5), 0, dp(18))
        }
        content.addView(sub)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setPadding(dp(26), dp(24), dp(26), dp(24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(47, 57, 82), Color.rgb(23, 27, 39), Color.rgb(14, 15, 20))
            ).apply { cornerRadius = dp(22).toFloat() }
            minimumHeight = dp(220)
        }
        val eyebrow = TextView(this).apply {
            text = "CINEPLEX ORIGINAL EXPERIENCE"
            textSize = 11f
            letterSpacing = .12f
            setTextColor(Color.rgb(190, 204, 235))
        }
        hero.addView(eyebrow)
        val heroTitle = TextView(this).apply {
            text = movies.firstOrNull()?.title ?: "Your next favourite story starts here."
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, dp(7))
        }
        hero.addView(heroTitle)
        val heroDesc = TextView(this).apply {
            text = "Explore the CinePlex catalogue"
            textSize = 14f
            setTextColor(Color.rgb(211, 215, 225))
        }
        hero.addView(heroDesc)
        val browse = Button(this).apply {
            text = "Browse catalogue"
            isFocusable = true
            setOnClickListener { navigate("Movies") }
        }
        val bLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48))
        bLp.topMargin = dp(16)
        hero.addView(browse, bLp)
        content.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(250)))

        if (movies.isEmpty()) {
            val loading = TextView(this).apply {
                text = "Connecting to CinePlex…"
                textSize = 16f
                setTextColor(muted)
                setPadding(0, dp(28), 0, dp(12))
            }
            content.addView(loading)
            val retry = Button(this).apply {
                text = "Open CinePlex portal"
                setOnClickListener { openPortal(homeUrl) }
            }
            content.addView(retry)
        } else {
            addMovieRow(content, "Popular on CinePlex", movies.take(12))
            addMovieRow(content, "Explore more", movies.drop(12).take(12))
        }
    }

    private fun addMovieRow(parent: LinearLayout, title: String, items: List<Movie>) {
        if (items.isEmpty()) return
        val heading = TextView(this).apply {
            text = title
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(25), 0, dp(12))
        }
        parent.addView(heading)
        val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        horizontal.addView(row)
        items.forEach { movie ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                isFocusable = true
                isFocusableInTouchMode = true
                background = rounded(panel, 12)
                setPadding(dp(5), dp(5), dp(5), dp(8))
                setOnFocusChangeListener { v, focus ->
                    v.scaleX = if (focus) 1.06f else 1f
                    v.scaleY = if (focus) 1.06f else 1f
                    v.elevation = if (focus) dp(12).toFloat() else 0f
                }
                setOnClickListener { openPortal(movie.href) }
            }
            val poster = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.rgb(43, 46, 55))
            }
            card.addView(poster, LinearLayout.LayoutParams(dp(142), dp(190)))
            val name = TextView(this).apply {
                text = movie.title
                textSize = 12f
                maxLines = 2
                setTextColor(Color.WHITE)
                setPadding(dp(5), dp(8), dp(5), 0)
            }
            card.addView(name, LinearLayout.LayoutParams(dp(142), dp(42)))
            val lp = LinearLayout.LayoutParams(dp(152), dp(250))
            lp.rightMargin = dp(12)
            row.addView(card, lp)
            if (movie.image.isNotBlank()) loadImage(movie.image, poster)
        }
        parent.addView(horizontal)
    }

    private fun showPortal(title: String, description: String) {
        main.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(30), dp(28), dp(30), dp(24))
        }
        main.addView(layout)
        addHeading(layout, title, description)
        val open = Button(this).apply {
            text = "Open $title on CinePlex"
            setOnClickListener {
                val url = if (title == "TV Series") homeUrl + "tcategory.php" else homeUrl + "category.php"
                openPortal(url)
            }
        }
        layout.addView(open)
        val note = TextView(this).apply {
            text = "The supplied site exposes category pages, but its PHP backend and category identifiers were not included in the project. Open the original catalogue to browse the server's live categories."
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(18), 0, 0)
        }
        layout.addView(note)
    }

    private fun showSearch() {
        main.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(30), dp(28), dp(30), dp(24))
        }
        main.addView(layout)
        addHeading(layout, "Search", "Find titles in the catalogue loaded from CinePlex.")
        val input = EditText(this).apply {
            hint = "Search movies and series"
            textSize = 18f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setSingleLine(true)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = rounded(panel, 12)
        }
        layout.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)))
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(results)
        input.addTextChangedListener(SimpleTextWatcher { query ->
            results.removeAllViews()
            movies.filter { it.title.contains(query, true) }.take(15).forEach { movie ->
                val item = TextView(this).apply {
                    text = "▶   ${movie.title}"
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    setPadding(dp(12), dp(14), dp(12), dp(14))
                    isFocusable = true
                    setOnClickListener { openPortal(movie.href) }
                }
                results.addView(item)
            }
        })
        input.requestFocus()
    }

    private fun showMessage(title: String, message: String) {
        main.removeAllViews()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(36), dp(30), dp(36), dp(30))
        }
        main.addView(box)
        addHeading(box, title, message)
    }

    private fun addHeading(parent: LinearLayout, title: String, subtitle: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        parent.addView(TextView(this).apply {
            text = subtitle
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(6), 0, dp(22))
        })
    }

    private fun openPortal(url: String) {
        main.removeAllViews()
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
            setBackgroundColor(panel)
        }
        val back = Button(this).apply {
            text = "‹  Back"
            setOnClickListener { showHome() }
        }
        bar.addView(back)
        val label = TextView(this).apply {
            text = "CinePlex"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(dp(18), 0, 0, 0)
        }
        bar.addView(label)
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)))
        val browser = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = 0
            webViewClient = WebViewClient()
            loadUrl(url)
        }
        page.addView(browser, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        main.addView(page)
    }

    private fun extractHomeContent() {
        val js = """
            (function(){
              var out=[], seen={};
              document.querySelectorAll('a[href*="view.php"],a[href*="watch.php"],a[href*="tview.php"]').forEach(function(a){
                var href=a.href;
                if(seen[href]) return; seen[href]=1;
                var img=a.querySelector('img') || (a.parentElement && a.parentElement.querySelector('img'));
                var title=(a.innerText||a.getAttribute('title')||(img&&(img.alt||img.title))||'').trim().replace(/\s+/g,' ');
                if(!title && img) title=(img.alt||img.title||'').trim();
                if(!title) return;
                out.push({title:title.substring(0,100),href:href,image:img?img.src:''});
              });
              return JSON.stringify(out.slice(0,60));
            })();
        """.trimIndent()
        web.evaluateJavascript(js) { value ->
            try {
                val encoded = value ?: "\"[]\""
                val raw = org.json.JSONTokener(encoded).nextValue() as? String ?: "[]"
                val array = JSONArray(raw)
                val found = mutableListOf<Movie>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    found.add(Movie(obj.optString("title"), obj.optString("href"), obj.optString("image")))
                }
                if (found.isNotEmpty()) {
                    movies.clear()
                    movies.addAll(found)
                    if (selected == "Home") showHome()
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadImage(url: String, target: ImageView) {
        executor.execute {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 7000
                connection.readTimeout = 7000
                connection.setRequestProperty("User-Agent", "CinePlexTV/2.0")
                val bitmap = connection.inputStream.use { BitmapFactory.decodeStream(it) }
                connection.disconnect()
                if (bitmap != null) runOnUiThread { target.setImageBitmap(bitmap) }
            } catch (_: Exception) { }
        }
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (main.childCount > 0 && selected != "Home") {
                navigate("Home")
                return true
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            expanded = !expanded
            val width = if (expanded) 220 else 76
            root.removeView(sidebar)
            root.addView(sidebar, 0, LinearLayout.LayoutParams(dp(width), ViewGroup.LayoutParams.MATCH_PARENT))
            buildSidebar()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        web.stopLoading()
        web.destroy()
        super.onDestroy()
    }
}

private class SimpleTextWatcher(val changed: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s?.toString().orEmpty()) }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
