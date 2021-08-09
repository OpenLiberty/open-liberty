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

package web.postparams;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 */

public class AddressBookServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    /**
      */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Send get request to processRequest to do the work.
     *
     * @param req request object.
     * @param res reponse object.
     * @exception javax.servlet.ServletException This exception is thrown to
     *                indicate a servlet problem.
     * @exception java.io.IOException Signals that an I/O exception of some
     *                sort has occurred.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    /**
     * Send post request to processRequest to do the work.
     *
     * @param req request object.
     * @param res reponse object.
     * @exception javax.servlet.ServletException This exception is thrown to
     *                indicate a servlet problem.
     * @exception java.io.IOException Signals that an I/O exception of some
     *                sort has occurred.
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest(req, res);
    }

    /**
     * Determine the type of address book operation requested and call the
     * appropriate helper routine.
     *
     * @param req request object.
     * @param res reponse object.
     * @exception javax.servlet.ServletException This exception is thrown to
     *                indicate a servlet problem.
     * @exception java.io.IOException Signals that an I/O exception of some
     *                sort has occurred.
     */
    void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String op = req.getParameter("operation");
        PrintWriter out = res.getWriter();
        out.println("AddressBookServlet ");
        if (op != null && op.equals("Add")) {
            // Get details for new Address Book Entry.
            String fName = req.getParameter("firstName");
            String lName = req.getParameter("lastName");
            String eMail = req.getParameter("eMailAddr");
            String phone = req.getParameter("phoneNum");
            out.println("Hi " + req.getRemoteUser());
            out.println("firstName : " + fName);
            out.println("lastName : " + lName);
            out.println("eMailAddr : " + eMail);
            out.println("phoneNum : " + phone);
        } else {
            // do nothing.
            out.println("Hi " + req.getRemoteUser());
            out.println(", opeation : " + op);
        }
        out.flush();
        out.close();
    }
}
