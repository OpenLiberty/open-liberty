/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.config;

import com.ibm.ws.kernel.instrument.serialfilter.util.Functor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A functor that can accept configuration parameters. To allow invocation from
 * other classloader contexts with alternate versions of these classes, the config
 * option is passed as an Integer corresponding to the ordinal value of an enum
 * representing the option. Input parameters and return values are explicitly cast
 * to the appropriate types around each call. If a suitable enum is provided, and
 * the implementation does not rely on parameter or return types that differ
 * between caller and target then this class should work seamlessly with the
 * companion {@link Configurator} class.
 *
 * @param <T> The underlying configuration target for this functor.
 * @param <R> The range of the function, i.e. the input type.
 * @param <D> The domain of the function, i.e. the output type.
 */
/*
 * Note: these classes make extensive use of generics solely in order to make the
 * compiler perform as much type checking as possible. Do not modify the use of
 * generics without a solid grounding in generics, including for example being
 * able to explain why Class and Enum are declared the way they are.
 */
public abstract class ConfigurableFunctor<T, R, D> extends Functor<R, D> {
    private final Logger log;
    private final T target;

    ConfigurableFunctor(T target) {
        this.target = target;
        log = Logger.getLogger(ConfigurableFunctor.class.getName());
    }

    @SuppressWarnings("unchecked")
    public final D put(final R key, final D value) {
        // Actually we don't care about R or D here.
        // Thanks to Java's type erasure, nor does the JVM.
        // We can cast parameters and return values with impunity.
        //if (!!!(key instanceof String)) return handleUnknownConfig("Config key should be transmitted as a string", key, value);

        // try to recreate the config key parameter
        String str = (String) key;
        Object o = Configurator.destringify(str);
        if (o == null) return handleUnknownConfig("Config key string could not be parsed", key, value);

        // check it is a ConfigurationSetting
        if (!!!(o instanceof ConfigurationSetting)) return handleUnknownConfig("Config key parsed as an unexpected type", o, value);
        @SuppressWarnings("rawtypes")
		ConfigurationSetting<T, ?, ?> setting = (ConfigurationSetting) o;

        // check it is one of the expected types for this ConfigurableFunctor
        if (!!!(isExpectedType(setting.getClass()))) return handleUnknownConfig("Config key is not an expected type for this target", setting, value);

        return apply(setting, value);
    }

    @SuppressWarnings("SameReturnValue")
    private D handleUnknownConfig(String msg, Object key, Object value) {
        // no translation since the errors which report here are all internal error which should not happen, and there is nothing the customer can do.
        if (!!!log.isLoggable(Level.SEVERE)) return null;
        final String keyMsg = key == null ? "key = null" : ("key = '" + key + "' of type " + key.getClass().getName());
        final String valMsg = value == null ? "value = null" : ("value = '" + value + "' of type " + value.getClass().getName());
        log.severe(msg + ": " + keyMsg + ", " + valMsg);
        return null;
    }

    protected abstract boolean isExpectedType(@SuppressWarnings("rawtypes") Class<? extends ConfigurationSetting> c);

    @SuppressWarnings("unchecked")
	private <I> D apply(ConfigurationSetting<T, I, ?> setting, D param) {
        try {
            I input = setting.getInputType().cast(param);
            return (D) setting.apply(target, input);
        } catch (ClassCastException e) {
            handleUnknownConfig("Config value does not match expected type for config key", setting, param);
            return null;
        }
    }
}

