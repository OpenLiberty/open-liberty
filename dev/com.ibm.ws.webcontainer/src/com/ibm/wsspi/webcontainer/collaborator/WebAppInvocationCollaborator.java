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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;

/**
 * LIBERTY: rather than extend multiple internal interfaces, all the methods are moved 
 * into this interface so it is now the only one needed (and the only one supported).
 * @ibm-private-in-use
 */
public interface WebAppInvocationCollaborator 
{
  public void preInvoke(WebComponentMetaData metaData);
  public void postInvoke(WebComponentMetaData metaData);
  public void preInvoke(WebComponentMetaData metaData, ServletRequest req, ServletResponse res);
  public void postInvoke(WebComponentMetaData metaData, ServletRequest req, ServletResponse res);
}

