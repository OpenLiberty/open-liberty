/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.anno.info;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;

/**
 * <p>A store of info objects (packages, classes, fields, methods, and annotations).</p>
 */
public interface InfoStore {
    // Logging ...

    /**
     * <p>Answer a print string for the receiver, for use in debugging. The value is
     * guaranteed to be unique during the lifetime of the receiver, and, for frequently
     * created types, will be created on demand.</p>
     * 
     * @return A print string for the receiver.
     */
    public String getHashText();

    /**
     * <p>Log the receiver to the specified logger.</p>
     * 
     * @param logger The logger to receive the display of the receiver.
     */
    public void log(TraceComponent logger);

    // Context ...

    public InfoStoreFactory getInfoStoreFactory();

    public ClassSource_Aggregate getClassSource();

    // Open close ...
    //
    // Needed to handle fast mode enablement, which, for containers mapped
    // to jar files, obtain enduring references to opened jar files.  As
    // external resources, these have a close API.  The close API is used
    // to close the external resources as soon as possible when the resource
    // is no longer in use.  The alternative is to rely on garbage collection
    // finalization, which does not occur at a predictable time.

    public void open() throws InfoStoreException;

    public void close() throws InfoStoreException;

    // Name storage ...

    /**
     * <p>Answer the interned copy of a specified description.</p>
     * 
     * @param description The string description value which is to be interned.
     * 
     * @return The interned copy of a specified string.
     * 
     * @see #internPackageName(String)
     * @see #internClassName(String)
     * @see #internQualifiedFieldName(String)
     * @see #internQualifiedMethodName(String)
     */
    public String internDescription(String description);

    /**
     * <p>Answer the interned copy of a specified package name.</p>
     * 
     * @param packageName The package name which is to be interned.
     * 
     * @return The interned copy of the package name.
     * 
     * @see #internDescription(String)
     * @see #internClassName(String)
     * @see #internQualifiedFieldName(String)
     * @see #internQualifiedMethodName(String)
     */
    public String internPackageName(String packageName);

    /**
     * <p>Answer the interned copy of a specified class name.</p>
     * 
     * <p>Each store has a string store used to guarantee unique
     * of string values. Maintenance of a string store is necessary
     * because the process of loading class and annotations information does
     * not guarantee identical instances for strings which have the same value,
     * and because collections of related classes tend to produce many
     * of the same string values. For example, the reference of a class
     * to a super type, or to an interface, uses the same string value as the
     * declaration of the super type, or of the interface.</p>
     * 
     * <p>Interning string values leads to dramatically smaller overall string
     * allocations.</p>
     * 
     * @param className The string class name which is to be interned.
     * 
     * @return The interned copy of the class name.
     * 
     * @see #internDescription(String)
     * @see #internQualifiedFieldName(String)
     * @see #internQualifiedMethodName(String)
     */
    public String internClassName(String className);

    //

    /**
     * <p>Answer the package info object for a specified package name.</p>
     * 
     * @return The package info object for a specified package name.
     * 
     *         TODO What happens if the package is not found?
     */
    public PackageInfo getPackageInfo(String name);

    /**
     * <p>Answer (possibly delayed) class info object for a specified class name.</p>
     * 
     * @return The class info object for a specified class name.
     */
    public ClassInfo getDelayableClassInfo(String name);

    //

    /**
     * <p>Answer the number of entries to stream processing.</p>
     * 
     * @return The number of entries to stream processing.
     */
    public long getStreamCount();

    /**
     * <p>Answer the total time, in milliseconds, doing stream processing.</p>
     * 
     * @return The total time, in milliseconds, doing stream processing.
     * 
     * @see #getRuleTime()
     */
    public long getStreamTime();

    /**
     * <p>Answer the total time, in milliseconds, spent scanning classes.</p>
     * 
     * @return The total time, in milliseconds, spent scanning classes.
     * 
     * @see #getRuleTime()
     */
    public long getScanTime();

    /**
     * <p>Answer the total time, in milliseconds, spent doing rule processing.</p>
     * 
     * @return The total time, in milliseconds, spent doing rule processing.
     * 
     * @see #getScanTime()
     */

    public long getRuleTime();
}
