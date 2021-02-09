/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package helloworldApp.web;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/hello")
public class HelloServlet extends FATServlet {

    @Inject
    HelloBeanCDI20 hello;

    private static final long serialVersionUID = 8549700799591343964L;

    @Test
    @Mode(TestMode.LITE)
    public void testHelloWorldBeanManagerLookup() throws Exception {

        System.out.println(hello.greeting());

        try {
            assertTrue("JNDI BeanManager from Bean FAILED - " + hello.getBeanMangerViaJNDI(), hello.getBeanMangerViaJNDI() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            BeanManager beanManager = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
            Set<Bean<?>> beans = beanManager.getBeans(Object.class);
            assertTrue("JNDI BeanManager from Servlet  FAILED - " + beans.size(), beans.size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
