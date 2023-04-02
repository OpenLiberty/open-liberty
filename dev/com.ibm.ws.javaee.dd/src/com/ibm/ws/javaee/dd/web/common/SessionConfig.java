/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.javaee.dd.web.common;

import java.util.List;

/**
 *
 */
public interface SessionConfig {

    static enum TrackingModeEnum {
        // lexical value must be (COOKIE|URL|SSL)
        COOKIE,
        URL,
        SSL;
    }

    /**
     * @return true if &lt;session-timeout> is specified
     * @see #getSessionTimeout
     */
    boolean isSetSessionTimeout();

    /**
     * @return &lt;session-timeout> if specified
     * @see #isSetSessionTimeout
     */
    int getSessionTimeout();

    /**
     * @return &lt;cookie-config>, or null if unspecified
     */
    CookieConfig getCookieConfig();

    /**
     * @return &lt;tracking-mode> as a read-only list
     */
    List<TrackingModeEnum> getTrackingModeValues();
}
