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

import java.lang.reflect.Member;

import javax.inject.Inject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.interfaces.CDIRuntime;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessor;

public class InjectInjectionProcessor extends InjectionSimpleProcessor<Inject> {

    private static final TraceComponent tc = Tr.register(InjectInjectionProcessor.class);
    private final CDIRuntime cdiRuntime;

    public InjectInjectionProcessor(CDIRuntime cdiRuntime) {
        super(Inject.class);
        this.cdiRuntime = cdiRuntime;
    }

    @Override
    public InjectionBinding<Inject> createInjectionBinding(Inject annotation, Class<?> instanceClass, Member member) throws InjectionException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "createInjectionBinding", new Object[] { annotation, instanceClass, member, this });

        InjectInjectionBinding iBinding = new InjectInjectionBinding(annotation, ivNameSpaceConfig, cdiRuntime);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "createInjectionBinding", iBinding);

        return iBinding;

    }

    @Override
    protected boolean isNonJavaBeansPropertyMethodAllowed() {
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":" + hashCode();
    }
}
