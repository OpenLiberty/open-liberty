/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.servlet31.writeListener;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet(urlPatterns = "/TestAsyncFalseWriteServlet")
public class TestAsyncFalseWriteServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = -1958686114371622945L;

    private static final Logger LOG = Logger.getLogger(TestAsyncFalseWriteServlet.class.getName());
    private final LinkedBlockingQueue<String> q = new LinkedBlockingQueue<String>();
    ServletOutputStream out = null;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
                    throws IOException, ServletException {

        out = res.getOutputStream();
        String testToCall = req.getHeader("TestToCall").toString();
        LOG.info("TestToCall :  " + testToCall);
        if (testToCall.equals("Test_ISE_SetWL_NonAsyncServlet")) {
            if (!q.isEmpty()) {
                q.poll();
            }
            WriteListener writeListener = new TestAsyncWriteListener(out, q, null, req, res, "blah");
            try {
                out.setWriteListener(writeListener);
            } catch (IllegalStateException e) {
                LOG.info("test_ISE_SetWL_NonAsyncServlet: caught IllegalStateException ,[" + e.toString() + "]");
                out.print(e.toString());
                //e.printStackTrace();
            }
        }
        else {
            LOG.info("TestAsyncFalseWriteServlet: Unknown Test");
        }

    }

}
