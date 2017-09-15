/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.logging;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;


public class LoggerFactory{
    
    private static boolean override;
    private static ClassLoader classloader;
    private static String classname;
    private static LoggerFactory helper;
    public static final String MESSAGES = "com.ibm.ws.webcontainer.resources.Messages";

    private LoggerFactory(){

    }

    public static synchronized LoggerFactory getInstance(){
        if(helper != null){
            return helper;
        }

        if(override){
            try {
                helper = (LoggerFactory)Class.forName (classname,false, getClassLoader()).newInstance();
            }catch(ClassNotFoundException e) {
                helper = new LoggerFactory();
            } catch(IllegalAccessException e) {
                helper = new LoggerFactory();
            } catch(InstantiationException e) {
                helper = new LoggerFactory();
            }
            return helper;
        }
        else{
            return helper = new LoggerFactory();
        }
    }       

        
    public Logger getLogger(final String name, final String bundle) {

    	// We used to return a WebContainerLogger, but we now don't want anything except 
        // normal Logger behaviour, since the logging code handles printing out 
        // exceptions, so just return the same logger as the WebContainerLogger would wrap
        
         return AccessController.doPrivileged(
                new PrivilegedAction<Logger>() {
                    public Logger run() {
                                return Logger.getLogger(name, bundle);
                    }
            });
    }

    public Logger getLogger(String name) {
    	return getLogger(name, MESSAGES);
    }
    

    public static ClassLoader getClassLoader() {
        return classloader==null ? LoggerHelper.class.getClassLoader() : classloader ;
    }
    public static void setClassloader(ClassLoader loader) {
        classloader = loader;
    }
    
    public static void setClassname(String name) {
        classname = name;
    }
    
    public static void setOverride(boolean value) {
        override = value;
    }
        
}

