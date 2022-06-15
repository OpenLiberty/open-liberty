/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transactional.web;

import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.transaction.Status;

public class TestContext {
    private final String testName;
    private Throwable cause;
    private TestResult result;
    private long UOWId;
    private int status = Status.STATUS_UNKNOWN;

    public TestContext(String test) {
        testName = test;
        result = TestResult.UNKNOWN;
        UOWId = -1L;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(testName).append(": ").append(result).append("<br>");

        if (result != TestResult.PASSED) {
            sb.append("UOW: ").append(UOWId).append("<br>");
            sb.append("Status: ").append(printableStatus(status)).append("<br>");
            sb.append("Exception: ").append(throwableStackAsHtml(cause));
        }

        return sb.toString();
    }

    private String printableStatus(int status) {
        switch (status) {
            case Status.STATUS_ACTIVE:
                return "Active";
            case Status.STATUS_COMMITTED:
                return "Committed";
            case Status.STATUS_COMMITTING:
                return "Committing";
            case Status.STATUS_MARKED_ROLLBACK:
                return "MarkedRollback";
            case Status.STATUS_NO_TRANSACTION:
                return "NoTransaction";
            case Status.STATUS_PREPARED:
                return "Prepared";
            case Status.STATUS_PREPARING:
                return "Preparing";
            case Status.STATUS_ROLLEDBACK:
                return "RolledBack";
            case Status.STATUS_ROLLING_BACK:
                return "RollingBack";
            case Status.STATUS_UNKNOWN:
                return "Unknown";
            default:
                return "WhoKnows";
        }
    }

    public void setFailed(Throwable o) {
        cause = o;
        result = TestResult.FAILED;
        fail();
    }

    public void setPassed() {
        // Only set passed if we haven't already failed
        if (result == TestResult.UNKNOWN) {
            result = TestResult.PASSED;
        }
    }

    public Throwable getCause() {
        return cause;
    }

    public void setUOWId(long id) {
        UOWId = id;
    }

    public long getUOWId() {
        return UOWId;
    }

    private enum TestResult {
        UNKNOWN, FAILED, PASSED
    }

    public void setStatus(int s) {
        status = s;
    }

    public int getStatus() {
        return status;
    }

    private String throwableStackAsHtml(Throwable t) {
        if (t != null) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            final String stackTrace = sw.toString();

            return stackTrace.replace(System.getProperty("line.separator"), "<br/>\n");
        }

        return "No exception. Did you forget to call tc.setPassed() in your test?<br/>";
    }
}