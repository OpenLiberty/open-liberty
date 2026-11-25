/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.request.logging;

/**
 * ZosRequestLogging Saf service interface.
 *
 */
public interface ZosRequestLoggingSafService {
    /**
     * Returns the mapped user name.
     *
     * @return The mapped user name. Null if not mapped.
     */
    public String getMappedUserName();

}
