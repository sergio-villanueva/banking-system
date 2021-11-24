package com.mybank.atm.account;

public class Account {
    private String pinNumber;
    private double balance;
    private Integer id;


    public Account(String pinNumber) {
        this.pinNumber = pinNumber;
        this.balance = 0;
        this.id = null;
    }

    public Account(String pinNumber, double balance, int id) {
        this.pinNumber = pinNumber;
        this.balance = balance;
        this.id = (Integer) id;
    }

    /*
     * Method: Used to return Object PIN
     * Return: String = PIN
     * */
    public String getPinNumber() {
        return pinNumber;
    }

    /*
     * Method: Used to return Object Balance
     * Return: double = balance
     * */
    public double getBalance() {
        return balance;
    }

    /*
     * Method: Used to return Object id
     * Return: Integer = record id | null - not assigned at Constructor
     * */
    public Integer getId() {
        return id;
    }

    /*
     * Method: Used to perform Account deposits
     * Return: N/A
     * */
    public void deposit(double depositAmount) {
        balance += depositAmount;
    }

    /*
     * Method: Used to perform Account withdrawals. If the withdrawal is greater than balance, then method fails.
     * Return: boolean = true - successful withdrawal | false - unsuccessful withdrawal
     * */
    public boolean withdraw(double withdrawAmount) {
        if (withdrawAmount <= balance) {
            balance -= withdrawAmount;
            return true;
        }
        return false;
    }

}
