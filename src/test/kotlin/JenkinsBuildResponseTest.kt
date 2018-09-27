import org.junit.Test

class JenkinsBuildResponseTest {
    @Test
    fun `can parse build message`() {
        val msg = """
            {"_class":"org.jenkinsci.plugins.workflow.job.WorkflowRun","actions":[{"_class":"hudson.model.CauseAction","causes":[{"_class":"hudson.model.Cause$UserIdCause","shortDescription":"Started by user Jason Field","userId":"jason","userName":"Jason Field"}]},{},{},{},{},{},{"_class":"org.jenkinsci.plugins.workflow.job.views.FlowGraphAction"},{},{}],"artifacts":[],"building":false,"description":null,"displayName":"#19","duration":26,"estimatedDuration":5343,"executor":null,"fullDisplayName":"Build my awesome project #19","id":"19","keepLog":false,"number":19,"queueId":45,"result":"FAILURE","timestamp":1538040826082,"url":"http://ec2-18-130-79-165.eu-west-2.compute.amazonaws.com/job/Build%20my%20awesome%20project/19/","changeSets":[],"culprits":[],"nextBuild":null,"previousBuild":{"number":18,"url":"http://ec2-18-130-79-165.eu-west-2.compute.amazonaws.com/job/Build%20my%20awesome%20project/18/"}}
        """.trimIndent()
    }
}