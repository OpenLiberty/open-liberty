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

import java.io.PrintWriter;
import java.util.Enumeration;

import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;

public class POP3MailTest {
    Session session;

    void readMail(HttpServletResponse response, PrintWriter out) throws FolderClosedException {

        try {
            Context context = new InitialContext();
            session = (javax.mail.Session) context.lookup("POP3JNDISession");
        } catch (NamingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {

            Store store = session.getStore();

            session.setDebug(true);
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
}
