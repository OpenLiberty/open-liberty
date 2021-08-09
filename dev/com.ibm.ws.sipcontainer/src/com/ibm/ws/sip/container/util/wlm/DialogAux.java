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
package com.ibm.ws.sip.container.util.wlm;

/** This is the type of objects stored in the
 * auxiliary slot of the SIP container's per-dialog
 * data.
 * @author dror yaffe
 *
 */
public interface DialogAux {

	/** The container calls this soon after the container
	 * notices that the dialog is over.
	 */
	public void done();
	
}
