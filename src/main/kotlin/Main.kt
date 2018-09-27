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
import org.symphonyoss.client.model.Room
import org.symphonyoss.symphony.clients.model.*
import java.util.*


fun main(args: Array<String>) {
    BasicConfigurator.configure()

    val symphony = connectToSymphony()

    val stream = getRoomStream(symphony)
    symphony.messageService.sendMessage(stream, message("oh hai room"))

    startWebServer(symphony, stream)

    watchForSymphonyMessages(symphony)
}

var requestDisplayName: String = ""
var requestersEmail: String = ""
var requestMessage: String = ""
var requestPromotionReceived: Boolean = false
var chatRoomName: String = ""

private fun getProductionUsers(): Array<String> {
    return arrayOf("wells.powell@bnpparibas.com", "jackie.wong@uk.bnpparibas.com")
}

private fun getDevelopmentUsers(): Array<String> {
    return arrayOf("jason.field@uk.bnpparibas.com", "stephen.wotton@uk.bnpparibas.com")
}

private fun getDevelopersChatRoom(): String {
    return "Dev Chat"
}

private fun getProductionChatRoom(): String {
    return "Prod Chat"
}

private fun getRoomStream(symphony: SymphonyClient, chatRoomName: String = "Dev Chat"): SymStream {
    val criteria = SymRoomSearchCriteria()
    criteria.member = symphony.localUser
    criteria.query = chatRoomName
    val roomSearch = symphony.streamsClient.roomSearch(criteria, null, null)

    val rd: SymRoomDetail = roomSearch.rooms[0]
    val stream = SymStream()
    stream.streamId = rd.roomSystemInfo.id
    return stream
}

private fun createChatRoom(symphony: SymphonyClient, chatRoomName: String, usersToAdd: ArrayList<String>): Room {
    val att = SymRoomAttributes()
    att.discoverable = true
    att.name = chatRoomName
    att.membersCanInvite = true
    att.description = chatRoomName
    att.creatorUser = symphony.localUser
    att.public = false
    val room = symphony.roomService.createRoom(att)

    for (email in usersToAdd) {
        var userId: Long = getUserId(symphony, email)
        symphony.roomMembershipClient.addMemberToRoom(room.streamId, userId)
    }

    return room
}

fun getUserId(symphony: SymphonyClient, email: String): Long {
    var user = symphony.usersClient.getUserFromEmail(email)
    return user.id
}


private fun connectToSymphony(userEmail: String = "stephen.wotton@uk.bnpparibas.com", messageText: String = "Bot online. Global takeover imminent."): SymphonyClient {
    val symphonyClientConfig = SymphonyClientConfig(true)

    val symphony = SymphonyClientFactory.getClient(SymphonyClientFactory.TYPE.V4, symphonyClientConfig)

    val stream = symphony.streamsClient.getStream(symphony.usersClient.getUserFromEmail(userEmail))

    if (messageText.isNotEmpty()) {
        symphony.messageService.sendMessage(stream, message(messageText))
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

                if (messageText.contains("bot build")) {
                    val jenkinsService = JenkinsService()
                    jenkinsService.build()
                }

                if (messageText.contains("bot deploy")) {
                    val jenkinsService = JenkinsService()
                    jenkinsService.deploy()
                }

                if (messageText.contains("promote request", true)) {
                    sendRequestToProdTeam(symphony, messageText, it)
                }

                if (messageText.contains("accept request", true)) {
                    prodTeamAcceptsRequest(symphony, it)
                }

                if (messageText.contains("release approved", true)) {
                    val a = it.initiator.displayName
                    val m = "Deploying request, approved by $a"
                    changeRequestRoomMessage(symphony, m, it, true)
                    var u = it.initiator.displayName
                    var x = "$u APPROVED request: [$requestMessage]"
                    sendMessage(symphony, x, getProductionChatRoom(), it)
                    sendMessage(symphony, x, getDevelopersChatRoom(), it)
                }

                if (messageText.contains("accept rejected", true)) {
                    val a = it.initiator.displayName
                    val m = "Request rejected by $a"
                    changeRequestRoomMessage(symphony, m, it)
                    var u = it.initiator.displayName
                    var x = "$u REJECTED request: [$requestMessage]"
                    sendMessage(symphony, x, getProductionChatRoom(), it)
                    sendMessage(symphony, x, getDevelopersChatRoom(), it)
                }

            }
        }
        print(".")
    }
}

