package cdi12.helloworld.extension.test;

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

/**
 *
 */

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello")
public class HelloWorldExtensionTestServlet extends HttpServlet {

    @Inject
    HelloWorldExtensionBean hello;
    private static EventMetadata beanStartMetaData;

    private static EventMetadata beanStopMetaData;

    private static final long serialVersionUID = 8549700799591343964L;

    public static void onStart(@Observes @Initialized(RequestScoped.class) Object e, EventMetadata em) {

        if (beanStartMetaData == null) {

            System.out.println("Initialize Event request scope is happening");

            beanStartMetaData = em;

        }

    }

    public static void onStop(@Observes @Destroyed(RequestScoped.class) Object e, EventMetadata em) {

        if (beanStopMetaData == null) {

            System.out.println("Stop Event request scope is happening");

            beanStopMetaData = em;

        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write(hello.hello());
        pw.write(beanStartMetaData.getQualifiers().toString());
        pw.flush();
        pw.close();
    }

}
