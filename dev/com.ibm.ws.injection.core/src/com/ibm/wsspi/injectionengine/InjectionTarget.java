/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Defines the injection target information specified via XML and/from annotations.
 * This class only provides the abstract methods to get and set the member. Each
 * implementing class will specify whether it is a method or field.
 */
public abstract class InjectionTarget {
    private static final String CLASS_NAME = InjectionTarget.class.getName();

    private static final TraceComponent tc = Tr.register(InjectionTarget.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    protected InjectionBinding<?> ivInjectionBinding;

    /**
     * Set to true when the injection target came from XML only; reset to false
     * if same target found as annotation. Used to provide proper XML override
     * support.
     **/
    // d510950
    protected boolean ivFromXML = false;

    /**
     * Returns the member(a field or a method) of the injection target.
     *
     * @return the ivMember
     */
    public abstract Member getMember();

    /**
     * Returns the Class of injection target
     */
    public abstract Class<?> getInjectionClassType();

    Class<?>[] getInjectionClassTypes() {
        return new Class<?>[] { getInjectionClassType() };
    }

    Object[] getInjectedObjects(Object injectedObject) {
        return new Object[] { injectedObject };
    }

    /**
     * Returns the Type for the field or method.
     */
    // d662220
    public abstract Type getGenericType();

    @Override
    public String toString() {
        return super.toString() + '[' + getMember() + ", " + ivInjectionBinding.toSimpleString() + ']';
    }

    /**
     * Injects the object identified by the injection binding associated with
     * this target into the specified target instance based on the specified
     * target context. <p>
     *
     * For example, let's say the objectToInject is a SessionBean, the
     * object defined by the associated resource binding/reference is the
     * String "blue", the target field is ivColor. When injected the String
     * "blue" would be set in the field ivColor on the bean. <p>
     *
     * @param objectToInject the object that is the target of the injection.
     * @param targetContext provides access to context data associated with
     *            the target of the injection (e.g. EJBContext).
     *            May be null if no context to be provided by the
     *            container.
     * @throws InjectionException if an error occurs locating the object to
     *             inject or actually injecting it into the target
     *             instance.
     */
    // F49213.1
    public void inject(Object objectToInject,
                       InjectionTargetContext targetContext) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "inject : " + getMember() + " : " +
                         Util.identity(objectToInject) + ", " + targetContext);

        Object injectedObject = ivInjectionBinding.getInjectableObject(objectToInject,
                                                                       targetContext);

        try {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "injecting " + Util.identity(injectedObject));

            injectMember(objectToInject, injectedObject);
        } catch (IllegalArgumentException ex) // RTC105173
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".inject", "140", this,
                                        new Object[] { objectToInject, targetContext, injectedObject, ivInjectionBinding });
            String msg = Tr.formatMessage(tc,
                                          "INCOMPATIBLE_INJECTED_OBJECT_TYPE_CWNEN0074E",
                                          injectedObject.getClass().getName(),
                                          ivInjectionBinding.getDisplayName(),
                                          getMember());
            InjectionException targetEx = new InjectionException(msg, ex);
            if (isTraceOn && tc.isEntryEnabled()) {
                final Class<?>[] types = getInjectionClassTypes();
                final Object[] injectedObjects = getInjectedObjects(injectedObject);

                // Include the relevant ClassLoader in the trace.          RTC118506
                String debug = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0, end = Math.max(types.length, injectedObjects.length); i < end; i++) {
                            if (i != 0) {
                                sb.append(", ");
                            }
                            sb.append('[').append(i).append(']');

                            sb.append(" type=");
                            Class<?> type;
                            if (i < types.length) {
                                type = types[i];
                                // getClassLoader requires permission.
                                sb.append(type.getName()).append(" (loader=").append(type.getClassLoader()).append(')');
                            } else {
                                type = null;
                                sb.append("error");
                            }

                            sb.append(" object=");

                            Class<?> injectedObjectType;
                            if (i < injectedObjects.length) {
                                boolean assignable;

                                Object injectedObject = injectedObjects[i];
                                if (injectedObject == null) {
                                    assignable = type != null && !type.isPrimitive();
                                    sb.append("null");
                                } else {
                                    injectedObjectType = injectedObjects[i].getClass();
                                    assignable = type != null && type.isAssignableFrom(injectedObjectType);
                                    // getClassLoader requires permission.
                                    sb.append(injectedObjectType.getName()).append(" (loader=").append(injectedObjectType.getClassLoader()).append(')');
                                }

                                if (type != null) {
                                    sb.append(" assignable=").append(assignable);
                                }
                            } else {
                                sb.append("error");
                            }
                        }

                        return sb.toString();
                    }
                });

                Tr.exit(tc, "inject : " + debug + " : " + targetEx);
            }
            throw targetEx;
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".inject",
                                        "97", this, new Object[] { objectToInject, targetContext, injectedObject, ivInjectionBinding });

            // All exceptions returned through a reflect method call are wrapped,
            // making it difficult to debug a problem since InovcationTargetEx
            // does not print the root exception. Unwrap the 'target' exception
            // so that the client will see it as the cause of the
            // InjectionException.                                        d534353.1
            Throwable targetEx = ex;
 
            //Unwrap ManagedObjectException before checking for InvocationTargetException. Use the class name to avoid a compile time dependency.
            if (ex.getClass().getName().equals("com.ibm.ws.managedobject.ManagedObjectException")) {
                Throwable unwrappedEx = ex.getCause();
                if (unwrappedEx != null)
                    ex = unwrappedEx;
            }

            if (ex instanceof InvocationTargetException) {
                targetEx = ex.getCause();
                if (targetEx == null)
                    targetEx = ex;
            }

            String name = ivInjectionBinding.getDisplayName();
            InjectionException iex = new InjectionException("The injection engine encountered an error injecting " +
                                                            name + " into " + getMember() + ": " + targetEx, targetEx);
            Tr.error(tc, "INJECTION_FAILED_CWNEN0028E",
                     name, getMember(), targetEx.getMessage());
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "inject : " + targetEx);
            throw iex;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "inject : " + ivInjectionBinding.getDisplayName() + " : " +
                        Util.identity(injectedObject));
    }

    /**
     * Perform the actual injection into the field or method. <p>
     *
     * @param objectToInject the object to inject into
     * @param dependentObject the dependent object to inject
     *
     * @throws Exception if the dependent object cannot be injected into the
     *             associated member of the specified object.
     */
    // F743-29174
    protected abstract void injectMember(Object objectToInject, Object dependentObject) throws Exception;

    /**
     * Returns the parent InjectionBinding containing the information for this injection target
     *
     * @return InjectionBinding
     */
    public InjectionBinding<?> getInjectionBinding() {
        return ivInjectionBinding;
    }

    /**
     * Set the parent InjectionBinding containing the information for this injection target
     *
     * @param injectionBinding
     */
    public void setInjectionBinding(InjectionBinding<?> injectionBinding) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setInjectionBinding : " + injectionBinding);

        ivInjectionBinding = injectionBinding;
    }
}
