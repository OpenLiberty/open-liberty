/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.jmock.Mockery;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.ibm.websphere.ras.CapturedOutputHolder;
import com.ibm.websphere.ras.TrConfigZapper;

/**
 * The SharedOutputManager redirects the System.out and System.err PrintStreams
 * to make running the tests quiet unless an unexpected exception happens.
 * This makes it easier to figure out which testcase bombed in ant build output.
 * <p>
 * The default output location for the logs is under the project root:
 * build/trace-logs/
 * 
 * <h4> recommended use </h4>
 * <pre>
 * SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");
 * &#064;Rule
 * public TestRule outputRule = outputMgr;
 * </pre>
 * 
 * The TestRule manages capturing/restoring streams, and dumping stream contents if
 * the test fails. Using two declarations (the separate assignment to outputMgr
 * allows assertions to test for error/output messages.
 * 
 * <h4> depreciated (old) use </h4>
 * <pre>
 * static SharedOutputManager outputMgr;
 * 
 * &#064;BeforeClass
 * public static void setUpBeforeClass() throws Exception
 * {
 * // make stdout/stderr &quot;quiet&quot;
 * outputMgr = new SharedOutputManager();
 * outputMgr.captureStreams();
 * }
 * 
 * &#064;AfterClass
 * public static void tearDownAfterClass() throws Exception
 * {
 * // Make stdout and stderr &quot;normal&quot;
 * outputMgr.restoreStreams();
 * }
 * 
 * Mockery context; // if you're using jmock _without_ JUnit4 integration
 * 
 * &#064;After
 * public void tearDown() throws Exception
 * {
 * // If you're using JMock without JUnit4 integration, this will wrap the
 * // context.assertIsSatisfied in a try/catch block,
 * // and will preserve the output streams if Expectations aren't satisfied..
 * outputMgr.assertContextStatisfied(context);
 * 
 * // Clear the output generated after each method invocation, this keeps things
 * // sane
 * outputMgr.resetStreams();
 * }
 * 
 * &#064;Test
 * public void testMethod()
 * {
 * final String m = &quot;testMethod&quot;;
 * 
 * try
 * {
 * // do stuff
 * }
 * catch (Throwable t)
 * {
 * // This will add information about the Throwable to System.err, and will
 * // dump the captured
 * // output (both System.out and System.err) to the original streams (this
 * // allows ant
 * // to capture/display the output).
 * outputMgr.failWithThrowable(m, t);
 * }
 * }
 * </pre>
 */
public class SharedOutputManager implements TestRule {
    /**
     * Use a nested interface to hold an instance of this output manager.
     * This instance will not be created until it is first accessed.
     */
    private static interface SingletonHolder {
        SharedOutputManager INSTANCE = new SharedOutputManager();
    }

    /**
     * Retrieve an instance of this class.
     */
    public static SharedOutputManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    String traceSpec = null;
    File logDirectory = null;
    CapturedOutputHolder outputHolder = null;
    List<String> expectedOutputMessageKeys;
    List<String> expectedErrorMessageKeys;

    /**
     * Create a new shared output stream with the default trace spec.
     * <p>
     * This will try to set the log directory to the build directory
     * so we are subject to ant clean, etc. Otherwise, the default will
     * be the project root if we can't determine the build directory.
     */
    private SharedOutputManager() {
        File buildDir = null;
        String buildDirProperty = System.getProperty("test.buildDir");
        if (buildDirProperty != null) {
            try {
                buildDir = new File(buildDirProperty);
                if (!buildDir.exists()) {
                    buildDir = null;
                }
            } catch (SecurityException e) {
                // Ignore it and try next location
            }
        }
        if (buildDir == null) {
            // Eclipse and ant both set the project root to user.dir
            String projectRoot = System.getProperty("user.dir");
            buildDir = new File(projectRoot + File.separator + "build");
        }
        try {
            // Only try to create trace-logs if build already exists.
            // If build doesn't exist, we're not in the right path, and
            // we don't want to be creating directories in random places.
            if (buildDir.exists()) {
                File buildLogsDir = new File(buildDir.getAbsolutePath() + File.separator + "trace-logs");
                buildLogsDir.mkdir();
                if (buildLogsDir.exists()) {
                    logDirectory = buildLogsDir;
                }
            }
        } catch (SecurityException e) {
            // Ignore it and keep the default location
        }
    }

    /**
     * @param traceSpec
     *            Description of enabled trace, default is *=audit=enabled
     */
    public SharedOutputManager trace(String traceSpec) {
        traceTo(traceSpec, this.logDirectory);
        return this;
    }

