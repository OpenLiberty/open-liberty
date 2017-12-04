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
package com.ibm.ws.cdi.test.session.destroy;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class TestHttpSessionListener implements HttpSessionListener {

    @Inject
    private BeanManager beanManager;

    @Inject
    private SimpleSessionBean simpleBean;

    private static String results = "";

    public static String getResults() {
        return results;
    }

    public static void clearResults() {
        results = "";
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        results += "session created: " + checkSessionContextActive() + " ";

    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        results += "session destroyed: " + checkSessionContextActive() + " ";

    }

    private String checkSessionContextActive() throws IllegalStateException {
        try {
            if (!beanManager.getContext(SessionScoped.class).isActive()) {
                return " context is not active";
            }
            if (simpleBean == null) {
                return " simple bean is null";
            }
            simpleBean.getID();
        } catch (Exception e) {
            return " simple bean invocation failed " + e.getMessage();
        }

        return "true";
    }

}
