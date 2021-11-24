package com.mybank.atm.account;

public class CheckingAccount extends Account {
    private String cardNumber;

    public CheckingAccount(String cardNumber, String pinNumber) {
        super(pinNumber);
        this.cardNumber = cardNumber;
    }

    public CheckingAccount(String cardNumber, String pinNumber, double balance, int id) {
        super(pinNumber, balance, (Integer) id);
        this.cardNumber = cardNumber;
    }

    /*
     * Method: Used to return Object card number
     * Return: String = card number
     * */
    public String getCardNumber() {
        return cardNumber;
    }

    /*
     * Method: Used to check if two accounts are the same
     * Return: boolean = true - same accounts | false - different accounts
     * */
    public boolean equals(CheckingAccount account) {
        return cardNumber.equals(account.getCardNumber());
    }
}
