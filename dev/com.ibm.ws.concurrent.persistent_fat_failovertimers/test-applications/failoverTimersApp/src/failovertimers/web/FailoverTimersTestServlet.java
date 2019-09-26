/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package failovertimers.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/FailoverTimersTestServlet")
public class FailoverTimersTestServlet extends FATServlet {
    private static final long POLL_INTERVAL_MS = 500;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource
    private UserTransaction tx;

    /**
     * TODO write a useful test now that the test bucket is created and running cleanly
     */
    public void test1(HttpServletRequest request, HttpServletResponse response) throws Exception {
        System.out.println("test1");
    }
}
