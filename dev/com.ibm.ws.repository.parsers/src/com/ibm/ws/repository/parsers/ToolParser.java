/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.repository.parsers;

import java.io.File;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.resources.writeable.ToolResourceWritable;

public class ToolParser extends ProductRelatedJarParser<ToolResourceWritable> {
    @Override
    public ResourceType getType(String contentType, File archive) {
        return ResourceType.TOOL;
    }
}
