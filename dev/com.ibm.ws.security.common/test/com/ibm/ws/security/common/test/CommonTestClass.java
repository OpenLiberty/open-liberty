package com.ibm.ws.security.common.test;

import static org.junit.Assert.assertFalse;
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

    protected void verifyPattern(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(input);
        assertTrue("Input did not contain the expected expression. Expected: [" + regex + "]. Value was: [" + input + "]", m.find());
    }

    protected void verifyPattern(String input, String regex, String failureMsg) {
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(input);
        assertTrue(failureMsg + " Input did not contain the expected expression. Expected: [" + regex + "]. Value was: [" + input + "]", m.find());
    }

    protected void verifyNoLogMessage(SharedOutputManager outputMgr, String messageRegex) {
        assertFalse("Found message [" + messageRegex + "] in log but should not have.", outputMgr.checkForMessages(messageRegex));
    }

    protected void verifyLogMessage(SharedOutputManager outputMgr, String messageRegex) {
        assertTrue("Did not find message [" + messageRegex + "] in log.", outputMgr.checkForMessages(messageRegex));
    }

    protected void verifyLogMessageWithInserts(SharedOutputManager outputMgr, String msgKey, String... inserts) {
        String fullPattern = buildStringWithInserts(msgKey, inserts).toString();
        assertTrue("Did not find message [" + fullPattern + "] in log.", outputMgr.checkForMessages(fullPattern));
    }

    protected void verifyException(Exception e, String errorMsgRegex) {
        String errorMsg = e.getLocalizedMessage();
        Pattern pattern = Pattern.compile(errorMsgRegex);
        Matcher m = pattern.matcher(errorMsg);
        assertTrue("Exception message did not match expected expression. Expected: [" + errorMsgRegex + "]. Message was: [" + errorMsg + "]", m.find());
    }

    protected void verifyExceptionWithInserts(Exception e, String msgKey, String... inserts) {
        String errorMsg = e.getLocalizedMessage();
        verifyStringWithInserts(errorMsg, msgKey, inserts);
    }

    protected void verifyStringWithInserts(String searchString, String msgKey, String... inserts) {
        String fullPattern = buildStringWithInserts(msgKey, inserts).toString();
        Pattern pattern = Pattern.compile(fullPattern);
        Matcher m = pattern.matcher(searchString);
        assertTrue("Provided string did not contain [" + fullPattern + "] as expected. Full string was: [" + searchString + "]", m.find());
    }

    protected StringBuilder buildStringWithInserts(String msgKey, String... inserts) {
        // Expects inserts to be in square brackets '[]'
        StringBuilder regexBuilder = new StringBuilder(msgKey).append(".*");
        for (String insert : inserts) {
            regexBuilder.append("\\[" + insert + "\\]").append(".*");
        }
        return regexBuilder;
    }

}
