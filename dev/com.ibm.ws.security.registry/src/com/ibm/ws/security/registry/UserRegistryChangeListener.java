/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry;

/**
 * Notification mechanism to inform components that need to respond to
 * user registry configuration changes that a change has occurred.
 * <p>
 * Note knowledge of the user registry configuration is only required in
 * specific cases and should be avoided whenever possible.
 */
public interface UserRegistryChangeListener {

    /**
     * Callback method to be invoked by the UserRegistryService upon a
     * user registry configuration change.
     * <p>
     * Implementation note: Currently <b>any</b> change to <b>any</b>
     * user registry configuration triggers this change. This logic may
     * be improved for multi-domain.
     */
    void notifyOfUserRegistryChange();
}
