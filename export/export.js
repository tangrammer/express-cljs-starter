'use strict'

const Promise = require('bluebird')
const sql = require('mssql')
const micros = require('micros')
const moment = require('moment')
const _ = require('lodash')
const mongojs = require('mongojs')

Promise.promisifyAll([
  require('mongojs/lib/collection'),
  require('mongojs/lib/database'),
  require('mongojs/lib/cursor'),
])

micros.setBrand('starbucks')

const
  database = 'SCV_HUB',
  username = 'SCV_SWARM',
  password = '$35L$3a9w',
  sqlServerUrl = `mssql://${username}:${password}@localhost:1433/${database}`,
  mongoUrl = 'localhost:1234/rebujito'

const users = mongojs(mongoUrl).collection('users')

const cols = [
  'firstname',
  'lastname',
  'primaryposref',
  'emailaddress',
  'city',
  'state',
  'signupdate',
]

sql.connect(sqlServerUrl)
.then(getAccounts)
.then(accounts => {

  let accountNumbers = _(accounts)
                         .map((acc) => acc.primaryposref)
                         .filter((acc) => !!acc)
                         .uniq()
                         .valueOf()

  console.log(`got ${accountNumbers.length} accounts`)

  let idx = 0
  return Promise.map(accountNumbers, (accountNumber) => {
    idx++
    console.log(`starting ${accountNumber} ${idx}/${accountNumbers.length}`)
    return exportCustomer(accountNumber)
    .catch(e => {
      console.error(`culprit: ${accountNumber}`)
      throw e
    })
  }, {concurrency: 20})
})
.then(() => {
  console.log('export finished')
  process.exit()
})
.catch(err => console.error(err.stack))

function exportCustomer(accountNumber) {
  return Promise.all([
    micros.getCustomerDetails(accountNumber, cols),
    micros.transactions({accountNumber}),
    micros.getStarbucksBalances(accountNumber),
  ])
  .then(([customerArr, txs, balances]) => {
    if (!customerArr || !customerArr[0]) return Promise.resolve()

    let customer = customerArr[0]
    return exportCustomerProfile(customer)
    .then(() => {
      return exportTransactions(customer, txs)
    })
    .then(() => deleteBalances(customer))
    .then(() => exportBalances(customer, balances))
  })
}

function getAccounts() {
  console.log('fetching accounts')
  return micros.get('Customer', {condition: '', values: [], resultCols: ['primaryposref']})
}

const microsToMongoId = id => `${_.repeat('0', 24)}${Number(id).toString(16)}`.slice(-24)

function exportCustomerProfile(custInMicros) {

  console.log(`exporting ${custInMicros.primaryposref} [${custInMicros.id}]`)

  return sql.query `delete from customers where id = ${custInMicros.id}`
  .then(() => users.findOneAsync({_id: mongojs.ObjectId(microsToMongoId(custInMicros.id))}))
  .then((custInMongo) => exportAddresses(custInMicros, custInMongo))
  .then((custInMongo) => {

    const ps = new sql.PreparedStatement()

    return new Promise((resolve, reject) => {

      ps.input('id', sql.Int)
      ps.input('primaryposref', sql.VarChar(128))
      ps.input('first_name', sql.VarChar(128))
      ps.input('last_name', sql.VarChar(128))
      ps.input('email', sql.VarChar(128))
      ps.input('city', sql.VarChar(128))
      ps.input('province', sql.VarChar(128))
      ps.input('created', sql.DateTime())
      ps.input('birth_month', sql.Int)
      ps.input('birth_day', sql.Int)
      ps.input('receive_starbucks_email_communications', sql.Bit)
      ps.input('mobile', sql.VarChar(128))

      ps.prepare(`insert into customers(id,
                                        primaryposref,
                                        first_name,
                                        last_name,
                                        email,
                                        created,
                                        birth_month,
                                        birth_day,
                                        receive_starbucks_email_communications,
                                        mobile
                                      )
                              values(@id,
                                     @primaryposref,
                                     @first_name,
                                     @last_name,
                                     @email,
                                     @created,
                                     @birth_month,
                                     @birth_day,
                                     @receive_starbucks_email_communications,
                                     @mobile
                                   )`, err => {

        if (err) throw err

        ps.execute({
          id: custInMicros.id,
          primaryposref: custInMicros.primaryposref,
          'first_name': custInMicros.firstname,
          'last_name': custInMicros.lastname,
          email: custInMicros.emailaddress,
          created: moment(custInMicros.signupdate, 'YYYY-MM-DD hh:mm:ss.S').toDate(),
          'birth_month': custInMongo ? custInMongo.birthMonth : null,
          'birth_day': custInMongo ? custInMongo.birthDay : null,
          'receive_starbucks_email_communications': custInMongo ? custInMongo.receiveStarbucksEmailCommunications : null,
          'mobile': custInMongo ? custInMongo.mobileNumber : null,
        }, (err, recordsets, affected) => {

          if (err) return reject(err)

          console.log(`inserted customer ${custInMicros.primaryposref}`)

          ps.unprepare(err => {
            resolve()
          })
        })
      })
    })
  })
}

