import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.apache.log4j.BasicConfigurator
import org.symphonyoss.client.SymphonyClient
import org.symphonyoss.client.SymphonyClientConfig
import org.symphonyoss.client.SymphonyClientFactory
import org.symphonyoss.client.events.SymEvent
import org.symphonyoss.symphony.clients.model.*


fun main(args: Array<String>) {
    BasicConfigurator.configure()

    val symphony = connectToSymphony()

    val stream = getRoomStream(symphony)
    val aMessage = SymMessage()
    aMessage.messageText = "oh hai room"
    symphony.messageService.sendMessage(stream, aMessage)

    startWebServer(symphony, stream)

    watchForSymphonyMessages(symphony)
}

private fun getProductionUsers() : Array<String> {
    return arrayOf("wells.powell@bnpparibas.com", "jackie.wong@uk.bnpparibas.com")
}

private fun getDevelopmentUsers() : Array<String> {
    return arrayOf("jason.field@uk.bnpparibas.com", "stephen.wotton@uk.bnpparibas.com")
}

private fun getDevelopersChatRoom() : String {
    return "Dev Chat"
}

private fun getProductionChatRoom() : String {
    return "Prod Chat"
}

private fun getRoomStream(symphony: SymphonyClient, chatRoomName: String = "JF Testing"): SymStream {
    val criteria = SymRoomSearchCriteria()
    criteria.member = symphony.localUser
    criteria.query = chatRoomName
    val roomSearch = symphony.streamsClient.roomSearch(criteria, null, null)

    val rd: SymRoomDetail = roomSearch.rooms[0]
    val stream = SymStream()
    stream.streamId = rd.roomSystemInfo.id
    return stream
}

private fun connectToSymphony(userEmail : String = "jason.field@uk.bnpparibas.com",  messageText : String = "Bot online. Global takeover imminent." ): SymphonyClient {
    val symphonyClientConfig = SymphonyClientConfig(true)

    val symphony = SymphonyClientFactory.getClient(SymphonyClientFactory.TYPE.V4, symphonyClientConfig)

    val stream = symphony.streamsClient.getStream(symphony.usersClient.getUserFromEmail(userEmail))

    if (messageText.isNotEmpty()) {
        val aMessage = SymMessage()
        aMessage.messageText = messageText
        symphony.messageService.sendMessage(stream, aMessage)
    }

    return symphony
}

private fun watchForSymphonyMessages(symphony: SymphonyClient) {
    val datafeed = symphony.dataFeedClient.createDatafeed(ApiVersion.V4)
    while (true) {
        val eventsFromDatafeed: MutableList<SymEvent>? = symphony.dataFeedClient.getEventsFromDatafeed(datafeed)
        eventsFromDatafeed?.forEach {
            if (it.type == "MESSAGESENT" && it.initiator != symphony.localUser) {
                val messageText = it.payload.messageSent.messageText.trim()
                println("### $messageText")

                if (messageText == "bot build") {
                    val jenkinsService = JenkinsService()
                    jenkinsService.build()
                }

                if (messageText == "bot deploy") {
                    val jenkinsService = JenkinsService()
                    jenkinsService.deploy()
                }

                if (messageText.startsWith("promote request", true)) {
                    sendRequestToProdTeam(messageText, it)
                }
            }
        }
        print(".")
    }
}

fun sendRequestToProdTeam(messageText: String, it: SymEvent) {
    val user = it.initiator.emailAddress
    val conn = connectToSymphony()
    val roomStream = getRoomStream(conn, getProductionChatRoom())

    val aMessage = SymMessage()
    aMessage.messageText = "$user: $messageText"
    conn.messageService.sendMessage(roomStream, aMessage)

}

private fun startWebServer(symphony: SymphonyClient, stream: SymStream) {
    val server = embeddedServer(Netty, port = 6677) {
        routing {
            get("/") {
                call.respondText("Hello World!", ContentType.Text.Plain)
            }

            post("/jenkins") {
                val rx = call.receiveText()
                println("Got a message from jenkins! [$rx]")
                call.respondText("OK")

                val mapper = jacksonObjectMapper()
                val jenkinsMessage = mapper.readValue<JenkinsStatusMessage>(rx)

                symphony.messageService.sendMessage(stream, message("Jenkins job ${jenkinsMessage.display_name} ${jenkinsMessage.build.phase}"))

                println("################ ${jenkinsMessage.build.phase} ${jenkinsMessage.build.status}")

                if (jenkinsMessage.build.phase == "COMPLETED" && jenkinsMessage.build.status == "FAILURE") {
                    val me = symphony.usersClient.getUserFromEmail("jason.field@uk.bnpparibas.com")
                    symphony.messageService.sendMessage(me, message("Build failed"))

                    val jenkinsService = JenkinsService()
                    val buildInfo = jenkinsService.getBuildInfo(jenkinsMessage.build.url)

                    println("********** $buildInfo")

                    val github = GitHubService()
                    val author = github.getAuthorFromChangeset(getRevision(buildInfo))

                    println("********** $author")

                    symphony.messageService.sendMessage(me, message("Build failed for author $author"))
                }

                val me = symphony.usersClient.getUserFromEmail("jason.field@uk.bnpparibas.com")
                symphony.messageService.sendMessage(me, message(rx))
            }
        }
    }
    server.start(wait = false)
}

private fun message(messageText: String): SymMessage {
    val aMessage = SymMessage()
    aMessage.messageText = messageText
    return aMessage
}