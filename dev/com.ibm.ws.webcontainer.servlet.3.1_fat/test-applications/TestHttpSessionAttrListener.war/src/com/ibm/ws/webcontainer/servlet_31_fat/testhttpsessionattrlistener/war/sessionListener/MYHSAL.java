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
package com.ibm.ws.webcontainer.servlet_31_fat.testhttpsessionattrlistener.war.sessionListener;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

/**
 * Application Lifecycle Listener implementation class MYHSAL
 *
 */
public class MYHSAL implements HttpSessionAttributeListener {

    /**
     * Default constructor.
     */
    public MYHSAL() {
        System.out.println("MYHSAL construct");
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent eve) {
        System.out.println("MYHSAL called for the attributeAdded in session [ " + eve.getSession() + " ]");
        CalculateListenerInvoke.addNumber();
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent eve) {
        Thread.dumpStack();
        System.out.println("MYHSAL called for the attributeRemoved in session [ " + eve.getSession() + " ]");
        CalculateListenerInvoke.subtractNumber();
        System.out.println("MYSessionAttributeListener addAttrValueOnRemoved");
        CalculateListenerInvoke.addAttrValueOnDestroy(eve.getName());
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent eve) {
        System.out.println("MYHSAL called for the attributeReplaced in session [ " + eve.getSession() + " ]");

    }

}
