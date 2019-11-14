package de.mel.auth.file

import de.mel.Lok
import de.mel.auth.file.AbstractFile.Configuration
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * Think of this as an abstraction of [java.io.File]. It is necessary cause Android 7+ restricts access to external storages via [java.io.File].
 * Before using [AbstractFile], call configure() and hand over a [Configuration]. This determines the implementation you want to use.
 * [DefaultFileConfiguration] uses [StandardFile] which wraps [File].
 */
abstract class AbstractFile<out T> {
    abstract val separator: String?
    /**
     * @param subFile
     * @return true if subFile is located in a subfolder of this instance.
     */
    fun hasSubContent(subFile: AbstractFile<*>): Boolean = subFile.absolutePath.startsWith(absolutePath)

    @get:Throws(IOException::class)
    abstract val canonicalPath: String?

    /**
     * creates common instances of [AbstractFile]s
     */
    abstract class Configuration {
        abstract fun instance(path: String): AbstractFile<Any>
        abstract fun separator(): String
        abstract fun instance(file: File): AbstractFile<Any>
        abstract fun instance(parent: AbstractFile<Any>, name: String): AbstractFile<Any>
        abstract fun instance(originalFile: AbstractFile<Any>): AbstractFile<Any>
    }

    open var path: String? = null

    abstract val name: String
    abstract val absolutePath: String
    abstract fun exists(): Boolean
    abstract val isFile: Boolean
    //    public abstract boolean move(T target);
    abstract val isDirectory: Boolean

    abstract fun length(): Long
    abstract fun listFiles(): Array<out T>
    abstract fun listDirectories(): Array<out T>
    abstract fun delete(): Boolean
    abstract val parentFile: T
    abstract fun mkdirs(): Boolean
    @Throws(FileNotFoundException::class)
    abstract fun inputStream(): InputStream?

    @Throws(IOException::class)
    abstract fun writer(): AbstractFileWriter?

    abstract val freeSpace: Long?
    abstract val usableSpace: Long?
    abstract fun lastModified(): Long?
    @Throws(IOException::class)
    abstract fun createNewFile(): Boolean

    abstract fun listContent(): Array<out T>?

    companion object {
        var configuration: Configuration? = null
            private set

        @JvmStatic
        fun instance(file: File): AbstractFile<Any> {
            return configuration!!.instance(file)
        }

        @JvmStatic
        fun instance(originalFile: AbstractFile<Any>): AbstractFile<Any> {
            return configuration!!.instance(originalFile)
        }

        @JvmStatic
        fun configure(configuration: Configuration) {
            if (Companion.configuration != null) {
                Lok.error("AFile implementation has already been set!")
                return
            } else {
                Companion.configuration = configuration
            }
        }

        /**
         * It just creates some sort of root element.
         *
         * @param path
         * @return
         */
        @JvmStatic
        fun instance(path: String): AbstractFile<Any> {
            if (configuration == null) Lok.error(AbstractFile::class.java.simpleName + ". NOT INITIALIZED! Call configure() before!")
            return configuration!!.instance(path)
        }

        @JvmStatic
        fun instance(parent: AbstractFile<Any>, name: String): AbstractFile<Any> {
            return configuration!!.instance(parent, name)
        }

        @JvmStatic
        fun separator(): String {
            return configuration!!.separator()
        }
    }
}