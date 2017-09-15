package com.ibm.ws.objectManager;

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

import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * Ensures that when a Token is read from the store it is replaced an equivalent one already known to the ObjectStore.
 * This is so that any references to it then refer to the same instance of the ManagedObject.
 * 
 * @version @(#) 1/25/13
 * @author Andrew Banks
 */
class ManagedObjectInputStream
                extends java.io.ObjectInputStream
{
    private static final Class cclass = ManagedObjectInputStream.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(cclass,
                                                                     ObjectManagerConstants.MSG_GROUP_OBJECTS);

    protected ObjectManagerState objectManagerState;

    /**
     * Constructor
     * 
     * @param inputStream from which to construct the serialized ManagedObjects.
     * @param objectManagerState of the objectManager reconstructing the ManagedObjects.
     * @throws java.io.IOException
     * @throws java.io.StreamCorruptedException
     */
    ManagedObjectInputStream(java.io.InputStream inputStream,
                             ObjectManagerState objectManagerState)
        throws java.io.IOException, java.io.StreamCorruptedException
    {
        super(inputStream);
        final String methodName = "<init>";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        methodName,
                        new Object[] { inputStream, objectManagerState }
                            );

        this.objectManagerState = objectManagerState;
        java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction/* <Object> */() {
                            public Object run() {
                                enableResolveObject(true); // Switch on calls to resolveObject().
                                return null; // nothing to return
                            }
                        });

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName);
    } // ManagedObjectInputStream().

    /*
     * (non-Javadoc)
     * 
     * @see java.io.ObjectInputStream#readClassDescriptor()
     */
    protected java.io.ObjectStreamClass readClassDescriptor()
                    throws java.io.IOException, java.lang.ClassNotFoundException {
        final String methodName = "readClassDescriptor";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this, cclass,
                        methodName);

        java.io.ObjectStreamClass objectStreamClass = super.readClassDescriptor();

        if (objectStreamClass.getSerialVersionUID() == 2723354058036620229L
            && objectStreamClass.getName().equals(ManagedObject.class.getName())) {
            // Old JCLRM default SerialVersionUID found, class definition 
            // identical to SerialVersionUID=1212101998694878938L.
            if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                trace.entry(this, cclass,
                            methodName, new Object[] { "Migrating:93", objectStreamClass });
            objectStreamClass = java.io.ObjectStreamClass.lookup(ManagedObject.class);
        } // if (   objectStreamClass.getName().

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass, methodName, new Object[] { objectStreamClass });
        return objectStreamClass;
    } // readClassDescriptor().

    /**
     * Resolve any Token to an existing instance of the Token representing a ManagedObject if there is one already in
     * memory.
     */
    protected Object resolveObject(Object objectToResolve) {
        final String methodName = "resolveObject";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectToResolve });

        Object resolvedObject; // The resolved object we will return.
        if (objectToResolve instanceof Token) {
            resolvedObject = ((Token) objectToResolve).current();
        } else {
            resolvedObject = objectToResolve;
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this, cclass,
                       methodName,
                       new Object[] { resolvedObject });
        return resolvedObject;
    } // resolveObject().

    protected Class resolveClass(java.io.ObjectStreamClass objectStreamClass)
                    throws java.io.IOException,
                    java.lang.ClassNotFoundException {
        final String methodName = "resolveClass";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName,
                        new Object[] { objectStreamClass });

        Class theClass = null;
        try {
            theClass = Class.forName(objectStreamClass.getName());

        } catch (java.lang.ClassNotFoundException classNotFoundException) {
            // Get the ClassLoader, which may be the standard ClassLoader or may be an application
            // ClassLoader provided by WebSphere
            ClassLoader classLoader = (ClassLoader) java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction/* <ClassLoader> */() {
                                public Object run() {
                                    return Thread.currentThread().getContextClassLoader();
                                }
                            });
            theClass = Class.forName(objectStreamClass.getName(), true, classLoader);
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { theClass });
        return theClass;
    } // resolveClass().
} // class ManagedObjectInputStream.
