'use strict'

const fs = require('fs')
const Promise = require('bluebird')

Promise.promisifyAll(fs)

const promiseForContents = fs.readFileAsync('./email.html', 'utf-8')

exports.getContents = function getContents() {
  return promiseForContents
}
