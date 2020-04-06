/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.load;

/**
 * 
 * @author anat
 * Class which will implement this interface will receive
 * concurrent updates from counters which can change server load 
 * whenever it is needed.
 */
public interface ConcurentLoadListener {

	/**
	 * 
	 * @param counter
	 * @param currentLoad - passes the information about load which caused for 
	 * this new weight.
	 */
	public void setNewWeight(Weighable counter,long currentLoad);
}
