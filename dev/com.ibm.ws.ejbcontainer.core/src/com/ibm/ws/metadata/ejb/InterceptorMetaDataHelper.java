/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import static com.ibm.ws.metadata.ejb.CheckEJBAppConfigHelper.isValidationFailable;
import static com.ibm.ws.metadata.ejb.CheckEJBAppConfigHelper.isValidationLoggable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.interceptor.InvocationContext;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.javaee.dd.common.InterceptorCallback;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.Interceptor;
import com.ibm.ws.javaee.dd.ejb.InterceptorBinding;
import com.ibm.ws.javaee.dd.ejb.InterceptorOrder;
import com.ibm.ws.javaee.dd.ejb.Interceptors;
import com.ibm.ws.javaee.dd.ejb.MessageDriven;
import com.ibm.ws.javaee.dd.ejb.NamedMethod;
import com.ibm.ws.javaee.dd.ejb.Session;

/**
 * Provides utility methods that are needed to create the EJB 3 interceptor metadata
 * from WCCM objects that are created by reading the ejb-jar.xml file in
 * a EJB 3 module.
 */
public class InterceptorMetaDataHelper
{
    private static final String CLASS_NAME = InterceptorMetaDataHelper.class.getName();

    private static final TraceComponent tc = Tr.register(InterceptorMetaDataHelper.class,
                                                         "EJB3Interceptors",
                                                         "com.ibm.ejs.container.container");

    private final static Class<?>[] PARM_TYPES = new Class[] { InvocationContext.class };
    private final static Class<?>[] NO_PARMS = null; //d461068

