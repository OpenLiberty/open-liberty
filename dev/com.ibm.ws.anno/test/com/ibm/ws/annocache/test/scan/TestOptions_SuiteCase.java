/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan;

public enum TestOptions_SuiteCase {
    SINGLE(TestOptions_Suite.SINGLE_OPTIONS),
    SINGLE_JANDEX(TestOptions_Suite.SINGLE_JANDEX_OPTIONS),
    SINGLE_JANDEX_FULL(TestOptions_Suite.SINGLE_JANDEX_FULL_OPTIONS),

    MULTI(TestOptions_Suite.MULTI_OPTIONS),
    MULTI_JANDEX(TestOptions_Suite.MULTI_JANDEX_OPTIONS),
    MULTI_JANDEX_FULL(TestOptions_Suite.MULTI_JANDEX_FULL_OPTIONS),

    SINGLE_WRITE(TestOptions_Suite.SINGLE_WRITE_OPTIONS),
    SINGLE_READ(TestOptions_Suite.SINGLE_READ_OPTIONS, SINGLE_WRITE),

    MULTI_WRITE(TestOptions_Suite.MULTI_WRITE_OPTIONS), 
    MULTI_READ(TestOptions_Suite.MULTI_READ_OPTIONS, MULTI_WRITE),

    SINGLE_WRITE_ASYNC(TestOptions_Suite.SINGLE_WRITE_ASYNC_OPTIONS),
    MULTI_WRITE_ASYNC(TestOptions_Suite.MULTI_WRITE_ASYNC_OPTIONS),

    SINGLE_WRITE_JANDEX_NOWRITE(TestOptions_Suite.SINGLE_WRITE_JANDEX_NOWRITE_OPTIONS),
    SINGLE_READ_JANDEX_NOWRITE(TestOptions_Suite.SINGLE_READ_JANDEX_NOWRITE_OPTIONS, SINGLE_WRITE_JANDEX_NOWRITE),
    MULTI_WRITE_JANDEX_NOWRITE(TestOptions_Suite.MULTI_WRITE_JANDEX_NOWRITE_OPTIONS),
    MULTI_READ_JANDEX_NOWRITE(TestOptions_Suite.MULTI_READ_JANDEX_NOWRITE_OPTIONS, MULTI_WRITE_JANDEX_NOWRITE),

    SINGLE_WRITE_SEPARATE(TestOptions_Suite.SINGLE_WRITE_SEPARATE_OPTIONS),
    SINGLE_READ_SEPARATE(TestOptions_Suite.SINGLE_READ_SEPARATE_OPTIONS, SINGLE_WRITE_SEPARATE),
    MULTI_WRITE_SEPARATE(TestOptions_Suite.MULTI_WRITE_SEPARATE_OPTIONS),
    MULTI_READ_SEPARATE(TestOptions_Suite.MULTI_READ_SEPARATE_OPTIONS, MULTI_WRITE_SEPARATE),

    SINGLE_WRITE_JANDEX_FORMAT(TestOptions_Suite.SINGLE_WRITE_JANDEX_FORMAT_OPTIONS),
    SINGLE_READ_JANDEX_FORMAT(TestOptions_Suite.SINGLE_READ_JANDEX_FORMAT_OPTIONS, SINGLE_WRITE_JANDEX_FORMAT),

    SINGLE_WRITE_BINARY_FORMAT(TestOptions_Suite.SINGLE_WRITE_BINARY_FORMAT_OPTIONS),
    SINGLE_READ_BINARY_FORMAT(TestOptions_Suite.SINGLE_READ_BINARY_FORMAT_OPTIONS, SINGLE_WRITE_BINARY_FORMAT),
    SINGLE_READ_BINARY_FORMAT_VALID(TestOptions_Suite.SINGLE_READ_BINARY_FORMAT_VALID_OPTIONS, SINGLE_WRITE_BINARY_FORMAT);

    //

    private TestOptions_SuiteCase(TestOptions options, TestOptions_SuiteCase[] prereqs) {
        this.options = options;
        this.prereqs = prereqs;
    }

    private TestOptions_SuiteCase(TestOptions options, TestOptions_SuiteCase prereq) {
        this.options = options;
        this.prereqs = new TestOptions_SuiteCase[] { prereq };
    }

    private TestOptions_SuiteCase(TestOptions options) {
        this.options = options;
        this.prereqs = null;
    }

    //

    private final TestOptions options;

    public TestOptions getOptions() {
        return options;
    }

    public boolean getIgnoreMissingPackages() {
        return options.ignoreMissingPackages;
    }

    public boolean getIgnoreMissingInterfaces() {
        return options.getIgnoreMissingInterfaces();
    }

    //

    private final TestOptions_SuiteCase[] prereqs;

    public TestOptions_SuiteCase[] getPrereqs() {
        return prereqs;
    }
}