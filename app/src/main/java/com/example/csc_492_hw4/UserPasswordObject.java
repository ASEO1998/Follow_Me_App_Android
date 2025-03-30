package com.example.csc_492_hw4;

import java.io.Serializable;

public class UserPasswordObject implements Serializable {
    private final String username;
    private final String password;


    public UserPasswordObject(String username, String password) {
        this.username = username;
        this.password = password;

    }

    String getUsername(){return username;}
    String getPassword(){return password;}

}
