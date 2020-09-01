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
package TestingApp.POP3;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.FolderClosedException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Folder;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/POP3JNDIServlet")
public class POP3JNDIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static String jndiName = "POP3JNDISession";
    Address[] in;
    Session session;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        Object jndiConstant;

        try {
            jndiConstant = new InitialContext().lookup("TestingApp/pop3_port");
        } catch (NamingException e) {
            System.out.println("Failed to lookup 'TestingApp/pop3_port': " + e.getMessage());
            e.printStackTrace(System.out);
            throw new RuntimeException(e);
        }

        String pop3Port = Integer.toString((Integer) jndiConstant);

        Properties props = new Properties();
        props.put("mail.store.protocol", "pop3");
        props.put("mail.pop3.connectiontimeout", "10000");
        props.put("mail.pop3.port", pop3Port);
        props.put("mail.pop3.host", "localhost");
        props.put("user", "test");
        props.put("mail.debug.auth", true);
        props.put("mail.debug", true);
        props.put("password", "test");

        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                PasswordAuthentication passwordAuthentication = new PasswordAuthentication("test", "test");
                return passwordAuthentication;
            }
        });
        session.setDebug(true);
        POP3MailTest pop3Mail = new POP3MailTest();

        try {
            InitialContext ic = new InitialContext();
            ic.bind(jndiName, session);
            try {
                pop3Mail.readMail(response, out);
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
