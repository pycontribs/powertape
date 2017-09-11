#!/usr/bin/env groovy
/* Generic notification function that aims to be reusable between most
pipelines.

reference:
- https://www.cloudbees.com/blog/sending-notifications-pipeline
- https://wiki.jenkins.io/display/JENKINS/Email-ext+plugin
*/
import static hudson.model.Result.FAILURE
import static hudson.model.Result.SUCCESS

def call() {

    // build status of null means successful
    def subject = "${currentBuild.result ?: 'SUCCESSFUL'}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    def summary = "${subject} (${env.BUILD_URL})"
    def details = '''${SCRIPT, template="groovy-html.template"}'''
    def attachLog = false
    // Allways send a mail to the requestor (the one who started the job)
    def to = []
    to << emailextrecipients([[$class: 'RequesterRecipientProvider']])

    // Inform others when the build is not successfull
    if (!currentBuild.result && currentBuild.result.isWorseThan(SUCCESS)) {
      attachLog = true
      to << emailextrecipients([
        [$class: 'CulpritsRecipientProvider'],
        [$class: 'DevelopersRecipientProvider'],
        [$class: 'FailingTestSuspectsRecipientProvider'],
        [$class: 'UpstreamComitterRecipientProvider']
        ])
    }
    to = to.join(',')

    // Send notifications
    // slackSend (color: colorCode, message: summary)

    // hipchatSend (color: color, notify: true, message: summary)

    emailext (
        subject: subject,
        body: details,
        mimeType: 'text/html',
        attachLog: attachLog,
        replyTo: '$DEFAULT_REPLYTO',
        to: to,
        // compressLog: false
        // recipientProviders: [
        //   [$class: 'DevelopersRecipientProvider'],
        //   //[$class: 'CulpritsRecipientProvider'],
        //   //[$class: 'RequesterRecipientProvider'],
        //   [$class: 'FailingTestSuspectsRecipientProvider'],
        //   [$class: 'UpstreamComitterRecipientProvider']
        //   ]
      )
  }
