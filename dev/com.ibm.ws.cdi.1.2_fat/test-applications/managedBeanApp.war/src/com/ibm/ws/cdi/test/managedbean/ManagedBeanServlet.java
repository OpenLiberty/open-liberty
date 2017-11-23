/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