    /**
     * @param logDirectory
     *            Location of log files
     */
    public SharedOutputManager logTo(File logDirectory) {
        traceTo(this.traceSpec, logDirectory);
        return this;
    }

    /**
     * @param logDirectory
     *            Location of log files
     */
    public SharedOutputManager logTo(String logDirectory) {
        return logTo(new File(logDirectory));
    }

    /**
     * @param traceSpec
     *            Description of enabled trace, default is *=audit=enabled
     * @param logDirectory
     *            Location of log files
     */
    public SharedOutputManager traceTo(String traceSpec, File logDirectory) {
        String rasTraceSpec = (traceSpec == null) ? TrConfigZapper.getSystemTraceSpec() : traceSpec;
        this.traceSpec = rasTraceSpec;
        this.logDirectory = logDirectory;
        return this;
    }

    /**
     * This method prints a message about the caught {@link Throwable}, and the
     * associated stack trace to the (still hushed) standard error. It then copies
     * the captured contents of both standard out and standard err to the system
     * originals before rethrowing the original {@link Throwable}.
     * 
     * @param methodName name of calling method
     * @throws t
     */
    public void failWithThrowable(String methodName, Throwable t) {
        // only need to log the exception if 't' is not the result of a failed assertion
        // or assumption violation.
        if (!!!(t instanceof AssertionError) &&
            !!!(t instanceof AssumptionViolatedException)) {
            String m = getMethodNameOfCaller();
            System.err.println(m + " encountered unexpected exception: " + t.getMessage());
            t.printStackTrace();
        }

        if (outputHolder != null) {
            outputHolder.dumpStreams();
        }
        rethrow(t);
    }

    /**
     * Get the name of the method that called the caller of this method.
     */
    private String getMethodNameOfCaller() {
        return getMethodName(2);
    }

    /**
     * Get the name of the calling method <code>stackOffset</code> steps up the stack from the caller.
     * So <code>0</code> would be the caller itself, <code>1</code> would be the caller of the caller, etc.
     * 
     * @param stackOffset the number of steps up the stack to look
     * @return the method name found or <code>"unknown"</code> if no name could be found
     * @throws ArrayIndexOutOfBoundsException if the offset passed in is larger than the stack
     */
    private String getMethodName(int stackOffset) {
        final String methodName = "getMethodName";
        StackTraceElement[] stack = new Throwable().getStackTrace();
        if (stack == null || stack.length == 0)
            return "unknown";
        int index = 0;
        /*
         * Some Java runtimes will add additional method calls to the stack trace,
         * such as the constructor of Throwable. To avoid these messing up the output,
         * we look up this method's name. Normally this will be at index = 0.
         */
        while (!!!stack[index].getMethodName().equals(methodName))
            index++;
        // Move one step up the stack to get to this method's caller.
        index++;
        // Now we need to offset by the number passed in.
        index += stackOffset;
        // dig out the method name and pass it back
        return stack[index].getMethodName();
    }

    /**
     * This method is a hack to re-throw any exception, checked or unchecked.
     * This is only acceptable to use in test code.
     * 
     * @param throwable
     */
    private static void rethrow(Throwable throwable) {
        SharedOutputManager.<RuntimeException> useRuntimeTypeErasureToCircumventExceptionChecking(throwable);
    }

    private static <T extends Throwable> void useRuntimeTypeErasureToCircumventExceptionChecking(Throwable throwable) throws T {
        @SuppressWarnings("unchecked")
        // at runtime, T is 'erased' to type java.lang.Throwable, so this cast cannot fail
        T t = (T) throwable;
        throw t;
    }

    /**
     * In Lieu of using the JUnit4 integration with JMock, this method can
     * be called to assert that all of the conditions associated with the
     * given jmock context have been satisfied. If they haven't, the throwable
     * is routed through failWithThrowable, to ensure that all testcase related
     * output is copied to stdout and stderr, including the detailed stack
     * trace containing information about the satisfied and unsatisfied
     * attributes of the jmock context expectations.
     * 
     * @param context
     *            Mockery context to verify.
     * @see org.jmock.Mockery#assertIsSatisfied()
     * @see #failWithThrowable(String, Throwable)
     */
    public void assertContextStatisfied(Mockery context) {
        final String methodName = "assertContextStatisfied";
        try {
            context.assertIsSatisfied();
        } catch (Throwable t) {
            failWithThrowable(methodName, t);
        }
    }

