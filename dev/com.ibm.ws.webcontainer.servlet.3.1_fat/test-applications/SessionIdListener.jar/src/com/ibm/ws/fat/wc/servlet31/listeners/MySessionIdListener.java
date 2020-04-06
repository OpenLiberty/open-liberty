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
package com.ibm.ws.fat.wc.servlet31.listeners;

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