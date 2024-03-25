/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.recordelresolver.servlets;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import io.openliberty.el60.fat.recordelresolver.records.TestRecordString;
import io.openliberty.el60.fat.recordelresolver.records.TestRecordStringRecord;
import jakarta.el.ELException;
import jakarta.el.ELProcessor;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.RecordELResolver;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet for Expression Language 6.0 RecordELResolver.
 */
@WebServlet({ "/EL60RecordELResolverTest" })
public class EL60RecordELResolverTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    ELProcessor elp = new ELProcessor();

    public EL60RecordELResolverTestServlet() {
        elp.defineBean("testRecordString", new TestRecordString("Test Data!"));
    }

    /**
     * Test the RecordELResolver for a String.
     *
     * @throws Exception
     */
    @Test
    public void testEL60RecordELResolver_String() throws Exception {
        String expectedData = "Test Data!";
        String actualData = (String) elp.eval("testRecordString.data");
        assertTrue("The actual data was: " + actualData + " but was expected to be: " + expectedData, actualData.equals(expectedData));
    }

    /**
     * Test the RecordELResolver getValue method to ensure a PropertyNotFoundException is thrown when the specified property does not exist.
     *
     * The RecordELResolver getValue method states the following: "@throws PropertyNotFoundException if the {@code base} is an instance of {@link Record} and the specified property
     * does not exist."
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60RecordELResolver_PropertyNotFoundException() throws Exception {
        boolean propertyNotFoundExceptionThrown = false;
        try {
            elp.eval("testRecordString.doesNotExist");
        } catch (PropertyNotFoundException e) {
            propertyNotFoundExceptionThrown = true;

            // Verify that the RecordELResolver is actually in the stack trace.
            assertTrue("The RecordELResolver was not found in the exception stack trace!", isRecordELResolverInStackTrace(e.getStackTrace()));
        }

        assertTrue("A PropertyNotFoundException was not thrown as expected!", propertyNotFoundExceptionThrown);
    }

    /**
     * Test the RecordELResolver getValue method to ensure an ELException is thrown when an exception was thrown while performing the property resolution..
     *
     * The RecordELResolver getValue method states the following: "@throws ELException if an exception was thrown while performing the property resolution. The thrown
     * exception must be included as the cause of this exception, if available."
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60RecordELResolver_ELException() throws Exception {
        boolean elExceptionThrow = false;
        try {
            elp.eval("testRecordString.throwException");
        } catch (ELException e) {
            elExceptionThrow = true;
            Throwable causedBy = e;

            // Get the root cause of the ELException to verify it is an UnsupportedOperationException.
            while (causedBy.getCause() != null) {
                causedBy = causedBy.getCause();
            }

            // Verify the UnsupportedOperationException thrown by the throwException method is the cause of the ELException.
            assertTrue("The cause of the ELException was not a java.lang.UnsupportedOperationException as expected but was: " + causedBy,
                       causedBy.getClass().getCanonicalName().equals(UnsupportedOperationException.class.getCanonicalName()));

            // Verify that the RecordELResolver is actually in the stack trace.
            assertTrue("The RecordELResolver was not found in the exception stack trace!", isRecordELResolverInStackTrace(e.getStackTrace()));
        }

        assertTrue("An ELException was not thrown as expected!", elExceptionThrow);
    }

    /**
     * Test to ensure the RecordELResolver is read only.
     *
     * The RecordELResolver states the following: "This resolver is always read-only since {@link Record}s are always read-only."
     *
     * The RecordELResolver setValue method states the following: "If the base object is an instance of {@link Record}, always throws an exception since {@link Record}s are
     * read-only."
     *
     * The RecordELResolver setValue method states the following: "@throws PropertyNotWritableException if the {@code base} is an instance of {@link Record} and the specified
     * property exists."
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60RecordELResolver_PropertyNotWritableException() throws Exception {
        boolean propertyNotWritableExceptionThrown = false;

        // Use a unique ELProcessor since we have to import some classes.
        ELProcessor elProcessor = new ELProcessor();

        // Import the TestRecordString and TestRecordStringRecord so we can use them within the ELProcessor.
        elProcessor.getELManager().importClass("io.openliberty.el60.fat.recordelresolver.records.TestRecordString");
        elProcessor.getELManager().importClass("io.openliberty.el60.fat.recordelresolver.records.TestRecordStringRecord");

        try {
            elProcessor.setVariable("testRecordString", "TestRecordString(\"Test Data!\")");
            elProcessor.setVariable("testRecordStringRecord", "TestRecordStringRecord(\"Test Data!\", testRecordString)");
            log("testRecordString: " + elProcessor.getValue("testRecordString", TestRecordString.class));
            log("testRecordStringRecord: " + elProcessor.getValue("testRecordStringRecord", TestRecordStringRecord.class));
            elProcessor.setValue("testRecordStringRecord.record", new TestRecordString("New Test Data!"));

        } catch (PropertyNotWritableException e) {
            propertyNotWritableExceptionThrown = true;

            // Verify that the RecordELResolver is actually in the stack trace.
            assertTrue("The RecordELResolver was not found in the exception stack trace!", isRecordELResolverInStackTrace(e.getStackTrace()));
        }

        assertTrue("A PropertyNotWritableException was not thrown as expected!", propertyNotWritableExceptionThrown);
    }

    /*
     * Return true if the jakarta.el.RecordELResolver is found in the stack trace.
     */
    private boolean isRecordELResolverInStackTrace(StackTraceElement[] stack) {
        boolean recordELResolverInStackTrace = false;

        for (StackTraceElement element : stack) {
            if (element.getClassName().equals(RecordELResolver.class.getCanonicalName())) {
                recordELResolverInStackTrace = true;
                log("StackTraceElement containing RecordELResolver: " + element.toString());
                break;
            }
        }
        return recordELResolverInStackTrace;
    }
}
