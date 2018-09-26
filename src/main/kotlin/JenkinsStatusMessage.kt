import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class JenkinsStatusMessage(
        val name: String,
        val display_name: String,
        val url: String,
        val build: Build
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Build(
        val full_url: String,
        val phase: String

)