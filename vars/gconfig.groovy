#!/usr/bin/env groovy

/*

gconfig(var) can be used to read global configuration items, ones shared across
all our jobs and Jenkins masters.

As opposed to using the map directly by using the function we can alter the
value returned based on context.

For example we could make it return different values based on RHOS version
from the job, job type or anything else.

Also the function allows us to use default fallback values.

println(gconfig('hello'))
println(gconfig('foo','foo-value!'))
println(gconfig('bar')) // will print 'null'

*/


def call(key, fallback) {

  def gconfig_map = [
    'hello': 'world!',
  ]

  if (key in gconfig_map) {
    return gconfig_map[key]
  } else {
    return fallback
  }
}

def call(def key) {
    return gconfig(key, null)
}
