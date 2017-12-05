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

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ctorInjection")
public class JEEResourceTestServletCtorInjection extends HttpServlet {

    private static final long serialVersionUID = 8549700799591343964L;

    private final HelloWorldExtensionBean2 hello;

    @Inject
    public JEEResourceTestServletCtorInjection(HelloWorldExtensionBean2 bean2) {
        super();
        hello = bean2;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write(hello.hello());
        pw.flush();
        pw.close();
    }

}
