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

package com.ibm.ws.repository.transport.client;

import com.ibm.ws.repository.transport.exceptions.BadVersionException;

/**
 * Classes implementing this interface understand and obey the limited semantic versioning
 * rules we're implementing for Repository / Massive content-things.
 *
 */
public interface VersionableContent {

    void validate(String version) throws BadVersionException;

    String nameOfVersionAttribute();
}
