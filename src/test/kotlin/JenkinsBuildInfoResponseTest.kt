import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JenkinsBuildInfoResponseTest {
    @Test
    fun `can get revision from build info`() {
        val msg = """{
  "_class": "org.jenkinsci.plugins.workflow.job.WorkflowRun",
  "actions": [
    {
      "_class": "hudson.model.CauseAction",
      "causes": [
        {
          "_class": "hudson.model.Cause\\$\\UserIdCause",
          "shortDescription": "Started by user Jason Field",
          "userId": "jason",
          "userName": "Jason Field"
        }
      ]
    },
    {
      "_class": "hudson.plugins.git.util.BuildData",
      "buildsByBranchName": {
        "refs/remotes/origin/master": {
          "_class": "hudson.plugins.git.util.Build",
          "buildNumber": 5,
          "buildResult": null,
          "marked": {
            "SHA1": "7db4d51be652f3e90e91612d5f06c4e6352295b6",
            "branch": [
              {
                "SHA1": "7db4d51be652f3e90e91612d5f06c4e6352295b6",
                "name": "refs/remotes/origin/master"
              }
            ]
          },
          "revision": {
            "SHA1": "7db4d51be652f3e90e91612d5f06c4e6352295b6",
            "branch": [
              {
                "SHA1": "7db4d51be652f3e90e91612d5f06c4e6352295b6",
                "name": "refs/remotes/origin/master"
              }
            ]
          }
        }
      },
      "lastBuiltRevision": {
        "SHA1": "7db4d51be652f3e90e91612d5f06c4e6352295b6",
        "branch": [
          {
            "SHA1": "7db4d51be652f3e90e91612d5f06c4e6352295b6",
            "name": "refs/remotes/origin/master"
          }
        ]
      },
      "remoteUrls": [
        "https://github.com/jasonfield/symphony_hackathon_test.git"
      ],
      "scmName": ""
    },
    {
      "_class": "hudson.plugins.git.GitTagAction"
    },
    {},
    {},
    {},
    {},
    {},
    {},
    {},
    {
      "_class": "org.jenkinsci.plugins.workflow.job.views.FlowGraphAction"
    },
    {},
    {}
  ],
  "artifacts": [],
  "building": false,
  "description": null,
  "displayName": "#5",
  "duration": 1281,
  "estimatedDuration": 445,
  "executor": null,
  "fullDisplayName": "test #5",
  "id": "5",
  "keepLog": false,
  "number": 5,
  "queueId": 52,
  "result": "SUCCESS",
  "timestamp": 1538043522038,
  "url": "http://ec2-18-130-79-165.eu-west-2.compute.amazonaws.com/job/test/5/",
  "changeSets": [],
  "culprits": [],
  "nextBuild": null,
  "previousBuild": {
    "number": 4,
    "url": "http://ec2-18-130-79-165.eu-west-2.compute.amazonaws.com/job/test/4/"
  }
}"""

        assertThat(getRevision(msg)).isEqualTo("7db4d51be652f3e90e91612d5f06c4e6352295b6")
    }
}