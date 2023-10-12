/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package reactiveapp.web;

import javax.naming.InitialContext;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/ConcurrentReactiveServlet")
public class ConcurrentReactiveServlet extends FATServlet {

    @Resource(name = "java:comp/env/concurrent/executorRef")
    private ManagedExecutorService executor;

    /*
     * Test that context is available at different points in a flow (publisher, subscriber)
     * when using a ManagerExecutor.
     */
    @Test
    public void basicFlowTest() throws Exception {

        new InitialContext().lookup("java:comp/env/entry1");

        ThreadPublisher publisher = new ThreadPublisher(executor);
        publisher.subscribe(new ThreadSubscriber());

        publisher.close();
    }

}
