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

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;

import com.ibm.ejs.util.dopriv.SetAccessiblePrivilegedAction;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Defines the injection target information specified via XML and/ro annotations.
 * This class provides the implementation method target.
 */
public class InjectionTargetMethod extends InjectionTarget
{
    private static final String CLASS_NAME = InjectionTargetMethod.class.getName();

    private static final TraceComponent tc = Tr.register(InjectionTargetMethod.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    /**
     * The method that is the target of injection.
     */
    Method ivMethod = null;

    /**
     * Constructor to create an InjectionTarget
     *
     * @param method
     * @param binding - the binding this target will be associated with.
     */
    protected InjectionTargetMethod(Method method, InjectionBinding<?> binding)
        throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, getClass().getSimpleName() + ".<init> : " + method);

        boolean isFromClient = binding.ivNameSpaceConfig.isClientMain(method.getDeclaringClass());
        int methodModifiers = method.getModifiers();

        // -----------------------------------------------------------------------
        // Per Specification - an injection target must NOT be static, except
        //                     for application client
        // -----------------------------------------------------------------------
        //d519665:
        if (Modifier.isStatic(methodModifiers) && !isFromClient)
        {
            Tr.warning(tc, "INJECTION_TARGET_MUST_NOT_BE_STATIC_CWNEN0057W",
                       method.getDeclaringClass().getName(), method.getName());
            if (binding.isValidationFailable()) // fail if enabled             F743-14449
            {
                throw new InjectionConfigurationException("The " + method.getDeclaringClass().getName() + "." +
                                                          method.getName() + " injection target must not be declared static.");
            }

        }
        else if (!Modifier.isStatic(methodModifiers) && isFromClient)
        {
            Tr.error(tc, "INJECTION_TARGET_IN_CLIENT_MUST_BE_STATIC_CWNEN0058E",
                     method.getDeclaringClass().getName(), method.getName());
            InjectionConfigurationException icex = new InjectionConfigurationException
                            ("The injection target method " + method.getName() +
                             " must be declared static in the client container.");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, getClass().getSimpleName() + ".<init> : " + icex);
            throw icex;
        }

        // -----------------------------------------------------------------------
        // This is a valid method to inject into, but if it is not public,
        // 'setAccessable' must be called on the reflect object to allow
        //  WebSphere to invoke the method.
        // -----------------------------------------------------------------------
        if (!Modifier.isPublic(methodModifiers))
        {
            try
            {
                SetAccessiblePrivilegedAction privilegedMethodAction =
                                new SetAccessiblePrivilegedAction(method, true);
                AccessController.doPrivileged(privilegedMethodAction);
            } catch (PrivilegedActionException paex)
            {
                FFDCFilter.processException(paex, CLASS_NAME + ".<init>",
                                            "97", this, new Object[] { method, binding });

                SecurityException ex = (SecurityException) paex.getException();
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, getClass().getSimpleName() + ".<init> : " + ex);
                throw ex;
            }
        }

        // -----------------------------------------------------------------------
        // Inform the binding of the type of object required for injection into
        // this method by providing the method object. The binding may determine
        // the type(s) by evaluating the parameter(s). Normally, exactly one
        // parameter is allowed, but if the binding supports initializer methods
        // then it may evaluate the parameters for annotations as well as type.
        // -----------------------------------------------------------------------
        binding.setInjectionClassType(method); // F743-32637

        // Finally, save the reflect object for use at runtime to inject
        ivMethod = method;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, getClass().getSimpleName() + ".<init> : " + ivMethod);
    }

    /**
     * Returns the method specified in this Injection Target
     */
    @Override
    public Member getMember()
    {
        return ivMethod;
    }

    /**
     * Returns an array of <code>Class</code> objects that represent the formal
     * parameter type of the method.
     */
    @Override
    public Class<?> getInjectionClassType()
    {
        return ivMethod.getParameterTypes()[0];
    }

    /**
     * Returns the Type for the field or method.
     */
    // d662220
    @Override
    public Type getGenericType()
    {
        return ivMethod.getGenericParameterTypes()[0];
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
    protected void injectMember(Object objectToInject, Object dependentObject)
                    throws Exception
    {
        try {
            ivMethod.invoke(objectToInject, dependentObject);
        } finally {
            ivInjectionBinding.cleanAfterMethodInvocation();
        }
    }
}
