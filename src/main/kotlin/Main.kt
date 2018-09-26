import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.apache.log4j.BasicConfigurator
import org.symphonyoss.client.SymphonyClient
import org.symphonyoss.client.SymphonyClientConfig
import org.symphonyoss.client.SymphonyClientFactory
import org.symphonyoss.client.events.SymEvent
import org.symphonyoss.symphony.clients.model.ApiVersion
import org.symphonyoss.symphony.clients.model.SymMessage


fun main(args: Array<String>) {
    BasicConfigurator.configure()

    val symphony = connectToSymphony()
    startWebServer(symphony)

    watchForSymphonyMessages(symphony)
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
                println(it.payload)
                val msg = SymMessage()
                msg.messageText = "You said -> ${it.payload.messageSent.messageText}"
                symphony.messageService.sendMessage(it.payload.messageSent.stream, msg)
            }
        }
        print(".")
    }
}

private fun startWebServer(symphony: SymphonyClient) {
    val server = embeddedServer(Netty, port = 6677) {
        routing {
            get("/") {
                call.respondText("Hello World!", ContentType.Text.Plain)
            }
            get("/jenkins") {
                println("Got a message from jenkins!")
                println(call.receiveText())
                call.respondText("HELLO WORLD!")

                val stream = symphony.streamsClient.getStream(symphony.usersClient.getUserFromEmail("jason.field@uk.bnpparibas.com"))

                val aMessage = SymMessage()
                aMessage.messageText = "Bot online. Global takeover imminent."

                symphony.messageService.sendMessage(stream, aMessage)
            }
        }
    }
    server.start(wait = false)
}