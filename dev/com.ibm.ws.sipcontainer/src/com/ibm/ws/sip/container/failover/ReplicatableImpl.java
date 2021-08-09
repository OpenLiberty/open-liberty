/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.failover;

/**
 * A common implementation for a Replicatable.
 * 
 * @author Nitzan Nissim
 *
 */
abstract public class ReplicatableImpl implements Replicatable {
	/**Common ID shared by SAS elements*/
	private String _sharedID;
	/**Indicates that a change was done to this Replicatable and was not updated in storage*/
	private boolean isDirty;
	/**An identification for the current server. Will be a prefix for the shared ID*/
	private String serverID; 
	
	/**
	 * @return the server ID
	 */
	public String getServerID() {
		return serverID;
	}

	/**
	 * Sets the server id
	 * @param serverID
	 */
	public void setServerID(String serverID) {
		this.serverID = serverID;
	}
	
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public String getSharedId() {
    	
    	return _sharedID;
    }
	
	/**
	 * 
	 */
	@Override
	public void setSharedId(String id){
		_sharedID = id;
	}
	
	public void setDirty(){
		isDirty = true;
	}
	
	public void unsetDirty(){
		isDirty = false;
	}

//	@Override
//	public void store() {
////		TODO Liberty generic storing per storage (and replicatable) type
//		
//	}
//	
//	@Override
//	public void removeFromStorage(){
////		TODO Liberty generic storing per storage (and replicatable) type
//	}
}
