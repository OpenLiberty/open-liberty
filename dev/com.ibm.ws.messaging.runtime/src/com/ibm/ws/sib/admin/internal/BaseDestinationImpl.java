/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin.internal;

import com.ibm.ws.sib.admin.BaseDestination;

public class BaseDestinationImpl implements BaseDestination {

	protected String uuid = null;
	protected String name = null;
	protected boolean isAlias = false;
	protected boolean isLocal = true;
	
	public BaseDestinationImpl() {
		
	}
	
	public BaseDestinationImpl(String name,boolean isAlias,boolean isLocal) {
		this.name = name;
		this.isAlias = isAlias;
		this.isLocal = isLocal;
	}
	
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getUUID() {
		// TODO Auto-generated method stub
		return uuid;
	}

	@Override
	public boolean isAlias() {
		// TODO Auto-generated method stub
		return isAlias;
	}

	@Override
	public boolean isLocal() {
		// TODO Auto-generated method stub
		return isLocal;
	}

	@Override
	public void setAlias(boolean isAlias) {
		this.isAlias = isAlias; 

	}

	@Override
	public void setLocal(boolean isLocal) {
		this.isLocal = isLocal;

	}

	@Override
	public void setName(String name) {
		this.name = name;

	}

	@Override
	public void setUUID(String value) {
		this.uuid = value;

	}

}
