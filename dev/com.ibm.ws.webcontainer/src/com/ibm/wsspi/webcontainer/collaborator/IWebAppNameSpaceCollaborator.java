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
package com.ibm.wsspi.webcontainer.collaborator;

public interface IWebAppNameSpaceCollaborator {

	// Added LIDB1181.2
	// LIDB1181.2.4 - modified to accept a ComponentMetaData object
	public void preInvoke(Object compMetaData);

	public void postInvoke();

}