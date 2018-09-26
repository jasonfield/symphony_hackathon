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

private fun getRoomStream(symphony: SymphonyClient): SymStream {
    val criteria = SymRoomSearchCriteria()
    criteria.member = symphony.localUser
    criteria.query = "JF Testing"
    val roomSearch = symphony.streamsClient.roomSearch(criteria, null, null)

    val rd: SymRoomDetail = roomSearch.rooms[0]
    val stream = SymStream()
    stream.streamId = rd.roomSystemInfo.id
    return stream
}

private fun connectToSymphony(): SymphonyClient {
    val symphonyClientConfig = SymphonyClientConfig(true)

    val symphony = SymphonyClientFactory.getClient(SymphonyClientFactory.TYPE.V4, symphonyClientConfig)

    val stream = symphony.streamsClient.getStream(symphony.usersClient.getUserFromEmail("jason.field@uk.bnpparibas.com"))

    val aMessage = SymMessage()
    aMessage.messageText = "Bot online. Global takeover imminent."

    symphony.messageService.sendMessage(stream, aMessage)

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
            }
        }
        print(".")
    }
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
                val message = mapper.readValue<JenkinsStatusMessage>(rx)

                val aMessage = SymMessage()
                aMessage.messageText = "Jenkins job ${message.display_name} ${message.build.phase}"

                symphony.messageService.sendMessage(stream, aMessage)
            }
        }
    }
    server.start(wait = false)
}