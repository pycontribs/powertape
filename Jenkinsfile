#!/usr/bin/env groovy

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
        pipelineTriggers([cron('H 01 * * *')])
])



/*
   Runs a command inside a virtual environment, creates the virtualenv if it does not exist.

   Keeping files inside the workspace is better than using system temp for portability, security and debugging.
*/

def venv(String environment = '.venv', String script) {
    /*
        PS=1 is a workaround for https://github.com/pypa/virtualenv/issues/1029
     */
    sh """#!/bin/bash
    set -ex

    if [ ! -d "${environment}" ]; then

        pip check >/dev/null || {
            # keep the force here, we had too many cases where pip reported upgraded version
            # but when you run it it will run an older version.
            pip install --force-reinstall -U pip
        }

        # using system gives great speed and disk usage improvements
        pip check && VIRTUALENV_SYSTEM_SITE_PACKAGES=1 || \
                echo "WARNING: Conflicts found on system python packages, for safety " \
                     "we will create and use an isolated virtualenv. Slower but safer."

        virtualenv "${environment}"
        PS1= source ${environment}/bin/activate
        # default version of pip installed in virtualenv is the ancient ~1.4 and mostly broken
        pip install -q -U pip
        # we also need a recent version of setuptools to avoid installation failures
        pip install -q -U setuptools
    else
        PS1= source ${environment}/bin/activate
    fi
    # assuring that we have no conflicts inside the virtualenv
    pip check
    """ + script
}

timestamps {

    ansiColor('xterm') {

        // some tools could fail if no TERM is defined
        env.TERM = env.TERM ?: 'xterm-color'
        // Inspired from http://unix.stackexchange.com/questions/148/colorizing-your-terminal-and-shell-environment
        env.ANSIBLE_FORCE_COLOR = env.ANSIBLE_FORCE_COLOR ?: 'true'
        env.CLICOLOR = env.CLICOLOR ?: '1'
        env.LSCOLORS = env.LSCOLORS ?: 'ExFxCxDxBxegedabagacad'

        node('master') {

            try {

                stage('prep') {

                    // https://support.cloudbees.com/hc/en-us/articles/226122247-How-to-Customize-Checkout-for-Pipeline-Multibranch
                    // clean needed to avoid potential leftovers from a reused workspace
                    checkout([
                            $class           : 'GitSCM',
                            branches         : scm.branches,
                            extensions       : scm.extensions + [[$class: 'CleanCheckout']],
                            userRemoteConfigs: scm.userRemoteConfigs
                    ])

                    git_branch = env.BRANCH_NAME ?: sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
                    if (git_branch.length() > 15 || git_branch.contains(' ')) {
                        // to avoid messing with jenkins UI or breaking other things in scripts
                        println "WARNING: Identified unsafe value for git branch name, falling back to UNKNOWN value: ${git_branch}"
                        git_branch = 'UNKNOWN'
                    }

                    git_commit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

                    /* First execution of setup.py can produce undesired stdout
                    (like pbr print) and we don't want this on next commands which
                    do read package name, version. */
                    sh 'python setup.py --help-commands >/dev/null'
                    package_name = sh(returnStdout: true, script: 'python setup.py -q --name').trim()
                    package_version = sh(returnStdout: true, script: 'python setup.py -q --version').trim()

                    currentBuild.displayName = "${package_name}-${package_version}@${git_branch}"

                    sh """set
                  which python
                  """

                    venv '''
                  pip install -U pip wheel setuptools
                  pip freeze | tee pip-freeze.log
                  pip check || "echo WARNING: pip checked returned $? error."
                  which python
                  '''
                }

                stage('lint') {
                }

                stage('unit') {
                    venv '''
                  pwd
                  ls -la
                  ./bin/sample.sh
                  pip install -e '.[test]'
                  tox
                  '''
                }

                stage('promotion') {

                    // see https://jenkins.io/doc/pipeline/steps/pipeline-input-step/
                    // https://support.cloudbees.com/hc/en-us/articles/204986450-Pipeline-How-to-manage-user-inputs
                    input message: "promote?",
                            id: "promote"
                }

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

                    step $class: 'ArtifactArchiver',
                            artifacts: '**/*.log',
                            fingerprint: true,
                            allowEmptyArchive: true
                    // defaultExcludes: true
                    // caseSensitive: false
                    // onlyIfSuccessful: false

                    step $class: 'JUnitResultArchiver',
                            testResults: '**/TEST-*.xml **/nosetests.xml, **/tempest*.xml **/junit.xml'

                } // end-clean
            } // finally

        } // node

        def do_clean = false
        timeout(time: 60, unit: 'MINUTES') {
            do_clean = input message:'clean?'
        }
        stage('clean')
        echo "Now I am releasing all those resources... ${do_clean}"

    } // end-ansiColor
} // end-timestamper
