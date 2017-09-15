/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin;

/**
 * @author philip
 * 
 *         To change this generated comment edit the template variable "typecomment":
 *         Window>Preferences>Java>Templates.
 *         To enable and disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public interface JsMain {

    public void initialize(JsMEConfig config) throws Exception;

    public void start() throws Exception;

    public void destroy() throws Exception;

    public void createDestinationLocalization(BaseDestination config) throws Exception;

    public void alterDestinationLocalization(BaseDestination config) throws Exception;

    public void deleteDestinationLocalization(BaseDestination config) throws Exception;

    public void reloadEngine(long highMessageThreshold) throws Exception;

}
