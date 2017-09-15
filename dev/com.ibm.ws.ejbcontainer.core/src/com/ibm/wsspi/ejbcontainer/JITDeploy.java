/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.ejbcontainer;

import java.util.Arrays;
import java.util.List;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.jitdeploy.JIT_Stub;
import com.ibm.ws.ejbcontainer.jitdeploy.JIT_StubPluginImpl;

/**
 * Provides access to Just-In-Time deployment of EJB artifacts to WebSphere
 * tooling providers. <p>
 *
 * @author IBM Corp.
 * @since WAS 7.0
 * @ibm-spi
 **/
public final class JITDeploy
{
    private static final TraceComponent tc = Tr.register(JITDeploy.class,
                                                         "JITDeploy",
                                                         "com.ibm.ejs.container.container");

    public static final String rmicCompatible = "com.ibm.websphere.ejbcontainer.rmicCompatible";
    public static final int RMICCompatible = parseRMICCompatible(System.getProperty(rmicCompatible));

    public static final String throwRemoteFromEjb3Stub = "com.ibm.websphere.ejbcontainer.ejb3StubThrowsRemote";
    public static final boolean ThrowRemoteFromEjb3Stub = Boolean.getBoolean((throwRemoteFromEjb3Stub));

    private static final int RMIC_COMPATIBLE_DEFAULT = 0;
    private static final int RMIC_COMPATIBLE_VALUES = 1 << 0;
    private static final int RMIC_COMPATIBLE_EXCEPTIONS = 1 << 1;

    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Property: RMICCompatible          = " + Integer.toHexString(RMICCompatible));
    }

    /**
     * Creating an instance of JITDeploy is not allowed.
     **/
    private JITDeploy()
    {
        throw new IllegalStateException();
    }

    /**
     * The list of options accepted by {@link #parseRMICCompatible}.
     *
     * @return the list of RMIC compatibility options
     */
    // PM94096
    public static List<String> getRMICCompatibleOptions()
    {
        return Arrays.asList(new String[] {
                                           "none",
                                           "all",
                                           "values",
                                           "exceptions",
        });
    }

    /**
     * Parse an rmic compatibility options string.
     *
     * @param options the options string
     * @return the compatibility flags
     * @see #isRMICCompatibleValues
     */
    public static int parseRMICCompatible(String options) // PM46698
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "parseRMICCompatible: " + options);

        int flags;

        if (options == null)
        {
            flags = RMIC_COMPATIBLE_DEFAULT;
        }
        else if (options.equals("none"))
        {
            flags = 0;
        }
        else if (options.isEmpty() || options.equals("all"))
        {
            flags = -1;
        }
        else
        {
            flags = 0;
            for (String option : options.split(","))
            {
                if (option.equals("values"))
                {
                    flags |= RMIC_COMPATIBLE_VALUES;
                }
                else if (option.equals("exceptions")) // PM94096
                {
                    flags |= RMIC_COMPATIBLE_EXCEPTIONS;
                }
                else
                {
                    throw new IllegalArgumentException("unknown RMIC compatibility option: " + option);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "parseRMICCompatible: " + Integer.toHexString(flags));
        return flags;
    }

    /**
     * Returns true if the flags returned from {@link #parseRMICCompatible} indicate that read_value/write_value should be used for compatibility
     * with rmic.
     *
     * @param flags rmic compatibility flags
     */
    public static boolean isRMICCompatibleValues(int flags) // PM46698
    {
        return (flags & RMIC_COMPATIBLE_VALUES) != 0;
    }

    /**
     * Returns true if the flags returned from {@link #parseRMICCompatible} indicate that exception names should be mangled for compatibility with
     * rmic.
     *
     * @param flags rmic compatibility flags
     */
    public static boolean isRMICCompatibleExceptions(int flags) // PM94096
    {
        return (flags & RMIC_COMPATIBLE_EXCEPTIONS) != 0;
    }

    /**
     * Returns the name of the Stub class that needs to be loaded for the
     * specified remote interface class. <p>
     *
     * Basically, the name of the Stub class for any remote interface is
     * the simple name of the remote interface class, with an '_' prepended,
     * and '_Stub' appended. The package of the returned Stub class name
     * will be the same as the package of the remote interface. <p>
     *
     * @param remoteInterface remote interface class.
     *
     * @return the name of the Stub class that needs to be loaded for the
     *         specified remote interface class. <p>
     **/
    public static String getStubClassName(Class<?> remoteInterface)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getStubClassName : " + remoteInterface.getName());

        String result = JIT_Stub.getStubClassName(remoteInterface.getName());

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getStubClassName : " + result);

        return result;
    }

    /**
     * Generates the class bytes for the Stub class corresponding to the
     * specified remote interface class. <p>
     *
     * The corresponding method, getStubClassName(), provides the name
     * of the generated stub class. <p>
     *
     * Intended for use by WebSphere tooling providers. <p>
     *
     * @param remoteInterface Interface implemented by the generated Stub;
     *            not required to implement java.rmi.Remote.
     *
     * @return the class bytes for the Stub class corresponding to the
     *         specified remote interface class.
     **/
    public static byte[] generateStubBytes(Class<?> remoteInterface)
                    throws EJBConfigurationException
    {
        return generateStubBytes(remoteInterface, RMICCompatible);
    }

    /**
     * Generates the class bytes for the Stub class with rmic compatibility
     * corresponding to the specified remote interface class. <p>
     *
     * The corresponding method, getStubClassName(), provides the name
     * of the generated stub class. <p>
     *
     * Intended for use by WebSphere tooling providers. <p>
     *
     * @param remoteInterface Interface implemented by the generated Stub;
     *            not required to implement java.rmi.Remote.
     * @param rmicCompatible The rmic compatibility to use as returned by {@link #parseRMICCompatible}.
     *
     * @return the class bytes for the Stub class corresponding to the
     *         specified remote interface class.
     **/
    public static byte[] generateStubBytes(Class<?> remoteInterface,
                                           int rmicCompatible)
                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "generateStubBytes : " + remoteInterface.getName() +
                         ", rmicCompatible=" + rmicCompatible);

        byte[] stubBytes = JIT_Stub.generateStubBytes(remoteInterface, rmicCompatible);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "generateStubBytes : " + stubBytes.length + " bytes");

        return stubBytes;
    }

    /**
     * Registers a Just-In-Time Stub Class plugin with the specified
     * ClassLoader. <p>
     *
     * The specified ClassLoader must be an instance of CompoundClassLoader,
     * or an IllegalArgumentException will occur. <p>
     *
     * If a JIT_StubClassPlugin has already been registered with the specified
     * ClassLoader, then this method will have no effect. <p>
     *
     * The JIT_StubClassPlugin will be invoked to define _Stub classes
     * for each class with a name ending with '_Stub', that cannot be
     * found in the classpath. The parent ClassLoader will be invoked
     * prior to invoking the JIT_StubClassPlugin regardless of the
     * delegation model for this ClassLoader. <p>
     *
     * @param classloader the ClassLoader to plugin the new Just-In-Time Stub
     *            Class Plugin
     *
     * @throws IllegalArgumentException if the specified ClassLoader does not
     *             support the JIT_StubClassPlugin.
     **/
    // F1339-8988
    public static void registerJIT_StubClassPlugin(ClassLoader classloader)
    {
        boolean isRegistered = JIT_StubPluginImpl.register(classloader);

        if (!isRegistered)
        {
            throw new IllegalArgumentException("Specified ClassLoader does not support JIT_StubClassPlugin : " + classloader);
        }
    }

}
