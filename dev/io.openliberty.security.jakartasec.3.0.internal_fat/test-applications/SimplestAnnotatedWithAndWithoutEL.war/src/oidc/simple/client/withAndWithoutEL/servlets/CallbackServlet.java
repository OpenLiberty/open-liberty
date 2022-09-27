/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.simple.client.withAndWithoutEL.servlets;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/Callback")
public class CallbackServlet extends HttpServlet {

    private static final long serialVersionUID = -417476984908088827L;

<<<<<<< HEAD:dev/io.openliberty.security.jakartasec.3.0.internal_fat/test-applications/SimplestAnnotatedWithAndWithoutEL.war/src/oidc/simple/client/withAndWithoutEL/servlets/CallbackServlet.java
=======
    @Inject
    private OpenIdContext context;

>>>>>>> origin/integration:dev/io.openliberty.security.jakartasec.3.0.internal_fat/test-applications/SimplestAnnotated.war/src/oidc/servlets/CallbackServlet.java
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
<<<<<<< HEAD:dev/io.openliberty.security.jakartasec.3.0.internal_fat/test-applications/SimplestAnnotatedWithAndWithoutEL.war/src/oidc/simple/client/withAndWithoutEL/servlets/CallbackServlet.java
        System.out.println("SimplestAnnotatedWithAndWithoutEL: got here");
        sos.println("SimplestAnnotatedWithAndWithoutEL: got here");
=======
        System.out.println("got here");
        System.out.println("OpenIdContext is " + context);
        sos.println("got here");
>>>>>>> origin/integration:dev/io.openliberty.security.jakartasec.3.0.internal_fat/test-applications/SimplestAnnotated.war/src/oidc/servlets/CallbackServlet.java
    }

}