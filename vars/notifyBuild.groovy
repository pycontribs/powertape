#!/usr/bin/env groovy
/* Generic notification function that aims to be reusable between most
pipelines.

Parameters (all optional):

subject: email subject

allowedDomains : list of domains to email to. If empty all are accepted.

msg: message to add to the body

template: which template to use, if not specified will use default one.

          groovy-html.template (default)
          groovy-text.template
          groovy-html-larry.template
          html-with-health-and-console.jelly
          html.jelly
          html_gmail.jelly
          static-analysis.jelly
          text.jelly

reference:
- https://www.cloudbees.com/blog/sending-notifications-pipeline
- https://wiki.jenkins.io/display/JENKINS/Email-ext+plugin
*/

def call(Map params = [:]) {

    def template2mime = [
      "groovy-html.template": 'text/html',
      "groovy-text.template": 'text/plain',
      "groovy-html-larry.template": 'text/html',
      "html-with-health-and-console.jelly": 'text/html',
      "html.jelly": 'text/html',
      "html_gmail.jelly": 'text/html',
      "static-analysis.jelly": 'text/html',
      "text.jelly": 'text/plain',
    ]
    // build status of null means successful
    def subject = "${currentBuild.result ?: 'SUCCESSFUL'}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    def summary = "${subject} (${env.BUILD_URL})"
    def template = params.template ?: 'groovy-html.template'
    def msg = params.msg ?: "\nCI notifies job stakeholders. It is possible that you may receive a notification on something that is not directly related to you. In this case, please just ignore it."
    def allowedDomains = params.allowedDomains ?: []

    // note that the expansion is implemented by the emailext itself
    def body = "\${SCRIPT, template=\"${template}\"}"
    def mimeType = template2mime[template]
    def dry = params.dry ?: false

    if (msg) {
      if (mimeType == 'text/plain') {
        body += "\n${msg}"
      } else { // assumes is html
        body.replaceAll("(?i)</body>", "${msg}</body>")
      }
    }

    def attachLog = false
    // Allways send a mail to the requestor (the one who started the job)
    def to = []
    to << emailextrecipients([[$class: 'RequesterRecipientProvider']])

    // Inform others when the build is *not* successfull
    if (currentBuild.result && currentBuild.result != 'SUCCESS') {
      attachLog = true
      to << emailextrecipients([
        [$class: 'CulpritsRecipientProvider'],
        [$class: 'DevelopersRecipientProvider'],
        [$class: 'FailingTestSuspectsRecipientProvider'],
        [$class: 'UpstreamComitterRecipientProvider']
        ])
    }

    // https://github.com/jenkinsci/ownership-plugin/blob/master/doc/PipelineIntegration.md
    if (ownership?.job?.ownershipEnabled) {
      to << ownership.job.primaryOwnerEmail
      to += ownership.job.secondaryOwnerEmails
    }
    // disabled to avoid affecting the build result
    // try {
    //   if (ownership?.node?.ownershipEnabled) {
    //     to << ownership.node.primaryOwnerEmail
    //     to += ownership.node.secondaryOwnerEmails
    //   }
    // }
    // catch (e) {
    //    log "[notifyBuild] ${e}", level: 'WARN'
    // }

    to.unique() // remove duplicates
    to.removeAll { !it } // remove null or false elements
    // if we have a whitelist of allowed domains we use it to sanitize the list
    if (allowedDomains) {
      match = /.*(${allowedDomains.join('|').replaceAll('\\.','\\.')})$$/
      to.removeAll { !(it ==~ match) }
    }
    to = to.join(',')

    if (env.DEBUG) {
      log "Sending emails to: ${to}", level: 'DEBUG'
    }
    // Send notifications
    // slackSend (color: colorCode, message: summary)

    // hipchatSend (color: color, notify: true, message: summary)

    if (!dry) {
      emailext (
          subject: subject,
          body: body,
          mimeType: mimeType,
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
    } else { // in dry mode we only same email body to files with log extension
        writeFile file: template + '.eml.log',
                  text: """To: ${to}
Subject: ${subject}


${body}"""
    }
  }

def call(def msg, Map params = [:]) {
  // msg can be a non String like an exception.
  params.msg = msg
  return notifyBuild(params)
}
