/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.BUSINESS_LOCAL;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.BUSINESS_REMOTE;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.LOCAL;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.LOCAL_BEAN;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.LOCAL_HOME;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.REMOTE;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.REMOTE_HOME;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.SERVICE_ENDPOINT;
import static com.ibm.ws.ejbcontainer.jitdeploy.RMItoIDL.getIdlMethodNames;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Provides Just In Time runtime deployment of EJBs and other
 * business components.
 **/
public final class JITDeploy
{
    private static final String CLASS_NAME = JITDeploy.class.getName();
    private static final TraceComponent tc = Tr.register(JITDeploy.class, JITUtils.JIT_TRACE_GROUP, JITUtils.JIT_RSRC_BUNDLE);

    /**
     * Returns the local or remote Component interface class that corresponds to
     * the specified local or remote Component Home interface class. <p>
     * 
     * The Component interface class may be derived from the Home interface by
     * examining the return type of one of the 'create' methods. All Homes
     * must have at least one 'create' method, and the return type must be
     * the Component interface, per the EJB Specification. <p>
     * 
     * If the Component interface cannot be found, either because there were
     * no create methods or the create method returned void, then an
     * EJBConfigurationException will be thrown. <p>
     * 
     * Other validation will NOT be performed on the returned interface.
     * The method 'validateInterfaceBasics()' should be called to insure
     * the returned Class is a valid Component interface. <p>
     * 
     * @param homeInterface local or remote EJB home interface class
     * @param beanName name used to identify the bean if an error is logged.
     * 
     * @return the corresponding local or remote Component interface class.
     * @throws EJBConfigurationException when the specified home interface
     *             either has no 'create' methods, or a create method
     *             returns 'void'.
     **/
    // d443878
    public static Class<?> getComponentInterface(Class<?> homeInterface,
                                                 String beanName)
                    throws EJBConfigurationException
    {
        return EJBHomeImpl.getComponentInterface(homeInterface, beanName);
    }

