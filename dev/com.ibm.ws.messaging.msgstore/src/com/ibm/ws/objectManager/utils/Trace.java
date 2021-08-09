package com.ibm.ws.objectManager.utils;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Tracing/Info logging.
 */
public interface Trace
{
    public static final int Level_All = 1;
    public static final int Level_Entry = 2;
    public static final int Level_Debug = 3;
    public static final int Level_Event = 4;
    public static final int Level_None = 5;

    /**
     * Test to see if debug tracing is enabled.
     * 
     * @return boolean true is method entry tracing is enabled.
     */
    public abstract boolean isDebugEnabled();

    /**
     * Test to see if method entry tracing is enabled.
     * 
     * @return boolean true is method entry tracing is enabled.
     */
    public abstract boolean isEntryEnabled();

    /**
     * Test to see if event tracing is enabled.
     * 
     * @return boolean true is method entry tracing is enabled.
     */
    public abstract boolean isEventEnabled();

    // Defect 607710
    public abstract boolean isAnyTracingEnabled();

    /**
     * Byte data trace for static classes.
     * 
     * @param sourceClass of the static object making the trace call.
     * @param data the byte array to be traced
     */
    public abstract void bytes(Class sourceClass,
                               byte[] data);

    /**
     * Byte data trace for static Objects.
     * 
     * @param sourceClass of the static class making the trace call.
     * @param data the byte array to be traced
     * @param start position to start tracing the byte array
     */
    public abstract void bytes(Class sourceClass,
                               byte[] data,
                               int start);

    /**
     * Byte data trace for static Objects.
     * 
     * @param sourceClass of the static Object making the trace call.
     * @param data the byte array to be traced
     * @param start position to start tracing the byte array
     * @param count of bytes from start position that should be traced
     */
    public abstract void bytes(Class sourceClass,
                               byte[] data,
                               int start,
                               int count);

    /**
     * Byte data trace.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of Object making the trace call.
     * @param data the byte array to be traced.
     */
    public abstract void bytes(Object source,
                               Class sourceClass,
                               byte[] data);

    /**
     * Byte data trace for static Objects.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of the Object making the trace call.
     * @param data the byte array to be traced.
     * @param start position to start tracing the byte array.
     */
    public abstract void bytes(Object source,
                               Class sourceClass,
                               byte[] data,
                               int start);

    /**
     * Byte data trace.
     * 
     * @param source making the trace call.
     * @param sourceClass of Object making the trace call.
     * @param data the byte array to be traced.
     * @param start position to start tracing the byte array.
     * @param count of bytes from start position that should be traced.
     */
    public abstract void bytes(Object source,
                               Class sourceClass,
                               byte[] data,
                               int start,
                               int count);

    /**
     * Method debug tracing for static classes.
     * 
     * @param sourceClass of static object making the trace call.
     * @param methodName of the method being entered.
     */
    public abstract void debug(Class sourceClass,
                               String methodName);

    /**
     * Method debug tracing for static classes.
     * 
     * @param sourceClass of the static object making the trace call.
     * @param methodName of the method being entered.
     * @param object on which toString() is called.
     */
    public abstract void debug(Class sourceClass,
                               String methodName,
                               Object object);

    /**
     * Method debug tracing for static classes.
     * 
     * @param sourceClass of the static object making the trace call.
     * @param methodName of the method being entered.
     * @param objects Object or array of <code>Objects</code>. toString() is called on each object
     *            and the results are appended to the methodName.
     */
    public abstract void debug(Class sourceClass,
                               String methodName,
                               Object[] objects);

    /**
     * Method debug tracing.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of the Object making the trace call.
     * @param methodName of the method being entered.
     */
    public abstract void debug(Object source,
                               Class sourceClass,
                               String methodName);

    /**
     * Method entry tracing.
     * 
     * @param source making the trace call.
     * @param sourceClass of the Object making the trace call.
     * @param methodName of the method being entered.
     * @param object on which toString() is called.
     */
    public abstract void debug(Object source,
                               Class sourceClass,
                               String methodName,
                               Object object);

    /**
     * Method debug tracing.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of the Object making the trace call.
     * @param methodName of the method being traced.
     * @param objects array of <code>Objects</code> on which toString() is called
     *            and the results are appended to the methodName.
     */
    public abstract void debug(Object source,
                               Class sourceClass,
                               String methodName,
                               Object[] objects);

    /**
     * Method entry tracing for static classes.
     * 
     * @param sourceClass of static object making the trace call.
     * @param methodName of the method being entered.
     */
    public abstract void entry(Class sourceClass,
                               String methodName);

    /**
     * Method entry tracing for static classes.
     * 
     * @param sourceClass of the static object making the trace call.
     * @param methodName of the method being entered.
     * @param object on which toString() is called.
     */
    public abstract void entry(Class sourceClass,
                               String methodName,
                               Object object);

    /**
     * Method entry tracing for static classes.
     * 
     * @param sourceClass of the static object making the trace call.
     * @param methodName of the method being entered.
     * @param objects Object or array of <code>Objects</code>. toString() is called
     *            on each object and the results are appended to the methodName.
     */
    public abstract void entry(Class sourceClass,
                               String methodName,
                               Object[] objects);

    /**
     * Method entry tracing.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of the Object making the trace call.
     * @param methodName of the method being entered.
     */
    public abstract void entry(Object source,
                               Class sourceClass,
                               String methodName);

    /**
     * Method entry tracing.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of the Object making the trace call.
     * @param methodName of the method being entered.
     * @param object on which toString() is called.
     */
    public abstract void entry(Object source,
                               Class sourceClass,
                               String methodName,
                               Object object);

