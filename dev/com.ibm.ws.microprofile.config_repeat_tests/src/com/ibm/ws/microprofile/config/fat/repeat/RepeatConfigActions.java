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

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 *
 */
public class RepeatConfigActions {

    public enum Version {
        CONFIG11_EE7, CONFIG11_EE8, CONFIG12_EE7, CONFIG12_EE8, CONFIG13_EE7, CONFIG13_EE8, CONFIG14_EE7, CONFIG14_EE8, CONFIG20_EE8, LATEST
    };

    public static FeatureReplacementAction getAction(Version version, String server) {
        switch (version) {
            case CONFIG11_EE7:
                return new RepeatConfig11EE7(server);
            case CONFIG11_EE8:
                return new RepeatConfig11EE8(server);
            case CONFIG12_EE7:
                return new RepeatConfig12EE7(server);
            case CONFIG12_EE8:
                return new RepeatConfig12EE8(server);
            case CONFIG13_EE7:
                return new RepeatConfig13EE7(server);
            case CONFIG13_EE8:
                return new RepeatConfig13EE8(server);
            case CONFIG14_EE7:
                return new RepeatConfig14EE7(server);
            case CONFIG14_EE8:
                return new RepeatConfig14EE8(server);
            case CONFIG20_EE8:
                return new RepeatConfig20EE8(server);
            case LATEST:
                return new RepeatConfig20EE8(server); //currently the latest version is 1.4 (EE8). Will update to 2.0 when that reaches a beta standard.
            default:
                throw new RuntimeException("Unknown version: " + version);
        }
    }

    public static RepeatTests repeatAllConfigVersionsEE8(String server) {
        return repeat(server, Version.CONFIG11_EE8, Version.CONFIG12_EE8, Version.CONFIG13_EE8, Version.CONFIG14_EE8, Version.CONFIG20_EE8);
    }

    public static RepeatTests repeat(String server, Version firstVersion, Version... otherVersions) {
        RepeatTests r = RepeatTests.with(getAction(firstVersion, server));
        for (int i = 0; i < otherVersions.length; i++) {
            r = r.andWith(getAction(otherVersions[i], server));
        }
        return r;
    }
}
