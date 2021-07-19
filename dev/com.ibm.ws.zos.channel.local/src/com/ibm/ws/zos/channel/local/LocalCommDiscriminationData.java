/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local;

/**
 * Data used by upstream channels (e.g WOLA) to determine whether or not they
 * will accept a new local comm connection.
 */
public interface LocalCommDiscriminationData {
    
	/**
	 * WOLA protocol (the only known protocol at this time).
	 */
	public int WOLA_PROTOCOL = 1;
	
	/** 
	 * @return the protocol used on the connection. 
	 */
	public int getProtocol();
}
