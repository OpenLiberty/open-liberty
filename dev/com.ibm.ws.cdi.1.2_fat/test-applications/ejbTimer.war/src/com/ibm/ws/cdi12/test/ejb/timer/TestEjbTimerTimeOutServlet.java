/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.ejb.timer;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.test.ejb.timer.view.SessionBeanLocal;

/**
 *
 */
@WebServlet("/timerTimeOut")
public class TestEjbTimerTimeOutServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    @EJB
    SessionBeanLocal bean;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        out.println(bean.getValue());
        bean.setUpTimer();
    }
}
