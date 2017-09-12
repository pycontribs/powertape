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
  - maxLines: 200 - maximum of lines to (head+tail) from output,
    this parameter overrides env.MAX_LINES and or default 200
  - filters: ['STRIP_ANSI'] - list of filters to apply to output stored in log file,
    List can contain names of builtin filters or commands (both as strings).
    There are two builtin filters which can be referenced by name:
    - STRIP_ANSI (removes ansi color markup from output)
    - TIMESTAMPS (prepends plaintext timestamp to every line in log file)
    Aside these two, any other command (chain) can be specified,
    and these can be mixed e.g.:
      filters: ['STRIP_ANSI', 'grep "[abc]" | sort -u', 'md5sum', 'TIMESTAMPS']
      would take uniq sorted lines containing latter a, b or c,
      but instead of this output prints it's md5sum and prepends timestamp to it
  - echoScript: true - if to print code/script which will be run,
    and url of the log to the console output first, before it's execution
  - workspaceStepNodeNum: -1 - number of node (in url) in your job/build's
    'Pipeline Steps' (flowGraphTable), which acquires the slave with node()
    and so the one which has access to workspace, if provided and >0,
    this enables generating urls to log files LIVE, in workspace,
    usable even while job is running.

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

    cmd['maxLines'] = cmd.get('maxLines', env.MAX_LINES ?: 200 )
    cmd['timestamps'] = cmd.get('timestamps', false)
    cmd['ansiColor'] = cmd.get('ansiColor', true)
    cmd['returnStdout'] = cmd.get('returnStdout', false)
    cmd['compress'] = cmd.get('compress', false)
    // number of seconds before adding a new progress line
    cmd['progressSeconds'] = cmd.get('progressSeconds', 30)
    cmd['basename'] = cmd.get('basename', false)
    cmd['filters'] = cmd.get('filters', ['STRIP_ANSI'])
    cmd['echoScript'] = cmd.get('echoScript', true)
    cmd['workspaceStepNodeNum'] = cmd.get('workspaceStepNodeNum', 0)

    def INFO_COLOR=''
    def DEBUG_COLOR=''
    def ERROR_COLOR=''
    def NO_COLOR=''
    // test if we can use ANSI, ansiColor can be false if wrapper is already on
    if (cmd['ansiColor'] || (env.TERM ?: '').toLowerCase().contains('xterm')) {
        INFO_COLOR="\u001B[32m" // green
        DEBUG_COLOR="\u001B[33m" // yellow
        ERROR_COLOR="\u001B[31m" // red
        NO_COLOR="\u001B[0m"
    }

    def LOG_FILEPATH = false // relative to $WORKSPACE
    def LOG_FILENAME_SUFFIX = ".log"
    def LOG_FOLDER = '.sh'
    def BUILTIN_FILTERS = [
        'STRIP_ANSI': "sed 's/\\x1b\\[[0-9;]*m//g'",
        'TIMESTAMPS': """awk '{
            cmd = \"date +\\\"%Y-%m-%d %H:%M:%S.%3N | \\\""
            cmd | getline now
            close(cmd)
            sub(/^/, now)
            print
            fflush()
            }' """
    ]
    def filters = []
    def verbosity = ''
    def result = null

    def boundary_marker = ('\u2500' * 60)

    for (int filter_idx=0; filter_idx < cmd['filters'].size(); filter_idx++) {
        def filter = cmd['filters'][filter_idx]
        // use filter as key for builtin, if not then as default value for get()
        // allows usage of filters as: ['STRIP_ANSI', 'rev | base64']
        filters.add( BUILTIN_FILTERS.get(filter, filter) )
    }

    if (cmd['compress']) {
      // compression to be last filter, so that others can modify the text
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
      def user_script = cmd['script'] // store for writing it to logfile below
      cmd['script'] = """#!/bin/bash
      set -eo$verbosity pipefail
      # MacOS awk does not have systime() extension
      which gawk >/dev/null && AWK=gawk || AWK=awk
      mkdir -p \$WORKSPACE/$LOG_FOLDER >/dev/null

      ( ${ cmd['script'] } ) 2>&1 | \
          tee >(${ filters.join('|') } >> \$WORKSPACE/$LOG_FILEPATH) | \
          \$AWK -v offset=${cmd['maxLines']} \
          '{
               if (NR <= offset) print;
               else {
                   a[NR] = \$0;
                   delete a[NR-offset];
                   currTime = systime()
                   if ( (currTime - prevTime) > ${ cmd['progressSeconds'] } ) {
                       printf "${INFO_COLOR}INFO: %s lines redirected to $LOG_FILENAME ...\\n${NO_COLOR}", NR | "cat>&2"
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
                      text: cmd['script'] + '\n# CWD: ' + pwd() + '\n# SCRIPT: ' + user_script.stripIndent() + '\n# OUTPUT:\n'
          } // TODO: implement the same for compressed logs
      }
      def header_text = ''
      if (cmd['echoScript']) {
          header_text += "[sh2] \u256D${boundary_marker}\n${user_script.stripIndent()}\n"
          header_text += "[sh2] \uD83D\uDD0E unabridged log at ${BUILD_URL}artifact/${LOG_FOLDER}/${LOG_FILENAME}\n"
      }
      if (cmd['workspaceStepNodeNum'] > 0) {
          header_text += "[sh2] \uD83D\uDC41 live log at ${BUILD_URL}execution/node/${cmd['workspaceStepNodeNum']}/ws/${LOG_FOLDER}/${LOG_FILENAME}\n"
      }
      if ( header_text ) { log header_text }

    } // end of if !returnStdout

    if (verbosity) {
       log "[sh2] params: ${cmd}", level: 'DEBUG'
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
      log "[sh2] ${e}", level: 'ERROR'
      error = e
    } finally {
        if (! cmd['returnStdout'] && LOG_FILEPATH) {
            dir("$WORKSPACE") {
                // avoid collection failure when called from subdirs
                archiveArtifacts artifacts: "${LOG_FOLDER}/${LOG_FILENAME}" // tricks it into keeping the destination folder
                // when specific file is mentioned the target becomes root
            }
            if (! cmd['echoScript']) { // echoScript prints url before execution
                log "[sh2] \uD83D\uDD0E unabridged log at ${BUILD_URL}artifact/${LOG_FOLDER}/${LOG_FILENAME}"
            } else {
                log "[sh2] \u2570${boundary_marker}"
            }
            // TODO(psedlak): hide xtrace of this grep later, when proven working in all jobs
            // for now keep it verbose, later prepend bash shebang to run without xtrace
            sh("grep -s '[B]uild mark:' \"\$WORKSPACE/${LOG_FOLDER}/${LOG_FILENAME}\" || true")
        }
        if (error) {
          log "[sh2] finally: ${error}", level: 'ERROR'
          throw error
        }
    }
    log "[sh2] returning ${result}", level: 'DEBUG'
    return result
}

def call(Map cmd, String script) {
    cmd['script'] = script
    return sh2(cmd)
}

def call(String script) {
    return sh2('script': script)
}
