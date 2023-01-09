/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.sessionidlistener.jar.listeners;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;

/**
*
*/
public class MySessionIdListener implements HttpSessionIdListener {

    public static final String attributeName = "changeCount";

    @Override
    public void sessionIdChanged(HttpSessionEvent event, String oldSessionId) {
        HttpSession sess = event.getSession();
        Integer changeCount = (Integer) sess.getAttribute(attributeName);
        if (changeCount == null) {
            sess.setAttribute(attributeName, 1);
        } else {
            sess.setAttribute(attributeName, changeCount.intValue() + 1);
        }
        System.out.println("sessionIdChanged method called on MySessionIdListener");
    }
}