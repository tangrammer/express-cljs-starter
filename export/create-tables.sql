create table customers (
  id integer PRIMARY KEY,
  primaryposref varchar(128),
  first_name varchar(128),
  last_name varchar(128),
  mobile varchar(128),
  email varchar(128),
  city varchar(128),
  province varchar(128),
  gender char,
  birthday date,
  created datetime,
  createddate datetime
);

create table menu_items (
  id integer PRIMARY KEY,
  name varchar(256),
  major_group varchar(128),
  family_group varchar(128)
);

create table locations (
  id   INTEGER PRIMARY KEY,
  name VARCHAR(128)
);

create table programs (
  id integer PRIMARY KEY,
  name varchar(128),
  code varchar(128)
);

/*
create table transactions (
  id integer identity(1,1) PRIMARY KEY ,
  customer_id integer, -- FOREIGN KEY references customers(id),
  program_id integer, -- FOREIGN KEY references programs,
  location_id integer, -- FOREIGN KEY REFERENCES locations,
  check_number integer,
  amount decimal(19,2),
  balance decimal(19,2),
  date datetime,
  description varchar(128)
);

create table transaction_items (
  transaction_id integer, -- FOREIGN KEY references transactions,
  menu_item_id integer, -- FOREIGN KEY REFERENCES menu_items,
  quantity integer,
  amount decimal(19,2)
);
*/
