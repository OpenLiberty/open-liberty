/*******************************************************************************
 * Copyright (c) 1998, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.AccessTimeout;
import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.csi.ActivitySessionMethod;
import com.ibm.websphere.csi.ActivitySessionAttribute;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.MethodInterface;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.javaee.dd.ejb.AsyncMethod;
import com.ibm.ws.javaee.dd.ejb.ConcurrentMethod;
import com.ibm.ws.javaee.dd.ejb.ContainerTransaction;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.ExcludeList;
import com.ibm.ws.javaee.dd.ejb.MethodPermission;
import com.ibm.ws.javaee.dd.ejb.NamedMethod;
import com.ibm.ws.javaee.dd.ejb.Session;
import com.ibm.ws.javaee.util.DDUtil;

/**
 * 
 * A collection of useful utility methods for parsing and processing
 * method-level attributes. <p>
 * 
 */
public class MethodAttribUtils
{
    private static final String CLASS_NAME = MethodAttribUtils.class.getName();
    private static TraceComponent tc = Tr.register(MethodAttribUtils.class, "MetaData", "com.ibm.ejs.container.container");

    private static TraceComponent tcDebug = Tr.register(CLASS_NAME + "_Validation ",
                                                        MethodAttribUtils.class,
                                                        "MetaDataValidation",
                                                        "com.ibm.ejs.container.container"); // F743-1752.1

    static
    {
        populateTxMofMap();
        populateTxAttrFromJEE15Map();
        populateIsoStringMap();
    }

    /**
     * getAnnotationCMTTransactions fill the TransactionAttribute array with data from
     * Annotations. //d366845.8
     */
    public static final void getAnnotationCMTransactions(TransactionAttribute[] tranAttrs,
                                                         int methodInterfaceType,
                                                         Method[] beanMethods,
                                                         BeanMetaData bmd)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getAnnotationCMTransactions");

