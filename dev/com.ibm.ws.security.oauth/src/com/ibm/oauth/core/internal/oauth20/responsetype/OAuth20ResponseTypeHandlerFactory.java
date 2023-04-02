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
package com.ibm.oauth.core.internal.oauth20.responsetype;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.internal.oauth20.config.OAuth20ConfigProvider;

public interface OAuth20ResponseTypeHandlerFactory {
    public void init(OAuthComponentConfiguration oldconfig);

    public OAuth20ResponseTypeHandler getHandler(
            String responseType, OAuth20ConfigProvider config)
            throws OAuthException;
}