    /**
     * Populate the IntercetorBindingMap for the EJB module with the metadata from the
     * WCCM objects created from ejb-jar.xml file of the EJB module. See {@link com.ibm.ejs.csi.EJBModuleMetaDataImpl#ivInterceptorBindingMap} for details
     * about this map.
     * 
     * <dl>
     * <dt>pre-conditions:</dt>
     * <dd>populdateInterceptorMap must be called prior to this method.</dd>
     * </dl>
     * 
     * @param bindingList is the InterceptorBinding objects obtained from WCCM for the
     *            WCCM EJBJar object that represents metadata from ejb-jar.xml file.
     * 
     * @param map is the map object to be populated with metadata
     *            obtained from WCCM objects.
     * 
     * @throws EJBConfigurationException is thrown if configuration error is detected
     *             in the interceptor-binding.
     */
    public static void populateInterceptorBindingMap(List<InterceptorBinding> bindingList
                                                     , Map<String, List<EJBInterceptorBinding>> map)
                    throws EJBConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "populateInterceptorBindingMap");
        }

        // For each InterceptorBinding, create a map entry and update InterceptorsMap
        //
        for (InterceptorBinding ib : bindingList)
        {
            // Get EJBName from WCCM objects and the list of EJBInterceptorBinding
            // for this EJB from the map (which might be null if this is the first one).
            String ejbName = ib.getEjbName();
            List<EJBInterceptorBinding> ejbList = map.get(ejbName);

            // Get method name and parameters from WCCM objects.
            NamedMethod nm = ib.getMethod();
            List<String> parms = null;

            // Get method parameters if a NamedMethod is provided
            // by the InterceptorBinding.
            if (nm != null)
            {
                parms = nm.getMethodParamList();
            }

            // Get list of interceptor class names if there is no
            // InterceptorOrder in InterceptorBinding object since they
            // are required to be mutually exclusive by the xml schema.
            InterceptorOrder order = ib.getInterceptorOrder();
            List<String> interceptorClassNameList = null;
            List<String> interceptorOrderList = null;
            if (order == null)
            {
                interceptorClassNameList = ib.getInterceptorClassNames();
            }
            else
            {
                interceptorOrderList = order.getInterceptorClassNames();
            }

            // Create a EJBInterceptorBinding object from metadata obtained from WCCM.
            EJBInterceptorBinding binding;
            if (ejbName.equals("*"))
            {
                // Sytle 1 InterceptorBinding, which is for default interceptors.
                if (nm != null)
                {
                    EJBConfigurationException ecex;
                    ecex = new EJBConfigurationException("CNTR0239E: The method-name element is not valid in"
                                                         + " an interceptor-binding when EJB name is \"*\"");

                    // CNTR0239E: The method-name element is not valid for a style 1 interceptor-binding element.
                    Tr.error(tc, "METHOD_NAME_INVALID_FOR_DEFAULT_CNTR0239E");
                    throw ecex;
                }

                // Create EJBInterceptorBinding for default interceptors and
                // put it into the InterceptorBindingMap for the module.
                binding = new EJBInterceptorBinding(interceptorClassNameList, interceptorOrderList);
            }
            else if (nm == null)
            {
                // Sytle 2 InterceptorBinding, which is for class level interceptors
                // for a specified EJB. Create EJBInterceptorBinding and put it
                // into the InterceptorBindingMap for the module.
                binding = new EJBInterceptorBinding(ejbName, interceptorClassNameList, interceptorOrderList);
            }
            else if (parms == null)
            {
                // Sytle 3 InterceptorBinding, which is for method level interceptors
                // all methods of a specified EJB of a specified method name.
                // Create EJBInterceptorBinding and put it
                // into the InterceptorBindingMap for the module.
                String methodName = nm.getMethodName();
                binding = new EJBInterceptorBinding(ejbName, interceptorClassNameList, interceptorOrderList
                                , methodName, null);
            }
            else
            {
                // Sytle 4 InterceptorBinding, which is for method level interceptors
                // with a specified method signature of a specified EJB.
                // Create EJBInterceptorBinding and put it
                // into the InterceptorBindingMap for the module.
                String methodName = nm.getMethodName();
                binding = new EJBInterceptorBinding(ejbName, interceptorClassNameList, interceptorOrderList
                                , methodName, parms);
            }

            // Set exclude-default-interceptors only if set in ejb-jar.xml file.
            if (ib.isSetExcludeDefaultInterceptors())
            {
                binding.setExcludeDefaultInterceptors(ib.isExcludeDefaultInterceptors());
            }

            // Set exclude-class-interceptors only if set in ejb-jar.xml file.
            if (ib.isSetExcludeClassInterceptors())
            {
                binding.setExcludeClassLevelInterceptors(ib.isExcludeClassInterceptors());
            }

            // Add this binding to the EJB binding list if this is not the first
            // binding for the EJBName. Otherwise, create list and add to the map.
            if (ejbList != null)
            {
                ejbList.add(binding);
            }
            else
            {
                ejbList = new LinkedList<EJBInterceptorBinding>();
                ejbList.add(binding);
                map.put(ejbName, ejbList);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                binding.dump();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "populateInterceptorBindingMap");
        }
    }

    /**
     * Populate the IntercetorMap for the EJB module with the metadata from the
     * WCCM objects created from ejb-jar.xml file of the EJB module. See {@link com.ibm.ejs.csi.EJBModuleMetaDataImpl#ivInterceptorsMap} for details
     * about this map.
     * 
     * @param interceptors is the Interceptors object obtained from WCCM for the
     *            WCCM EJBJar object that represents metadata from ejb-jar.xml file.
     * 
     * @param interceptorsMap is the map object to be populated with metadata
     *            obtained from WCCM objects.
     * 
     * @return a map where the key is the fully qualified name of an interceptor class that is
     *         defined in the ejb-jar.xml file for the module and the value is the WCCM
     *         Interceptor object that is created for the interceptor class in the EJB module.
     * 
     * @throws EJBConfigurationException if unable to load an interceptor class,
     *             find one of its interceptor methods, or other configuration errors
     *             such as invalid method signature.
     */
    public static Map<String, Interceptor> // d468919
    populateInterceptorsMap
                    (ClassLoader classLoader
                     , Interceptors interceptors
                     , IdentityHashMap<Class<?>, EnumMap<InterceptorMethodKind, List<Method>>> interceptorsMap
                     , J2EEName name)
                                    throws EJBConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "populateInterceptorsMap");
        }

        // Get the list of Interceptor objects from the WCCM Interceptors object
        // passed as argument to this method.
        List<Interceptor> interceptorList = interceptors.getInterceptorList();
        HashMap<String, Interceptor> interceptorMap = new HashMap<String, Interceptor>(interceptorList.size()); // d468919

        // For each Interceptor object in the list.
        for (Interceptor interceptor : interceptorList)
        {
            String className = interceptor.getInterceptorClassName();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            {
                Tr.event(tc, "Loading EJB 3.0 Interceptor class: " + className);
            }
            Class<?> c;
            try
            {
                c = classLoader.loadClass(className);
            } catch (ClassNotFoundException ex)
            {
                FFDCFilter.processException(ex, CLASS_NAME + ".initializeInterceptorMD", "5352");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                {
                    Tr.event(tc, "Load of EJB 3.0 Interceptor class failed: " + className, ex);
                }
                EJBConfigurationException ecex;
                ecex = new EJBConfigurationException("CNTR0237E: The user-provided EJB 3.0 interceptor class \""
                                                     + className + "\" could not be found or loaded.", ex);

                // CNTR0237E: The user-provided enterprise bean level 3.0 {0} interceptor class cannot be found or loaded.
                Tr.error(tc, "INTERCEPTOR_CLASS_NOT_FOUND_CNTR0237E", new Object[] { className });
                throw ecex;
            }

            // Update the interceptor map with this WCCM Interceptor object.
            interceptorMap.put(className, interceptor); // d468919

            // Create a LIFO list of Class object where most generic super class of interceptor
            // is first out and the interceptor class itself is last out.
            LinkedList<Class<?>> lifoClasses = InterceptorMetaDataHelper.getLIFOSuperClassesList(c);

            // Create EnumMap for this Interceptor class and populate it with any
            // interceptor methods configured for this Interceptor class.
            EnumMap<InterceptorMethodKind, List<Method>> methodMap = new EnumMap<InterceptorMethodKind, List<Method>>(InterceptorMethodKind.class);

            // F743-17763 - Iterate over all interceptor kinds, and find the
            // interceptor methods defined in the deployment descriptor.
            for (InterceptorMethodKind kind : InterceptorMethodKind.values())
            {
                // Get list of methods and populate EnumMap with the list of Method
                // objects for these methods.
                List<? extends InterceptorCallback> methodMetaDataList = kind.getMethods(interceptor);

                if (methodMetaDataList != null && !methodMetaDataList.isEmpty())
                {
                    populateInterceptorMethodMap(c, lifoClasses, kind, PARM_TYPES, methodMetaDataList, methodMap, false, name);
                }
            }

            // Create a map entry for this Interceptor class.
            interceptorsMap.put(c, methodMap);

        } // end for each interceptor in list

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "populateInterceptorsMap");
        }

        return interceptorMap; // d468919
    }

    /**
     * Populate the interceptor map for an EJB or interceptor class. Each
     * mapping value is a List of Methods ordered by class hierarchy, with
     * methods from java.lang.Object appearing first, which is the order
     * required by the EJB specification.
     * 
     * @param c the class to inspect
     * @param lifoClasses the class hierarchy of c with java.lang.Object first
     * @param kind the interceptor kind to search for
     * @param parmTypes the parameter types of the interceptor method
     * @param methodMetaDataList a List of AroundInvokeMethod,
     *            AroundTimeoutMethod, or LifecycleCallbackType methods
     * @param methodMap the mapping to populate
     * @param ejbClass is true if the interceptor is defined on the enterprise bean class
     * @throws EJBConfigurationException if a configuration error is detected
     */
    private static void
                    populateInterceptorMethodMap
                    (Class<?> c
                     , LinkedList<Class<?>> lifoClasses
                     , InterceptorMethodKind kind
                     , Class<?>[] parmTypes
                     , List<? extends InterceptorCallback> methodMetaDataList
                     , Map<InterceptorMethodKind, List<Method>> methodMap // F743-17763
                     , boolean ejbClass
                     , J2EEName name)
                                    throws EJBConfigurationException
    {
        String className = c.getName();

        List<Method> methodList = new LinkedList<Method>();
        for (InterceptorCallback methodMetaData : methodMetaDataList)
        {
            // Get the method name and find the java reflection Method object
            // for this around-invoke method.
            String methodName = methodMetaData.getMethodName();

            Method m = findMethod(c, methodName, parmTypes);
            if (m == null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                {
                    Tr.event(tc, methodName + " not found in " + className
                                 + " or in any of it's super classes");
                }

                EJBConfigurationException ecex;
                ecex = new EJBConfigurationException("CNTR0238E: " + methodName
                                                     + " is not a " + kind.getXMLElementName() + " method of EJB interceptor class "
                                                     + className);

                // CNTR0238E: The {2} interceptor class specifies the {1} method, which is
                // not an {0} method of this class.
                Tr.error(tc, "INTERCEPTOR_METHOD_NOT_FOUND_CNTR0238E"
                         , new Object[] { methodName, kind.getXMLElementName(), className });
                throw ecex;
            }

            if (kind.isLifecycle())
            {
                InterceptorMetaDataHelper.validateLifeCycleSignatureExceptParameters(kind, kind.getXMLElementName(), m, ejbClass, name);
            }
            else
            {
                InterceptorMetaDataHelper.validateAroundSignature(kind, m, name);
            }

            // Add Method found to List of methods.
            methodList.add(m);
        }

        // Sort method list into a LIFO LinkedList where the most
        // generic super class method is first out and the interceptor
        // class method is last out. This is the invocation order
        // required by the EJB specification.
        LinkedList<Method> lifo = InterceptorMetaDataHelper.getLIFOMethodList(methodList, lifoClasses);

        methodMap.put(kind, lifo);
    }

    /**
     * Get a EnumMap<InterceptorMethodKind, List<Method>> for each of the
     * interceptor methods of a specified EJB that is either a Session or
     * MessageDriven bean from a EJB 3 or later module. The EnumMap key is
     * is a enum of InterceptorMethodKind that indicates the kind of
     * interceptor method (around-invoke, post-construct, post-activate,
     * pre-passivate, or pre-destroy) that is in the List of Method objects.
     * <p>
     * Each List<Method> created in the EnumMap is a LIFO list so that the
     * first Method out of the list is a method of the most generic superclass of
     * the EJB class and the last out is a method of the EJB class itself.
     * This is done so that the methods are invoked in the order required
     * by the EJB specification.
     * 
     * @param ejbClass is the Class object for the EJB.
     * 
     * @param bean is the WCCM Enterprise object for the EJB.
     * 
     * @param lifoClasses is a LIFO list of Class object where most generic super class
     *            of the EJB class is the first out and the EJB class itself is the last out.
     * 
     * @return the desired EnumMap.
     * 
     * @throws EJBConfigurationException is thrown if configuration error is detected.
     */
    public static EnumMap<InterceptorMethodKind, List<Method>> getEJBInterceptorMethods
                    (Class<?> ejbClass
                     , EnterpriseBean bean
                     , LinkedList<Class<?>> lifoClasses
                     , J2EEName name)
                                    throws EJBConfigurationException
    {
        String className = ejbClass.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getEJBInterceptorMethods for EJB class: " + className);
        }

        // Create empty EnumMap for the interceptor methods of this EJB.
        EnumMap<InterceptorMethodKind, List<Method>> methodMap;
        methodMap = new EnumMap<InterceptorMethodKind, List<Method>>(InterceptorMethodKind.class);

        // F743-17763 - Iterate over all interceptor kinds, and find the
        // interceptor methods defined in the deployment descriptor.
        for (InterceptorMethodKind kind : InterceptorMethodKind.values())
        {
            List<? extends InterceptorCallback> methodMetaDataList;
            if (bean.getKindValue() == EnterpriseBean.KIND_SESSION)
            {
                methodMetaDataList = kind.getMethods((Session) bean);
            }
            else
            {
                methodMetaDataList = kind.getMethods((MessageDriven) bean);
            }

            if (methodMetaDataList != null && !methodMetaDataList.isEmpty())
            {
                populateInterceptorMethodMap(ejbClass, lifoClasses,
                                             kind, kind.isLifecycle() ? NO_PARMS : PARM_TYPES,
                                             methodMetaDataList, methodMap, true, name);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "getEJBInterceptorMethods: " + methodMap);
        }
        return methodMap;
    }

    /**
     * Sorts a specified Method object list into a LIFO list where the
     * first out is the Method object of the most generic super class
     * of an interceptor class and the last out is a method of the
     * interceptor class itself.
     * 
     * @param methodList is the list of Method objects to be sorted.
     * 
     * @param lifoSuperClassesList is a LIFO list of Class objects where
     *            the first out is the most generic super class of an interceptor
     *            class and the last out is the interceptor class itself. Use the {@link #getLIFOSuperClassesList(Class)} method to obtain this list.
     * @return
     */
    public static LinkedList<Method>
                    getLIFOMethodList(List<Method> methodList, LinkedList<Class<?>> lifoSuperClassesList)
    {
        // Create LinkedList to be returned.
        LinkedList<Method> sortedList = new LinkedList<Method>();

        // For each Class object in sorted list of super classes.
        for (Class<?> c : lifoSuperClassesList)
        {
            // Break out of loop if Method object list is empty,
            // which most like became empty during last iteration
            // of this loop.
            if (methodList.isEmpty())
            {
                break;
            }

            // Create an Iterator over the Method object list and
            // iterate over the list.
            Iterator<Method> it = methodList.iterator();
            while (it.hasNext())
            {
                Method m = it.next();
                if (m.getDeclaringClass() == c)
                {
                    // This method is a method of the current Class object,
                    // so added to front of LIFO and remove it from Method
                    // object list so that it is not processed again.
                    sortedList.addFirst(m);
                    it.remove();
                }
            }
        }

        // Return the soreted LIFO list.
        return sortedList;
    }

    /**
     * Returns a Method object that reflects the specified method of a
     * specified EJB or interceptor class.
     * The methodName parameter is a String that specifies the simple name
     * of the desired method, and the parameterTypes parameter is an array
     * of Class objects that identify the method's formal parameter types in
     * the declared order. If more than one method with the same parameter
     * types is declared in a class, and one of these methods has a return
     * type that is more specific than any of the others, that method is returned;
     * otherwise one of the methods is chosen arbitrarily. If the specified
     * class is extended and the methodName exists in more than one class
     * of the inheritance tree, then the subclass method is returned rather
     * than the method in the more generic class.
     * 
     * @param c is the class to be searched.
     * 
     * @param methodName is the name of the desired method.
     * 
     * @param parmTypes is the array of Class objects for the desired method or
     *            null reference if method takes no parameters.
     * 
     * @return java reflection Method object for the desired method or a null
     *         reference if desired method is NOT found.
     */
    public static Method findMethod(final Class<?> c, String methodName, final Class<?>[] parmTypes)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "findMethod", new Object[] { c, methodName, Arrays.toString(parmTypes) });
        }

        // Start with specified class.
        Class<?> classObject = c;

        // Use java reflection to locate method until Method is found
        // or we have reflected entire inheritance tree.
        Method m = null;
        while (classObject != null && m == null) //d461068
        {
            try
            {
                // Attempt to find in current Class object.
                m = classObject.getDeclaredMethod(methodName, parmTypes);
            } catch (NoSuchMethodException e)
            {
                // Look in super class if not found.
                classObject = classObject.getSuperclass();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    if (classObject != null) //d461068
                    {
                        Tr.debug(tc, "searching superclass: " + classObject.getName());
                    }
                    else
                    {
                        Tr.debug(tc, methodName + " was not found");
                    }
                }
            }
        }

        // Return Method object or null if not found.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "findMethod returning: " + m);
        }
        return m;
    }

    /**
     * Create a LIFO LinkedList of Class objects starting with a specified
     * interceptor class object itself and then each of its the super classes.
     * A LIFO is used so that the interceptor methods in the most general
     * superclass are invoked first as required by the EJB specification.
     * 
     * @param interceptorClass is the Class object of the interceptor class.
     * 
     * @return the desired LIFO LinkedList of Class objects.
     */
    public static LinkedList<Class<?>> getLIFOSuperClassesList(Class<?> interceptorClass)
    {
        LinkedList<Class<?>> supers = new LinkedList<Class<?>>();
        supers.addFirst(interceptorClass);
        Class<?> interceptorSuperClass = interceptorClass.getSuperclass();
        while (interceptorSuperClass != null && interceptorSuperClass != java.lang.Object.class) // d469514
        {
            supers.addFirst(interceptorSuperClass);
            interceptorSuperClass = interceptorSuperClass.getSuperclass();
        }

        return supers;
    }

    /**
     * Verify a specified AroundInvoke interceptor method has correct method
     * modifiers and signature.
     * 
     * @param kind the interceptor kind
     * @param m is the java reflection Method object for the around invoke method.
     * @param ejbClass is true if the interceptor is defined on the enterprise bean class
     * @param name is the {@link J2EEName} of the EJB.
     * @throws EJBConfigurationException is thrown if any configuration error is detected.
     */
    public static void validateAroundSignature(InterceptorMethodKind kind, Method m, J2EEName name)
                    throws EJBConfigurationException
    {

        // Get the modifers for the interceptor method and verify that the
        // method is neither final nor static as required by EJB specification.
        int mod = m.getModifiers();
        if (Modifier.isFinal(mod) || Modifier.isStatic(mod))
        {
            // CNTR0229E: The {0} interceptor method must not be declared as final or static.
            String method = m.toGenericString();
            Tr.error(tc, "INVALID_INTERCEPTOR_METHOD_MODIFIER_CNTR0229E", new Object[] { method });
            throw new EJBConfigurationException(kind.getXMLElementName() + " interceptor \"" + method
                                                + "\" must not be declared as final or static.");
        }

        // Verify AroundInvoke has 1 parameter of type InvocationContext.
        Class<?>[] parmTypes = m.getParameterTypes();
        if ((parmTypes.length == 1) && (parmTypes[0].equals(InvocationContext.class)))
        {
            // Has correct signature of 1 parameter of type InvocationContext
        }
        else
        {
            String method = m.toGenericString();
            Tr.error(tc, "INVALID_AROUND_INVOKE_SIGNATURE_CNTR0230E", new Object[] { method, kind.getXMLElementName() }); // F743-17763.1
            throw new EJBConfigurationException(kind.getXMLElementName() + " interceptor \"" + method
                                                + "\" must have a single parameter of type javax.interceptors.InvocationContext.");
        }

        // Verify return type.
        Class<?> returnType = m.getReturnType();

        // F743-17763.1 - The spec requires that around interceptor methods have
        // exactly Object as the return type.  The original AroundInvoke check
        // was not as strict, but we keep it for compatibility with previous
        // releases.
        if (returnType != Object.class)
        {
            boolean compatiblyValid = kind == InterceptorMethodKind.AROUND_INVOKE &&
                                      Object.class.isAssignableFrom(returnType); // d668039
            if (!compatiblyValid || isValidationLoggable())
            {
                String method = m.toGenericString();
                Tr.error(tc, "INVALID_AROUND_INVOKE_SIGNATURE_CNTR0230E", new Object[] { method, kind.getXMLElementName() }); // F743-17763.1
                if (!compatiblyValid || isValidationFailable()) // d668039
                {
                    throw new EJBConfigurationException(kind.getXMLElementName() + " interceptor \"" + method
                                                        + "\" must have a return value of type java.lang.Object.");
                }
            }
        }

        // Per the EJB 3.2 spec, "Note: An around-invoke interceptor method may
        // be declared to throw any checked exceptions that the associated
        // target method allows within its throws clause. It may be declared to
        // throw the java.lang.Exception if it interposes on several methods
        // that can throw unrelated checked exceptions."
    }

