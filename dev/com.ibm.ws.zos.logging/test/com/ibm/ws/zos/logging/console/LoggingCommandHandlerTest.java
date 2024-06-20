/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.wsspi.zos.command.processing.ModifyResults;

public class LoggingCommandHandlerTest {

    final Mockery context = new JUnit4Mockery();

    LoggingCommandHandler lh;
    ConfigurationAdmin configAdmin;
    Configuration config;

    /** Response for invalid input to command handler. Includes msg and help text */
    List<String> badCommandResponseMsg;

    @Before
    public void setup() throws Exception {
        configAdmin = context.mock(ConfigurationAdmin.class, "configAdmin");
        config = context.mock(Configuration.class, "config");

        lh = new LoggingCommandHandler();
        lh.configuredTraceSpec = "*=info=enabled";
        lh.setConfigAdmin(configAdmin);
    }

    @After
    public void tearDown() {
        lh = null;
    }

    @Test
    public void testLifecycle() {
        lh = new LoggingCommandHandler();

        // Test setters
        assertNull(lh.configAdmin);

        lh.configuredTraceSpec = "*=info=enabled";
        lh.setConfigAdmin(configAdmin);
        assertSame(configAdmin, lh.configAdmin);
    }

    @Test
    public void testGetHelp() throws Exception {
        assertEquals(LoggingCommandHandler.HELP_TEXT_SHORT, lh.getHelp());
        assertTrue("Short help text size must be 3 lines or less", lh.getHelp().size() <= 3);

    }

    @Test
    public void testGetName() throws Exception {
        assertEquals(LoggingCommandHandler.NAME, lh.getName());
    }

    @Test
    public void testTrimQuotes() {
        assertEquals("foo", lh.trimQuotes("foo"));
        assertEquals("foo", lh.trimQuotes("'foo'"));
        assertEquals("", lh.trimQuotes("'something that's not terminated"));
        assertEquals("", lh.trimQuotes("'"));
        assertEquals("", lh.trimQuotes("''"));
    }

    @Test
    public void testHandleModifyResetQuotes() throws Exception {

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(LoggingCommandHandler.TRACE_SPEC_KEY, "*=all=disabled");

        final ModifyResults modifyResults = context.mock(ModifyResults.class);
        context.checking(new Expectations() {
            {
                oneOf(configAdmin).getConfiguration(LoggingCommandHandler.LOGGING_PID, null);
                will(returnValue(config));

                oneOf(config).getProperties();
                will(returnValue(props));
                oneOf(config).update(props);

                oneOf(modifyResults).setProperty(ModifyResults.HELP_RESPONSE_KEY, false);
                oneOf(modifyResults).setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                oneOf(modifyResults).setResponsesContainMSGIDs(false);
                oneOf(modifyResults).setResponses(null);
            }
        });

        String commandString = "logging='reset'";
        lh.handleModify(commandString, modifyResults);
    }

    @Test
    public void testHandleModifyResetNoQuotes() throws Exception {

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(LoggingCommandHandler.TRACE_SPEC_KEY, "*=all=disabled");

        final ModifyResults modifyResults = context.mock(ModifyResults.class);
        context.checking(new Expectations() {
            {
                oneOf(configAdmin).getConfiguration(LoggingCommandHandler.LOGGING_PID, null);
                will(returnValue(config));

                oneOf(config).getProperties();
                will(returnValue(props));
                oneOf(config).update(props);

                allowing(modifyResults).setProperty(ModifyResults.HELP_RESPONSE_KEY, false);
                oneOf(modifyResults).setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                oneOf(modifyResults).setResponsesContainMSGIDs(false);
                oneOf(modifyResults).setResponses(null);
            }
        });

        String commandString = "logging=reset";
        lh.handleModify(commandString, modifyResults);
    }

    @Test
    public void testHandleModifyNoEquals() throws Exception {
        final ModifyResults modifyResults = context.mock(ModifyResults.class);
        final String command = "logging";
        final List<String> response = getInvalidCommandRepsonseList(command);

        context.checking(new Expectations() {
            {
                oneOf(modifyResults).setProperty(ModifyResults.HELP_RESPONSE_KEY, false);
                oneOf(modifyResults).setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                oneOf(modifyResults).setProperty(ModifyResults.HELP_RESPONSE_KEY, true);
                oneOf(modifyResults).setResponsesContainMSGIDs(false);

                oneOf(modifyResults).setResponses(with(response));
            }
        });

        lh.handleModify(command, modifyResults);
    }

