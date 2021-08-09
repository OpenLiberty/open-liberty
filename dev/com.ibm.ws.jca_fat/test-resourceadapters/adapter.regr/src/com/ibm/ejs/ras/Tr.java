/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

public class Tr {
    public static TraceComponent register(Class<?> aClass) {
        TraceComponent tc = null;
        TraceOptions options = aClass.getAnnotation(TraceOptions.class);

        if (options == null) {
            options = aClass.getPackage().getAnnotation(TraceOptions.class);
        }
        String name = aClass.getName();
        if (options == null) {
            tc = new TraceComponent(name, aClass, (String) null, null);
        }

        return tc;
    }

    public static TraceComponent register(Class<?> aClass, String traceGroup,
                                          String nlsFile) {
        return null;
    }

    /**
     * @param classname
     * @param rasGroup
     * @param nlsFile
     * @return
     */
    public static TraceComponent register(String classname, String rasGroup,
                                          String nlsFile) {
        return null;
    }

    /**
     * @param classname
     * @param traceGroup
     * @return
     */
    public static TraceComponent register(String classname, String traceGroup) {
        return null;
    }

    /**
     * @param classname
     * @param traceGroup
     * @return
     */
    public static TraceComponent register(Class<?> aClass, String traceGroup) {
        return new TraceComponent("", aClass, traceGroup, null);
    }

    public static final void debug(TraceComponent tc, String msg) {
        System.out.println("msg :" + msg);
    }

    public static final void debug(TraceComponent tc, String msg,
                                   Object objs) {
        System.out.println("msg :" + msg);
        System.out.println("Objects :" + objs);
    }

    public static final void entry(TraceComponent tc, String methodName) {
        System.out.println("Entry:" + methodName);
    }

    public static final void entry(TraceComponent tc, String methodName,
                                   Object objs) {
        System.out.println("Entry:" + methodName);
        System.out.println("Objects :" + objs);
    }

    public static final void exit(TraceComponent tc, String methodName) {
        System.out.println("Exit :" + methodName);
    }

    public static final void exit(TraceComponent tc, String methodName, Object o) {
        System.out.println("Exit :" + methodName);
        System.out.println("Objects :" + o);
    }

    public static final void event(TraceComponent tc, String msg) {
        System.out.println("msg :" + msg);
    }

    public static final void event(TraceComponent tc, String msg,
                                   Object objs) {
        System.out.println("msg :" + msg);
        System.out.println("Objects :" + objs);
    }

    public static void warning(TraceComponent tc, String string) {
        System.out.println("msg :" + string);
    }

    /**
     * @param tc
     * @param string
     * @param reassociationX
     */
    public static void warning(TraceComponent tc, String string,
                               Object objs) {
        System.out.println("msg :" + string);
        System.out.println("Objects :" + objs);
    }

    public static void error(TraceComponent tc, String string) {
        System.out.println("msg :" + string);
    }

    /**
     * @param tc
     * @param string
     * @param reassociationX
     */
    public static void error(TraceComponent tc, String string, Object objs) {
        System.out.println("msg :" + string);
        System.out.println("Objects :" + objs);
    }

}
