/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.osgi.service.component.ComponentContext;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

/**
 *
 */
public class LibertyRecoveryDirectorImpl extends RecoveryDirectorImpl
{
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(LibertyRecoveryDirectorImpl.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    /**
     * A reference to the singleton instance of the WSRecoveryDirectorImpl class.
     */
    private static LibertyRecoveryDirectorImpl _instance;
    private static RecoveryLogFactory theRecoveryLogFactory = null;

    /**
     * Private constructor for creation of the singleton LibertyRecoveryDirectorImpl class.
     * Internal code may access this instance via the LibertyRecoveryDirectorImpl.instance()
     * method. Client services may access this instance via the RecoveryDirectorFactory.
     * recoveryDirector() method.
     */
    public LibertyRecoveryDirectorImpl()
    {
        super();

        if (theRecoveryLogFactory != null)
        {
            String className = theRecoveryLogFactory.getClass().getName();
            _customLogFactories.put(className, theRecoveryLogFactory);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "LibertyRecoveryDirectorImpl: setting RecoveryLogFactory, " + theRecoveryLogFactory + "for classname, " + className);
        }
        else if (tc.isDebugEnabled())
            Tr.debug(tc, "LibertyRecoveryDirectorImpl: the RecoveryLogFactory is null");

        if (tc.isDebugEnabled())
            Tr.debug(tc, "LibertyRecoveryDirectorImpl", this);
    }

    //------------------------------------------------------------------------------
    // Method: LibertyRecoveryDirectorImpl.instance
    //------------------------------------------------------------------------------
    /**
     * Create or lookup the singleton instance of the LibertyRecoveryDirectorImpl class. This
     * method is intended for internal use only. Client services should access this
     * instance via the RecoveryDirectorFactory.recoveryDirector() method.
     * 
     * @return The singleton instance of the WSRecoveryDirectorImpl class.
     */
    public static synchronized RecoveryDirector instance()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "instance");

        if (_instance == null)
        {
            _instance = new LibertyRecoveryDirectorImpl();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "instance", _instance);
        return _instance;
    }

    /*
     * Called by DS to activate service
     */
    protected void activate(ComponentContext cc) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "activate", this);
    }

    // methods to handle dependency injection in osgi environment
    protected void setRecoveryLogFactory(RecoveryLogFactory fac) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setRecoveryLogFactory, factory: " + fac, this);
        theRecoveryLogFactory = fac;

    }

    protected void unsetRecoveryLogFactory(RecoveryLogFactory fac) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "unsetRecoveryLogFactory, factory: " + fac, this);
    }

    public static void reset()
    {
        if (tc.isEntryEnabled())
            Tr.exit(tc, "reset");
        _instance = null;
    }

    public void drivePeerRecovery() throws RecoveryFailedException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "drivePeerRecovery", this);
        RecoveryAgent libertyRecoveryAgent = null;

        // Get peers from the table

        // Use configuration to determine if recovery is local (for z/OS).
        final FailureScope localFailureScope = Configuration.localFailureScope(); /* @LI1578-22A */
        Tr.audit(tc, "WTRN0108I: " +
                     localFailureScope.serverName() + " checking to see if any peers need recovering");
        ArrayList<String> peersToRecover = null;

        // Extract the 'values' collection from the _registeredRecoveryAgents map and create an iterator
        // from it. This iterator will return ArrayList objects each containing a set of RecoveryAgent
        // objects. Each ArrayList corrisponds to a different sequence priority value.
        final Collection registeredRecoveryAgentsValues = _registeredRecoveryAgents.values();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "work with RA values: " + registeredRecoveryAgentsValues + ", collection size: " + registeredRecoveryAgentsValues.size(), this);
        Iterator registeredRecoveryAgentsValuesIterator = registeredRecoveryAgentsValues.iterator();
        while (registeredRecoveryAgentsValuesIterator.hasNext())
        {
            // Extract the next ArrayList and create an iterator from it. This iterator will return RecoveryAgent
            // objects that are registered at the same sequence priority value.
            final ArrayList registeredRecoveryAgentsArray = (java.util.ArrayList) registeredRecoveryAgentsValuesIterator.next();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "work with Agents array: " + registeredRecoveryAgentsArray + ", of size: " + registeredRecoveryAgentsArray.size(), this);
            final Iterator registeredRecoveryAgentsArrayIterator = registeredRecoveryAgentsArray.iterator();

            while (registeredRecoveryAgentsArrayIterator.hasNext())
            {
                // Extract the next RecoveryAgent object
                final RecoveryAgent recoveryAgent = (RecoveryAgent) registeredRecoveryAgentsArrayIterator.next();

                //TODO: This is a bit hokey. Can we safely assume that there is just the one RecoveryAgent in a Liberty environment?
                libertyRecoveryAgent = recoveryAgent;

                String recoveryGroup = libertyRecoveryAgent.getRecoveryGroup();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "work with Agent: " + recoveryAgent + " and recoveryGroup " + recoveryGroup, this);
                peersToRecover = recoveryAgent.processLeasesForPeers(localFailureScope.serverName(), recoveryGroup);
            }
        }

        if (peersToRecover != null && !peersToRecover.isEmpty() && libertyRecoveryAgent != null)
            peerRecoverServers(libertyRecoveryAgent, localFailureScope.serverName(), peersToRecover);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "drivePeerRecovery");
    }

    public void peerRecoverServers(RecoveryAgent recoveryAgent, String myRecoveryIdentity, ArrayList<String> peersToRecover) throws RecoveryFailedException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "peerRecoverServers", new Object[] { recoveryAgent, myRecoveryIdentity, peersToRecover });

        for (String peerRecoveryIdentity : peersToRecover)
        {

            try
            {
                //Read lease check if it is still expired. If so, then update lease and proceed to peer recover
                // if not still expired (someone else has grabbed it) then bypass peer recover.
                LeaseInfo leaseInfo = new LeaseInfo();
                if (recoveryAgent.claimPeerLeaseForRecovery(peerRecoveryIdentity, myRecoveryIdentity, leaseInfo))
                {

                    // drive directInitialization(**retrieved scope**);
                    Tr.audit(tc, "WTRN0108I: " +
                                 "PEER RECOVER server with recovery identity " + peerRecoveryIdentity);
                    //String peerServerName = "Cell\\Node\\cloud002";
                    FileFailureScope peerFFS = new FileFailureScope(peerRecoveryIdentity, leaseInfo);

                    directInitialization(peerFFS);
                }
                else
                {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Failed to claim lease for peer", this);
                }
            } catch (Exception exc)
            {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "peerRecoverServers", exc);
                throw new RecoveryFailedException(exc);
            }

        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "peerRecoverServers");
    }
}
