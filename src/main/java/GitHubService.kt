import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.CommitService
import org.eclipse.egit.github.core.service.RepositoryService
import java.io.File


data class GitDetails(
        val user: String,
        val password: String
)

data class CommitDetails(val author: String, val link: String)

class GitHubService {
    fun getDetailsFromChangeset(commitHash: String): CommitDetails {
        val mapper = jacksonObjectMapper()
        val gitDetails = mapper.readValue<GitDetails>(File("git.json").readText())

        val client = GitHubClient()
        client.setCredentials(gitDetails.user, gitDetails.password)

        val service = RepositoryService()
        val repository = service.getRepository("jasonfield", "symphony_hackathon_test")

        val commitService = CommitService()
        val commit = commitService.getCommit(repository, commitHash)

        return CommitDetails(commit.author.login, commit.url)
    }

}
