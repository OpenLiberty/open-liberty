/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs.utils.test;

import org.junit.Assert;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.junit.Test;

/**
 * This test makes sure that the updates that were done for Rfc3986UriValidator work correctly for what is tested in CXF.
 * The tests are based off of the tests done in LinkBuilderImplTest and URiBuilderImplTest in the CXF repo.
 */
public class InvalidUriTest {

    @Test
    public void testLinkBuilderNoHost() {
        try {
            Link.fromUri("http://@").build();
            Assert.fail("Expected a UriBuilderException");
        } catch (UriBuilderException e) {
            // expected
        }
        try {
            Link.fromUri("http://:@").build();
            Assert.fail("Expected a UriBuilderException");
        } catch (UriBuilderException e) {
            // expected
        }
        try {
            Link.fromUri("HTTP://:@").build();
            Assert.fail("Expected a UriBuilderException");
        } catch (UriBuilderException e) {
            // expected
        }
    }

    @Test
    public void testUriBuilderNoHost() {
        try {
            UriBuilder.fromUri("http://@").build();
            Assert.fail("Expected a UriBuilderException");
        } catch (UriBuilderException e) {
            // expected
        }
        try {
            UriBuilder.fromUri("http://:@").build();
            Assert.fail("Expected a UriBuilderException");
        } catch (UriBuilderException e) {
            // expected
        }
        try {
            UriBuilder.fromUri("HTTP://:@").build();
            Assert.fail("Expected a UriBuilderException");
        } catch (UriBuilderException e) {
            // expected
        }
    }
}
