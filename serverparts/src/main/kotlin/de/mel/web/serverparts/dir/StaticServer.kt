package de.mel.web.serverparts.dir

import com.sun.net.httpserver.HttpServer
import de.mel.Lok
import de.mel.auth.data.access.CertificateManager
import de.mel.auth.tools.F
import de.mel.konsole.DependenciesContainer
import de.mel.konsole.Konsole
import de.mel.sql.RWLock
import de.mel.web.serverparts.AbstractHttpsThingy
import de.mel.web.serverparts.HttpContextCreator
import de.mel.web.serverparts.Page
import de.mel.web.serverparts.SetupHelper
import java.io.File
import java.util.*

/**
 * Publishes a directory structure via HTTP(s).
 * See main method for arguments this class digests.
 */
class StaticServer(private val certificateManager: CertificateManager?, val settings: DirSettings) : AbstractHttpsThingy(settings.port, certificateManager?.sslContext) {
    override fun configureContext(server: HttpServer) {
        val contextCreator = HttpContextCreator(server)
        var rootIndexHtml: File? = null
        val stack = Stack<File>()
        Lok.info("indexing dir=${settings.dir!!.canonicalPath}")
        settings.dir!!.walkTopDown().onEnter {
            if (it != settings.dir)
                stack.push(it)
            true
        }.onLeave {
            if (!stack.empty())
                stack.pop()
        }.forEach {
            var subUrl = "${stack.fold("/") { acc: String, file: File? -> "$acc${file!!.name}/" }}${it.name}"
            Lok.debug("mapping $subUrl to ${it.canonicalPath}")
            if (stack.size == 0 && it.name == "index.html")
                rootIndexHtml = it

            contextCreator.createContext(subUrl)
                    .withGET()
                    .handle { httpExchange, _ ->
                        val contentType = F.readMimeType(it)
                        respondPage(httpExchange, Page(path = subUrl, file = it), contentType = contentType)
                    }
        }
        if (rootIndexHtml != null) {
            contextCreator.createContext("/")
                    .withGET()
                    .handle { httpExchange, _ -> redirect(httpExchange, "index.html") }
        }
    }

    override fun getRunnableName(): String = "static page server"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val settings = DirSettings()
            val konsole = Konsole<DirSettings>(settings)
            konsole.mandatory("-dir", "set the directory to publish") { result, a -> result.dir = File(a[0]) }
                    .optional("-port", "port the server listens on") { result, a -> result.port = a[0].toInt() }
                    .optional("-https", "enables HTTPS") { result, a -> result.https = true }
                    .optional("-certdir", "set the dir where to store/load the servers certificates", { result, a -> result.workingDir = File(a[0]) }, DependenciesContainer.DependencySet("-https"))
            konsole.handle(args)
            if (!settings.workingDir.exists())
                settings.workingDir.mkdirs()

            var httpCertificateManager: CertificateManager? = null
            if (settings.https) {
                val sqlQueries = SetupHelper.setupMelAuthSqlqueries(settings.workingDir)
                httpCertificateManager = CertificateManager(settings.workingDir, sqlQueries, 2048)
            }
            val pageServer = StaticServer(httpCertificateManager, settings)
            pageServer.start()
            val lock = RWLock()
            lock.lockWrite().lockWrite()
            Lok.debug("exiting... bye")
        }
    }
}