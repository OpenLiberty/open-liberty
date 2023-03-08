/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.credentials.saf;

import java.util.List;

import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;

/**
 *
 */
public interface SAFGroupCredential {

    /**
     * Retrieve the MVS groups this SAFCredential was mapped to. If the credential was not mapped,
     * the unmapped groups are returned.
     *
     * @throws RegistryException
     * @throws EntryNotFoundException
     */
    List<String> getMvsGroupIds() throws CustomRegistryException, EntryNotFoundException;

}
