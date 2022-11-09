package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/WithSpanServlet")
public class WithSpanServlet extends FATServlet {

    @Test
    public void current_empty() {
        withSpanTest();
        assertEquals("equals", "equals");
    }

    @WithSpan
    private void withSpanTest() {
        System.out.println("Hello test");
    }

}