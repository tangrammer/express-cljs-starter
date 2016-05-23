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

Promise.all([
  micros.transactions({accountNumber}),
  sql.connect(`mssql://${username}:${password}@localhost:1433/${database}`)
])

.then(([txs]) => {
  console.log('connected...')

  var table = new sql.Table('tx4')

  table.create = true;

  table.columns.add('check_number', sql.Int, {nullable: true})
  table.columns.add('amount', sql.Decimal(19, 2), {nullable: true})
  table.columns.add('balance', sql.Decimal(19, 2), {nullable: true})
  table.columns.add('description', sql.VarChar(128), {nullable: true})

  txs.forEach((tx) => {
    console.log('inserting', tx)
    table.rows.add(tx.check, tx.amount, tx.balance, tx.description)
  })

  var request = new sql.Request()
  request.bulk(table, function(err, rowCount) {
    console.log('done, err?', err, rowCount)
  })

}).catch(err => {
  console.error(err)
})
