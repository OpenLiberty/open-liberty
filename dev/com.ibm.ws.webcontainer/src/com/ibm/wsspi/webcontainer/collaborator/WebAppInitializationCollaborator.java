/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
package com.ibm.wsspi.webcontainer.collaborator;

import com.ibm.ws.webcontainer.spiadapter.collaborator.IInitializationCollaborator;
import com.ibm.wsspi.adaptable.module.Container;

public interface WebAppInitializationCollaborator extends IInitializationCollaborator 
{
	public void starting(Container moduleContainer);
	public void started(Container moduleContainer);
	public void stopping(Container moduleContainer);
	public void stopped(Container moduleContainer);
}