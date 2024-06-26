/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.userfeature;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.trace.ReadableSpan;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet("/testSpanCurrent")
public class UserFeatureServlet extends FATServlet {

    //Copied from UserFeatureServletFilter to avoid creating a compile dependency
    //between an app and a user feature
    private static final AttributeKey<String> SPAN_ATTRIBUTE_KEY = AttributeKey.stringKey("FromUserFeature");
    private static final String SPAN_ATTRIBUTE_VALUE = "True";

    //This tests that a span that was set in a user feature can be seen inside an
    //application.
    @Test
    public void testSpanPropagatedFromUserFature() {

        ReadableSpan span = (ReadableSpan) Span.current();
        span.getSpanContext().getSpanId();
        String value = span.getAttribute(SPAN_ATTRIBUTE_KEY);

        Assert.assertEquals("We did not find Attribute: FromUserFeature with value: " + SPAN_ATTRIBUTE_VALUE
                            + " in span " + span.toString(), SPAN_ATTRIBUTE_VALUE, value);

    }

}
