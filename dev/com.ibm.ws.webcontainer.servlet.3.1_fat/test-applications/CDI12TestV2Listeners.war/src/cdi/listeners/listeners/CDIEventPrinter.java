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
package cdi.listeners.listeners;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;

/**
 * Helper to generate log text for events.
 */
public class CDIEventPrinter {

    // Intercept.  The print string for sessions is too large for the listener logs.

    private static String getSourceString(Object source) {
        String sourceString;
        if (source == null) {
            sourceString = "null";
        } else if (source instanceof HttpSession) {
            HttpSession sourceSession = (HttpSession) source;
            sourceString = getPrintString(sourceSession);
        } else {
            sourceString = source.toString();
        }
        return sourceString;
    }

    private static String getPrintString(HttpSession session) {
        return session.getClass().getName() + "( " + session.getId() + " )";
    }

    // @formatter:off
    
    public static String[] getEventText(HttpSessionBindingEvent bindingEvent) {
        return new String[] {
            "Name [ " + bindingEvent.getName() + " ]",
            "  Value [ " + bindingEvent.getValue() + " ]",
            "  Source [ " + getSourceString(bindingEvent.getSource()) + " ]"
        };
    }

    public static String[] getEventText(HttpSessionEvent sessionEvent, String oldSessionId) {
        return new String[] {
            "Session [ " + getPrintString(sessionEvent.getSession()) + " ]",
            "  Source [ " + getSourceString(sessionEvent.getSource()) + " ]",
            "  Old session ID [ " + oldSessionId + " ]"
        };
    }

    public static String[] getEventText(HttpSessionEvent sessionEvent) {
        return new String[] {
            "Session [ " + getPrintString(sessionEvent.getSession()) + " ]",
            "  Source [ " + getSourceString(sessionEvent.getSource()) + " ]"
        };
    }

    public static String[] getEventText(ServletContextAttributeEvent attributeEvent) {
        return new String[] {
            "Name [ " + attributeEvent.getName() + " ]",
            "  Value [ " + attributeEvent.getValue() + " ]", 
            "  Context [ " + attributeEvent.getServletContext() + " ]",
            "  Source [ " + getSourceString(attributeEvent.getSource()) + " ]"
        };
    }

    public static String[] getEventText(ServletContextEvent contextEvent) {
        return new String[] {
            "Context [ " + contextEvent.getServletContext() + " ]",
            "  Source [ " + getSourceString(contextEvent.getSource()) + " ]"
        };
    }

    public static String[] getEventText(ServletRequestAttributeEvent attributeEvent) {
        return new String[] {
            "Name [ " + attributeEvent.getName() + " ]",
            "  Value [ " + attributeEvent.getValue() + " ]",
            "  Context [ " + attributeEvent.getServletContext() + " ]",
            "  Request [ " + attributeEvent.getServletRequest() + " ]",            
            "  Source [ " + getSourceString(attributeEvent.getSource()) + " ]"
        };
    }

    public static String[] getEventText(ServletRequestEvent requestEvent) {
        return new String[] {
            "Context [ " + requestEvent.getServletContext() + " ]",
            "  Response [ " + requestEvent.getServletRequest() + " ]",
            "  Source [ " + getSourceString(requestEvent.getSource()) + " ]"
        };
    }
    
    // @formatter:on
}
