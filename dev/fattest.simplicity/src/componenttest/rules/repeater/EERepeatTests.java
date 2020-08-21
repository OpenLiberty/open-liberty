/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

/**
 *
 */
public class EERepeatTests {

    public static enum EEVersion {
        EE7, EE8, EE9, EE7_FULL, EE8_FULL, EE9_FULL
    }

    public static FeatureReplacementAction getEEAction(EEVersion version, String serverName, String clientName) {
        FeatureReplacementAction action = null;
        switch (version) {
            case EE7: {
                action = FeatureReplacementAction.EE7_FEATURES();
                break;
            }
            case EE8: {
                action = FeatureReplacementAction.EE8_FEATURES();
                break;
            }
            case EE9: {
                action = FeatureReplacementAction.EE9_FEATURES();
                break;
            }
            case EE7_FULL: {
                action = FeatureReplacementAction.EE7_FEATURES().fullFATOnly();
                break;
            }
            case EE8_FULL: {
                action = FeatureReplacementAction.EE8_FEATURES().fullFATOnly();
                break;
            }
            case EE9_FULL: {
                action = FeatureReplacementAction.EE9_FEATURES().fullFATOnly();
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown EE version: " + version);
            }
        }
        if (serverName != null) {
            action = action.forServers(serverName);
        }
        if (clientName != null) {
            action = action.forClients(clientName);
        }
        return action;
    }

    /**
     * Convenience method to repeat EE versions.
     *
     * Instead of
     * <code>public static RepeatTests r =
     * RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("myServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("myServer")).andWith(FeatureReplacementAction.EE9_FEATURES().forServers("myServer"));</code>
     *
     * Use
     * <code>public static RepeatTests r = EERepeatTests.with("myServer", EE7, EE8, EE9);</code>
     *
     * @param  version
     * @param  otherVersions
     * @return
     */
    public static RepeatTests with(String serverName, EEVersion version, EEVersion... otherVersions) {
        return with(serverName, FeatureReplacementAction.NO_CLIENTS, version, otherVersions);
    }

    public static RepeatTests with(String serverName, String clientName, EEVersion version, EEVersion... otherVersions) {
        RepeatTests r = RepeatTests.with(getEEAction(version, serverName, clientName));
        for (EEVersion ver : otherVersions) {
            r = r.andWith(getEEAction(ver, serverName, clientName));
        }
        return r;
    }

}
