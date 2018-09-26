import org.apache.log4j.BasicConfigurator
import org.slf4j.LoggerFactory
import org.symphonyoss.client.SymphonyClientConfigID
import java.util.HashSet
import org.symphonyoss.symphony.clients.model.SymUser
import org.symphonyoss.client.model.Chat
import org.symphonyoss.symphony.clients.model.SymMessage
import org.symphonyoss.client.SymphonyClientFactory
import org.symphonyoss.client.SymphonyClientConfig
import org.symphonyoss.client.events.SymEvent
import org.symphonyoss.symphony.agent.model.V4Message
import org.symphonyoss.symphony.clients.model.ApiVersion
import org.symphonyoss.symphony.clients.model.SymStream
import org.symphonyoss.symphony.pod.model.UserDetail


fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("main")

    BasicConfigurator.configure()

    val symphonyClientConfig = SymphonyClientConfig(true)

    //Create an initialized client
    val symphony = SymphonyClientFactory.getClient(SymphonyClientFactory.TYPE.V4, symphonyClientConfig)

    val userFromEmail = symphony.usersClient.getUserFromEmail("jason.field@uk.bnpparibas.com")
    println(userFromEmail)
    val stream = symphony.streamsClient.getStream(userFromEmail)

    val aMessage = SymMessage()
    aMessage.messageText = "Bot online. Global takeover imminent."

    symphony.messageService.sendMessage(stream, aMessage)

    val datafeed = symphony.dataFeedClient.createDatafeed(ApiVersion.V4)
    while(true) {
        val eventsFromDatafeed: MutableList<SymEvent>? = symphony.dataFeedClient.getEventsFromDatafeed(datafeed)
        eventsFromDatafeed?.forEach {
            if(it.type == "MESSAGESENT" && it.initiator != symphony.localUser) {
                println(it.payload)
                val msg = SymMessage()
                msg.messageText = "You said -> ${it.payload.messageSent.messageText}"
                symphony.messageService.sendMessage(it.payload.messageSent.stream, msg)
            }
        }
        print(".")
    }
}