'use strict'

const
  Promise = require('bluebird'),
  sql = require('mssql'),
  micros = require('micros')

micros.setBrand('starbucks')

const
  database = 'SCV_HUB',
  username = 'SCV_SWARM',
  password = '$35L$3a9w'

const cols = [
  'firstname',
  'lastname',
  'primaryposref',
  'emailaddress',
  'city',
  'state',
]

// console.log('starting')
// getAccounts().then(a => {
//   console.log('done')
//   console.log(a)
//   console.log(a.length)
// })

sql.connect(`mssql://${username}:${password}@localhost:1433/${database}`)
.then(getAccounts)
// .then(console.log)
.then(accounts => {
  console.log(`got ${accounts.length} accounts`)
  return Promise.map(accounts, (account) => {
    const accountNumber = account.primaryposref
    console.log(`starting ${accountNumber}`)
    return exportCustomer(accountNumber)
  }, {concurrency: 1})
})
.catch(err => console.error(err.stack))

function exportCustomer(accountNumber) {

  return Promise.all([
    micros.getCustomerDetails(accountNumber, cols),
    micros.transactions({accountNumber}),
    micros.getStarbucksBalances(accountNumber),
  ])
  .then(([customerArr, txs, balances]) => {
    if (!customerArr || !customerArr[0]) return

    let customer = customerArr[0]
    return exportCustomerProfile(customer)
    .then(() => {
      return exportTransactions(customer, txs)
    })
    .then(() => {
      return exportBalances(customer, balances)
    })
  })
}

function getAccounts() {
  // const accounts = require('./accounts')
  // return accounts
  // return micros.get('Customer', {condition: 'primaryposref = ?', values: [{primaryposref: '9623570900002'}], resultCols: ['primaryposref']})
  return micros.get('Customer', {condition: '', values: [], resultCols: ['primaryposref']})
  // return Promise.resolve([ { id: '42406261', primaryposref: '9623570900002' } ])
  // return Promise.resolve([ { id: '42229452', primaryposref: '9623570900003' }])
}

function exportCustomerProfile(customer) {

  console.log(`exporting ${customer.primaryposref}`)

  return sql.query`delete from customers where id = ${customer.id}`
  .then(() => {

    const ps = new sql.PreparedStatement()

    return new Promise((resolve, reject) => {

      ps.input('id', sql.Int)
      ps.input('primaryposref', sql.VarChar(128))
      ps.input('first_name', sql.VarChar(128))
      ps.input('last_name', sql.VarChar(128))
      ps.input('email', sql.VarChar(128))
      ps.input('city', sql.VarChar(128))
      ps.input('province', sql.VarChar(128))

      ps.prepare('insert into customers(id, primaryposref, first_name, last_name, email, city, province) values(@id, @primaryposref, @first_name, @last_name, @email, @city, @province)', err => {

        if (err) throw err

        ps.execute({
          id: customer.id,
          primaryposref: customer.primaryposref,
          'first_name': customer.firstname,
          'last_name': customer.lastname,
          email: customer.email,
          city: customer.city,
          province: customer.region,
        }, (err, recordsets, affected) => {

          if (err) return reject(err)

          console.log(`inserted customer ${customer.primaryposref}`)

          ps.unprepare(err => {
            resolve()
          })
        })
      })
    })
  })
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

    txs.forEach((tx) => {
      table.rows.add(tx.id, customer.id, tx.check, tx.amount, tx.balance, tx.description)
      exportCheckItems(tx, tx.items)
    })

    const request = new sql.Request()
    return request.bulk(table)
    .then((rowCount) => {
      console.log(`done exporting ${rowCount} transactions`)
    })
  })
}

function exportBalances(customer, balances) {
  const table = new sql.Table('balances')
  table.create = true

  table.columns.add('customer_id', sql.Int, {nullable: false})
  table.columns.add('program', sql.VarChar(128), {nullable: false})
  table.columns.add('balance', sql.Decimal(19, 2), {nullable: false})

  balances.programs.forEach((program) => {
    table.rows.add(customer.id, program.program, program.balance)
  })

  var request = new sql.Request()
  return request.bulk(table)
}

function exportCheckItems(transaction) {

  const table = new sql.Table('transaction_items')

  table.create = true

  table.columns.add('transaction_id', sql.Int, {nullable: false})
  table.columns.add('menu_item_id', sql.Int, {nullable: false})
  table.columns.add('quantity', sql.Int, {nullable: false})
  table.columns.add('amount', sql.Decimal(19, 2), {nullable: false})

  transaction.items.forEach((item) => {
    table.rows.add(transaction.id, item.id, item.quantity, item.amount)
  })

  var request = new sql.Request()
  return request.bulk(table)
}

function removeTransactions({id: customerId, primaryposref: accountNumber}) {
  console.log(`removing previous transactions for ${accountNumber} [id:${customerId}]`)
  return sql.query`delete from transactions where customer_id = ${customerId}`
}