/**
    * Verify that a specified life cycle event interceptor method has correct
    * method modifiers, return type, and exception types on throws clause.
    * Note, the parameter types are not verified since the caller already ensured
    * parameter types is correct for the method.
    * Use the {@link #validateLifeCycleSignature(String, Method, boolean)
    * method if you need entire signature validated.
    *
    * @param kind is the interceptor kind
    * 
    * @param lifeCycle is a string that identifies the type of life cycle event callback.
    *
    * @param m is the java reflection Method object for the life cycle interceptor method.
    * 
    * @param ejbClass is true if the interceptor is defined on the enterprise bean class.
    * 
    * @param name is the {@link J2EEName} of the EJB.
    *
    * @throws EJBConfigurationException is thrown if any configuration error is detected.
    */
    public static void validateLifeCycleSignatureExceptParameters(InterceptorMethodKind kind,
                                                                  String lifeCycle,
                                                                  Method m,
                                                                  boolean ejbClass,
                                                                  J2EEName name)
                    throws EJBConfigurationException
    {
        validateLifeCycleSignatureExceptParameters(kind, lifeCycle, m, ejbClass, name, InterceptorMethodKind.isInterceptor1_2());
    }

    /**
     * Method for unittesting only
     */
    static void validateLifeCycleSignatureExceptParameters(InterceptorMethodKind kind,
                                                           String lifeCycle, Method m,
                                                           boolean ejbClass,
                                                           J2EEName name,
                                                           boolean isInterceptor1_2)
                    throws EJBConfigurationException
    {

        if (kind == InterceptorMethodKind.AROUND_CONSTRUCT && ejbClass)
        {
            String method = m.toGenericString();
            Tr.error(tc, "INVALID_AROUND_CONSTRUCT_DEFINITION_CNTR0249E",
                     new Object[] { name.getComponent(), name.getModule(), name.getApplication(), method });
            throw new EJBConfigurationException("CNTR0249E: The "
                                                + name.getComponent()
                                                + " enterprise bean in the "
                                                + name.getModule()
                                                + " module in the "
                                                + name.getApplication()
                                                + " application specifies the @AroundConstruct annotation on the "
                                                + method
                                                + " method, but this annotation can only be used by interceptor classes.");
        }

        // Get the modifers for the interceptor method and verify that it
        // is neither final nor static as required by EJB specification.
        int mod = m.getModifiers();
        if (Modifier.isFinal(mod) || Modifier.isStatic(mod))
        {
            // CNTR0229E: The {0} interceptor method must not be declared as final or static.
            String method = m.toGenericString();
            Tr.error(tc, "INVALID_INTERCEPTOR_METHOD_MODIFIER_CNTR0229E", new Object[] { method });
            throw new EJBConfigurationException(lifeCycle + " interceptor \"" + method
                                                + "\" must not be declared as final or static.");
        }

        // Verify return type is void or Object.
        Class<?> returnType = m.getReturnType();
        if (returnType == java.lang.Void.TYPE || (isInterceptor1_2 && returnType == Object.class))
        {
            // Return type is void as required.
        }
        else
        {
            // CNTR0231E: The {0} method signature is not valid as
            // a {1} method of an enterprise bean class.
            String method = m.toGenericString();
            Tr.error(tc, "INVALID_LIFECYCLE_SIGNATURE_CNTR0231E", new Object[] { method, lifeCycle });
            throw new EJBConfigurationException(lifeCycle + " interceptor \"" + method
                                                + "\" must have void as return type.");
        }

        // Per the EJB 3.2 spec, "Note: A lifecycle callback interceptor method
        // must not throw application exceptions, but it may be declared to
        // throw checked exceptions including the java.lang.Exception if the
        // same interceptor method interposes on business or timeout methods in
        // addition to lifecycle events. If a lifecycle callback interceptor
        // method returns a value, it is ignored by the container.
    }