    /**
     * Method entry tracing.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of Object making the trace call.
     * @param methodName of the method being entered.
     * @param objects toString() is called on each object and the results are appended to
     *            the methodName.
     */
    public abstract void entry(Object source,
                               Class sourceClass,
                               String methodName,
                               Object[] objects);

    /**
     * Method exit tracing for static methods.
     * 
     * @param sourceClass of static making the trace call.
     * @param methodName of the method being entered.
     */
    public abstract void exit(Class sourceClass,
                              String methodName);

    /**
     * Method exit tracing for static methods.
     * 
     * @param sourceClass of static making the trace call.
     * @param methodName of the method being entered.
     * @param object on which toString() is called.
     */
    public abstract void exit(Class sourceClass,
                              String methodName,
                              Object object);

    /**
     * Method exit tracing for static methods.
     * 
     * @param sourceClass of static making the trace call.
     * @param methodName of the method being entered.
     * @param objects toString() is called on each object.
     */
    public abstract void exit(Class sourceClass,
                              String methodName,
                              Object[] objects);

    /**
     * Method exit tracing.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of Object making the trace call.
     * @param methodName of the method being entered.
     */
    public abstract void exit(Object source,
                              Class sourceClass,
                              String methodName);

    /**
     * Method exit tracing.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of Object making the trace call.
     * @param methodName of the method being entered.
     * @param object on which toString() is called.
     */
    public abstract void exit(Object source,
                              Class sourceClass,
                              String methodName,
                              Object object);

    /**
     * Method exit tracing.
     * 
     * @param source Object making the trace call.
     * @param sourceClass of Object making the trace call.
     * @param methodName name of the method being entered.
     * @param objects toString() is called on each object.
     */
    public abstract void exit(Object source,
                              Class sourceClass,
                              String methodName,
                              Object[] objects);

    /**
     * Event tracing when a throwable is caught in a static class.
     * 
     * @param sourceClass of the static object catching the exception.
     * @param methodName of the method catching the exception.
     * @param throwable causing the event.
     */
    public abstract void event(Class sourceClass,
                               String methodName,
                               Throwable throwable);

    /**
     * Event tracing.
     * 
     * @param source Object making the trace call.
     * @param sourceClass os the Object making the trace call.
     * @param methodName of the method being entered.
     * @param throwable causing the Event.
     */
    public abstract void event(Object source,
                               Class sourceClass,
                               String methodName,
                               Throwable throwable);

    /**
     * Method information tracing for static objects.
     * 
     * @param sourceClass of the static Object making the trace call.
     * @param methodName of the method being entered.
     * @param messageIdentifier identifying the message in the message catalog.
     * @param object to be inserted into the message.
     */
    public abstract void info(Class sourceClass,
                              String methodName,
                              String messageIdentifier,
                              Object object);

    /**
     * Method information tracing for static objects.
     * 
     * @param sourceClass of the static Object making the trace call.
     * @param methodName of the method being entered.
     * @param messageIdentifier identifying the message in the message catalog.
     * @param objects to be inserted into the message.
     */
    public abstract void info(Class sourceClass,
                              String methodName,
                              String messageIdentifier,
                              Object[] objects);

    /**
     * Method information tracing.
     * 
     * @param source making the trace call.
     * @param sourceClass of Object making the trace call.
     * @param methodName of the method being entered.
     * @param messageIdentifier identifying the message in the message catalog.
     * @param object to be inserted into the message.
     */
    public abstract void info(Object source,
                              Class sourceClass,
                              String methodName,
                              String messageIdentifier,
                              Object object);

    /**
     * Method information tracing.
     * 
     * @param source making the trace call.
     * @param sourceClass of Object making the trace call.
     * @param methodName of the method being entered.
     * @param messageIdentifier identifying the message in the message catalog.
     * @param objects array containing inserts into the message.
     */
    public abstract void info(Object source,
                              Class sourceClass,
                              String methodName,
                              String messageIdentifier,
                              Object[] objects);

    /**
     * Method warning tracing for static objects.
     * 
     * @param Class of the static object making the trace call.
     * @param String name of the method being entered.
     * @param String identifying the message.
     * @param Object to be inserted into the message.
     */
    public abstract void warning(Class sourceClass,
                                 String methodName,
                                 String messageIdentifier,
                                 Object object);

    /**
     * Method warning tracing for static objects.
     * 
     * @param Class of the static object making the trace call.
     * @param String name of the method being entered.
     * @param String identifying the message.
     * @param Object[] containing inserts into the message.
     */
    public abstract void warning(Class sourceClass,
                                 String methodName,
                                 String messageIdentifier,
                                 Object[] objects);

    /**
     * Method warning tracing.
     * 
     * @param Object making the trace call.
     * @param Class of Object making the trace call.
     * @param String name of the method being entered.
     * @param String identifying the message.
     * @param Object to be inserted into the message.
     */
    public abstract void warning(Object source,
                                 Class sourceClass,
                                 String methodName,
                                 String messageIdentifier,
                                 Object object);

    /**
     * Method warning tracing.
     * 
     * @param Object making the trace call.
     * @param Class of Object making the trace call.
     * @param String name of the method being entered.
     * @param String identifying the message.
     * @param Object[] containing inserts into the message.
     */
    public abstract void warning(Object source,
                                 Class sourceClass,
                                 String methodName,
                                 String messageIdentifier,
                                 Object[] objects);
} // interface Trace.
