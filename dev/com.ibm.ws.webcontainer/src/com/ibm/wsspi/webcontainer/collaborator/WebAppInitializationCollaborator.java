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