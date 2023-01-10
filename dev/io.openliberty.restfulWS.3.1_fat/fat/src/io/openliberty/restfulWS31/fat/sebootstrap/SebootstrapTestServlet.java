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
package io.openliberty.restfulWS31.fat.sebootstrap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CompletionStage;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.core.Application;



@WebServlet(urlPatterns = "/SebootstrapTestServlet")
public class SebootstrapTestServlet extends FATServlet {
    private static final long serialVersionUID = -5621884169063102743L;

    /**
     * Verifies that attempting to use the Java SE Bootstrap API in Liberty will result in an 
     * UnsupportedOperationException.
     */
    @Test
    public final void ensureSeBootstrapNotSupported() {
        boolean exceptionCaught = false;
        try {
            // given
            final Application application = new SebootstrapResource();
            final SeBootstrap.Configuration.Builder bootstrapConfigurationBuilder = SeBootstrap.Configuration.builder();
            final SeBootstrap.Configuration requestedConfiguration = bootstrapConfigurationBuilder
                    .property(SeBootstrap.Configuration.PROTOCOL, "HTTP")
                    .property(SeBootstrap.Configuration.HOST, "localhost")
                    .property(SeBootstrap.Configuration.PORT, Integer.getInteger("bvt.prop.HTTP_default"))
                    .property(SeBootstrap.Configuration.ROOT_PATH, "/root/path").build();

            // when
            final CompletionStage<SeBootstrap.Instance> completionStage = SeBootstrap.start(application,
                    requestedConfiguration);
            final SeBootstrap.Instance instance = completionStage.toCompletableFuture().get();
            final SeBootstrap.Configuration actualConfiguration = instance.configuration();
            
        } catch (UnsupportedOperationException e) {
            exceptionCaught = true;
            assertTrue("Received exception message is: " + e.getMessage() + " and the expected message is: Liberty does not support the optional Jakarta Rest SE Bootstrap API.",
                       e.getMessage().contains("Liberty does not support the optional Jakarta Rest Java SE Bootstrap API."));
        } catch (Throwable t) {
            fail("Expected UnsupportedOperationException but caught: " + t);
        }
        if (!exceptionCaught) {
            fail("Expected exception not caught");
        }
    }
    
}