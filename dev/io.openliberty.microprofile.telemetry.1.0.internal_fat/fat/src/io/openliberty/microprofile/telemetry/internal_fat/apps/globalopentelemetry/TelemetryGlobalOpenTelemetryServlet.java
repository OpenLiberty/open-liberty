/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package io.openliberty.microprofile.telemetry.internal_fat.apps.globalopentelemetry;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

@WebServlet("/GlobalOpenTelemetryServlet")
@ApplicationScoped
public class TelemetryGlobalOpenTelemetryServlet extends FATServlet {

    @Inject
    OpenTelemetry openTelemetry;

    //The GlobalOpenTelemetry object can only be set once
    @Test
    public void testSetGlobalOpenTelemetry() {
        try {
            GlobalOpenTelemetry.set(openTelemetry);
            fail("Able to set GlobalOpenTelemetry");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(),
                       containsString("CWMOT5001E: Setting the GlobalOpenTelemetry object is not supported."));
        }
    }

    @Test
    public void testGetGlobalOpenTelemetry() {
        OpenTelemetry global = GlobalOpenTelemetry.get();
    }
}
