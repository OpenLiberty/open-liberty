/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.repeat;

import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.RepeatTests;

/**
 *
 */
public class RepeatConfigActions {

    /**
     * Get the RepeatTests actions for Config 1.1 tests. This is the same as for Config 1.2 tests but
     * if the test mode is FULL then it also adds Config 1.1 + EE7 and Config 1.1 + EE8.
     *
     * @param server The name of the test server
     * @return The RepeatTests for Config 1.1 tests
     */
    public static RepeatTests repeatConfig11(String server) {
        RepeatTests r = repeatConfig12(server);
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            r = r.andWith(new RepeatConfig11EE7(server));
            r = r.andWith(new RepeatConfig11EE8(server));
        }
        return r;
    }

    /**
     * Get the RepeatTests actions for Config 1.2 tests. This is the same as for Config 1.3 tests but
     * if the test mode is FULL then it also adds Config 1.2 + EE7 and Config 1.2 + EE8.
     *
     * @param server The name of the test server
     * @return The RepeatTests for Config 1.2 tests
     */
    public static RepeatTests repeatConfig12(String server) {
        RepeatTests r = repeatConfig13(server);
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            r = r.andWith(new RepeatConfig12EE7(server));
            r = r.andWith(new RepeatConfig12EE8(server));
        }
        return r;
    }

    /**
     * Get the RepeatTests actions for Config 1.3 tests. This is the same as for Config 1.4 tests but
     * if the test mode is FULL then it also adds Config 1.3 + EE7 and Config 1.3 + EE8.
     *
     * @param server The name of the test server
     * @return The RepeatTests for Config 1.3 tests
     */
    public static RepeatTests repeatConfig13(String server) {
        RepeatTests r = repeatConfig14(server);
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            r = r.andWith(new RepeatConfig13EE7(server));
            r = r.andWith(new RepeatConfig13EE8(server));
        }
        return r;
    }

    /**
     * Get the RepeatTests actions for Config 1.4 tests.
     *
     * In LITE mode, this runs Config 1.4 + EE8.
     * In FULL mode, this also runs Config 1.4 + EE7
     * In EXPERIMENTAL mode, this also runs Config 2.0.
     *
     * @param server The name of the test server
     * @return The RepeatTests for Config 1.4 tests
     */
    public static RepeatTests repeatConfig14(String server) {

        RepeatTests r = null;

        if (TestModeFilter.shouldRun(TestMode.EXPERIMENTAL)) {
            r = repeatConfig20(server);
            r = r.andWith(new RepeatConfig14EE8(server));
        } else {
            r = RepeatTests.with(new RepeatConfig14EE8(server));
        }

        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            r = r.andWith(new RepeatConfig14EE7(server));
        }
        return r;
    }

    /**
     * Get the RepeatTests actions for Config 2.0 tests.
     *
     * In all modes, this runs Config 2.0 + EE8.
     *
     * @param server The name of the test server
     * @return The RepeatTests for Config 2.0 tests
     */
    public static RepeatTests repeatConfig20(String server) {
        RepeatTests r = RepeatTests.with(new RepeatConfig20EE8(server));

        return r;
    }

    /**
     * There are some Config 1.1 dynamic tests which are not applicable to MP Config 1.4 and higher.
     * So this returns Config 1.3 + EE8 in LITE mode and then adds the others if in FULL mode.
     *
     * @param server The name of the test server
     * @return The RepeatTests for Config 1.1 tests which should not be run against Config 1.4
     */
    public static RepeatTests repeatConfig11Not14(String server) {
        RepeatTests r = RepeatTests.with(new RepeatConfig13EE8(server));
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            r = r.andWith(new RepeatConfig11EE7(server));
            r = r.andWith(new RepeatConfig11EE8(server));
            r = r.andWith(new RepeatConfig12EE7(server));
            r = r.andWith(new RepeatConfig12EE8(server));
            r = r.andWith(new RepeatConfig13EE7(server));
        }
        return r;
    }

}
