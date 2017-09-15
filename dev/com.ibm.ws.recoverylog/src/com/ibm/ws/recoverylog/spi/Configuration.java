/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
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
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: Configuration
//------------------------------------------------------------------------------
/**
 * This class provides a common location to store configuration information for
 * the RLS code. The content of this class is established during component
 * initilziation by the RecLogServiceImpl class.
 */
public class Configuration
{
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(Configuration.class, TraceConstants.TRACE_GROUP, null);

    /**
     * Holds the name of the cell in which the current server resides
     */
    private static String _cellName = null;

    /**
     * Holds the name of the cluster in which the current server resides
     */
    private static String _clusterName = null;

    /**
     * Holds the name of the node in which the current server resides
     */
    private static String _nodeName = null;

    /**
     * Holds the name of the current server
     */
    private static String _serverName = null;

    /**
     * Holds the short name of the current server
     */
    private static String _serverShortName = null;

    /**
     * Holds the UUID of the current server
     */
    private static String _uuid = null; /* @LI1578-22A */

    /**
     * Holds the location of the Was install.
     */
    private static String _WASInstallDirectory = null;

    /**
     * Reference to the ORB
     */
//  private static org.omg.CORBA.ORB _orb = null;

    /**
     * Flag to indicate if this is a z/OS platform.
     */
    private static boolean _isZOS = false;

    /**
     * Reference fo the ClusterMemberService used to join and disjoin
     * self-declare style cluster to support end-point redirection.
     */
    // private static ClusterMemberService _clusterMemberService = null;

    /**
     * Flag indicating if the HA support is enabled in this server instance.
     * This will be disabled until the 'EnableHA' property is selected by
     * the user and this server is deployed in a cluster.
     */
    private static boolean _HAEnabled = false;

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
     * Boolean flag to configure if the RLS should attempt to use exclusive file
     * locking to gain access to recovery log files prior to opening them.
     */
    static boolean _useFileLocking = true;

    /**
     * RLS compatability array. This indicates the software levels of the RLS service
     * with which this level is compatible. Clearly, this version must always be
     * compatible with itself.
     */
    protected static final int[] COMPATIBLE_RLS_VERSIONS = { RLS_VERSION };

    /**
     * Transaction Service Recovery Agent Reference.
     */
    private static RecoveryAgent _txRecoveryAgent = null;

    /**
     * Boolean flag indicates if the RLS is configured to allow for
     * the suspension of RLS logging in a safe manner i.e. the logs are consistent
     */
    protected static boolean _isSnapshotSafe;

    /**
     * Interface to Service component
     */
    private static RecoveryLogComponent _reclogComponent;

    /**
     * Interface to security manager
     */
    private static AccessController _accessController;

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
    public static final void WASInstallDirectory(String WASInstallDirectory)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "WASInstallDirectory", WASInstallDirectory);

        _WASInstallDirectory = WASInstallDirectory;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "WASInstallDirectory");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.WASInstallDirectory
    //------------------------------------------------------------------------------
    /**
     * Gets the WAS install directory.
     * 
     * @return String The WAS install directory.
     */
    public static final String WASInstallDirectory()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "WASInstallDirectory");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "WASInstallDirectory", _WASInstallDirectory);
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
    public static final void cellName(String name)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "cellName", name);

        _cellName = name;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "cellName");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.cellName
    //------------------------------------------------------------------------------
    /**
     * Gets the name of the cell in which the current server resides
     * 
     * @return String The name of the WAS cell
     */
    public static final String cellName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "cellName");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "cellName", _cellName);
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
    public static final void clusterName(String name)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "clusterName", name);

        _clusterName = name;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "clusterName");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.clusterName
    //------------------------------------------------------------------------------
    /**
     * Gets the name of the cluster in which the current server resides
     * 
     * @return String The name of the WAS cluster
     */
    public static final String clusterName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "clusterName");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "clusterName", _clusterName);
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
    public static final void nodeName(String name)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "nodeName", name);

        _nodeName = name;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "nodeName");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.nodeName
    //------------------------------------------------------------------------------
    /**
     * Gets the name of the node in which the current server resides
     * 
     * @return String The node name
     */
    public static final String nodeName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "nodeName");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "nodeName", _nodeName);
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
    public static final void serverName(String name)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serverName", name);

        _serverName = name;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "serverName");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.serverName
    //------------------------------------------------------------------------------
    /**
     * Gets the name of the current WAS server
     * 
     * @return String The name of the WAS server
     */
    public static final String serverName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serverName");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "serverName", _serverName);
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
    public static final void serverShortName(String name)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serverShortName", name);

        _serverShortName = name;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "serverShortName");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.serverShortName
    //------------------------------------------------------------------------------
    /**
     * Gets the short name of the current WAS server
     * 
     * @return String The short name of the WAS server
     */
    public static final String serverShortName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serverShortName");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "serverShortName", _serverShortName);
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
    public static final void uuid(String uuid)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "uuid", uuid);

        _uuid = uuid;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "uuid");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.uuid
    //------------------------------------------------------------------------------
    /**
     * Gets the UUID of the current WAS server
     * 
     * @return String the UUID of the WAS server
     */
    public static final String uuid()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "uuid");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "uuid", _uuid);
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
    public static final String fqServerName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "fqServerName");

        String fqServerName = _serverName; // RLSUtils.FQHAMCompatibleServerName(_cellName,_nodeName,_serverName); tWAS

        if (tc.isEntryEnabled())
            Tr.exit(tc, "fqServerName", fqServerName);
        return fqServerName;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.orb
    //------------------------------------------------------------------------------
    /**
     * Sets the ORB reference
     * 
     * @param orb The ORB reference
     */
