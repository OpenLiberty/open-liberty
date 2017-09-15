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
package com.ibm.wsspi.webcontainer;

public interface IPlatformHelper {

	public abstract Object securityIdentityPush();

	public abstract void securityIdentityPop(Object o);

	public abstract String getServerID();

	//change class to abstract and this method to public static boolean class member?
	public abstract boolean isSyncToThreadPlatform();

	public abstract boolean isDecodeURIPlatform();
	public abstract boolean isTransferToOS();

}