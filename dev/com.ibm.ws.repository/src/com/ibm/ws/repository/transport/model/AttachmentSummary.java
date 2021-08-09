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

package com.ibm.ws.repository.transport.model;

import java.io.File;
import java.util.Locale;

public interface AttachmentSummary {
    public String getName();

    public File getFile(); // One of getFile() and getURL() must return null. 

    public String getURL(); // The other must not return null. 

    public Attachment getAttachment();

    public Locale getLocale();
}
