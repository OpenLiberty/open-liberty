package com.ibm.ws.objectManager;

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;
import com.ibm.ws.objectManager.utils.UtilsException;

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
 * A subclass of ObjectManagerException is thrown whenever an ObjectManager error occurs.
 */
public abstract class ObjectManagerException
                extends UtilsException
{
    private static final Class cclass = ObjectManagerException.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_EXCEPTIONS);

    /**
     * Construct a new objectManagerException object.
     * 
     * @param source in which the exception is raised, or Class if static.
     * @param exceptionClass of the ObjectManagerException subclass.
     */
    protected ObjectManagerException(Object source,
                                     Class exceptionClass) {
        super(ObjectManager.nls.format(exceptionClass.getName().substring(exceptionClass.getName().lastIndexOf(".") + 1) + "_info"));

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { source,
                                      exceptionClass });
            trace.exit(this, cclass, "<init>");
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).
    } // ObjectManagerException().

    /**
     * Construct a new objectManagerException object.
     * 
     * @param source Object in which the exception is raised, or Class if static.
     * @param exceptionClass of the ObjectManagerException subclass.
     * @param insert to be insterted into the message.
     */
    protected ObjectManagerException(Object source,
                                     Class exceptionClass,
                                     Object insert) {
        super(ObjectManager.nls.format(exceptionClass.getName().substring(exceptionClass.getName().lastIndexOf(".") + 1) + "_info", insert));

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this,
                        cclass,
                        "<init>",
                        new Object[] { source,
                                      exceptionClass,
                                      insert });
            trace.exit(this, cclass, "<init>");
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).   
    } // ObjectManagerException().

    /**
     * Construct a new objectManagerException object.
     * 
     * @param source in which the exception is raised, or Class if static.
     * @param exceptionClass of the ObjectManagerException subclass.
     * @param inserts to be insterted into the message.
     */
    protected ObjectManagerException(Object source,
                                     Class exceptionClass,
                                     Object[] inserts) {
        super(ObjectManager.nls.format(exceptionClass.getName().substring(exceptionClass.getName().lastIndexOf(".") + 1) + "_info", inserts));

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass, "<init>"
                        , new Object[] { source, exceptionClass, inserts });
            trace.exit(this, cclass, "<init>");
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).
    } // ObjectManagerException().

    /**
     * Construct a new objectManagerException object for a linked exception.
     * 
     * @param source in which the exception is raised, or Class if static.
     * @param exceptionClass of the ObjectManagerException subclass.
     * @param throwable linked to this exception.
     */
    protected ObjectManagerException(Object source,
                                     Class exceptionClass,
                                     Throwable throwable) {
        super(ObjectManager.nls.format(exceptionClass.getName().substring(exceptionClass.getName().lastIndexOf(".") + 1) + "_info")
              , throwable);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass, "<init>"
                        , new Object[] { source, exceptionClass, throwable });
            trace.exit(this, cclass, "<init>");
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).
    } // ObjectManagerException().

    /**
     * Construct a new objectManagerException object for a linked exception.
     * 
     * @param source in which the exception is raised, or Class if static.
     * @param exceptionClass of the ObjectManagerException subclass.
     * @param throwable linked to this exception.
     * @param insert to be insterted into the message.
     */
    protected ObjectManagerException(Object source,
                                     Class exceptionClass,
                                     Throwable throwable,
                                     Object insert) {
        super(ObjectManager.nls.format(exceptionClass.getName().substring(exceptionClass.getName().lastIndexOf(".") + 1) + "_info", insert)
              , throwable);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass, "<init>"
                        , new Object[] { source, exceptionClass, throwable, insert });
            trace.exit(this, cclass, "<init>");
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
    } // ObjectManagerException().

    /**
     * Construct a new objectManagerException object for a linked exception.
     * 
     * @param source in which the exception is raised, or Class if static.
     * @param exceptionClass of the ObjectManagerException subclass.
     * @param throwable linked to this exception.
     * @param inserts to be insterted into the message.
     */
    protected ObjectManagerException(Object source,
                                     Class exceptionClass,
                                     Throwable throwable,
                                     Object[] inserts) {
        super(ObjectManager.nls.format(exceptionClass.getName().substring(exceptionClass.getName().lastIndexOf(".") + 1) + "_info", inserts)
              , throwable);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()) {
            trace.entry(this, cclass, "<init>"
                        , new Object[] { source, exceptionClass, throwable, inserts });
            trace.exit(this, cclass, "<init>");
        } // if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled()).  
    } // ObjectManagerException().

} // class ObjectManagerException.
