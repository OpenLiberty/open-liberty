package com.ibm.ws.javamail.client;

import java.util.Properties;

import javax.annotation.Resource;
import javax.mail.Authenticator;
import javax.mail.MailSessionDefinition;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

@MailSessionDefinition(name = "java:comp/mail/MailSessionDefinitionAnnotation",
                       host = "localhost",
                       from = "fat@testserver.com",
                       storeProtocol = "imap",
                       transportProtocol = "smtp")
public class Main {
    @Resource(lookup = "java:comp/mail/MailSessionDefinitionAnnotation")
    private static Session mailSessionDefinitionAnnotation;

    @Resource(lookup = "java:comp/mail/MySession")
    private static Session mailSessionDefinitionDescriptor;

    public static void main(String[] args) throws Exception {
        //print out a message that FAT will be checking for
        System.out.println("Enter JavaMailClient test app main.");

        //Create a mail session inline for initial test. Ports to use must be set by FAT when client is launched.
        String imapPort = System.getProperty("com.ibm.ws.javaMailClient.fat.imap.port");
        String smtpPort = System.getProperty("com.ibm.ws.javaMailClient.fat.smtp.port");

        Properties props = new Properties();
        props.setProperty("mail.smtp.port", smtpPort);
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.host", "localhost");
        props.setProperty("mail.imap.port", imapPort);
        props.setProperty("mail.from", "fat@testserver.com");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("user@testserver.com", "userPass");
            }
        });
        session.setDebug(true);

        SendReceiveMailFromClientTest testBasicSendReceive = new SendReceiveMailFromClientTest(session, "Client container test message 1 sent at time: "
                                                                                                    + System.currentTimeMillis(),
                                                                                           "testBasicSendReceive");
        testBasicSendReceive.send();
        // Receiving mails later give server to process incoming mails
        for(int i=0;i<5;++i) {
          System.out.println("Iteration: " + i + ", at time: " + System.currentTimeMillis());
          Thread.sleep(5000);
          if(testBasicSendReceive.receive())  {
            System.out.println("Message received at time: " + System.currentTimeMillis());
            break;
          }
        }

        //define the ports here because they are controlled from the FAT ports file and so can't be
        //  defined in the annotation which takes constant values.
        mailSessionDefinitionAnnotation.getProperties().setProperty("mail.smtp.port", smtpPort);
        mailSessionDefinitionAnnotation.getProperties().setProperty("mail.imap.port", imapPort);
        //set user and password like this until defect on storing password in MailSession definition is fixed.
        mailSessionDefinitionAnnotation.getProperties().setProperty("my.bypass.user", "user@testserver.com");
        mailSessionDefinitionAnnotation.getProperties().setProperty("my.bypass.password", "userPass");
        SendReceiveMailFromClientTest testInjectionFromAnnotationDefinition = new SendReceiveMailFromClientTest(mailSessionDefinitionAnnotation, "Client container test message 2 sent at time: "
                                                                                                                                                 + System.currentTimeMillis(), "testInjectionFromAnnotationDefinition");
        testInjectionFromAnnotationDefinition.send();
        // Receiving mails later give server to process incoming mails
        testInjectionFromAnnotationDefinition.receive();

        //define the ports here because they are controlled from the FAT ports file and so can't be
        //  defined in the annotation which takes constant values.
        mailSessionDefinitionDescriptor.getProperties().setProperty("mail.smtp.port", smtpPort);
        mailSessionDefinitionDescriptor.getProperties().setProperty("mail.imap.port", imapPort);
        //set user and password like this until defect on storing password in MailSession definition is fixed.
        mailSessionDefinitionDescriptor.getProperties().setProperty("my.bypass.user", "user@testserver.com");
        mailSessionDefinitionDescriptor.getProperties().setProperty("my.bypass.password", "userPass");
        SendReceiveMailFromClientTest testInjectionFromDescriptorDefinition = new SendReceiveMailFromClientTest(mailSessionDefinitionDescriptor, "Client container test message 3 sent at time: "
                                                                                                                                                 + System.currentTimeMillis(), "testInjectionFromDescriptorDefinition");
        testInjectionFromDescriptorDefinition.send();
        // Receiving mails later give server to process incoming mails
        testInjectionFromDescriptorDefinition.receive();

        System.out.println("Exit client app main");
    }

    public Main() {
        super();
    }

}