    /**
     * Capture all stream output (System.out, System.err, messages.log and trace.log)
     * <p>
     * Use {@link #resetStreams()} to reset the captured streams between test methods,
     * and {@link #copySystemStreams()} to copy the contents of the captured stream onto
     * the original output streams in the case of a test failure, and {@link #restoreStandardOut()} to
     * restore the streams to their natural state in test class tearDown.
     */
    public void captureStreams() {
        if (outputHolder == null) {
            TrConfigZapper.revert(); // back to vanilla
            outputHolder = TrConfigZapper.zapTrConfig(traceSpec, logDirectory);
            outputHolder.captureStreams();
        } else {
            throw new IllegalStateException("A previous test did not restore the streams -- this can cause unpredictable results.. ");
        }
    }

    /**
     * Convenience method to copy all captured output back to its original stream.
     */
    public void copySystemStreams() {
        if (outputHolder != null) {
            outputHolder.copySystemStreams();
        }
    }

    /**
     * Convenience method to copy captured trace to standard out
     */
    public void copyTraceStream() {
        if (outputHolder != null) {
            outputHolder.copyTraceStream();
        }
    }

    /**
     * Convenience method to copy captured messages to standard out
     */
    public void copyMessageStream() {
        if (outputHolder != null) {
            outputHolder.copyMessageStream();
        }
    }

    public void dumpStreams() {
        if (outputHolder != null) {
            outputHolder.dumpStreams();
        }
    }

    /**
     * Convenience method to reset/clear the contents of hushed stderr and stdout.
     * 
     * @see #resetStandardErr()
     * @see #resetStandardOut()
     */
    public void resetStreams() {
        if (outputHolder != null) {
            outputHolder.resetStreams();
        }
    }

    /**
     * Convenience method to restore both stderr and stdout to original streams.
     * 
     * @see #restoreStandardErr()
     * @see #restoreStandardOut()
     */
    public void restoreStreams() {
        if (outputHolder != null) {
            outputHolder.stop();
            TrConfigZapper.revert(); // back to vanilla
            outputHolder = null;
        }
    }

    public boolean isEmptyTraceLog() {
        if (outputHolder != null) {
            String output = outputHolder.getCapturedTrace();
            return output.isEmpty();
        }
        return true;
    }

    public boolean isEmptyMessageLog() {
        if (outputHolder != null) {
            String output = outputHolder.getCapturedMessages();
            return output.isEmpty();
        }
        return true;
    }

    public boolean isEmptyStandardOut() {
        if (outputHolder != null) {
            String output = getCapturedOut();
            return output.isEmpty();
        }
        return true;
    }

    public boolean isEmptyStandardErr() {
        if (outputHolder != null) {
            String output = getCapturedErr();
            return output.isEmpty();
        }
        return true;
    }

    public String getCapturedOut() {
        return outputHolder.getCapturedOut();
    }

    public String getCapturedErr() {
        return outputHolder.getCapturedErr();
    }

