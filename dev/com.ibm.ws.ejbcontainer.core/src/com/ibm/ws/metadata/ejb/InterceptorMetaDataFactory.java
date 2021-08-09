/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.LifecycleInterceptorWrapper;
import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.container.interceptors.InterceptorProxy;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.JCDIHelper;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.managedobject.ManagedObjectFactory;

/**
 * InterceptorMetaDataFactory is used to create a InterceptorMetaData
 * object for an EJB 3 SessionBean or MessageDrivenBean. The static
 * createInterceptorMetaData method is invoked in this class to create the
 * interceptorMetaData object required for a specified EJB class of a specified
 * EJB module. A null reference is returned if the specified EJB class is one
 * that does not require any EJB 3 interceptor methods to ever be invoked.
 *
 * @see #createInterceptorMetaData(BeanMetaData, Map)
 */
public class InterceptorMetaDataFactory
{
    private static final TraceComponent tc = Tr.register(InterceptorMetaDataFactory.class, "EJB3Interceptors", "com.ibm.ejs.container.container");

    /**
     * BeanMetaData object for the EJB.
     */
    final private BeanMetaData ivBmd;

    /**
     * The class object of the EJB being processed.
     */
    final private Class<?> ivEjbClass;

    /**
     * The J2EEName for the EJB being processed.
     */
    final private J2EEName ivJ2EEName;

    /**
     * EJB name of the bean being processed.
     */
    final private String ivEjbName;

    /**
     * ClassLoader to use for this EJB.
     */
    final private ClassLoader ivClassLoader;

    /**
     * The module meta data for this EJB.
     */
    private EJBModuleMetaDataImpl ivEJBModuleMetaDataImpl;

    /**
     * List of fully qualified class-level interceptor names.
     */
    final private ArrayList<String> ivClassInterceptorNames = new ArrayList<String>();

    /**
     * List of class-level interceptors in the order that they should be
     * invoke. See {@link orderClassLevelInterceptors}.
     */
    private ArrayList<String> ivClassInterceptorOrder; // d630717

    /**
     * List of fully qualified default interceptor names.
     */
    final private ArrayList<String> ivDefaultInterceptorNames = new ArrayList<String>();

    /**
     * JCDI 'First' Interceptor class, if the module is CDI enabled.
     */
    // F743-29169
    private Class<?> ivJCDIFirstInterceptorClass;

    /**
     * JCDI 'Last' Interceptor class, if the module is CDI enabled.
     */
    // F743-15628
    private Class<?> ivJCDILastInterceptorClass;

    /**
     * Map of interceptor names to class object for the interceptor.
     */
    final private HashMap<String, Class<?>> ivInterceptorNameToClassMap = new HashMap<String, Class<?>>();

    /**
     * List of interceptor binding objects from WCCM.
     */
    private List<EJBInterceptorBinding> ivInterceptorBinding; // d367572.9

    /**
     * Interceptor binding object for the module.
     */
    private EJBInterceptorBinding ivModuleInterceptorBinding;

    /**
     * Interceptor binding object for this EJB.
     */
    private EJBInterceptorBinding ivClassInterceptorBinding;

    /**
     * Set to true if default interceptors are to be excluded or disabled
     * at the class level (which includes all the methods of the EJB).
     */
    private boolean ivExcludeDefaultFromClassLevel;

    /**
     * List of interceptor classes.
     */
    final private ArrayList<Class<?>> ivInterceptorClasses = new ArrayList<Class<?>>();

    /**
     * A map of interceptor classes to a map of interceptor kinds to a list of
     * interceptor proxies. When an entry is added to this map, the class key
     * must be added to ivInterceptorClasses, and all associated IntercetorProxy
     * objects must use that index.
     */
    final private Map<Class<?>, Map<InterceptorMethodKind, List<InterceptorProxy>>> ivInterceptorProxyMaps =
                    new HashMap<Class<?>, Map<InterceptorMethodKind, List<InterceptorProxy>>>(); // d630717

    /**
     * A map of interceptor kinds to a list of interceptor proxies for the bean
     * class. This map is not added to ivInterceptorProxyMaps because the
     * rules for a interceptor methods on the bean class are different from the
     * rules of an arbitrary interceptor class.
     */
    private Map<InterceptorMethodKind, List<InterceptorProxy>> ivBeanInterceptorProxyMap; // d630717

    // F743-1751
    /**
     * The list of Method objects for lifecycle interceptors declared on the
     * bean class directly. This field is non-null only for beans that require
     * a defined transaction context during lifecycle interceptors. The size is {@link LifecycleInterceptorWrapper.NUM_METHODS}, and an entry is non-null
     * only if the relevant method is declared on the bean class itself.
     */
    private Method[] ivBeanLifecycleMethods;

    /**
     * A map of java.lang.reflect.Method object to a ArrayList EJBMethodInfo.
     * One entry per business method of the EJB and one EJBMethodInfo object
     * in list for each interface that business method appears in. The Method
     * object must be the Method object of the EJB class, not the businees interface.
     */
    private Map<Method, ArrayList<EJBMethodInfoImpl>> ivEJBMethodInfoMap; // d430356

    /**
     * A map of method name to style 3 InterceptorBinding. One entry per
     * style 3 InterceptorBinding in ejb-jar.xml file for this module.
     */
    final private HashMap<String, EJBInterceptorBinding> ivStyle3InterceptorBindingMap = new HashMap<String, EJBInterceptorBinding>();

    /**
     * A map of method signature to style 4 InterceptorBinding. One entry per
     * style 4 InterceptorBinding in ejb-jar.xml file for this module.
     */
    final private HashMap<String, EJBInterceptorBinding> ivStyle4InterceptorBindingMap = new HashMap<String, EJBInterceptorBinding>();

    /**
     * Set to true if bean implements MessageDriven or SessionBean
     * component interface.
     */
    boolean ivHasComponentInterface;

    /**
     * Set to true if bean is a MDB.
     */
    boolean ivMDB;

    /**
     * Set to true if bean is a SLSB.
     */
    boolean ivSLSB;

    /**
     * Set to true if bean is a SFSB.
     */
    boolean ivSFSB;

    /**
     * Set to true if bean is a ManagedBean.
     */
    // F743-34301
    boolean ivMB;

    /**
     * Set to true if ejb-jar.xml has metadata-complete attribute set to true,
     * indicating we should skip processing annotations.
     */
    private boolean ivMetadataComplete; // d367572.9

