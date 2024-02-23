/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.tests;

import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasEventLog;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasNoParent;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.hasParentSpanId;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerSpanMatcher.isSpan;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;
import io.jaegertracing.api_v2.Model.Span;
import io.jaegertracing.api_v2.Model.SpanRef;
import io.jaegertracing.api_v2.Model.SpanRefType;
import io.openliberty.microprofile.telemetry.internal.apps.spanTest.TestResource;
import io.openliberty.microprofile.telemetry.internal.utils.jaeger.JaegerQueryClient;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * Base set of tests for exporting spans to Jaeger over any protocol
 * <p>
 * Subclasses must set up the server, deploy the spanTest application and implement {@link #getJaegerClient()} to return a client for the Jaeger server
 */
public abstract class JaegerBaseTest {

    private static final Class<?> c = JaegerBaseTest.class;
    protected static final String SERVER_NAME = "spanTestServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public JaegerQueryClient client = getJaegerClient();

    /**
     * Get a valid client for the Jaeger server
     * <p>
     * This method is called for each test and subclasses may return the same client or different classes for each test.
     * <p>
     * The subclass is responsible for closing the client at the end of the test or the class.
     *
     * @return a client for the Jaeger server
     */
    protected abstract JaegerQueryClient getJaegerClient();

    @Test
    public void testBasic() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest");
        String traceId = request.run(String.class);
        Log.info(c, "testBasic", "TraceId is " + traceId);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(1));
        Log.info(c, "testBasic", "Spans returned: " + spans);

        Span span = spans.get(0);

        assertThat(span, isSpan().withTraceId(traceId)
                                 .withAttribute(SemanticAttributes.HTTP_ROUTE, "/spanTest/")
                                 .withAttribute(SemanticAttributes.HTTP_METHOD, "GET"));

        // This is mostly just to check that getSpansForServiceName works for TracingNotEnabledTest
        List<Span> allSpans = client.getSpansForServiceName("Test service");
        assertThat(allSpans, hasItem(span));
    }

    @Test
    public void testEventAdded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/eventAdded");
        String traceId = request.run(String.class);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(1));

        Span span = spans.get(0);

        assertThat(span, hasEventLog(TestResource.TEST_EVENT_NAME));
        assertThat(span.getLogs(0).hasTimestamp(), is(true));
    }

    @Test
    public void testSubspan() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/subspan");
        String traceId = request.run(String.class);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(2));

        Span parent, child;
        if (hasParent(spans.get(0))) {
            child = spans.get(0);
            parent = spans.get(1);
        } else {
            child = spans.get(1);
            parent = spans.get(0);
        }

        assertThat(parent, hasNoParent());
        assertThat(child, hasParentSpanId(parent.getSpanId()));

        if (RepeatTestFilter.isRepeatActionActive(MicroProfileActions.MP60_ID)) {
            assertThat(parent, hasProperty("operationName", equalTo("/spanTest/subspan")));
        } else {
            assertThat(parent, hasProperty("operationName", equalTo("GET /spanTest/subspan")));
        }

        assertThat(child, hasProperty("operationName", equalTo(TestResource.TEST_OPERATION_NAME)));
    }

    @Test
    public void testExceptionRecorded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/exception");
        String traceId = request.run(String.class);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(1));

        Span span = spans.get(0);

        assertThat(span, isSpan().withStatus(StatusCode.ERROR)
                                 .withExceptionLog(RuntimeException.class));
        assertThat(span.getLogs(0).hasTimestamp(), is(true));
    }

    @Test
    public void testAttributeAdded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/attributeAdded");
        String traceId = request.run(String.class);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(1));

        Span span = spans.get(0);

        assertThat(span, isSpan().withAttribute(TestResource.TEST_ATTRIBUTE_KEY, TestResource.TEST_ATTRIBUTE_VALUE));
    }

    private boolean hasParent(Span span) {
        Optional<SpanRef> parentRef = span.getReferencesList()
                                          .stream()
                                          .filter(ref -> ref.getRefType() == SpanRefType.CHILD_OF)
                                          .findAny();
        return parentRef.isPresent();
    }

}
