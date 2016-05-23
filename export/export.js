'use strict'

Promise = require('bluebird')
const sql = require('mssql')
const micros = require('micros')

micros.setBrand('starbucks')

const
  database = 'SCV_HUB',
  username = 'SCV_SWARM',
  password = '$35L$3a9w'

const accountNumber = '9623570800021'
// const accountNumber = '9623570800136'

const cols = [
  'firstname',
  'lastname',
  'primaryposref',
  'emailaddress',
  'city',
  'state',
]

Promise.all([
  micros.getCustomerDetails(accountNumber, cols),
  micros.transactions({accountNumber}),
  sql.connect(`mssql://${username}:${password}@localhost:1433/${database}`),
])

.then(([[customer], txs]) => {
  console.log(customer.id)
  return exportCustomer(customer)
  .then(() => {
    return exportTransactions(customer, txs)
  })

}).catch(err => {
  console.error(err)
})

function exportCustomer(customer) {

  console.log(`exporting ${customer.primaryposref}`)

  console.log(customer.id)

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
