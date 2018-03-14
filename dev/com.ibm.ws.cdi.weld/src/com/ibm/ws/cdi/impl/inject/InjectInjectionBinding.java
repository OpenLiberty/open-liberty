/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl.inject;

import java.lang.reflect.Method;

import javax.enterprise.context.spi.CreationalContext;
import javax.inject.Inject;

import org.jboss.weld.construction.api.WeldCreationalContext;
import org.jboss.weld.manager.api.WeldManager;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIException;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.CDIUtils;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionSimpleBinding;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.RecursiveInjectionException;

public class InjectInjectionBinding extends InjectionSimpleBinding<Inject> {

    private static final String CLASS_NAME = InjectInjectionBinding.class.getName();

    private static final TraceComponent tc = Tr.register(InjectInjectionBinding.class);
    private final CDIRuntime cdiRuntime;

    private CreationalContext<Object> methodInvocationContext;

    public InjectInjectionBinding(Inject annotation, ComponentNameSpaceConfiguration nameSpaceConfig, CDIRuntime cdiRuntime) {
        super(annotation, nameSpaceConfig);
        this.cdiRuntime = cdiRuntime;

    }

    public J2EEName getJ2EEName() {
        return ivJ2eeName;
    }

    @Override
    public boolean isResolved() {
        return true;
    }

    /**
     * Returns the object to be injected.
     * <p>
     *
     * This is a different (new) instance every time the method is called.
     * <p>
     *
     * @throws Exception
     *             if a problem occurs while creating the instance to be
     *             injected.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object getInjectionObjectInstance(Object targetObject, InjectionTargetContext targetContext) throws Exception {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInjectionObjectInstance");
        WeldCreationalContext<Object> cc = null;

        if (targetContext == null) {
            if (!(targetObject instanceof Class<?>)) {
                // Null target context is only valid when we're doing limited injection on an application main class
                // where we're injecting into the static fields of the class and there is no instance.
                throw new CDIException(Tr.formatMessage(tc, "no.injection.target.context.CWOWB1006E", targetObject));
            }
        } else {
            cc = targetContext.getInjectionTargetContextData(WeldCreationalContext.class);
        }

        WeldManager beanManager = (WeldManager) cdiRuntime.getCurrentBeanManager();
        if (cc == null) {
            //create a dependent scope
            if (beanManager != null) {
                cc = beanManager.createCreationalContext(null);
            }
        }

        if (beanManager != null) {
            methodInvocationContext = beanManager.createCreationalContext(null);
        }
        Object retObj = InjectInjectionObjectFactory.getObjectInstance(this, targetObject, cc, methodInvocationContext, cdiRuntime);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInjectionObjectInstance : " + Util.identity(retObj));

        return retObj;
    }

    @Override
    public void setInjectionClassType(Method method) throws InjectionException {

        if (method.getParameterTypes().length == 1) {
            super.setInjectionClassType(method);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Initializer method multi parameter Injection. InjectionClassType will be null.");
            }
        }
    }

    @Override
    public void cleanAfterMethodInvocation() {

        if (methodInvocationContext != null) {
            methodInvocationContext.release();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.ibm.wsspi.injectionengine.InjectionBinding#getInjectionObject(java.lang.Object, com.ibm.wsspi.injectionengine.InjectionTargetContext)
     * 
     * This method was copied from InjectionBinding.getInjectionObject() and has three differences.
     * 
     * Firstly it does not look in the InjectionBinding.ivInjectedObject field, as InjectInjectionBinding never has that field set anyway.
     *   
     * Secondly if the property com.ibm.ws.cdi.ignoreInjectionFailure is true it will ignore any failure except a RecursiveInjectionException
     * The special status of RecursiveInjectionException is inherited from InjectionBinding where RecursiveInjectionException was the only 
     * Throwable that isn't converted into an InjectionException. 
     * 
     * Thirdly if getInjectionObjectInstance returns a null this method will return a null rather than throw an exception. 
     */
    @Override
    public Object getInjectionObject(Object targetObject,
            InjectionTargetContext targetContext)
                    throws InjectionException {

        Object retObj = null;

        try
        {
            retObj = getInjectionObjectInstance(targetObject, targetContext);
        } catch (RecursiveInjectionException ex)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getInjectionObject: " + ex);
            throw ex;
        } catch (Throwable ex)
        {

            if (CDIUtils.isInjectionFailureIgnored()){
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "com.ibm.ws.cdi.ignoreInjectionFailure = true, ignoring InjectionException", ex);
                }
                return null;
            }

            // Only log FFDC if this is a failed injection; failed naming lookups may
            // be normal and the caller should decide if FFDC is needed.
            if (targetObject != null) {
                FFDCFilter.processException(ex, CLASS_NAME + ".getInjectionObject",
                        "408", this, (Object[]) null);
            }

            String displayName = getDisplayName();
            Object exMessage = ex.getLocalizedMessage();
            if (exMessage == null)
            {
                exMessage = ex.toString();
            }

            String message = Tr.formatMessage(tc,
                    "FAILED_TO_CREATE_OBJECT_INSTANCE_CWNEN0030E",
                    displayName,
                    exMessage);
            InjectionException ex2 = new InjectionException(message, ex);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getInjectionObject", ex2);
            throw ex2;
        }
        
        return retObj;
    }
}
