package com.ibm.ws.sib.msgstore.persistence;

import java.sql.Timestamp;

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

public class MELockOwner
{
    private String _meUUID;
    private String _incUUID;
    private int    _version;
    private int    _migrationVersion;
    private Timestamp _dbLastTimestamp; //R000382
    
    private String _meName;
    private Timestamp _dbCurrentTimestamp; //R000382
    private String _meStatus; //R000382
    
    public MELockOwner(String meUUID, String incUUID, int version, int migrationVersion, String meName, Timestamp dbLastTimestamp, String meInfo, Timestamp dbCurrentTimestamp, String meStatus)
    {
        _meUUID           = meUUID;
        _incUUID          = incUUID;
        _version          = version;
        _migrationVersion = migrationVersion;
        _meName           = meName;
        _dbLastTimestamp  = dbLastTimestamp; //R000382
        
        _dbCurrentTimestamp      = dbCurrentTimestamp; //R000382
        _meStatus = meStatus; //R000382
    }
      public MELockOwner(String meUUID, String incUUID, int version, int migrationVersion, String meName)
    {
        _meUUID           = meUUID;
        _incUUID          = incUUID;
        _version          = version;
        _migrationVersion = migrationVersion;
        _meName           = meName;
        _dbLastTimestamp  = null;         
        _dbCurrentTimestamp  = null; 
        _meStatus = "";
    }

    public String getMeUUID()
    {
        return _meUUID;
    }

    public String getIncUUID()
    {
        return _incUUID;
    }

    public int getVersion()
    {
        return _version;
    }

    public int getMigrationVersion()
    {
        return _migrationVersion;
    }
    
    public Timestamp getDBLastTimestamp() 
    {
	    return _dbLastTimestamp;
	}
    
	public String getMeName()
    {
        return _meName;
    }
	
    public Timestamp getDBCurrentTimestamp()
	{
		return _dbCurrentTimestamp;
	}
	
    public String getMeStatus()
	{   
		return _meStatus;
    }
	
    public void setMeStatus(String meStatus)
    {   
    		_meStatus = meStatus;
    }

    public String toString()
    {
        StringBuffer reply = new StringBuffer("MELockOwner[ME_UUID=");
        reply.append(_meUUID);
        reply.append(", INC_UUID=");
        reply.append(_incUUID);
        reply.append(", VERSION=");
        reply.append(_version);
        reply.append(", MIGRATION_VERSION=");
        reply.append(_migrationVersion);
        reply.append(",LAST_TIMESTAMP="); //R000382
        reply.append(_dbLastTimestamp); //R000382
        reply.append(", ME_NAME=");
        reply.append(_meName);
        reply.append(", CURRENT_TIMESTAMP="); //R000382
        reply.append(_dbCurrentTimestamp); //R000382
        reply.append(", ME_STATUS="); //R000382
        reply.append(_meStatus); //R000382
        reply.append("]");

        return reply.toString();
    }
}
