package io.openliberty.microprofile.telemetry.internal_fat.apps.longrunning;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/longrunning")
@ApplicationScoped
public class LongRunningTask extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @Inject
    OpenTelemetry openTelemetry;

    private static final String INVALID_SPAN_ID = "0000000000000000";

    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Test
    public void testLongRunningTask() {
        Tracer tracer = openTelemetry.getTracer("logging-exporter-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        assertThat(doWork(), equalTo(SUCCESS_MESSAGE));
        assertThat(span.getSpanContext().getSpanId(), not(equalTo(INVALID_SPAN_ID)));
        span.end();
    }

    private String doWork() {
        Span span = tracer.spanBuilder("doingWork").startSpan();

        for (int i = 0; i < 240; i++) {
            try {
                Thread.sleep(1000L);
                System.out.println("Sleeping for " + i + " seconds");
            } catch (InterruptedException ie) {
            }
        }
        System.out.println("Long running task Completed");

        assertThat(span.getSpanContext().getSpanId(), not(equalTo(INVALID_SPAN_ID)));
        span.end();
        return SUCCESS_MESSAGE;
    }

}