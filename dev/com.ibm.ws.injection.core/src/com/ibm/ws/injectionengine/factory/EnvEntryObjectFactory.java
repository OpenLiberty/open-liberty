/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.factory;

import static com.ibm.ws.injectionengine.factory.EnvEntryInfoRefAddr.ADDR_TYPE;

import java.security.AccessController;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;

/**
 * This object factory can be used when binding env-entry Class and Enum values
 * when a class loader is not available when binding is performed. The context
 * class loader is used to load the class and optionally resolve the Enum.
 */
public class EnvEntryObjectFactory
                implements ObjectFactory
{
    private static final String CLASS_NAME = EnvEntryObjectFactory.class.getName();

    private static final TraceComponent tc = Tr.register(EnvEntryObjectFactory.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    private final static ThreadContextAccessor svThreadContextAccessor =
                    (ThreadContextAccessor) AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    private static Reference createReference(String referenceClassName,
                                             String name,
                                             ComponentNameSpaceConfiguration compNSConfig,
                                             String className,
                                             String valueName)
    {
        EnvEntryInfo info = new EnvEntryInfo(name,
                        compNSConfig.getApplicationName(),
                        compNSConfig.getModuleName(),
                        compNSConfig.getDisplayName(),
                        className,
                        valueName);
        return new Reference(referenceClassName,
                        new EnvEntryInfoRefAddr(info),
                        CLASS_NAME, null);
    }

    public static Reference createClassReference(String name,
                                                 ComponentNameSpaceConfiguration compNSConfig,
                                                 String className)
    {
        return createReference("java.lang.Class", name, compNSConfig, className, null);
    }

    public static Reference createEnumReference(String name,
                                                ComponentNameSpaceConfiguration compNSConfig,
                                                String className,
                                                String valueName)
    {
        return createReference(className, name, compNSConfig, className, valueName);
    }

    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment)
                    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance: " + obj);

        // -----------------------------------------------------------------------
        // Is obj a Reference?
        // -----------------------------------------------------------------------
        if (!(obj instanceof Reference))
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance: not a reference: " + Util.identity(obj));
            return null;
        }

        // -----------------------------------------------------------------------
        // Is the right factory for this reference?
        // -----------------------------------------------------------------------
        Reference ref = (Reference) obj;
        if (!ref.getFactoryClassName().equals(CLASS_NAME))
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance: wrong factory: " + ref);
            return null;
        }

        // -----------------------------------------------------------------------
        // Is address null?
        // -----------------------------------------------------------------------
        RefAddr addr = ref.get(ADDR_TYPE);
        if (addr == null)
        {
            NamingException nex = new NamingException("The address for this Reference is empty (null)");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : " + nex);
            throw nex;
        }

        EnvEntryInfo info = (EnvEntryInfo) addr.getContent();

        String className = info.ivClassName;
        if (className == null) // d701200
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance: missing type: " + ref);

            // Avoid a bad error message when deserializing a java:global
            // env-entry from a v8.0 GA server, which did not have origin data.
            if (info.ivName != null)
            {
                throw new IllegalStateException("The " + info.ivName +
                                                " env-entry was declared without a type by the " + info.ivComponentName +
                                                " component in the " + info.ivModuleName +
                                                " module of the " + info.ivApplicationName +
                                                " application.");
            }

            throw new IllegalStateException("The env-entry was declared without a type.");
        }

        ClassLoader classLoader = svThreadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());
        Class<?> klass = classLoader.loadClass(info.ivClassName);

        Object result;
        if (info.ivValueName == null)
        {
            result = klass;
        }
        else
        {
            @SuppressWarnings("unchecked")
            Object enumValue = Enum.valueOf((Class<? extends Enum>) klass, info.ivValueName);
            result = enumValue;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance");
        return result;
    }
}
