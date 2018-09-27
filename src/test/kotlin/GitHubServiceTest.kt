import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GitHubServiceTest {
    @Test
    fun `can get user from changeset`() {
        val github = GitHubService()
        val authorFromChangeset = github.getAuthorFromChangeset("7db4d51be652f3e90e91612d5f06c4e6352295b6")
        assertThat(authorFromChangeset).isEqualTo("jasonfield")
    }
}