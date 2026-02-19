package com.example.kimiwebapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

/**
 * Kimi WebView App - Enhanced Version
 * 
 * Features:
 * - Default URL: https://kimi.moonshot.cn
 * - Custom URL support with SharedPreferences
 * - Quick navigation shortcuts
 * - Browse history
 * - Bookmarks
 * - Desktop mode toggle
 * - Fullscreen mode
 * - Night/Day mode
 * - Clear cache
 * - Share page
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var urlInput: TextInputEditText
    private lateinit var btnGo: ImageButton
    private lateinit var btnMore: ImageButton
    private lateinit var fabHome: FloatingActionButton
    private lateinit var layoutUrlBar: View

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val TAG = "KimiWebApp"

    // Preferences
    private lateinit var prefs: SharedPreferences
    private val PREFS_NAME = "KimiWebAppPrefs"
    private val KEY_DEFAULT_URL = "default_url"
    private val KEY_BOOKMARKS = "bookmarks"
    private val KEY_HISTORY = "history"
    private val KEY_DESKTOP_MODE = "desktop_mode"
    private val KEY_DARK_MODE = "dark_mode"

    // Default URLs - Using the correct URL
    companion object {
        const val DEFAULT_URL = "https://kimi.moonshot.cn"

        // Quick shortcuts
        val QUICK_SHORTCUTS = listOf(
            Pair("Kimi", "https://kimi.moonshot.cn"),
            Pair("ChatGPT", "https://chat.openai.com"),
            Pair("Claude", "https://claude.ai"),
            Pair("Gemini", "https://gemini.google.com"),
            Pair("百度", "https://www.baidu.com"),
            Pair("Google", "https://www.google.com"),
            Pair("GitHub", "https://github.com"),
            Pair("知乎", "https://www.zhihu.com")
        )
    }

    private val REQUEST_PERMISSIONS = 1
    private var isDesktopMode = false
    private var isFullscreen = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize preferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Apply dark mode
        applyDarkMode()

        setContentView(R.layout.activity_main)

        // Initialize views
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        urlInput = findViewById(R.id.urlInput)
        btnGo = findViewById(R.id.btnGo)
        btnMore = findViewById(R.id.btnMore)
        fabHome = findViewById(R.id.fabHome)
        layoutUrlBar = findViewById(R.id.layoutUrlBar)

        // Request permissions
        checkAndRequestPermissions()

        // Setup WebView
        setupWebView()

        // Setup UI interactions
        setupUI()

        // Load default or saved URL
        if (savedInstanceState == null) {
            loadDefaultUrl()
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    private fun applyDarkMode() {
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun loadDefaultUrl() {
        val savedUrl = prefs.getString(KEY_DEFAULT_URL, DEFAULT_URL)
        val urlToLoad = if (savedUrl.isNullOrEmpty()) DEFAULT_URL else savedUrl
        Log.d(TAG, "Loading URL: $urlToLoad")
        webView.loadUrl(urlToLoad)
    }

    private fun setupUI() {
        // URL input
        urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                navigateToUrl()
                true
            } else {
                false
            }
        }

        // Go button
        btnGo.setOnClickListener {
            navigateToUrl()
        }

        // More options menu
        btnMore.setOnClickListener {
            showMoreMenu()
        }

        // Home FAB
        fabHome.setOnClickListener {
            showQuickShortcuts()
        }

        // Swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            webView.reload()
        }

        // Long press on URL bar to edit default URL
        layoutUrlBar.setOnLongClickListener {
            showSetDefaultUrlDialog()
            true
        }
    }

    private fun navigateToUrl() {
        var url = urlInput.text.toString().trim()
        if (url.isEmpty()) return

        // Add https:// if no protocol specified
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        webView.loadUrl(url)
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(urlInput.windowToken, 0)
    }

    private fun showQuickShortcuts() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_shortcuts, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerShortcuts)
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        recyclerView.adapter = ShortcutsAdapter(QUICK_SHORTCUTS) { name, url ->
            webView.loadUrl(url)
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnBookmarks).setOnClickListener {
            dialog.dismiss()
            showBookmarks()
        }

        view.findViewById<Button>(R.id.btnHistory).setOnClickListener {
            dialog.dismiss()
            showHistory()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showMoreMenu() {
        val popup = PopupMenu(this, btnMore)
        popup.menuInflater.inflate(R.menu.menu_more, popup.menu)

        // Update menu item titles based on current state
        popup.menu.findItem(R.id.menu_desktop_mode).title = 
            if (isDesktopMode) "切换到手机版" else "切换到桌面版"
        popup.menu.findItem(R.id.menu_dark_mode).title = 
            if (prefs.getBoolean(KEY_DARK_MODE, false)) "切换到日间模式" else "切换到夜间模式"

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_share -> shareCurrentPage()
                R.id.menu_bookmark -> addBookmark()
                R.id.menu_desktop_mode -> toggleDesktopMode()
                R.id.menu_dark_mode -> toggleDarkMode()
                R.id.menu_fullscreen -> toggleFullscreen()
                R.id.menu_clear_cache -> clearCache()
                R.id.menu_set_default -> showSetDefaultUrlDialog()
                R.id.menu_about -> showAboutDialog()
            }
            true
        }

        popup.show()
    }

    private fun shareCurrentPage() {
        val url = webView.url ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_SUBJECT, webView.title)
        }
        startActivity(Intent.createChooser(intent, "分享页面"))
    }

    private fun addBookmark() {
        val url = webView.url ?: return
        val title = webView.title ?: "未命名"

        val bookmarks = getBookmarks()
        val bookmark = JSONObject().apply {
            put("title", title)
            put("url", url)
            put("timestamp", System.currentTimeMillis())
        }
        bookmarks.put(bookmark)
        saveBookmarks(bookmarks)

        Toast.makeText(this, "已添加到书签", Toast.LENGTH_SHORT).show()
    }

    private fun getBookmarks(): JSONArray {
        val json = prefs.getString(KEY_BOOKMARKS, "[]")
        return JSONArray(json)
    }

    private fun saveBookmarks(bookmarks: JSONArray) {
        prefs.edit().putString(KEY_BOOKMARKS, bookmarks.toString()).apply()
    }

    private fun showBookmarks() {
        val bookmarks = getBookmarks()
        if (bookmarks.length() == 0) {
            Toast.makeText(this, "暂无书签", Toast.LENGTH_SHORT).show()
            return
        }

        val items = Array(bookmarks.length()) { i ->
            val obj = bookmarks.getJSONObject(i)
            obj.getString("title")
        }

        AlertDialog.Builder(this)
            .setTitle("书签")
            .setItems(items) { _, which ->
                val url = bookmarks.getJSONObject(which).getString("url")
                webView.loadUrl(url)
            }
            .setPositiveButton("管理书签") { _, _ ->
                showBookmarkManager()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showBookmarkManager() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_bookmarks, null)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerBookmarks)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val bookmarks = getBookmarks()
        val bookmarkList = mutableListOf<Pair<String, String>>()
        for (i in 0 until bookmarks.length()) {
            val obj = bookmarks.getJSONObject(i)
            bookmarkList.add(Pair(obj.getString("title"), obj.getString("url")))
        }

        recyclerView.adapter = BookmarkAdapter(bookmarkList,
            onClick = { _, url ->
                webView.loadUrl(url)
                dialog.dismiss()
            },
            onDelete = { position ->
                val newBookmarks = JSONArray()
                for (i in 0 until bookmarks.length()) {
                    if (i != position) {
                        newBookmarks.put(bookmarks.getJSONObject(i))
                    }
                }
                saveBookmarks(newBookmarks)
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                showBookmarkManager()
            }
        )

        dialog.setContentView(view)
        dialog.show()
    }

    private fun addToHistory(url: String, title: String) {
        val history = getHistory()
        val item = JSONObject().apply {
            put("title", title)
            put("url", url)
            put("timestamp", System.currentTimeMillis())
        }

        val newHistory = JSONArray()
        newHistory.put(item)

        for (i in 0 until minOf(history.length(), 99)) {
            newHistory.put(history.getJSONObject(i))
        }

        prefs.edit().putString(KEY_HISTORY, newHistory.toString()).apply()
    }

    private fun getHistory(): JSONArray {
        val json = prefs.getString(KEY_HISTORY, "[]")
        return JSONArray(json)
    }

    private fun showHistory() {
        val history = getHistory()
        if (history.length() == 0) {
            Toast.makeText(this, "暂无历史记录", Toast.LENGTH_SHORT).show()
            return
        }

        val items = Array(history.length()) { i ->
            val obj = history.getJSONObject(i)
            val title = obj.getString("title")
            val url = obj.optString("url", "").take(40) + "..."
            "$title\n$url"
        }

        AlertDialog.Builder(this)
            .setTitle("历史记录")
            .setItems(items) { _, which ->
                val url = history.getJSONObject(which).getString("url")
                webView.loadUrl(url)
            }
            .setPositiveButton("清除历史") { _, _ ->
                prefs.edit().remove(KEY_HISTORY).apply()
                Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        prefs.edit().putBoolean(KEY_DESKTOP_MODE, isDesktopMode).apply()

        val userAgent = if (isDesktopMode) {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            null
        }

        webView.settings.userAgentString = userAgent
        webView.reload()

        Toast.makeText(this, if (isDesktopMode) "已切换到桌面版" else "已切换到手机版", Toast.LENGTH_SHORT).show()
    }

    private fun toggleDarkMode() {
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false)
        prefs.edit().putBoolean(KEY_DARK_MODE, !isDarkMode).apply()
        recreate()
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen

        if (isFullscreen) {
            layoutUrlBar.visibility = View.GONE
            fabHome.visibility = View.GONE
            supportActionBar?.hide()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let {
                    it.hide(android.view.WindowInsets.Type.statusBars() or 
                            android.view.WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
        } else {
            layoutUrlBar.visibility = View.VISIBLE
            fabHome.visibility = View.VISIBLE
            supportActionBar?.show()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.show(android.view.WindowInsets.Type.statusBars() or 
                        android.view.WindowInsets.Type.navigationBars())
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }

        Toast.makeText(this, if (isFullscreen) "全屏模式 (点击返回退出)" else "已退出全屏", Toast.LENGTH_SHORT).show()
    }

    private fun clearCache() {
        AlertDialog.Builder(this)
            .setTitle("清除缓存")
            .setMessage("确定要清除所有缓存数据吗？")
            .setPositiveButton("确定") { _, _ ->
                webView.clearCache(true)
                webView.clearHistory()
                CookieManager.getInstance().removeAllCookies(null)
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSetDefaultUrlDialog() {
        val currentUrl = prefs.getString(KEY_DEFAULT_URL, DEFAULT_URL) ?: DEFAULT_URL

        val input = EditText(this).apply {
            setText(currentUrl)
            hint = "输入默认网址"
        }

        AlertDialog.Builder(this)
            .setTitle("设置默认网址")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    prefs.edit().putString(KEY_DEFAULT_URL, newUrl).apply()
                    Toast.makeText(this, "默认网址已设置", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("恢复默认") { _, _ ->
                prefs.edit().putString(KEY_DEFAULT_URL, DEFAULT_URL).apply()
                Toast.makeText(this, "已恢复默认网址", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于 Kimi WebView")
            .setMessage("""
                Kimi WebView App v1.1.0

                功能特点：
                • 支持自定义默认网址
                • 快捷导航书签
                • 浏览历史记录
                • 桌面/手机模式切换
                • 夜间模式
                • 全屏浏览

                默认网址：https://kimi.moonshot.cn
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            saveFormData = true
            javaScriptCanOpenWindowsAutomatically = true

            // Apply desktop mode if enabled
            if (prefs.getBoolean(KEY_DESKTOP_MODE, false)) {
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                isDesktopMode = true
            }
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d(TAG, "Loading URL: $url")

                return when {
                    url.startsWith("tel:") -> {
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))
                        true
                    }
                    url.startsWith("mailto:") -> {
                        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
                        true
                    }
                    url.startsWith("intent://") -> {
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                true
                            } else {
                                false
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }
                    else -> false
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
                urlInput.setText(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefreshLayout.isRefreshing = false
                progressBar.visibility = View.GONE
                CookieManager.getInstance().flush()

                url?.let { addToHistory(it, view?.title ?: "") }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "Error loading page: ${error?.description}")
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                handler?.proceed()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                openFileChooser()
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                runOnUiThread {
                    request?.grant(request.resources)
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d(TAG, "Console: ${consoleMessage?.message()}")
                return true
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.addRequestHeader("User-Agent", userAgent)
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, 
                URLUtil.guessFileName(url, contentDisposition, mimeType))

            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(applicationContext, "开始下载...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        fileChooserLauncher.launch(Intent.createChooser(intent, "选择文件"))
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results: Array<Uri>? = when {
                data?.clipData != null -> {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_PERMISSIONS)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isFullscreen) {
                toggleFullscreen()
                return true
            }
            if (webView.canGoBack()) {
                webView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
