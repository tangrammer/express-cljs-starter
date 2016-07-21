'use strict'

const Promise = require('bluebird')
const mongojs = require('mongojs')
const moment = require('moment')
const micros = require('micros')
const sendGrid = require('sendgrid').SendGrid('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')
const event = require('./event.js')
const email = require('./email')

const users = mongojs('rebujito').collection('users')

micros.setBrand('starbucks')

const starbucks = require('micros/lib/starbucks')

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

  if (!birthdayGuy.cards) {
    console.log(birthdayGuy.emailAddress, 'no cards yet, not issuing coupon')
    return Promise.resolve()
  }

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
  if (!user.cards) return Promise.resolve() // try later guy
  console.log(user.emailAddress, 'issuing micros coupon')
  return starbucks.issueCoupon({account: user.cards[0].cardNumber, code: 'FBD001'})
}

function sendHappyBirthdayEmail(user) {
  console.log(user.emailAddress, 'sending a mail')

  return email.getContents()
  .then(emailText => {

    return new Promise((resolve) => {

      let request = sendGrid.emptyRequest()
      request.body = {
        personalizations: [{
          to: [{email: user.emailAddress}],
          bcc: [{email: 'marcin@swarmloyalty.co.za'}],
          subject: 'Happy Birhday!',
        }],
        from: {email: 'info@swarmloyalty.co.za'},
        content: [{ type: 'text/html', value: emailText }]
      }

      request.method = 'POST'
      request.path = '/v3/mail/send'
      sendGrid.API(request, (response) => {
        console.log(response.statusCode)
        console.log(response.body)
        console.log(response.headers)
        event.log(
          'birthday-coupon-email-sent',
          {
            uid: {id: user._id, brand: 'starbucks'},
            provider: 'sendgrid',
            response,
          }
        )
        resolve()
      })
    })
  })
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
