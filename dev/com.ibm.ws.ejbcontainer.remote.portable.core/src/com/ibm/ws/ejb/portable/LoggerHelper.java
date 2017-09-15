/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejb.portable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to help integration between java.util.logging and Tr logging.
 * 
 * <p>To enable ras.lite logging:
 * <pre>
 * -Djava.util.logging.manager=com.ibm.ws.bootstrap.WsLogManager
 * -Dcom.ibm.ejs.ras.lite.traceSpecification=EJBContainer=all
 * -Dcom.ibm.ejs.ras.lite.traceFileName=/path/to/output.txt
 * </pre>
 * 
 * <p><b>NOTE:</b> This class (and Logger) can only safely be used by classes
 * that will only be used on JDK 1.4 or later (when Logger was introduced).
 */
public class LoggerHelper
{
    private static final String CLASS_NAME = LoggerHelper.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static volatile boolean svCheckedForWAS;
    private static Method svAddLoggerToGroupMethod;

    /**
     * Gets a new logger and registers it with the specified Tr logging group.
     * 
     * @param className the requesting class name
     * @param groupName the Tr logging group name
     * @return the logger
     */
    public static Logger getLogger(String className, String groupName)
    {
        Logger logger = Logger.getLogger(className);

        if (!svCheckedForWAS)
        {
            try
            {
                Class loggerHelperClass = Class.forName("com.ibm.ws.logging.LoggerHelper");
                svAddLoggerToGroupMethod = loggerHelperClass.getMethod("addLoggerToGroup",
                                                                       new Class[] { Logger.class, String.class });
            } catch (Exception ex)
            {
                svLogger.logp(Level.FINE, CLASS_NAME, "getLogger", "failed to find addLoggerToGroup method", ex);
            }

            svCheckedForWAS = true;
        }

        if (svAddLoggerToGroupMethod != null)
        {
            try
            {
                svAddLoggerToGroupMethod.invoke(null, new Object[] { logger, groupName });
            } catch (IllegalAccessException ex)
            {
                svLogger.logp(Level.FINE, CLASS_NAME, "getLogger", "addLoggerToGroup failed", ex);
            } catch (InvocationTargetException ex)
            {
                svLogger.logp(Level.FINE, CLASS_NAME, "getLogger", "addLoggerToGroup failed", ex);
            }
        }

        return logger;
    }
}
