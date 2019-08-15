/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.component;

import java.lang.reflect.AnnotatedType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.OutputConverter;

/**
 * OutputConverter to ensure that DataFetcherResult can be returned instead of the
 * expected return type object.
 */
public class DataFetcherResultOutputConverter implements OutputConverter<Object, Object> {
    private final static TraceComponent tc = Tr.register(DataFetcherResultOutputConverter.class);

    @Override
    public Object convertOutput(Object original, AnnotatedType type, ResolutionEnvironment resolutionEnvironment) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "convertOutput: " + original);
        }
        return original;
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }
}
