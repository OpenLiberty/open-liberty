/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.OpenAPIVersionConfigImpl.OpenAPIVersion;

public class TestOpenAPIVersion {

    @Test
    public void testFullParse() {
        Optional<OpenAPIVersion> versionOptional = OpenAPIVersion.parse("3.0.0");
        assertThat(versionOptional, hasProperty("present", equalTo(true)));

        OpenAPIVersion version = versionOptional.get();
        assertEquals(3, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(0, version.getPatch());
        assertEquals("3.0.0", version.toString());
    }

    @Test
    public void testPartialParse() {
        Optional<OpenAPIVersion> versionOptional = OpenAPIVersion.parse("3.0");
        assertThat(versionOptional, hasProperty("present", equalTo(true)));

        OpenAPIVersion version = versionOptional.get();
        assertEquals(3, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(-1, version.getPatch());
        assertEquals("3.0", version.toString());
    }

    @Test
    public void testLeadingZeroes() {
        Optional<OpenAPIVersion> versionOptional = OpenAPIVersion.parse("015.020.004");
        assertThat(versionOptional, hasProperty("present", equalTo(true)));

        OpenAPIVersion version = versionOptional.get();
        assertEquals(15, version.getMajor());
        assertEquals(20, version.getMinor());
        assertEquals(4, version.getPatch());
        assertEquals("15.20.4", version.toString());
    }

    @Test
    public void testInvalidParse() {
        assertInvalidVersion("a");
        assertInvalidVersion("1.a");
        assertInvalidVersion("1.3.a");
        assertInvalidVersion("99999999999999.5.4"); // Integer overflow
        assertInvalidVersion("1..2");
        assertInvalidVersion(".1");
        assertInvalidVersion("1.");
        assertInvalidVersion("1.2.3.");
        assertInvalidVersion("1.2.3e1");
        assertInvalidVersion("-1.2.3");
        assertInvalidVersion("1.-2.3");
        assertInvalidVersion("1.2.-3");
        assertInvalidVersion("1.2.3.4");
    }

    private void assertInvalidVersion(String version) {
        assertThat(version, OpenAPIVersion.parse(version), hasProperty("present", equalTo(false)));
    }

}
