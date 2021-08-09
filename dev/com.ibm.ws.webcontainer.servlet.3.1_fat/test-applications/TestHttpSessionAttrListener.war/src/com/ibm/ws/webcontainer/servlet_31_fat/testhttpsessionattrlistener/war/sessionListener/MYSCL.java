/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.testhttpsessionattrlistener.war.sessionListener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;

/**
 * Application Lifecycle Listener implementation class MYSCL
 *
 */
@WebListener
public class MYSCL implements ServletContextListener {

    /**
     * Default constructor.
     */
    public MYSCL() {
        System.out.println("ServletContextListener : MYSCL construct");
    }

    /**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {

//		 * @test_Strategy: In a ServletContextListener, call:
//	     *                  - ServletContext.addListener(TCKTestListener.class)
//	     *                  - ervletContext.addListener("TCKTestListener")
//	     *                  - ServletContext.createListener(TCKTestListener.class)

        System.out.println("MYSCL initialized");
        // Get the ServletContext
        ServletContext context = sce.getServletContext();

        // Add a Listener class
        context.addListener(MYHSAL.class);
        System.out.println("MYSCL Finished adding a HttpSessionAttributeListener: MYHSAL class");

        // Add a Listener2 class via String API
        // fully qualified String name
        context.addListener("com.ibm.ws.webcontainer.servlet_31_fat.testhttpsessionattrlistener.war.sessionListener.MYHSAL2");
        System.out.println("MYSCL Finished adding a HttpSessionAttributeListener2: MYHSAL2 String");

        try {
            context.createListener(MYHSAL3.class);
        } catch (ServletException e) {
            e.printStackTrace();
        }
        System.out.println("MYSCL Created a HttpSessionAttributeListener3: MYHSAL3 class");

        // Add a Listener3
        context.addListener(MYHSAL3.class);
        System.out.println("MYSCL Finished adding a HttpSessionAttributeListener3: MYHSAL3 class");

        // Add session listener
        context.addListener(MYSessionListener.class);

        System.out.println("MYSCL Finished adding a HttpSessionListener : MYSessionListener class");
    }

    /**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        System.out.println("ServletContextListener : MYSCL destoryed");
    }

}
