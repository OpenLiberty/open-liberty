/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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
 * JavaEE/Jakarta repeat test helper widget.
 * 
 * This helper widget provides a table of JavaEE versions and factory methods
 * which assist in creating a {@link RepeatTest} instance with repeats for
 * the specified JavaEE versions.
 * 
 * See {@link EERepeatTests.EEVersion},
 * {@link #with(String, EEVersion, EEVersion...)},
 * and {@link #with(String, String, EEVersion, EEVersion...). 
 */
public class EERepeatTests {

    /**
     * Enumeration of JavaEE versions for which repeat actions are available.
     * 
     * Note that the enumeration contains contains two values: 
     *
     */
    public static enum EEVersion {
        EE7, EE8, EE9, EE7_FULL, EE8_FULL, EE9_FULL
    }

    /**
     * Feature action factory method: Create a feature replacement action
     * for a specified JavaEE version, server, and client.
     * 
     * The server may be specified as {@link FeatureReplacementAction#ALL_SERVERS}
     * or as {@link FeatureReplacementAction#NO_SERVERS}, in which case
     * the action is used, respectively, on all servers or on no servers.
     * 
     * The client may be specified as {@link FeatureReplacementAction#ALL_CLIENTS}
     * or as {@link FeatureReplacementAction#NO_CLIENTS}, in which case
     * the action is used, respectively, on all clients or on no clients.
     * 
     * @param version The JavaEE version of the replacement action.
     * @param serverName The name of the server on which to replace features.
     * @param clientName The name of the client on which to replace features.
     *
     * @return A new feature replacement test repeat action set for the
     *     specified JavaEE version, server, and client.
     */
    public static FeatureReplacementAction getEEAction(
        EEVersion version, String serverName, String clientName) {

        FeatureReplacementAction action;

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

        if ( serverName != null ) {
            action = action.forServers(serverName);
        }

        if ( clientName != null ) {
            action = action.forClients(clientName);
        }

        return action;
    }

    /**
     * JavaEE repeat test factory method.
     * 
     * Create a repeat test widget set for one or more JavaEE versions, for
     * a named server, and for no clients.
     *
     * Instead of
     * <code>
     * @ClassRule
     * public static RepeatTests r =
     *     RepeatTests.with( FeatureReplacementAction.EE7_FEATURES().forServers("myServer") )
     *                .andWith( FeatureReplacementAction.EE8_FEATURES().forServers("myServer") )
     *                .andWith( FeatureReplacementAction.EE9_FEATURES().forServers("myServer") );
     * </code>
     *
     * Use
     * <code>
     * @ClassRule
     * public static RepeatTests r = EERepeatTests.with("myServer", EE7, EE8, EE9);
     * </code>
     *
     * The server may be specified as {@link FeatureReplacementAction#ALL_SERVERS}
     * or as {@link FeatureReplacementAction#NO_SERVERS}, in which case
     * the action is used, respectively, on all servers or on no servers.
     * 
     * Perform feature replacement on no clients.
     *
     * @param serverName The server on which to perform feature replacements.
     * @param version The JavaEE version to which to replace features.
     * @param otherVersions Other JavaEE versions to which to replace features.
     *
     * @return A repeat test widget set to repeat tests for one or more JavaEE versions.
     */
    public static RepeatTests with(String serverName, EEVersion version, EEVersion... otherVersions) {
        return with(serverName, FeatureReplacementAction.NO_CLIENTS, version, otherVersions);
    }

    /**
     * JavaEE repeat test factory method.
     * 
     * Create a repeat test widget set for one or more JavaEE versions, for
     * a named server, and for a named client.
     *
     * The server may be specified as {@link FeatureReplacementAction#ALL_SERVERS}
     * or as {@link FeatureReplacementAction#NO_SERVERS}, in which case
     * the action is used, respectively, on all servers or on no servers.
     * 
     * The client may be specified as {@link FeatureReplacementAction#ALL_CLIENTS}
     * or as {@link FeatureReplacementAction#NO_CLIENTS}, in which case
     * the action is used, respectively, on all clients or on no clients.
     *
     * @param serverName The server on which to perform feature replacements.
     * @param clientName The client on which to perform feature replacements.
     * @param version The JavaEE version to which to replace features.
     * @param otherVersions Other JavaEE versions to which to replace features.
     *
     * @return A repeat test widget set to repeat tests for one or more JavaEE versions.
     */
    public static RepeatTests with(String serverName, String clientName, EEVersion version, EEVersion... otherVersions) {
        RepeatTests r = RepeatTests.with( getEEAction(version, serverName, clientName));
        for ( EEVersion ver : otherVersions ) {
            r = r.andWith(getEEAction(ver, serverName, clientName));
        }
        return r;
    }
}
