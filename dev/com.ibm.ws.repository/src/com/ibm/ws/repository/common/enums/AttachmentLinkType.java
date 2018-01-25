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
package com.ibm.ws.repository.common.enums;

/**
 * Denotes the meaning of the attachment URL
 * <p>
 * Note that when the attachment content is to be stored in the repository server, the attachment link type should be unset.
 */
public enum AttachmentLinkType {

    /**
     * @deprecated Not used
     */
    EFD,

    /**
     * URL links directly to the attachment file
     */
    DIRECT,

    /**
     * URL links to a web page where the attachment file is available
     */
    WEB_PAGE;
}