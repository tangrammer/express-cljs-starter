'use strict'

const Promise = require('bluebird')
const mongojs = require('mongojs')
const moment = require('moment')
const micros = require('micros')
const event = require('./event.js')

const users = mongojs('rebujito').collection('users')

function todaysBirthdays() {
  const today = moment()
  return users.findAsync({
    $or: [{
      birthDay: today.date(),
      birthMonth: today.month() + 1
    },
    {
      birthDay: String(today.date()),
      birthMonth: String(today.month() + 1)
    }]
  })
}

function birthdayIssueEvents(uid) {
  return event.findByTypeAndUid('birthday-coupon-issued', uid)
}

function issueBirthdayVoucher(birthdayGuy) {

  console.log(birthdayGuy.emailAddress, 'issuing coupon')

  return issueCouponInMicros(birthdayGuy)
  .then((coupon) => {
    console.log('issued coupon', coupon)
    return event.log(
      'birthday-coupon-issued',
      {
        uid: {id: birthdayGuy._id, brand: 'starbucks'},
        coupon
      })
  })
  .catch(error => {
    console.error('error issuing coupon', error)
    event.error(
      'issue-birthday-coupon-error',
      {uid: {id: birthdayGuy._id, brand: 'starbucks'}},
      error
    )
    throw error
  })
  .then(() => {
    return sendHappyBirthdayEmail(birthdayGuy)
  })
  .then(() => {
    return
  })
}

function issueCouponInMicros(user) {
  //TK
  console.log(user.emailAddress, 'todo, issue a coupon in micros')

  return Promise.delay(500)
}

function sendHappyBirthdayEmail(user) {
  //TK
  console.log(user.emailAddress, 'todo, send a mail')

  return Promise.delay(500)
}

function start() {

  console.log('birthday job starting')

  return todaysBirthdays()
  .map(birthdayGuy => {

    console.log(birthdayGuy.emailAddress, 'today\'s birthday guy')

    return birthdayIssueEvents({id: birthdayGuy._id, brand: 'starbucks'})
    .then(issuedEvents => {

      // TK: filter these events
      if (!issuedEvents || issuedEvents.length === 0) {
        return issueBirthdayVoucher(birthdayGuy)
      } else {
        console.log(birthdayGuy.emailAddress, 'already issued to')
      }
    })
    .catch(error => {
      console.error('error', birthdayGuy.emailAddress, error)
      event.error(
        'birthday-coupon-error',
        {uid: {id: birthdayGuy._id, brand: 'starbucks'}},
        error
      )
    })
  }, {concurrency: 1})
  .then(() => {
    console.log('job finished')
    process.exit()
  })
}

start()
