/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.util;

import javax.faces.context.ExternalContext;
import javax.servlet.ServletContext;

import org.apache.myfaces.shared.util.ExternalContextUtils;

/**
 * Utilities for determining the current container and for the unified
 * expression language.
 * 
 */
public class ContainerUtils
{
    /**
     * Used for determining whether Myfaces is running on Google App Engine.
     */
    private static final String GAE_SERVER_INFO_BEGINNING = "Google App Engine";

    /**
     * Determines whether we're running in a Servlet 2.5/JSP 2.1 environment.
     * 
     * @return <code>true</code> if we're running in a JSP 2.1 environment,
     *         <code>false</code> otherwise
     */
    public static boolean isJsp21(ServletContext context)
    {
        //if running on GAE, treat like it is JSP 2.0
        if(isRunningOnGoogleAppEngine(context))
        {
            return false;
        }
        
        try 
        {
            // simply check if the class JspApplicationContext is available
            Class.forName("javax.servlet.jsp.JspApplicationContext");
            return true;
        } 
        catch (ClassNotFoundException ex) 
        {
            // expected exception in a JSP 2.0 (or less) environment
        }
        
        return false;
    }
    
    /**
     * Return true if the specified string contains an EL expression.
     * 
     * <p>
     * <strong>NOTICE</strong> This method is just a copy of
     * {@link javax.faces.webapp.UIComponentTag#isValueReference(String)}, but it's required
     * because the class UIComponentTag depends on a JSP 2.1 container 
     * (for example, it indirectly implements the interface JspIdConsumer)
     * and therefore internal classes shouldn't access this class. That's
     * also the reason why this method is inside the class ContainerUtils,
     * because it allows MyFaces to be independent of a JSP 2.1 container.
     * </p>
     */
    public static boolean isValueReference(String value) 
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }

        int start = value.indexOf("#{");
        if (start < 0)
        {
            return false;
        }

        int end = value.lastIndexOf('}');
        return (end >=0 && start < end);
    }
    
private static Boolean runningOnGoogleAppEngine = null;
    
    /**Returns true if running on Google App Engine (both production and development environment).
     * <p>If this method returns true, then
     * <ul>
     * <li>MyFaces is initialized as in JSP 2.0 or less environment.</li>
     * <li>Last modification check of faces config is not done during update.</li>
     * </ul>
     */
    public static boolean isRunningOnGoogleAppEngine(
            ServletContext servletContext)
    {
        if (runningOnGoogleAppEngine != null)
        {
            return runningOnGoogleAppEngine.booleanValue();
        }
        else
        {
            return isServerGoogleAppEngine(servletContext.getServerInfo());
        }
    }

    /**
     * @see ContainerUtils#isRunningOnGoogleAppEngine(ServletContext)
     */
    public static boolean isRunningOnGoogleAppEngine(
            ExternalContext externalContext)
    {

        if (runningOnGoogleAppEngine != null)
        {
            return runningOnGoogleAppEngine.booleanValue();
        }
        else
        {
            String serverInfo = ExternalContextUtils.getServerInfo(externalContext);
            
            return isServerGoogleAppEngine(serverInfo);
        }
    }

    private static boolean isServerGoogleAppEngine(String serverInfo)
    {
        //for GAE, server info can be "Google App Engine/x.x.x" or "Google App Engine Development/x.x.x" 
        if (serverInfo != null && serverInfo.startsWith(GAE_SERVER_INFO_BEGINNING))
        {
            runningOnGoogleAppEngine = Boolean.TRUE;
        }
        else
        {
            runningOnGoogleAppEngine = Boolean.FALSE;
        }

        return runningOnGoogleAppEngine;
    }
}
