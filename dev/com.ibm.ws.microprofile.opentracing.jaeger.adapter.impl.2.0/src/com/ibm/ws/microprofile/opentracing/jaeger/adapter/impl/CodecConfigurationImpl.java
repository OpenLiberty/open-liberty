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

package com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl;

import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.CodecConfiguration;
import com.ibm.ws.microprofile.opentracing.jaeger.adapter.Configuration.Propagation;

public class CodecConfigurationImpl extends AbstractJaegerAdapter<io.jaegertracing.Configuration.CodecConfiguration> implements CodecConfiguration {

    public CodecConfigurationImpl() {
        super(new io.jaegertracing.Configuration.CodecConfiguration());
    }
    
    @Override
    public CodecConfiguration withPropagation(Propagation propagation) {
        getDelegate().withPropagation(convert(propagation));
        return this;
    }

    private io.jaegertracing.Configuration.Propagation convert(Propagation propagation) {
        switch (propagation) {
            case JAEGER:
                return io.jaegertracing.Configuration.Propagation.JAEGER;
            case B3:
                return io.jaegertracing.Configuration.Propagation.B3;
            default:
                return null;
        }
    }
}
