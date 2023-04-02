/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.cdi;

import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntry;

/**
 * This proxy class redirects method calls to Baggage.current(), by doing so it allows people to use @Inject Baggage and get an object which will not become obsolete.
 */
public class BaggageProxy implements Baggage {

    @Override
    public int size() {
        return Baggage.current().size();
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super BaggageEntry> consumer) {
        Baggage.current().forEach(consumer);
    }

    @Override
    public Map<String, BaggageEntry> asMap() {
        return Baggage.current().asMap();
    }

    @Override
    public String getEntryValue(String entryKey) {
        return Baggage.current().getEntryValue(entryKey);
    }

    @Override
    public BaggageBuilder toBuilder() {
        return Baggage.current().toBuilder();
    }
    
    @Override
    public boolean equals(Object obj) {
        return Baggage.current().equals(obj);
    }

    public int hashCode() {
        return Baggage.current().hashCode();
    }

    @Override
    public String toString() {
        return Baggage.current().toString();
    }
}