    @Test
    public void testHandleModifyNoSpec() throws Exception {
        final ModifyResults modifyResults = context.mock(ModifyResults.class);
        final String command = "logging=";
        final List<String> response = getInvalidCommandRepsonseList(command);

        context.checking(new Expectations() {
            {
                oneOf(modifyResults).setProperty(ModifyResults.HELP_RESPONSE_KEY, false);
                oneOf(modifyResults).setCompletionStatus(ModifyResults.ERROR_PROCESSING_COMMAND);
                oneOf(modifyResults).setProperty(ModifyResults.HELP_RESPONSE_KEY, true);
                oneOf(modifyResults).setResponsesContainMSGIDs(false);

                oneOf(modifyResults).setResponses(with(response));
            }
        });

        lh.handleModify(command, modifyResults);
    }

    /** Helper method to get invalid command response */
    private List<String> getInvalidCommandRepsonseList(String command) {
        List<String> response = new ArrayList<String>(LoggingCommandHandler.HELP_TEXT_LONG);
        response.add(0, "Could not parse command: \"" + command + "\"");
        return response;

    }

    @Test
    public void testHandleModifySingleSpec() throws Exception {
        final String traceSpec = "zos.native=all";

        final ModifyResults modifyResults = context.mock(ModifyResults.class);
        setupTraceSpecExpectations(modifyResults, traceSpec);

        lh.handleModify("logging=" + wrapWithQuotes(traceSpec), modifyResults);
    }

    @Test
    public void testHandleModifyMultipleSpecs() throws Exception {
        final String traceSpec = "zos.native=all:Security=all:foo.bar=all=disabled";

        final ModifyResults modifyResults = context.mock(ModifyResults.class);
        setupTraceSpecExpectations(modifyResults, traceSpec);

        lh.handleModify("logging=" + wrapWithQuotes(traceSpec), modifyResults);
    }

    @Test
    public void testSetConfiguredTraceSpec() throws Exception {

        final Dictionary<Object, Object> props = new Hashtable<Object, Object>();
        props.put(LoggingCommandHandler.TRACE_SPEC_KEY, "*=all=disabled");

        context.checking(new Expectations() {
            {
                exactly(2).of(configAdmin).getConfiguration(LoggingCommandHandler.LOGGING_PID, null);
                will(returnValue(config));

                oneOf(config).getProperties();
                will(returnValue(props));
            }

        });

        lh = new LoggingCommandHandler();
        lh.setConfigAdmin(configAdmin);
        assertEquals("*=all=disabled", lh.configuredTraceSpec);

    }

    String wrapWithQuotes(String string) {
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(string).append("'");
        return sb.toString();
    }

    void setupTraceSpecExpectations(final ModifyResults modifyResults, final String traceSpec) throws Exception {
        context.checking(new Expectations() {
            {
                oneOf(configAdmin).getConfiguration(LoggingCommandHandler.LOGGING_PID, null);
                will(returnValue(config));

                oneOf(config).getProperties();
                will(returnValue(new Hashtable<String, Object>()));

                oneOf(config).update(with(DictionaryMatcher.dictionaryHasEntry(LoggingCommandHandler.TRACE_SPEC_KEY, traceSpec)));

                oneOf(modifyResults).setProperty(ModifyResults.HELP_RESPONSE_KEY, false);
                oneOf(modifyResults).setCompletionStatus(ModifyResults.PROCESSED_COMMAND);
                oneOf(modifyResults).setResponsesContainMSGIDs(false);
                oneOf(modifyResults).setResponses(null);
            }
        });
    }
}

@SuppressWarnings("rawtypes")
class DictionaryMatcher extends TypeSafeMatcher<Dictionary> {

    @Factory
    public static Matcher<Dictionary> dictionaryHasEntry(Object key, Object value) {
        return new DictionaryMatcher(key, value);
    }

    final Object key;
    final Object value;

    public DictionaryMatcher(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void describeTo(Description desc) {
        desc.appendText("a dictionary that has entry " + key + " => " + value);
    }

    @Override
    public boolean matchesSafely(Dictionary dictionary) {
        Object val = dictionary.get(key);
        return val == value || (val != null && val.equals(value));
    }
}