    /**
     * Validate the basic aspects of a business or component interface as
     * required by the EJB specification. This includes checking to insure
     * the interface is an interface, and that it does or does not extend
     * the javax.ejb interfaces (such as javax.ejb.EJBObject), corresponding
     * to the configured interface type ( business or component / local or
     * remote / home ). <p>
     * 
     * @param remoteHome component remote home interface to validate.
     * @param remoteInterface component remote interface to validate.
     * @param localHome component local home interface to validate.
     * @param localInterface component local interface to validate.
     * @param webServiceEndpointInterface WebService endpoint interface to validate.
     * @param remoteBusinessInterfaces business remote interfaces to validate.
     * @param localBusinessInterfaces business local interfaces to validate.
     * @param ejbClass EJB implementation class to validate.
     * @param beanName EJB name used for any error messages/exceptions.
     * @param beanType EJB type (Stateless/Stateful/etc..).
     * 
     * @throws EJBConfigurationException whenever the specified interface does
     *             not conform the the EJB Specification requirements.
     **/
    // d431543
    public static void validateInterfaceBasics
                    (Class<?> remoteHome,
                     Class<?> remoteInterface,
                     Class<?> localHome,
                     Class<?> localInterface,
                     Class<?> webServiceEndpointInterface,
                     Class<?>[] remoteBusinessInterfaces,
                     Class<?>[] localBusinessInterfaces,
                     Class<?> ejbClass,
                     String beanName,
                     int beanType)
                                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "validateInterfaceBasics : " + beanName);

        if (remoteHome != null)
        {
            EJBHomeImpl.validateInterfaceBasics(remoteHome,
                                                remoteInterface,
                                                REMOTE_HOME,
                                                beanName,
                                                beanType); // d461100
        }

        if (remoteInterface != null)
        {
            EJBWrapper.validateInterfaceBasics(remoteInterface,
                                               REMOTE,
                                               beanName);

            if (remoteHome == null)
            {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_MISSING_REMOTE_HOME_CNTR5001E",
                         new Object[] { beanName,
                                       remoteInterface.getName() });
                throw new EJBConfigurationException("An EJB remote component interface requires a remote-home interface : "
                                                    + remoteInterface.getName() + " on EJB " + beanName);
            }
        }

        if (localHome != null)
        {
            EJBHomeImpl.validateInterfaceBasics(localHome,
                                                localInterface,
                                                LOCAL_HOME,
                                                beanName,
                                                beanType); // d461100
        }

        if (localInterface != null &&
            beanType != InternalConstants.TYPE_MESSAGE_DRIVEN) // d443878.1
        {
            EJBWrapper.validateInterfaceBasics(localInterface,
                                               LOCAL,
                                               beanName);

            if (localHome == null)
            {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_MISSING_LOCAL_HOME_CNTR5002E",
                         new Object[] { beanName,
                                       localInterface.getName() });
                throw new EJBConfigurationException("An EJB local component interface requires a local-home interface : "
                                                    + localInterface.getName() + " on EJB " + beanName);
            }
        }

        // JITDeploy now generates the WebService Endpoint wrappers.     LI3294-35
        if (webServiceEndpointInterface != null)
        {
            EJBWrapper.validateInterfaceBasics(webServiceEndpointInterface,
                                               SERVICE_ENDPOINT,
                                               beanName);
        }

        if (remoteBusinessInterfaces != null)
        {
            for (Class<?> remote : remoteBusinessInterfaces)
            {
                EJBWrapper.validateInterfaceBasics(remote,
                                                   BUSINESS_REMOTE,
                                                   beanName);
            }
        }

        if (localBusinessInterfaces != null)
        {
            EJBWrapperType wrapperType = null;
            for (Class<?> local : localBusinessInterfaces)
            {
                // No-Interface if it is the EJB class, otherwise local.   F743-1756
                wrapperType = (local == ejbClass) ? LOCAL_BEAN : BUSINESS_LOCAL;
                EJBWrapper.validateInterfaceBasics(local,
                                                   wrapperType,
                                                   beanName);
            }
        }

        // Also validate the EJB Class implementation.                     d457128
        if (ejbClass != null)
        {
            EJBUtils.validateEjbClass(ejbClass,
                                      beanName,
                                      beanType);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "validateInterfaceBasics : " + beanName);
    }

    /**
     * Generates and loads an EJB Wrapper class. <p>
     * 
     * @param wrapperClassName name of the wrapper class to be generated.
     * @param wrapperInterfaces Interfaces implemented by the generated wrapper.
     * @param wrapperType Type of wrapper to be generated (REMOTE, LOCAL, etc.)
     * @param allMethods Set of all methods from all interfaces of the
     *            type of wrapper being generated.
     * @param methodInfos EJB method info objects for all of the methods
     *            from all interfaces of the type of wrapper being
     *            generated.
     * @param ejbClassName Name of the EJB implementation class, that
     *            the generated wrapper will route methods to.
     * @param beanName Name of the EJB (for messages)
     **/
    public static Class<?> generateEJBWrapper(ClassLoader classLoader,
                                              String wrapperClassName,
                                              Class<?>[] wrapperInterfaces,
                                              EJBWrapperType wrapperType,
                                              Method[] allMethods,
                                              EJBMethodInfoImpl[] methodInfos,
                                              String ejbClassName,
                                              String beanName,
                                              ClassDefiner classDefiner)
                    throws ClassNotFoundException, EJBConfigurationException
    {
        Class<?> rtnClass = null;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "generateEJBWrapper: " + wrapperClassName + " : " + beanName);

        try
        {
            byte[] classbytes = EJBWrapper.generateClassBytes(wrapperClassName,
                                                              wrapperInterfaces,
                                                              wrapperType,
                                                              allMethods,
                                                              methodInfos,
                                                              ejbClassName,
                                                              beanName);

            rtnClass = classDefiner.findLoadedOrDefineClass(classLoader, wrapperClassName, classbytes);
        } catch (EJBConfigurationException ejbex)
        {
            FFDCFilter.processException(ejbex, CLASS_NAME + ".generateEJBWrapper",
                                        "110");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generateEJBWrapper failed: " + ejbex.getMessage());

            throw ejbex;
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".generateEJBWrapper",
                                        "119");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generateEJBWrapper failed: " + ex.getMessage());

            throw new ClassNotFoundException(wrapperClassName + " : " + ex.getMessage(), ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "generateEJBWrapper: " + rtnClass);

        return rtnClass;
    }

    /**
     * Generates and loads an EJB Wrapper Proxy class. <p>
     * 
     * @param classLoader Class loader to use to define the class; must
     *            have visibility to the EJSWrapperBaseProxy class.
     * @param proxyClassName Name of the wrapper proxy class to be generated.
     * @param wrapperInterfaces Interfaces implemented by the generated wrapper.
     * @param methods Array of methods for the proxy
     **/
    public static Class<?> generateEJBWrapperProxy(ClassLoader classLoader,
                                                   String proxyClassName,
                                                   Class<?>[] wrapperInterfaces,
                                                   Method[] methods, // F58064
                                                   ClassDefiner classDefiner)
                    throws ClassNotFoundException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "generateEJBWrapperProxy: " + proxyClassName);

        Class<?> rtnClass = classDefiner.findLoadedClass(classLoader, proxyClassName);

        if (rtnClass == null)
        {
            try
            {
                byte[] classbytes = EJBWrapperProxy.generateClassBytes(proxyClassName,
                                                                       wrapperInterfaces,
                                                                       methods);

                rtnClass = classDefiner.findLoadedOrDefineClass(classLoader, proxyClassName, classbytes);
            } catch (Throwable ex)
            {
                FFDCFilter.processException(ex, CLASS_NAME + ".generateEJBWrapperProxy", "525");
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "generateEJBWrapperProxy failed: " + ex.getMessage());
                throw new ClassNotFoundException(proxyClassName + " : " + ex.getMessage(), ex);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "generateEJBWrapperProxy: " + rtnClass);
        return rtnClass;
    }

    /**
     * TODO : javadoc
     **/
    public static Class<?> generate_Tie(ClassLoader classLoader,
                                        String remoteClassName,
                                        Class<?> remoteInterface,
                                        String beanName,
                                        ClassDefiner classDefiner,
                                        int rmicCompatible,
                                        boolean portableServer)
                    throws ClassNotFoundException, EJBConfigurationException
    {
        Class<?> rtnClass = null;
        String tieClassName = JIT_Tie.getTieClassName(remoteClassName);
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "generate_Tie: " + tieClassName + " : " + beanName);

        ArrayList<Method> compatibilityMethods = new ArrayList<Method>(); // d684761
        Method[] remoteMethods = remoteInterface.getMethods();
        String[] idlNames = getIdlMethodNames(remoteMethods,
                                              remoteInterface, // d576626
                                              compatibilityMethods); // d684761

        // Some problems have been found in the way the idlNames were formed,
        // so to support older Stubs (with the incorrect idlNames), the list
        // of idlNames will contain both the correct idlName and the old version
        // where the old version is added to the end of the list.
        // The corresponding method will have also been added to the
        // compatibilityMethods list... and these needed to be added to the
        // end of the list of remoteMethods.                               d684761
        if (compatibilityMethods.size() > 0)
        {
            int length = remoteMethods.length;
            Method[] allMethods = new Method[idlNames.length];
            System.arraycopy(remoteMethods, 0, allMethods, 0, length);
            for (Method extraMethod : compatibilityMethods)
            {
                allMethods[length++] = extraMethod;
            }
            remoteMethods = allMethods;
        }

        try
        {
            byte[] classbytes = JIT_Tie.generateClassBytes(tieClassName,
                                                           remoteClassName,
                                                           remoteInterface,
                                                           remoteMethods,
                                                           idlNames,
                                                           rmicCompatible,
                                                           portableServer);

            rtnClass = classDefiner.findLoadedOrDefineClass(classLoader, tieClassName, classbytes);
        } catch (EJBConfigurationException ejbex)
        {
            FFDCFilter.processException(ejbex, CLASS_NAME + ".generate_Tie",
                                        "110");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generate_Tie failed: " + ejbex.getMessage());

            throw ejbex;
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".generate_Tie",
                                        "119");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generate_Tie failed: " + ex.getMessage());

            throw new ClassNotFoundException(tieClassName + " : " + ex.getMessage(), ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "generate_Tie: " + rtnClass);

        return rtnClass;
    }

    /**
     * TODO : javadoc
     **/
    public static final Class<?> generateEJBHomeImplClass
                    (ClassLoader classLoader,
                     String homeClassName,
                     Class<?> remoteInterface,
                     Class<?> localInterface,
                     Class<?> ejbClass, // d369262.5
                     Class<?> pkeyClass,
                     HashMap<String, String> initMethods, // d369262.5
                     String beanName,
                     int beanType,
                     ClassDefiner classDefiner)
                                    throws ClassNotFoundException, EJBConfigurationException
    {
        Class<?> rtnClass = null;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "generateEJBHomeImplClass: " + homeClassName + " : " + beanName);

        try
        {
            byte[] classbytes = EJBHomeImpl.generateClassBytes(homeClassName,
                                                               remoteInterface,
                                                               localInterface,
                                                               ejbClass,
                                                               pkeyClass,
                                                               initMethods,
                                                               beanName,
                                                               beanType);

            rtnClass = classDefiner.findLoadedOrDefineClass(classLoader, homeClassName, classbytes);
        } catch (EJBConfigurationException ejbex)
        {
            FFDCFilter.processException(ejbex, CLASS_NAME + ".generateEJBHomeImplClass",
                                        "476");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generateEJBHomeImplClass failed: " + ejbex.getMessage());

            throw ejbex;
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".generateEJBHomeImplClass",
                                        "195");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generateEJBHomeImplClass failed: " + ex.getMessage());

            throw new ClassNotFoundException(homeClassName + " : " + ex.getMessage(), ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "generateEJBHomeImplClass: " + rtnClass);

        return rtnClass;
    }

    /**
     * Generates and loads an MDB proxy class. <p>
     * 
     * @param classLoader Class loader to use to define the class
     * @param proxyClassName name of the proxy class to be generated.
     * @param proxyInterfaceAndClass Interface implemented by the generated proxy,
     *            and the enterprise class.
     * @param wrapperType Type of wrapper to be generated (REMOTE, LOCAL, etc.)
     * @param ejbClassName Name of the EJB implementation class, that
     *            the generated proxy will route methods to.
     * @param allMethods Set of all methods for the proxy being generated.
     * @param methodInfos EJB method info objects for all of the methods
     *            from all interfaces of the type of wrapper being
     *            generated.
     * @param beanName Name of the EJB
     * @param classDefiner ClassDefiner object used to generate the Proxy class
     **/
    public static Class<?> generateMDBProxy(ClassLoader classLoader,
                                            String proxyClassName,
                                            Class<?>[] proxyInterfaceAndClass,
                                            EJBWrapperType wrapperType,
                                            String ejbClassName,
                                            Method[] allMethods,
                                            EJBMethodInfoImpl[] methodInfos,
                                            String beanName,
                                            ClassDefiner classDefiner)
                    throws ClassNotFoundException, EJBConfigurationException
    {
        Class<?> rtnClass = null;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "generateMDBProxy: " + proxyClassName);

        try
        {
            byte[] classbytes = EJBWrapper.generateClassBytes(proxyClassName,
                                                              proxyInterfaceAndClass,
                                                              wrapperType,
                                                              allMethods,
                                                              methodInfos,
                                                              ejbClassName,
                                                              beanName);

            rtnClass = classDefiner.findLoadedOrDefineClass(classLoader, proxyClassName, classbytes);
        } catch (EJBConfigurationException ejbex)
        {
            FFDCFilter.processException(ejbex, CLASS_NAME + ".generateMDBProxy",
                                        "505");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generateMDBProxy failed: " + ejbex.getMessage());

            throw ejbex;
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".generateMDBProxy",
                                        "513");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generateMDBProxy failed: " + ex.getMessage());

            throw new ClassNotFoundException(proxyClassName + " : " + ex.getMessage(), ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "generateMDBProxy: " + rtnClass);

        return rtnClass;
    }

    /**
     * Generates a WebService Endpoint EJB Proxy class. <p>
     * 
     * @param classLoader application class loader.
     * @param proxyClassName name of the proxy class to be generated.
     * @param proxyInterface Interface implemented by the generated proxy (optional).
     * @param proxyMethods Set of all WebService Endpoint methods that must
     *            be implemented by the proxy being generated.
     * @param methodInfos EJB method info objects for all of the
     *            WebService Endpoint methods for the type of
     *            proxy being generated.
     * @param ejbClassName Name of the EJB implementation class, that
     *            the generated proxy will route methods to.
     * @param beanName Name of the EJB (for messages).
     * 
     * @return an EJBProxy implementation for the specified WebService Endpoint.
     **/
    // d497921
    public static Class<?> generateWSEJBProxy(ClassLoader classLoader,
                                              String proxyClassName,
                                              Class<?> proxyInterface,
                                              Method[] proxyMethods,
                                              EJBMethodInfoImpl[] methodInfos,
                                              String ejbClassName,
                                              String beanName,
                                              ClassDefiner classDefiner)
                    throws ClassNotFoundException, EJBConfigurationException
    {
        Class<?> rtnClass = null;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "generateWSEJBProxy: " + proxyClassName + " : " + beanName);

        try
        {
            byte[] classbytes = WSEJBProxy.generateClassBytes(proxyClassName,
                                                              proxyInterface,
                                                              proxyMethods,
                                                              methodInfos,
                                                              ejbClassName,
                                                              beanName);

            rtnClass = classDefiner.findLoadedOrDefineClass(classLoader, proxyClassName, classbytes);
        } catch (EJBConfigurationException ejbex)
        {
            FFDCFilter.processException(ejbex, CLASS_NAME + ".generateWSEJBProxy",
                                        "536");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generateWSEJBProxy failed: " + ejbex.getMessage());

            throw ejbex;
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".generateWSEJBProxy",
                                        "545");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "generateWSEJBProxy failed: " + ex.getMessage());

            throw new ClassNotFoundException(proxyClassName + " : " + ex.getMessage(), ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "generateWSEJBProxy: " + rtnClass);

        return rtnClass;
    }

}
