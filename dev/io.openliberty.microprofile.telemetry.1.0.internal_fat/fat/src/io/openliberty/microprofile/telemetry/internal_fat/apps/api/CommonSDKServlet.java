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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.common.InstrumentationScopeInfoBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/common")
@ApplicationScoped // Make this a bean so that there's one bean in the archive, otherwise CDI gets disabled and @Inject doesn't work
public class CommonSDKServlet extends FATServlet {

    /**
     * Very simple test that we can use Clock.nanoTime()
     * {@link Clock}
     *
     * @throws InterruptedException
     */
    @Test
    public void testClockNanoTime() throws InterruptedException {
        Clock clock = Clock.getDefault();
        //get the clock start time
        long startNano = clock.nanoTime();
        //sleep for a known period
        Duration sleepDuration = Duration.ofSeconds(1);
        Thread.sleep(sleepDuration.toMillis());
        //get the clock end time
        long endNano = clock.nanoTime();
        //work out the duration according to the clock
        long clockDurationNano = endNano - startNano;
        Duration clockDuration = Duration.ofNanos(clockDurationNano);

        //the duration according to the clock should be at least as long as the time we slept for (we might have slept longer)
        assertTrue(clockDuration.compareTo(sleepDuration) >= 0);

        //make sure that the clockDuration isn't vastly bigger than the intended sleep duration
        Duration maxSleepDuration = sleepDuration.plus(Duration.ofSeconds(5));
        assertTrue(clockDuration.compareTo(maxSleepDuration) < 0);
    }

    /**
     * Very simple test that we can use Clock.now()
     * {@link Clock}
     *
     * @throws InterruptedException
     */
    @Test
    public void testClockNow() throws InterruptedException {
        Clock clock = Clock.getDefault();
        long clockNow = clock.now(); //nanos
        long systemNow = System.currentTimeMillis(); //millis

        Instant clockInstant = Instant.ofEpochMilli(Duration.ofNanos(clockNow).toMillis());
        Instant systemInstant = Instant.ofEpochMilli(systemNow);
        Instant systemMinusOne = systemInstant.minus(1, ChronoUnit.HOURS);
        Instant systemPlusOne = systemInstant.plus(1, ChronoUnit.HOURS);

        System.out.println("Clock: " + clockInstant + " System: " + systemInstant);
        //check that the clock instant is roughly the same as the system instant, plus or minus one hour
        assertTrue("Clock: " + clockInstant + " System: " + systemInstant, clockInstant.isAfter(systemMinusOne));
        assertTrue("Clock: " + clockInstant + " System: " + systemInstant, clockInstant.isBefore(systemPlusOne));
    }

    /**
     * Very simple test that we can use CompletableResultCode
     * {@link CompletableResultCode}
     *
     */
    @Test
    public void testCompletableResultCode() {
        CompletableResultCode success = CompletableResultCode.ofSuccess();
        assertTrue(success.isSuccess());
        CompletableResultCode failure = CompletableResultCode.ofFailure();
        assertFalse(failure.isSuccess());
    }

    /**
     * Very simple test that we can use InstrumentationLibraryInfo. Note that this class is deprecated.
     * {@link InstrumentationLibraryInfo}
     *
     */
    @Test
    public void testInstrumentationLibraryInfo() {
        InstrumentationLibraryInfo info = InstrumentationLibraryInfo.create("myName", "myVersion", "mySchemaUrl");
        assertEquals("myName", info.getName());
        assertEquals("myVersion", info.getVersion());
        assertEquals("mySchemaUrl", info.getSchemaUrl());
    }

    /**
     * Very simple test that we can use InstrumentationScopeInfo.
     * {@link InstrumentationScopeInfo}
     * {@link InstrumentationScopeInfoBuilder}
     *
     */
    @Test
    public void testInstrumentationScopeInfo() {
        InstrumentationScopeInfoBuilder builder = InstrumentationScopeInfo.builder("myName");
        builder.setVersion("myVersion");
        builder.setSchemaUrl("mySchemaUrl");
        AttributeKey<String> key = AttributeKey.stringKey("myAttrName");
        builder.setAttributes(Attributes.of(key, "myAttrValue"));

        InstrumentationScopeInfo info = builder.build();
        assertEquals("myName", info.getName());
        assertEquals("myVersion", info.getVersion());
        assertEquals("mySchemaUrl", info.getSchemaUrl());
        Attributes attrs = info.getAttributes();
        String value = attrs.get(key);
        assertEquals("myAttrValue", value);
    }

    /**
     * Very simple test that we can use Resource.
     * {@link Resource}
     * {@link ResourceBuilder}
     *
     */
    @Test
    public void testResource() {
        ResourceBuilder builder = Resource.builder();
        AttributeKey<String> key = AttributeKey.stringKey("myKey");
        builder.put(key, "myValue");
        Resource resource = builder.build();
        String value = resource.getAttribute(key);
        assertEquals("myValue", value);
    }
}
