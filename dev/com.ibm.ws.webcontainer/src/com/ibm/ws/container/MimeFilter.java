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

public class MimeFilter
{
	String type;
	String target;
	
	public MimeFilter(String type, String target)
	{
		this.type = type;
		this.target = target;
	}
	/**
	 * Returns the target.
	 * @return String
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Returns the type.
	 * @return String
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the target.
	 * @param target The target to set
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * Sets the type.
	 * @param type The type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

}
