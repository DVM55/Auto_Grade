package com.example.Auto_Grade.exception;

public class UsernameNotFoundException extends RuntimeException{
    public UsernameNotFoundException(String message){
        super(message);
    }
}