fun changeRequestRoomMessage(symphony: SymphonyClient, messageText: String, it: SymEvent, doJenkins: Boolean = false) {
    val user = it.initiator.displayName
    val userEmail = it.initiator.emailAddress
    val roomStream = getRoomStream(symphony, chatRoomName)
    val aMessage = SymMessage()
    val userIsProductionUser = getProductionUsers().any { it.equals(userEmail) }

    if (!userIsProductionUser) {
        aMessage.messageText = "$user you are not permitted to approve requests."
    } else {
        aMessage.messageText = messageText
        //do jenkins magic here and message
        if (doJenkins) {
            JenkinsService().deploy()
        }
    }

    symphony.messageService.sendMessage(roomStream, aMessage)
}

fun prodTeamAcceptsRequest(symphony: SymphonyClient, it: SymEvent) {
    val user = it.initiator.displayName
    val userEmail = it.initiator.emailAddress
    val roomStream = getRoomStream(symphony, getProductionChatRoom())
    val aMessage = SymMessage()
    val userIsProductionUser = getProductionUsers().any { it.equals(userEmail) }

    if (!requestPromotionReceived) {

        aMessage.messageText = "$user there are no requests to accept!"
    } else if (!userIsProductionUser) {
        aMessage.messageText = "$user you are not permitted to accept requests."
    } else {
        var usersToAdd = arrayListOf<String>(userEmail, requestersEmail)
        chatRoomName = "Promote Request Discussion" + UUID.randomUUID().toString().substring(0, 8)
        val newRoom = createChatRoom(symphony, chatRoomName, usersToAdd)
        symphony.messageService.sendMessage(newRoom, message("This is a room to discuss release promotion <a href='https://somthing.com'>serviceNow</a>"))

        aMessage.messageText = "$user has accepted request $requestMessage a chatroom $chatRoomName has been created."
    }

    symphony.messageService.sendMessage(roomStream, aMessage)
    changeRequestRoomMessage(symphony, requestMessage, it)
}


fun sendMessage(symphony: SymphonyClient, messageText: String, roomName: String, it: SymEvent) {
    val user = it.initiator.displayName
    val roomStream = getRoomStream(symphony, roomName)

    val aMessage = SymMessage()
    aMessage.messageText = messageText
    symphony.messageService.sendMessage(roomStream, aMessage)
}


fun sendRequestToProdTeam(symphony: SymphonyClient, messageText: String, it: SymEvent) {
    val user = it.initiator.displayName
    val roomStream = getRoomStream(symphony, getProductionChatRoom())

    val content = "$user wishes to: $messageText"
    symphony.messageService.sendMessage(roomStream, message("$content <a href='https://something.com'>serviceNow</a>"))

    requestDisplayName = it.initiator.displayName
    requestMessage = content
    requestersEmail = it.initiator.emailAddress
    requestPromotionReceived = true
}

private fun startWebServer(symphony: SymphonyClient, stream: SymStream) {

    val authorMap = mapOf(
            "jasonfield" to "jason.field@uk.bnpparibas.com",
            "WottonParibasTest" to "stephen.wotton@uk.bnpparibas.com"
    )

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

                if (jenkinsMessage.build.phase == "COMPLETED" && jenkinsMessage.build.status == "FAILURE") {
                    val me = symphony.usersClient.getUserFromEmail("jason.field@uk.bnpparibas.com")
                    symphony.messageService.sendMessage(me, message("Build failed"))

                    val jenkinsService = JenkinsService()
                    val buildInfo = jenkinsService.getBuildInfo(jenkinsMessage.build.url)

                    val github = GitHubService()
                    val details = github.getDetailsFromChangeset(getRevision(buildInfo))

                    val target = symphony.usersClient.getUserFromEmail(authorMap.get(details.author))
                    symphony.messageService.sendMessage(target, message("Hi it looks like your commit broke the build - <a href='${jenkinsMessage.build.full_url}'>jenkins</a> | <a href='${details.link}'>github</a>"))
                }

                val me = symphony.usersClient.getUserFromEmail("jason.field@uk.bnpparibas.com")
                symphony.messageService.sendMessage(me, message(rx))
            }
        }
    }
    server.start(wait = false)
}

private fun message(messageText: String): SymMessage {
    val symMsg = SymMessage()
    symMsg.message = "<messageML>$messageText</messageML>"
    return symMsg
}