//  public static final void orb(org.omg.CORBA.ORB orb)
//  {
//    if (tc.isEntryEnabled()) Tr.entry(tc, "orb", orb);
//
//    _orb = orb;
//
//    if (tc.isEntryEnabled()) Tr.exit(tc, "orb");
//  }

    //------------------------------------------------------------------------------
    // Method: Configuration.orb
    //------------------------------------------------------------------------------
    /**
     * Gets the ORB reference.
     * 
     * @return org.omg.CORBA.ORB The ORB reference.
     */
//  public static final org.omg.CORBA.ORB orb()
//  {
//    if (tc.isEntryEnabled()) Tr.entry(tc, "orb");
//    if (tc.isEntryEnabled()) Tr.exit(tc, "orb", _orb);
//    return _orb;
//  }

    //------------------------------------------------------------------------------
    // Method: Configuration.HAEnabled
    //------------------------------------------------------------------------------
    /**
     * Sets the HAEnabled flag
     * 
     * @param HAEnabled The HAEnabled flag
     */
    public static final void HAEnabled(boolean HAEnabled)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "HAEnabled", new Boolean(HAEnabled));

        _HAEnabled = HAEnabled;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "HAEnabled");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.HAEnabled
    //------------------------------------------------------------------------------
    /**
     * Gets the HAEnabled flag
     * 
     * @return boolean name The HAEnabled flag
     */
    public static final boolean HAEnabled()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "HAEnabled");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "HAEnabled", new Boolean(_HAEnabled));
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
    public static final void localFailureScope(FailureScope localFailureScope)
    {
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
    public static final FailureScope localFailureScope()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "localFailureScope");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "localFailureScope", _localFailureScope);
        return _localFailureScope;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.isZOS
    //------------------------------------------------------------------------------
    /**
     * Sets the isZOS flag.
     * 
     * @param isZOS Flag to indicate if this is a zOS platform.
     */
    public static final void isZOS(boolean isZOS)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isZOS", new Boolean(isZOS));

        _isZOS = isZOS;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isZOS");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.isZOS
    //------------------------------------------------------------------------------
    /**
     * Gets the isZOS flag.
     * 
     * @return boolean Flag to indicate if this is a zOS platform.
     */
    public static final boolean isZOS()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isZOS");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "isZOS", new Boolean(_isZOS));
        return _isZOS;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.useFileLocking
    //------------------------------------------------------------------------------
    /**
     * Sets the useFileLocking flag.
     */
    public static final void useFileLocking(boolean useFileLocking)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "useFileLocking", new Boolean(useFileLocking));

        _useFileLocking = useFileLocking;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "useFileLocking");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.useFileLocking
    //------------------------------------------------------------------------------
    /**
     * Gets the useFileLocking flag.
     */
    public static final boolean useFileLocking()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "useFileLocking");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "useFileLocking", new Boolean(_useFileLocking));

        return _useFileLocking;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.txRecoveryAgent
    //------------------------------------------------------------------------------
    /**
  * 
  */
    public static void txRecoveryAgent(RecoveryAgent txRecoveryAgent)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "txRecoveryAgent", txRecoveryAgent);

        _txRecoveryAgent = txRecoveryAgent;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "txRecoveryAgent");
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.txRecoveryAgent
    //------------------------------------------------------------------------------
    /**
  * 
  */
    public static RecoveryAgent txRecoveryAgent()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "txRecoveryAgent");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "txRecoveryAgent", _txRecoveryAgent);

        return _txRecoveryAgent;
    }

    //------------------------------------------------------------------------------
    // Method: Configuration.setSnapshotSafe
    //------------------------------------------------------------------------------
    /**
  * 
  */
    public static final void setSnapshotSafe(boolean isSnapshotSafe)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setSnapshotSafe", new Boolean(isSnapshotSafe));

        _isSnapshotSafe = isSnapshotSafe;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setSnapshotSafe");
    }

    //---------------------------------------------------------------------------
    // Method: Configuration.setRecoveryLogComponent
    //---------------------------------------------------------------------------

    public static final void setRecoveryLogComponent(RecoveryLogComponent rls)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setRecoveryLogComponent", rls);
        _reclogComponent = rls;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setRecoveryLogComponent");
    }

    //---------------------------------------------------------------------------
    // Method: Configuration.getSetRecoveryLogComponent
    //---------------------------------------------------------------------------

    public static final RecoveryLogComponent getRecoveryLogComponent()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRecoveryLogComponent");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRecoveryLogComponent", _reclogComponent);
        return _reclogComponent;
    }

    //---------------------------------------------------------------------------
    // Method: Configuration.setAccessController    
    //---------------------------------------------------------------------------

    public static final void setAccessController(AccessController mgr)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setAccessController", mgr);
        _accessController = mgr;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setAccessController");
    }

    //---------------------------------------------------------------------------
    // Method: Configuration.getAccessController
    //---------------------------------------------------------------------------

    public static final AccessController getAccessController()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getAccessController");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getAccessController", _accessController);
        return _accessController;
    }

    //---------------------------------------------------------------------------
    // Method: Configuration.setAlarmManager    
    //---------------------------------------------------------------------------

    public static final void setAlarmManager(AlarmManager mgr)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setAlarmManager", mgr);
        _alarmManager = mgr;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setAlarmManager");
    }

    //---------------------------------------------------------------------------
    // Method: Configuration.getAlarmManager
    //---------------------------------------------------------------------------

    public static final AlarmManager getAlarmManager()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getAlarmManager");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getAlarmManager", _alarmManager);
        return _alarmManager;
    }

}
