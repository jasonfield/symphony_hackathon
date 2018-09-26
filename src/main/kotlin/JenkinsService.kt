import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result

class JenkinsService {
    private val jenkinsUrl = "http://ec2-18-130-79-165.eu-west-2.compute.amazonaws.com"

    fun build(): Boolean {
        val (_, _, result) = "$jenkinsUrl/job/Build%20my%20awesome%20project/build?delay=0sec"
                .httpPost()
                .authenticate("jason", "foobar")
                .header("Jenkins-Crumb" to getCrumb())
                .responseString()
        return when (result) {
            is Result.Failure -> {
                val (_, error) = result
                throw RuntimeException("Running Jenkins job failed - $error")
            }
            is Result.Success -> {
                true
            }
        }
    }

    fun getCrumb(): String {
        val (_, _, result) = "$jenkinsUrl/crumbIssuer/api/json"
                .httpGet()
                .authenticate("jason", "foobar")
                .responseString()
        return when (result) {
            is Result.Failure -> {
                val (_, error) = result
                throw RuntimeException("Getting Jenkins crumb failed - $error")
            }
            is Result.Success -> {
                val mapper = jacksonObjectMapper()
                val (data, _) = result
                val message = mapper.readValue<JenkinsCrumbResponse>(data!!)
                message.crumb
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class JenkinsCrumbResponse(val crumb: String)
