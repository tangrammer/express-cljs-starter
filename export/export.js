'use strict'

Promise = require('bluebird')
const sql = require('mssql')
const micros = require('micros')

micros.setBrand('starbucks')

const
  database = 'SCV_HUB',
  username = 'SCV_SWARM',
  password = '$35L$3a9w'

// const accountNumber = '9623570800021'
const accountNumber = '9623570800136'

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
  return exportCustomer(customer)
  .then(() => {
    return exportTransactions(customer, txs)
  })

}).catch(err => {
  console.error(err)
})

function exportCustomer(customer) {

  console.log(`exporting ${customer.primaryposref}`)

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

        console.log('inserted customer')

        ps.unprepare(err => {
          resolve()
        })
      })
    })
  })
}

function exportTransactions(customer, txs) {
  var table = new sql.Table('transactions')

  table.create = true

  console.log('customer', customer.id)

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

  var request = new sql.Request()
  request.bulk(table, function(err, rowCount) {
    console.log('done, err?', err, rowCount)
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
  request.bulk(table, function(err, rowCount) {
    console.log('done, err?', err, rowCount)
  })

}
