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
package TestingApp.POP3;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/POP3InlineServlet")
public class POP3InlineServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    Address[] in;
    Session session;
    Object jndiConstant;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {
            jndiConstant = new InitialContext().lookup("TestingApp/POP3InlineServlet/pop3_port");

        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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

        try {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    PasswordAuthentication passwordAuthentication = new PasswordAuthentication("test", "test");
                    return passwordAuthentication;
                }
            });
            session.setDebug(true);

            Store store = session.getStore();

            store.connect();
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message msg = inbox.getMessage(inbox.getMessageCount()); // get newest message
            out.println("Message headers are: ==========="); // includes content-type, might get charset.
            out.println("<p></p>");

            if (!store.isConnected()) {
                store.connect();
                inbox.open(Folder.READ_WRITE);
            }
            Enumeration en = msg.getAllHeaders();

            while (en.hasMoreElements()) {
                Header h = (Header) en.nextElement();
                out.println(h.getName() + " " + h.getValue());
                out.println("<p></p>");
            }
            out.println(" end headers =================");
            out.println("<p></p>");
            out.println("*** Message Found and Accessed ***");
            System.out.println("*** Message Found and Accessed ***");

            store.close();
        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub

    }

}
