/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi12.beanmanagerlookup.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class BeanManagerLookupServlet extends HttpServlet {

    private static final long serialVersionUID = 8549700799591343964L;

    @Inject
    BeanManager bmI;

    @Inject
    MyBean mb;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        CDI cdi = CDI.current();
        BeanManager bm = cdi.getBeanManager();
        PrintWriter pw = response.getWriter();

        pw.append("Bean manager from CDI.current().getBeanManager: " + (bm instanceof BeanManager) + System.lineSeparator());

        Context c;
        BeanManager bmJ = null;
        try {
            c = new InitialContext();
            bmJ = (BeanManager) c.lookup("java:comp/BeanManager");
            pw.append("Bean manager from JNDI: " + (bmJ instanceof BeanManager) + System.lineSeparator());

        } catch (NamingException e) {
            pw.append("JNDI lookup failed" + System.lineSeparator());

        }

        pw.append("Bean manager from inject: " + (bmI instanceof BeanManager) + System.lineSeparator());

        Set<Bean<?>> set = bm.getBeans(MyBean.class);
        if (!set.isEmpty()) {
            pw.append("BeanManager from CDI.current().getBeanManager found a Bean." + System.lineSeparator());

        } else {
            pw.append("BeanManager from CDI.current().getBeanManager could not find a Bean" + System.lineSeparator());
        }

        set = bmI.getBeans(MyBean.class);
        if (!set.isEmpty()) {
            pw.append("BeanManager from injection found a Bean." + System.lineSeparator());

        } else {
            pw.append("BeanManager from injection could not find a Bean" + System.lineSeparator());
        }

        set = bmJ.getBeans(MyBean.class);
        if (!set.isEmpty()) {
            pw.append("BeanManager from jndi found a Bean." + System.lineSeparator());

        } else {
            pw.append("BeanManager from jndi could not find a Bean" + System.lineSeparator());
        }

        pw.flush();
        pw.close();
    }
}
