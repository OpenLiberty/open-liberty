/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.internal.test.suite;

import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(FATRunner.class)
public class MPRepeatUtils {

    //all versions
    static final String[] MP_VERSIONS = { "1.0", "1.2", "1.3", "1.4", "2.0", "2.1", "2.2", "3.0", "3.2", "3.3", "4.0", "4.1" };
    //all versions that have MP Config in
    static final String[] MP_CONFIG_VERSIONS = { "1.2", "1.3", "1.4", "2.0", "2.1", "2.2", "3.0", "3.2", "3.3", "4.0", "4.1" };
    static final String LITE_MODE = "4.1";

    static RepeatTests getMPRepeat(String server) {
        return getMPRepeat(server, MP_VERSIONS);
    }

    static RepeatTests getMPRepeat(String server, String[] versions) {
        RepeatTests repeat = RepeatTests.with(new MicroProfile(LITE_MODE, server));
        if (TestModeFilter.shouldRun(TestMode.FULL)) {
            for (String ver : versions) {
                if (!ver.equals(LITE_MODE)) {
                    repeat = repeat.andWith(new MicroProfile(ver, server));
                }
            }
        }

        return repeat;
    }

    static class MicroProfile extends FeatureReplacementAction {
        public MicroProfile(String version, String server) {
            for (String ver : MP_VERSIONS) {
                if (ver.equals(version)) {
                    addFeature("microProfile-" + ver);
                } else {
                    removeFeature("microProfile-" + ver);
                }
            }
            forServers(server);
            withID("MP" + version);
        }
    }
}
