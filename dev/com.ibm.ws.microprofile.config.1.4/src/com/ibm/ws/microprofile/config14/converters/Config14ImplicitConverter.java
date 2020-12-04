/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.converters;

import java.util.function.Function;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config12.converters.ConstructorFunction;
import com.ibm.ws.microprofile.config12.converters.MethodFunction;
import com.ibm.ws.microprofile.config13.converters.Config13ImplicitConverter;

/**
 *
 */
public class Config14ImplicitConverter extends Config13ImplicitConverter {

    private static final TraceComponent tc = Tr.register(Config14ImplicitConverter.class);

    @Trivial
    public Config14ImplicitConverter(Class<?> converterType) {
        super(converterType);
    }

    /**
     * <p>If no explicit Converter and no built-in Converter could be found for a certain type,
     * the {@code Config} provides an <em>Implicit Converter</em>, if</p>
     * <ul>
     * <li>the target type {@code T} has a {@code public static T of(String)} method, or</li>
     * <li>the target type {@code T} has a {@code public static T valueOf(String)} method, or</li>
     * <li>the target type {@code T} has a {@code public static T parse(CharSequence)} method</li>
     * <li>The target type {@code T} has a public Constructor with a String parameter, or</li>
     * </ul>
     *
     *
     * @param converterType The class to convert using
     */
    @Override
    @Trivial
    protected <X> Function<String, X> getImplicitFunction(Class<X> converterType) {
        Function<String, X> implicitFunction = null;

        implicitFunction = MethodFunction.getOfMethod(converterType);
        if (implicitFunction != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Automatic converter for " + converterType + " using \"of\"");
        }

        if (implicitFunction == null) {
            implicitFunction = MethodFunction.getValueOfFunction(converterType);
            if (implicitFunction != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Automatic converter for " + converterType + " using \"valueOf\"");
            }

        }
        if (implicitFunction == null) {
            implicitFunction = MethodFunction.getParseFunction(converterType);
            if (implicitFunction != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Automatic converter for " + converterType + " using \"parse\"");
            }
        }

        if (implicitFunction == null) {
            implicitFunction = ConstructorFunction.getConstructorFunction(converterType);
            if (implicitFunction != null && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Automatic converter for " + converterType + " using \"constructor\"");
            }
        }

        if (implicitFunction == null) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "implicit.string.constructor.method.not.found.CWMCG0017E", converterType));
        }

        return implicitFunction;
    }
}
