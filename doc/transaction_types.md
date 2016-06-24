Transaction without a point earn:

```JSON
{
		"historyId": 44,
		"historyType": "ScvTransaction",
		"cardId": null,
		"isoDate": "2016-04-21T08:31:04Z",
		"modifiedDate": null,
		"currency": "EUR",
		"localCurrency": "EUR",
		"totalAmount": -1.54,
		"localTotalAmount": -1.54,
		"svcTransaction": {
			"checkId": "21945",
			"transactionType": "Purchase",
			"isVoid": false,
			"localizedStoreName": "W Mariahilfer Strasse ",
			"storeId": "2281",
			"storeType": "Physical",
			"localDate": "2016-04-21T08:31:04Z",
			"currency": "EUR",
			"localCurrency": "EUR",
			"transactionAmount": -1.54,
			"localTransactionAmount": -1.54,
			"tax": null,
			"newBalance": 31.66,
			"description": " Chk: 21945",
			"tipInfo": null
		},
		"points": []
	}
```

Points earned (2 options I guess?):

```JSON
	{
		"historyId": 45,
		"historyType": "Points",
		"cardId": null,
		"isoDate": "2016-04-21T08:31:01Z",
		"modifiedDate": null,
		"currency": "EUR",
		"localCurrency": "EUR",
		"totalAmount": 0.0,
		"localTotalAmount": 0.0,
		"svcTransaction": {
			"checkId": "21945",
			"transactionType": "POS Points Issuance",
			"isVoid": false,
			"localizedStoreName": "W Mariahilfer Strasse ",
			"storeId": "2281",
			"storeType": "Physical",
			"localDate": "2016-04-21T08:31:01Z",
			"currency": "EUR",
			"localCurrency": "EUR",
			"transactionAmount": 0.0,
			"localTransactionAmount": 0.0,
			"tax": null,
			"newBalance": 0.0,
			"description": "Purchase Chk: 21945",
			"tipInfo": null
		},
		"points": [{
			"pointType": "Default",
			"pointsEarned": 1.00,
			"promotionName": "Default",
			"localAmount": 0.0,
			"localCurrency": 0.0
		}]
	}
```

```JSON
	{
		"historyId": 5476631219,
		"historyType": "SvcTransactionWithPoints",
		"cardId": "856074FF93D218A890",
		"isoDate": "2016-05-31T05:49:23.0000000Z",
		"modifiedDate": "2016-06-23T10:51:29.4600000Z",
		"currency": "EUR",
		"localCurrency": "EUR",
		"totalAmount": 4.85,
		"svcTransaction": {
			"checkId": "LARWs2roc7z",
			"transactionType": "Redemption",
			"isVoid": false,
			"localizedStoreName": "Poissonniere",
			"storeId": "12687",
			"storeNumber": "12687",
			"storeType": "Physical",
			"localDate": "2016-05-31T07:49:23.0000000",
			"currency": "EUR",
			"localCurrency": "EUR",
			"transactionAmount": 4.85,
			"localTransactionAmount": 4.85,
			"tax": null,
			"newBalance": 25.65,
			"description": null,
			"tipInfo": {
				"tippable": false,
				"tippableEndDate": null,
				"tipTransactionId": null,
				"amount": null,
				"status": "None"
			},
			"brandName": "Starbucks",
			"localTransactionAccruableAmount": null
		},
		"localTotalAmount": 4.85,
		"points": [{
			"pointType": "Purchases",
			"pointsEarned": 1,
			"promotionName": "France Starbucks Admin Accrual",
			"amount": 4.85,
			"currency": 0,
			"expirationDate": null,
			"totalPointsEarned": 1.0,
			"pointCategory": "Tier",
			"status": "Unknown"
		}],
		"localTotalAccruableAmount": null,
		"coupon": null
	}
```

Coupon earned/redeemed:

```JSON
	{
		"historyId": 5472747845,
		"historyType": "Coupon",
		"cardId": null,
		"isoDate": "2016-05-23T01:00:00.0000000",
		"modifiedDate": "2016-06-23T10:51:29.4770000Z",
		"currency": null,
		"localCurrency": null,
		"totalAmount": null,
		"svcTransaction": null,
		"localTotalAmount": null,
		"points": null,
		"localTotalAccruableAmount": null,
		"coupon": {
			"couponCode": "BFB",
			"name": "BIRTHDAY FREE BEVERAGE FR",
			"issueDate": "2016-05-23T01:00:00.0000000",
			"expirationDate": "2016-07-07T23:59:59.0000000-07:00",
			"allowedRedemptionCount": 1,
			"voucherType": "MSREarnCoupon",
			"status": "Redeemed",
			"startDate": "2016-05-23T01:00:00.0000000",
			"lastRedemptionDate": "2016-05-31T05:49:15.0000000Z",
			"redemptionCount": 1,
			"posCouponCode": "593",
			"deliveryMethod": "Email",
			"source": "System",
			"rewardCode": "593",
			"description": {
				"short": "BIRTHDAY FREE BEVERAGE FR",
				"long": null
			},
			"redemptionDetail": {
				"lastRedemptionDate": "2016-05-31T05:49:15.0000000Z",
				"redemptionSources": []
			},
			"couponId": "1-3QDGZ-3963",
			"modifiedDate": "2016-05-30T22:49:15.0000000"
		}
	}
```
