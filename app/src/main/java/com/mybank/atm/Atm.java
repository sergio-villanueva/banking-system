package com.mybank.atm;

import com.mybank.atm.account.Account;
import com.mybank.atm.account.CheckingAccount;
import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.*;

public class Atm {

    private String bankName;
    private String databaseUrl;
    private Set<Account> updatedAccounts;

    public Atm(String bankName, String databaseUrl) {
        this.bankName = bankName;
        this.databaseUrl = databaseUrl;
        this.updatedAccounts = new HashSet<Account>(); // used to update to database all accounts at once after client logs out

        // also creates the database if it does not exist
        createTable();
    }

    /*Method: Used to run the Atm class
     * Return: None
     * */
    public void start() {
        // try statement used to close scanner in case of abrupt application exits
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to " + bankName);

            while (true) {
                // Get User Input
                displayMainMenu();
                int userOption = scanner.nextInt();
                scanner.nextLine(); //throw away the \n that is not consumed
                //
                switch (userOption) {
                    case 0:
                        // code for exiting
                        return;
                    case 1:
                        // code for creating account
                        String newCardNumber = generateCardNumber();
                        String newPinNumber = generatePin();
                        CheckingAccount account = new CheckingAccount(newCardNumber, newPinNumber);

                        System.out.println(); // empty line to prevent terminal clutter
                        System.out.println("Your card has been created");
                        System.out.println("Your card number:");
                        System.out.println(newCardNumber);
                        System.out.println("Your card PIN:");
                        System.out.println(newPinNumber);

                        // insert account into database
                        addCheckingAccountToDatabase(account);
                        break;
                    case 2:
                        // code for logging in
                        System.out.println(); // empty line to prevent terminal clutter
                        System.out.println("Enter your card number:");
                        String clientCardNumber = scanner.nextLine();
                        System.out.println("Enter your PIN:");
                        String clientPinNumber = scanner.nextLine();

                        boolean loginState = logIn(clientCardNumber, clientPinNumber);
                        System.out.println(); // empty line to prevent terminal clutter

                        if (loginState) {
                            // Login was successful

                            // Used to represent account data while logged in
                            CheckingAccount client = getCheckingAccount(clientCardNumber);
                            System.out.println("You have successfully logged in!");
                            boolean userExit = false;

                            // display menu again until user logs out or exits the application
                            while (!userExit) {
                                displayAccountMenu();
                                int accountOption = scanner.nextInt();
                                scanner.nextLine(); //throw away the \n that is not consumed

                                switch (accountOption) {
                                    case 0:
                                        // code for exiting
                                        saveBalanceUpdates();
                                        return;
                                    case 1:
                                        // code to display balance
                                        double userBalance = client.getBalance();

                                        System.out.println(); // empty line to prevent terminal clutter
                                        System.out.print("Balance: ");
                                        System.out.println(String.format("%.2f", userBalance));
                                        break;
                                    case 2:
                                        // code for adding income
                                        System.out.println(); // empty line to prevent terminal clutter
                                        System.out.println("Enter Deposit Amount: ");
                                        double depositAmount = scanner.nextDouble();
                                        scanner.nextLine(); //throw away the \n that is not consumed

                                        client.deposit(depositAmount);
                                        updatedAccounts.add(client);
                                        break;
                                    case 3:
                                        // code for doing transfer
                                        System.out.println(); // empty line to prevent terminal clutter
                                        System.out.println("Enter Sender Card Number: ");
                                        String receiverCardNumber = scanner.nextLine();

                                        // check if receiver card number is not valid before recreating its account
                                        if (!luhnAlgorithm(receiverCardNumber)) {
                                            System.out.println(); // empty line to prevent terminal clutter
                                            System.out.println("Invalid Card Number: Try Again");
                                            continue;
                                        }

                                        CheckingAccount receiver = getCheckingAccount(receiverCardNumber);

                                        // check if receiver account does NOT exist in database
                                        if (receiver == null) {
                                            System.out.println(); // empty line to prevent terminal clutter
                                            System.out.println("Invalid Card Number: Account Does Not Exist");
                                            //System.out.println("Invalid Card Number");
                                            continue;
                                        }

                                        // ensure client and receiver are NOT the same account
                                        if (client.equals(receiver)) {
                                            System.out.println("Invalid Transfer: Sender and Receiver Accounts are the same");
                                            continue;
                                        }


                                        System.out.println("Enter Transfer Amount: ");
                                        double transferAmount = scanner.nextDouble();
                                        scanner.nextLine(); //throw away the \n that is not consumed

                                        boolean transferState = transfer(client, receiver, transferAmount);
                                        // if statement used for debugging
                                        if (!transferState) {
                                            System.out.println("Invalid Transfer: Insufficient Funds");
                                        } else {
                                            System.out.println("Transfer Was Successful");
                                            updatedAccounts.add(client);
                                            updatedAccounts.add(receiver);
                                        }
                                        break;
                                    case 4:
                                        // code for deleting an account
                                        /*
                                        System.out.println("Account data will be lost");
                                        System.out.println("Confirm Delete: ");


                                        boolean deleteState = scanner.nextBoolean();
                                        if (deleteState) {
                                            deleteAccount(client);
                                        }
                                        */
                                        deleteCheckingAccount(client);
                                        System.out.println("Account Deleted");
                                        // log out
                                        saveBalanceUpdates();
                                        userExit = true;
                                        break;
                                    case 5:
                                        // code to log out
                                        saveBalanceUpdates();
                                        userExit = true;
                                        break;
                                    default:
                                        // code for invalid account option
                                        System.out.println(); // empty line to prevent terminal clutter
                                        System.out.println("Invalid Option: Try Again");
                                }
                            }


                        } else {
                            System.out.println("Invalid Credentials");
                        }
                        break;
                }
            }
        }


    }

    /*
     * Method: Used to delete a client account from the database
     * Return: N/A
     * */
    private void deleteCheckingAccount(CheckingAccount client) {
        // remove client from the update list
        updatedAccounts.remove(client);

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(databaseUrl);

        String sql = "DELETE FROM card WHERE number = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, client.getCardNumber());
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * Method: Used to update the data from all Account Objects into database
     * Return: N/A
     * */
    private void saveBalanceUpdates() {
        if (updatedAccounts.isEmpty()) { return; }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(databaseUrl);

        String sql = "UPDATE card SET balance = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection()) {


            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                Iterator<Account> iterator = updatedAccounts.iterator();
                while (iterator.hasNext()) {
                    Account account = iterator.next();
                    iterator.remove();
                    preparedStatement.setDouble(1, account.getBalance());
                    preparedStatement.setInt(2, account.getId());

                    preparedStatement.executeUpdate();

                }

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * Method: Used to return CheckingAccount Object created using data from the database
     * Return: CheckingAccount = a valid record from database
     * */
    private CheckingAccount getCheckingAccount(String cardNumber) {

        if (!isActiveCardNumber(cardNumber)) { return null; }

        String pinNumber = getUserPin(cardNumber);
        double balance = getUserBalance(cardNumber);
        int id = getUserId(cardNumber);

        return new CheckingAccount(cardNumber, pinNumber, balance, id);
    }

    /*
     * Method: Used to transfer funds between valid Accounts
     * Return: N/A
     * */
    private boolean transfer(Account sender, Account receiver, double transferAmount) {

        // withdrawal was a success
        if (sender.withdraw(transferAmount)) {
            receiver.deposit(transferAmount);
            return true;
        }

        // insufficient funds
        return false;

    }

    /*
     * Method: Used to add a Checking Account to the 'card' Table within the Database
     * Return: None
     * */
    private void addCheckingAccountToDatabase(CheckingAccount account) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(databaseUrl);

        String sql = "INSERT INTO card (number, pin) VALUES (?,?);";

        // Use a PrepareStatement Object since we will execute a Dynamic SQL Query
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, account.getCardNumber());
            preparedStatement.setString(2, account.getPinNumber());

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * Method: Displays the Main Menu for clients to interface with.
     * Return: N/A
     * */
    private void displayMainMenu() {
        System.out.println(); // empty line to prevent terminal clutter
        System.out.println("1. Create an account");
        System.out.println("2. Log into account");
        System.out.println("0. Exit");
    }

    /*
     * Method: Used for client login
     * Return: boolean = true - successful login | false - unsuccessful login
     * */
    private boolean logIn(String cardNumber, String pinNumber) {

        String realPin = getUserPin(cardNumber);

        // Validate Client Credentials
        if (pinNumber.equals(realPin) && luhnAlgorithm(cardNumber)) {

            return true;
        }

        return false;
    }


    /*
     * Method: Used to generate client PIN from the database based on the client Card Number
     * Return: String = client pin
     * */
    private String getUserPin(String cardNumber) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(databaseUrl);

        String userPin = null;
        String sql = "SELECT * FROM card WHERE number = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, cardNumber);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                // Prevent accessing PIN with an invalid Credit Card Number
                if (resultSet.next()) {
                    //resultSet.previous();
                    userPin = resultSet.getString("pin");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userPin;
    }

    /*
     * Method: Used to generate client record id from the database based on the client Card Number
     * Return: double = client record id
     * */
    private int getUserId(String cardNumber) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(databaseUrl);

        Integer id = null;
        String sql = "SELECT * FROM card WHERE number = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, cardNumber);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                id = resultSet.getInt("id");
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return id;
    }

    /*
     * Method: Used to generate client balance from the database based on the client Card Number
     * Return: double = client balance
     * */
    private double getUserBalance(String cardNumber) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(databaseUrl);

        double userBalance = 0;
        String sql = "SELECT * FROM card WHERE number = ?;";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, cardNumber);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                userBalance = resultSet.getDouble("balance");
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userBalance;
    }

    /*
     * Method: Displays the Account Menu for clients to interface with.
     * Return: N/A
     * */
    private void displayAccountMenu() {
        System.out.println(); // empty line to prevent terminal clutter
        System.out.println("1. Balance");
        System.out.println("2. Add income");
        System.out.println("3. Do transfer");
        System.out.println("4. Close account");
        System.out.println("5. Log out");
        System.out.println("0. Exit");
    }

    /*
     * Method: Used to validate a Card Number
     * Return: boolean = true - card number is valid | false - card number is invalid
     * */
    private boolean luhnAlgorithm(String cardNumber) {

        String[] processDigits = cardNumber.split("");
        int controlNumber = 0;
        for (int index = 0; index < processDigits.length; index++) {
            int digit = Integer.parseUnsignedInt(processDigits[index]);
            digit = index % 2 == 0 ? digit * 2 : digit;
            digit = digit > 9 ? digit - 9 : digit;
            controlNumber += digit;
        }

        return controlNumber % 10 == 0;
    }

    /*
     * Method: Used to create the Database and Table 'card' if necessary
     * Return: N/A
     * */
    private void createTable() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(databaseUrl);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS card (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "number TEXT," +
                            "pin TEXT," +
                            "balance INTEGER DEFAULT 0" +
                            ")"
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * Method: Used to generate PIN values for new accounts
     * Return: String = PIN value
     * */
    private String generatePin() {
        Random random = new Random();
        String randomDigits = Arrays.toString(random.ints(4,0,10).toArray());
        String pin = randomDigits.substring(1, randomDigits.length() - 1).replace(", ", "");

        return pin;
    }

    /*
     * Method: Used to generate Card Numbers for new accounts
     * Return: String = Card Number
     * */
    private String generateCardNumber() {

        Random random = new Random();
        // BIN
        String bankIdNumber = "400000";
        // Generate the Account Identifier
        String randomDigits = Arrays.toString(random.ints(9,0,10).toArray()); // temp
        String accountNumber = randomDigits.substring(1, randomDigits.length() - 1).replace(", ", "");

        // Determine a valid Checksum
        int controlNumber = 0;
        String[] processDigits = bankIdNumber.concat(accountNumber).split("");
        for (int index = 0; index < processDigits.length; index++) {
            int digit = Integer.parseUnsignedInt(processDigits[index]);
            digit = index % 2 == 0 ? digit * 2 : digit;
            digit = digit > 9 ? digit - 9 : digit;
            controlNumber += digit;
        }

        String checkSum = (controlNumber % 10) == 0 ? "0" : Integer.toString(10 - (controlNumber % 10));

        // Card Number = BIN + Account Identifier + Checksum
        String cardNumber = bankIdNumber + accountNumber + checkSum;

        // card number may be assigned twice
        if (isActiveCardNumber(cardNumber)) {
            cardNumber = generateCardNumber();
        }


        return cardNumber;
    }

    /*
     * Method: Used to check if a Card Number exists in the database
     * Return: boolean = true - active | false - inactive
     * */
    private boolean isActiveCardNumber(String cardNumber) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(databaseUrl);

        boolean cardState = false;
        String sql = "SELECT 1 FROM card WHERE number = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, cardNumber);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                cardState = resultSet.next();
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return cardState;
    }

}
