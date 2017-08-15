#!/usr/bin/env groovy

/*
  sh2() is enhanced version of sh() which:
  - limits console output, showing only head+tail of up to env.MAX_LINES or 200
  - unabridged console output is saved in numbered files
  - basename: false - log filename when valid string else
    env.STAGE_NAME is used if defined. Fallback to 'sh'.
  - timestamps: false - add timestamps wrapper
  - ansiColor: true - enables ansicoloring wrapper
  - returnStdout : false - when true it will fallback to original sh().
    This is for avoiding the risk of breaking pipes that do process stdout.
  - compress: false - if true, it will create and archive .log.gz
    instead of .log files
  - progressSeconds: 30 - number of seconds to wait before adding a new
    progress line.

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
    if (! cmd.containsKey('script')) {
      error("Fatal: sh2 called without any script parameter.")
    }

    cmd['timestamps'] = cmd['timestamps'] ?: false
    cmd['ansiColor'] = cmd['ansiColor'] ?: true
    cmd['returnStdout'] = cmd['returnStdout'] ?: false
    cmd['compress'] = cmd['compress'] ?: false
    // number of seconds before adding a new progress line
    cmd['progressSeconds'] = cmd['progressSeconds'] ?: 30
    cmd['basename'] = cmd['basename'] ?: false

    def LOG_FILEPATH = false // relative to $WORKSPACE
    def LOG_FILENAME_SUFFIX = ".log"
    def STRIP_ANSI = "sed 's/\\x1b\\[[0-9;]*m//g'"
    def LOG_FOLDER = '.sh'
    def filters = [STRIP_ANSI]
    def verbosity = ''
    def result = null

    if (cmd['compress']) {
      filters.add("gzip -9 --stdout")
      LOG_FILENAME_SUFFIX = ".log.gz"
    }
    if (env.DEBUG && env.DEBUG != 'false') {
      verbosity = 'x' // for use in bash set -x
    }

    if (! cmd['returnStdout']) {

      if (cmd['basename']) {
          // when prefix is specifed we do not determine log filename
          LOG_FILEPATH = LOG_FOLDER + '/' + cmd['basename'] + LOG_FILENAME_SUFFIX
      } else {
          // getermine log filename, using auto-incrementing
          LOG_FILEPATH = sh returnStdout: true, script: """#!/bin/bash
          set -eo$verbosity pipefail

          mkdir -p \$WORKSPACE/$LOG_FOLDER >/dev/null
          function next_logfile() {
            n=
            set -C
            until
              file=\$1\${n:+-\$n}
              [[ ! \$(find \$WORKSPACE/$LOG_FOLDER/\$file* 2>/dev/null) ]]
            do
              ((n++))
            done
            printf $LOG_FOLDER/\$file$LOG_FILENAME_SUFFIX
          }

          # Sanitize filename https://stackoverflow.com/a/44811468/99834
          printf \$(next_logfile \$(echo "\${STAGE_NAME:-sh}" | \
                       awk -F '[^[:alnum:]]+' -v OFS=- \
                       '{\$0=tolower(\$0); \$1=\$1; gsub(/^-|-\$/, "")} 1'))
          """
          }
      LOG_FILENAME = LOG_FILEPATH.substring(
                     LOG_FILEPATH.lastIndexOf(File.separator)+1,
                                                  LOG_FILEPATH.length())
      // wrapper for limiting console output from commands
      cmd['script'] = """#!/bin/bash
      set -eo$verbosity pipefail
      # MacOS awk does not have systime() extension
      which gawk >/dev/null && AWK=gawk || AWK=awk
      mkdir -p \$WORKSPACE/$LOG_FOLDER >/dev/null

      ( ${ cmd['script'] } ) 2>&1 | \
          tee >(${ filters.join('|') } >> \$WORKSPACE/$LOG_FILEPATH) | \
          \$AWK -v offset=\${MAX_LINES:-200} \
          '{
               if (NR <= offset) print;
               else {
                   a[NR] = \$0;
                   delete a[NR-offset];
                   currTime = systime()
                   if ( (currTime - prevTime) > ${ cmd['progressSeconds'] } ) {
                       printf "INFO: %s lines redirected to $LOG_FILENAME ...\\n", NR | "cat>&2"
                       prevTime = currTime
                   }
                   }
           }
           END {
             for(i=NR-offset+1 > offset ? NR-offset+1: offset+1 ;i<=NR;i++)
             { print a[i]}
           }'
      """
     // we save the script to the log file, so we can know what commands
     // were executed.
     if (! cmd['compress']) {
         dir("$WORKSPACE") {
           writeFile file: LOG_FILEPATH,
                     text: cmd['script'] + '\n# CWD: ' + pwd() + '\n# OUTPUT:\n'
           } // TODO: implement the same for compressed logs
         }
    } // end of if !returnStdout

    if (verbosity) {
       echo "DEBUG: [sh2] params: ${cmd}"
    }

    def error = false
    try {
        if (cmd['timestamps']) {
            timestamps {
                if (cmd['ansiColor']) {
                  ansiColor('xterm') {
                      result = sh(cmd)
                  }
                }
                else {
                   result = sh(cmd)
                }
            }
        } else {
            if (cmd['ansiColor']) {
              ansiColor('xterm') {
                  result = sh(cmd)
              }
            }
            else {
               result = sh(cmd)
            }
        }
    } catch (e) {
      echo "ERROR: [sh2] ${e}"
      error = e
    } finally {
        if (! cmd['returnStdout'] && LOG_FILEPATH) {
            dir("$WORKSPACE") {
                // avoid collection failure when called from subdirs
                archiveArtifacts artifacts: "${LOG_FOLDER}/${LOG_FILENAME}" // tricks it into keeping the destination folder
                // when specific file is mentioned the target becomes root
            }
            echo "[sh2] \uD83D\uDD0E unabridged log at ${BUILD_URL}artifact/${LOG_FOLDER}/${LOG_FILENAME}"
        }
        if (error) {
          echo "ERROR: [sh2] finally: ${error}"
          throw error
        }
    }
    echo "DEBUG: [sh2] returning ${result}"
    return result
}

def call(Map cmd, String script) {
    cmd['script'] = script
    return sh2(cmd)
}

def call(String script) {
    return sh2('script': script)
}
