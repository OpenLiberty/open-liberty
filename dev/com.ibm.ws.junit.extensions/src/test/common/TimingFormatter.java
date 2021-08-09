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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitVersionHelper;

/**
 * This class is to be used in a <code>&lt;formatter><code> block under a <code>&lt;junit></code> task.
 * <p>
 * This formatter outputs the time taken to run each test in real time. It's intended for use when debugging timeout failures.
 * <p>
 * <pre>
 * Testcase: com.example.tests.Foo&nbsp;&nbsp;&nbsp; PASSED 30 ms Testsuite so far: 30 ms
 * Testcase: com.example.tests.FooBar PASSED 40 ms Testsuite so far: 70 ms
 * </pre>
 * <p>
 * The "plain" builtin formatter has similar output but is not suitable as it collects all output until the end of the suite. When the test run times out, the end of the suite is
 * not reached and no output is produced in the log.
 */
public class TimingFormatter implements JUnitResultFormatter {

    private PrintWriter out = null;
    private Long suiteStartTime = 0l;
    private final Map<Test, Long> testStartTimes = new HashMap<Test, Long>();
    private Status testStatus = Status.PASSED;

    @Override
    public void startTest(Test test) {
        testStartTimes.put(test, System.currentTimeMillis());
        testStatus = Status.PASSED;
    }

    @Override
    public void endTest(Test test) {
        long endTime = System.currentTimeMillis();
        Long startTime = testStartTimes.get(test);
        if (startTime != null && out != null) {
            long testTime = endTime - startTime;
            long suiteSubtotal = endTime - suiteStartTime;

            Formatter f = null;
            try {
                f = new Formatter();
                f.format("Testcase: %-50s %6s %5d ms Testsuite so far: %d ms", JUnitVersionHelper.getTestCaseName(test), testStatus, testTime, suiteSubtotal);
                out.println(f.out().toString());
            } finally {
                if (f != null) {
                    f.close();
                }
            }
        }
    }

    @Override
    public void startTestSuite(JUnitTest test) throws BuildException {
        suiteStartTime = System.currentTimeMillis();
    }

    @Override
    public void endTestSuite(JUnitTest test) throws BuildException {
        if (out != null) {
            long suiteEndTime = System.currentTimeMillis();
            long suiteTime = suiteEndTime - suiteStartTime;
            out.println("Testsuite finished: " + test.getName() + " " + suiteTime + "ms");
        }
    }

    @Override
    public void setOutput(OutputStream outputStream) {
        out = new PrintWriter(outputStream, true);
    }

    @Override
    public void addError(Test test, Throwable error) {
        testStatus = Status.ERROR;
    }

    @Override
    public void addFailure(Test test, AssertionFailedError failure) {
        testStatus = Status.FAILED;
    }

    @Override
    public void setSystemError(String arg0) {}

    @Override
    public void setSystemOutput(String arg0) {}

    private enum Status {
        PASSED,
        FAILED,
        ERROR
    };

}
