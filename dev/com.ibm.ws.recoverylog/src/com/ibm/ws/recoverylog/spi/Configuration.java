/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import com.ibm.tx.util.alarm.AlarmManager;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

//------------------------------------------------------------------------------
// Class: Configuration
//------------------------------------------------------------------------------
/**
 * This class provides a common location to store configuration information for
 * the RLS code. The content of this class is established during component
 * initialization by the RecLogServiceImpl class.
 */
public class Configuration {
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(Configuration.class, TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * Holds the name of the cell in which the current server resides
     */
    private static String _cellName;

    /**
     * Holds the name of the cluster in which the current server resides
     */
    private static String _clusterName;

    /**
     * Holds the name of the node in which the current server resides
     */
    private static String _nodeName;

    /**
     * Holds the name of the current server
     */
    private static String _serverName;

    /**
     * Holds the short name of the current server
     */
    private static String _serverShortName;

    /**
     * Holds the UUID of the current server
     */
    private static String _uuid; /* @LI1578-22A */

    /**
     * Holds the location of the Was install.
     */
    private static String _WASInstallDirectory;

    /**
     * Flag indicating if the HA support is enabled in this server instance.
     * This will be disabled until the 'EnableHA' property is selected by
     * the user and this server is deployed in a cluster.
     */
    private static boolean _HAEnabled;

    /**
     * A refrence to a failure scope that defines the local region of
     * execution (eg an application server or server region)
     */
    private static FailureScope _localFailureScope;

    /**
     * RLS version number. This will be written to the header so future versions of the
     * RLS will be able to detect which level of the code wrote a given log file.
     */
    static final int RLS_VERSION = 3;

    /**
     * RLS compatability array. This indicates the software levels of the RLS service
     * with which this level is compatible. Clearly, this version must always be
     * compatible with itself.
     */
    protected static final int[] COMPATIBLE_RLS_VERSIONS = { RLS_VERSION };

    /**
     * Transaction Service Recovery Agent Reference.
     */
    private static RecoveryAgent _txRecoveryAgent;

    /**
     * Interface to Service component
     */
    private static RecoveryLogComponent _reclogComponent;

    /**
     * Interface to timer/AlarmManager
     */
    private static AlarmManager _alarmManager;

    //------------------------------------------------------------------------------
    // Method: Configuration.WASInstallDirectory
    //------------------------------------------------------------------------------
    /**
     * Sets the WAS install directory.
     *
     * @param WASInstallDirectory The WAS install directory
     */
    public static final void WASInstallDirectory(String WASInstallDirectory) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "WASInstallDirectory", WASInstallDirectory);

