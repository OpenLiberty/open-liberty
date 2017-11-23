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
package cdi12.helloworld.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    @Inject
    HelloBean hello;

    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write(hello.hello());
        try {
            pw.write(hello.getBeanMangerViaJNDI());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            BeanManager beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
            Set<Bean<?>> beans = beanManager.getBeans(Object.class);
            if (beans.size() > 0) {
                pw.write(" JNDI BeanManager from Servlet PASSED!");
            } else {
                pw.write(" JNDI BeanManager from Servlet  FAILED!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
        pw.close();

    }
}
