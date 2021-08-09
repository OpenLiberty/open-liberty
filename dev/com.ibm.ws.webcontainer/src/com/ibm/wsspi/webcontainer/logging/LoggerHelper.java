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
package com.ibm.wsspi.webcontainer.logging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogRecord;


public class LoggerHelper extends Logger {
    
    private static boolean override = false;
    private static ClassLoader classloader = LoggerHelper.class.getClassLoader();
    private static String classname = LoggerHelper.class.getName();
        
    protected LoggerHelper(String arg0, String arg1) {
        super(arg0, arg1);
    }

    public static Logger getNewLogger(String name, String bundle) {
        if(override) {
            try {
                Class args[] = new Class[2];
                Class clazz = classloader.loadClass(classname);
                args[0] = String.class;
                args[1] = String.class;
                Method m = clazz.getMethod("getNewLogger", args);
                Object arg[] = new Object[2];
                arg[0] = name;
                arg[1] = bundle;
                return (Logger)m.invoke(null, arg);
            
            //return clazz.getLogger(name, bundle);
            } catch(ClassNotFoundException e) {
                return Logger.getLogger(name, bundle);
            } catch(NoSuchMethodException e) {
                return Logger.getLogger(name, bundle);
            } catch(IllegalAccessException e) {
                return Logger.getLogger(name, bundle);
            } catch(InvocationTargetException e) {
                return Logger.getLogger(name, bundle);
            }
        }
        else {
            return Logger.getLogger(name, bundle);
        }
    }
    
    public static ClassLoader getClassloader() {
        return classloader;
    }
    public static void setClassloader(ClassLoader loader) {
        classloader = loader;
    }
    public static String getClassname() {
        return classname;
    }
    public static void setClassname(String name) {
        classname = name;
    }
    public static void setOverride(boolean value) {
        override = value;
    }

    //596191:: PK97815
    /**
     * @param l is used for Logger
     * @param lev is used for FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE
     * @param methodClassName is the class name logger is used
     * @param methodName is the method name logger is used
     * @param message is what info needs to be logged
     * @param p any parameter needs to be sent
     * @param t is the exception
     */
    public static void logParamsAndException(Logger l, Level lev, String methodClassName, String methodName, String message, Object[] p, Throwable t) {    	
    	LogRecord logRecord = new LogRecord(lev, message); 
    	if(logRecord != null) {
	    	logRecord.setLoggerName(l.getName());      
	    	logRecord.setResourceBundle(l.getResourceBundle());     
	    	logRecord.setResourceBundleName(l.getResourceBundleName());      
	    	logRecord.setSourceClassName(methodClassName);       
	    	logRecord.setSourceMethodName(methodName);       
	    	logRecord.setParameters(p);      
	    	logRecord.setThrown(t);      
	    	l.log(logRecord);
    	}
    }
}
