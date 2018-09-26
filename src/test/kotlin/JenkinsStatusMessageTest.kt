import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JenkinsStatusMessageTest {
    @Test
    fun `can parse a Jenkins status message`() {
        val source = """{"name":"Build my awesome project","display_name":"Build my awesome project","url":"job/Build%20my%20awesome%20project/","build":{"full_url":"http://ec2-18-130-79-165.eu-west-2.compute.amazonaws.com/job/Build%20my%20awesome%20project/3/","number":3,"queue_id":5,"timestamp":1537966542240,"phase":"STARTED","url":"job/Build%20my%20awesome%20project/3/","scm":{"changes":[],"culprits":[]},"log":"","notes":"","artifacts":{}}}"""

        val mapper = jacksonObjectMapper()
        val message = mapper.readValue<JenkinsStatusMessage>(source)

        assertThat(message.display_name).isEqualTo("Build my awesome project")
        assertThat(message.build.phase).isEqualTo("STARTED")
    }
}