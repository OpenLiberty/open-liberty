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
import java.util.Enumeration;

import javax.annotation.Resource;
import jakarta.mail.Header;
import jakarta.mail.Message;
import jakarta.mail.Session;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/IMAPMailSessionServlet")
public class IMAPMailSessionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Resource(name = "TestingApp/IMAPMailSessionServlet/testIMAPMailSession")
    Session session;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try {
            session.setDebug(true);
            Store store = session.getStore();
            store.connect();
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            Message msg = inbox.getMessage(inbox.getMessageCount()); // get newest message
            out.println("Message headers are: ==========="); // includes content-type, might get charset.
            out.println("<p></p>");

            Enumeration en = msg.getAllHeaders();
            while (en.hasMoreElements()) {
                Header h = (Header) en.nextElement();
                out.println(h.getName() + " " + h.getValue());
                out.println("<p></p>");
            }

            out.println(" end headers =================");
            out.println("<p></p>");

        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}
