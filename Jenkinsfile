#!/usr/bin/env groovy
/*
   Shared libraries cannot be tested with CR/PR, as Jenkins can only load
   a library from a defined branch.

   Due to this, we will merge to @master only from green branches.
*/
@Library('powertape') _

properties([
        // https://st-g.de/2016/12/parametrized-jenkins-pipelines
        parameters([
                booleanParam(
                  name: 'DEBUG',
                  defaultValue: false,
                  description: 'Enables debug mode which increase verbosity level.'),
                string(
                  name: 'GERRIT_REFSPEC',
                  defaultValue: '+refs/heads/master:refs/remotes/origin/master',
                  description: 'The REFSPEC of the component'),
                string(
                  name: 'GERRIT_BRANCH',
                  defaultValue: 'master',
                  description: 'The branch of the component')

        ]),

        // schedule job to run daily between 1am-2am
        pipelineTriggers([
                [$class: 'GitHubPushTrigger'],
                pollSCM('H/5 * * * *'),
                cron('H 01 * * *')
        ])
])


timestamps {

    // some tools could fail if no TERM is defined
    env.TERM = env.TERM ?: 'xterm-color'

    // Inspired from http://unix.stackexchange.com/questions/148/colorizing-your-terminal-and-shell-environment
    env.ANSIBLE_FORCE_COLOR = env.ANSIBLE_FORCE_COLOR ?: 'true'
    env.CLICOLOR = env.CLICOLOR ?: '1'
    env.LSCOLORS = env.LSCOLORS ?: 'ExFxCxDxBxegedabagacad'

    node('master') {

        try {

            sh "set > .envrc"
            // run all test stages for the pipeline shared library
            _testsuite

        } // end-try
        catch (error) {
            println error
            emailext attachLog: true,
                    body: "Build failed (see ${env.BUILD_URL}): ${error}",
                    subject: "[JENKINS] ${env.JOB_NAME} failed",
                    to: env.CHANGE_AUTHOR_EMAIL ?: 'ssbarnea+spam@redhat.com'
            // compressLog: false
            // recipientProviders
            // replyTo
            CulpritsRecipientProvider
            DevelopersRecipientProvider
            throw error
        }
        finally {
            stage('archive') {

                archiveArtifacts artifacts: '**/*.log,**/*.log.gz,.envrc',
                                 fingerprint: false,
                                 allowEmptyArchive: true
                // defaultExcludes: true
                // caseSensitive: false
                // onlyIfSuccessful: false

            } // end-clean
        } // finally

    } // node

} // timestamps
