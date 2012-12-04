#!/usr/bin/env groovy
//@Grab("org.mortbay.jetty:jetty-embedded:6.1.26")
import groovy.json.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.awt.*
import javax.servlet.ServletException
import javax.servlet.http.*
import org.mortbay.jetty.Connector
import org.mortbay.jetty.Handler
import org.mortbay.jetty.Request
import org.mortbay.jetty.Server
import org.mortbay.jetty.handler.AbstractHandler
import groovy.util.logging.*

@Log
class FacebookApi {
    def appId
    def localHost = "localhost"
    def localPort = 9999
    def scopeList = []

    def String accessToken = ""
    def String expiresIn = ""

	def BASE_URL = "https://graph.facebook.com"
    def jsonSlurper = new JsonSlurper()
    def xmlSlurper = new XmlSlurper()

    void setAccessToken( String accessToken ) {
        this.accessToken = accessToken ?: ""
    }

    void setExpiresIn( String expiresIn ) {
        this.expiresIn = expiresIn ?: ""
    }

    void reset() {
        this.accessToken = ""
        this.expiresIn = ""
        Desktop.desktop.browse new URI("https://www.facebook.com/")
    }

    String auth() {
        assert appId
        assert localHost
        assert localPort
        assert accessToken?.empty
        assert expiresIn?.empty
        assert scopeList && !scopeList.empty

        def urltext = "https://www.facebook.com/dialog/oauth?" + [
                        "client_id=${appId}",
                        "redirect_uri=" + URLEncoder.encode("http://${localHost}:${localPort}/login_success.html", "utf-8"),
                        "scope=" + URLEncoder.encode(scopeList.join(","), "utf-8"),
                        "response_type=token",
                    ].join("&")

        Desktop.desktop.browse new URI(urltext)

        def server = new Server(localPort)
        server.with{
            connectors.each{
                it.host = "${localHost}"
            }

            handler = [
                handle:{ String target, HttpServletRequest request, HttpServletResponse response, int dispatch ->
                    switch(target) {
                        case "/login_success.html":
                            response.with{
                                status = HttpServletResponse.SC_OK
                                contentType = "text/html"
                                writer.withWriter{
                                    it.println("<html>")
                                    it.println("<head>")
                                    it.println("<script type='text/javascript'>")
                                    it.println("function init() {")
                                    it.println("    window.location = 'http://localhost:9999/access_token.html?' + window.location.toString().split('#')[1];");
                                    it.println("}")
                                    it.println("</script>")
                                    it.println("</head>")
                                    it.println("<body onload=\"init();\">")
                                    it.println("</body>")
                                    it.println("</html>")
                                }
                            }
                            break
                        case "/access_token.html":
                            synchronized(this){
                                accessToken = request.getParameter("access_token")
                                expiresIn = request.getParameter("expires_in")
                            }
                            response.with{
                                characterEncoding = "UTF-8"
                                status = HttpServletResponse.SC_OK
                                contentType = "text/html"
                                writer.withWriter{
                                    it.println("<html>")
                                    it.println("<head>")
                                    it.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />")
                                    it.println("<script type='text/javascript'>")
                                    it.println("function init() {")
                                    it.println("    window.close();");
                                    it.println("}")
                                    it.println("</script>")
                                    it.println("</head>")
                                    it.println("<body onload=\"init();\">")
                                    it.println("<p>認証が正常に終了しました。</p>")
                                    it.println("<p>こちらのページは閉じていただいて構いません。</p>")
                                    it.println("</body>")
                                    it.println("</html>")
                                }
                            }
                            break
                        default:
                            response.status = HttpServletResponse.SC_NOT_FOUND
                            break
                    }

                }
            ] as AbstractHandler

        }
        server.start()

        while ( ( accessToken?:"" ).empty ) {
            Thread.sleep(1000)
        }
        server.stop()
        server.join()

        this.accessToken
    }

	def get( path, params = [:] ) {
	    jsonSlurper.parseText(
	        new URL("${BASE_URL}/${path}?" + buildParamText( params )).text
	    )
	}

    private buildParamText( params ) {
       ([ "access_token":accessToken ] + params).collect{ it.key + "=" + it.value }.join("&")
    }

	def post( path, params = [:] ) {
        log.info "post start."
        log.info "path:${path}"
        log.info "params:${params}"
	    new URL(
	            "${BASE_URL}/${path}"
        ).openConnection().with{
            doOutput = true
            setRequestProperty("User-Agent", "JsonSlurper")
            setRequestProperty("Accept-Language", "ja")

            outputStream.withWriter {
                it << buildParamText( params )
            }

            inputStream.text
        }
        log.info "post end."
	}

	def fql( query ) {
        def params = [ "query":java.net.URLEncoder.encode(query, "UTF-8") ]
	    xmlSlurper.parseText(
	        new URL("https://api.facebook.com/method/fql.query?" + buildParamText( params )).text
	    )
	}

}
