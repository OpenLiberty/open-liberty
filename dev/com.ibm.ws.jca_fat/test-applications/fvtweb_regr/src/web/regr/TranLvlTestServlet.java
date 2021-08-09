/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.regr;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class TranLvlTestServlet extends HttpServlet {

    private final String servletName = this.getClass().getSimpleName();
    private Context ctx;

    public TranLvlTestServlet() {
        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            e.printStackTrace(System.out);
        }
    }

    /*
     * Resource adapter DD set transaction support level to LocalTransaction.
     * MCF.getTransactionSupport() returns NoTransaction.
     * Transaction support level should be changed to NoTransaction.
     */
    public void testTranLocNo(HttpServletRequest request,
                              HttpServletResponse response) throws Throwable {
        ctx.lookup("eis/tranlvl_Loc_No");
    }

    /*
     * Resource adapter DD set transaction support level to LocalTransaction.
     * MCF.getTransactionSupport() returns LocalTransaction.
     * Transaction support level should stay LocalTransaction.
     */
    public void testTranLocLoc(HttpServletRequest request,
                               HttpServletResponse response) throws Throwable {
        ctx.lookup("eis/tranlvl_Loc_Loc");
    }

    /*
     * Resource adapter DD set transaction support level to LocalTransaction.
     * MCF.getTransactionSupport() returns XATransaction.
     * This will throw an error in Liberty.
     */
    public void testTranLocXA(HttpServletRequest request,
                              HttpServletResponse response) throws Throwable {
        try {
            ctx.lookup("eis/tranlvl_Loc_XA");
        } catch (NamingException ne) {
        }
    }

    /*
     * Resource adapter DD set transaction support level to NoTransaction.
     * MCF.getTransactionSupport() returns NoTransaction.
     * Transaction support level should stay NoTransaction.
     */
    public void testTranNoNo(HttpServletRequest request,
                             HttpServletResponse response) throws Throwable {
        ctx.lookup("eis/tranlvl_No_No");
    }

    /*
     * Resource adapter DD set transaction support level to NoTransaction.
     * MCF.getTransactionSupport() returns LocalTransaction.
     * This will throw an error in Liberty
     */
    public void testTranNoLoc(HttpServletRequest request,
                              HttpServletResponse response) throws Throwable {
        try {
            ctx.lookup("eis/tranlvl_No_Loc");
        } catch (NamingException ne) {
        }

    }

    /*
     * Resource adapter DD set transaction support level to NoTransaction.
     * MCF.getTransactionSupport() returns XATransaction.
     * This will throw an error in Liberty
     */
    public void testTranNoXA(HttpServletRequest request,
                             HttpServletResponse response) throws Throwable {
        try {
            ctx.lookup("eis/tranlvl_No_XA");
        } catch (NamingException ne) {
        }

    }

    /*
     * Resource adapter DD set transaction support level to XATransaction.
     * MCF.getTransactionSupport() returns NoTransaction.
     * Transaction support level should be changed to NoTransaction.
     */
    public void testTranXANo(HttpServletRequest request,
                             HttpServletResponse response) throws Throwable {
        ctx.lookup("eis/tranlvl_XA_No");
    }

    /*
     * Resource adapter DD set transaction support level to XATransaction.
     * MCF.getTransactionSupport() returns LocalTransaction.
     * Transaction support level should be changed to LocalTransaction.
     */
    public void testTranXALoc(HttpServletRequest request,
                              HttpServletResponse response) throws Throwable {
        ctx.lookup("eis/tranlvl_XA_Loc");
    }

    /*
     * Resource adapter DD set transaction support level to XATransaction.
     * MCF.getTransactionSupport() returns XATransaction.
     * Transaction support level should stay XATransaction.
     */
    public void testTranXAXA(HttpServletRequest request,
                             HttpServletResponse response) throws Throwable {
        ctx.lookup("eis/tranlvl_XA_XA");
    }

    /*
     * Resource adapter DD set transaction support level to XATransaction.
     * MCF does not implement TransactionSupport
     * Transaction support level should stay XATransaction.
     */
    public void testTranSupportNotImplemented(HttpServletRequest request,
                                              HttpServletResponse response) throws Throwable {
        ctx.lookup("eis/tranlvl_TranSupportNotImplemented");
    }

    /**
     * Message written to servlet to indicate that is has been successfully
     * invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println(" ---> " + servletName + " is starting " + test + "<br>");
        System.out.println(" ---> " + servletName + " is starting test: " + test);

        try {
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
            System.out.println(" <--- " + test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
            x.printStackTrace();
            out.println(" <--- " + test + " FAILED");
            System.out.println(" <--- " + test + " FAILED");
        } finally {
            out.flush();
            out.close();
        }
    }

}