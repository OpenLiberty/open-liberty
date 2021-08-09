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
package testservlet40.jar.servlets;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

//since our HandlesTypes annotation contains the class Servlet, then all classes within the
//application that implement Servlet will be sent to the onStartup method below
@HandlesTypes(javax.servlet.Servlet.class)
public class ServletContainerInitializerImpl implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> setOfClassesInterestedIn, ServletContext context) throws ServletException {
        System.out.println("--- TestServlet40 CONTAINER INITIALIZER! ---");
        //going to add a context attribute to show the set of classes that were passed in
        if (setOfClassesInterestedIn != null) {
            context.setAttribute("SciTestMessage", "  SCI says Hi!");
            // context.setRequestCharacterEncoding("KSC5601");
            // context.setResponseCharacterEncoding("KSC5601");
            //context.setResponseCharacterEncoding("PMDINH"); //causes new message CWWWC0401E: Failed to set response character encoding
            context.setAttribute("SET_OF_SERVLETS_IN_APP", setOfClassesInterestedIn);
        } else {
            context.setAttribute("SET_OF_SERVLETS_IN_APP", "null");
        }
        System.out.println("--- TestServlet40 CONTAINER INITIALIZER! --- EXIT");
    }
}
