/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer;

import com.ibm.ws.container.DeployedModule;
import com.ibm.ws.webcontainer.exception.WebAppNotLoadedException;

public interface IWebContainer {
	public void addWebApplication(DeployedModule deployedModule, boolean production) throws WebAppNotLoadedException ;
}