    /**
     * Match the regex to each line within the output.
     * 
     * @param output The captured output (may contain embedded newlines)
     * @param regex The regex to match to any line within the output
     * @return {@code true} if any of the lines match the regex, {@code false} otherwise.
     */
    private boolean anyLineMatches(String output, String regex) {
        Pattern pattern = Pattern.compile(regex);
        String newline = "\n|\r\n";
        for (String line : output.split(newline)) {
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Use to detect whether a given message was issued to System.out
     * since {@link SharedOutputManager#captureStreams()} was called.
     */
    public boolean checkForStandardOut(String regex) {
        if (outputHolder != null) {
            String output = getCapturedOut();
            return anyLineMatches(output, regex);
        }
        return false;
    }

    /**
     * Use to detect whether a given message was issued to System.err
     * since {@link SharedOutputManager#captureStreams()} was called.
     */
    public boolean checkForStandardErr(String regex) {
        if (outputHolder != null) {
            String output = getCapturedErr();
            return anyLineMatches(output, regex);
        }
        return false;
    }

    /**
     * Use to detect whether a given message was issued to the trace log
     * since {@link SharedOutputManager#captureStreams()} was called.
     */
    public boolean checkForTrace(String regex) {
        if (outputHolder != null) {
            String output = outputHolder.getCapturedTrace();
            return anyLineMatches(output, regex);
        }
        return false;
    }

    /**
     * Use to detect whether a given message was issued to the messages log
     * since {@link SharedOutputManager#captureStreams()} was called.
     */
    public boolean checkForMessages(String regex) {
        if (outputHolder != null) {
            String output = outputHolder.getCapturedMessages();
            return anyLineMatches(output, regex);
        }
        return false;
    }

    /**
     * Use to detect whether a given String literal was issued to System.out
     * since {@link SharedOutputManager#captureStreams()} was called.
     */
    public boolean checkForLiteralStandardOut(String literal) {
        if (outputHolder != null) {
            String output = getCapturedOut();
            return output.contains(literal);
        }
        return false;
    }

    /**
     * Use to detect whether a given String literal was issued to System.err
     * since {@link SharedOutputManager#captureStreams()} was called.
     */
    public boolean checkForLiteralStandardErr(String literal) {
        if (outputHolder != null) {
            String output = getCapturedErr();
            return output.contains(literal);
        }
        return false;
    }

    /**
     * Use to detect whether a given String literal was issued to the trace log
     * since {@link SharedOutputManager#captureStreams()} was called.
     */
    public boolean checkForLiteralTrace(String literal) {
        if (outputHolder != null) {
            String output = outputHolder.getCapturedTrace();
            return output.contains(literal);
        }
        return false;
    }

    /**
     * Use to detect whether a given String literal was issued to the messages log
     * since {@link SharedOutputManager#captureStreams()} was called.
     */
    public boolean checkForLiteralMessages(String literal) {
        if (outputHolder != null) {
            String output = outputHolder.getCapturedMessages();
            return output.contains(literal);
        }
        return false;
    }

    /**
     * Use to specify an expected output message keys that should be
     * logged as a result of the test. Multiple message keys can be specified.
     * <p>
     * The check for the expected message keys is done automatically
     * as part of the test rule.
     * 
     * @param messageKey
     */
    public void expectOutput(String messageKey) {
        if (expectedOutputMessageKeys == null) {
            expectedOutputMessageKeys = new ArrayList<String>();
        }
        expectedOutputMessageKeys.add(messageKey);
    }

    /**
     * Use to specify an expected warning message keys that should be
     * logged as a result of the test. Multiple message keys can be specified.
     * <p>
     * The check for the expected message keys is done automatically
     * as part of the test rule.
     * 
     * @param messageKey
     */
    public void expectWarning(String messageKey) {
        // This overload exists because it's not necessarily intuitive that
        // warnings go to stdout rather than stderr.
        expectOutput(messageKey);
    }

    /**
     * Use to specify an expected error message key that should be logged
     * as a result of the test. Multiple message keys can be specified.
     * <p>
     * The check for the expected error message keys is done automatically
     * as part of the test rule.
     * 
     * @param messageKey
     */
    public void expectError(String messageKey) {
        if (expectedErrorMessageKeys == null) {
            expectedErrorMessageKeys = new ArrayList<String>();
        }
        expectedErrorMessageKeys.add(messageKey);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        synchronized (this) {
            if (outputHolder != null) {
                sb.append("\nCaptured Standard Out: \n");
                sb.append(getCapturedOut());
                sb.append("\nCaptured Standard Err: \n");
                sb.append(getCapturedErr());
                sb.append("\nMessage Log: \n");
                sb.append(outputHolder.getCapturedMessages());
            }
        }

        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public Statement apply(final Statement stmt, final Description desc) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // capture stdout and stderr before every test
                // Do not set any options here: allow a separate declaration 
                // of the SharedOutputManager (such that logTo or traceTo can be driven)
                // to handle customization. Keep this basic/common/no-fuss
                captureStreams();
                try {
                    // run the test
                    stmt.evaluate();
                    if (expectedOutputMessageKeys != null) {
                        for (String messageKey : expectedOutputMessageKeys) {
                            // Check for key + ':' to ensure the key was found
                            // in the resource bundle.
                            Assert.assertTrue("expected stdout message: " + messageKey, checkForStandardOut(messageKey + ':'));
                        }
                    }
                    if (expectedErrorMessageKeys != null) {
                        for (String messageKey : expectedErrorMessageKeys) {
                            Assert.assertTrue("expected stderr message: " + messageKey, checkForStandardErr(messageKey + ':'));
                        }
                    }
                } catch (Throwable t) {
                    // print out what we captured if the test failed
                    failWithThrowable(desc.getMethodName(), t);
                } finally {
                    // stop capturing the streams after every test
                    restoreStreams();
                    expectedOutputMessageKeys = null;
                    expectedErrorMessageKeys = null;
                }
            }
        };
    }
}
