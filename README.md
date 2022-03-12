# Banking System
An ATM console system used to manage client accounts. 

## Startup
This application was build using Java 17. When ran from the command-line, the application may take two optional arguments:
1) Bank Name - A name for our bank
2) Database Name - A name for database file

After reaching the main menu, we have three options:
1) Create an Account - App returns a 16-digit Card Number and 4-digit PIN for a client account. Use these credentials for logging in.
2) Log into Account - App checks the given PIN and applies the Luhn Algorithm to given card number before granting account access.
3) Exit - Shut down the application

Note: All card numbers follow standard industry format: Bank Identificatio Number + Account Identifier + Checksum. For application purposes, the BIN (Bank Identification Number) is '400000'.
## Services

This application uses a local SQLite database to store client data and credentials. After logging in, the client is granted access to the following services:

1) Balance - Display the current balance
2) Add income - Make a deposit
3) Do transfer - Make a transfer between two valid accounts
4) Close Account - Delete an account from the database
5) Log out - exit the account


