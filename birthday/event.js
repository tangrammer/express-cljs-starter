'use strict'

const Promise = require('bluebird')
const mongojs = require('mongojs')
const serializeError = require('serialize-error')

Promise.promisifyAll([
  require('mongojs/lib/collection'),
  require('mongojs/lib/database'),
  require('mongojs/lib/cursor'),
])

const events = mongojs('rebujito').collection('events')

exports.log = function log(eventType, data) {

  return events.insertAsync({
    type: eventType,
    source: 'starbucks:birthdayJob',
    createdDate: new Date(),
    data,
  })

}

exports.error = function error(eventType, data, errorInfo) {

  return events.insertAsync({
    type: eventType,
    source: 'starbucks:birthdayJob',
    createdDate: new Date(),
    data,
    errorInfo: serializeError(errorInfo),
  })
}

exports.findByTypeAndUid = function findByTypeAndUid(eventType, uid) {

  return events.findAsync({
    type: eventType,
    'data.uid': uid,
  })
}
