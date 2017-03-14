#!/usr/bin/env groovy

/*
   Runs a command inside a virtual environment, creates the virtualenv if it does not exist.

   Keeping files inside the workspace is better than using system temp for portability, security and debugging.
*/
def venv(String environment='.venv', String script) {

    sh """#!/bin/bash
    set -ex
    [ ! -d "${environment}" ] && virtualenv --system-site-packages "${environment}"
    source ${environment}/bin/activate
    """ + script
}

timestamps {

  ansiColor('xterm') {

    // some tools could fail if no TERM is defined
    env.TERM =  env.TERM ?: 'xterm-color'
    // Inspired from http://unix.stackexchange.com/questions/148/colorizing-your-terminal-and-shell-environment
    env.ANSIBLE_FORCE_COLOR = env.ANSIBLE_FORCE_COLOR ?: 'true'
    env.CLICOLOR = env.CLICOLOR ?: '1'
    env.LSCOLORS = env.LSCOLORS ?: 'ExFxCxDxBxegedabagacad'

    node('master') {

        stage('prep') {

          // https://support.cloudbees.com/hc/en-us/articles/226122247-How-to-Customize-Checkout-for-Pipeline-Multibranch
          // clean needed to avoid potential leftovers from a reused workspace
          checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            extensions: scm.extensions + [[$class: 'CleanCheckout']],
            userRemoteConfigs: scm.userRemoteConfigs
            ])

          git_branch = env.BRANCH_NAME ?: sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
          git_commit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

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
          pip install -e .
          tox
          '''
        }

        stage('integration') {
        }

        stage('clean') {
        }

    }
  }
}
