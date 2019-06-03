package ru.snek;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;


import static javax.xml.transform.OutputKeys.ENCODING;

public class MailHandler {
    private final static String subject = "Завершение регистрации";
    private final static String smtpHost = "smtp.yandex.ru";
    private final static String address = "fortestpurpose49@yandex.ru";
    private final static String password = "zngxfhyesdiyyzqc";
    private final static int smtpPort = 465;

    public static boolean sendEmail(String email, String token) {
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(address, password);
            }
        };

        Properties props = System.getProperties();
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.auth", "true");
        //props.put("mail.mime.charset", ENCODING);
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        Session session = Session.getDefaultInstance(props, auth);

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(address));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
            msg.setSubject(subject);
            msg.setText("Вот токен для завершения регистрации: " + token);
            Transport.send(msg);
        } catch (MessagingException e) {
            return false;
        }
        return true;
    }
}
