/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package TestingApp.SMTP;

import java.io.PrintWriter;

import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;

public class SMTPMailTest {
    private Session session;
    private String testSubjectString = "Sent from Liberty JavaMail";
    private String testBodyString = "Test mail sent by GreenMail";

    void sendMail(HttpServletResponse response, PrintWriter out) throws FolderClosedException {

        try {
            Context context = new InitialContext();
            session = (javax.mail.Session) context.lookup("SMTPJNDISession");
        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(session.getProperty("user")));
            message.setRecipients(Message.RecipientType.TO,
                                  InternetAddress.parse(session.getProperty("from")));
            message.setSubject(testSubjectString);
            message.setText(testBodyString);

            Transport transport = session.getTransport("smtp");
            transport.connect();
            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
