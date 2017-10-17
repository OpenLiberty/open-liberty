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
package com.ibm.ws.cdi12.test.dynamicBeans.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean1;
import com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean2;

@WebServlet("/")
public class DynamicBeansServlet extends HttpServlet {

    private final DynamicBean1 bean1;
    private final DynamicBean2 bean2;

    @Inject
    public DynamicBeansServlet(DynamicBean1 bean1, DynamicBean2 bean2) {
        this.bean1 = bean1;
        this.bean2 = bean2;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();

        out.println("DynamicBean1 count: " + bean1.increment() + ", " + bean1.increment());
        out.println("DynamicBean2 count: " + bean2.increment() + ", " + bean2.increment());
    }
}
