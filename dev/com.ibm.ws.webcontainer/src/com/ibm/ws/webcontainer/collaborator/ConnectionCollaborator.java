/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.collaborator;

import com.ibm.ejs.j2c.HandleList;
import com.ibm.websphere.csi.CSIException;
import com.ibm.wsspi.webcontainer.collaborator.IConnectionCollaborator;

public class ConnectionCollaborator implements IConnectionCollaborator{
	
	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.collaborator.IConnectionCollaboratorHelper#preInvoke(boolean)
	 */
	public void preInvoke (HandleList hl,boolean isSingleThreadModel) throws CSIException{
		
	}
	/* (non-Javadoc)
	 * @see com.ibm.wsspi.webcontainer.collaborator.IConnectionCollaboratorHelper#postInvoke(boolean)
	 */
	public void postInvoke (HandleList hl,boolean isSingleThreadModel) throws CSIException{
		
	}
}
