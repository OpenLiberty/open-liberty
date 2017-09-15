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

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: FileFailureScope
//------------------------------------------------------------------------------
/**
 * <p>
 * The "failure scope" is defined as a potential region of failure (such as an
 * application server) All operations that take place within the WebSphere
 * deployment do so under a given failure scope. For example, a transaction
 * running on application server 1 is operating under the failure scope for
 * server 1.
 * </p>
 * 
 * <p>
 * This implementation provides the WebSphere Distributed version of a failure
 * scope.
 * </p>
 */
public class FileFailureScope implements FailureScope
{
    /**
     * WebSphere RAS TraceComponent registration.
     */
    private static final TraceComponent tc = Tr.register(FileFailureScope.class,
                                                         TraceConstants.TRACE_GROUP, null);

    /**
     * The name of the server that this failure scope represents.
     */
    protected String _serverName;

    /**
     * The hash code value use during map operations. We currently use the hash
     * code of the server name string.
     */
    private final int _hashCode;

    /**
     * Cached string representation of this failure scope. Used in toString. This
     * contains the server name and hashcode values.
     */
    private final String _stringForm;

    private LeaseInfo _leaseInfo = null;

    /**
     * @return the _leaseInfo
     */
    public LeaseInfo getLeaseInfo() {
        return _leaseInfo;
    }

    //------------------------------------------------------------------------------
    // Method: FileFailureScope.FileFailureScope
    //------------------------------------------------------------------------------
    /**
     * Constructor to create a new failure scope that encompasses the current point
     * of execution. In WebSphere distributed, this means the current server.
     */
    public FileFailureScope()
    {
        this(Configuration.fqServerName());

        if (tc.isEntryEnabled())
            Tr.entry(tc, "FileFailureScope");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "FileFailureScope", this);
    }

    //------------------------------------------------------------------------------
    // Method: FileFailureScope.FileFailureScope
    //------------------------------------------------------------------------------
    /**
     * Constructor to create a new failure scope that represents the specified servers
     * execution scope.
     * 
     * @param serverName The target server name
     */
    public FileFailureScope(String serverName)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "FileFailureScope", new Object[] { serverName });

        _serverName = serverName;
        _hashCode = _serverName.hashCode();

        char[] chars = _serverName.toCharArray();

        _stringForm = "FileFailureScope: " + _serverName + " [" + _hashCode + "]";

        if (tc.isEntryEnabled())
            Tr.exit(tc, "FileFailureScope", this);
    }

    public FileFailureScope(String serverName, LeaseInfo leaseInfo)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "FileFailureScope", new Object[] { serverName, leaseInfo });

        _serverName = serverName;
        _leaseInfo = leaseInfo;
        _hashCode = _serverName.hashCode();

        char[] chars = _serverName.toCharArray();

        _stringForm = "FileFailureScope: " + _serverName + " [" + _hashCode + "]";

        if (tc.isEntryEnabled())
            Tr.exit(tc, "FileFailureScope", this);
    }

    //------------------------------------------------------------------------------
    // Method: FileFailureScope.isContainedBy
    //------------------------------------------------------------------------------
    /**
     * Returns true if the target failure scope is encompassed by failureScope. For
     * example, if the target failure scope identifies a server region inside a z/OS
     * scalable server identified by failureScope then this method returns true.
     * 
     * @param failureScope Failure scope to test
     * 
     * @return boolean Flag indicating if the target failure scope is contained by the
     *         specified failure scope
     */
    @Override
    public boolean isContainedBy(FailureScope failureScope)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isContainedBy", new Object[] { failureScope, this });

        // Since the failure scope is always an application server only the same
        // failure scope, i.e. one that is equal, can be considered to contain
        // this failure scope. 
        final boolean contains = equals(failureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isContainedBy", new Boolean(contains));
        return contains;
    }

    //------------------------------------------------------------------------------
    // Method: FileFailureScope.equals
    //------------------------------------------------------------------------------
    /**
     * Returns true if the target failure scope represents the same logcial failure
     * scope as the supplied failure scope.
     * 
     * @param anotherScope Failure scope to test
     * 
     * @return boolean Flag indicating if the target failure scope represents the
     *         same logical failure scope as the specified failure scope.
     */
    @Override
    public boolean equals(Object anotherScope)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "equals", new Object[] { anotherScope, this });

        boolean result = false;

        // If they are the same object instance then they must be the same
        // failure scope.
        if (anotherScope == this)
        {
            result = true;
        }
        else if (anotherScope instanceof FileFailureScope)
        {
            // They are different physical instances, but they can still
            // represent the same scope. Compare the server names to find
            // out if they are logically the same.
            if (((FileFailureScope) anotherScope)._serverName.equals(_serverName))
            {
                result = true;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "equals", new Boolean(result));
        return result;
    }

    //------------------------------------------------------------------------------
    // Method: FileFailureScope.hashCode
    //------------------------------------------------------------------------------
    /**
     * Returns a hashing code for the target failure scope.
     * 
     * @return int The hashing code
     */
    @Override
    public int hashCode()
    {
        return _hashCode;
    }

    //------------------------------------------------------------------------------
    // Method: FileFailureScope.toString
    //------------------------------------------------------------------------------
    /**
     * Returns a string representation of the target failure scope.
     * 
     * @return String string representation of the target failure scope.
     */
    @Override
    public String toString()
    {
        return _stringForm;
    }

    //------------------------------------------------------------------------------
    // Method: FileFailureScope.serverName
    //------------------------------------------------------------------------------
    /**
     * Returns the server name that this failure scope represents
     * 
     * @return String server name
     */
    @Override
    public String serverName()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "serverName", this);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "serverName", _serverName);
        return _serverName;
    }

    //------------------------------------------------------------------------------
    // Method: FileFailureScope.isSameExecutionZone
    //------------------------------------------------------------------------------
    /**
     * Returns true if this failure scope represents the same general recovery scope as
     * the input parameter. For instance, if more than one FailureScope was created
     * which referenced the same server, they would be in the same execution zone.
     * 
     * @param anotherScope Failure scope to test
     * 
     * @return boolean Flag indicating if the target failure scope represents the
     *         same logical failure scope as the specified failure scope.
     */
    @Override
    public boolean isSameExecutionZone(FailureScope anotherScope)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isSameExecutionZone", anotherScope);

        boolean isSameZone = equals(anotherScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isSameExecutionZone", new Boolean(isSameZone));

        return isSameZone;
    }
}
