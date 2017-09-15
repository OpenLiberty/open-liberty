/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.management.j2ee;


/**
 * The WebModule model identifies a deployed WAR module.
 */
public interface WebModuleMBean extends J2EEModuleMBean {

    /**
     * A list of servlets contained in the deployed WAR module. For each servlet
     * contained in the deployed WAR module there must be one Servlet
     * OBJECT_NAME in the servlets list that identifies it.
     */
    String[] getservlets();
}
