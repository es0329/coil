@file:Suppress("MemberVisibilityCanBePrivate", "NOTHING_TO_INLINE", "unused")

package coil

import android.app.Application
import android.content.Context
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.RequestDisposable
import coil.request.RequestResult
import coil.util.CoilContentProvider

/**
 * A singleton that holds the default [ImageLoader] instance.
 */
object Coil {

    private var imageLoader: ImageLoader? = null
    private var imageLoaderFactory: ImageLoaderFactory? = null

    /** @see imageLoader */
    @Deprecated(
        message = "Migrate to imageLoader(context).",
        replaceWith = ReplaceWith("this.imageLoader(context)")
    )
    @JvmStatic
    fun loader(): ImageLoader = imageLoader(CoilContentProvider.context)

    /**
     * Get the default [ImageLoader]. Creates a new instance if none has been set.
     */
    @JvmStatic
    fun imageLoader(context: Context): ImageLoader = imageLoader ?: newImageLoader(context)

    /**
     * Set the default [ImageLoader]. Shutdown the current instance if there is one.
     */
    @JvmStatic
    fun setImageLoader(loader: ImageLoader) {
        setImageLoader(object : ImageLoaderFactory {
            override fun newImageLoader() = loader
        })
    }

    /**
     * Convenience function to get the default [ImageLoader] and execute the [request].
     *
     * @see ImageLoader.execute
     */
    @JvmStatic
    inline fun execute(request: LoadRequest): RequestDisposable {
        return imageLoader(request.context).execute(request)
    }

    /**
     * Convenience function to get the default [ImageLoader] and execute the [request].
     *
     * @see ImageLoader.execute
     */
    @JvmStatic
    suspend inline fun execute(request: GetRequest): RequestResult {
        return imageLoader(request.context).execute(request)
    }

    /**
     * Set the [ImageLoaderFactory] that will be used to create the default [ImageLoader].
     * Shutdown the current instance if there is one. The [factory] is guaranteed to be called at most once.
     *
     * Using this method to set an explicit [factory] takes precedence over an [Application] that
     * implements [ImageLoaderFactory].
     */
    @JvmStatic
    @Synchronized
    fun setImageLoader(factory: ImageLoaderFactory) {
        imageLoaderFactory = factory

        // Shutdown the image loader after clearing the reference.
        val loader = imageLoader
        imageLoader = null
        loader?.shutdown()
    }

    /** @see setImageLoader */
    @Deprecated(
        message = "Migrate to setImageLoader(loader).",
        replaceWith = ReplaceWith("this.setImageLoader(loader)")
    )
    @JvmStatic
    fun setDefaultImageLoader(loader: ImageLoader) = setImageLoader(loader)

    /** @see setImageLoader */
    @Deprecated(
        message = "Migrate to setDefaultImageLoader(ImageLoaderFactory).",
        replaceWith = ReplaceWith(
            expression = "" +
                "this.setImageLoader(object : ImageLoaderFactory {" +
                "    override fun getImageLoader() {" +
                "        return initializer()" +
                "    }" +
                "})",
            imports = ["coil.ImageLoaderFactory"]
        )
    )
    @JvmStatic
    fun setDefaultImageLoader(initializer: () -> ImageLoader) {
        setImageLoader(object : ImageLoaderFactory {
            override fun newImageLoader() = initializer()
        })
    }

    /** Create and set the new default [ImageLoader]. */
    @Synchronized
    private fun newImageLoader(context: Context): ImageLoader {
        // Check again in case imageLoader was just set.
        imageLoader?.let { return it }

        // Create a new ImageLoader.
        val loader = imageLoaderFactory?.newImageLoader()
            ?: (context.applicationContext as? ImageLoaderFactory)?.newImageLoader()
            ?: ImageLoader(context)
        imageLoaderFactory = null
        setImageLoader(loader)
        return loader
    }
}
