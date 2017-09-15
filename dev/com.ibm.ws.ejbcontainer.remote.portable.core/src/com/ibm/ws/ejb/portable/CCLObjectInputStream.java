/*******************************************************************************
 * Copyright (c) 2001, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejb.portable;

import java.io.*;

//     CCLObjectInputStream - ObjectInputStream for portable package using the ContextClass Loader

public class CCLObjectInputStream extends java.io.ObjectInputStream
{
    protected ClassLoader classloader;

    /*
     * CCLObjectInputStream
     * Basically, this constructor sets up the classloader for the stream
     * to use the context class loader, something objectinputstream does not do
     * 
     * @param is - The input stream to use.
     * 
     * @exception java.io.IOException - a system-level failure.
     */

    public CCLObjectInputStream(InputStream is)
        throws IOException {
        this(is, Thread.currentThread().getContextClassLoader());
    }

    /*
     * CCLObjectInputStream
     * Basically, this constructor sets up the classloader for the stream
     * to use the context class loader, something objectinputstream does not do
     * 
     * @param is - The input stream to use.
     * 
     * @param cl - The class loader to use.
     * 
     * @exception java.io.IOException - a system-level failure.
     */
    public CCLObjectInputStream(InputStream is, ClassLoader cl)
        throws IOException {
        super(is);
        classloader = cl;
    }

    /*
     * resolveClass
     * resolves class using the contextClassLoader
     * 
     * @param objStrmClass - class to load
     * 
     * @return Serializable - a Class loaded by context class loader.
     * 
     * @exception java.io.IOException - a system-level failure.
     * 
     * @exception java.lang.ClassNotFoundException - could not load requested class
     * .
     */

    protected Class resolveClass(ObjectStreamClass objStrmClass)
                    throws IOException,
                    ClassNotFoundException
    {

        // changed to use runtime classloader first and then if that fails
        // use the context classloader.
        // WARNING:  since this will load a class using Class.forName()
        // it is a potential security problem if an application could
        // instantiate an instance of it.  The ProtectionClassLoader prevents
        // this from happening.

        try
        {
            return Class.forName(objStrmClass.getName());
        } catch (ClassNotFoundException cnfe)
        {
            //FFDCFilter.processException(ex, CLASS_NAME + "resolveClass", "97", this);
        }
        return Class.forName(objStrmClass.getName(), true, classloader);

    }

}
