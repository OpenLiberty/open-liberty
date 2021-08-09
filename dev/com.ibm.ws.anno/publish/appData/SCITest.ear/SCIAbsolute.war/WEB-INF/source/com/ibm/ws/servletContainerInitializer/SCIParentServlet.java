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
package com.ibm.ws.servletContainerInitializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class SCIParentServlet extends HttpServlet {

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doSuperPost(String servletName, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream os = response.getOutputStream();
        String s = servletName +" successful";
        System.out.println(s);
        os.println(s);
        ServletContext ctx = request.getServletContext();
        if ("executed".equals(ctx.getAttribute("SCIImpl"))) {
            os.println("SCIImpl executed");
        }
        if ("executed".equals(ctx.getAttribute("SCIImpl2"))) {
            os.println("SCIImpl2 executed");
        }
        if ("executed".equals(ctx.getAttribute("SCIImpl2b"))) {
            os.println("SCIImpl2b executed");
        }
        if ("executed".equals(ctx.getAttribute("SCIImpl3"))) {
            os.println("SCIImpl3 executed");
        }
        if ("executed".equals(ctx.getAttribute("ListenerImplContextInitialized"))) {
            os.println("ListenerImplContextInitialized executed");
        }
        if ("executed".equals(ctx.getAttribute("SCIImpl4"))) {
            os.println("SCIImpl4 executed");
        }
        if ("executed".equals(ctx.getAttribute("SCIImplRelative1"))) {
            os.println("SCIImplRelative1 executed");
        }
        if ("executed".equals(ctx.getAttribute("SCIImplRelative2"))) {
            os.println("SCIImplRelative2 executed");
        }
        if ("executed".equals(ctx.getAttribute("SCIImplRelative3"))) {
            os.println("SCIImplRelative3 executed");
        }
        if ("true".equals(ctx.getAttribute("UnsupportedOperationExceptionThrown"))) {
            os.println("UnsupportedOperationExceptionThrown is true (from Listener)");
        }
        HashMap<String, Set<Class<?>>> classesSetHashMap = (HashMap<String, Set<Class<?>>>) ctx.getAttribute("classesSetHashMap");
        if (classesSetHashMap!=null) {
            Set<String> keySet=classesSetHashMap.keySet();
            String[] keys = convertToStringArray(keySet);
            for (String key:keys) {
                Set<Class<?>> classesSet = classesSetHashMap.get(key);
                //do I need to check if classesSet is null
                String[] classArray = convertClassesToStringArray(classesSet);
                for (String className:classArray) {
                    os.println("Class "+className+" was sent to onStartup method of " + key);
                }   
            }
        }
    }
    
    private String[] convertToStringArray(Set<String> stringSet) {
        String[] s = stringSet.toArray(new String[0]);//new String[stringSet.size()];
        //Object[] o = stringSet.toArray();
        //s = (String[])stringSet.toArray();
        Arrays.sort(s);
        return s;
    }
    
    private String[] convertClassesToStringArray(Set<Class<?>> classSet) {
        String[] classArray = new String[classSet.size()];
        int i=0;
        for (Iterator<Class<?>> it = classSet.iterator();it.hasNext();) {
            classArray[i++] = ((Class<?>)it.next()).getName();
        }
        Arrays.sort(classArray);
        return classArray;
    }
}
