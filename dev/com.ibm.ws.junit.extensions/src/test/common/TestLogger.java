/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.common;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 *
 */
public class TestLogger extends TestWatcher {

    private void logInfo(Description description, String logMsg) {
        Logger logger = Logger.getLogger(description.getClassName());
        logger.logp(Level.INFO, description.getClassName(), description.getMethodName(), logMsg);
    }

    @Override
    public void starting(Description description) {
        logInfo(description, "Entering test " + description.getMethodName());
    }

    @Override
    public void succeeded(Description description) {
        logInfo(description, "PASS: " + description.getMethodName());
    }

    @Override
    public void failed(Throwable e, Description description) {
        logInfo(description, "FAIL: " + description.getMethodName());
    }

    @Override
    public void finished(Description description) {
        logInfo(description, "Exiting test " + description.getMethodName());
    }

}
