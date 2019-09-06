package de.mein.serverparts

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpsServer
import de.mein.Lok
import de.mein.auth.tools.N

open class HttpContextCreator(val server: HttpsServer) {


    /**
     * checks if the requirements (request parameters) are met and executes code dealing with it.
     * call expect to check parameters. if all Expectations are met, the cuntion you hand over to
     * Expectation.handle() is executed. If no Expectations are present, the function handed over in RequestHandeler.handle()
     * will allways be exectuted when the server context is matched.
     */
    class RequestHandler(internal val contextInit: ContextInit) {
        var handleFunction: ((HttpExchange, QueryMap) -> Boolean)? = null
        var isGeneralHandler = true

        /**
         * checks whether parameters of the request matches a criterion
         * @param key key in the parameter map
         * @param expectedValue the value you expect
         */
        fun expect(key: String, expectedValue: String?): Expectation {
            return expect(key) { it == expectedValue }
        }

        /**
         * checks whether parameters of the request matches a criterion
         * @param key key in the parameter map
         * @param expectFunction if you expect more sophisticated things
         */
        fun expect(key: String, expectFunction: (String?) -> Boolean): Expectation {
            isGeneralHandler = false
            val expectation = Expectation(this, key, expectFunction)
            return expectation
        }

        fun handle(handleFunction: (HttpExchange, QueryMap) -> Unit): ContextInit {
            // wrap and return true
            this.handleFunction = { httpExchange, queryMap ->
                handleFunction.invoke(httpExchange, queryMap)
                true
            }

            return contextInit
        }

        internal fun onContextCalled(httpExchange: HttpExchange, queryMap: QueryMap): Boolean {
            if (handleFunction == null)
                return false
            return handleFunction!!.invoke(httpExchange, queryMap)
        }
    }


    open class ContextInit {
        private var errorFunction: ((HttpExchange, Exception) -> Unit)? = null


        private val fallbackErrorFunction: ((HttpExchange, Exception) -> Unit) = { httpExchange, exception ->
            try {
                with(httpExchange) {
                    val text = "something went wrong"
                    de.mein.Lok.debug("sending error to $remoteAddress")
                    sendResponseHeaders(400, text.toByteArray().size.toLong())
                    responseBody.write(text.toByteArray())
                    responseBody.close()
                }
            } finally {
                httpExchange.close()
            }
        }


        internal fun contextCalled(httpExchange: HttpExchange) {
            val queryMap = QueryMap()
            if (httpExchange.requestMethod == "POST") {
                queryMap.fillFomPost(httpExchange)
                runRequestHandlers(postHandlers, queryMap, httpExchange)
            } else if (httpExchange.requestMethod == "GET") {
                queryMap.fillFromGet(httpExchange)
                runRequestHandlers(getHandlers, queryMap, httpExchange)
            } else
                Lok.error("unknown request method; ${httpExchange.requestMethod}")
        }

        fun runRequestHandlers(requestHandlers: List<RequestHandler>, queryMap: QueryMap, httpExchange: HttpExchange) {
            var result: Boolean? = null
            try {
                N.forEachAdv(requestHandlers) { stoppable, index, it ->
                    if (it.onContextCalled(httpExchange, queryMap)) {
                        stoppable.stop()
                        result = true
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                onExceptionThrown(httpExchange, e)
            }
            // execution has failed
            if (result == null)
                onErrorFunctionNullorFailed(httpExchange, java.lang.Exception("did nothing"))
        }


        fun onExceptionThrown(httpExchange: HttpExchange, e: Exception) {
            Lok.debug("error happened")
            e.printStackTrace()
            if (errorFunction != null) {
                try {
                    errorFunction!!.invoke(httpExchange, e)
                } catch (ee: Exception) {
                    onErrorFunctionNullorFailed(httpExchange, ee)
                }
            }
        }

        fun withPOST(): RequestHandler {
            val handler = RequestHandler(this)
            postHandlers.add(handler)
            return handler
        }

        fun withGET(): RequestHandler {
            val handler = RequestHandler(this)
            getHandlers.add(handler)
            return handler
        }

        val postHandlers = mutableListOf<RequestHandler>()
        val getHandlers = mutableListOf<RequestHandler>()

        fun onError(onErrorFunction: (HttpExchange, Exception) -> Unit): ContextInit {
            this.errorFunction = onErrorFunction
            return this
        }

        private fun onErrorFunctionNullorFailed(httpExchange: HttpExchange, ee: Exception) {
            fallbackErrorFunction.invoke(httpExchange, ee)
        }
    }

    fun createContext(path: String): ContextInit {
        val contextContainer = ContextInit()
        server.createContext(path) { ex ->
            contextContainer.contextCalled(ex)
        }
        return contextContainer
    }
}