        for (int i = 0; i < beanMethods.length; i++)
        {
            // Only get the value from annotations if it is not already set by WCCM
            if (tranAttrs[i] == null)
            {
                javax.ejb.TransactionAttribute methTranAttr = null;

                // Don't bother looking for annotations, if we're told there are none.
                if (!bmd.metadataComplete) {
                    methTranAttr = beanMethods[i].getAnnotation(javax.ejb.TransactionAttribute.class);

                    if (methTranAttr == null &&
                        // EJB 3.2, section 4.3.14:
                        // "A stateful session bean's PostConstruct, PreDestroy,
                        // PrePassivate or PostActivate lifecycle callback
                        // interceptor method is invoked in the scope of a
                        // transaction determined by the transaction attribute
                        // specified in the lifecycle callback method's metadata
                        // annotations or deployment descriptor"
                        (bmd.type != InternalConstants.TYPE_STATEFUL_SESSION ||
                        methodInterfaceType != InternalConstants.METHOD_INTF_LIFECYCLE_INTERCEPTOR))
                    {
                        methTranAttr = beanMethods[i].getDeclaringClass().getAnnotation(javax.ejb.TransactionAttribute.class);

                        if (isTraceOn && tc.isDebugEnabled() &&
                            methTranAttr != null)
                            Tr.debug(tc, beanMethods[i].getName() + " from class " +
                                         beanMethods[i].getDeclaringClass().getName());
                    }
                }

                if (methTranAttr == null) {
                    // set the default value
                    if (methodInterfaceType == InternalConstants.METHOD_INTF_LIFECYCLE_INTERCEPTOR &&
                        bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {
                        // "By default a stateful session bean's PostConstruct,
                        // PreDestroy, PrePassivate and PostActivate methods are
                        // executed in an unspecified transactional context."
                        //
                        // WebSphere implements an unspecified transactional
                        // context as TX_NOT_SUPPORTED.
                        tranAttrs[i] = TransactionAttribute.TX_NOT_SUPPORTED;
                    } else {
                        tranAttrs[i] = TransactionAttribute.TX_REQUIRED;
                    }

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, beanMethods[i].getName() + " = REQUIRED (defaulted)");
                } else {
                    // Switch the javax.ejb.TransactionAttribute to com.ibm.websphere.csi.TransactionAttribute
                    int tranType = methTranAttr.value().ordinal();
                    tranAttrs[i] = txAttrFromJEE15Map[tranType];

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, beanMethods[i].getName() + " = " + methTranAttr.value());
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "getAnnotationsCMTransactions", Arrays.toString(tranAttrs));
        }

    }

    /**
     * F743-4582
     * Determines which methods on the specified Enterprise Bean should be
     * marked asynchronous by updating the passed-in boolean array.
     * 
     * Pre-conditions:
     * <ol>
     * <li>asynchMethodFlags, ejbMethods, wccmEnterpriseBean, and bmd must be non-null</li>
     * <li>asynchMethodFlags.length MUST equal ejbMethods.length</li>
     * <li>bmd.classLoader must be set to the bean's classloader (not null)</li>
     * <li>bmd.ivBusiness[Local|Remote]InterfaceClasses must be non-null</li>
     * </ol>
     * 
     * @param asynchMethodFlags - array indicating which ejb methods are asynchronous
     *            - corresponds to the methods in <code>ejbMethods</code>
     * @param ejbMethods - array of methods on the bean class, limited by the interface
     *            currently being processed
     * @param wccmEnterpriseBean - Enterprise bean object read in from ejb-jar.xml via WCCM
     * @param methodInterface - MethodInterface type (Local, Remote, etc.) currently being
     *            processed
     * 
     * @return True if at least one asynchronous method was found on this bean.
     * 
     * @throws EJBConfigurationException if the XML DD contains invalid meta-data
     */
    public static boolean getXMLAsynchronousMethods(boolean[] asynchMethodFlags,
                                                    MethodInterface methodInterface,
                                                    String[] methodNames,
                                                    Class<?>[][] methodParamTypes,
                                                    EnterpriseBean wccmEnterpriseBean)
                    throws EJBConfigurationException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        boolean asynchMethodFound = false; //d621123

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "getXMLAsynchronousMethods", new Object[] {
                                                                    asynchMethodFlags, wccmEnterpriseBean, methodInterface });
        }

        //iterate through the asynch-methodTypes in the deployment descriptor
        // and match them with the methods defined in the ejbMethods array.
        // then for each match, set the asynchMethodsFlags array at the same
        // index to true. all others should be false (or left as they were...)

        //first verify if all methods are set to asynch (i.e. if the asynch applies
        // to the entire bean)

        if (wccmEnterpriseBean.getKindValue() == EnterpriseBean.KIND_SESSION &&
            //d599046 - check SERVICE_ENDPOINT
            methodInterface != MethodInterface.SERVICE_ENDPOINT)
        {
            Session sb = (Session) wccmEnterpriseBean;
            //F00743.9717
            List<AsyncMethod> asynchMethods = sb.getAsyncMethods();
            for (AsyncMethod am : asynchMethods) {

                String methodName = am.getMethodName(); //d603858
                if (methodName == null || "".equals(methodName.trim())) {
                    //error in ejb-jar.xml! method-name element is required
                    Tr.error(tc, "INVALID_ASYNC_METHOD_ELEMENT_MISSING_METHOD_NAME_CNTR0203E", sb.getName());
                    throw new EJBConfigurationException("Async method declared without a required method-name");
                }

                List<String> parms = am.getMethodParamList();

                if ("*".equals(methodName) && parms != null) {
                    //error in ejb-jar.xml! cannot specify parms with wildcard method
                    Tr.error(tc, "INVALID_ASYNC_METHOD_ELEMENT_SPECIFIED_PARMS_WITH_WILDCARD_METHOD_CNTR0204E", sb.getName());
                    throw new EJBConfigurationException("Cannot specify parameters when specifying a wildcard method-name for async methods");
                }

                //style type 1 - unqualified wildcard:
                if ("*".equals(methodName))
                {
                    //in this case, all methods are asynchronous:
                    for (int i = 0; i < asynchMethodFlags.length; i++) {
                        asynchMethodFlags[i] = true;
                    }
                    asynchMethodFound = true; //d621123

                    //if this is the case, there is no need for further processing
                    if (isTraceOn && tc.isEntryEnabled()) {
                        Tr.exit(tc, "getXMLAsynchronousMethods - all methods are marked async");
                    }
                    return asynchMethodFound; //d621123
                }

                // if we are here, then methodName is not a wildcard - style type 2 (no parms) & 3 (parms)

                //iterate over method array and check method name and parms (if specified):
                for (int i = 0; i < methodNames.length; i++) {
                    if (methodNames[i] != null && methodNames[i].equals(methodName)) { //d599046 - null check
                        if (parms == null || DDUtil.methodParamsMatch(parms, methodParamTypes[i])) { // RTC100828
                            asynchMethodFlags[i] = true;
                            asynchMethodFound = true; //d621123
                        }
                    }
                } // end for (int i=0; i<ejbMethods.length; i++)
            } // end for (AsyncMethod am : asynchMethods)

            if (isTraceOn && tc.isDebugEnabled()) {
                for (int i = 0; i < methodNames.length; i++) {
                    Tr.debug(tc, methodNames[i] + Arrays.toString(methodParamTypes[i]) + " == " + //d599046 - null check
                                 (asynchMethodFlags[i] ? "Asynchronous" : "Synchronous"));
                }
            }

        } // end if (wccmEnterpriseBean.isSession())
        else {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Not a session bean");
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "getXMLAsynchronousMethods");
        }
        return (asynchMethodFound); //d621123
    }

    /**
     * <code>getAsynchronousMethods<code> builds an array of boolean results. This
     * output array contains one entry corresponding to each Method object in beanMethods array
     * that is passed into this method. Each entry in the output array is either set to
     * false (ie. if the corresponding beanMethod is not asynchronous), or true (if the
     * corresponding beanMethod is asynchronous).
     * 
     * @param beanMethods Input array of Method objects representing the methods of this EJB.
     * @param asynchMethods Output array of boolean entries that identifies which methods
     *            in the input array are asynchronous methods.
     * 
     * @return True if at least one asynchronous method was found on this bean.
     */
    public static boolean getAsynchronousMethods(Method[] beanMethods,
                                                 boolean[] asynchMethodFlags,
                                                 MethodInterface methodInterface)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getAsynchronousMethods");

        Asynchronous asynchAnnotation = null;
        boolean asynchMethodFound = false;

        if (methodInterface != MethodInterface.SERVICE_ENDPOINT) { //d599046
            for (int i = 0; i < beanMethods.length; i++)
            {
                // Check to make sure this beanMethod is not null because it can be if the method
                // was a remove method.  Remove methods get special handling.
                if (beanMethods[i] != null) {

                    // Only get the value from annotations if it is not already set by WCCM.  There is no way to
                    // turn off the asynchronous setting using annotations or xml (ie. it can only be turned on).
                    if (asynchMethodFlags[i] == false)
                    {
                        asynchAnnotation = null;

                        // Try to get method level annotation
                        asynchAnnotation = beanMethods[i].getAnnotation(Asynchronous.class);

                        if (asynchAnnotation == null)
                        {
                            // Method level annotation not found so check for class level annotation
                            asynchAnnotation = beanMethods[i].getDeclaringClass().getAnnotation(Asynchronous.class);

                            if (isTraceOn && tc.isDebugEnabled() &&
                                asynchAnnotation != null) {

                                // Class level @Asynchronous annotation found
                                Tr.debug(tc, "The " + beanMethods[i].getName() + " method on the " +
                                             beanMethods[i].getDeclaringClass().getName() + " bean is configured asynchronous at the class level");
                            }
                        } else {

                            // Method level @Asynchronous annotation found
                            if (isTraceOn && tc.isDebugEnabled()) {
                                Tr.debug(tc, "The " + beanMethods[i].getName() + " method on the " +
                                             beanMethods[i].getDeclaringClass().getName() + " bean is configured asynchronous at the method level");
                            }
                        }

                        // Update array of asynch methods with true or false for the current method
                        if (asynchAnnotation != null)
                        {
                            asynchMethodFlags[i] = true;
                            asynchMethodFound = true;
                        } else {
                            asynchMethodFlags[i] = false;
                        }

                    } // no annotation at method level

                } else {

                    // bean method is null so set asynch method array entry to false (ie. the default value)
                    // F743-4582 Update - do not set this to false, as it would overwrite the value from WCCM
                    //asynchMethodFlags[i] = false;
                }
            } // for all bean methods
        } // end if (methodInterface != MethodInterface.SERVICE_ENDPOINT)

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "getAsynchonousMethods");
        }
        return (asynchMethodFound);

    } // getAsynchronousMethods

    /**
     * Check all methods for method-level Security annotations. Specifically
     * <code>RolesAllowed</code>, <code>PermitAll</code> and<code>DenyAll</code>.
     * If no Roles are defined via annotations or XML, then default to PermitAll.
     * 
     * @param beanMethods Array of methods for this EJB.
     * @param rolesAllowed ArrayList array to hold all allowed roles (value returned)
     * @param denyAll boolean array indicating which methods are designated
     *            to deny all access (value returned).
     * @param permitAll boolean array indicating which methods are designated
     *            to permit all roles (value returned).
     */
    //366845.11.2
    public static final void getAnnotationsForSecurity(Method[] beanMethods,
                                                       ArrayList<String>[] rolesAllowed,
                                                       boolean[] denyAll,
                                                       boolean[] permitAll)
                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getAnnotationsForSecurity");

        // Check for security annotations @PermitAll, @DenyAll, and @RolesAllowed
        // The following rules apply:
        // 1. If a method has been defined in XML to permit all access or deny
        //    all access, then we do not need to look at annotations at all.
        // 2. Any role defined in XML will override any roles defined in
        //    annotations at either the class or method level.  So
        //    the values contained in the @RolesAllowed annotations will only
        //    be used for a method if no roles are yet defined for that method
        //    via XML.
        // 3. Roles defined within XML will remain in affect regardless of
        //    any annotations defined.
        //
        // Also, from the "Common Annotations for Java" Specification: JSR 250
        // 1. PermitAll, DenyAll and RolesAllowed annotations MUST NOT be applied
        //    on the same method or class.
        // 2. If PermitAll is applied at the class level and RolesAllowed or
        //    DenyAll are applied on methods of the same class, then the method
        //    level annotations take precedence over the class level annotation.
        // 3. If DenyAll is specified at the class level and PermitAll or
        //    RolesAllowed are specified on methods of the same class, then
        //    the method level annotation takes precedence over the class level
        //    annotation.
        // 4. If RolesAllowed is specified at the class level and PermitAll
        //    or DenyAll are specified on methods, then the method level annotation
        //    takes precedence over the class level annotation.
        //
        // Also: The RolesAllowed annotation can be specified on a class or on method(s).
        // Specifying it at a class level means that it applies to all the methods in
        // the class. Specifying it on a method means that it is applicable to that
        // method only. If applied at both the class and method level, the method value
        // overrides the class value.
        //
        for (int i = 0; i < beanMethods.length; i++)
        {
            RolesAllowed classRolesAllowed = null;
            RolesAllowed methRolesAllowed = null;
            DenyAll classDenyAll = null; // F743-21028
            DenyAll methDenyAll = null;
            PermitAll classPermitAll = null;
            PermitAll methPermitAll = null;
            Method beanMethod = beanMethods[i];

            if (beanMethod != null) {

                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Processing method: " + beanMethod.getName());
                }
                // Is Security Policy information defined in XML?.
                if ((denyAll[i]) || (permitAll[i]) || (rolesAllowed[i] != null)) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "DenyAll, PermitAll, or Roles already set in XML.");
                    continue;
                }

                // Process DenyAll - can be either method or class level, but the
                // class-level version can be overridden by @PermitAll or
                // @RolesAllowed at the method-level, so only the method-level
                // will be evaluated here.
                methDenyAll = beanMethod.getAnnotation(DenyAll.class);
                if (methDenyAll != null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "DenyAll annotation set: " + methDenyAll.toString());
                    denyAll[i] = true;
                }

                // PermitAll can be at either method or class level,
                // but the class-level version can be overridden by @RolesAllowed
                // at the method-level.  So this point we will only look for
                // @PermitAll at the method-level.
                methPermitAll = beanMethod.getAnnotation(PermitAll.class);
                if (methPermitAll != null) {
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "PermitAll annotation set: " + methPermitAll.toString());
                    }
                    if (methDenyAll != null) {
                        Tr.error(tc, "CONFLICTING_ANNOTATIONS_CONFIGURED_ON_METHOD_CNTR0150E",
                                 new Object[] { "@PermitAll", "@DenyAll", beanMethod.getName(), beanMethod.getDeclaringClass().getName() });
                        throw new EJBConfigurationException("@DenyAll and @PermitAll annotations are both set on class:  " +
                                                            beanMethod.getDeclaringClass().getName() +
                                                            " method: " + beanMethod.getName());
                    }
                    permitAll[i] = true;
                }

                // Check for @RolesAllowed at the method.
                methRolesAllowed = beanMethod.getAnnotation(RolesAllowed.class);
                if (methRolesAllowed != null)
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Method-level RolsAllowed annotation set. ");

                    if ((methDenyAll != null)) {
                        Tr.error(tc, "CONFLICTING_ANNOTATIONS_CONFIGURED_ON_METHOD_CNTR0150E",
                                 new Object[] { "@RolesAllowed", "@DenyAll", beanMethod.getName(), beanMethod.getDeclaringClass().getName() });
                        throw new EJBConfigurationException("@RolesAllowed and @DenyAll annotations are both set on class:  " +
                                                            beanMethod.getDeclaringClass().getName() + " method: " + beanMethod.getName());
                    } else if (methPermitAll != null) {
                        Tr.error(tc, "CONFLICTING_ANNOTATIONS_CONFIGURED_ON_METHOD_CNTR0150E",
                                 new Object[] { "RolesAllowed", "@PermitAll", beanMethod.getName(), beanMethod.getDeclaringClass().getName() });
                        throw new EJBConfigurationException("@RolesAllowed and @PermitAll annotations are both set on class:  " +
                                                            beanMethod.getDeclaringClass().getName() +
                                                            " method: " + beanMethod.getName());
                    }

                    rolesAllowed[i] = new ArrayList<String>();
                    for (String role : methRolesAllowed.value()) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Adding role: " + role);

                        // Duplicate roles are not allowed.
                        boolean roleAdded = rolesAllowed[i].add(role);
                        if (!roleAdded) {
                            Tr.error(tc, "DUPLICATE_ROLES_SPECIFIED_IN_METHOD_ANNOTATION_CNTR0151E",
                                     new Object[] { role, beanMethod.getName(), beanMethod.getDeclaringClass().getName() });
                            throw new EJBConfigurationException("Role: " + role + " is defined multiple times on the @RolesAllowed annotation for class: " +
                                                                beanMethod.getDeclaringClass().getName() +
                                                                " method: " + beanMethod.getName());
                        }

                    } // for (String role : methRolesAllowed.value())
                } // if (methRolesAllowed != null)

                // Get role values from class level.
                // These are only valid if nothing has been set at the method level
                // already.  We have to check these for each method because methods
                // might be inherited and only the class annotations from the declaring
                // class will be applicable for this particular method.
                if ((methRolesAllowed == null) &&
                    (methDenyAll == null) &&
                    (methPermitAll == null))
                {
                    classRolesAllowed = beanMethod.getDeclaringClass().getAnnotation(RolesAllowed.class);
                    classPermitAll = beanMethod.getDeclaringClass().getAnnotation(PermitAll.class);
                    classDenyAll = beanMethod.getDeclaringClass().getAnnotation(DenyAll.class); // F743-21028

                    // Must not be more than one of @DenyAll, @PermitAll or
                    // @RolesAllowed set at class level.                   F743-21028
                    String conflict1 = null, conflict2 = null;
                    if (classRolesAllowed != null)
                    {
                        if (classPermitAll != null)
                        {
                            conflict1 = "@RolesAllowed";
                            conflict2 = "@PermitAll";
                        }
                        else if (classDenyAll != null)
                        {
                            conflict1 = "@RolesAllowed";
                            conflict2 = "@DenyAll";
                        }
                    }
                    else if (classPermitAll != null && classDenyAll != null)
                    {
                        conflict1 = "@PermitAll";
                        conflict2 = "@DenyAll";
                    }

                    if (conflict1 != null)
                    {
                        Tr.error(tc, "CONFLICTING_ANNOTATIONS_CONFIGURED_ON_CLASS_CNTR0152E",
                                 new Object[] { conflict1, conflict2, beanMethod.getDeclaringClass().getName() });
                        throw new EJBConfigurationException(conflict1 + " and " + conflict2 +
                                                            " must not both be set on class level annotation" +
                                                            " of class: " +
                                                            beanMethod.getDeclaringClass().getName());
                    }

                    if (classRolesAllowed != null)
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Class RolesAllowed annotation set: ");

                        rolesAllowed[i] = new ArrayList<String>();

                        for (String role : classRolesAllowed.value()) {
                            boolean roleAdded = rolesAllowed[i].add(role);
                            // Duplicate Roles will not be allowed.
                            if (!roleAdded) {
                                Tr.error(tc, "DUPLICATE_ROLES_SPECIFIED_IN_CLASS_ANNOTATION_CNTR0153E",
                                         new Object[] { role, beanMethod.getDeclaringClass().getName() });
                                throw new EJBConfigurationException("Role: " + role + " is defined multiple times on the @RolesAllowed class level annotation of class: " +
                                                                    beanMethod.getDeclaringClass().getName());
                            }

                        } // for (String role : classRolesAllowed.value())

                    } //if ((methRolesAllowed == null) &&

                    if (classPermitAll != null) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Class PermitAll annotation set: ");
                        permitAll[i] = true;
                    }

                    else if (classDenyAll != null) // F743-21028
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Class DenyAll annotation set");
                        denyAll[i] = true;
                    }

                } // if ((methRolesAllowed == null) &&

            } //if (beanMethod != null)

            if (isTraceOn && tc.isDebugEnabled())
            {
                String methodName = "Unknown";
                if (beanMethods[i] != null) {
                    methodName = beanMethods[i].getName();
                }
                Tr.debug(tc, methodName + " denyAll:  " + denyAll[i]);
                Tr.debug(tc, methodName + " permitAll:  " + permitAll[i]);
                if (rolesAllowed[i] == null) {
                    Tr.debug(tc, methodName + " roles:  null");
                } else {
                    Tr.debug(tc, methodName + " roles:  " +
                                 Arrays.toString(rolesAllowed[i].toArray()));
                }
            }

        } // for (int i = 0; i < beanMethods.length; i++)

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "getAnnotationsForSecurity");
        }

    }

    /**
     * Returns a map of method name to list of business methods.
     */
    static Map<String, List<Method>> getBusinessMethodsByName(BeanMetaData bmd) {
        Map<String, List<Method>> methodsByName = new HashMap<String, List<Method>>();
        collectBusinessMethodsByName(methodsByName, bmd.methodInfos);
        collectBusinessMethodsByName(methodsByName, bmd.localMethodInfos);
        return methodsByName;
    }

    private static void collectBusinessMethodsByName(Map<String, List<Method>> methodsByName, EJBMethodInfoImpl[] methodInfos) {
        if (methodInfos != null) {
            for (EJBMethodInfoImpl methodInfo : methodInfos) {
                String name = methodInfo.getName();

                List<Method> methods = methodsByName.get(name);
                if (methods == null) {
                    methods = new ArrayList<Method>();
                    methodsByName.put(name, methods);
                }

                methods.add(methodInfo.getMethod());
            }
        }
    }

    /**
     * Finds the first matching method in a map of method name to list of methods.
     */
    static Method findMatchingMethod(Map<String, List<Method>> methodsByName, com.ibm.ws.javaee.dd.ejb.Method me) {
        List<Method> methods = methodsByName.get(me.getMethodName());
        if (methods == null) {
            return null;
        }

        List<String> meParms = me.getMethodParamList();
        if (meParms == null) {
            return methods.get(0);
        }

        for (Method method : methods) {
            if (DDUtil.methodParamsMatch(meParms, method.getParameterTypes())) {
                return method;
            }
        }

        return null;
    }

    // The precedences for <method> styles:
    // 1. <method-name>*</method-name> (with or without <method-intf>)
    // 2. <method-name>METHOD</method-name> (with or without <method-intf>)
    // 3. <method-name>METHOD</method-name> and <method-params>...</method-params
    //    (with or without <method-intf>)
    //
    // The spec implies that style 1 with method-intf is higher precedence than
    // without.  The spec also implies the same for style 2, but we have never
    // implemented that.  The spec does not imply the same for style 3 for
    // unknown reasons.
    private static final int XML_STYLE1 = 1;
    private static final int XML_STYLE1_METHOD_INTF = 2;
    private static final int XML_STYLE2 = 3;
    private static final int XML_STYLE3 = 4;

    /**
     * getXMLCMTransactions fills the TransactionAttribute array of given type
     * from the XML based WCCM data. If transaction attributes are not specified
     * for a method, then the array element will be left null.
     * 
     * @param tranAttrs the output array
     * @param type the method interface type of the methods
     * @param methodNames the method names
     * @param methodParamTypes the method parameter types
     * @param tranAttrList the transaction attributes declared in ejb-jar.xml
     * @param bmd the bean metadata
     */
    public static final void getXMLCMTransactions(TransactionAttribute[] tranAttrs,
                                                  int type,
                                                  String[] methodNames,
                                                  Class<?>[][] methodParamTypes,
                                                  List<ContainerTransaction> tranAttrList,
                                                  BeanMetaData bmd) //PK93643
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getXMLCMTransactions", Arrays.toString(tranAttrs));

        if (tranAttrList != null) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "*** TX list has " + tranAttrList.size() + " TX attribute(s) to process ***");
            }

            // array to keep track of what kind of style last set the method-specific behavior
            int[] highestStyleOnMethod = new int[methodNames.length];
            // Lazily initialized getBusinessMethodsByName.
            Map<String, List<Method>> businessMethodsByName = null;

            // Elements of highestStyleOnMethod[] are initialized to zero by Java rules.

            // i : TranAttrList loop
            // j : MethodElements loop
            // k : Method loop

            for (int i = 0; i < tranAttrList.size(); ++i) {
                ContainerTransaction methodTransaction = tranAttrList.get(i);
                int tranType = methodTransaction.getTransAttributeTypeValue(); //LIDB2257.19
                List<com.ibm.ws.javaee.dd.ejb.Method> methodElements = methodTransaction.getMethodElements();

                for (int j = 0; j < methodElements.size(); ++j) {
                    com.ibm.ws.javaee.dd.ejb.Method me = methodElements.get(j);
                    String ejbNameFromMethodElement = me.getEnterpriseBeanName();
                    int meType = me.getInterfaceTypeValue();

                    // If ejbNameFromMethodElement is null the customer most likey has an xml
                    // coding error in the method element where the ejb-name is incorrect.
                    if (ejbNameFromMethodElement == null) { //PK93643
                        Tr.warning(tc, "INVALID_CONTAINER_TRANSACTION_XML_CNTR0121W",
                                   new Object[] { bmd.getJ2EEName().getModule(), txMOFMap[tranType] });
                    }

                    if (isTraceOn && tc.isDebugEnabled()) {
                        if (j == 0) {
                            Tr.debug(tc, "#" + i + " Method transaction type: " + txMOFMap[tranType]);
                        }
                    }

                    if ((ejbNameFromMethodElement != null) && (ejbNameFromMethodElement.equals(bmd.enterpriseBeanName))) { //PK93643
                        String meName = me.getMethodName().trim();
                        if (meName.equals("*")) {
                            if (meType == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED) {
                                // EJB 3.2, section 8.3.7.2.1, style 1:
                                // "This style may be used for stateful session
                                // bean lifecycle callback methods to specify
                                // their transaction attributes if used with the
                                // method-intf element value LifecycleCallback."
                                if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION &&
                                    type == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_LIFECYCLE_CALLBACK) {
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "skipping wildcard for stateful LifecycleCallback");
                                    }
                                    continue;
                                }

                                // style type 1 -- wildcard on all bean methods
                                for (int k = 0; k < methodNames.length; ++k) {
                                    if (highestStyleOnMethod[k] <= XML_STYLE1) {
                                        if (isTraceOn && tc.isDebugEnabled()) {
                                            trace(me, "Style 1 - replacing TX attribute " + tranAttrs[k] + " with " + txMOFMap[tranType]);
                                        }
                                        tranAttrs[k] = txMOFMap[tranType];
                                        highestStyleOnMethod[k] = XML_STYLE1;
                                    }
                                }

                            } else if (meType == type) { //LIDB2257.19
                                // style type 1 with <method-intf>
                                for (int k = 0; k < methodNames.length; ++k) {
                                    if (highestStyleOnMethod[k] <= XML_STYLE1_METHOD_INTF) {
                                        if (isTraceOn && tc.isDebugEnabled()) {
                                            trace(me, "Style 1 (method-intf) - replacing " + tranAttrs[k] + " with " + txMOFMap[tranType]);
                                        }
                                        tranAttrs[k] = txMOFMap[tranType];
                                        highestStyleOnMethod[k] = XML_STYLE1_METHOD_INTF;
                                    }
                                }
                            }
                        } else if (meType == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED || meType == type) { //LIDB2257.19
                            // EJB 3.2, section 8.3.7.2.1, style 2:
                            // "This style may be used to refer to stateful
                            // session bean PostConstruct, PreDestroy,
                            // PrePassivate or PostActivate methods to specify
                            // their transaction attributes if any of the
                            // following is true:
                            // * There is only one method with this name in the
                            //   specified enterprise bean
                            // * All overloaded methods with this name in the
                            //   specified enterprise bean are lifecycle
                            //   callback methods
                            // * The method-intf element is specified and it
                            //   contains LifecycleCallback as the value"
                            //
                            // We interpret the first two bullets to mean that
                            // we should only apply a transaction attribute to a
                            // lifecycle callback if there are no business
                            // methods that have the same name.
                            //
                            // The style 3 section makes no mention of stateful
                            // lifecycle callback methods.  We assume that this
                            // form should be allowed, but to be conservative
                            // in the spirit of style 2 when method-intf is not
                            // specified, we require the method not also be a
                            // business method.
                            if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION &&
                                meType == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED &&
                                type == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_LIFECYCLE_CALLBACK) {
                                if (businessMethodsByName == null) {
                                    businessMethodsByName = getBusinessMethodsByName(bmd);
                                }

                                Method businessMethod = findMatchingMethod(businessMethodsByName, me);
                                if (businessMethod != null) {
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "skipping method " + meName + "/" + me.getMethodParamList() +
                                                     " for stateful LifecycleCallback that matches " + businessMethod);
                                    }
                                    continue;
                                }
                            }

                            // It is not a wildcard, and either has an unspecified
                            // type, or is of the right type (Remote/Home/Local),
                            // so check for a method name match.             d123321
                            for (int k = 0; k < methodNames.length; ++k) {
                                if (meName.equals(methodNames[k])) {
                                    List<String> meParms = me.getMethodParamList();
                                    if (meParms == null) {
                                        // style type 2 - matching method name, no signature
                                        //                specified
                                        if (highestStyleOnMethod[k] <= XML_STYLE2) {
                                            if (isTraceOn && tc.isDebugEnabled()) {
                                                trace(me, "Style 2 - replacing " + tranAttrs[k] + " with " + txMOFMap[tranType]);
                                            }
                                            tranAttrs[k] = txMOFMap[tranType];
                                            highestStyleOnMethod[k] = XML_STYLE2;
                                        }
                                    } else {
                                        if (DDUtil.methodParamsMatch(meParms, methodParamTypes[k])) {
                                            // style type 3 - matching method name and signature
                                            if (isTraceOn && tc.isDebugEnabled()) {
                                                trace(me, "Style 3 - replacing " + tranAttrs[k] + " with " + txMOFMap[tranType]);
                                            }
                                            tranAttrs[k] = txMOFMap[tranType];
                                            highestStyleOnMethod[k] = XML_STYLE3;

                                        } // method parms match specified parms
                                    } // method parms were specified
                                } // methodExtension name matches method name
                            } // method loop (k)
                        } // if/then/else for meName='*'
                    } // enterpriseBean name matches parm in methodExtension
                } // methodElements loop (j)
            } // tranAttrList loop (i)

        } // tranAttrList != null

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getXMLCMTransactions", Arrays.toString(tranAttrs));
        }

    } // getXMLCMTransactions

    private static void trace(com.ibm.ws.javaee.dd.ejb.Method m, String message)
    {
        int methodIntfType = m.getInterfaceTypeValue();
        MethodInterface methodIntf = methodIntfType ==
                        com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED ? null : methodInterfaceTypeMap[methodIntfType - 1];
        Tr.debug(tc, message,
                 new Object[] { "ejb-name:      " + m.getEnterpriseBeanName(),
                               "method-intf:   " + methodIntf,
                               "method-name:   " + m.getMethodName(),
                               "method-params: " + m.getMethodParamList() });
    }

    private static void trace(Method m, String beanName, String message)
    {
        Tr.debug(tc, message,
                 new Object[] { "Enterprise Bean Name: " + beanName,
                               "Method name: " + m.toString() });
    }

    /**
     * getXMLPermissions fills the SecurityRoles array
     * from the XML based WCCM data.
     */
    //366845.11.1
    public static final void getXMLMethodsDenied(boolean[] denyAll,
                                                 int type,
                                                 String[] methodNames,
                                                 Class<?>[][] methodParamTypes,
                                                 ExcludeList excludeList,
                                                 BeanMetaData bmd) //PK93643
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getXMLMethodsDenied", Arrays.toString(denyAll));

        if (excludeList != null) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "*** Exclude List has " + excludeList.getMethodElements().size() + " entries to process ***");
            }

            // array to keep track of what kind of style last set the method-specific behavior
            int[] highestStyleOnMethod = new int[methodNames.length];

            // Elements of highestStyleOnMethod[] are initialized to zero by Java rules.

            // j : MethodElements loop
            // k : Method loop

            List<com.ibm.ws.javaee.dd.ejb.Method> methodElements = excludeList.getMethodElements();

            for (int j = 0; j < methodElements.size(); j++) {
                com.ibm.ws.javaee.dd.ejb.Method me = methodElements.get(j);
                String ejbNameFromMethodElement = me.getEnterpriseBeanName();
                int meType = me.getInterfaceTypeValue();

                // If ejbNameFromMethodElement is null the customer most likely has an xml
                // coding error in the method element where the ejb-name is incorrect.
                if (ejbNameFromMethodElement == null) { //PK93643
                    Tr.warning(tc, "INVALID_EXCLUDE_LIST_XML_CNTR0124W",
                               new Object[] { bmd.getJ2EEName().getModule(), bmd.enterpriseBeanName }); //PK93643
                }

                if ((ejbNameFromMethodElement != null) && (ejbNameFromMethodElement.equals(bmd.enterpriseBeanName))) { //PK93643
                    String meName = me.getMethodName().trim();
                    if (meName.equals("*")) {
                        if (meType == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED) {
                            // style type 1 -- wildcard on all bean methods
                            for (int k = 0; k < methodNames.length; ++k) {
                                if (highestStyleOnMethod[k] <= 1) {
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        trace(me, "Style 1 - assigning denyAll " + true);
                                    }
                                    // set denyAll for this method as true.
                                    denyAll[k] = true;

                                    highestStyleOnMethod[k] = 1;
                                }
                            }

                        } else if (meType == type) { //LIDB2257.19
                            // style type 2 -- wildcard on home or remote interface methods
                            for (int k = 0; k < methodNames.length; ++k) {
                                if (highestStyleOnMethod[k] <= 2) {
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        trace(me, "Style 2 - assigning denyAll " + true);
                                    }
                                    // set denyAll for this method as true.
                                    denyAll[k] = true;

                                    highestStyleOnMethod[k] = 2;
                                }
                            }
                        }
                    } else if (meType == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED || meType == type) { //LIDB2257.19
                        // It is not a wildcard, and either has an unspecified
                        // type, or is of the right type (Remote/Home/Local),
                        // so check for a method name match.             d123321
                        for (int k = 0; k < methodNames.length; ++k) {
                            if (meName.equals(methodNames[k])) {
                                List<String> meParms = me.getMethodParamList();
                                if (meParms == null) {
                                    // style type 3 - matching method name, no signature
                                    //                specified
                                    if (highestStyleOnMethod[k] <= 3) {
                                        if (isTraceOn && tc.isDebugEnabled()) {
                                            trace(me, "Style 3 - assigning denyAll " + true);
                                        }
                                        // set denyAll for this method as true.
                                        denyAll[k] = true;

                                        highestStyleOnMethod[k] = 3;
                                    }
                                } else {
                                    if (DDUtil.methodParamsMatch(meParms, methodParamTypes[k])) {
                                        // style type 4 - matching method name and signature
                                        if (isTraceOn && tc.isDebugEnabled()) {
                                            trace(me, "Style 4 - assigning denyAll " + true);
                                        }
                                        // set denyAll for this method as true.
                                        denyAll[k] = true;

                                        highestStyleOnMethod[k] = 4;

                                    } // method parms match specified parms
                                } // method parms were specified
                            } // methodExtension name matches method name
                        } // method loop (k)
                    } // if/then/else for meName='*'
                } // enterpriseBean name matches parm in methodExtension
            } // methodElements loop (j)
        } // permissionsList != null

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "getXMLMethodsDenied", Arrays.toString(denyAll));
        }

    } // getXMLMethodsDenied

    /**
     * Process all of the method-permissions from XML. <p>
     * 
     * @param securityRoles an array of ArrayLists that will be filled
     *            with the Security Roles for each method.
     * @param permitAll boolean array representing each method that
     *            will be set to true for each method that
     *            is set as "Unchecked", meaning that all
     *            security roles are permitted.
     * @param denyAll boolean array representing each method. The
     *            value will be true if the method was listed
     *            in the exclude-list XML stanza.
     * @param type Indicates the type of methods we are processing
     *            (ie. Local, Remote, Home, etc.).
     * @param methodNames Array of all method names for this component.
     * @param methodSignatures Array of all method signatures for this component.
     * @param permissionsList List of Role information from XML
     * @param enterpriseBean The EJB that is being processed.
     **/
    //366845.12.2
    @SuppressWarnings("unchecked")
    public static final void getXMLPermissions(ArrayList[] securityRoles,
                                               boolean[] permitAll,
                                               boolean[] denyAll,
                                               int type,
                                               String[] methodNames,
                                               Class<?>[][] methodParamTypes,
                                               List permissionsList,
                                               BeanMetaData bmd) //PK93643
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getXMLPermissions", Arrays.toString(securityRoles));

        if (permissionsList != null) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "*** Method Permissions list has " + permissionsList.size() + " Security Permission(s) to process ***");
            }

            // Elements of highestStyleOnMethod[] are initialized to zero by Java rules.

            // i : PermissionsList loop
            // j : MethodElements loop
            // k : Method loop

            for (int i = 0; i < permissionsList.size(); ++i) {
                MethodPermission methodPermission = (MethodPermission) permissionsList.get(i);
                boolean isUnchecked = methodPermission.isUnchecked();
                List<String> roleNames = methodPermission.getRoleNames();
                List<com.ibm.ws.javaee.dd.ejb.Method> methodElements = methodPermission.getMethodElements();

                for (int j = 0; j < methodElements.size(); ++j) {
                    com.ibm.ws.javaee.dd.ejb.Method me = methodElements.get(j);
                    String ejbNameFromMethodElement = me.getEnterpriseBeanName();
                    int meType = me.getInterfaceTypeValue();

                    // If ejbNameFromMethodElement is null the customer most likey has an xml
                    // coding error in the method element where the ejb-name is incorrect.
                    if (ejbNameFromMethodElement == null) { //PK93643
                        Tr.warning(tc, "INVALID_METHOD_PERMISSION_XML_CNTR0123W",
                                   new Object[] { bmd.getJ2EEName().getModule(), bmd.enterpriseBeanName }); //PK93643
                    }

                    if (isTraceOn && tc.isDebugEnabled()) {
                        if (j == 0) {
                            Tr.debug(tc, "#" + i + " Method Permission  has " +
                                         methodElements.size() + " Method Element(s).");
                        }
                        Tr.debug(tc, "Process element " + me.getMethodName() +
                                     " Method Element type = " + meType);

                    }

                    if ((ejbNameFromMethodElement != null) && (ejbNameFromMethodElement.equals(bmd.enterpriseBeanName))) { //PK93643
                        String meName = me.getMethodName().trim();
                        if (meName.equals("*")) {
                            if (meType == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED) {
                                // style type 1 -- wildcard on all bean methods
                                for (int k = 0; k < methodNames.length; ++k) {
                                    // If unchecked, then all roles are permitted access
                                    if (isUnchecked) {
                                        if (isTraceOn && tc.isDebugEnabled()) {
                                            trace(me, "Style 1 - Permit all roles.");
                                        }
                                        permitAll[k] = true;
                                    } else {
                                        if (isTraceOn && tc.isDebugEnabled()) {
                                            trace(me, "Style 1 - assigning roles " + roleNames);
                                        }

                                        // create ArrayList of the security roles
                                        if (securityRoles[k] == null) { //d444436
                                            securityRoles[k] = new ArrayList<String>();
                                        }
                                        securityRoles[k].addAll(roleNames);
                                    }
                                }

                            } else if (meType == type) { //LIDB2257.19
                                // style type 2 -- wildcard on home or remote interface methods
                                for (int k = 0; k < methodNames.length; ++k) {
                                    // If unchecked, then all roles are permitted access
                                    if (isUnchecked) {
                                        if (isTraceOn && tc.isDebugEnabled()) {
                                            trace(me, "Style 2 - Permit all roles.");
                                        }
                                        permitAll[k] = true;
                                    } else {
                                        if (isTraceOn && tc.isDebugEnabled()) {
                                            trace(me, "Style 2 - assigning roles " + roleNames);
                                        }

                                        // Create new ArrayList of security roles         //d444436
                                        if (securityRoles[k] == null) {
                                            securityRoles[k] = new ArrayList<String>();
                                        }
                                        securityRoles[k].addAll(roleNames);
                                    }
                                }
                            }
                        } else if (meType == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED || meType == type) { //LIDB2257.19
                            // It is not a wildcard, and either has an unspecified
                            // type, or is of the right type (Remote/Home/Local),
                            // so check for a method name match.             d123321
                            for (int k = 0; k < methodNames.length; ++k) {

                                if (meName.equals(methodNames[k])) {
                                    List<String> meParms = me.getMethodParamList();
                                    if (meParms == null) {
                                        // style type 3 - matching method name, no signature
                                        //                specified

                                        // If unchecked, then all roles are permitted access
                                        if (isUnchecked) {
                                            if (isTraceOn && tc.isDebugEnabled()) {
                                                trace(me, "Style 3 - Permit all roles.");
                                            }
                                            permitAll[k] = true;
                                        } else {
                                            if (isTraceOn && tc.isDebugEnabled()) {
                                                trace(me, "Style 3 - assigning roles " + roleNames);
                                            }

                                            // create ArrayList of security roles
                                            if (securityRoles[k] == null) { //d444436
                                                securityRoles[k] = new ArrayList<String>();
                                            }
                                            securityRoles[k].addAll(roleNames);
                                        }
                                    } else {
                                        if (DDUtil.methodParamsMatch(meParms, methodParamTypes[k])) {
                                            // style type 4 - matching method name and signature

                                            // If unchecked, then all roles are permitted access
                                            if (isUnchecked) {
                                                if (isTraceOn && tc.isDebugEnabled()) {
                                                    trace(me, "Style 4 - Permit all roles.");
                                                }
                                                permitAll[k] = true;
                                            } else {
                                                if (isTraceOn && tc.isDebugEnabled()) {
                                                    trace(me, "Style 4 - assigning roles " + roleNames);
                                                }

                                                // create security roles ArrayList
                                                if (securityRoles[k] == null) { //d444436
                                                    securityRoles[k] = new ArrayList<String>();
                                                }
                                                securityRoles[k].addAll(roleNames);
                                            }

                                        } // method parms match specified parms
                                    } // method parms were specified
                                } // methodExtension name matches method name
                            } // method loop (k)
                        } // if/then/else for meName='*'
                    } // enterpriseBean name matches parm in methodExtension
                } // methodElements loop (j)
            } // permissionsList loop (i)

        } // permissionsList != null

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "getXMLPermissions " +
                        Arrays.toString(securityRoles) +
                        " PermitAll = " +
                        Arrays.toString(permitAll));

        }

    } // getXMLPermissions

    /**
     * getActivitySessions fills the ActivitySessionAttribute array of given type
     */
    public static final void getActivitySessions(ActivitySessionAttribute[] asAttrs,
                                                 int type,
                                                 String[] methodNames,
                                                 Class<?>[][] methodParamTypes,
                                                 List<ActivitySessionMethod> asAttrList,
                                                 String ejbName,
                                                 boolean usesBeanManagedAS)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getActivitySessions", (Object[])asAttrs);

        if (!usesBeanManagedAS) { // d127328

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Bean is CMAS");

            if (asAttrList != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "asAttrList non-null");

                // array to keep track of what kind of style last set the method-specific behavior
                int[] highestStyleOnMethod = new int[methodNames.length];

                // Elements of highestStyleOnMethod[] are initialized to zero by Java rules.

                // i : asAttrList loop
                // j : MethodElements loop
                // k : Method loop

                for (int i = 0; i < asAttrList.size(); ++i) {
                    ActivitySessionMethod asMethod = asAttrList.get(i);
                    ActivitySessionAttribute asAttr = asMethod.getActivitySessionAttribute();
                    @SuppressWarnings("unchecked")
                    List<com.ibm.ws.javaee.dd.ejb.Method> methodElements = asMethod.getMethodElements();
                    for (int j = 0; j < methodElements.size(); ++j) {
                        com.ibm.ws.javaee.dd.ejb.Method me = methodElements.get(j);
                        if (isTraceOn && tc.isDebugEnabled()) {
                            trace(me, me.getInterfaceTypeValue() == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED ?
                                            "Interface type unspecified" :
                                            "Interface type: " + me.getInterfaceTypeValue());
                        }
                        // Is this method element for the bean we are processing?
                        if ((me.getEnterpriseBeanName()).equals(ejbName)) {
                            String meName = me.getMethodName().trim();
                            int meType = me.getInterfaceTypeValue();

                            if (meName.equals("*")) {
                                if (meType == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED) {
                                    // style type 1 -- wildcard on all bean methods
                                    if (isTraceOn && tc.isDebugEnabled())
                                        Tr.debug(tc, "Style type 1 - all bean methods:", asAttr);
                                    for (int k = 0; k < methodNames.length; ++k) {
                                        if (highestStyleOnMethod[k] <= 1) {
                                            asAttrs[k] = asAttr;
                                            highestStyleOnMethod[k] = 1;
                                        }
                                    }

                                } else if (meType == type) { //LIDB2257.19
                                    // style type 2 -- wildcard on home or remote interface methods
                                    if (isTraceOn && tc.isDebugEnabled())
                                        Tr.debug(tc, "Style type 2 - home/remote methods only:", asAttr);
                                    for (int k = 0; k < methodNames.length; ++k) {
                                        if (highestStyleOnMethod[k] <= 2) {
                                            asAttrs[k] = asAttr;
                                            highestStyleOnMethod[k] = 2;
                                        }
                                    }
                                }
                            } else if (meType == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED || meType == type) { //LIDB2257.19
                                // It is not a wildcard, and either has an unspecified
                                // type, or is of the right type (Remote/Home/Local),
                                // so check for a method name match.             d123321
                                for (int k = 0; k < methodNames.length; ++k) {
                                    if (meName.equals(methodNames[k])) {
                                        List<String> meParms = me.getMethodParamList();
                                        if (meParms == null) {
                                            // style type 3 - matching method name, no signature
                                            //                specified
                                            if (isTraceOn && tc.isDebugEnabled())
                                                Tr.debug(tc, "Style type 3 - method name only:", asAttr);
                                            if (highestStyleOnMethod[k] <= 3) {
                                                asAttrs[k] = asAttr;
                                                highestStyleOnMethod[k] = 3;
                                            }
                                        } else {
                                            if (DDUtil.methodParamsMatch(meParms, methodParamTypes[k])) {
                                                // style type 4 - matching method name and signature
                                                if (isTraceOn && tc.isDebugEnabled())
                                                    Tr.debug(tc, "Style type 4 - method name and signature:", asAttr);
                                                asAttrs[k] = asAttr;
                                                highestStyleOnMethod[k] = 4;
                                            } // method parms match specified parms
                                        } // method parms were specified
                                    } // methodExtension name matches method name
                                } // method loop (k)
                            } // if/then/else for meName='*'
                        } // enterpriseBean name matches parm in methodExtension
                    } // methodElements loop (j)
                } // asAttrList loop (i)
            } // asAttrList != null

        } else if (usesBeanManagedAS) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Bean is BMAS -- all methods will be set to AS_BEAN_MANAGED");
            for (int i = 0; i < asAttrs.length; i++) {
                asAttrs[i] = ActivitySessionAttribute.AS_BEAN_MANAGED;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getActivitySessions");

        return;
    }

    /**
     * For all methodNames in checkedNames, set their txAttr to prescribedAttr.
     * To set all txAttr in list, use 1-element array {"*"} for checkedNames.
     **/
    public static final void checkTxAttrs(TransactionAttribute[] txAttrs,
                                          String[] methodNames, String[] methodSignatures,//PQ63130
                                          String[] checkedNames, String[] checkedSignatures,//PQ63130

                                          TransactionAttribute prescribedAttr)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "checkTxAttrs");
        for (int i = 0; i < methodNames.length; ++i) {
            for (int j = 0; j < checkedNames.length; ++j) {

                //PQ63130 added check of method signature
                if (((methodNames[i].equals(checkedNames[j])) && (methodSignatures[i].equals(checkedSignatures[j]))) | checkedNames[j].equals("*")) {

                    txAttrs[i] = prescribedAttr;
                    break;
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "checkTxAttrs");

    }

    //d141634
    /**
     * Check BMT beans for unneeded Tran Attributes in XML (ie. WCCM)
     */
    public static final void chkBMTFromXML(List<ContainerTransaction> tranAttrList, EnterpriseBean enterpriseBean, J2EEName j2eeName)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "chkBMTFromXML");

        if (tranAttrList != null && enterpriseBean != null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "tranAttrList non-null");

            // Elements of highestStyleOnMethod[] are initialized to zero by Java rules.

            // i : TranAttrList loop
            // j : MethodElements loop

            for (int i = 0; i < tranAttrList.size(); ++i) {
                ContainerTransaction methodTransaction = tranAttrList.get(i);
                List<com.ibm.ws.javaee.dd.ejb.Method> methodElements = methodTransaction.getMethodElements();
                for (int j = 0; j < methodElements.size(); ++j) {
                    com.ibm.ws.javaee.dd.ejb.Method me = methodElements.get(j);
                    if (isTraceOn && tc.isDebugEnabled()) {
                        trace(me, me.getInterfaceTypeValue() == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED ?
                                        "Interface type unspecified" :
                                        "Interface type: " + me.getInterfaceTypeValue());
                    }
                    if (enterpriseBean.getName().equals(me.getEnterpriseBeanName())) {

                        Tr.warning(tc, "BMT_DEFINES_CMT_ATTRIBUTES_CNTR0067W", new Object[] { j2eeName });

                    } // enterpriseBean name matches parm in methodExtension
                } // methodElements loop (j)
            } // tranAttrList loop (i)

        } // tranAttrList != nullcd

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "chkBMTFromXML");

        return;
    }

    /**
     * Check BMT beans for unneeded Tran Attributes from annotations
     * 
     * @param beanMethods Array of methods within this EJB
     * @param j2eeName Name of EJB for warning message if needed
     */
    //d395828
    public static final void chkBMTFromAnnotations(Method[] beanMethods, J2EEName j2eeName)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "chkBMTFromAnnotations");

        javax.ejb.TransactionAttribute methTranAttr = null;

        for (int i = 0; i < beanMethods.length; i++)
        {
            // Unless a Remove method is added the last index in the beanMethods
            // array will be null causing an NPE.   So we need to check for
            // null first.
            if (beanMethods[i] != null) {

                methTranAttr = beanMethods[i].getAnnotation(javax.ejb.TransactionAttribute.class);
                if (methTranAttr == null)
                {
                    methTranAttr = beanMethods[i].getDeclaringClass().getAnnotation(javax.ejb.TransactionAttribute.class);
                }
                // If a Transaction attribute exists then create a warning message.
                if (methTranAttr != null)
                {
                    Tr.warning(tc, "BMT_DEFINES_CMT_ATTRIBUTES_CNTR0067W", new Object[] { j2eeName });
                }
            }

        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "chkBMTFromAnnotations");

        return;
    }

    // d141634 Check BMAS beans for unneeded CMAS attributes in XML (ie. WCCM)

    public static final void chkBMASFromXML(List<ActivitySessionMethod> asAttrList, EnterpriseBean enterpriseBean, J2EEName j2eeName)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "chkBMAS");

        //For Annotations only configuration, Enterprise Bean from WCCM may be null.  In this case there
        // is no need to check for AS attributes from xml.
        if (enterpriseBean != null) {

            if (asAttrList != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "asAttrList non-null");

                // i : asAttrList loop
                // j : MethodElements loop

                for (int i = 0; i < asAttrList.size(); ++i) {
                    ActivitySessionMethod asMethod = asAttrList.get(i);
                    @SuppressWarnings("unchecked")
                    List<com.ibm.ws.javaee.dd.ejb.Method> methodElements = asMethod.getMethodElements();
                    for (int j = 0; j < methodElements.size(); ++j) {
                        com.ibm.ws.javaee.dd.ejb.Method me = methodElements.get(j);
                        if (isTraceOn && tc.isDebugEnabled()) {
                            trace(me, me.getInterfaceTypeValue() == com.ibm.ws.javaee.dd.ejb.Method.INTERFACE_TYPE_UNSPECIFIED ?
                                            "Interface type unspecified" :
                                            "Interface type: " + me.getInterfaceTypeValue());
                        }
                        if (enterpriseBean.getName().equals(me.getEnterpriseBeanName())) {
                            Tr.warning(tc, "BMAS_DEFINES_CMAS_ATTRIBUTES_CNTR0068W", new Object[] { j2eeName });

                        } // enterpriseBean name matches parm in methodExtension
                    } // methodElements loop (j)
                } // asAttrList loop (i)
            } // asAttrList != null
        } // Enterprise Bean != null

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "chkBMAS");

        return;
    }

    // d141634

    public static final String normalizeSignature(String deplDescriptorSignature)
    {
        /*
         * Removes embedded blanks in the vicinity of array brackets, within a blank-delimited list
         * of method arguments.
         * 
         * Example input: int char [] char[ ] [ ]
         * Example output: int char[] char[][]
         * 
         * Requires: no leading or trailing blanks on input string -- use trim() first if this
         * might be the case.
         */

        StringBuffer theSignature = new StringBuffer(deplDescriptorSignature);
        int scanIndex = 0;

        while (scanIndex < theSignature.length()) {
            if (theSignature.charAt(scanIndex) == ' ') {
                char next = theSignature.charAt(scanIndex + 1);
                if (next == ' ' | next == '[' | next == ']')
                    theSignature.deleteCharAt(scanIndex);
                else
                    ++scanIndex;
            } else
                ++scanIndex;
        }

        return theSignature.toString();
    }

    public static final String METHOD_ARGLIST_SEP = ":";

    public static final String methodSignature(Method method) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "methodSignature", method.getName());
        }
        StringBuffer sb = new StringBuffer();
        sb.append(method.getName());
        sb.append(METHOD_ARGLIST_SEP);
        Class<?>[] methodParams = method.getParameterTypes();
        for (int j = 0; j < methodParams.length; j++) {
            if (j != 0)
                sb.append(",");
            if (methodParams[j].isArray()) {
                sb.append(convertArraySignature(methodParams[j].getName()));
            } else {
                sb.append(methodParams[j].getName());
            }
        }
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "methodSignature", sb.toString());
        }
        return sb.toString();
    }

    public static final String convertArraySignature(String theJavaSignature)
    {
        /*
         * This method converts a Java-internal format argument signature to a Java language signature.
         * Such conversion is necessary because of the difference in how Array argument types are
         * represented internally in the JVM vs. their natural Java language encoding (used in Java EE
         * deployment descriptors).
         * 
         * The following text is from the documentation for the Class.getName() method:
         * -----------------------------
         * If this Class object represents a class of arrays, then the internal form of the
         * name consists of the name of the element type in Java signature format, preceded
         * by one or more "[" characters representing the depth of array nesting. Thus:
         * 
         * (new Object[3]).getClass().getName()
         * 
         * returns "[Ljava.lang.Object;" and:
         * 
         * (new int[3][4][5][6][7][8][9]).getClass().getName()
         * 
         * returns "[[[[[[[I". The encoding of element type names is as follows:
         * 
         * B byte
         * C char
         * D double
         * F float
         * I int
         * J long
         * Lclassname; class or interface
         * S short
         * Z boolean
         * -----------------------------
         * 
         * Thus, if the input is "[[[B", the returned string from this method will be "byte[][][]".
         * 
         * REQUIREMENTS/GUARANTEES:
         * This method requires that there be no blanks in the input. This will be satisfied where
         * the result from Class.getName() is supplied as input (the typical case).
         * 
         * It is guaranteed that there will be no blanks in the result value.
         */

        StringBuffer sb = new StringBuffer();
        int dimensionCount = 0;
        while (theJavaSignature.charAt(dimensionCount) == '[')
            ++dimensionCount;

        switch (theJavaSignature.charAt(dimensionCount)) {
            case 'B':
                sb.append("byte[]");
                break;
            case 'C':
                sb.append("char[]");
                break;
            case 'D':
                sb.append("double[]");
                break;
            case 'F':
                sb.append("float[]");
                break;
            case 'I':
                sb.append("int[]");
                break;
            case 'J':
                sb.append("long[]");
                break;
            case 'L':
                sb.append(theJavaSignature.substring(dimensionCount + 1, theJavaSignature.length() - 1));
                sb.append("[]");
                break;
            case 'S':
                sb.append("short[]");
                break;
            case 'Z':
                sb.append("boolean[]");
                break;
        }

        while (--dimensionCount > 0)
            sb.append("[]");

        return sb.toString();
    }

    public static final String methodSignatureOnly(Method method)
    {
        // Conform to the WCCM style
        StringBuffer sb = new StringBuffer();
        Class<?>[] methodParams = method.getParameterTypes();
        for (int j = 0; j < methodParams.length; j++) {
            if (j != 0)
                sb.append(" ");
            if (methodParams[j].isArray()) {
                sb.append(convertArraySignature(methodParams[j].getName()));
            } else {
                sb.append(methodParams[j].getName());
            }
        }
        return sb.toString();
    }

    public static final String mapTypeToJDIEncoding(Class<?> type)
    {
        String returnValue;
        String typeName = type.getName();
        if (type.isArray()) {
            returnValue = typeName.replace('.', '/');
        } else {
            // check for these in rough order of frequency for best performance
            if (typeName.indexOf('.') > 0)
                returnValue = "L" + typeName.replace('.', '/') + ";";
            else if (typeName.equals("void"))
                returnValue = "V";
            else if (typeName.equals("boolean"))
                returnValue = "Z";
            else if (typeName.equals("int"))
                returnValue = "I";
            else if (typeName.equals("long"))
                returnValue = "J";
            else if (typeName.equals("double"))
                returnValue = "D";
            else if (typeName.equals("float"))
                returnValue = "F";
            else if (typeName.equals("char"))
                returnValue = "C";
            else if (typeName.equals("byte"))
                returnValue = "B";
            else if (typeName.equals("short"))
                returnValue = "S";

            else
                returnValue = "L" + typeName + ";";
        }
        return returnValue;
    }

    public static final String jdiMethodSignature(Method method)
    {
        StringBuffer sb = new StringBuffer();
        Class<?>[] methodParams = method.getParameterTypes();
        sb.append("(");
        for (int j = 0; j < methodParams.length; j++) {
            sb.append(mapTypeToJDIEncoding(methodParams[j]));
        }
        sb.append(")");
        sb.append(mapTypeToJDIEncoding(method.getReturnType()));
        return sb.toString();
    }

    /**
     * This utility method compares a method on the bean's remote
     * interface with a method on the bean and returns true iff they
     * are equal for the purpose of determining if the control
     * descriptor associated with the bean method applies to the
     * remote interface method. Equality in this case means the methods
     * are identical except for the abstract modifier on the remote
     * interface method.
     */
    public static final boolean methodsEqual(Method remoteMethod, Method beanMethod)
    {
        if ((remoteMethod == null) || (beanMethod == null)) {
            return false;
        }

        //----------------------
        // Compare method names
        //----------------------

        if (!remoteMethod.getName().equals(beanMethod.getName())) {
            return false;
        }

        //-------------------------
        // Compare parameter types
        //-------------------------

        Class<?> remoteMethodParamTypes[] = remoteMethod.getParameterTypes();
        Class<?> beanMethodParamTypes[] = beanMethod.getParameterTypes();

        if (remoteMethodParamTypes.length != beanMethodParamTypes.length) {
            return false;
        }

        for (int i = 0; i < remoteMethodParamTypes.length; i++) {
            if (!remoteMethodParamTypes[i].equals(beanMethodParamTypes[i])) {
                return false;
            }
        }

        //-----------------------------------------------------------------
        // If method names are equal and parameter types match then the
        // methods are equal for our purposes. Java does not allow methods
        // with the same name and parameter types that differ only in
        // return type and/or exception signature.
        //-----------------------------------------------------------------

        return true;
    } // methodsEqual

    /**
     * This utility method compares a method on the bean's home
     * interface with a method on the bean and returns true iff they
     * are equal for the purpose of determining if the control
     * descriptor associated with the bean method applies to the
     * remote interface method. Equality in this case means that the
     * bean method has the same name as the home method and they have
     * the same parameters.
     */
    public static final boolean homeMethodEquals(Method homeMethod,
                                                 Properties beanMethodProps)
    {
        if ((homeMethod == null) || (beanMethodProps == null)) {
            return false;
        }

        //----------------------
        // Compare method names
        //----------------------

        String homeMethodName = homeMethod.getName();
        String beanMethodName = (String) beanMethodProps.get("Name");

        if (!homeMethodName.equals(beanMethodName)) {
            return false;
        }

        //-------------------------
        // Compare parameter types
        //-------------------------

        Class<?> homeMethodParamTypes[] = homeMethod.getParameterTypes();
        String beanMethodParamTypes[] =
                        (String[]) beanMethodProps.get("ArgumentTypes");

        if (homeMethodParamTypes.length != beanMethodParamTypes.length) {
            return false;
        }

        for (int i = 0; i < homeMethodParamTypes.length; i++) {
            if (!homeMethodParamTypes[i].getName().equals(beanMethodParamTypes[i])) {
                return false;
            }
        }

        //-----------------------------------------------------------------
        // If method names are equal and parameter types match then the
        // methods are equal for our purposes. Java does not allow methods
        // with the same name and parameter types that differ only in
        // return type and/or exception signature.
        //-----------------------------------------------------------------

        return true;

    } // homeMethodEquals

    /**
     * Map transaction attribute value to string.
     */
    public static final String TX_ATTR_STR[] = {
                                                "TX_NOT_SUPPORTED", // 0
                                                "TX_BEAN_MANAGED", // 1
                                                "TX_REQUIRED", // 2
                                                "TX_SUPPORTS", // 3
                                                "TX_REQUIRES_NEW", // 4
                                                "TX_MANDATORY", // 5
                                                "TX_NEVER" // 6
    };

    /**
     * Map isolation level to string representation.
     */
    private static String[] ISOLATION_STR;

    public static final String getIsolationLevelString(int isolationLevel)
    {
        if (isolationLevel >= 0 && isolationLevel < ISOLATION_STR.length)
            return ISOLATION_STR[isolationLevel];
        return "-- ILLEGAL ISOLATION LEVEL --";
    }

    private static final void populateIsoStringMap()
    {
        ISOLATION_STR = new String[9];
        for (int i = 0; i < ISOLATION_STR.length; i++)
            ISOLATION_STR[i] = "-- ILLEGAL ISOLATION LEVEL --";
        ISOLATION_STR[java.sql.Connection.TRANSACTION_READ_UNCOMMITTED] = "TRANSACTION_READ_UNCOMMITTED";
        ISOLATION_STR[java.sql.Connection.TRANSACTION_READ_COMMITTED] = "TRANSACTION_READ_COMMITTED";
        ISOLATION_STR[java.sql.Connection.TRANSACTION_REPEATABLE_READ] = "TRANSACTION_REPEATABLE_READ";
        ISOLATION_STR[java.sql.Connection.TRANSACTION_SERIALIZABLE] = "TRANSACTION_SERIALIZABLE";
        ISOLATION_STR[java.sql.Connection.TRANSACTION_NONE] = "TRANSACTION_NONE";
    }

    private static final void populateTxMofMap()
    {
        txMOFMap = new TransactionAttribute[7];
        txMOFMap[ContainerTransaction.TRANS_ATTRIBUTE_NOT_SUPPORTED] = TransactionAttribute.TX_NOT_SUPPORTED;
        txMOFMap[ContainerTransaction.TRANS_ATTRIBUTE_SUPPORTS] = TransactionAttribute.TX_SUPPORTS;
        txMOFMap[ContainerTransaction.TRANS_ATTRIBUTE_REQUIRED] = TransactionAttribute.TX_REQUIRED;
        txMOFMap[ContainerTransaction.TRANS_ATTRIBUTE_REQUIRES_NEW] = TransactionAttribute.TX_REQUIRES_NEW;
        txMOFMap[ContainerTransaction.TRANS_ATTRIBUTE_MANDATORY] = TransactionAttribute.TX_MANDATORY;
        txMOFMap[ContainerTransaction.TRANS_ATTRIBUTE_NEVER] = TransactionAttribute.TX_NEVER;
    }

    private static TransactionAttribute txMOFMap[];

    private static final void populateTxAttrFromJEE15Map()
    {
        txAttrFromJEE15Map = new TransactionAttribute[6];
        txAttrFromJEE15Map[javax.ejb.TransactionAttributeType.NOT_SUPPORTED.ordinal()] = TransactionAttribute.TX_NOT_SUPPORTED;
        txAttrFromJEE15Map[javax.ejb.TransactionAttributeType.SUPPORTS.ordinal()] = TransactionAttribute.TX_SUPPORTS;
        txAttrFromJEE15Map[javax.ejb.TransactionAttributeType.REQUIRED.ordinal()] = TransactionAttribute.TX_REQUIRED;
        txAttrFromJEE15Map[javax.ejb.TransactionAttributeType.REQUIRES_NEW.ordinal()] = TransactionAttribute.TX_REQUIRES_NEW;
        txAttrFromJEE15Map[javax.ejb.TransactionAttributeType.MANDATORY.ordinal()] = TransactionAttribute.TX_MANDATORY;
        txAttrFromJEE15Map[javax.ejb.TransactionAttributeType.NEVER.ordinal()] = TransactionAttribute.TX_NEVER;
    }

    private static TransactionAttribute txAttrFromJEE15Map[];

    // Map values of enum (i.e. int) back to enum
    private static final MethodInterface methodInterfaceTypeMap[] = MethodInterface.getAllValues();

    /**
     * Fills a specified long array with data from the @AccessTimeout annotation for each
     * of the business methods of a specified Singleton or Stateful session bean. <p>
     * 
     * See "4.8.4.5 Specification of Concurrency Locking Attributes with Metadata Annotations"
     * for rules regarding the merge of xml and annotation data for the method
     * access timeout.
     * 
     * <dl>
     * <dt>pre-conditions
     * <dd>For each ejbMethods[i], timeouts[i] is required to be initialized
     * with values obtained from ejb-jar.xml for ejbMethods[i].
     * <dd>timeouts[i] must be set to to -2 if ejb-jar.xml does not contain
     * a access timeout value for ejbMethods[i].
     * <dt>post-conditions
     * <dd>For each ejbMethods[i] that is -2 upon entry to this method,
     * timeouts[i] is set to either the value obtained from @AccessTimeout annotation or
     * the specified default value required by EJB specification (-1 starting in EE6
     * and 0 for EE5 Compatibility).
     * </dl>
     * 
     * @param timeouts
     *            is the array of timeout values to be filled with annotation values.
     * @param ejbMethods
     *            is the array of business methods of the session bean.
     * @param defaultTimeout
     *            is the default AccessTimeout to be used if not specified.
     * @param metadataComplete
     *            is the metadata-complete setting applicable to the session bean.
     * 
     * @throws EJBConfigurationException
     * 
     * @see javax.ejb.AccessTimeout
     */
    // F743-1752.1 added entire method.
    public static void getAnnotationCMCLockAccessTimeout(long[] timeouts,
                                                         Method[] ejbMethods,
                                                         long defaultTimeout,
                                                         boolean metadataComplete)
                    throws EJBConfigurationException // F743-1752CodRev
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getAnnotationCMCLockAccessTimeout: "
                         + " methods = " + Arrays.toString(ejbMethods)); //F743-7027.1
        }

        // For each of the business methods, determine the access timeout
        // value to use if not already set by the ejb-jar.xml processing.
        for (int i = 0; i < ejbMethods.length; i++)
        {
            Method beanMethod = ejbMethods[i];

            // Only use the value from annotations if it is not already set
            // from value in ejb-jar.xml (e.g. obtained via WCCM).
            if (timeouts[i] == -2)
            {
                // ejb-jar.xml did not contain the lock type for this method.
                // Only change if metadata complete is false.
                // EJB 2.1 remove method (null) has no annotation.        F743-22642
                if (metadataComplete == false && beanMethod != null)
                {
                    // metadata complete is false, so see if there is a @AccessTimeout
                    // annotation on this method.
                    AccessTimeout annotation = beanMethod.getAnnotation(AccessTimeout.class);
                    if (annotation == null)
                    {
                        // No @AccessTimeout annotation on method, see if there is one at class level of
                        // the class that declared this method object.
                        Class<?> c = beanMethod.getDeclaringClass();
                        annotation = c.getAnnotation(AccessTimeout.class);

                        if (isTraceOn && tc.isDebugEnabled() && annotation != null)
                        {
                            Tr.debug(tc, beanMethod.getName() + " from class " + c.getName());
                        }
                    }

                    // Did we find a @AccessTimeout annotation at method or class level?
                    if (annotation != null)
                    {
                        // F743-1752CodRev start
                        // Use value from @AccessTimeout annotation provided it is
                        // a valid value (must be -1 or greater).
                        long value = annotation.value(); // F743-7027
                        TimeUnit unit = annotation.unit(); // F743-7027

                        if (value < -1 || value == Long.MAX_VALUE) // F743-7027
                        {
                            // CNTR0192E: The access timeout value {0} is not valid for the enterprise
                            // bean {1} method of the {2} class. The value must be -1 or greater and
                            // less than java.lang.Long.MAX_VALUE (9223372036854775807).
                            Tr.error(tc, "SINGLETON_INVALID_ACCESS_TIMEOUT_CNTR0192E"
                                     , new Object[] { value, beanMethod.getName(), beanMethod.getDeclaringClass().getName() });

                            throw new EJBConfigurationException("CNTR0192E: @AccessTimeout annotation value " + value +
                                                                " is not valid for the enterprise bean " + beanMethod.getName() +
                                                                " method of the " + beanMethod.getDeclaringClass().getName() +
                                                                " class. The value must be -1 or greater and less than" +
                                                                " java.lang.Long.MAX_VALUE (9223372036854775807).");
                        }
                        else if (value > 0)
                        {
                            // Valid value, convert to milli-seconds if necessary.
                            if (unit == TimeUnit.MILLISECONDS) // F743-7027
                            {
                                timeouts[i] = value;
                            }
                            else
                            {
                                timeouts[i] = TimeUnit.MILLISECONDS.convert(value, unit); // F743-6605.1
                                // Throw exception if conversion resulted in an overflow. The assumption
                                // is pre-conditions are honored by caller of this method.
                                // begin F743-6605.1
                                if (timeouts[i] == Long.MAX_VALUE || timeouts[i] == Long.MIN_VALUE) {
                                    // CNTR0196E: The conversion of access timeout value {0} from {1} time
                                    // unit to
                                    // milli-seconds time unit resulted in an overflow.
                                    Tr.error(tc, "SINGLETON_ACCESS_TIMEOUT_OVERFLOW_CNTR0196E", new Object[] { value, unit });

                                    if (isTraceOn && tc.isEntryEnabled()) {
                                        Tr.exit(tc, "convertToMilliSeconds: " + value + unit + " overflow");
                                    }

                                    throw new EJBConfigurationException("Conversion of access timeout value of " + value + " " + unit
                                                                        + " to milliseconds resulted in overflow.");
                                }
                                // end F743-6605.1
                            }
                        } // F743-1752CodRev end
                        else
                        {
                            timeouts[i] = value; // special value -1 or 0  F743-21028.5
                        }
                    }
                    else
                    {
                        timeouts[i] = defaultTimeout; // set default  F743-21028.5 F743-22462
                    }
                } // if !metadataComplete
                else
                {
                    timeouts[i] = defaultTimeout; // set default     F743-21028.5 F743-22462
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "getAnnotationCMCLockAccessTimeout", Arrays.toString(timeouts));
        }
    }

    /**
     * Fills a specified LockType array with data from the @Lock annotation for each
     * of the business methods of a specified Singleton session bean.
     * See "4.8.4.5 Specification of Concurrency Locking Attributes with Metadata Annotations"
     * for rules regarding the merge of xml and annotation data for the method lock type.
     * 
     * <dl>
     * <dt>pre-conditions
     * <dd>For each ejbMethods[i], lockType[i] is required to be initialized
     * with values obtained from ejb-jar.xml for ejbMethods[i].
     * <dd>lockType[i] must be set to to null if ejb-jar.xml does not contain
     * a LockType for ejbMethods[i].
     * <dt>post-conditions
     * <dd>For each ejbMethods[i] that is null on entry to this method,
     * lockType[i] is set to either the value obtained from @Lock annotation or
     * the default value required by EJB specification (LockType.WRITE).
     * </dl>
     * 
     * @param lockType
     *            is the array of LockType objects to be filled with annotation values. *
     * @param ejbMethods
     *            is the array of business methods of the Singleton session bean.
     * @param bmd
     *            is the BeanMetaData for the Singleton session bean.
     * 
     * @see javax.ejb.Lock
     * @see javax.ejb.LockType
     */
    // F743-1752.1 added entire method.
    public static void getAnnotationCMCLockType(LockType[] lockType, Method[] ejbMethods, BeanMetaData bmd)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.entry(tc, "getAnnotationCMCLockType: "
                         + " methods = " + Arrays.toString(ejbMethods)); //F743-7027.1
        }

        LockType methodLockType = null;
        boolean metadataComplete = bmd.metadataComplete;

        // For each of the business methods, determine the lock type
        // value to use if not already set by the ejb-jar.xml processing.
        for (int i = 0; i < ejbMethods.length; i++)
        {
            Method beanMethod = ejbMethods[i];

            // Only use the value from annotations if it is not already set
            // from value in ejb-jar.xml (e.g. obtained via WCCM).
            methodLockType = lockType[i];
            if (methodLockType == null)
            {
                // ejb-jar.xml did not contain the lock type for this method.
                // Metadata complete in XML?
                if (metadataComplete)
                {
                    // metadata complete is true in ejb-jar.xml file, so use the default value for LockType.
                    methodLockType = LockType.WRITE;
                }
                else
                {
                    // metadata complete is false, so see if there is a @Lock annotation on this method.
                    Lock annotation = beanMethod.getAnnotation(Lock.class);
                    if (annotation == null)
                    {
                        // No @Lock annotation on method, see if there is one at class level of
                        // the class that declared this method object.
                        Class<?> c = beanMethod.getDeclaringClass();
                        annotation = c.getAnnotation(Lock.class);

                        if (isTraceOn && tc.isDebugEnabled() && annotation != null)
                        {
                            Tr.debug(tc, beanMethod.getName() + " from class " + c.getName());
                        }
                    }

                    // Did we find a @Lock annotation at method or class level?
                    if (annotation == null)
                    {
                        // no @Lock annotation found, so use default lock type.
                        methodLockType = LockType.WRITE;
                    }
                    else
                    {
                        // there is a @Lock annotation, so use it.
                        methodLockType = annotation.value();
                    }

                } // end if ( metadataComplete )

                // Set the lock type for this method as determined by above logic.
                lockType[i] = methodLockType; //F743-1752CodRev

                if (isTraceOn && tc.isDebugEnabled())
                {
                    Tr.debug(tc, beanMethod.getName() + " = " + methodLockType);
                }

            } // end if ( methodLockType == null )
            else
            {
                // The ejb-jar.xml did specify a lock type for this method. Check
                // to see if overriding an explicit lock ty0e specified by the
                // Bean Provider via annotation.  EJB spec that DD should not
                // override an explicitly provided value by Bean Provider, but
                // we can not tell whether DD is from Bean Provider vs application
                // assembler.  Even if application assembler, it could be Bean Provider
                // is fixing an application error via the DD rather than changing the
                // annotations. So we will doing nothing more that trace the fact
                // that it occured.
                boolean traceEnabled = isTraceOn && (tcDebug.isDebugEnabled() || tc.isDebugEnabled());
                if (traceEnabled)
                {
                    // Is there an annotation for this method to use?
                    Lock annotation = beanMethod.getAnnotation(Lock.class);
                    if (annotation == null)
                    {
                        // No, is one specified at declaring class level for this method?
                        Class<?> c = beanMethod.getDeclaringClass();
                        annotation = c.getAnnotation(Lock.class);
                    }

                    // If an annotation for lock type was specified and it was overridden by DD to
                    // be a different value, then we want to trace that it was changed since it could
                    // potentially be causing an application failure.  It could also be fixing an
                    // application error, which is why we only trace the occurance.
                    if (annotation != null && (annotation.value().equals(methodLockType) == false))
                    {
                        // Change in lock type, build a warning message and trace it.
                        String msg;
                        if (methodLockType == LockType.WRITE)
                        {
                            msg = "ejb-jar.xml is overriding a @Lock(READ) with a write lock. This may cause a deadlock to occur.";
                        }
                        else
                        {
                            msg = "ejb-jar.xml is overriding a @Lock(WRITE) with a read lock. This may cause data integrity problems.";
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("warning, for the ").append(beanMethod.toString()).append(" method, the ").append(msg);

                        if (tcDebug.isDebugEnabled())
                        {
                            Tr.debug(tcDebug, sb.toString());
                        }
                        else
                        {
                            Tr.debug(tc, sb.toString());
                        }
                    }
                }
            }
        } // end for

        if (isTraceOn && tc.isEntryEnabled())
        {
            Tr.exit(tc, "getAnnotationCMCLockType", Arrays.toString(lockType));
        }
    }

    /**
     * The deployment descriptor may contain something like following for a
     * singleton session bean:
     * <p>
     * <session>
     * <ejb-name>NMTOKEN</ejb-name>
     * <session-type>Singleton</session-type>
     * <concurrency-management-type>Container</concurrency-management-type>
     * <concurrent-method>
     * <method><br>
     * <method-name>doSomething</method-name>
     * <method-params>
     * <method-param>java.lang.Interger</method-param>
     * </method-params>
     * </method>
     * <lock>Read</lock>
     * <access-timeout>
     * <timeout>2</timeout>
     * <unit>Seconds</unit>
     * </access-timeout>
     * </concurrent-method>
     * </session>
     * <p>
     * Since the <concurrent-method> can occur zero to n times, this method fills a specified
     * LockType array with data from each <lock> stanza that occurs for a specified method
     * of a specified singleton session bean.
     * 
     * <dl>
     * <dt>pre-conditions
     * <dd>For each ejbMethods[i], lockType[i] is required to be initialized to null.
     * <dt>post-conditions
     * <dd>For each ejbMethods[i], lockType[i] is set to either the value obtained
     * from the <lock> stanza in DD or to null if DD does not contain a value for this method.
     * </dl>
     * 
     * @param lockType
     *            is the array of LockType objects to be filled with annotation values.
     * 
     * @param ejbMethods
     *            is the array of business methods of the Singleton session bean.
     * 
     * @param sessionBean
     *            is the non null reference to the WCCM Session object for the singleton
     *            session bean. Do not call this method with a null reference since there
     *            is no WCCM data to process when null.
     * 
     * @see javax.ejb.Lock
     * @see javax.ejb.LockType
     */
    // F743-7027 added entire method.
    public static void getXMLCMCLockType(LockType[] lockType, Method[] ejbMethods, Session sessionBean)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getXMLCMCLockType: " + sessionBean.getEjbClassName()
                         + " methods = " + Arrays.toString(ejbMethods)); //F743-7027.1

        // Create an array of ints that will hold the highest priority
        // style of <lock> stanza configured for each method.  The priority
        // from lowest to highest is as follows:
        //
        //  1 - style one is the case where method name is "*" which indicates
        //      a lock type value configured for all methods of the singleton SB.
        //  2 - style two is the case where method name is specified, but not the
        //      method parameters. This must override any style 1 configuration.
        //  3 - style three is the case where both method name and parameters are specified.
        //      This style is required by spec to override any style 1 or 2 configured for the bean.

        int numberOfEjbMethods = ejbMethods.length; //F743-7027CodRev
        int[] highestStyleOnMethod = new int[numberOfEjbMethods]; //F743-7027CodRev

        // Get the list of WCCM ConcurrentMethod objects configured for this
        // singleton session bean and its enterprise bean name. There is one ConcurrentMethod
        // object created for each <lock> stanza that exists in DD.

        String enterpriseBeanName = sessionBean.getName();
        //F00743.9717
        List<ConcurrentMethod> cmcMethodList = sessionBean.getConcurrentMethods();

        // For each of WCCM ConcurrentMethod objects, determine which EJB methods this
        // ConcurrentMethod object applies to and set the lock type in the lockType array
        // passed to this method by the caller.
        int numberOfCmcMethods = cmcMethodList.size(); //F743-7027CodRev
        for (int i = 0; i < numberOfCmcMethods; i++) //F743-7027CodRev
        {
            // Get the WCCM ConcurrentLockType, NamedMethod, and MethodParams objects
            // from the WCCM ConcurrentMethod object being processed in this iteration.
            //F00743.9717
            ConcurrentMethod cmcMethod = cmcMethodList.get(i);
            int cmcLockType = cmcMethod.getLockTypeValue();

            // Process lock type if DD provided one for this ConcurrentMethod object.
            if (cmcLockType != ConcurrentMethod.LOCK_TYPE_UNSPECIFIED)
            {
                NamedMethod namedMethod = cmcMethod.getMethod();
                List<String> parms = namedMethod.getMethodParamList();

                // Map the ConcurrentLockType object to the corresponding javax.ejb.LockType value.
                // Assumption here is WCCM parser error occurs and application does not install
                // if invalid value was coded (something other than Read or Write).
                LockType methodLockType = null;
                //F00743.9717
                if (cmcLockType == ConcurrentMethod.LOCK_TYPE_READ)
                {
                    methodLockType = LockType.READ;
                }
                else if (cmcLockType == ConcurrentMethod.LOCK_TYPE_WRITE)
                {
                    methodLockType = LockType.WRITE;
                }

                // Now determine whether ConcurrentMethod is a style 1, 2, or 3 configuration
                // and update lockType array with method lock type as appropriate.
                String cmcMethodName = namedMethod.getMethodName().trim();
                if (isTraceOn && tc.isDebugEnabled()) //F743-7027.1
                {
                    Tr.debug(tc, cmcMethodName + " LockType = " + methodLockType); //F743-7027.1
                }
                if (cmcMethodName.equals("*"))
                {
                    // style type 1 -- update all entries in lockType array that was not
                    // previously set by either a style 2 or 3 configuration.
                    // The EJB spec is silent on whether more than one style 1 can be specified
                    // for the same singleton session bean.  Our assumption is the last style 1 is used.
                    for (int j = 0; j < numberOfEjbMethods; ++j) //F743-7027CodRev
                    {
                        Method m = ejbMethods[j];
                        if (highestStyleOnMethod[j] <= 1)
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                trace(m, enterpriseBeanName,
                                      "Style 1 - replacing " + lockType[j] + " lock type with " + methodLockType);

                            lockType[j] = methodLockType;
                            highestStyleOnMethod[j] = 1;
                        }
                    }
                }
                else if (parms == null)
                {
                    // style type 2 -- update all entries in lockType array for a
                    // method with matching method name that was not previously
                    // set by a style 3 configuration.
                    // The EJB spec is silent on whether more than one style 2 can be specified
                    // for the same method.  Our assumption is the last style 2 is used.
                    for (int j = 0; j < numberOfEjbMethods; ++j) //F743-7027CodRev
                    {
                        if (highestStyleOnMethod[j] <= 2)
                        {
                            Method m = ejbMethods[j];
                            String methodName = m.getName(); //F743-7027.1
                            if (cmcMethodName.equals(methodName)) //F743-7027.1
                            {
                                if (isTraceOn && tc.isDebugEnabled())
                                    trace(m, enterpriseBeanName,
                                          "Style 2 - replacing " + lockType[j] + " lock type with " + methodLockType);

                                lockType[j] = methodLockType;
                                highestStyleOnMethod[j] = 2;
                            }
                        }
                    }
                }
                else
                {
                    // style type 3 -- update only the lockType array entry with a
                    // matching method signature.
                    // The EJB spec is silent on whether more than one style 3 can be specified
                    // for the same method signature.  Our assumption is the last style 3 is used.
                    Method style3Method = DDUtil.findMethod(namedMethod, ejbMethods);
                    if (style3Method != null) //F743-7027.1
                    {
                        for (int j = 0; j < numberOfEjbMethods; ++j) //F743-7027CodRev
                        {
                            Method m = ejbMethods[j];
                            if (style3Method.equals(m))
                            {
                                if (isTraceOn && tc.isDebugEnabled())
                                    trace(m, enterpriseBeanName,
                                          "Style 3 - replacing " + lockType[j] + " lock type with " + methodLockType);

                                lockType[j] = methodLockType;
                                highestStyleOnMethod[j] = 3;

                                // break out of for loop since there can only be 1 EJB method that
                                // matches the signature.
                                break;
                            }
                        }
                    }
                }
            }
        } // end for

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getXMLCMCLockType: " + sessionBean.getEjbClassName(), Arrays.toString(lockType)); //F743-7027.1
    }

    /**
     * The deployment descriptor may contain something like following for a
     * singleton session bean:
     * <p>
     * <session>
     * <ejb-name>NMTOKEN</ejb-name>
     * <session-type>Singleton</session-type>
     * <concurrency-management-type>Container</concurrency-management-type>
     * <concurrent-method>
     * <method><br>
     * <method-name>doSomething</method-name>
     * <method-params>
     * <method-param>java.lang.Interger</method-param>
     * </method-params>
     * </method>
     * <lock>Read</lock>
     * <access-timeout>
     * <timeout>2</timeout>
     * <unit>Seconds</unit>
     * </access-timeout>
     * </concurrent-method>
     * </session>
     * <p>
     * Since the <concurrent-method> can occur zero to n times, this method fills a specified
     * long array with data from each <access-timeout> stanza that occurs for a specified method
     * of a specified singleton session bean.
     * 
     * <dl>
     * <dt>pre-conditions
     * <dd>For each ejbMethods[i], accessTimeouts[i] is required to be initialized to -2.
     * <dt>post-conditions
     * <dd>For each ejbMethods[i], accessTimeouts[i] is set to either the value obtained
     * from the <access-timeout> stanza in DD or to -2 if DD does not contain a value for this method.
     * </dl>
     * 
     * @param accessTimeouts
     *            is the array of long values to be filled access timeout values in milli-seconds.
     * 
     * @param ejbMethods
     *            is the array of business methods of the Singleton session bean.
     * 
     * @param sessionBean
     *            is the non null reference to the WCCM Session object for the singleton
     *            session bean. Do not call this method with a null reference since there
     *            is no WCCM data to process when null.
     * 
     * @throws EJBConfigurationException if the DD contains a <access-timeout> value that is invalid
     *             or results in overflow when conversion from the specified unit to milli-seconds occurs.
     * 
     * @see java.util.concurrent.TimeUnit
     */
    // F743-7027 added entire method.
    public static void getXMLCMCLockAccessTimeout(long[] accessTimeouts, Method[] ejbMethods, Session sessionBean)
                    throws EJBConfigurationException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getXMLCMCLockAccessTimeout: " + sessionBean.getEjbClassName()
                         + " methods = " + Arrays.toString(ejbMethods)); //F743-7027.1

        // Create an array of ints that will hold the highest priority
        // style of <access-timeout> stanza configured for each method.  The priority
        // from lowest to highest is as follows:
        //
        //  1 - style one is the case where method name is "*" which indicates
        //      a access timeout value configured for all methods of the singleton SB.
        //  2 - style two is the case where method name is specified, but not the
        //      method parameters. This must override any style 1 configuration.
        //  3 - style three is the case where both method name and parameters are specified.
        //      This style is required by spec to override any style 1 or 2 configured for the bean.

        int numberOfEjbMethods = ejbMethods.length; //F743-7027CodRev
        int[] highestStyleOnMethod = new int[numberOfEjbMethods]; //F743-7027CodRev

        // Get the list of WCCM ConcurrentMethod objects configured for this
        // singleton session bean and its enterprise bean name. There is one ConcurrentMethod
        // object created for each <lock> stanza that exists in DD.
        String enterpriseBeanName = sessionBean.getName();

        //F00743.9717
        List<ConcurrentMethod> cmcMethodList = sessionBean.getConcurrentMethods();

        // For each of WCCM ConcurrentMethod objects, determine which EJB methods this
        // ConcurrentMethod object applies to and set the access timeout in the array
        // passed to this method by the caller.
        int numberOfCmcMethods = cmcMethodList.size(); //F743-7027CodRev
        for (int i = 0; i < numberOfCmcMethods; i++) //F743-7027CodRev
        {
            // Get the WCCM AccessTimeout, NamedMethod, and MethodParams objects
            // from the WCCM ConcurrentMethod object being processed in this iteration.
            //F00743.9717
            ConcurrentMethod cmcMethod = cmcMethodList.get(i);
            com.ibm.ws.javaee.dd.ejb.AccessTimeout accessTimeout = cmcMethod.getAccessTimeout();
            NamedMethod namedMethod = cmcMethod.getMethod();
            List<String> parms = namedMethod.getMethodParamList();

            // If access timeout configured via xml, then process it.
            if (accessTimeout != null)
            {
                long timeout;
                long value = accessTimeout.getTimeout();
                String cmcMethodName = namedMethod.getMethodName().trim();
                if (isTraceOn && tc.isDebugEnabled()) //F743-7027.1
                    Tr.debug(tc, cmcMethodName + " AccessTimeout value = " + value); //F743-7027.1

                // Validate configured value and process it.
                if (value < -1 || value == Long.MAX_VALUE)
                {
                    // CNTR0192E: The access timeout value {0} is not valid for the enterprise
                    // bean {1} method of the {2} class. The value must be -1 or greater and
                    // less than java.lang.Long.MAX_VALUE (9223372036854775807).
                    String className = sessionBean.getEjbClassName();
                    Tr.error(tc, "SINGLETON_INVALID_ACCESS_TIMEOUT_CNTR0192E"
                             , new Object[] { value, cmcMethodName, className });

                    throw new EJBConfigurationException("CNTR0192E: The access timeout value " + value +
                                                        " is not valid for the enterprise bean " + cmcMethodName +
                                                        " method of the " + className +
                                                        " class. The value must be -1 or greater and less" +
                                                        " than java.lang.Long.MAX_VALUE (9223372036854775807).");
                }
                else if (value > 0)
                {
                    //F00743.9717
                    // Map the access timeout unit into a milli-second unit.
                    TimeUnit tu = accessTimeout.getUnitValue();
                    timeout = TimeUnit.MILLISECONDS.convert(value, tu); // F743-6605.1

                    // begin F743-6605.1
                    if (timeout == Long.MAX_VALUE || timeout == Long.MIN_VALUE) {
                        // CNTR0196E: The conversion of access timeout value {0} from {1} time
                        // unit to milliseconds time unit resulted in an overflow.
                        Tr.error(tc, "SINGLETON_ACCESS_TIMEOUT_OVERFLOW_CNTR0196E", new Object[] { value, tu });

                        if (isTraceOn && tc.isEntryEnabled()) {
                            Tr.exit(tc, "convertToMilliSeconds: " + value + tu + " overflow");
                        }

                        throw new EJBConfigurationException("Conversion of access timeout value of " + value + " " + tu
                                                            + " to milliseconds resulted in overflow.");
                    }
                    // end F743-6605.1
                }
                else
                {
                    timeout = value; // special value -1 or 0        F743-21028.5
                }

                // Determine if ConcurrentMethod is a style 1, 2, or 3 configuration and update
                // access timeout array with the method access timeout value specified.
                // The EJB spec is silent on whether more than one style 1 can be specified
                // for the singleton session bean.  Our assumption is the last style 1 is used.
                if (cmcMethodName.equals("*"))
                {
                    // style type 1 -- update all entries in access timeout array that was not
                    // previously set by either a style 2 or 3 configuration.
                    for (int j = 0; j < numberOfEjbMethods; ++j) //F743-7027CodRev
                    {
                        Method m = ejbMethods[j];
                        if (highestStyleOnMethod[j] <= 1)
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                trace(m, enterpriseBeanName,
                                      "Style 1 - replacing access timeout value of " + accessTimeouts[j] + " with " + timeout);

                            accessTimeouts[j] = timeout;
                            highestStyleOnMethod[j] = 1;
                        }
                    }
                }
                else if (parms == null)
                {
                    // style type 2 -- update all entries in access timeout array for a method
                    // with matching method name that was not previously set by a style 3 configuration.
                    // The EJB spec is silent on whether more than one style 2 can be specified
                    // for the same method name.  Our assumption is the last style 2 is used.
                    for (int j = 0; j < numberOfEjbMethods; ++j) //F743-7027CodRev
                    {
                        if (highestStyleOnMethod[j] <= 2)
                        {
                            Method m = ejbMethods[j];
                            String methodName = m.getName(); //F743-7027.1
                            if (cmcMethodName.equals(methodName)) //F743-7027.1
                            {
                                if (isTraceOn && tc.isDebugEnabled())
                                    trace(m, enterpriseBeanName,
                                          "Style 2 - replacing access timeout value of " + accessTimeouts[j] + " with " + timeout);

                                accessTimeouts[j] = timeout;
                                highestStyleOnMethod[j] = 2;
                            }
                        }
                    }
                }
                else
                {
                    // style type 3 -- update only the access timeout array entry with a
                    // matching method signature. style 3 always overrides style 1 and 2.
                    // The EJB spec is silent on whether more than one style 3 can be specified
                    // for the same method.  Our assumption is the last style 3 is used.
                    Method style3Method = DDUtil.findMethod(namedMethod, ejbMethods);
                    if (style3Method != null) //F743-7027.1
                    {
                        for (int j = 0; j < numberOfEjbMethods; ++j) //F743-7027CodRev
                        {
                            Method m = ejbMethods[j];
                            if (style3Method.equals(m))
                            {
                                if (isTraceOn && tc.isDebugEnabled())
                                    trace(m, enterpriseBeanName,
                                          "Style 3 - replacing access timeout value of " + accessTimeouts[j] + " with " + timeout);

                                accessTimeouts[j] = timeout;
                                highestStyleOnMethod[j] = 3;

                                // break out of for loop since there can only be 1 EJB method that
                                // matches the signature.
                                break;
                            }
                        }
                    }
                }

            } // end if ( accessTimeout != null )
        } // end for

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getXMLCMCLockAccessTimeout: " + sessionBean.getEjbClassName(), Arrays.toString(accessTimeouts));
    }

    /**
     * Converts a specified javax.util.concurrent.TimeUnit into a value that is in
     * javax.util.concurrent.TimeUnit.MILLISECONDS time units.
     * 
     * <dl>
     * <dt>pre-conditions
     * <dd>
     * unit parameter is something other than a javax.util.concurrent.TimeUnit.MILLISECONDS time unit.
     * <dd>
     * 0 < value parameter < Long.MAX_VALUE.
     * </dl>
     * 
     * @param value is the value to be converted. Since -1 has special meaning (infinite),
     *            return without converting.
     * 
     * @param unit is the javax.util.concurrent.TimeUnit that value parameter currently is.
     * 
     * @param annotation is a flag indicating whether to issue the annotation-specific or the XML-specific msg
     * 
     * @param bmd is the BeanMetaData, used for the message, if issued.
     * 
     * @return
     *         timeout value in javax.util.concurrent.TimeUnit.MILLISECONDS units.
     * 
     * @throws EJBConfigurationException is thrown if overflow occurs during conversion.
     */
    // F743-7027 added entire method.
    // F743-6605.1 streamlined method.
    public static long timeUnitToMillis(long value,
                                        TimeUnit unit,
                                        boolean annotation,
                                        BeanMetaData bmd) throws EJBConfigurationException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "timeUnitToMillis: " + value + unit);
        }

        // Initialize to input value in case current unit is already in
        // javax.util.concurrent.TimeUnit.MILLISECONDS
        long timeout = value;

        if (timeout > 0) {

            timeout = TimeUnit.MILLISECONDS.convert(value, unit);
            if (timeout == Long.MAX_VALUE || timeout == Long.MIN_VALUE) {
                Tr.error(tc, "STATEFUL_TIMEOUT_OVERFLOW_CNTR0309E",
                         new Object[] { bmd.getName(), bmd.getModuleMetaData().getName(),
                                       bmd.getModuleMetaData().getApplicationMetaData().getName(),
                                       value, unit });
                throw new EJBConfigurationException("Conversion of stateful session timeout value of " +
                                                    value + " " + unit + " to milliseconds resulted in overflow.");
            }

        }
        else {
            if (timeout == 0) {
                // 0 means no wait, represented internally as the nominal 1 millisecond
                timeout = 1;
            }
            else if (timeout == -1) {
                // -1 indicates wait forever, represented internally as 0
                timeout = 0L;
            }
            else {

                // invalid (negative) timeout value
                Object[] parms = new Object[] { bmd.getName(), bmd.getModuleMetaData().getName(), bmd.getModuleMetaData().getApplicationMetaData().getName(), timeout };
                if (annotation) {
                    Tr.error(tc, "NEGATIVE_STATEFUL_TIMEOUT_ANN_CNTR0311E", parms);
                }
                else {
                    Tr.error(tc, "NEGATIVE_STATEFUL_TIMEOUT_XML_CNTR0312E", parms);
                }

                throw new EJBConfigurationException("Stateful session timeout value of " +
                                                    value + " " + unit + " is negative.");

            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "timeUnitToMillis: " + timeout);
        }

        return timeout;
    }

} // MethodAttribUtils
