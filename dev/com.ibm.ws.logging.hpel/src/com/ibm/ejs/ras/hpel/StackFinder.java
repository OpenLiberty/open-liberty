/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * %Z% %I% %W% %G% %U% [%H% %T%]
 */
package com.ibm.ejs.ras.hpel;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class StackFinder extends SecurityManager {
    private static StackFinder finder = null;
//    private TreeSet<String> hpelClasses = new TreeSet<String>();

    @SuppressWarnings("unchecked")
    public static StackFinder getInstance() {
        if (finder == null) {
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    finder = new StackFinder();
                    return null;
                }
            });
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.StackFinder");
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.Messages");
//            finder.hpelClasses.add("java.util.logging.Logger");
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.TraceNLSResolver");
//
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.messages.HpelLogFormatter");
//
//            finder.hpelClasses.add("com.ibm.ws.logging.hpel.WsLogger");
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.messages.handlers.LogRecordTextHandler");
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.messages.handlers.LogRecordTextHandler$2");
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.messages.handlers.LogRecordTextHandler$SerializationBuffer");
//
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.messages.handlers.LogRecordHandler$SerializationBuffer");
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.messages.handlers.LogRecordHandler$2");
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.messages.handlers.LogRecordHandler");
//            
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.messages.ras.impl.BinaryLogRecordSerializerImpl");
//            finder.hpelClasses.add("com.ibm.ejs.ras.hpel.messages.ras.impl.BinaryLogRecordSerializerImpl$3");
        }


        return finder;
    }

    @SuppressWarnings("unchecked")
    public Class<Object> getCaller()
    {
        Class<Object> aClass = null;
        
        // Walk the stack backwards to find the calling class: don't 
        // want to use Class.forName, because we want the class as loaded
        // by it's original classloader
        Class<Object> stack[] = this.getClassContext();
//        for ( Class<Object> bClass : stack )
//        {
//            String bName = bClass.getName();
////            System.out.println(bName + " : " + hpelClasses.contains(bName));
//            // Find the first class in the stack that _isn't_ Tr or Tr.Finder
//            if ( ! hpelClasses.contains(bName)) {
//                aClass = bClass;
//                break;
//            }
//        }
        
        // new way we need to get class one over 
        // 1. java.util.Logger
        // 2. com.ibm.ws.logging.hpel.WsLogger
        // 3. com.ibm.ejs.Tr
        // there can be multiple of those above
        for (int i = 0; i < stack.length;i++) {
            Class bClass = stack[i];
            String bName = bClass.getName();
            if ("com.ibm.ws.logging.WsLogger".equals(bName) || "com.ibm.ws.logging.hpel.WsLogger".equals(bName) || "com.ibm.ejs.Tr".equals(bName) || "com.ibm.ejs.ras.Tr".equals(bName)) {
                for (int j=i+1; j < stack.length; j++) {
                    if (!bName.equals(stack[j].getName())) {
                        aClass = stack[j];
                        return aClass;                        
                    }
                }
            }
        }
        //TraceNLS can be called directly, if above did not find a class look for TraceNLS
        if (null == aClass) {
            for (int i = 0; i < stack.length;i++) {
                Class bClass = stack[i];
                String bName = bClass.getName();
                if ("com.ibm.ejs.ras.TraceNLS".equals(bName) ) {
                    for (int j=i+1; j < stack.length; j++) {
                        if (!bName.equals(stack[j].getName())) {
                            aClass = stack[j];
                            return aClass;                        
                        }
                    }
                }
            }            
        }
        
        return aClass;
    }

    @SuppressWarnings("unchecked")
    public boolean callstackContains(String fragment) {
        // Walk the stack backwards to find the calling class: don't
        // want to use Class.forName, because we want the class as loaded
        // by it's original classloader
        Class<Object> stack[] = this.getClassContext();
        for (Class<?> bClass : stack) {
            // See if any class in the stack contains the following string
            if (bClass.getName().contains(fragment))
                return true;
        }

        return false;
    }
}
