package de.mel.android.file

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import de.mel.auth.file.AbstractFile
import de.mel.auth.file.IFile
import java.io.File

class SAFFileConfiguration(context: Context) : AbstractFile.Configuration<SAFFile>() {
    private var internalCache: DocFileCache? = null
    private var externalCache: DocFileCache? = null

    val androidFileConfiguration = AndroidFileConfiguration(context)

    @Throws(SAFAccessor.SAFException::class)
    fun getInternalDoc(parts: Array<String>): DocumentFile? {
        if (internalCache == null) internalCache = DocFileCache(SAFAccessor.getInternalRootDocFile(), 50)
        return internalCache!!.findDoc(parts)
    }

    @Throws(SAFAccessor.SAFException::class)
    fun getExternalDoc(parts: Array<String>): DocumentFile? {
        if (externalCache == null) externalCache = DocFileCache(SAFAccessor.getExternalRootDocFile(), 50)
        return externalCache!!.findDoc(parts)
    }

    override fun instance(path: String): SAFFile = SAFFile(path)

    override fun instance(file: File): SAFFile = SAFFile(file.absolutePath)

    override fun instance(parent: SAFFile, name: String): SAFFile = SAFFile(parent, name)

    override fun separator(): String = androidFileConfiguration.separator()

    override fun instance(originalFile: SAFFile): SAFFile = SAFFile(originalFile)

}