/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package TestingApp.SMTP;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.FolderClosedException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/SMTPJNDIServlet")
public class SMTPJNDIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static Address[] in;
    public static Session session;
    private static String jndiName = "SMTPJNDISession";

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        Object jndiConstant = null;
        try {
            // this is the mail session JNDI name in the server.xml
            jndiConstant = new InitialContext().lookup("TestingApp/smtp_port");
        } catch (NamingException e) {
            System.out.println("Failed to lookup 'TestingApp/smtp_port': " + e.getMessage());
            e.printStackTrace(System.out);
            throw new RuntimeException(e);
        }
        String smtpPort = Integer.toString((Integer) jndiConstant);

        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", smtpPort);
        props.setProperty("user", "smtp@testserver.com");
        props.setProperty("password", "smtpPa$$word4U2C");
        props.setProperty("from", "smtp@testserver.com");

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                PasswordAuthentication passwordAuthentication = new PasswordAuthentication("smtp@testserver.com", "smtpPa$$word4U2C");
                return passwordAuthentication;
            }
        });
        session.setDebug(true);
        SMTPMailTest smtpMail = new SMTPMailTest();

        try {
            InitialContext ic = new InitialContext();
            ic.bind(jndiName, session);
            try {
                smtpMail.sendMail(response, out);
            } catch (FolderClosedException e) {
                e.printStackTrace();
            }
            ic.unbind(jndiName);
        } catch (NamingException e) {
            e.printStackTrace(System.out);
        }

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}

}
