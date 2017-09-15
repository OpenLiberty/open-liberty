/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container;

public class TagLibRef
{
	private String uri;
	private String location;
	
	public TagLibRef(String uri, String location)
	{
		this.uri = uri;
		this.location = location;
	}
	
	/**
	 * Returns the location.
	 * @return String
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Returns the uri.
	 * @return String
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Sets the location.
	 * @param location The location to set
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * Sets the uri.
	 * @param uri The uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

}
