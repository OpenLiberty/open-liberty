/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.service;

import com.ibm.ws.webcontainer.osgi.webapp.AppInstaller;

/**
 * @author rbackhouse
 */
public interface AppInstallService
{
  public boolean installAppForUri(String uri, AppInstaller installer);
}