function exportAddresses(custInMicros, custInMongo) {

  if (!custInMongo) return

  const {addresses} = custInMongo
  if (!addresses) return
  console.log(`${custInMicros.primaryposref} exoprting ${addresses.length} address(es)`)

  return sql.query `delete from addresses where customer_id = ${custInMicros.id}`
  .then(() => {
    const table = new sql.Table('addresses')
    table.create = true

    table.columns.add('id', sql.VarChar(128), {nullable: false})
    table.columns.add('customer_id', sql.Int, {nullable: false})
    table.columns.add('type', sql.VarChar(128), {nullable: false})
    table.columns.add('first_name', sql.VarChar(128), {nullable: false})
    table.columns.add('last_name', sql.VarChar(128), {nullable: false})
    table.columns.add('phone_number', sql.VarChar(128), {nullable: true})
    table.columns.add('address_line_1', sql.VarChar(128), {nullable: false})
    table.columns.add('address_line_2', sql.VarChar(128), {nullable: true})
    table.columns.add('city', sql.VarChar(128), {nullable: false})
    table.columns.add('postal_code', sql.VarChar(128), {nullable: false})
    table.columns.add('country', sql.VarChar(128), {nullable: false})

    addresses.forEach(address => table.rows.add(
                                  address.addressId,
                                  custInMicros.id,
                                  address.type,
                                  address.firstName,
                                  address.lastName,
                                  address.phoneNumber,
                                  address.addressLine1,
                                  address.addressLine2,
                                  address.city,
                                  address.postalCode,
                                  address.country))

    const request = new sql.Request()
    return request.bulk(table)
  })
  .then(() => custInMongo)
}

function exportTransactions(customer, txs) {

  console.log(`exporting transactions for ${customer.primaryposref}`)

  return removeTransactions(customer)
  .then(() => {
    const table = new sql.Table('transactions')
    table.create = true

    table.columns.add('id', sql.Int, {nullable: false})
    table.columns.add('customer_id', sql.Int, {nullable: false})
    table.columns.add('check_number', sql.Int, {nullable: false})
    table.columns.add('amount', sql.Decimal(19, 2), {nullable: false})
    table.columns.add('balance', sql.Decimal(19, 2), {nullable: false})
    table.columns.add('description', sql.VarChar(128), {nullable: false})
    table.columns.add('date', sql.DateTime(), {nullable: true})
    table.columns.add('location_id', sql.Int, {nullable: true})
    table.columns.add('program_id', sql.Int, {nullable: true})
    table.columns.add('category_id', sql.Int, {nullable: true})
    table.columns.add('points', sql.Decimal(19, 2), {nullable: true})
    table.columns.add('bonus_points', sql.Decimal(19, 2), {nullable: true})
    table.columns.add('bonus_amount', sql.Decimal(19, 2), {nullable: true})

    const request = new sql.Request()

    return Promise.each(txs, (tx) => {
      table.rows.add(
        tx.id,
        customer.id,
        tx.check,
        tx.amount,
        tx.balance,
        tx.description,
        tx.date,
        tx.location,
        tx.program,
        tx.category,
        tx.points,
        tx.bonusPoints,
        tx.bonusAmount
      )
      return exportCheckItems(tx, tx.items)
    })
    .then(() => request.bulk(table))
    .then((rowCount) => {
      console.log(`done exporting ${rowCount} transactions`)
    })
  })
}

function deleteBalances(customer) {
  return sql.query `delete from balances where customer_id = ${customer.id}`
}

function exportBalances(customer, balances) {
  const table = new sql.Table('balances')
  table.create = true

  table.columns.add('customer_id', sql.Int, {nullable: false})
  table.columns.add('program_id', sql.Int, {nullable: false})
  table.columns.add('balance', sql.Decimal(19, 2), {nullable: false})
  table.columns.add('tier', sql.VarChar(128), {nullable:true})

  balances.programs.forEach((program) => {
    const tierName = program.programTierId ? balances.tier.name : null
    table.rows.add(customer.id, program.id, program.balance, tierName)
  })

  var request = new sql.Request()
  return request.bulk(table)
}

function exportCheckItems(transaction) {
  const table = new sql.Table('transaction_items')

  table.create = true

  table.columns.add('transaction_id', sql.Int, {nullable: false})
  table.columns.add('item_index', sql.Int, {nullable: false})
  table.columns.add('menu_item_id', sql.Int, {nullable: false})
  table.columns.add('quantity', sql.Int, {nullable: false})
  table.columns.add('amount', sql.Decimal(19, 2), {nullable: false})

  transaction.items.forEach((item, itemIndex) => {
    table.rows.add(transaction.id, itemIndex, item.id, item.quantity, item.amount)
  })

  var request = new sql.Request()
  return request.bulk(table)
}

function removeTransactions({id: customerId, primaryposref: accountNumber}) {
  console.log(`removing previous transactions for ${accountNumber} [id:${customerId}]`)

  return sql.query `delete from transaction_items where transaction_id in (select id from transactions where customer_id = ${customerId})`
    .then(() => sql.query `delete from transactions where customer_id = ${customerId}`)
}
