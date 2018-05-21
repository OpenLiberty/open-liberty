/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

/**
 * Notification mechanism to inform components that need to respond to
 * webAppSecurity configuration changes that a change has occurred.
 * <p>
 * At this point, this is used for detecting the condition which requires
 * recycleing JSR375 applications.
 */
public interface WebAppSecurityConfigChangeListener {

    /**
     * Callback method to be invoked by the WebAppConfig service upon a
     * webAppSecurity configuration change.
     * <p>
     * Implementation note: Currently <b>any</b> change to <b>any</b>
     * user registry configuration triggers this change. This logic may
     * be improved for multi-domain.
     */
    void notifyWebAppSecurityConfigChanged(WebAppSecurityConfigChangeEvent event);
}
