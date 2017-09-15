/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer;

import com.ibm.wsspi.webcontainer.IPlatformHelper;

public class PlatformHelper implements IPlatformHelper {
	/* (non-Javadoc)
	 * @see com.ibm.ws.webcontainer.IPlatformHelper#securityIdentityPush()
	 */
	public Object securityIdentityPush() {return null;}
	/* (non-Javadoc)
	 * @see com.ibm.ws.webcontainer.IPlatformHelper#securityIdentityPop(java.lang.Object)
	 */
	public void securityIdentityPop(Object o) {}
	/* (non-Javadoc)
	 * @see com.ibm.ws.webcontainer.IPlatformHelper#getServerID()
	 */
	public String getServerID() {return null;}
	/* (non-Javadoc)
	 * @see com.ibm.ws.webcontainer.IPlatformHelper#isSyncToThreadPlatform()
	 */
	public boolean isSyncToThreadPlatform (){
    	return false;
    }
    /* (non-Javadoc)
	 * @see com.ibm.ws.webcontainer.IPlatformHelper#isDecodeURIPlatform()
	 */
    public boolean isDecodeURIPlatform (){
    	return true;
    }
	public boolean isTransferToOS() {
		// TODO Auto-generated method stub
		return false;
	}
}
