# banking-system
ATM console system used to create and log into accounts. When ran from the command-line, the application may take two arguments:
1) Bank Name
2) Database Name

When the application first starts, a database is created for storing client information. When a client creates an account, a 16-digit Card Number and 4-digit PIN is displayed so that the user may log in with. After logging in, a user can:
1) Display Balance
2) Make a Deposit
3) Make a Transfer to another Valid Account
4) Delete their Account
5) Log out

Note: For application purposes, the BIN (Bank Identification Number) is '400000'. All Card Numbers outputted by this application satisfy the Luhn Algorithm.