/**
    * Verify that a specified life cycle event interceptor method has correct
    * method modifiers, parameter types, return type, and exception types for
    * the throws clause.  Note, if parameter types is known to be valid,
    * then use the {@link #validateLifeCycleSignatureExceptParameters(String, Method)
    * method of this class to skip parameter type validation.
    *
    * @param lifeCycle is a string that identifies the type of life cycle event callback.
    *
    * @param m is the java reflection Method object for the life cycle interceptor method.
    *
    * @param ejbClass must be boolean true if the m is a method of the EJB class.
    *        If m is a method of an interceptor or a super class of the EJB class,
    *        then boolean false must be specified.
    *
    * @throws EJBConfigurationException is thrown if any configuration error is detected.
    */
    public static void validateLifeCycleSignature(InterceptorMethodKind kind, String lifeCycle, Method m, boolean ejbClass, J2EEName name)
                    throws EJBConfigurationException
    {
        // Validate method signature except for the parameter types,
        // which is done by this method.
        validateLifeCycleSignatureExceptParameters(kind, lifeCycle, m, ejbClass, name); // d450431

        // Now verify method parameter types.
        Class<?>[] parmTypes = m.getParameterTypes();
        if (ejbClass)
        {
            // This is EJB class, so interceptor should have no parameters.
            if (parmTypes.length != 0)
            {
                // CNTR0231E: "{0}" interceptor method "{1}" signature is incorrect.
                String method = m.toGenericString();
                Tr.error(tc, "INVALID_LIFECYCLE_SIGNATURE_CNTR0231E", new Object[] { method, lifeCycle });
                throw new EJBConfigurationException(lifeCycle + " interceptor \"" + method
                                                    + "\" must have zero parameters.");
            }
        }
        else
        {
            // This is an interceptor class, so InvocationContext is a required parameter
            // for the interceptor method.
            if ((parmTypes.length == 1) && (parmTypes[0].equals(InvocationContext.class)))
            {
                // Has correct signature of 1 parameter of type InvocationContext
            }
            else
            {
                // CNTR0232E: The {0} method does not have the required
                // method signature for a {1} method of a interceptor class.
                String method = m.toGenericString();
                Tr.error(tc, "INVALID_LIFECYCLE_SIGNATURE_CNTR0232E", new Object[] { method, lifeCycle });
                throw new EJBConfigurationException("CNTR0232E: The \"" + method
                                                    + "\" method does not have the required method signature for a \""
                                                    + lifeCycle + "\" method of a interceptor class.");
            }
        }
    }

    /**
     * Determine if a specified method is overridden by one of it's subclasses.
     * 
     * @param m is the Method object for the method.
     * 
     * @param supers is a LinkedList created by the {@link #getLIFOSuperClassesList(Class)} method of this class. This list has the most general class first in the list
     *            and the most specific subclass last in the list.
     * 
     * @return true if and only if the method is overridden by a method in the subclass.
     */
    // d469514 - added entire method.
    public static boolean isMethodOverridden(Method m, LinkedList<Class<?>> supers)
    {
        // Only check non-private methods for an override.
        int methodModifier = m.getModifiers();
        if (!Modifier.isPrivate(methodModifier))
        {
            // Not a private method, so we need to check if it is overridden.
            int startIndex = supers.indexOf(m.getDeclaringClass()) + 1;
            if (startIndex < supers.size())
            {
                String name = m.getName();
                Class<?>[] pTypes = m.getParameterTypes();
                for (ListIterator<Class<?>> lit = supers.listIterator(startIndex); lit.hasNext();)
                {
                    Class<?> subClass = lit.next();
                    try
                    {
                        subClass.getDeclaredMethod(name, pTypes);
                        return true;
                    } catch (NoSuchMethodException e)
                    {
                        //FFDCFilter.processException( e, "", "", m);
                        // This is normal, it just means the method was not overridden
                        // by a method in the current subclass being processed.
                    }
                }
            }
        }

        // The method is not overridden either because it is a private method,
        // or there is no subclass that overrides the method.
        return false;
    }
}
