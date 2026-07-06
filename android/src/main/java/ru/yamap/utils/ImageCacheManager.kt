package ru.yamap.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import java.io.BufferedInputStream
import java.net.URL

class ImageCacheManager {
    companion object {
        private const val TAG = "YamapImageCache"

        private val imageCache = mutableMapOf<String, Bitmap>()

        // Returns null (instead of throwing / returning a null-typed-as-nonnull
        // bitmap) when the source cannot be decoded. Callers must fall back.
        fun getBitmapSync(context: Context, url: String): Bitmap? {
            imageCache[url]?.let { return it }

            val bitmap: Bitmap? = try {
                if (url.contains("http://") || url.contains("https://")) {
                    val aURL = URL(url)
                    val conn = aURL.openConnection()
                    conn.connect()
                    conn.getInputStream().use { stream ->
                        BufferedInputStream(stream).use { bis ->
                            BitmapFactory.decodeStream(bis)
                        }
                    }
                } else if (url.contains("data:image")) {
                    val pureBase64Encoded = url.substring(url.indexOf(",") + 1)
                    val decodedString = Base64.decode(pureBase64Encoded, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                } else if (url.startsWith("file://")) {
                    BitmapFactory.decodeFile(url.removePrefix("file://"))
                } else {
                    val id = context.resources.getIdentifier(url, "drawable", context.packageName)
                    if (id == 0) null else BitmapFactory.decodeResource(context.resources, id)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load image from source: $url", e)
                null
            }

            if (bitmap == null) {
                Log.w(TAG, "Could not decode image from source: $url")
            } else {
                imageCache[url] = bitmap
            }

            return bitmap
        }

        private fun downloadImageBitmap(context: Context, url: String, cb: Callback<Bitmap?>) {
            object : Thread() {
                override fun run() {
                    // getBitmapSync never throws; the callback is ALWAYS posted
                    // (with null on failure) so callers can fall back instead of
                    // silently dropping the map object they were building.
                    val bitmap = getBitmapSync(context, url)
                    Handler(Looper.getMainLooper()).post { cb.invoke(bitmap) }
                }
            }.start()
        }

        fun getImage(context: Context, source: String, setImage: (image: Bitmap?) -> Unit) {
            imageCache[source]?.let {
                setImage(it)
                return
            }

            downloadImageBitmap(context, source, object : Callback<Bitmap?> {
                override fun invoke(arg: Bitmap?) {
                    setImage(arg)
                }
            })
        }
    }
}
