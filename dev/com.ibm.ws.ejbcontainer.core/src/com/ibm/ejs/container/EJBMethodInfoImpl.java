/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ejb.LockType;

import com.ibm.ejs.container.interceptors.InterceptorProxy;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.websphere.csi.ActivitySessionAttribute;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.MethodInterface;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBTransactionAttribute;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;

/**
 * Provides description of method to preinvoke/postinvoke calls on
 * collaborators. <p>
 */

public class EJBMethodInfoImpl
                extends MetaDataImpl
                implements EJBMethodMetaData //139562-5.EJBC
{
    private static final TraceComponent tc = Tr.register(EJBMethodInfoImpl.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container"); //d154596

    private String methodSignature; //92697
    private String jdiMethodSignature = "- NO JDI SIGNATURE AVAILABLE - "; // 135893
    private TransactionAttribute txAttr;
    private ActivitySessionAttribute asAttr = ActivitySessionAttribute.AS_UNKNOWN;
    protected MethodInterface ivInterface; // d162441 LI2281.07
    private String methodName;
    private boolean isHome;
    private boolean isStatefulSessionBean;
    protected BeanMetaData bmd; // d171555.4
    boolean isStatelessSessionBean;
    boolean isSingletonSessionBean; // F743-508
    boolean isHomeCreate;
    boolean isComponentRemove; // d647928.2
    protected int isolationAttr;//d140003.20
    boolean readOnlyAttr;

    // d112604.1
    private boolean isaCMP11CustomFinderWithForUpdateAI = false;
    private boolean isaCMP11FBPK = false;

    private boolean isLocal; // LIDB1587.33
    protected boolean setClassLoader; // LI2281.07
    public boolean isCMRSetMethod = false; // d184523
    protected boolean isLightweight = false; // LI3795-56
    public boolean isLightweightTxCapable = false; // LI3795-56
    private String ivJPATaskName = null; // d515803

    /**
     * AroundInvoke interceptors if this is a business method, AroundTimeout
     * interceptors if this is a timeout method, or null if there are no
     * interceptors for this method.
     * 
     * <p>The array is ordered so that iterating over it from index 0 to the
     * last element results in the interceptor methods being invoked in the
     * order defined in the interceptors specification. The required order is:
     * default interceptor methods, class level interceptor methods, method level
     * interceptor methods, and interceptor methods within the bean class itself
     * is last. Each of these classes can have super classes that define interceptor
     * methods as well. In that case, the method in the most general superclass must
     * be called prior to its subclass method.
     */
    InterceptorProxy[] ivAroundInterceptors = null; // d367572.1, F743-17763.1

    /**
     * The Method object from java reflection of the public method in the
     * EJB class itself, not the interface method. Will be null for
     * home methods and internal methods.
     */
    Method ivMethod = null; // d367572.2

    /**
     * The number of parms required by the Method.
     * 
     * This is only set when the Method is the recipient of a timer callback.
     * 
     * This is used by the TimedObjectWrapper to determine if 0 or 1 parms
     * should be used when invoking the Method.
     */
    int ivNumberOfMethodParms; //F743-15870

    /**
     * The Method object that is the 'bridge' method for the target method
     * (i.e. ivMethod) of this EJBMethodInfo. Bridge methods occur when an
     * interface uses generics. The bridge method is also from the EJB class
     * itself, not the interface, but will have the same signature as the
     * interface method.
     **/
    // d517824
    Method ivBridgeMethod = null;

    /**
     * Set to true if and only if this is a SFSB business method
     * that is annotated to be a "remove" method.
     */
    boolean ivSFSBRemove = false; // d384182

    /**
     * Set to true if and only if SFSB should be retained if the
     * SFSB "remove" business method throws an application exception.
     */
    boolean ivRetainIfException = false; // d384182

    /**
     * Value from AccessTimeout metadata when the method is a method
     * of a EJB 3.1 Singleton or Stateful bean. A value of -1 is used
     * if none is specified.
     */
    long ivAccessTimeout = -1; // d565527

    /**
     * Value from Lock metadata when the method is a method of a
     * EJB 3.1 Singleton.
     */
    LockType ivLockType; // d565527

    /**
     * Flag to indicate if this method is asynchronous. Default is false.
     */
    boolean ivAsynchMethod = false;

    /**
     * Exception classes obtained from the throws clause of the methods on the
     * business interfaces. The first index of the array is the business
     * interface index, which results in the list of exceptions declared for
     * this method on that business interface. This field is only non-null
     * for asynchronous methods.
     */
    // F743-761, F743-24429
    Class<?>[][] ivDeclaredExceptions;

    /**
     * Exception classes obtained from the throws clause of the methods on the
     * component interface. This is a list of exceptions declared for
     * this method on the component interface. This field is only non-null
     * for asynchronous methods.
     */
    // d734957
    Class<?>[] ivDeclaredExceptionsComp;

    /**
     * Indicates whether denyAll security permission is set.
     */
    boolean ivDenyAll;
    /**
     * Indicates whether permitAll security permission is set.
     */
    boolean ivPermitAll;
    /**
     * List of security roles permitted to execute the method.
     */
    String[] ivRolesAllowed;

    public EJBMethodInfoImpl(int slotSize)
    {
        super(slotSize);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() +
               '[' + methodSignature +
               ", " + (ivInterface != null ? getEJBMethodInterface() : null) +
               ", " + (bmd != null ? bmd.j2eeName : null) +
               ']';
    }

    public void setSecurityPolicy(boolean denyAll, boolean permitAll, String[] rolesAllowed)
    {
        ivDenyAll = denyAll;
        ivPermitAll = permitAll;
        ivRolesAllowed = rolesAllowed;
    }

    /**
     * Return classname of bean implementation class.
     */
    public String getBeanClassName()
    {
        return bmd.enterpriseBeanClassName;
    } // getBeanClassName

    /**
     * Return classname of bean abstract class (EJB 2.0) or the implementation class otherwise.
     */
    // d184983 Begins
    public String getAbstractBeanClassName()
    {
        return bmd.enterpriseBeanAbstractClass.getName();
    }

    // d184983 Ends

    /**
     * @deprecated //d513978
     *             Return home name for this method. <p>
     */
    @Deprecated
    public String getHomeName()
    {
        // If the simpleJndiBindingName has been set then we will
        // use that.  If that is null it is possible that the
        // Home name was set in the xml or a default binding
        // name was generated.   In this case we will
        // use the remote name if one exists, otherwise
        // we will use the local name (which may also be null).  //d513978
        String returnValue = bmd.simpleJndiBindingName;
        if ((returnValue == null) || (returnValue.equals(""))) {
            returnValue = bmd.ivRemoteHomeJndiName;
            if ((returnValue == null) || (returnValue.equals(""))) {
                returnValue = bmd.ivLocalHomeJndiName;
            }
        }

        return returnValue;
    } // getHomeName

    /**
     * Return method name for this method. <p>
     */
    @Override
    public String getMethodName()
    {
        // Generally, the method name is pretty static, but for internal
        // CMR methods (with index < 0) they are dynamic, and based on
        // the signature. For performance, the substring operation is
        // deferred until the method name is actually used.             d154342.10
        if (methodName == null && methodSignature != null)
            methodName = methodSignature.substring(0, methodSignature.indexOf(":"));

        return methodName;
    } // getMethodName

    /**
     * Return method signature for this method. <p>
     */
    @Override
    public String getMethodSignature()
    {
        return methodSignature; //92697
    } // getMethodSignature

    public void setMethodDescriptor(String descriptor)
    {
        this.jdiMethodSignature = descriptor;
    }

    @Override
    public String getMethodDescriptor()
    {
        return jdiMethodSignature;
    }

    /**
     * Return JDI (Java-internal) method signature for this method.
     * Required by debuggers.<p>
     */
    public String getJDIMethodSignature() // 135893
    {
        return jdiMethodSignature;
    } // getMethodSignature

    /**
     * Return transaction attribute associated with this method.
     */
    public TransactionAttribute getTransactionAttribute()
    {
        return txAttr;
    } // getTransactionAttribute

    @Override
    public EJBTransactionAttribute getEJBTransactionAttribute()
    {
        return EJBTransactionAttribute.forValue(txAttr.getValue());
    }

    public void setCMP11FBPK(boolean value)
    {
        this.isaCMP11FBPK = value;
    }

    // d112604.1 begins
    /**
     * Return true if method is a FPBK.
     */
    public boolean getIsCMP11FBPK()
    {
        return (isaCMP11FBPK);
    } // getIsCMP11FBPK

    public void setCMP11CustomFinderWithForUpdateAI(boolean value)
    {
        this.isaCMP11CustomFinderWithForUpdateAI = value;
    }

    /**
     * Return true if method is a custom finder.
     */
    public boolean getIsCMP11CustomFinderWithForUpdateAI()
    {
        return (isaCMP11CustomFinderWithForUpdateAI);
    } // getIsCMP11CustomFinder

    // d112604.1 ends

    public void setIsolationLevel(int isolationLevel)
    {
        this.isolationAttr = isolationLevel;
    }

    /**
     * Return isolation attribute associated with this method.
     */
    public int getIsolationLevel()
    {
        return isolationAttr;
    }

    public void setActivitySessionAttribute(ActivitySessionAttribute asAttr)
    {
        this.asAttr = asAttr;
    }

    /**
     * Return activity session attribute associated with this method.
     */
    public ActivitySessionAttribute getActivitySessionAttribute()
    {
        return asAttr;
    } // getActivitySessionAttribute

    public void setReadOnly(boolean readOnly)
    {
        this.readOnlyAttr = readOnly;
    }

    /**
     * Return the read-only attribute associated with this method
     */
    public boolean getReadOnlyAttribute()
    {
        return readOnlyAttr;
    }

    @Override
    public EJBMethodInterface getEJBMethodInterface()
    {
        return EJBMethodInterface.forValue(ivInterface.getValue());
    }

    /**
     * Returns the type of interface this method belongs to. For example,
     * Remote, Local, etc..
     **/
    // d162441
    public MethodInterface getInterfaceType()
    {
        return ivInterface;
    }

    /**
     * Return true if this method is associated with a home bean.
     */
    public boolean isHome()
    {
        return isHome;
    } // isHome

    /**
     * Return true if this method is associated with stateful session bean.
     */
    public boolean isStatefulSessionBean()
    {
        return isStatefulSessionBean;
    } // isStatefulSessionBean

    /**
     * Return true if this method is associated with stateless session bean.
     */
    public boolean isStatelessSessionBean()
    {
        return isStatelessSessionBean;
    } // isStatelessSessionBean

    /**
     * Return true if this method is associated with singleton session bean.
     */
    // F743-508
    public boolean isSingletonSessionBean()
    {
        return isSingletonSessionBean;
    } // isSingletonSessionBean

    /**
     * Return true if this method is 'create' on a Home. Provided for performance.
     */
    public boolean isHomeCreate()
    {
        return isHomeCreate;
    }

    /**
     * Return true if this is {@link javax.ejb.EJBObject#remove} or {@link javax.ejb.EJBLocalObject#remove}.
     */
    public boolean isComponentRemove() // d647928.2
    {
        return isComponentRemove;
    }

    public void setComponentRemove(boolean componentRemove) // d647928.2
    {
        isComponentRemove = componentRemove;
    }

    /**
     * Returns true if this method is defined in a local home, local component,
     * or local business interface.
     */
    public boolean isLocalInterface() // LIDB1587.33
    {
        return isLocal; // LIDB1587.33
    } // LIDB1587.33

    /**
     * Returns true if this method is defined to be 'Lightweight', indicating
     * pre- and post-method processing should be streamlined.
     **/
    // LI3795-56
    public boolean isLightweight()
    {
        return isLightweight;
    }

    /**
     * return ComponentMetaData
     */
    @Override
    public ComponentMetaData getComponentMetaData() //LIDB1181.2.1
    {
        return bmd;
    }

    /**
     * return EJBComponentMetaData
     */
    @Override
    public EJBComponentMetaData getEJBComponentMetaData() // 120687
    {
        return bmd;
    }

    public BeanMetaData getBeanMetaData()
    {
        return bmd;
    }

    /**
     * Return true if dangling work at the end of a local transaction
     * is to be committed
     */
    public boolean getCommitDanglingWork()
    {
        return bmd.commitDanglingWork;
    }

    @Override
    public String getName()
    {
        return methodName;
    }

    @Override
    public void release()
    {
        // nothing to release.
    }

    /**
     * Reset EJBMethodInfoImpl.
     * The method initializeInstanceData is used to allow EJBMethodInfoImpl
     * Objects to be pooled, and reused. The method signature, methodName,
     * beanmetadata, isHome, and isLocal members need to be reset in order for
     * it to be reused.
     * Note that reuse is only for method Ids <0 i.e. used by PM code.
     **/
    // d140003.35
    public void initializeInstanceData(String methodSign,
                                       String methodNameOnly,
                                       BeanMetaData beanMetaData,
                                       MethodInterface methodInterface, // d162441 199625
                                       TransactionAttribute tranAttr,
                                       boolean asyncMethod) // 199625
    {
        this.methodSignature = methodSign; //92697
        this.methodName = methodNameOnly;
        this.bmd = beanMetaData;
        this.ivInterface = methodInterface; // d162441
        this.txAttr = tranAttr; // 199625
        this.ivAsynchMethod = asyncMethod;

        // Method info stack may be used for Entity or Stateful.        F743-29185
        if (bmd != null) {
            isStatefulSessionBean = bmd.type == InternalConstants.TYPE_STATEFUL_SESSION;
            isStatelessSessionBean = bmd.type == InternalConstants.TYPE_STATELESS_SESSION;
            isSingletonSessionBean = bmd.type == InternalConstants.TYPE_SINGLETON_SESSION;
        }

        // The following are optimizations that may be determined from
        // the method interface type.                                      d162441
        this.isHome = (methodInterface == MethodInterface.HOME ||
                        methodInterface == MethodInterface.LOCAL_HOME);
        this.isLocal = (methodInterface == MethodInterface.LOCAL ||
                        methodInterface == MethodInterface.LOCAL_HOME);
        this.isHomeCreate = (this.isHome &&
                        "create".equals(methodName)); // F61004.3

        // Determine if the ClassLoader should be set in preInovke.      LI2281.07
        // Set if ivAsyncMethod because neither async WorkManager nor the ORB code
        // paths will manage the class loader in remote async methods calls.    d614994
        this.setClassLoader = (methodInterface == MethodInterface.LOCAL ||
                               methodInterface == MethodInterface.LOCAL_HOME ||
                               methodInterface == MethodInterface.SERVICE_ENDPOINT ||
                               methodInterface == MethodInterface.MESSAGE_LISTENER ||
                               methodInterface == MethodInterface.TIMED_OBJECT || // d667153.2
        ivAsynchMethod); // d614994

        // Optimizations for determining if method is 'Lightweight'.     LI3795-56
        this.isLightweight = isLocal && beanMetaData != null && beanMetaData.isLightweight; // d652578.1
        this.isLightweightTxCapable = isLightweight &&
                                      ((tranAttr == TransactionAttribute.TX_REQUIRED ||
                                        tranAttr == TransactionAttribute.TX_SUPPORTS ||
                                      tranAttr == TransactionAttribute.TX_MANDATORY) &&
                                      (asAttr == ActivitySessionAttribute.AS_UNKNOWN || // d352213
                                      asAttr == ActivitySessionAttribute.AS_SUPPORTS)); // d352213
    }// d140003.35

    // ---------------------------------------------------------
    //  NOTE:: getAMCName() should only be called by SMF.
    //    to get the official AMC name, use J2EEName.toString().
    // ---------------------------------------------------------
    public String getAMCName() { // @MD16426A
        J2EEName j2eeName = bmd.getJ2EEName(); // @MD16426A
        String app = j2eeName.getApplication(); // @MD16426A
        String mod = j2eeName.getModule(); // @MD16426A
        String comp = j2eeName.getComponent(); // @MD16426A
        String retval = app + "::" + mod + "::" + comp; // @PK07137
        return retval; // @MD16426A
    }

    public J2EEName getJ2EEName()
    {
        return bmd.getJ2EEName();
    }

    public int getBeanType()
    {
        return bmd.type;
    }

    public boolean isReentrant()
    {
        return bmd.reentrant;
    }

    // LIDB2775-23.0 Ends

    /**
     * Get the java reflection Method object for the business
     * method this is EJBMethodInfoImpl object is associated with.
     * 
     * @return the java reflection Method object. Note, this Method object
     *         is for the EJB class itself, not the business or component
     *         interface Method object (e.g. Method.getDeclaringClass
     *         returns class for the EJB class, not the interface class).
     */
    // d367572.2 added method.
    @Override
    public Method getMethod()
    {
        return ivMethod;
    }

    /**
     * Set the java reflection Method object for the business
     * method this is EJBMethodInfoImpl object is associated with.
     * 
     * @param method is the java reflection Method object. Note, this Method object
     *            is for the EJB class itself, not the business or component
     *            interface Method object (e.g. Method.getDeclaringClass
     *            returns class for the EJB class, not the interface class).
     */
    // d367572.2 added method.
    public void setMethod(Method method)
    {
        ivMethod = method;
    }

    //F743-15870
    /**
     * Get the number of parms required by the java reflection Method
     * object this EJBMethodInfoImpl object is associated with.
     * 
     * @return the number of parms the method takes
     */
    public int getNumberOfMethodParms()
    {
        return ivNumberOfMethodParms;
    }

    //F743-15870
    /**
     * Sets the number of parms required by the java reflection Method
     * object this EJBMethodInfoImpl object is associated with.
     * 
     * @param numberOfParms
     */
    public void setNumberOfMethodParms(int numberOfParms)
    {
        ivNumberOfMethodParms = numberOfParms;
    }

    /**
     * Set the Bridge Method object that is the 'bridge' method for the target
     * method of this EJBMethodInfo. Bridge methods occur when an interface
     * uses generics. The bridge method is also from the EJB class itself, not
     * the interface, but will have the same signature as the interface method.
     **/
    // d517824
    public void setBridgeMethod(Method bridgeMethod)
    {
        ivBridgeMethod = bridgeMethod;
    }

    /**
     * Returns the Bridge Method object that is the 'bridge' method for the
     * target method of this EJBMethodInfo. Null will be returned if there
     * is no Bridge method for this EJBMethodInfo. <p>
     * 
     * Bridge methods occur when an interface uses generics. The bridge method is
     * also from the EJB class itself, not the interface, but will have the same
     * signature as the interface method.
     **/
    // d540438
    public Method getBridgeMethod()
    {
        return ivBridgeMethod;
    }

    /**
     * Returns true if the specified method has the same name and parameters
     * as either the target or bridge methods of this EJBMethodInfo. <p>
     * 
     * Similar to Method.equals(), except declaring class and return type are
     * NOT considered. <p>
     * 
     * @param method method to compare
     * 
     * @return true if method has the same name and parameters;
     *         otherwise false.
     **/
    // d517824
    public boolean methodsMatch(Method method)
    {
        boolean match = false;

        if (method == ivMethod ||
            method == ivBridgeMethod)
        {
            match = true;
        }
        else if (method.getName().equals(ivMethod.getName()))
        {
            Class<?>[] parms1 = method.getParameterTypes();
            Class<?>[] parms2 = ivMethod.getParameterTypes();
            if (parms1.length == parms2.length)
            {
                match = true;
                int length = parms1.length;
                for (int i = 0; i < length; i++)
                {
                    if (parms1[i] != parms2[i])
                    {
                        match = false;
                        break;
                    }
                }

                // Also check against the bridge method, if present
                if (!match && ivBridgeMethod != null)
                {
                    match = true;
                    parms2 = ivBridgeMethod.getParameterTypes();
                    for (int i = 0; i < length; i++)
                    {
                        if (parms1[i] != parms2[i])
                        {
                            match = false;
                            break;
                        }
                    }
                }
            }
        }

        return match;
    }

    /**
     * Get the array of InterceptorProxy objects needed for
     * invoking the around invoke or around timeout interceptor methods
     * whenever the method associated with this EJBMethodInfoImpl object
     * is invoked.
     * 
     * @return InterceptorProxy array or null if there are no around invoke
     *         interceptors to be called when the method is invoked.
     */
    // d367572.2 added method, F743-17763.1 renamed method
    public InterceptorProxy[] getAroundInterceptorProxies()
    {
        return ivAroundInterceptors;
    }

    /**
     * Set the array of InterceptorProxy objects needed for
     * invoking the around invoke or around timeout interceptor methods
     * whenever the method associated with this EJBMethodInfoImpl object
     * is invoked.
     * 
     * @param proxies is the array of InterceptorProxy objects, one per
     *            interceptor method to be invoked.
     */
    // d367572.2 added method, F743-17763.1 renamed method
    public void setAroundInterceptorProxies(InterceptorProxy[] proxies)
    {
        ivAroundInterceptors = proxies;
    }

    /**
     * Flag this EJBMethodInfoImpl object as being a SFSB business method
     * that should cause SFSB to be removed once business method completes.
     * 
     * @param retainIfException must be true if SFSB should be retained if
     *            business method throws an application exception.
     */
    public void setSFSBRemove(boolean retainIfException) // d384182
    {
        ivSFSBRemove = true;
        ivRetainIfException = retainIfException;
    }

    /**
     * Return a String indicating the run-as identity for the execution of this
     * method.
     * 
     * @return String indicating the run-as identity for the execution of this
     *         method.
     */
    //366845.11.1
    public String getRunAsSpecifiedIdentity() {
        // The RunAs value is EJB scoped, so get it from BeanMetaData
        return bmd.ivRunAs;
    }

    /**
     * Set the String indicating the run-as identity for the execution of this
     * method.
     */
    //366845.11.2
    public void setRunAsSpecifiedIdentity(String identity) {
        // The RunAs value is EJB scoped, so set it on BeanMetaData
        bmd.ivRunAs = identity;
    }

    /**
     * Return a boolean indicating if the identity for the execution of this
     * method is to come from the caller.
     * 
     * @return boolean indicating if the identity for the execution of this
     *         method is to come from the caller.
     */
    //366845.11.1
    public boolean isRunAsCallerIdentity() {
        return bmd.ivUseCallerIdentity;
    }

    /**
     * Set the boolean indicating if the identity for the execution of this
     * method is to come from the caller.
     */
    //366845.11.2
    public void setRunAsCallerIdentity(boolean runAs) {
        bmd.ivUseCallerIdentity = runAs;
    }

    /**
     * Return a String representing the security role that is
     * associated with the given link. If the given string is
     * an actual role and not a link, then null is returned.
     * 
     * @param link The String representing the link.
     * 
     * @return String representing the security role associated with the given
     *         link. Null is returned when the given string is not found in the
     *         link-to-role mapping. This indicated that the given string is a
     *         role itself and not a link.
     */
    //366845.11.1
    public String getRole(String link) {

        String result = null;
        if (bmd.ivRoleLinkMap != null) {
            result = bmd.ivRoleLinkMap.get(link);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getRoleFromLink returning " + result);
        return result;
    }

    /**
     * Writes the significant state data of this class, in a readable format,
     * to the specified output writer. <p>
     * 
     * @param writer output resource for the introspection data
     * @param type interface type of the method, if known. (e.g. Remote, Local, etc.)
     * @param methodId is the index EJBMethodInfo array kept in the BeanMetaData.
     *            A value of Integer.MAX_VALUE is used to indicate method ID is unknown.
     * @param dumpCMP2xAccessIntent indicates whether or not to dump CMP 2.x AccessIntent.
     *            This can only be done if Application Profile AccessIntent service has
     *            completed its initialization for the bean during server startup or during
     *            EJB deferred initialization. If not sure if this initialization is completed,
     *            the pass false for this parameter. Only pass true when you know with certainty
     *            that it has completed. If true is passed when AccessIntent has not completed,
     *            an unexpected exception could occurring during the dump and prevent the
     *            application from starting successfully. So make sure AI init for the bean
     *            has completed (e.g. dump is called while application is running and invoked
     *            this method).
     */
    // F86406
    public void introspect(IntrospectionWriter writer, String type, int methodId, boolean dumpCMP2xAccessIntent)
    {
        writer.begin(null);
        writer.println("Method signature  = " + methodSignature + ((type != null) ? ("  (" + type + ")") : ""));
        writer.println("Method index      = " + ((methodId != Integer.MAX_VALUE) ? methodId : "unknown"));
        writer.println("TX attribute      = " + MethodAttribUtils.TX_ATTR_STR[txAttr.getValue()]);

        if (asAttr != null)
        {
            writer.println("AS attribute      = " + asAttr.toString());
        }

        // d493211 start
        // Is this a EJB 1.x module?
        if (bmd.ivModuleVersion <= BeanMetaData.J2EE_EJB_VERSION_1_1)
        {
            // Yep, this is a 1.x module version, so old connection manager is being used.
            // Old connection manager does not allow isolation level to change once set
            // for a transaction.  So dump the isolation level that is used for CMP 1.x access
            // regardless of bean type being processed.  This is done since the first
            // method call in a transaction that is started by a EJB in a 1.x module
            // causes the isolation level for that TX to be set. See preInvokeActivate
            // method in EJSContainer to see the logic for setting TX isolation level.
            writer.println("CMP 1.x Isolation Level = " + MethodAttribUtils.getIsolationLevelString(isolationAttr));

            // If this is a CMP 1.x bean, then dump read only attribute for the CMP 1.x bean.
            // Does not apply for other bean types, so only do for CMP 1.x beans. See ContainerManagedBeanO
            // for CMP 1.x implementation since that is the only place that uses the read only attribute
            // (e.g. used to determine if dirty flag should be set for the CMP 1.x bean). Note, WAS does
            // NOT support CMP 2.x beans in a 1.x module, so we do not have to worry about CMP 2.x.
            if (bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X)
            {
                writer.println("CMP 1.x access intent(read-only attribute) = " + readOnlyAttr);
            }
        }
        else
        {
            // This is a 2.x or later module version, so new J2C connection manager is being used.
            // Is this a CMP 2.x bean and dumpCMP2xAccessIntent is true?
            if (bmd.cmpVersion == InternalConstants.CMP_VERSION_2_X && dumpCMP2xAccessIntent)
            {
                introspectCMP2xAccessIntent(writer);
            }
            else
            {
                // Not a CMP 2.x bean in 2.x module or later or dumpCMP2xAccessIntent is false.
                // We know this is a 2.x or later module version. If this is a CMP 1.x bean, then dump the
                // CMP 1.x isolation level and read only attribute.  Only do this for CMP 1.x rather
                // than all EJB types since preInvokeActivate in EJSContainer only sets TX
                // isolation level when module version is 2.x or later when the EJB type is CMP 1.x.
                if (bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X)
                {
                    writer.println("CMP 1.x Isolation Level = " + MethodAttribUtils.getIsolationLevelString(isolationAttr));
                    writer.println("CMP 1.x access intent(read-only attribute) = " + readOnlyAttr);
                }
            }
        }
        // d493211 end
        writer.println("isAsynchMethod    = " + ivAsynchMethod);
        writer.println("JDI signature     = " + jdiMethodSignature);

        //F743-1752CodRev start
        if (isSingletonSessionBean) //F743-1752.1
        {

            if (bmd.ivSingletonUsesBeanManagedConcurrency)
            {
                writer.println("Singleton LockType       = Bean Managed");
            }
            else
            {
                String lockType = null;
                if (ivLockType != null)
                {
                    lockType = ivLockType.name();
                }
                else
                {
                    if (ivInterface == MethodInterface.LIFECYCLE_INTERCEPTOR)
                    {
                        lockType = "not applicable for lifecycle methods";
                    }
                }
                writer.println("Singleton LockType       = " + lockType);
                writer.println("Singleton Access Timeout = " + ivAccessTimeout);
            }
        } //F743-1752CodRev end
        else if (isStatefulSessionBean)
        {
            // dump unique Stateful method data                          F743-22462
            writer.println("Stateful Access Timeout = " + ivAccessTimeout);
        }

        introspectDeclaredExceptions(writer);
        introspectSecurityInfo(writer);
        writer.end();
    }

    private void introspectDeclaredExceptions(IntrospectionWriter writer)
    {
        if (ivDeclaredExceptions != null)
        {
            int interfaceidx = 0;
            for (Class<?>[] exceptions : ivDeclaredExceptions)
            {
                writer.println("Declared exceptions in business interface [" +
                               interfaceidx++ + "] = " + Arrays.toString(exceptions));
            }
        }
        if (ivDeclaredExceptionsComp != null)
        {
            writer.println("Declared exceptions in component interface    = " + Arrays.toString(ivDeclaredExceptionsComp));
        }
    }

    protected void introspectCMP2xAccessIntent(IntrospectionWriter writer)
    {
        // This method is overridden in SharedEJBMethodInfoImpl to allow for
        // AccessIntent logic to be moved out of shared logic and into specific
        // traditional WAS logic.
    }

    /**
     * Check whether EJBMethodInfoImpl is for a remove method of a EJB 3 SFSB.
     * 
     * @return true if and only if EJBMethodInfo is for a EJB 3 SFSB.
     */
    // d430356 - added entire method
    public boolean isSFSBRemoveMethod()
    {
        return ivSFSBRemove;
    }

    /**
     * Returns true if this is a stateful remove-method.
     */
    @Override
    public boolean isStatefulRemove()
    {
        return ivSFSBRemove;
    }

    /**
     * Returns true if {@link #isComponentRemove} or {@link #isStatefulRemove} are true.
     */
    public boolean isBeanRemove()
    {
        return isComponentRemove || ivSFSBRemove;
    }

    /**
     * Returns the JPA task name to be assigned to transactions started by this
     * method for JPA Access Intent support. <p>
     * 
     * The 'task name' is defined by JPA and consists of the bean name + method
     * name of the method which began the transaction. <p>
     **/
    // d515803
    public String getJPATaskName()
    {
        if (ivJPATaskName == null)
        {
            ivJPATaskName = bmd.enterpriseBeanClassName + "." + getMethodName();
        }
        return ivJPATaskName;
    }

    /**
     * Return true if this method is asynchronous.
     */
    public boolean isAsynchMethod()
    {
        return (ivAsynchMethod);
    }

    /**
     * Set the LockType for a singleton session bean that is
     * using container managed concurrency control.
     * 
     * @param lockType
     */
    // F743-1752.1
    public void setCMLockType(LockType lockType)
    {
        ivLockType = lockType;
    }

    /**
     * Set the lock access timeout value for a singleton session bean that is
     * using container managed concurrency control or the access timeout
     * value for a stateful session bean.
     * 
     * @param accessTimeout
     */
    // F743-1752.1
    public void setCMLockAccessTimeout(long accessTimeout)
    {
        ivAccessTimeout = accessTimeout;
    }

    /**
     * Sets the declared exceptions for this method on the business interface.
     * The types are obtained from the throws clause of the interface methods.
     * 
     * <p>This method is only called for asynchronous methods.
     */
    // F743-761
    public void setDeclaredExceptions(Class<?>[][] declaredExceptions)
    {
        ivDeclaredExceptions = declaredExceptions;
    }

    /**
     * Sets the declared exceptions for the method on the component interface.
     * The types are obtained from the throws clause of the interface methods.
     * 
     * <p>This method is only called for asynchronous methods.
     */
    // d734957
    public void setDeclaredExceptionsComp(Class<?>[] declaredExceptionsComp)
    {
        ivDeclaredExceptionsComp = declaredExceptionsComp;
    }

    /**
     * Return boolean indicating that no security roles are allowed
     * to execute this method.
     * 
     * @return boolean indicating if all roles are not to be permitted to execute
     *         this method.
     */
    @Override
    public boolean isDenyAll()
    {
        return ivDenyAll;
    }

    /**
     * Return boolean indicating that all security roles are allowed
     * to execute this method.
     * 
     * @return boolean indicating if all roles are permitted to execute
     *         this method.
     */
    @Override
    public boolean isPermitAll()
    {
        return ivPermitAll;
    }

    /**
     * Return a list containing all security roles that are
     * allowed to execute this method.
     * 
     * @return List of strings containing all security roles that are
     *         allowed to execute this method.
     */
    @Override
    public List<String> getRolesAllowed()
    {
        return ivRolesAllowed == null ? Collections.<String> emptyList() : Arrays.asList(ivRolesAllowed);
    }

    /**
     * Return a boolean indicating if the identity for the execution of this
     * method is to come from the caller.
     * 
     * @return boolean indicating if the identity for the execution of this
     *         method is to come from the caller.
     */
    @Override
    public boolean isUseCallerPrincipal()
    {
        throw new UnsupportedOperationException("This method is not currently implemented");
    }

    /**
     * Return a boolean indicating if the identity for the execution of this
     * method is the system principle.
     * 
     * @return boolean indicating if the identity for the execution of this
     *         method is the system principle.
     */
    @Override
    public boolean isUseSystemPrincipal()
    {
        throw new UnsupportedOperationException("This method is not currently implemented");
    }

    @Override
    public String getRunAs()
    {
        // The RunAs value is EJB scoped, so get it from BeanMetaData
        return bmd.ivRunAs;
    }

    protected void introspectSecurityInfo(IntrospectionWriter writer)
    {
        writer.println("DenyAll       = " + ivDenyAll);
        writer.println("PermitAll     = " + ivPermitAll);
        writer.println("RolesAllowed  = " + getRolesAllowed());
    }
} // EJBMethodInfoImpl
