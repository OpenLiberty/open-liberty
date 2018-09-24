package com.ibm.ws.security.test.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

public class CommonTestClass {

    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    protected final static String defaultExceptionMsg = "This is an exception message.";

    protected final static String MSG_BASE = "CWWKS";
    protected final static String MSG_BASE_ERROR_WARNING = "CWWKS[0-9]{4}(E|W)";

    @Rule
    public final TestName testName = new TestName();

    /****************************************** Helper methods ******************************************/

    public void assertStringInTrace(SharedOutputManager outputMgr, String checkForString) {
        checkForString = (checkForString == null) ? "null" : checkForString;
        assertTrue("Did not find expected value [" + checkForString + "] in trace.", outputMgr.checkForLiteralTrace(checkForString));
    }

    public void assertStringNotInTrace(SharedOutputManager outputMgr, String checkForString) {
        checkForString = (checkForString == null) ? "null" : checkForString;
        assertFalse("Found value [" + checkForString + "] in trace but should not have.", outputMgr.checkForLiteralTrace(checkForString));
    }

    public void assertRegexInTrace(SharedOutputManager outputMgr, String checkForRegex) {
        checkForRegex = (checkForRegex == null) ? "null" : checkForRegex;
        assertTrue("Did not find expected regular expression [" + checkForRegex + "] in trace.", outputMgr.checkForTrace(checkForRegex));
    }

    public void assertRegexNotInTrace(SharedOutputManager outputMgr, String checkForRegex) {
        checkForRegex = (checkForRegex == null) ? "null" : checkForRegex;
        assertFalse("Found regular expression [" + checkForRegex + "] in trace but should not have.", outputMgr.checkForTrace(checkForRegex));
    }

    public void assertStringInStandardOut(SharedOutputManager outputMgr, String checkForString) {
        checkForString = (checkForString == null) ? "null" : checkForString;
        assertTrue("Did not find expected value [" + checkForString + "] in standard out.", outputMgr.checkForLiteralStandardOut(checkForString));
    }

    public void assertStringNotInStandardOut(SharedOutputManager outputMgr, String checkForString) {
        checkForString = (checkForString == null) ? "null" : checkForString;
        assertFalse("Found value [" + checkForString + "] in standard out but should not have.", outputMgr.checkForLiteralStandardOut(checkForString));
    }

    public void verifyPattern(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        verifyPatternExists(input, pattern, "Input did not contain the expected expression.");
    }

    public void verifyPattern(String input, String regex, String failureMsg) {
        Pattern pattern = Pattern.compile(regex);
        verifyPatternExists(input, pattern, failureMsg);
    }

    public void verifyPatternMatches(String input, Pattern pattern, String failureMsg) {
        assertNotNull(failureMsg + " Value should not have been null but was. Expected pattern [" + pattern.toString() + "].", input);
        assertTrue(failureMsg + " Expected to find pattern [" + pattern.toString() + "]. Value was [" + input + "].", pattern.matcher(input).matches());
    }

    public void verifyPatternExists(String input, Pattern pattern, String failureMsg) {
        assertNotNull(failureMsg + " Value should not have been null but was. Expected pattern [" + pattern.toString() + "].", input);
        assertTrue(failureMsg + " Expected to find pattern [" + pattern.toString() + "]. Value was [" + input + "].", pattern.matcher(input).find());
    }

    public void verifyNoLogMessage(SharedOutputManager outputMgr, String messageRegex) {
        assertFalse("Found message [" + messageRegex + "] in log but should not have.", outputMgr.checkForMessages(messageRegex));
    }

    public void verifyLogMessage(SharedOutputManager outputMgr, String messageRegex) {
        assertTrue("Did not find message [" + messageRegex + "] in log.", outputMgr.checkForMessages(messageRegex));
    }

    public void verifyLogMessageWithInserts(SharedOutputManager outputMgr, String msgKey, String... inserts) {
        String fullPattern = buildStringWithInserts(msgKey, inserts).toString();
        assertTrue("Did not find message [" + fullPattern + "] in log.", outputMgr.checkForMessages(fullPattern));
    }

    public void verifyException(Throwable e, String errorMsgRegex) {
        String errorMsg = e.toString();
        Pattern pattern = Pattern.compile(errorMsgRegex);
        Matcher m = pattern.matcher(errorMsg);
        assertTrue("Exception message did not match expected expression. Expected: [" + errorMsgRegex + "]. Message was: [" + errorMsg + "]", m.find());
    }

    public void verifyExceptionWithInserts(Exception e, String msgKey, String... inserts) {
        String errorMsg = e.toString();
        verifyStringWithInserts(errorMsg, msgKey, inserts);
    }

    public void verifyStringWithInserts(String searchString, String msgKey, String... inserts) {
        String fullPattern = buildStringWithInserts(msgKey, inserts).toString();
        Pattern pattern = Pattern.compile(fullPattern);
        Matcher m = pattern.matcher(searchString);
        assertTrue("Provided string did not contain [" + fullPattern + "] as expected. Full string was: [" + searchString + "]", m.find());
    }

    private StringBuilder buildStringWithInserts(String msgKey, String... inserts) {
        // Expects inserts to be in square brackets '[]'
        StringBuilder regexBuilder = new StringBuilder(msgKey).append(".*");
        for (String insert : inserts) {
            regexBuilder.append("\\[" + insert + "\\]").append(".*");
        }
        return regexBuilder;
    }

}
