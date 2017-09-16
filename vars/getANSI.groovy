/*

getANSI() is used to obtain ANSI color codes used for logging in various
other places.

It receives the color or log level and returns an ANSI sequence for it.
When called with null parameter it returns the clear ANSI sequence.

Please note that this function returns empty string if it does not
detect the presence of ANSI in the system, thing being a degrade gracefully
logic.

Reference:
* http://bitmote.com/index.php?post/2012/11/19/Using-ANSI-Color-Codes-to-Colorize-Your-Bash-Prompt-on-Linux

Visibility priority rules:

[pipeline] < DEBUG < NORMAL < INFO < WARN < ERROR

CSS colors that are safe to use for both white and black backgrounds.
Can be configured using JENKINS_URL/configure : ANSI Color : xterm

black    #000 #444
red      #c00 #f00
green    #080 #0c0
yellow   #da0 #fe0
blue     #29f #48b
magenta  #c0c #f0f
cyan     #0cc #0ff
white    #888 #fff

Please note that use of 3 char shorthands instead of 6 is by design as it does
lower the size of the console logs considerably

Also please note that you *MUST* specify the ansiColor('xterm') {} parameter
because when ommited the colors configured on Jenkins are ignored and you
would use the hardcoded values of the plugin (which are known to cause some
visibility problems).
*/


def call(String level) {
  def ANSI_MAP = [
    // [pipeline] messages are using #9A9999 so even our debug mode needs
    // to be bit more visible than them
    'DEBUG': '\u001B[1;37m', // gray, the least visible color
    /* 37 was reported to default to #E5E5E5 in ansiColor which is too bright
    for some monitors. You should reconfigure the xterm css values and change
    the value from #E5E5E5 to #CCC. This will affect only new jobs.
    30 is rendered as pure black, cannot use/override it.
    */
    // 36 cyan - not assigned yet but is less visible magenta
    'CMD': '\u001B[0;35m', // magenta, commands/script executed
    'INFO': '\u001B[0m', // "nothing" so we just clear, may change in future
    'PASS': '\u001B[1;32m', // bold green
    'WARN': '\u001B[1;33m', // bold yellow
    'FAIL': '\u001B[1;31m', // bold red
    'ERROR': '\u001B[1;31m', // bold red
    'FATAL': '\u001B[1;31;43m', // bold red on yellow
    'UNSET': '\u001B[0m' // clear/defaults
  ]

  if ((env.TERM ?: '').toLowerCase().contains('xterm')) {
      if (level == null) {
         return ANSI_MAP['UNSET']

      }
      if (level in ANSI_MAP) {
         return ANSI_MAP[level]
      }
  }
  return ""
}
