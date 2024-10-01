package com.ns.membership.application.port.in;

import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;

public interface MailSendUseCase {

    String sendRegisterMessage(String name) throws MessagingException, UnsupportedEncodingException;
    String sendPasswordResetMessage(String name) throws MessagingException, UnsupportedEncodingException;
}
