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
package com.ibm.ws.repository.common.enums;

/**
 * The attachment type, which can be of type THUMBNAIL, ILLUSTRATION, DOCUMENTATION, CONTENT, LICENSE, LICENSE_INFORMATION,
 * LICENSE_AGREEMENT
 */
public enum AttachmentType {
    THUMBNAIL,
    ILLUSTRATION,
    DOCUMENTATION,
    CONTENT,
    LICENSE, // Used for the aggregate LI+LA.html
    LICENSE_INFORMATION,
    LICENSE_AGREEMENT;
}