#!/usr/bin/env groovy

/*
  sh2() is enhanced version of sh() which:
  - limits console output, showing only head+tail of up to env.MAX_LINES or 200
  - unabridged console output is saved in numbered files
  - env.STAGE_NAME is used as for log filename base when defined, or it
    fallbacks to 'output'. This var will become soon available in Jenkins,
    the PR was already merged.
  - timestamps: false - auto add wrapper if not manually set to false
  - ansiColor: true - auto add wrapper if not manually set to false
  - returnStdout : false - when true it will fallback to original sh().
    This is for avoiding the risk of breaking pipes that do process stdout.

  Degrades gracedully: Unless you use the newly added parameters your
  pipeline code would work even when the new sh() is not loaded.

  TODO:
  - make it transparent by replacing original sh()

  This workaround bugs like:
  - https://issues.jenkins-ci.org/browse/JENKINS-37575
  - https://issues.jenkins-ci.org/browse/JENKINS-38381

  Working status of wrappers as of 2017-06-30:

                      ansiColor  timestamps
  console:            yes        yes
  stage-view-plugin:  yes        yes
  blue-ocean:         no         no
*/


def call(Map cmd) {
    /*
    - one progressbar dot is added for each line above the head limit
    - progresbar goes to stderr, not stdout

    */
    cmd['timestamps'] = cmd['timestamps'] ?: false
    cmd['ansiColor'] = cmd['ansiColor'] ?: true
    cmd['returnStdout'] = cmd['returnStdout'] ?: false
    def LOG_FILENAME = false
    if (! cmd['returnStdout']) {

      // get log filename
      LOG_FILENAME = sh returnStdout: true, script: '''#!/bin/bash
      set -eo pipefail

      function next_logfile() {
        n=
        set -C
        until
          file=$1${n:+-$n}.log.gz
          [[ ! -f "$file" ]]
        do
          ((n++))
        done
        printf "$file"

      }

      # Sanitize filename https://stackoverflow.com/a/44811468/99834
      LOG_PREFIX=$(echo "${STAGE_NAME:-output}" | \
                   awk -F '[^[:alnum:]]+' -v OFS=- \
                   '{$0=tolower($0); $1=$1; gsub(/^-|-$/, "")} 1')

      printf "$(next_logfile $LOG_PREFIX)"
      '''

      cmd['script'] = '''#!/bin/bash
      # wrapper for limiting stdout output from commands
      set -eo pipefail

      ( ''' + cmd['script'] + ''' ) 2>&1 | \
          tee >(gzip -9 --stdout >> ''' + LOG_FILENAME + ''') | \
          stdbuf -i0 -o0 -e0 awk -v offset=${MAX_LINES:-200} \
          '{
               if (NR <= offset) print;
               else {
                   a[NR] = $0;
                   delete a[NR-offset];
                   printf "." > "/dev/stderr"
                   }
           }
           END {
             print "" > "/dev/stderr";
             for(i=NR-offset+1 > offset ? NR-offset+1: offset+1 ;i<=NR;i++)
             { print a[i]}
           }'
      '''
    }

    def error = false
    try {
        if (cmd['timestamps']) {
            timestamps {
                if (cmd['ansiColor']) {
                  ansiColor('xterm') {
                      result = sh cmd
                  }
                }
                else {
                   result = sh cmd
                }
            }
        } else {
            if (cmd['ansiColor']) {
              ansiColor('xterm') {
                  result = sh cmd
              }
            }
            else {
               result = sh cmd
            }
        }
    } catch (e) {
      error = e
    } finally {
        if (LOG_FILENAME) {
            archiveArtifacts artifacts: LOG_FILENAME, allowEmpty: true
            echo "[sh2] \uD83D\uDD0E unabridged log at ${BUILD_URL}artifact/${LOG_FILENAME}"
        }
        if (error) throw error
    }
    return result
}

def call(String cmd) {
    sh2 'script': cmd
}
