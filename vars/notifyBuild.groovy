#!/usr/bin/env groovy
/* Generic notification function that aims to be reusable between most
pipelines.

reference:
- https://www.cloudbees.com/blog/sending-notifications-pipeline
- https://wiki.jenkins.io/display/JENKINS/Email-ext+plugin
*/


def call(String buildStatus = 'STARTED', String message = '') {

  // build status of null means successful
    buildStatus =  buildStatus ?: 'SUCCESSFUL'

    // Default values
    def colorName = 'RED'
    def colorCode = '#FF0000'
    def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    def summary = "${subject} (${env.BUILD_URL})"
    def details = '''${SCRIPT, template="groovy-html.template"}'''
    // Override default values based on build status
    if (buildStatus == 'STARTED') {
      color = 'YELLOW'
      colorCode = '#FFFF00'
    } else if (buildStatus == 'SUCCESSFUL') {
      color = 'GREEN'
      colorCode = '#00FF00'
    } else {
      color = 'RED'
      colorCode = '#FF0000'
    }

    // Send notifications
    // slackSend (color: colorCode, message: summary)

    // hipchatSend (color: color, notify: true, message: summary)

    emailext (
        subject: subject,
        body: details,
        mimeType: 'text/html',
        attachLog: currentBuild.result != "SUCCESS",
        // compressLog: false
        // replyTo
        recipientProviders: [
          [$class: 'DevelopersRecipientProvider'],
          [$class: 'CulpritsRecipientProvider'],
          [$class: 'RequesterRecipientProvider'],
          [$class: 'FailingTestSuspectsRecipientProvider'],
          [$class: 'UpstreamComitterRecipientProvider']
          ]
      )

}
