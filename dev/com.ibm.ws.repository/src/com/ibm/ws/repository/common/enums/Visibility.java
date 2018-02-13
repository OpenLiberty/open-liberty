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
 * Enumeration of the available visibility settings for a feature
 *
 * @see <a href="https://www-01.ibm.com/support/knowledgecenter/SSEQTP_8.5.5/com.ibm.websphere.wlp.doc/ae/rwlp_feat_definition.html?lang=en">IBM Knowledge Center</a>
 */
public enum Visibility {
    /**
     * Feature is considered API, can be included in server.xml
     */
    PUBLIC,

    /**
     * Feature is considered internal, should not be relied upon and may change at any time
     */
    PRIVATE,

    /**
     * Feature is considered SPI, can be relied upon by other features but cannot be specified in server.xml
     */
    PROTECTED,

    /**
     * Feature is only used as a shortcut at install time to install all of its dependencies. Can never be started in a running server.
     */
    INSTALL
}
