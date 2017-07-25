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

            // we don't want any leftovers to influence our execution (like previous logs)
            step([$class: 'WsCleanup'])

            checkout scm

            // start-of-unittests
            stage('gitClean') {
                gitClean()
            }

            sh "set > .envrc"

            stage("mkdtemp") {
               def x = mkdtemp('cd-')
               println "${x}"
            }

            stage("md5") {
               def jobname = "${env.JOB_NAME}#${env.BUILD_NUMBER}"
               def x = md5(jobname, 6)
               println "md5('${jobname}', 6) => ${x}"
            }

            stage("sh2") {
                withEnv(["MAX_LINES=2"]) {
                    // should display 1,2,4,5 (missing 3) and sh.log
                    def result = sh2 script: "seq 5; exit 200", returnStatus: true
                    print "[${result}]"
                    if (result != 200) currentBuild.result = 'FAILURE'

                    // should generate sh-1.log
                    sh2 "./bin/ansitest"

                    // should generate sh-2.log.gz
                    sh2 script: """#!/bin/bash
                        for i in \$(seq 100)
                        do
                          printf "\$i\n"
                          sleep 1
                        done""",
                        compress: true,
                        progressSeconds: 10

                    // this should not generate a log file or limit the output due
                    // to returnStdout: true
                    result = sh2 script: "seq 5", returnStdout: true
                    print "[${result}]"
                    // normalize output
                    result = result.replaceAll('\\r\\n?', '\n').trim()
                    if (result != '1\n2\n3\n4\n5') currentBuild.result = 'FAILURE'
                }
            }
            // end-of-unittests

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
