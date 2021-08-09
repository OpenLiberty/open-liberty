package com.ibm.ws.sib.msgstore.persistence.objectManager;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.ws.objectManager.ManagedObject;

public class MEStoredOwner extends ManagedObject
{
    private static final long serialVersionUID = -4603482375570542419L;

    private String _meUUID;
    private String _incUUID;
    private int    _version;
    private int    _migrationVersion;


    public MEStoredOwner(String meUUID, String incUUID, int version, int migrationVersion)
    {
        _meUUID           = meUUID;
        _incUUID          = incUUID;
        _version          = version;
        _migrationVersion = migrationVersion;
    }

    public String getMeUUID()
    {
        return _meUUID;
    }

    public String getIncUUID()
    {
        return _incUUID;
    }

    public void setIncUUID(String incUUID)
    {
        _incUUID = incUUID;
    }

    public int getVersion()
    {
        return _version;
    }

    public int getMigrationVersion()
    {
        return _migrationVersion;
    }

    public void becomeCloneOf(ManagedObject other)
    {
        MEStoredOwner otherME = (MEStoredOwner)other;

        _meUUID           = otherME._meUUID;
        _incUUID          = otherME._incUUID;
        _version          = otherME._version;
        _migrationVersion = otherME._migrationVersion;
    }

    public String toString()
    {
        StringBuffer reply = new StringBuffer("MEStoredOwner[ME_UUID=");
        reply.append(_meUUID);
        reply.append(", INC_UUID=");
        reply.append(_incUUID);
        reply.append(", VERSION=");
        reply.append(_version);
        reply.append(", MIGRATION_VERSION=");
        reply.append(_migrationVersion);
        reply.append("]");

        return reply.toString();
    }
}
