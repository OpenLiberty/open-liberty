/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.injection;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

import javax.ws.rs.CookieParam;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.injection.metadata.ParamInjectionMetadata;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;

public class CookieParamInjectionBinding extends AbstractParamInjectionBinding<CookieParam> {

    private static final TraceComponent tc = Tr.register(CookieParamInjectionBinding.class);

    public CookieParamInjectionBinding(CookieParam annotation,
                                       ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(annotation, nameSpaceConfig);
    }

    @Override
    protected Object getJAXRSValueFromContext(CookieParam annotation, Class<?> classType, Type genericType, Annotation[] memberAnnotations, Member member,
                                              ParamInjectionMetadata paramInjectionMetadata) throws IOException {

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "getJAXRSValueFromContext", new Object[] { annotation, classType,
                                                                   genericType, memberAnnotations, member, paramInjectionMetadata });
        }

        Object value = getInjectedObjectFromCXF(classType, genericType, memberAnnotations, paramInjectionMetadata);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "getJAXRSValueFromContext", new Object[] { value });
        }

        return value;
    }
}
