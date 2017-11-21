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
package test.common;

import java.io.OutputStream;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

/**
 * This formatter is to be used with a formatter element within an ant junit task, e.g.:
 * <pre> &lt;junit&gt;
 * &nbsp;&nbsp;&nbsp;&nbsp;&hellip;
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;formatter classname="test.common.QuietFormatter" /&gt;
 * &lt;/junit&gt;</pre>
 */
public class QuietFormatter implements JUnitResultFormatter {

    SharedOutputManager outputManager = SharedOutputManager.getInstance();

    @Override
    public void startTestSuite(JUnitTest t) {
        outputManager.captureStreams();
        outputManager.resetStreams();
    }

    @Override
    public void endTestSuite(JUnitTest t) {
        outputManager.restoreStreams();
    }

    @Override
    public void startTest(Test t) {}

    @Override
    public void endTest(Test t) {}

    @Override
    public void addError(Test t, Throwable th) {
        // only need to log the exception if 't' is not the result of a failed assertion
        outputManager.copySystemStreams();
    }

    @Override
    public void addFailure(Test t, AssertionFailedError e) {
        outputManager.copySystemStreams();
    }

    @Override
    public void setOutput(OutputStream arg0) {}

    @Override
    public void setSystemError(String arg0) {}

    @Override
    public void setSystemOutput(String arg0) {}

}
