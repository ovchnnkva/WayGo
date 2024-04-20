package ru.project.waygo.mail;

import android.os.AsyncTask;
import android.util.Log;

public class MailSenderAsync extends AsyncTask<Object, String, Boolean> {
    private String theme;
    private String message;
    private String fromEmail;

    public MailSenderAsync(String theme, String message, String fromEmail) {
        this.theme = theme;
        this.message = message;
        this.fromEmail = fromEmail;
    }
    @Override
    protected Boolean doInBackground(Object... objects) {
        MailSender mailSender = new MailSender();
        mailSender.sendMail(theme, message, fromEmail, "nuta.super.12340@gmail.com");
        return null;
    }
}
