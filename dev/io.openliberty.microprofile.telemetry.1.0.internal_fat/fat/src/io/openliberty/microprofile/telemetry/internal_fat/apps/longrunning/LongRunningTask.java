package io.openliberty.microprofile.telemetry.internal_fat.apps.longrunning;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/longrunning")
@ApplicationScoped
public class LongRunningTask extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    OpenTelemetry openTelemetry;

    private static final String INVALID_SPAN_ID = "0000000000000000";

    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Test
    public void testLongRunningTask() {
        Span span = tracer.spanBuilder("longRunningTask").startSpan();
        try (Scope scope = span.makeCurrent()) {
            assertThat(doWork(), equalTo(SUCCESS_MESSAGE));
            assertThat(span.getSpanContext().getSpanId(), not(equalTo(INVALID_SPAN_ID)));
        } finally {
            span.end();
        }
    }

    private String doWork() {
        Span workSpan = tracer.spanBuilder("workSpan").startSpan();
        try {
            assertThat(workSpan.getSpanContext().getSpanId(), not(equalTo(INVALID_SPAN_ID)));
            return SUCCESS_MESSAGE;
        } finally {
            workSpan.end();
        }
    }

}