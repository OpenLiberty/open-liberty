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
package TestingApp.IMAP;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.FolderClosedException;
import jakarta.mail.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import TestingApp.web.IMAPMailTest;

@WebServlet("/IMAPJNDIServlet")
public class IMAPJNDIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static Address[] in;
    public static Session session;
    private static String jndiName = "IMAPJNDISession";

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        Object jndiConstant;

        try {
            jndiConstant = new InitialContext().lookup("TestingApp/imap_port");
        } catch (NamingException e) {
            System.out.println("Failed to lookup 'TestingApp/imap_port': " + e.getMessage());
            e.printStackTrace(System.out);
            throw new RuntimeException(e);
        }
        String imapPort = Integer.toString((Integer) jndiConstant);

        try {
            jndiConstant = new InitialContext().lookup("TestingApp/smtp_port");
        } catch (NamingException e) {
            System.out.println("Failed to lookup 'TestingApp/smtp_port': " + e.getMessage());
            e.printStackTrace(System.out);
            throw new RuntimeException(e);
        }
        String smtpPort = Integer.toString((Integer) jndiConstant);

        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.imap.host", "localhost");
        props.setProperty("user", "imap@testserver.com");
        props.setProperty("password", "imapPa$$word4U2C");
        props.setProperty("mail.imap.port", imapPort);
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.transport.host", "smtp");
        props.setProperty("from", "imap@testserver.com");
        props.setProperty("mail.smtp.port", smtpPort);

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                PasswordAuthentication passwordAuthentication = new PasswordAuthentication("imap@testserver.com", "imapPa$$word4U2C");
                return passwordAuthentication;
            }
        });
        session.setDebug(true);
        IMAPMailTest imapMail = new IMAPMailTest();

        try {
            InitialContext ic = new InitialContext();
            ic.bind(jndiName, session);
            try {
                imapMail.readMail(response, out);
            } catch (FolderClosedException e) {
                e.printStackTrace();
            }
            ic.unbind(jndiName);
        } catch (NamingException e) {
            e.printStackTrace(System.out);
            throw new RuntimeException(e);
        }

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}

}
