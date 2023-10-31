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
package io.openliberty.microprofile.reactive.messaging.fat.validation;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.reactive.messaging.fat.suite.ReactiveMessagingActions;

@RunWith(FATRunner.class)
@AllowedFFDC
public class ValidationTests {

    private static final String SERVER_NAME = "SimpleRxMessagingServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = ReactiveMessagingActions.repeat(SERVER_NAME,
                                                                  ReactiveMessagingActions.MP61_RM30,
                                                                  ReactiveMessagingActions.MP50_RM30,
                                                                  ReactiveMessagingActions.MP60_RM30);

    @BeforeClass
    public static void setup() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testIncomingMethodWithoutUpstream() throws Exception {
        AppValidator.validateAppOn(server)
                        .withClass(IncomingMethodWithoutUpstream.class)
                        .failsWith("Wiring error\\(s\\) detected in application")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.OpenGraphException: Some components are not connected to either downstream consumers or upstream producers:
//        - SubscriberMethod{method:'io.openliberty.microprofile.reactive.messaging.fat.validation.IncomingMethodWithoutUpstream#badMethod', incoming:'IncomingMethodWithoutUpstream'} has no upstream
    }

    @Test
    public void testOutgoingMethodWithoutDownstream() throws Exception {
        AppValidator.validateAppOn(server)
                        .withClass(OutgoingMethodWithoutDownstream.class)
                        .failsWith("Wiring error\\(s\\) detected in application")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.OpenGraphException: Some components are not connected to either downstream consumers or upstream producers:
//        - SubscriberMethod{method:'io.openliberty.microprofile.reactive.messaging.fat.validation.OutgoingMethodWithoutDownstream#badMethod', incoming:'OutgoingMethodWithoutDownstream'} has no downstream
    }

    @Test
    public void testIncomingMethodWithMultipleUpstreams() throws Exception {
        AppValidator.validateAppOn(server)
                        .withClass(IncomingMethodWithMultipleUpstreams.class)
                        .failsWith("Wiring error\\(s\\) detected in application")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.TooManyUpstreamCandidatesException:
//        'SubscriberMethod{method:'io.openliberty.microprofile.reactive.messaging.fat.validation.IncomingMethodWithMultipleUpstreams#badMethod',
//        incoming:'IncomingMethodWithMultipleUpstreams'}' supports a single upstream producer, but found 2 ...
    }

    @Test
    public void testOutgoingMethodWithMultipleDownstreams() throws Exception {
        AppValidator.validateAppOn(server)
                        .withClass(OutgoingMethodWithMultipleDownstreams.class)
                        .failsWith("Wiring error\\(s\\) detected in application")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.TooManyDownstreamCandidatesException:
//        'PublisherMethod{method:'io.openliberty.microprofile.reactive.messaging.fat.validation.OutgoingMethodWithMultipleDownstreams#outgoingMethod', outgoing:'OutgoingMethodWithMultipleDownstreams'}'
//        supports a single downstream consumer, but found 2
    }

    @Test
    public void testEmitterWithNoDownstream() throws Exception {
        AppValidator.validateAppOn(server)
                        .withClass(EmitterWithNoDownstream.class)
                        .failsWith("Unsatisfied dependencies for type Emitter with qualifiers @Default")
                        .run();
//      org.jboss.weld.exceptions.DeploymentException: WELD-001408: Unsatisfied dependencies for type Emitter with qualifiers @Default
    }

    @Test
    public void testEmitterWithMultipleDownstreams() throws Exception {
        AppValidator.validateAppOn(server)
                        .withClass(EmitterWithMultipleDownstreams.class)
                        .failsWith("Wiring error\\(s\\) detected in application")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.TooManyDownstreamCandidatesException: 'Emitter{channel:'EmitterWithMultipleDownstreams'}'
//        supports a single downstream consumer, but found 2
    }

    @Test
    public void testInjectedChannelWithNoUpstream() throws Exception {
        AppValidator.validateAppOn(server)
                        .withClass(InjectedChannelWithNoUpstream.class)
                        .failsWith("Wiring error\\(s\\) detected in application")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.OpenGraphException: Some components are not connected to either downstream consumers or upstream producers:
//        - @Channel{channel:'InjectedChannelWithNoUpstream'} has no upstream
    }

    @Test
    public void testInjectedChannelWithMultipleUpstreams() throws Exception {
        AppValidator.validateAppOn(server)
                        .withClass(InjectedChannelWithMultipleUpstreams.class)
                        .failsWith("Wiring error\\(s\\) detected in application")
                        .run();
//        Suppressed: io.smallrye.reactive.messaging.providers.wiring.TooManyUpstreamCandidatesException: '@Channel{channel:'InjectedChannelWithMultipleUpstreams'}' supports a single upstream producer, but found 2
    }
}