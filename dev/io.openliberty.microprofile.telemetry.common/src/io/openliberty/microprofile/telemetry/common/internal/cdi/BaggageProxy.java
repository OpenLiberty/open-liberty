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
package io.openliberty.microprofile.telemetry.common.internal.cdi;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This proxy class redirects method calls to Baggage.current(), by doing so it allows people to use @Inject Baggage and get an object which will not become obsolete.
 */
public class BaggageProxy implements Baggage {

    private static final TraceComponent tc = Tr.register(BaggageProxy.class);

    @Override
    public int size() {
        try {
            return Baggage.current().size();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return 0;
        }
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super BaggageEntry> consumer) {
        try {
            Baggage.current().forEach(consumer);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
        }
    }

    @Override
    public Map<String, BaggageEntry> asMap() {
        try {
            return Baggage.current().asMap();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return Collections.emptyMap();
        }
    }

    @Override
    public String getEntryValue(String entryKey) {
        try {
            return Baggage.current().getEntryValue(entryKey);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return "";
        }
    }

    @Override
    public BaggageBuilder toBuilder() {
        try {
            return Baggage.current().toBuilder();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return Baggage.builder();
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        try {
            return Baggage.current().equals(obj);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return false;
        }
    }

    public int hashCode() {
        try {
            return Baggage.current().hashCode();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return -1;
        }
    }

    @Override
    public String toString() {
        try {
            return Baggage.current().toString();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return "";
        }
    }
}
