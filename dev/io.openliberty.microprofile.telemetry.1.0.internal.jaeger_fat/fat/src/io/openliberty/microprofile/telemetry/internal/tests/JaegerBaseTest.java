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
package io.openliberty.microprofile.telemetry.internal.tests;

import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasEventLog;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasNoParent;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.hasParentSpanId;
import static io.openliberty.microprofile.telemetry.internal.utils.jaeger.SpanMatcher.span;
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

    @Server("spanTestServer")
    public static LibertyServer server;

    public JaegerQueryClient client = getJaegerClient();

    /**
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

        assertThat(span, span().withTraceId(traceId)
                               .withTag(SemanticAttributes.HTTP_ROUTE.getKey(), "/spanTest/")
                               .withTag(SemanticAttributes.HTTP_METHOD.getKey(), "GET"));

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

        assertThat(parent, hasProperty("operationName", equalTo("/spanTest/subspan")));
        assertThat(child, hasProperty("operationName", equalTo(TestResource.TEST_OPERATION_NAME)));
    }

    @Test
    public void testExceptionRecorded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/exception");
        String traceId = request.run(String.class);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(1));

        Span span = spans.get(0);

        assertThat(span, span().withStatus(StatusCode.ERROR)
                               .withExceptionLog(RuntimeException.class));
        assertThat(span.getLogs(0).hasTimestamp(), is(true));
    }

    @Test
    public void testAttributeAdded() throws Exception {
        HttpRequest request = new HttpRequest(server, "/spanTest/attributeAdded");
        String traceId = request.run(String.class);

        List<Span> spans = client.waitForSpansForTraceId(traceId, hasSize(1));

        Span span = spans.get(0);

        assertThat(span, span().withTag(TestResource.TEST_ATTRIBUTE_KEY.getKey(), TestResource.TEST_ATTRIBUTE_VALUE));
    }

    private boolean hasParent(Span span) {
        Optional<SpanRef> parentRef = span.getReferencesList()
                                          .stream()
                                          .filter(ref -> ref.getRefType() == SpanRefType.CHILD_OF)
                                          .findAny();
        return parentRef.isPresent();
    }

}
