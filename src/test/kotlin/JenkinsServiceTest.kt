import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JenkinsServiceTest {
    private val jenkinsService = JenkinsService()

    @Test
    fun `can get crumb`() {
        val crumb = jenkinsService.getCrumb()
        assertThat(crumb).isNotEmpty()
    }

    @Test
    fun `can trigger Jenkins builds`() {
        val result = jenkinsService.build()
        assertThat(result).isTrue()
    }

    @Test
    fun `can get build info`() {
        val result = jenkinsService.getBuildInfo("/job/Build%20my%20awesome%20project/19")
        assertThat(result).isTrue()
    }
}