    /**
     * Create InterceptorMetaData instance to use for a specified EJB
     * module and bean meta data object. Also, update EJBMethodInfoImpl
     * object with method level interceptor data as needed.
     *
     * <dl>
     * <dt>pre-conditions:</dt>
     * <dd>bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0, which means
     * this method must only be called for EJB 3 module versions or later.</dd>
     * <dd>bmd.isEntityBean() == false, which means this method must only
     * only be called for non-EntityBeans in a EJB 3 module version or later.</dd>
     * </dl>
     *
     * @param bmd is the BeanMetaData object for the EJB.
     *
     * @param methodInfoMap is a Map of java reflection Method object
     *            to a list of EJBMethodInfoImpl objects for the method. The Method
     *            object must be a Method object from the EJB class rather than
     *            a Method object obtained from one of the interfaces implemented
     *            by the EJB class. The list of EJBMethodInfoImpl objects represents
     *            the list of interface methods that is available to application
     *            when invoking a method implemented by EJB class.
     *
     * @return InterceptorMetaData object if this EJB has atleast 1 interceptor
     *         method (either around invoke or lifecycle callback). A null reference
     *         is returned if an interceptor method never needs to be called
     *         for this EJB.
     *
     * @throws EJBConfigurationException is thrown if an error is detected in the
     *             interceptor configuration for this EJB.
     */
    static public InterceptorMetaData createInterceptorMetaData
                    (EJBMDOrchestrator ejbMDOrchestrator
                     , BeanMetaData bmd
                     , final Map<Method, ArrayList<EJBMethodInfoImpl>> methodInfoMap) // d430356
                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "createInterceptorMetaData");
        }

        // Pre-condition ensures this method is only called for EJB 3 module and
        // for EJBs that are not a CMP or BMP.
        EJBModuleMetaDataImpl mmd = (EJBModuleMetaDataImpl) bmd.getModuleMetaData();

        // Create a factory instance to use for creating InterceptorMetaData.
        InterceptorMetaDataFactory factory = new InterceptorMetaDataFactory(mmd, bmd);

        // Initialize the ivEJBMethodInfoMap with an entry for each
        // business method of this EJB.
        factory.ivEJBMethodInfoMap = methodInfoMap; // d430356

        // Validate and initialize for processing any InterceptorBinding
        // objects from WCCM.
        if (factory.ivInterceptorBinding != null) // d367572.9
        {
            factory.validateInterceptorBindings();
        }

        // Determine whether default interceptors need to be excluded
        // for this EJB.
        factory.setExcludeDefaultInterceptors();

        // Add the list of default interceptors to the interceptor name to
        // class object map (ivInterceptorNameToClassMap).
        factory.addDefaultInterceptors();

        // Add the list of class-level interceptors to the interceptor name to
        // class object map (ivInterceptorNameToClassMap).
        factory.addClassLevelInterceptors();

        // Determine the ordering of interceptors to be used at class-level for this EJB.
        // This list is the default list to use for all business methods of this
        // EJB if no method-level annotation or interceptor binding exists for
        // the method.
        factory.ivClassInterceptorOrder = factory.orderClassLevelInterceptors();

        // If the module is CDI enabled, then add the interceptor provided by
        // the JCDI service to the class object map (ivInterceptorNameToClassMap).
        factory.addJCDIInterceptors(); // F743-15628

        // Process interceptor methods on the bean class.  This must be called
        // prior to calling getInterceptorProxies below (directly or indirectly
        // via updateEJBMethodInfoInterceptorProxies).
        factory.processBeanInterceptors(); // d630717

        // Process all business methods of the EJB and update any EJBMethodInfo
        // object for any business method that requires one or more around-invoke
        // or around-timeout interceptor methods to be invoked.
        factory.updateEJBMethodInfoInterceptorProxies(); // d386227

        // d630717 - Process all lifecycle interceptor classes of the EJB.  The
        // first call will populate ivInterceptorProxyMaps, and the subsequent
        // calls will simply concatenate the lists.
        InterceptorProxy[] postConstructProxies = factory.getInterceptorProxies(InterceptorMethodKind.POST_CONSTRUCT, factory.ivClassInterceptorOrder);
        InterceptorProxy[] preDestroyProxies = factory.getInterceptorProxies(InterceptorMethodKind.PRE_DESTROY, factory.ivClassInterceptorOrder);
        InterceptorProxy[] prePassivateProxies = factory.getInterceptorProxies(InterceptorMethodKind.PRE_PASSIVATE, factory.ivClassInterceptorOrder);
        InterceptorProxy[] postActivateProxies = factory.getInterceptorProxies(InterceptorMethodKind.POST_ACTIVATE, factory.ivClassInterceptorOrder);
        InterceptorProxy[] aroundConstructProxies = factory.getInterceptorProxies(InterceptorMethodKind.AROUND_CONSTRUCT, factory.ivClassInterceptorOrder);

        // d630717 - Finally, if the bean has interceptor methods or if
        // getInterceptorProxies added an interceptor class, then construct
        // InterceptorMetaData to return.
        InterceptorMetaData imd = null;
        if (!factory.ivBeanInterceptorProxyMap.isEmpty() || !factory.ivInterceptorClasses.isEmpty())
        {
            Class<?>[] classes = factory.ivInterceptorClasses.toArray(new Class<?>[0]);

            // Get the managed object factories for each interceptor class.  F87720
            ManagedObjectFactory<?>[] managedObjectFactories = null;
            for (int i = 0; i < classes.length; i++)
            {
                ManagedObjectFactory<?> managedObjectFactory = ejbMDOrchestrator.getInterceptorManagedObjectFactory(bmd, classes[i]);
                if (managedObjectFactory != null)
                {
                    if (managedObjectFactories == null)
                    {
                        managedObjectFactories = new ManagedObjectFactory[classes.length];
                    }
                    managedObjectFactories[i] = managedObjectFactory;
                }
            }

            imd = new InterceptorMetaData(classes
                            , managedObjectFactories
                            , aroundConstructProxies
                            , postConstructProxies
                            , postActivateProxies
                            , prePassivateProxies
                            , preDestroyProxies
                            , factory.ivBeanLifecycleMethods // F743-1751
            );
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "createInterceptorMetaData returning: " + imd);
        }
        return imd;
    }

    /**
     * Validate InterceptorBinding objects from WCCM and save references to
     * each InterceptorBinding needed by this EJB to enable quick access to them
     * when needed. Note, this method must only be called if there are EJBInterceptorBinding
     * objects to validate (e.g. ejb-jar.xml contains <interceptor-binding> stanzas).
     * <p>
     * The validation done ensures that if there is more than one interceptor binding for
     * a given EJB class or a given method of the EJB, then we verify that the merger of
     * the multiple bindings is something that is possible to obtain if annotations were
     * used instead of xml. If not possible to use annotation that is equivalent to the
     * merged result, then an error is logged and a EJBConfigurationException is thrown.
     *
     * @throws EJBConfigurationException is thrown if an invalid InterceptorBinding is detected.
     */
    // d367572.9 update for xml processing.
    private void validateInterceptorBindings() throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "validateInterceptorBindings");
        }

        // d630872 - Build a list of methods that can be the target of an
        // interceptor.  This includes all business methods (AroundInvoke) and
        // all timer methods (AroundTimeout).
        List<Method> interceptableMethods = new ArrayList<Method>(ivEJBMethodInfoMap.keySet());
        if (ivBmd.timedMethodInfos != null) // d630872.1
        {
            for (EJBMethodInfoImpl info : ivBmd.timedMethodInfos)
            {
                interceptableMethods.add(info.getMethod());
            }
        }

        // For each InterceptorBinding from WCCM.
        for (EJBInterceptorBinding binding : ivInterceptorBinding)
        {
            String ejbName = binding.getEJBName();
            EJBInterceptorBinding.BindingStyle bindingStyle = binding.getBindingStyle();
            if (ejbName.equals("*"))
            {
                // Style 1 binding. Is this the first style 1 InterceptorBinding?
                if (ivModuleInterceptorBinding == null)
                {
                    // Save reference to module-level binding for future use.
                    ivModuleInterceptorBinding = binding;
                }
                else
                {
                    // CNTR0245E: The {0} module of the {1) application has more than one style 1 interceptor-binding
                    // in the deployment descriptor. Only one style 1 interceptor-binding is allowed.

                    J2EEName j2eeName = ivEJBModuleMetaDataImpl.getJ2EEName();
                    String message = "The " + j2eeName.getModule() + " module of the " + j2eeName.getApplication()
                                     + " application has more than one style 1 interceptor-binding in the deployment"
                                     + " deployment descriptor. Only one style 1 interceptor-binding is allowed";
                    Tr.error(tc, "DUPLICATE_STYLE_1_INTERCEPTOR_BINDING_CNTR0245E"
                             , new Object[] { j2eeName.getModule(), j2eeName.getApplication() }); //d472939
                    throw new EJBConfigurationException(message); // d463727
                }
            }
            else
            {
                // Binding not for the module, so only process those interceptor bindings that are
                // for the EJB class specified by the caller.
                if (ejbName.equals(ivEjbName))
                {
                    if (bindingStyle == EJBInterceptorBinding.BindingStyle.STYLE2)
                    {
                        // This is a style 2 interceptor-binding that applies to the class.
                        // Is this the first style 2 interceptor binding for this EJB?
                        if (ivClassInterceptorBinding == null)
                        {
                            // Yep, save reference style 2 for future use.
                            ivClassInterceptorBinding = binding;
                        }
                        else
                        {
                            // Nope, then we need to validate and merge second style 2
                            // with the first style 2 interceptor binding.
                            EJBInterceptorBinding binding1 = ivClassInterceptorBinding; // d457352
                            ivClassInterceptorBinding = validateAndMergeStyle2Bindings(binding1, binding); // d457352
                        }
                    }
                    else if (bindingStyle == EJBInterceptorBinding.BindingStyle.STYLE3)
                    {
                        // This is a style 3 interceptor-binding that applies to all business
                        // methods with the method name specified by method-name element.
                        // Put style 3 interceptor binding into the map.
                        String methodName = binding.getMethodName();
                        EJBInterceptorBinding oldBinding = ivStyle3InterceptorBindingMap.put(methodName, binding);

                        // Did the put replace an existing map entry?
                        if (oldBinding != null)
                        {
                            // Yep, tthis was NOT the first style 3 binding for this EJB.  We need to validate and
                            // merge second style 3 with the first style 3 interceptor binding.
                            EJBInterceptorBinding merged;
                            merged = validateAndMergeStyle3Or4Bindings(methodName, null, oldBinding, binding); // d457352

                            // No exception was thrown, so merger is valid. Replace map entry with the merged binding.
                            ivStyle3InterceptorBindingMap.put(methodName, merged); // d457352
                        }
                        else
                        {
                            // Nope, this must be the first style 3 binding for this EJB. We must
                            // verify there is at least 1 business method exist by this method name.
                            boolean notFound = true;
                            for (Method m : interceptableMethods) // d630872
                            {
                                // Does method name match?
                                if (m.getName().equals(methodName))
                                {
                                    // Yep, indicate we found a business method and exit the loop.
                                    notFound = false;
                                    break;
                                }
                            }

                            // Log error and throw exception if business method was not found.
                            if (notFound)
                            {
                                if (isTraceOn && tc.isDebugEnabled())
                                {
                                    Tr.debug(tc, "method not found for interceptor binding");
                                    binding.dump();
                                }

                                // CNTR0244E: The {0} method name is not found in one of the business interfaces
                                // of the {1} enterprise bean. A style {2} interceptor-binding element requires
                                // the method to be a business method of the enterprise bean.
                                String j2eeName = ivJ2EEName.toString();
                                Tr.error(tc, "BUSINESS_METHOD_NOT_FOUND_FOR_INTERCEPTOR_BINDING_CNTR0244E"
                                         , new Object[] { methodName, j2eeName, "3" }); //d472939

                                String message = "The " + methodName + " method name is not found "
                                                 + " in one of the business interfaces of the " + j2eeName
                                                 + " enterprise bean. A style 3 interceptor-binding element requires"
                                                 + " the method to be a business method of the enterprise bean";
                                throw new EJBConfigurationException(message); // d463727
                            }
                        }
                    }
                    else if (bindingStyle == EJBInterceptorBinding.BindingStyle.STYLE4)
                    {
                        // This is a style 4 interceptor-binding that applies to a business method
                        // with a specific method signature. Verify it does not duplicate a style 3 binding.
                        String methodName = binding.getMethodName();
                        List<String> methodParms = binding.getMethodParms();
                        String signature = methodSignature(methodName, methodParms);
                        signature = normalizeSignature(signature.trim());

                        // d472972 start
                        // Put style 4 interceptor binding into the map.
                        EJBInterceptorBinding oldBinding = ivStyle4InterceptorBindingMap.put(signature, binding);

                        // Did the put replace an existing map entry?
                        if (oldBinding != null)
                        {
                            // Yep, this was NOT the first style 4 binding for this EJB method.  We need to validate and
                            // merge second style 4 with the first style 4 interceptor binding.
                            EJBInterceptorBinding merged;
                            merged = validateAndMergeStyle3Or4Bindings(methodName, signature, oldBinding, binding); // d457352

                            // No exception was thrown, so merger is valid. Replace map entry with the merged binding.
                            if (isTraceOn && tc.isDebugEnabled())
                            {
                                Tr.debug(tc, "replaced style 4 for method signature: " + signature);
                            }
                            ivStyle4InterceptorBindingMap.put(signature, merged);
                        }
                        else
                        {
                            // This is first style 4 binding for this EJB method.
                            // Verify there is a business method by this method name and signature.
                            if (isTraceOn && tc.isDebugEnabled())
                            {
                                Tr.debug(tc, "added style 4 for method signature: " + signature);
                            }
                            boolean notFound = true;
                            for (Method m : interceptableMethods) // d630872
                            {
                                // Does method name and signature match?
                                if (m.getName().equals(methodName))
                                {
                                    String methodSignature = MethodAttribUtils.methodSignature(m);
                                    if (signature.equals(methodSignature))
                                    {
                                        // Yep, indicate we found the business method and exit the loop.
                                        notFound = false;
                                        break;
                                    }
                                }
                            }

                            // Log error and throw exception if business method was not found.
                            if (notFound)
                            {
                                if (isTraceOn && tc.isDebugEnabled())
                                {
                                    Tr.debug(tc, "method not found for interceptor binding");
                                    binding.dump();
                                }

                                // CNTR0244E: The {0} method name is not found in one of the business interfaces
                                // of the {1} enterprise bean. A style {2} interceptor-binding element requires
                                // the method to be a business method of the enterprise bean.
                                String j2eeName = ivJ2EEName.toString();
                                Tr.error(tc, "BUSINESS_METHOD_NOT_FOUND_FOR_INTERCEPTOR_BINDING_CNTR0244E"
                                         , new Object[] { methodName, j2eeName, "4" }); //d472939

                                String message = "The " + methodName + " method name is not found "
                                                 + " in one of the business interfaces of the " + j2eeName
                                                 + " enterprise bean. A style 4 interceptor-binding element requires"
                                                 + " the method to be a business method of the enterprise bean";
                                throw new EJBConfigurationException(message); // d463727
                            }
                        } // d472972 end
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "validateInterceptorBindings");
        }
    }

    /**
     * Return a method signature string that is equivalent
     * to what MethodAttribUtils.methodSignature( Method ) returns.
     *
     * @param methodName is the name of the method
     * @param parmList is a list of parameter types.
     *
     * @return "methodName:parmList.get(0),parmList.get(1),parmList.get(2),..."
     */
    // d457352   - added entire method
    public static String methodSignature(String methodName, List<String> parmList)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.entry(tc, "methodSignature", methodName);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append(":");
        boolean appendComma = false;
        for (String parm : parmList)
        {
            if (appendComma)
            {
                sb.append(",");
            }
            else
            {
                appendComma = true;
            }

            sb.append(parm);
        }

        String signature = sb.toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            Tr.exit(tc, "methodSignature", signature);
        }
        return signature;
    }

    /**
     * Removes embedded blanks in the vicinity of array brackets, within a
     * blank-delimited list of method arguments.
     *
     * Example input: int char [] char[ ] [ ]
     * Example output: int char[] char[][]
     *
     * Requires: no leading or trailing blanks on input string --
     * use trim() first if this might be the case.
     */
    // d457352   - added entire method
    public static final String normalizeSignature(String deplDescriptorSignature)
    {
        StringBuilder theSignature = new StringBuilder(deplDescriptorSignature);
        int scanIndex = 0;
        while (scanIndex < theSignature.length())
        {
            if (theSignature.charAt(scanIndex) == ' ')
            {
                char next = theSignature.charAt(scanIndex + 1);
                if (next == ' ' | next == '[' | next == ']')
                {
                    theSignature.deleteCharAt(scanIndex);
                }
                else
                {
                    ++scanIndex;
                }
            }
            else
            {
                ++scanIndex;
            }
        }

        return theSignature.toString();
    }

    /**
     * Validate and merge two style 2 interceptor bindings into a single style 2
     * interceptor binding object.
     *
     * @param binding1 is the first interceptor binding for the EJB class.
     * @param binding2 is the second interceptor binding for the EJB class.
     *
     * @return merger of binding1 and binding2.
     *
     * @throws EJBConfigurationException if the merger is something that is not possible
     *             if annotations were used instead of xml.
     */
    // d457352 - added entire method.
    // d472972 - rewrote entire method to pass CTS.
    private EJBInterceptorBinding
                    validateAndMergeStyle2Bindings(EJBInterceptorBinding binding1, EJBInterceptorBinding binding2)
                                    throws EJBConfigurationException
    {
        // Add binding 2 interceptor-class list, unless it is empty.
        ArrayList<String> interceptorNames = binding1.ivInterceptorClassNames;
        if (!binding2.ivInterceptorClassNames.isEmpty())
        {
            interceptorNames.addAll(binding2.ivInterceptorClassNames);
        }

        // Add binding 2 interceptor-order list, unless it is empty.
        ArrayList<String> interceptorOrder = binding1.ivInterceptorOrder;
        if (!binding2.ivInterceptorOrder.isEmpty())
        {
            interceptorOrder.addAll(binding2.ivInterceptorOrder);
        }

        // CTS expects the exclude-default-interceptors setting in prior style 2
        // interceptor-binding to be overridden by current interceptor-binding setting.
        Boolean excludeDefault = binding1.ivExcludeDefaultLevelInterceptors;
        if (binding2.ivExcludeDefaultLevelInterceptors != null)
        {
            excludeDefault = binding2.ivExcludeDefaultLevelInterceptors;
        }

        // Bindings are valid, so merge into a single binding.
        EJBInterceptorBinding mergedBinding;
        mergedBinding = new EJBInterceptorBinding(binding1.ivEjbName, interceptorNames, interceptorOrder);

        // Set whether or not default or class level interceptors are excluded.
        if (excludeDefault != null)
        {
            mergedBinding.setExcludeDefaultInterceptors(excludeDefault);
        }

        // Return merged binding.
        return mergedBinding;
    }

    /**
     * Validate and merge two style 3 or two style 4 interceptor bindings into a single
     * style 3 or style 4 interceptor binding object.
     *
     * @param methodName is the name of the method in the EJB class this binding is for.
     * @param signature is method signature for style4 interceptor bindings or null
     *            if the interceptor bindings are for style 3.
     * @param binding1 is the first interceptor binding for the EJB class.
     * @param binding2 is the second interceptor binding for the EJB class.
     *
     * @return merger of binding1 and binding2.
     *
     * @throws EJBConfigurationException if the merger is something that is not possible
     *             if annotations were used instead of xml.
     */
    // d457352 - added entire method.
    // d472972 - rewrote entire method to pass CTS.
    private EJBInterceptorBinding
                    validateAndMergeStyle3Or4Bindings(String methodName, String signature, EJBInterceptorBinding binding1, EJBInterceptorBinding binding2)
                                    throws EJBConfigurationException
    {
        // Add binding 2 interceptor-class list, unless it is empty.
        ArrayList<String> interceptorNames = binding1.ivInterceptorClassNames;
        if (!binding2.ivInterceptorClassNames.isEmpty())
        {
            interceptorNames.addAll(binding2.ivInterceptorClassNames);
        }

        // Add binding 2 interceptor-order list, unless it is empty.
        ArrayList<String> interceptorOrder = binding1.ivInterceptorOrder;
        if (!binding2.ivInterceptorOrder.isEmpty())
        {
            interceptorOrder.addAll(binding2.ivInterceptorOrder);
        }

        // CTS expects the exclude-default-interceptors setting in prior style 2
        // interceptor-binding to be overridden by current interceptor-binding setting.
        Boolean excludeDefault = binding1.ivExcludeDefaultLevelInterceptors;
        if (binding2.ivExcludeDefaultLevelInterceptors != null)
        {
            excludeDefault = binding2.ivExcludeDefaultLevelInterceptors;
        }

        // CTS expects the exclude-class-interceptors setting in prior style 3 or 4
        // interceptor-binding to be overridden by current interceptor-binding setting.
        // Verify at most only 1 of the bindings provides exclude-class-interceptors.
        Boolean excludeClass = binding1.ivExcludeClassLevelInterceptors;
        if (binding2.ivExcludeClassLevelInterceptors != null)
        {
            excludeClass = binding2.ivExcludeClassLevelInterceptors;
        }

        // Bindings are valid, so merge into a single binding.
        EJBInterceptorBinding mergedBinding;
        if (signature == null)
        {
            mergedBinding = new EJBInterceptorBinding
                            (binding1.ivEjbName, interceptorNames, interceptorOrder, methodName, null);
        }
        else
        {
            mergedBinding = new EJBInterceptorBinding
                            (binding1.ivEjbName, interceptorNames, interceptorOrder, methodName, binding1.ivMethodParms);
        }

        // Set whether or not default or class level interceptors are excluded.
        if (excludeClass != null)
        {
            mergedBinding.setExcludeClassLevelInterceptors(excludeClass);
        }

        if (excludeDefault != null)
        {
            mergedBinding.setExcludeDefaultInterceptors(excludeDefault);
        }

        // Return merged binding.
        return mergedBinding;
    }

    /**
     * Hide default CTOR since we want caller to call the static
     * createInterceptorMetaData method in this class.
     *
     * @throws EJBConfigurationException
     */
    // d367572.9 update for xml processing.
    private InterceptorMetaDataFactory(EJBModuleMetaDataImpl mmd, BeanMetaData bmd)
        throws EJBConfigurationException
    {
        ivBmd = bmd;
        ivMetadataComplete = bmd.metadataComplete;
        ivEjbClass = bmd.enterpriseBeanClass;
        ivJ2EEName = bmd.getJ2EEName();
        ivEjbName = bmd.enterpriseBeanName;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) // d404960
        {
            Tr.entry(tc, "InterceptorMetaDataFactory for J2EEName = " + ivJ2EEName);
        }

        ivClassLoader = bmd.classLoader;
        ivEJBModuleMetaDataImpl = mmd;
        ivHasComponentInterface = false;
        ivMDB = false;
        ivSLSB = false;
        ivSFSB = false;
        ivMB = false;

        // Is this a bean that implements component interface?
        if (bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN)
        {
            ivMDB = true;
            ivHasComponentInterface = javax.ejb.MessageDrivenBean.class.isAssignableFrom(ivEjbClass); // d399469
        }
        else if (bmd.type == InternalConstants.TYPE_STATELESS_SESSION)
        {
            ivSLSB = true;
            ivHasComponentInterface = javax.ejb.SessionBean.class.isAssignableFrom(ivEjbClass); // d399469
        }
        else if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) // F743-1751
        {
            ivSFSB = true;
            ivHasComponentInterface = javax.ejb.SessionBean.class.isAssignableFrom(ivEjbClass); // d399469
            if (bmd.container.isTransactionStatefulLifecycleMethods()) {
                ivBeanLifecycleMethods = new Method[LifecycleInterceptorWrapper.NUM_METHODS];
            }
        }
        else if (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION) // F743-1751
        {
            ivHasComponentInterface = false;
            ivBeanLifecycleMethods = new Method[LifecycleInterceptorWrapper.NUM_METHODS];
        }
        else if (bmd.type == InternalConstants.TYPE_MANAGED_BEAN) // F743-34301
        {
            ivMB = true;
            // Does not implement component interfaces
        }
        else
        {
            // F743-1751 - Reject unknown bean types.
            throw new IllegalArgumentException("unsupported bean type: " + bmd.type);
        }

        if (isTraceOn && tc.isDebugEnabled()) // d404960
        {
            Tr.debug(tc, "ivHasComponentInterface = " + ivHasComponentInterface
                         + " for bmd.enterpriseBeanClass = " + ivEjbClass.getName());
        }

        // Get default interceptor binding from module metadata, but do not remove since
        // it may be needed for other EJB initialization.
        if (ivEJBModuleMetaDataImpl.ivInterceptorBindingMap != null)
        {
            List<EJBInterceptorBinding> list = ivEJBModuleMetaDataImpl.getEJBInterceptorBindings("*");
            if (list != null && list.size() > 0) // d463727
            {
                if (list.size() == 1) // d463727
                {
                    ivModuleInterceptorBinding = list.get(0);
                }
                else
                {
                    // CNTR0245E: The {0} module of the {1) application has more than one style 1 interceptor-binding
                    // in the deployment descriptor. Only one style 1 interceptor-binding is allowed.
                    J2EEName j2eeName = ivEJBModuleMetaDataImpl.getJ2EEName();
                    String message = "The " + j2eeName.getModule() + " module of the " + j2eeName.getApplication()
                                     + " application has more than one style 1 interceptor-binding in the deployment"
                                     + " deployment descriptor. Only one style 1 interceptor-binding is allowed";
                    Tr.error(tc, "DUPLICATE_STYLE_1_INTERCEPTOR_BINDING_CNTR0245E"
                             , new Object[] { j2eeName.getModule(), j2eeName.getApplication() }); //d472939
                    throw new EJBConfigurationException(message); // d463727
                }
            }

            ivInterceptorBinding = ivEJBModuleMetaDataImpl.getEJBInterceptorBindings(ivEjbName); // d695226
        }

        if (isTraceOn && tc.isEntryEnabled()) // d404960
        {
            Tr.exit(tc, "InterceptorMetaDataFactory for EJBName = " + ivEjbName
                        + ", metadata-complete = " + ivMetadataComplete);
        }
    }

    /**
     * @throws UnsupportedOperationException is thrown if called since we
     *             want other CTOR to always be used.
     */
    private InterceptorMetaDataFactory()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Set whether default interceptors are to be excluded or disabled for this
     * EJB class. ivExcludeDefaultFromClassLevel is set to true if default
     * interceptors are to be excluded for this EJB class.
     */
    // d367572.9 update for xml and metadata-complete.
    private void setExcludeDefaultInterceptors()
    {
        EJBInterceptorBinding binding = ivClassInterceptorBinding;
        boolean useAnnotation = false;
        if (binding == null)
        {
            // Use annotation provided metadata-complete is false.
            useAnnotation = !ivMetadataComplete;
        }
        else
        {
            // Assumption is xml overrides annotation, although EJB spec implies
            // interceptor-binding is used to augment annotation.  Not clear what
            // is intended, but we are going to assume xml overrides annotation.
            Boolean exclude = binding.ivExcludeDefaultLevelInterceptors;
            if (exclude == null)
            {
                // exclude is not set in ejb-jar.xml for class level binding,
                // Use annotation provided metadata-complete is false.
                useAnnotation = !ivMetadataComplete;
            }
            else if (exclude == Boolean.TRUE)
            {
                ivExcludeDefaultFromClassLevel = true;
            }
            else
            {
                ivExcludeDefaultFromClassLevel = false;
            }
        }

        // If xml did not have a exclude-default-interceptor setting for
        // class level binding, then see if annotation indicates to exclude
        // default level interceptors.
        if (useAnnotation)
        {
            ExcludeDefaultInterceptors a = ivEjbClass.getAnnotation(ExcludeDefaultInterceptors.class);
            if (a != null)
            {
                ivExcludeDefaultFromClassLevel = true;
            }
            else
            {
                ivExcludeDefaultFromClassLevel = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, "setExcludeDefaultInterceptors: " + ivExcludeDefaultFromClassLevel);
        }
    }

    /**
     * Add default interceptors to the interceptor names to class
     * map object (ivInterceptorNameToClassMap).
     *
     * @throws EJBConfigurationException is thrown if same class name appears
     *             more than once in the list of default interceptors.
     */
    // d367572.9 update for xml and metadata-complete.
    private void addDefaultInterceptors()
                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "addDefaultInterceptorNames");
        }

        // Get list of default interceptor names from interceptor-binding for EJB name *.
        List<String> names = new LinkedList<String>();
        if (ivModuleInterceptorBinding != null)
        {
            if (!ivModuleInterceptorBinding.ivInterceptorClassNames.isEmpty()) // d453477
            {
                names = ivModuleInterceptorBinding.ivInterceptorClassNames;
            }
            else if (!ivModuleInterceptorBinding.ivInterceptorOrder.isEmpty()) // d453477
            {
                // interceptor-order and interceptor-classes are mutually exclusive
                // in xml schema.  So only 1 will occur, not both.
                names = ivModuleInterceptorBinding.ivInterceptorOrder;
            }
        }

        // Process any default interceptor names found for the module this bean is in.
        if (names.size() > 0)
        {
            updateNamesToClassMap(names); // d630717

            // Remember list of default interceptor names.
            ivDefaultInterceptorNames.addAll(names);
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "addDefaultInterceptorNames: " + ivDefaultInterceptorNames);
        }
    }

    /**
     * Add the class-level interceptors to the interceptor names to class
     * map object (ivInterceptorNameToClassMap).
     *
     * @throws EJBConfigurationException is thrown if same class name appears
     *             more than once in the list of class-level interceptors.
     */
    // d367572.9 update for xml and metadata-complete.
    private void addClassLevelInterceptors() throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "addClassLevelInterceptors for EJB name: " + ivEjbName);
        }

        // Provided that metadata-complete is false, process annotation first
        // since the interceptor classes from annotation are the first ones
        // that need to be called.
        if (!ivMetadataComplete)
        {
            Interceptors cli = ivEjbClass.getAnnotation(Interceptors.class);
            if (cli != null)
            {
                if (isTraceOn && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "processing @Interceptor annotation for EJB name");
                }

                // Get list of interceptor names and classes.
                List<String> names = addLoadedInterceptorClasses(cli.value()); // d630717

                // Add to interceptor class name list.
                ivClassInterceptorNames.addAll(names);
                if (isTraceOn && tc.isDebugEnabled()) // d453477
                {
                    Tr.debug(tc, names + " were annotated with @Interceptor annotation at class level");
                }
            }
        }

        // Now augment this list by processing the class level InterceptorBinding from
        // WCCM for this EJB (if one exists).
        if (ivClassInterceptorBinding != null)
        {
            // Get list of class interceptor names from interceptor-binding for EJBname.
            Collection<String> classInterceptorNames = ivClassInterceptorBinding.ivInterceptorClassNames; // d453477
            List<String> interceptorOrder = ivClassInterceptorBinding.ivInterceptorOrder; // d453477
            if (!classInterceptorNames.isEmpty()) // d453477
            {
                if (isTraceOn && tc.isDebugEnabled()) // d453477
                {
                    Tr.debug(tc, "updating class level list with InterceptorBinding list: "
                                 + classInterceptorNames);
                }

                // Update interceptor class name to Class object map and verify every
                // interceptor class can be loaded and there are does not duplicate a
                // name provided by annotation since xml is suppose to augment the
                // annotation set of interceptor class names.
                updateNamesToClassMap(classInterceptorNames);

                // Add all of the names to the interceptor class name list.
                ivClassInterceptorNames.addAll(classInterceptorNames);
            }
            else if (!interceptorOrder.isEmpty()) // d453477
            {
                if (isTraceOn && tc.isDebugEnabled()) // d453477
                {
                    Tr.debug(tc, "updating class level list with InterceptorBinding order list: "
                                 + interceptorOrder);
                }

                // interceptor-order and interceptor-classes are mutually exclusive
                // in xml schema.  So only 1 will occur, not both. Get the total
                // set of ordered class names.
                HashSet<String> set = new HashSet<String>(interceptorOrder);

                // Remove the default interceptor class names from the set so
                // that only the names that remain are class level interceptors.
                set.removeAll(ivDefaultInterceptorNames); // d456352

                // Remove the class level interceptor names that came from annotation
                // from the set so that only the class level interceptor names that came
                // from xml remain in the set.
                set.removeAll(ivClassInterceptorNames);

                // Now add the names from xml to the list of class level interceptor names
                // if there are any names remaining in the set.
                if (set.isEmpty() == false)
                {
                    // First update interceptor class name to Class object map and
                    // verify the interceptor class can be loaded.
                    updateNamesToClassMap(set);

                    // Now add the names from xml to the list of interceptor class names.
                    ivClassInterceptorNames.addAll(set);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "addClassLevelInterceptors returning: " + ivClassInterceptorNames
                        + " for EJB name: " + ivEjbName);
        }
    }

    /**
     * Add the JCDI interceptors to the interceptor names to class
     * map object (ivInterceptorNameToClassMap). <p>
     *
     * The JCDI Service may provide an interceptor that is to be the 'first'
     * interceptor to run, and also an interceptor that is to be the 'last'
     * interceptor to run.
     */
    // F743-15628 d649636
    private void addJCDIInterceptors()
    {
        // Must be in a JCDI enabled module, and not a ManagedBean
        JCDIHelper jcdiHelper = ivEJBModuleMetaDataImpl.ivJCDIHelper;
        if (jcdiHelper != null && !ivBmd.isManagedBean()) // F743-34301.1
        {
            // Obtain the interceptor to start the interceptor chain.    F743-29169
            J2EEName j2eeName = ivEJBModuleMetaDataImpl.ivJ2EEName;
            ivJCDIFirstInterceptorClass = jcdiHelper.getFirstEJBInterceptor(j2eeName,
                                                                            ivEjbClass);
            if (ivJCDIFirstInterceptorClass != null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "addJCDIInterceptor : " + ivJCDIFirstInterceptorClass.getName());
                ivInterceptorNameToClassMap.put(ivJCDIFirstInterceptorClass.getName(),
                                                ivJCDIFirstInterceptorClass);
            }

            // Obtain the interceptor at the end of the interceptor chain.
            ivJCDILastInterceptorClass = jcdiHelper.getEJBInterceptor(j2eeName,
                                                                      ivEjbClass);
            if (ivJCDILastInterceptorClass != null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "addJCDIInterceptor : " + ivJCDILastInterceptorClass.getName());
                ivInterceptorNameToClassMap.put(ivJCDILastInterceptorClass.getName(),
                                                ivJCDILastInterceptorClass);
            }
        }
    }

    /**
     * For a given collection of class level interceptor class names, update the
     * the ivInterceptorNameToClassMap with a map entry using name
     * as the key and class object as the value.
     *
     * @param classInterceptorNames is the collection of class level interceptor class names.
     *
     * @throws EJBConfigurationException if unable to load class or name duplicates
     *             a previously added interceptor class.
     */
    // d472972 - rewrote entire method to pass CTS.
    private void updateNamesToClassMap(Collection<String> classInterceptorNames) throws EJBConfigurationException
    {
        String classNameToLoad = null;
        try
        {
            // for each interceptor class name.
            for (String name : classInterceptorNames)
            {
                // Get class object from the name to class map.
                Class<?> interceptorClass = ivInterceptorNameToClassMap.get(name);

                // Class already in the map?
                if (interceptorClass == null)
                {
                    // Nope, then load it and put it in the map.
                    classNameToLoad = name;
                    interceptorClass = ivClassLoader.loadClass(name);
                    ivInterceptorNameToClassMap.put(name, interceptorClass);
                }
            }
        } catch (ClassNotFoundException ex)
        {
            //FFDCFilter.processException(ex, CLASS_NAME + ".addClassLevelInterceptors", "376");

            // CNTR0075E: The user-provided class "{0}" needed by the EnterpriseBean could not be found or loaded.
            Tr.error(tc, "BEANCLASS_NOT_FOUND_CNTR0075E", classNameToLoad);

            EJBConfigurationException cex;
            cex = new EJBConfigurationException(classNameToLoad + " could not be found or loaded", ex);
            throw cex;
        }
    }

    /**
     * Adds a class to the list of loaded classes. getInterceptorProxies relies
     * on every interceptor class either being added via updateNamesToClassMap
     * or this method.
     *
     * @param klass the class
     * @return the list of class names
     */
    private List<String> addLoadedInterceptorClasses(Class<?>[] classes) // d630717
    {
        List<String> names = new ArrayList<String>();
        for (Class<?> klass : classes)
        {
            String className = klass.getName();
            ivInterceptorNameToClassMap.put(className, klass);
            names.add(className);
        }

        return names;
    }

    /**
     * Add method-level interceptors to the interceptor names to class
     * map object (ivInterceptorNameToClassMap).
     *
     * @param method is the Method object for a business method of the EJB.
     * @param methodBinding is the interceptor-binding for the method.
     *
     * @throws EJBConfigurationException is thrown if a method same class name appears
     *             more than once in the list of default interceptors.
     */
    // d367572.9 updated for xml and metadata-complete.
    private ArrayList<String> addMethodLevelInterceptors(Method method, EJBInterceptorBinding methodBinding)
                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "addMethodLevelInterceptors method: " + method.getName());
        }

        // List of method-level interceptor names to return to the caller.
        ArrayList<String> interceptorNames = new ArrayList<String>();

        if (!ivMetadataComplete)
        {
            // Process annotation first since the interceptor classes from
            // annotation are the first ones that need to be called.
            Interceptors methodLevelInterceptors = method.getAnnotation(Interceptors.class);
            String methodName = method.getName();
            if (methodLevelInterceptors != null)
            {
                if (isTraceOn && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "processing @Interceptor annotation for method: " + methodName
                                 + ", EJB name: " + ivEjbName);
                }

                // Get list of interceptor names and classes.
                List<String> names = addLoadedInterceptorClasses(methodLevelInterceptors.value()); // d630717

                // Add to the interceptor names list to be returned to the caller.
                interceptorNames.addAll(names);
            }
        }

        // Now augment this list by processing the class level InterceptorBinding from
        // WCCM for this method of the EJB (if one exists).
        if (methodBinding != null)
        {
            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "processing interceptor binding for method: " + method);
            }

            List<String> names = methodBinding.ivInterceptorClassNames; // d453477
            updateNamesToClassMap(names); // d630717

            // Add to the interceptor names list to be returned to the caller.
            interceptorNames.addAll(names);
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "addMethodLevelInterceptors: " + interceptorNames);
        }

        // Return list of method-level interceptor names for this business method.
        return interceptorNames;
    }

    /**
     * Process all business methods of the EJB and update any EJBMethodInfo
     * object for any business method that requires one or more around-invoke
     * or around-timeout interceptor methods to be invoked.
     *
     * @throws EJBConfigurationException is thrown if any configuration error
     *             of an interceptor method is detected.
     */
    // d367572.9 updated for xml
    private void updateEJBMethodInfoInterceptorProxies() // d386227
    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "updateEJBMethodInfoInterceptorProxies: " + ivEjbName);
        }

        // For each method, update EJBMethodInfo with the correct array of
        // InterceptorProxy array needed for that method.
        for (Map.Entry<Method, ArrayList<EJBMethodInfoImpl>> entry : ivEJBMethodInfoMap.entrySet()) // d630717
        {
            InterceptorProxy[] proxies = getAroundInterceptorProxies(InterceptorMethodKind.AROUND_INVOKE, entry.getKey()); // F743-17763

            // Update the EJBMethodInfos with the InterceptorProxy array for this method.
            if (proxies != null)
            {
                for (EJBMethodInfoImpl info : entry.getValue()) // d630717
                {
                    info.setAroundInterceptorProxies(proxies); // F743-17763.1
                }
            }
        }

        // F743-17763 - For each timer method, update EJBMethodInfo with the
        // correct array of InterceptorProxy for that method.  The preceding loop
        // does not include EJBMethodInfoImpls for MethodInterface.TIMED_OBJECT,
        // so this loop's call to setAroundInterceptorProxies does not conflict.
        if (ivBmd.timedMethodInfos != null)
        {
            for (EJBMethodInfoImpl info : ivBmd.timedMethodInfos)
            {
                InterceptorProxy[] proxies = getAroundInterceptorProxies(InterceptorMethodKind.AROUND_TIMEOUT
                                                                         , info.getMethod());

                if (proxies != null)
                {
                    info.setAroundInterceptorProxies(proxies); // F743-17763.1
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "updateEJBMethodInfoInterceptorProxies: " + ivJ2EEName);
        }
    }

    /**
     * Get the array of InterceptorProxy objects required for invoking the
     * AroundInvoke or AroundTimeout interceptors methods when a method is invoked.
     *
     * @param kind the interceptor kind; must be either AROUND_INVOKE or AROUND_TIMEOUT
     * @param m the method
     *
     * @return array of InterceptorProxy for invoking interceptors or a null reference
     *         if no around invoke interceptor methods need to be invoked.
     * @throws EJBConfigurationException is thrown if interceptor-order element is used
     *             for specified business method and the order is incomplete.
     */
    private InterceptorProxy[] getAroundInterceptorProxies(InterceptorMethodKind kind, Method m)
                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getAroundInterceptorProxies: " + kind + ", " + m);
        }

        // d630717 - Unconditionally create an ordered list of interceptors to
        // pass to getInterceptorProxies.
        List<String> orderedList = orderMethodLevelInterceptors(m);

        // Now create the InterceptorProxy array required for business method specified
        // by the caller.
        InterceptorProxy[] proxies = getInterceptorProxies(kind, orderedList); // d630717

        // Return the InterceptorProxy array for invoking around invoke interceptors
        // for the businesss method specified by the caller.
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "getAroundInterceptorProxies");
        }
        return proxies;
    }

    /**
     * Determine if default interceptors need to be excluded or disabled for a specified method.
     *
     * @param m is the method.
     * @param binding is the WCCM InterceptorBinding for the method or null if none exists.
     *
     * @return true if and only if class-level interceptors need to be excluded for specified method.
     */
    // d367572.9 updated for xml and metadata-complete
    private boolean isDefaultInterceptorsExcluded(Method m, EJBInterceptorBinding binding)
    {
        boolean exclude;
        if (ivExcludeDefaultFromClassLevel)
        {
            // Default interceptors excluded as class level, so return true.
            exclude = true;
        }
        else if (binding != null && binding.ivExcludeDefaultLevelInterceptors != null)
        {
            // Assumption is xml overrides annotation, although EJB spec implies
            // interceptor-binding is used to augment annotation.  Not clear what
            // is intended, but we are going to assume xml overrides annotation.
            exclude = binding.ivExcludeDefaultLevelInterceptors;
        }
        else
        {
            // If the module is not metadata complete, then check the annotation
            // to determine whether to exclude default interceptors.
            exclude = !ivMetadataComplete && m.getAnnotation(ExcludeDefaultInterceptors.class) != null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, "isDefaultInterceptorsExcluded for " + m.getDeclaringClass() + "." + m.getName()
                         + " returning: " + exclude);
        }
        return exclude;
    }

    /**
     * Determine if class-level interceptors need to be excluded or disabled
     * for a specified method.
     *
     * @param m is the method.
     *
     * @param binding is the WCCM InterceptorBinding for the method or null if none exists.
     *
     * @return true if and only if class-level interceptors need to be excluded
     *         for the specified method.
     */
    // d367572.9 updated for xml and metadata-complete
    private boolean isClassInterceptorsExcluded(final Method m, final EJBInterceptorBinding binding)
    {
        boolean exclude;
        if (binding != null && binding.ivExcludeClassLevelInterceptors != null)
        {
            // Assumption is xml overrides annotation, although EJB spec implies
            // interceptor-binding is used to augment annotation.  Not clear what
            // is intended, but we are going to assume xml overrides annotation.
            exclude = binding.ivExcludeClassLevelInterceptors;
        }
        else
        {
            // If the module is not metadata complete, then check the annotation
            // to determine whether to exclude class level interceptors.
            exclude = !ivMetadataComplete && m.getAnnotation(ExcludeClassInterceptors.class) != null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, "isClassInterceptorsExcluded for " + m.getDeclaringClass()
                         + "." + m.getName() + " returning: " + exclude);
        }
        return exclude;
    }

    /**
     * Processes interceptor methods on the bean class.
     *
     * @throws EJBConfigurationException
     */
    private void processBeanInterceptors() // d630717
    throws EJBConfigurationException
    {
        ivBeanInterceptorProxyMap = createInterceptorProxyMap(ivEjbClass, -1);

        // F743-1751 - Not all bean types collect bean methods.  For those
        // that do, find the methods that are declared on the bean.
        if (ivBeanLifecycleMethods != null)
        {
            for (InterceptorMethodKind kind : InterceptorMethodKind.values())
            {
                int mid = kind.getMethodID();
                if (mid != -1)
                {
                    List<InterceptorProxy> proxyList = ivBeanInterceptorProxyMap.get(kind);
                    if (proxyList != null)
                    {
                        for (InterceptorProxy proxy : proxyList)
                        {
                            Method m = proxy.ivInterceptorMethod;
                            if (m.getDeclaringClass() == ivEjbClass)
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "found bean " + LifecycleInterceptorWrapper.TRACE_NAMES[mid] + " method: " + m);

                                ivBeanLifecycleMethods[mid] = m;
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets an ordered list of interceptor proxies of the specified kind.
     * processBeanInterceptors must have been called prior to calling this
     * method.
     *
     * @param kind the interceptor kind
     * @param orderedList an ordered list of interceptor class names, all of
     *            which have been passed through loadInterceptorClass or
     *            addLoadedInterceptorClass
     * @return an ordered list of interceptor proxies, or null if neither the
     *         bean class nor the ordered list of interceptor classes has any
     *         interceptors of the specified kind
     * @throws EJBConfigurationException
     */
    private InterceptorProxy[] getInterceptorProxies(InterceptorMethodKind kind, List<String> orderedList) // d630717
    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getInterceptorProxies: " + kind + ", " + orderedList);
        }

        List<InterceptorProxy> proxyList = new ArrayList<InterceptorProxy>();

        // Iterate over the ordered list of interceptor classes.  For each
        // interceptor, get the list of interceptor proxies of the appropriate
        // interceptor kind, and add the proxies to the list.
        for (String name : orderedList)
        {
            // Every class should have been loaded by this point.  We make this
            // a requirement because it's more efficient than going to the class
            // loader and because it's trivial to ensure.
            Class<?> klass = ivInterceptorNameToClassMap.get(name);

            Map<InterceptorMethodKind, List<InterceptorProxy>> proxyMap = ivInterceptorProxyMaps.get(klass);
            if (proxyMap == null)
            {
                // The proxies for this interceptor have not been created yet.  Add
                // the class to the list of interceptor proxy classes, and then
                // create the map using that array index.
                int index = ivInterceptorClasses.size();
                ivInterceptorClasses.add(klass);

                proxyMap = createInterceptorProxyMap(klass, index);
                ivInterceptorProxyMaps.put(klass, proxyMap);
            }

            List<InterceptorProxy> kindProxyList = proxyMap.get(kind);
            if (kindProxyList != null)
            {
                proxyList.addAll(kindProxyList);
            }
        }

        // If CDI is enabled, add the proxies from the first JCDI interceptor
        // class. It should run before all other interceptors.          F743-29169
        if (ivJCDIFirstInterceptorClass != null)
        {
            Map<InterceptorMethodKind, List<InterceptorProxy>> proxyMap = ivInterceptorProxyMaps.get(ivJCDIFirstInterceptorClass);
            if (proxyMap == null)
            {
                // The proxies for this interceptor have not been created yet.  Add
                // the class to the list of interceptor proxy classes, and then
                // create the map using that array index.
                int index = ivInterceptorClasses.size();
                ivInterceptorClasses.add(ivJCDIFirstInterceptorClass);

                proxyMap = createInterceptorProxyMap(ivJCDIFirstInterceptorClass, index);
                ivInterceptorProxyMaps.put(ivJCDIFirstInterceptorClass, proxyMap);
            }

            List<InterceptorProxy> kindProxyList = proxyMap.get(kind);
            if (kindProxyList != null)
            {
                proxyList.addAll(0, kindProxyList); // adds to beginning
            }
        }

        // If CDI is enabled, add the proxies from the last JCDI interceptor
        // class. It should run after all other interceptors.           F743-15628
        if (ivJCDILastInterceptorClass != null)
        {
            Map<InterceptorMethodKind, List<InterceptorProxy>> proxyMap = ivInterceptorProxyMaps.get(ivJCDILastInterceptorClass);
            if (proxyMap == null)
            {
                // The proxies for this interceptor have not been created yet.  Add
                // the class to the list of interceptor proxy classes, and then
                // create the map using that array index.
                int index = ivInterceptorClasses.size();
                ivInterceptorClasses.add(ivJCDILastInterceptorClass);

                proxyMap = createInterceptorProxyMap(ivJCDILastInterceptorClass, index);
                ivInterceptorProxyMaps.put(ivJCDILastInterceptorClass, proxyMap);
            }

            List<InterceptorProxy> kindProxyList = proxyMap.get(kind);
            if (kindProxyList != null)
            {
                proxyList.addAll(kindProxyList); // adds to end
            }
        }

        // Finally, add the interceptor proxies from the bean class.
        List<InterceptorProxy> kindProxyList = ivBeanInterceptorProxyMap.get(kind);
        if (kindProxyList != null)
        {
            proxyList.addAll(kindProxyList);
        }

        // Convert the list of proxies to an array.
        InterceptorProxy[] proxies;
        if (proxyList.isEmpty())
        {
            proxies = null;
        }
        else
        {
            proxies = new InterceptorProxy[proxyList.size()];
            proxyList.toArray(proxies);
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "getInterceptorProxies: " + Arrays.toString(proxies));
        }
        return proxies;
    }

    /**
     * Add a InterceptorProxy object to proxy list for each interceptor method
     * found in a specified interceptor class (or EJB class itself).
     *
     * @param interceptorOrEjbClass is the class object of an interceptor class
     *            or the EJB class itself.
     *
     * @param index is the interceptor index into InterceptorMetaData.ivInterceptors array
     *            for locating the instance of interceptorClass that is created for an EJB instance.
     *            Note, a value < 0 must be passed as the index if the interceptorOrEjbClass
     *            parameter is for the EJB class or its superclass.
     *
     * @throws EJBConfigurationException if an error is detected (e.g. method is final or static).
     */
    // d367572.9 updated for xml and metadata-complete
    // d630717 modified to return a Map rather than populate an instance variable
    private Map<InterceptorMethodKind, List<InterceptorProxy>> createInterceptorProxyMap(final Class<?> interceptorOrEjbClass, int index)
                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "createInterceptorProxyMap: " + interceptorOrEjbClass.getName() + ", " + index);
        }

        // Create a LIFO LinkedList of Class objects starting with the interceptor or EJB class
        // itself and then each its the super classes. We create a LIFO so that the interceptor
        // methods in the most general superclass are invoked first as required by the
        // EJB specification.
        LinkedList<Class<?>> supers;
        supers = InterceptorMetaDataHelper.getLIFOSuperClassesList(interceptorOrEjbClass);

        // Get the EnumMap of interceptor methods to be processed.
        EnumMap<InterceptorMethodKind, List<Method>> enumMap = null;

        // Determine if this is the EJB class
        boolean ejbClass = (index < 0); //d451021
        if (ejbClass)
        {
            if (isTraceOn && tc.isDebugEnabled()) // d404960
            {
                Tr.debug(tc, "processing EJB class, ivHasComponentInterface = " + ivHasComponentInterface);
            }

            // EJB class, call helper to get the EnumMap for the EJB only
            // if there is a WCCM EnterpriseBean object for this EJB.
            // If null, then this EJB must be annotated rather than defined
            // in ejb-jar.xml file.
            EnterpriseBean bean = ivBmd.wccm.enterpriseBean;
            if (bean != null)
            {
                enumMap = InterceptorMetaDataHelper.getEJBInterceptorMethods(ivEjbClass, bean, supers, ivBmd.j2eeName);
            }
        }
        else
        {
            // Get EnumMap for this interceptor class. The EnumMap is built from
            // processing Interceptors stanza in ejb-jar.xml to get list of each
            // interceptor method specified via xml.
            IdentityHashMap<Class<?>, EnumMap<InterceptorMethodKind, List<Method>>> interceptorsMap;
            interceptorsMap = ivEJBModuleMetaDataImpl.ivInterceptorsMap;
            if (interceptorsMap != null) // d438904
            {
                enumMap = interceptorsMap.get(interceptorOrEjbClass);
            }
        }

        Map<InterceptorMethodKind, List<InterceptorProxy>> proxyMap =
                        new HashMap<InterceptorMethodKind, List<InterceptorProxy>>(); // d630717

        // For each class in the interceptor class inheritance tree.
        for (Class<?> superClass : supers)
        {
            Map<InterceptorMethodKind, InterceptorProxy> proxies =
                            new EnumMap<InterceptorMethodKind, InterceptorProxy>(InterceptorMethodKind.class); // F743-17763

            // If there were no Interceptor methods defined via xml, then EnumMap
            // is null.  So only process EnumMap if not null.
            if (enumMap != null)
            {
                // F743-17763 - Iterate over all interceptor kinds, and look for
                // interceptors of that type defined in XML.
                for (InterceptorMethodKind kind : InterceptorMethodKind.values())
                {
                    // Get methods specified via xml.
                    List<Method> methodList = enumMap.get(kind);
                    if (methodList != null)
                    {
                        for (Method m : methodList)
                        {
                            // Is the method overridden?
                            if (!InterceptorMetaDataHelper.isMethodOverridden(m, supers)) // d469514
                            {
                                if (kind.isLifecycle() && ejbClass && kind.isEJBCallbackMethodValidationRequired(this))
                                {
                                    validateEJBCallbackMethod(kind, m, false);
                                }

                                // Not overridden, so process the method.
                                if (m.getDeclaringClass() == superClass)
                                {
                                    // Validate.
                                    if (proxies.containsKey(kind))
                                    {
                                        // CNTR0223E: Only one method in the {0} class is allowed to be a {1} interceptor method.
                                        String className = superClass.getName();
                                        Object[] data = new Object[] { className, kind.getXMLElementName() };
                                        Tr.error(tc, "DUPLICATE_INTERCEPTOR_METHOD_CNTR0223E", data);
                                        throw new EJBConfigurationException("Only one " + kind.getXMLElementName()
                                                                            + " interceptor method is allowed in class " + className);
                                    }

                                    // Create an InterceptorProxy for it.
                                    addInterceptorProxy(kind, m, index, proxies, proxyMap);
                                }
                            }
                        }
                    }
                }
            }

            // Provided that metadata-complete is false, process annotation
            // in the superClass to determine if there is any interceptor
            // method to add that was not added already via xml.
            if (!ivMetadataComplete)
            {
                // For each method of the class.
                // For each method in a class of EJB class inheritance.
                Method[] methods = superClass.getDeclaredMethods();
                for (Method m : methods)
                {
                    // F743-17763 - Iterate over all interceptor kinds, and look for
                    // interceptors of that type defined in XML.
                    for (InterceptorMethodKind kind : InterceptorMethodKind.values())
                    {
                        // Does this method have the annotation?
                        Class<? extends Annotation> annotationClass = kind.getAnnotationClass();
                        if (annotationClass != null && m.isAnnotationPresent(annotationClass))
                        {
                            // Yes, is the method overridden?
                            if (!InterceptorMetaDataHelper.isMethodOverridden(m, supers)) // d469514
                            {
                                // Not overridden, so validate its signature if necessary.
                                if (kind.isLifecycle())
                                {
                                    if (ejbClass && kind.isEJBCallbackMethodValidationRequired(this))
                                    {
                                        validateEJBCallbackMethod(kind, m, true);
                                    }

                                    InterceptorMetaDataHelper.validateLifeCycleSignature(kind,
                                                                                         annotationClass.getSimpleName(),
                                                                                         m, ejbClass, ivBmd.j2eeName);
                                }
                                else
                                {
                                    InterceptorMetaDataHelper.validateAroundSignature(kind, m, ivBmd.j2eeName);
                                }

                                // Validate.
                                InterceptorProxy proxy = proxies.get(kind);
                                if (proxy == null)
                                {
                                    // Create an InterceptorProxy for it.
                                    addInterceptorProxy(kind, m, index, proxies, proxyMap);
                                }
                                else if (!m.equals(proxy.ivInterceptorMethod))
                                {
                                    // CNTR0223E: Only one method in the {0} class is allowed to be a {1} interceptor method.
                                    String className = superClass.getName();
                                    Object[] data = new Object[] { className, kind.getXMLElementName() };
                                    Tr.error(tc, "DUPLICATE_INTERCEPTOR_METHOD_CNTR0223E", data);
                                    throw new EJBConfigurationException("Only one " + kind.getXMLElementName()
                                                                        + " interceptor method is allowed in class " + className);
                                }
                            }
                        }
                        else if (kind.isLifecycle() && ejbClass && kind.isNonAnnotatedEJBCallbackMethodValidationRequired(this))
                        {
                            if (m.getName().equals(kind.getLifecycleCallbackMethodName()) &&
                                m.getParameterTypes().length == 0 &&
                                m.getReturnType() == java.lang.Void.TYPE) // d404960
                            {
                                InterceptorProxy proxy = proxies.get(kind);
                                if (proxy == null)
                                {
                                    addInterceptorProxy(kind, m, index, proxies, proxyMap);
                                }
                                else if (!m.equals(proxy.ivInterceptorMethod)) // d453498
                                {
                                    // CNTR0223E: Only one method in the {0} class is allowed to be a {1} interceptor method.
                                    String className = superClass.getName();
                                    Object[] data = new Object[] { className, kind.getXMLElementName() };
                                    Tr.error(tc, "DUPLICATE_INTERCEPTOR_METHOD_CNTR0223E", data);
                                    throw new EJBConfigurationException("Only one post-construct interceptor method is " +
                                                                        "allowed for class " + className + ". Both " +
                                                                        proxy.ivInterceptorMethod.getName() +
                                                                        " and " + m.getName() +
                                                                        " are configured as post-construct methods.");
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "createInterceptorProxyMap");
        }

        return proxyMap;
    }

    /**
     * Adds a new interceptor proxy to:
     * <ul>
     * <li>the interceptor map for the class currently being processed
     * <li>the list of all interceptors for the bean
     * <li>the bean lifecycle methods (if needed)
     * </ul>
     *
     * @param kind the interceptor kind
     * @param m the interceptor method
     * @param index is the interceptor index into InterceptorMetaData.ivInterceptors array
     *            for locating the instance of interceptorClass that is created for an EJB instance.
     *            Note, a value < 0 must be passed for the EJB class itself
     * @param classProxies the map of proxies for the class that is currently being processed.
     * @param proxyMap the map of proxies for the class hierarchy currently being processed.
     */
    private void addInterceptorProxy
                    (InterceptorMethodKind kind
                     , Method m
                     , int index
                     , Map<InterceptorMethodKind, InterceptorProxy> classProxies
                     , Map<InterceptorMethodKind, List<InterceptorProxy>> proxyMap) // F743-17763
    {
        InterceptorProxy proxy = new InterceptorProxy(m, index);
        classProxies.put(kind, proxy);

        List<InterceptorProxy> proxyList = proxyMap.get(kind);
        if (proxyList == null)
        {
            proxyList = new ArrayList<InterceptorProxy>();
            proxyMap.put(kind, proxyList);
        }
        proxyList.add(proxy);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, "adding " + kind + ": " + proxy);
        }
    }

    /**
     * Either xml or annotation was used to mark a method as being a interceptor
     * lifecycle event callback method and the EJB class implements one of the old
     * EBJ 2.1 component interfaces (javax.ejb.MessageDriven or javax.ejb.SessionBean).
     * For this scenario, we need to ensure the correct lifecycle event callback annotation
     * or xml was used for the method. Throws a EJBCOnfigurationException if the wrong
     * kind was used.
     *
     * @param actualKind is one of the InterceptorMethodKind enum values that indicates
     *            the kind of interceptor lifecycle callback the method is specified to be
     *            either in the ejb-jar.xml file or via use of annotations.
     *
     * @param m is the java reflection Method object.
     * @param annotation must be true if annotation being used rather than xml.
     *
     * @throws EJBConfigurationException if method is wrong kind of lifecycle event callback.
     */
    //d463727 entire method added.
    private void validateEJBCallbackMethod(InterceptorMethodKind actualKind, Method m, boolean annotation)
                    throws EJBConfigurationException
    {
        // Get the name of the method.
        String methodName = m.getName();

        // Map the method name into one of the InterceptorMethodKind enum value
        // it is required to be by the EJB 3 specification.
        InterceptorMethodKind requiredKind = mapEjbCallbackName(methodName);

        // If the method is required to be a interceptor lifecycle callback method,
        // does the actual kind match the required kind?
        if (requiredKind != null && actualKind != requiredKind)
        {
            // It is one of the ejbXXXXX methods of the javax.ejb.SessionBean or
            // javax.ejb.MessageDriven interfaces, but either annotation or xml was used
            // to indicate it is the wrong kind of lifecycle callback event.

            // Get the class name of the EJB and map both the required InterceptorMethodKind
            // and actual InterceptorMethodKind into a String for the error message.
            String ejbClassName = ivEjbClass.getName();
            String required = mapInterceptorMethodKind(requiredKind, true, annotation);
            String actual = mapInterceptorMethodKind(actualKind, false, annotation);

            // Now build and log the error message based on type of EJB.
            StringBuilder sb = new StringBuilder();
            if (ivMDB)
            {
                // CNTR0243E: Because the {0} enterprise bean implements the javax.ejb.MessageDriven interface,
                // the {1} method must be a {2} method and not a {3} method.
                sb.append("CNTR0243E: Because the ").append(ejbClassName);
                sb.append(" enterprise bean implements the javax.ejb.MessageDriven interface, the ");
                sb.append(methodName).append(" method must be a ").append(required);
                sb.append(" method and not a ").append(actual).append(" method.");
                Tr.error(tc, "INVALID_MDB_CALLBACK_METHOD_CNTR0243E"
                         , new Object[] { ejbClassName, methodName, required, actual });

            }
            else if (ivSLSB)
            {
                // CNTR0241E: Because the {0} enterprise bean implements the javax.ejb.SessionBean interface,
                // the {1} method must be a {2} method and not a {3} method.
                sb.append("CNTR0241E: Because the ").append(ejbClassName);
                sb.append(" enterprise bean implements the javax.ejb.SessionBean interface, the ");
                sb.append(methodName).append(" method must be a ").append(required);
                sb.append(" method and not a ").append(actual).append(" method.");
                Tr.error(tc, "INVALID_SLSB_CALLBACK_METHOD_CNTR0241E"
                         , new Object[] { ejbClassName, methodName, required, actual });
            }
            else if (ivSFSB)
            {
                // CNTR0242E: Because the {0} enterprise bean implements the javax.ejb.SessionBean interface,
                // the {1} method must be a {2} method and not a {3} method.
                sb.append("CNTR0242E: Because the ").append(ejbClassName);
                sb.append(" enterprise bean implements the javax.ejb.SessionBean interface, the ");
                sb.append(methodName).append(" method must be a ").append(required);
                sb.append(" method and not a ").append(actual).append(" method.");
                Tr.error(tc, "INVALID_SFSB_CALLBACK_METHOD_CNTR0242E"
                         , new Object[] { ejbClassName, methodName, required, actual });
            }

            // Throw the EJBConfigurationException with message text that was built for the error.
            throw new EJBConfigurationException(sb.toString());
        }
    }

    /**
     * Maps a specified EJB container callback method name to a InterceptorMethodKind.
     *
     * @param methodName is the name of method of a EJB class.
     *
     * @return the InterceptorMethodKind. Note, a null reference is returned if the
     *         the method name is not ejbCreate, ejbActivate, ejbPassivate, or
     *         ejbRemove.
     */
    // d463727 entire method added; F743-17763 method rewritten
    private InterceptorMethodKind mapEjbCallbackName(String methodName)
    {
        for (InterceptorMethodKind kind : InterceptorMethodKind.values())
        {
            if (kind.isLifecycle() && methodName.equals(kind.getLifecycleCallbackMethodName()))
            {
                return kind;
            }
        }

        return null;
    }

    /**
     * Maps a specified InterceptorMethodKind to one of the EJB 2.1 container
     * callback method names.
     *
     * @param kind is the InterceptorMethodKind.
     *
     * @param honorSFSBMapping must be true if we should map to @Init instead of
     * @PostConstruct if the EJB is a SFSB.
     *
     * @param annotation must be true if annotation is used rather than xml.
     *
     * @return returns either ejbCreate, ejbActivate, ejbPassivate, or
     *         ejbRemove or the empty string if not one of the callback methods.
     */
    // d463727 entire method added; F743-17763 method rewritten
    private String
                    mapInterceptorMethodKind(InterceptorMethodKind kind, boolean honorSFSBMapping, boolean annotation)
    {
        if (kind.isLifecycle())
        {
            if (kind == InterceptorMethodKind.POST_CONSTRUCT && ivSFSB && honorSFSBMapping)
            {
                if (annotation)
                {
                    return "@Init";
                }
                return "init-method";
            }

            if (annotation)
            {
                return kind.getAnnotationClass().getSimpleName();
            }
            return kind.getXMLElementName();
        }

        return "";
    }

    /**
     * Find the InterceptorBinding for a specified business method of the EJB.
     *
     * @param method is the Method object for the business method.
     *
     * @return InterceptorBinding or null if none is found for the business method.
     */
    // d472972 - rewrote entire method to pass CTS.
    private EJBInterceptorBinding findInterceptorBindingForMethod(final Method method)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "findInterceptorBindingForMethod: " + method.toString());
        }

        // Use style 4 binding if there is one for the method.
        String methodSignature = MethodAttribUtils.methodSignature(method);
        if (isTraceOn && tc.isDebugEnabled())
        {
            Tr.debug(tc, "lookup style 4 for method signature: " + methodSignature);
        }
        EJBInterceptorBinding binding = ivStyle4InterceptorBindingMap.get(methodSignature);

        // If no Style 4 binding, then see if there is a Style 3 binding.
        if (binding == null)
        {
            String methodName = method.getName();
            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "lookup style 3 for method: " + methodName);
            }
            binding = ivStyle3InterceptorBindingMap.get(methodName);
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            if (binding != null)
            {
                Tr.exit(tc, "findInterceptorBindingForMethod found: ");
                binding.dump();
            }
            else
            {
                Tr.exit(tc, "findInterceptorBindingForMethod, interceptor-binding not found");
            }
        }
        return binding;
    }

    /**
     * Get an ordered list of class level interceptors to be used for this EJB.
     *
     * @return ordered list of interceptors to use at class level.
     *
     * @throws EJBConfigurationException is thrown if interceptor-order element is not a
     *             total ordering of the interceptors for this class.
     */
    private ArrayList<String> orderClassLevelInterceptors() throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "orderClassLevelInterceptors");
        }

        // Create the default ordering of class level interceptor names
        // for this EJB.
        ArrayList<String> orderedList = new ArrayList<String>();

        // First add default interceptor names if they are not excluded
        // at the class level.
        if (ivExcludeDefaultFromClassLevel == false)
        {
            if (ivDefaultInterceptorNames.size() > 0)
            {
                orderedList.addAll(ivDefaultInterceptorNames);
            }
        }

        // Now add in the class level interceptors.
        if (ivClassInterceptorNames.size() > 0)
        {
            orderedList.addAll(ivClassInterceptorNames);
        }

        // Now check whether order is overridden by a <interceptor-order> deployment
        // descriptor for this EJB.
        if (ivClassInterceptorBinding != null)
        {
            List<String> order = ivClassInterceptorBinding.ivInterceptorOrder; // d453477
            if (!order.isEmpty())
            {
                // d472972 start
                // Yep, default order is being overridden.  Verify the <interceptor-order> is
                // a complete ordering of the interceptors.
                ArrayList<String> interceptorOrder = new ArrayList<String>(order);
                if (interceptorOrder.containsAll(orderedList))
                {
                    // The order list is complete, so just use the order that was
                    // provided by the interceptor-order deployment descriptor.
                    orderedList = interceptorOrder;
                }
                else
                {
                    // CNTR0227E: The {1} enterprise bean has an interceptor-order element which specifies
                    // the following interceptor-order list: {0}.  This list is not a total ordering of the
                    // class-level interceptors for this bean.  It is missing the following interceptor names: {2}
                    List<String> missingList;
                    if (interceptorOrder.size() < orderedList.size())
                    {
                        orderedList.removeAll(interceptorOrder);
                        missingList = orderedList;
                    }
                    else
                    {
                        interceptorOrder.removeAll(orderedList);
                        missingList = interceptorOrder;
                    }
                    String ejbName = ivJ2EEName.toString();
                    Object[] data = new Object[] { ejbName, order, missingList };
                    Tr.warning(tc, "PARTIAL_CLASS_INTERCEPTOR_ORDER_CNTR0227E", data);
                    throw new EJBConfigurationException(order + " is not a total ordering of class-level interceptors for EJB "
                                                        + ejbName + ". It is missing interceptor names: " + missingList);
                }
                // d472972 end
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "orderClassLevelInterceptors: " + orderedList);
        }

        return orderedList;
    }

    /**
     * Get an ordered list of method level interceptors for specified
     * method of an EJB.
     *
     * @param m the method
     *
     * @return ordered list of interceptors to use for this method.
     *
     * @throws EJBConfigurationException is thrown if order is not a total ordering of the interceptors
     *             for the method as required by EJB specification.
     */
    private ArrayList<String> orderMethodLevelInterceptors(Method m) // d630717
    throws EJBConfigurationException
    {
        // Create the default ordering of class level interceptor names
        // for this EJB.
        ArrayList<String> orderedList = new ArrayList<String>();

        // Find the InterceptorBinding for the method.
        EJBInterceptorBinding binding = findInterceptorBindingForMethod(m);

        // Determine if class interceptors are disabled for this method.
        boolean excludeClassInterceptors = isClassInterceptorsExcluded(m, binding);

        // Determine if default interceptors need to be disabled for this method.
        boolean excludeDefaultInterceptors = isDefaultInterceptorsExcluded(m, binding);

        // Determine if there are any method-level interceptor classes for this method.
        ArrayList<String> interceptors = addMethodLevelInterceptors(m, binding);

        // d630717 - Per CTS, we must use the class-level interceptor order.  The
        // spec is silent on the topic, but it seems reasonable to do so if we
        // are not excluding class-level interceptors and method-level agrees
        // with class-level about whether or not to exclude default interceptors.
        if (!excludeClassInterceptors && excludeDefaultInterceptors == ivExcludeDefaultFromClassLevel)
        {
            orderedList.addAll(ivClassInterceptorOrder);
        }
        else
        {
            // First add default interceptor names if they are not excluded.
            if (!excludeDefaultInterceptors)
            {
                orderedList.addAll(ivDefaultInterceptorNames);
            }

            // Now add in the class level interceptors.
            if (!excludeClassInterceptors)
            {
                orderedList.addAll(ivClassInterceptorNames);
            }
        }

        // Now add in the method-level interceptors.
        orderedList.addAll(interceptors);

        // Now check whether order is overridden by a <interceptor-order> deployment
        // descriptor for this EJB.
        if (binding != null && !binding.ivInterceptorOrder.isEmpty()) // d630717
        {
            List<String> order = binding.ivInterceptorOrder; // d630717

            // d630717 - Ensure that every class in the order can be loaded.
            updateNamesToClassMap(order);

            // d472972 start
            // Yep, default order is being overridden.  Verify the <interceptor-order> is
            // a complete ordering of the interceptors.
            ArrayList<String> interceptorOrder = new ArrayList<String>(order);
            if (interceptorOrder.containsAll(orderedList))
            {
                // The order list is complete, so just use the order that was
                // provided by the interceptor-order deployment descriptor.
                orderedList = interceptorOrder;
            }
            else
            {
                List<String> missingList;
                if (interceptorOrder.size() < orderedList.size())
                {
                    orderedList.removeAll(interceptorOrder);
                    missingList = orderedList;
                }
                else
                {
                    interceptorOrder.removeAll(orderedList);
                    missingList = interceptorOrder;
                }

                // CNTR0228E: The {2} enterprise bean specifies method-level interceptors for the
                // {1} method with the following interceptor-order list: {0}.  This list is not a
                // total ordering of the method-level interceptors for this bean.  The list is missing
                // the following interceptor names: {3}.
                String ejbName = ivJ2EEName.toString();
                String methodName = m.getName(); // d630717
                Object[] data = new Object[] { ejbName, methodName, order, missingList };
                Tr.warning(tc, "PARTIAL_METHOD_INTERCEPTOR_ORDER_CNTR0228E", data);
                throw new EJBConfigurationException(order + " is not a total ordering of method-level interceptors for method "
                                                    + methodName + " of EJB " + ejbName + ". It is missing interceptor names: " + missingList);
            }
            // d472972 end
        }

        return orderedList;
    }

}
