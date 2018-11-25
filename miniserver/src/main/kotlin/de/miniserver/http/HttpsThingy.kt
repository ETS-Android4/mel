package de.miniserver.http

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsParameters
import com.sun.net.httpserver.HttpsServer
import de.mein.DeferredRunnable
import de.mein.Lok
import de.mein.MeinThread
import de.mein.auth.tools.N
import de.miniserver.MiniServer
import de.miniserver.data.FileRepository
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import javax.net.ssl.SSLContext


class HttpsThingy(private val port: Int, private val miniServer: MiniServer, private val fileRepository: FileRepository) : DeferredRunnable() {
    override fun onShutDown() {

    }

    override fun runImpl() {
        start()
    }

    override fun getRunnableName(): String = "HTTP"


    private lateinit var serverSocket: ServerSocket
    private val threadSemaphore = Semaphore(1, true)
    private val threadQueue = LinkedList<MeinThread>()
    val pageProcessor = PageProcessor()

    private lateinit var server: HttpsServer
    private val threadFactory = { r: Runnable ->
        var meinThread: MeinThread? = null

        try {
            threadSemaphore.acquire()
            meinThread = threadQueue.poll()
            threadSemaphore.release()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        meinThread
    }
    private val executor = Executors.newFixedThreadPool(2)

    private fun readPostValue(ex: HttpExchange, key: String, size: Int = 40): String? {
        val bytes = ByteArray(size)
        ex.requestBody.read(bytes)
        val string = String(bytes)
        val found = string.split("&").first { URLDecoder.decode(it).substring(0, it.indexOf("=")) == key }
        val decoded = URLDecoder.decode(found).substring(key.length+1)
        return decoded
    }

    fun start() {
        val pageHello = pageProcessor.load("/de/miniserver/hello.html",
                Replacer("files") {
                    val s = StringBuilder()
                    miniServer.fileRepository.hashFileMap.entries.forEach { it ->
                        s.append("<p><a href=\"files/${it.key}\" download=\"${it.value.name}\">${it.value.name}</a> ${it.key}</p>")
                    }
                    return@Replacer s.toString()
                })
        val pageIndexLogin = pageProcessor.load("/de/miniserver/index.html")

        Lok.debug("binding http to           : $port")
        server = createServer()
        Lok.debug("successfully bound http to: $port")
        server.createContext("/") {
            if (it.requestMethod == "POST") {

                val pw = readPostValue(it, "pw")

                Lok.debug("k")
            } else
                answerPage(it, pageIndexLogin)
        }
        server.createContext("/test") {
            answerPage(it, pageHello)
        }

        // add files context
        server.createContext("/files/") {
            val uri = it.requestURI
            val hash = uri.path.substring("/files/".length, uri.path.length)
            Lok.debug("serving file: ${hash}")
            try {
                val bytes = miniServer.fileRepository.getBytes(hash)
                with(it) {
                    sendResponseHeaders(200, bytes.size.toLong())
                    responseBody.write(bytes)
                    responseBody.close()
                }
            } catch (e: Exception) {
                Lok.debug("did not find a file for ${hash}")
                /**
                 * does not work yet
                 */
                with(it) {
                    val response = "file not found".toByteArray()
                    sendResponseHeaders(404, response.size.toLong())
                    responseBody.write(response)
                    responseBody.close()
                }
            }

        }
        server.executor = executor
        server.start()
        Lok.debug("http is up")
        N.r {


        }
    }

    private fun answerPage(ex: HttpExchange, page: Page?) {
        with(ex) {
            Lok.debug("sending '${page?.path}' to $remoteAddress")
            sendResponseHeaders(200, page?.bytes?.size?.toLong() ?: "404".toByteArray().size.toLong())
            responseBody.write(page?.bytes ?: "404".toByteArray())
            responseBody.close()
        }
    }

    private fun createServer(): HttpsServer {
        val server = HttpsServer.create(InetSocketAddress(port), 0)
        val configurator = object : HttpsConfigurator(miniServer.certificateManager.sslContext) {
            override fun configure(params: HttpsParameters) {
                try {
                    // initialise the SSL context
                    val c = SSLContext.getDefault()
                    val engine = c.createSSLEngine()
                    params.needClientAuth = false
                    params.cipherSuites = engine.enabledCipherSuites
                    params.protocols = engine.enabledProtocols

                    // get the default parameters
                    val defaultSSLParameters = c.defaultSSLParameters
                    params.setSSLParameters(defaultSSLParameters)
                } catch (ex: Exception) {
                    Lok.error(ex)
                    Lok.error("Failed to create HTTPS port")
                }

            }
        }
        server.httpsConfigurator = configurator
        return server
    }


}