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
package com.ibm.ws.cdi.test.managedbean;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("")
public class ManagedBeanServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1599035198651566335L;
    @Resource
    MyManagedBean myManagedBean;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter pw = response.getWriter();
        List<String> allMsg = new ArrayList<String>();
        allMsg.add("Begin output");
        allMsg.addAll(CounterUtil.getMsgList());
        allMsg.addAll(myManagedBean.getMsgList());
        for (String msg : allMsg) {
            pw.append(msg + " ");
        }

        pw.flush();
    }
}
