/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntryMetadata;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/testBaggage")
@ApplicationScoped // Make this a bean so that there's one bean in the archive, otherwise CDI gets disabled and @Inject doesn't work
public class BaggageAPIServlet extends FATServlet {

    /**
     * Very simple test that we can use a BaggageBuilder
     * {@link BaggageBuilder}
     * {@link Baggage}
     * {@link BaggageEntryMetadata}
     */
    @Test
    public void testBaggageBuilder() {
        BaggageBuilder builder = Baggage.builder();
        BaggageEntryMetadata metadata1 = BaggageEntryMetadata.create("myMetaData1");
        builder.put("myKey1", "myValue1", metadata1);
        BaggageEntryMetadata metadata2 = BaggageEntryMetadata.create("myMetaData2");
        builder.put("myKey2", "myValue2", metadata2);
        BaggageEntryMetadata metadata3 = BaggageEntryMetadata.create("myMetaData3");
        builder.put("myKey3", "myValue3", metadata3);
        Baggage baggage = builder.build();

        assertEquals(3, baggage.size());

        String value = baggage.getEntryValue("myKey2");
        assertEquals("myValue2", value);

        BaggageEntryMetadata metadataValue = baggage.asMap().get("myKey2").getMetadata();
        assertEquals("myMetaData2", metadataValue.getValue());
    }

    /**
     * Very simple test that we can use a W3CBaggagePropagator
     * {@link W3CBaggagePropagator}
     */
    @Test
    public void testW3CBaggagePropagator() {
        W3CBaggagePropagator propagator = W3CBaggagePropagator.getInstance();
        Collection<String> fields = propagator.fields();
        assertTrue(fields.contains("baggage"));
    }

}
