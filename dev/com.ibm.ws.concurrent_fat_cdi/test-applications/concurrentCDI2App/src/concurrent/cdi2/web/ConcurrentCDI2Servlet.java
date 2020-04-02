/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi2.web;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrentCDI2Servlet extends FATServlet {

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Inject
    private ConcurrentCDI2AppScopedBean appScopedBean;

    @Test
    public void testTaskScheduledByServletContainerInitializerCompletes() throws Exception {
        Future<Integer> future = appScopedBean.getServletContainerInitFuture();
        assertEquals(Integer.valueOf(1), future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    @Test
    public void testTaskScheduledByServletContextListenerCompletes() throws Exception {
        Future<Integer> future = appScopedBean.getServletContextListenerFuture();
        assertEquals(Integer.valueOf(2), future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }
}
