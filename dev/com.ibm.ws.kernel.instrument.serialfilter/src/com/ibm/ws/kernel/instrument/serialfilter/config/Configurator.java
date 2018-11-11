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

import java.util.Map;
import java.util.logging.Logger;

/**
 * A configurator for transmitting configuration from one class loader context,
 * via a {@link Map} to a reciving implementation in another class loader context.
 *
 * @param <T> The eventual intended target of the configuration.
 */

final class Configurator<T> {
    private final Map<Object, Object> configTarget;

    @SuppressWarnings("unchecked")
    public Configurator(Map<?, ?> configTarget) {
        this.configTarget = (Map<Object, Object>)configTarget;
    }

    public <I, O, C extends Enum<C> & ConfigurationSetting<T, I, O>> O send(C option, I param) {
        Object result = configTarget.put(stringify(option), param);
        final Class<O> outputType = option.getOutputType();
        try {
            return option.getOutputType().cast(result);
        } catch (ClassCastException e) {
            throw new AssertionError("Config option to map should have returned object of type "+ outputType + " but returned object of type " + result.getClass());
        }
    }

    private static String stringify(Enum<?> e) {
        return e.getClass().getName() + '#' + e.name();
    }

    public static<E extends Enum<E>> E destringify(String s) {
        // no nls enablement is required since the errors which are reported here are all internal error.
        String[] parts = s.split("#", 2);
        if (parts.length == 2) {
            String classname = parts[0];
            String memberName = parts[1];
            try {
                @SuppressWarnings("unchecked")
                Class<E> enumClass = (Class<E>)Class.forName(classname);
                return Enum.valueOf(enumClass, memberName);
            } catch (ClassNotFoundException e) {
                Logger.getLogger(Configurator.class.getName()).severe("Could not find configuration option class: " + classname);
            } catch (IllegalArgumentException e) {
                Logger.getLogger(Configurator.class.getName()).severe("Could not locate enum member: " + classname + "." + memberName);
            }
        } else {
            Logger.getLogger(Configurator.class.getName()).severe("Could not parse config key:" + s);
        }
        return null;
    }
}
