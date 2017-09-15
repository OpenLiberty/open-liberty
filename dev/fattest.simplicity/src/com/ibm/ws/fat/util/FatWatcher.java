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
package com.ibm.ws.fat.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * <p>It watches your FAT. lol!<p>
 * <p>But seriously, you need to annotate a public member variable of your test class with {@link Rule} to to use this watcher. It logs extra messages to distinguish test A from
 * test B.</p>
 * 
 * @author Tim Burns
 */
public class FatWatcher extends TestWatcher {

    private static Logger LOG = Logger.getLogger(FatWatcher.class.getName());

    private final StopWatch timer = new StopWatch();
    private Boolean passed;

    @Override
    public void starting(Description description) {
        String delimiter = Props.getInstance().getProperty(Props.LOGGING_BREAK_LARGE);
        if (delimiter != null) {
            LOG.info(delimiter);
        }
        LOG.info("Starting Test: " + description);
        if (delimiter != null) {
            LOG.info(delimiter);
        }
        this.passed = null;
        this.timer.start();
    }

    @Override
    public void failed(Throwable t, Description description) {
        LOG.log(Level.WARNING, "Test Failure Details ...", t);
        this.passed = Boolean.FALSE;
    }

    @Override
    public void succeeded(Description description) {
        this.passed = Boolean.TRUE;
    }

    /**
     * Indicates whether the current test passed, failed, or did not finish.
     * 
     * @return true if the test passed, false if it failed, or null if it didn't finish yet
     */
    public Boolean passed() {
        return this.passed;
    }

    /**
     * Describes the current test status as a human-readable string.
     * 
     * @return "PASSED", "FAILED", or "DID NOT FINISH"
     */
    public String getStatus() {
        if (Boolean.TRUE.equals(this.passed)) {
            return "PASSED";
        }
        if (Boolean.FALSE.equals(this.passed)) {
            return "FAILED";
        }
        return "DID NOT FINISH";
    }

    @Override
    public void finished(Description description) {
        this.timer.stop();
        String delimiter = Props.getInstance().getProperty(Props.LOGGING_BREAK_LARGE);
        if (delimiter != null) {
            LOG.info(delimiter);
        }
        LOG.info("Finishing Test: " + description);
        if (delimiter != null) {
            LOG.info(delimiter);
        }
        LOG.info("Test Result  : " + this.getStatus());
        LOG.info("Time Elapsed : " + this.timer.getTimeElapsedAsString());
        if (delimiter != null) {
            LOG.info(delimiter);
        }
    }

}