        _WASInstallDirectory = WASInstallDirectory;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.WASInstallDirectory
    //------------------------------------------------------------------------------
    /**
     * Gets the WAS install directory.
     *
     * @return String The WAS install directory.
     */
    public static final String WASInstallDirectory() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "WASInstallDirectory", _WASInstallDirectory);
        return _WASInstallDirectory;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.cellName
    //------------------------------------------------------------------------------
    /**
     * Sets the name of the cell in which the current server resides
     *
     * @param name The name of the WAS cell.
     */
    public static final void cellName(String name) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "cellName", name);

        _cellName = name;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.cellName
    //------------------------------------------------------------------------------
    /**
     * Gets the name of the cell in which the current server resides
     *
     * @return String The name of the WAS cell
     */
    public static final String cellName() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "cellName", _cellName);
        return _cellName;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.clusterName
    //------------------------------------------------------------------------------
    /**
     * Sets the name of the cluster in which the current server resides
     *
     * @param name The name of the WAS cluster
     */
    public static final void clusterName(String name) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "clusterName", name);

        _clusterName = name;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.clusterName
    //------------------------------------------------------------------------------
    /**
     * Gets the name of the cluster in which the current server resides
     *
     * @return String The name of the WAS cluster
     */
    public static final String clusterName() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "clusterName", _clusterName);
        return _clusterName;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.nodeName
    //------------------------------------------------------------------------------
    /**
     * Sets the name of the node in which the current server resides
     *
     * @param name The node name
     */
    public static final void nodeName(String name) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "nodeName", name);

        _nodeName = name;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.nodeName
    //------------------------------------------------------------------------------
    /**
     * Gets the name of the node in which the current server resides
     *
     * @return String The node name
     */
    public static final String nodeName() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "nodeName", _nodeName);
        return _nodeName;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.serverName
    //------------------------------------------------------------------------------
    /**
     * Sets the name of the current WAS server
     *
     * @param name The name of the WAS server
     */
    public static final void serverName(String name) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "serverName", name);

        _serverName = name;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.serverName
    //------------------------------------------------------------------------------
    /**
     * Gets the name of the current WAS server
     *
     * @return String The name of the WAS server
     */
    public static final String serverName() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "serverName", _serverName);
        return _serverName;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.serverShortName
    //------------------------------------------------------------------------------
    /**
     * Sets the short name of the current WAS server
     *
     * @param name The short name of the WAS server
     */
    public static final void serverShortName(String name) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "serverShortName", name);

        _serverShortName = name;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.serverShortName
    //------------------------------------------------------------------------------
    /**
     * Gets the short name of the current WAS server
     *
     * @return String The short name of the WAS server
     */
    public static final String serverShortName() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "serverShortName", _serverShortName);
        return _serverShortName;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.uuid
    //------------------------------------------------------------------------------
    /**
     * Sets the UUID of the current WAS server
     *
     * @param name The UUID of the WAS server
     */
    public static final void uuid(String uuid) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "uuid", uuid);

        _uuid = uuid;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.uuid
    //------------------------------------------------------------------------------
    /**
     * Gets the UUID of the current WAS server
     *
     * @return String the UUID of the WAS server
     */
    public static final String uuid() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "uuid", _uuid);
        return _uuid;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.fqServerName
    //------------------------------------------------------------------------------
    /**
     * Gets the fully qualified name of the current WAS server
     *
     * @return String The fully qualified name of the WAS server
     */
    public static final String fqServerName() {
        String fqServerName = _serverName; // RLSUtils.FQHAMCompatibleServerName(_cellName,_nodeName,_serverName); tWAS

        if (tc.isDebugEnabled())
            Tr.debug(tc, "fqServerName", fqServerName);
        return fqServerName;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.HAEnabled
    //------------------------------------------------------------------------------
    /**
     * Sets the HAEnabled flag
     *
     * @param HAEnabled The HAEnabled flag
     */
    public static final void HAEnabled(boolean HAEnabled) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "HAEnabled", HAEnabled);

        _HAEnabled = HAEnabled;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.HAEnabled
    //------------------------------------------------------------------------------
    /**
     * Gets the HAEnabled flag
     *
     * @return boolean name The HAEnabled flag
     */
    public static final boolean HAEnabled() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "HAEnabled", _HAEnabled);
        return _HAEnabled;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.localFailureScope
    //------------------------------------------------------------------------------
    /**
     * Sets the local FailureScope reference.
     *
     * @param localFailureScope The local FailureScope
     */
    public static final void localFailureScope(FailureScope localFailureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "localFailureScope", localFailureScope);

        _localFailureScope = localFailureScope;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "localFailureScope");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.localFailureScope
    //------------------------------------------------------------------------------
    /**
     * Gets the local FailureScope reference.
     *
     * @return FailureScope The local FailureScope
     */
    public static final FailureScope localFailureScope() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "localFailureScope", _localFailureScope);
        return _localFailureScope;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.txRecoveryAgent
    //------------------------------------------------------------------------------
    /**
    *
    */
    public static void txRecoveryAgent(RecoveryAgent txRecoveryAgent) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "txRecoveryAgent", txRecoveryAgent);

        _txRecoveryAgent = txRecoveryAgent;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.txRecoveryAgent
    //------------------------------------------------------------------------------
    /**
    *
    */
    public static RecoveryAgent txRecoveryAgent() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "txRecoveryAgent", _txRecoveryAgent);
        return _txRecoveryAgent;
    }
    //---------------------------------------------------------------------------
    // Method: Configuration.setRecoveryLogComponent
    //---------------------------------------------------------------------------

    public static final void setRecoveryLogComponent(RecoveryLogComponent rls) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setRecoveryLogComponent", rls);
        _reclogComponent = rls;
    }

    //---------------------------------------------------------------------------
    // Method: Configuration.getSetRecoveryLogComponent
    //---------------------------------------------------------------------------

    public static final RecoveryLogComponent getRecoveryLogComponent() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRecoveryLogComponent", _reclogComponent);
        return _reclogComponent;
    }

    //---------------------------------------------------------------------------
    // Method: Configuration.setAlarmManager
    //---------------------------------------------------------------------------

    public static final void setAlarmManager(AlarmManager mgr) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setAlarmManager", mgr);
        _alarmManager = mgr;
    }

    //---------------------------------------------------------------------------
    // Method: Configuration.getAlarmManager
    //---------------------------------------------------------------------------

    public static final AlarmManager getAlarmManager() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getAlarmManager", _alarmManager);
        return _alarmManager;
    }

}
