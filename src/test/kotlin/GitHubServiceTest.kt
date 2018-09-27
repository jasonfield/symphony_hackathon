import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GitHubServiceTest {
    @Test
    fun `can get user from changeset`() {
        val github = GitHubService()
        val author = github.getDetailsFromChangeset("7db4d51be652f3e90e91612d5f06c4e6352295b6")
        assertThat(author).isEqualTo("jasonfield")
    }
}