/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.interfaces;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

public interface WebSphereConfig14 extends WebSphereConfig, Consumer<Set<String>> {

    /**
     * @param key
     * @param type
     * @param defaultString
     * @param evaluateVariables
     * @param converter
     * @return
     */
    SourcedValue getSourcedValue(List<String> keys, Type type, Class<?> genericSubType, Object defaultValue, String defaultString, boolean evaluateVariables,
                                 Converter<?> converter);

    void registerPropertyChangeListener(PropertyChangeListener listener, String propertyName);

    @FunctionalInterface
    public static interface PropertyChangeListener {
        public void onPropertyChanged();
    }
}
