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
package com.ibm.wsspi.injectionengine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;

import com.ibm.ejs.util.dopriv.SetAccessiblePrivilegedAction;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Defines the injection target information specified via XML and/ro annotations.
 * This class provides the implementation field target.
 */
public class InjectionTargetField extends InjectionTarget {
    private static final String CLASS_NAME = InjectionTargetField.class.getName();

    private static final TraceComponent tc = Tr.register(InjectionTargetField.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    Field ivField = null;

    /**
     * Constructor to create an InjectionTarget
     *
     * @param field the field this target represents
     * @param binding - the binding this target will be associated with.
     */
    protected InjectionTargetField(Field field, InjectionBinding<?> binding) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "<init> : " + field);

        boolean isFromClient = binding.ivNameSpaceConfig.isClientMain(field.getDeclaringClass());
        int fieldModifiers = field.getModifiers();

        // -----------------------------------------------------------------------
        // Per Specification - an injection target must NOT be final
        // -----------------------------------------------------------------------
        if (Modifier.isFinal(fieldModifiers)) {
            Tr.error(tc, "INJECTION_TARGET_FIELD_MUST_NOT_BE_FINAL_CWNEN0019E", field.getName());
            InjectionConfigurationException icex = new InjectionConfigurationException("The injection target field " + field.getName() + " must not be declared final");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "<init> : " + icex);
            throw icex;
        }

        // -----------------------------------------------------------------------
        // Per Specification - an injection target must NOT be static, except
        //                     for application client or if a CDI producer, annotated with @Produces
        // -----------------------------------------------------------------------
        //d519665:
        Class<? extends Annotation> producesClass = getProducesClass(field.getDeclaringClass().getClassLoader());
        if (producesClass == null || !field.isAnnotationPresent(producesClass)) {
            if (Modifier.isStatic(fieldModifiers) && !isFromClient) {
                Tr.warning(tc, "INJECTION_TARGET_MUST_NOT_BE_STATIC_CWNEN0057W",
                           field.getDeclaringClass().getName(), field.getName());
                if (binding.isValidationFailable()) // fail if enabled     F743-14449
                {
                    throw new InjectionConfigurationException("The " + field.getDeclaringClass().getName() + "." +
                                                              field.getName() + " injection target must not be declared static.");
                }
            } else if (!Modifier.isStatic(fieldModifiers) && isFromClient) {
                Tr.error(tc, "INJECTION_TARGET_IN_CLIENT_MUST_BE_STATIC_CWNEN0058E",
                         field.getDeclaringClass().getName(), field.getName());
                InjectionConfigurationException icex = new InjectionConfigurationException("The injection target field " + field.getName()
                                                                                           + " must be declared static in the client container.");
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "<init> : " + icex);
                throw icex;
            }
        }

        // -----------------------------------------------------------------------
        // This is a valid field to inject into, but if it is not public,
        // 'setAccessable' must be called on the reflect object to allow
        //  WebSphere to set the field.
        // -----------------------------------------------------------------------
        if (!Modifier.isPublic(fieldModifiers)) {
            try {
                SetAccessiblePrivilegedAction privilegedFieldAction = new SetAccessiblePrivilegedAction(field, true);
                AccessController.doPrivileged(privilegedFieldAction);
            } catch (PrivilegedActionException paex) {
                FFDCFilter.processException(paex, CLASS_NAME + ".<init>",
                                            "97", this, new Object[] { field, binding });

                SecurityException ex = (SecurityException) paex.getException();
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "<init> : " + ex);
                throw ex;
            }
        }

        // -----------------------------------------------------------------------
        // Inform the binding of the type required for injection into this field.
        // -----------------------------------------------------------------------
        binding.setInjectionClassType(field.getType()); // F743-32637

        // Finally, save the reflect object for use at runtime to inject
        ivField = field;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "<init> : " + ivField);
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore(ClassNotFoundException.class)
    private Class<? extends Annotation> getProducesClass(ClassLoader loader) {
        Class<? extends Annotation> producesClass = null;
        try {
            producesClass = (Class<? extends Annotation>) loader.loadClass("javax.enterprise.inject.Produces");
        } catch (ClassNotFoundException e) {
            //ignore FFDC
            producesClass = null;
        }
        return producesClass;
    }

    /**
     * Returns the field of the injection target
     */
    @Override
    public Member getMember() {
        return ivField;
    }

    /**
     * Returns the <code>Class</code> object that identifies the
     * declared type for the field represented by this
     * <code>Field</code> object.
     */
    @Override
    public Class<?> getInjectionClassType() {
        return ivField.getType();
    }

    /**
     * Returns the Type for the field or method.
     */
    // d662220
    @Override
    public Type getGenericType() {
        return ivField.getGenericType();
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
    @Override
    protected void injectMember(Object objectToInject, Object dependentObject) throws Exception {
        ivField.set(objectToInject, dependentObject);
    }
}
