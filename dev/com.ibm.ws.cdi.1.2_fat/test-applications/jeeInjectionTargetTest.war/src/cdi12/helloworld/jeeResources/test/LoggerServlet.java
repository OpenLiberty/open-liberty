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

package cdi12.helloworld.jeeResources.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/log")
public class LoggerServlet extends HttpServlet {

    @Inject
    private BeanManager beanManager;

    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        JEEResourceExtension extension = beanManager.getExtension(JEEResourceExtension.class);

        PrintWriter pw = response.getWriter();
        Iterator<String> itr = extension.logger.iterator();
        while (itr.hasNext()) {
            pw.write(itr.next());
            if (itr.hasNext()) {
                pw.write(",");
            }
        }
        pw.flush();
        pw.close();
    }

}
