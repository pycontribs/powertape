#!/usr/bin/env groovy
/* Logs jenkins console message at different levels: DEBUG, INFO, WARN,
ERROR which could appear colored when console supports ANSI coloring.
*/

// def ansi_message(msg){
//   return {
//     echo msg
//   }
// }

String call(String msg, String level = 'INFO') {

    def levels = [
      'DEBUG': '\u001B[34m', // blue
      'INFO': '\u001B[32m', // green
      'WARN': '\u001B[33m', // yellow
      'ERROR': '\u001B[31m', // red
      'UNSET': '\u001B[0m' // clear/defaults
    ]

    if ((env.TERM ?: '').toLowerCase().contains('xterm')) {
      ansiColor {
        echo "${levels[level]}${msg}${levels['UNSET']}"
      }
    }
    else {
        echo msg
    }
}
