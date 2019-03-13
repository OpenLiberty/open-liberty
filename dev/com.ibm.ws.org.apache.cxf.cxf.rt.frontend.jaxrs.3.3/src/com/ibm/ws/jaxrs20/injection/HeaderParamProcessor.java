/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxrs20.injection;

import java.lang.reflect.Member;

import javax.ws.rs.HeaderParam;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessor;

public class HeaderParamProcessor extends InjectionSimpleProcessor<HeaderParam> {

    private static final TraceComponent tc = Tr.register(HeaderParamProcessor.class);

    public HeaderParamProcessor() {
        super(HeaderParam.class);
    }

    @Override
    public InjectionBinding<HeaderParam> createInjectionBinding(HeaderParam annotation, Class<?> instanceClass, Member member)
                    throws InjectionException {
        final String methodName = "createInjectionBinding";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { annotation, instanceClass, member });
        }

        HeaderParamInjectionBinding binding =
                        new HeaderParamInjectionBinding(annotation, ivNameSpaceConfig);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, binding);
        }
        return binding;
    }

}
