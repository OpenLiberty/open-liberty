/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.internal;

import com.ibm.wsspi.security.token.ValidationResult;
import com.ibm.wsspi.security.token.WSSecurityPropagationHelper;

/**
 * @see com.ibm.wsspi.security.token.ValidationResult
 *
 * @author IBM Corp.
 * @version 7.0.0
 * @since 7.0.0
 * @ibm-spi
 *
 */
public class ValidationResultImpl implements ValidationResult {

    private final String uniqueId;

    public ValidationResultImpl(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public String getRealmFromUniqueId() {
        String result = WSSecurityPropagationHelper.getRealmFromUniqueID(uniqueId);
        return result;
    }

    @Override
    public String getUniqueId() {
        String result = uniqueId;
        return result;
    }

    @Override
    public String getUserFromUniqueId() {
        String result = WSSecurityPropagationHelper.getUserFromUniqueID(uniqueId);
        return result;
    }

    // Not support
    @Override
    public boolean requiresLogin() {
        return false;
    }

    @Override
    public String toString() {
        return super.toString() + " uniqueId = " + uniqueId;
    }
}
