import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GitHubServiceTest {
    @Test
    fun `can get user from changeset`() {
        val github = GitHubService()
        val details = github.getDetailsFromChangeset("7db4d51be652f3e90e91612d5f06c4e6352295b6")
        assertThat(details.author).isEqualTo("jasonfield")
        assertThat(details.link).isEqualTo("https://api.github.com/repos/jasonfield/symphony_hackathon_test/commits/7db4d51be652f3e90e91612d5f06c4e6352295b6")
    }
}