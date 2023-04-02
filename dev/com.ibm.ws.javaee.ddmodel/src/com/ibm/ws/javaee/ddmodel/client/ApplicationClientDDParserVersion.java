/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.client;

import com.ibm.ws.javaee.dd.client.ApplicationClient;

public abstract class ApplicationClientDDParserVersion {
    /**
     * Service property name with a value corresponding to {@link ApplicationClient#getVersionID} that
     * indicates the maximum version that the runtime supports.
     */
    public static final String VERSION = "version";
}
