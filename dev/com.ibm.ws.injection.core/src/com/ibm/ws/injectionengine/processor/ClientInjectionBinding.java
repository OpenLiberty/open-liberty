/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;

import javax.naming.Context;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.injectionengine.ClientInjection;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessorContextImpl;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

/**
 * Helper for resolving injection targets in the client process for federated
 * client modules.
 */
public class ClientInjectionBinding
                extends InjectionBinding<Annotation>
{
    private static final TraceComponent tc = Tr.register(InjectionBinding.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    /**
     * Acquires an InjectionTarget for the specified ClientInjection.
     *
     * @param compNSConfig the minimal namespace configuration
     * @param injection the injection target descriptor
     * @return the injection target
     * @throws InjectionException if the target cannot be acquired
     */
    public static InjectionTarget getInjectionTarget(ComponentNameSpaceConfiguration compNSConfig, ClientInjection injection)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInjectionTarget: " + injection);

        // Create a temporary InjectionBinding in order to resolve the target.
        ClientInjectionBinding binding = new ClientInjectionBinding(compNSConfig, injection);
        Class<?> injectionType = binding.loadClass(injection.getInjectionTypeName());
        String targetName = injection.getTargetName();
        String targetClassName = injection.getTargetClassName();

        // Add a single injection target and then retrieve it.
        binding.addInjectionTarget(injectionType, targetName, targetClassName);
        InjectionTarget target = InjectionProcessorContextImpl.getInjectionTargets(binding).get(0);
        binding.metadataProcessingComplete(); // d681767

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInjectionTarget: " + target);
        return target;
    }

    private final Context ivJavaColonContext; // d681767

    private ClientInjectionBinding(ComponentNameSpaceConfiguration compNSConfig, ClientInjection injection)
    {
        super(null, compNSConfig);
        setJndiName(injection.getRefName());
        ivJavaColonContext = compNSConfig.getJavaColonContext(); // d681767
    }

    @Override
    public String toString()
    {
        return Util.identity(this) + '[' + getJndiName() + ']';
    }

    @Override
    public void merge(Annotation annotation, Class<?> instanceClass, Member member)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object getInjectionObjectInstance(Object targetObject, // F48603.4
                                                InjectionTargetContext targetContext) // F49213.1
    throws Exception
    {
        return ivJavaColonContext.lookup(getJndiName()); // d681767
    }
}
