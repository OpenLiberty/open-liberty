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
package com.ibm.ws.ejbcontainer.security.internal;

import java.util.Map;

/**
 * Represents security configurable options for EJB.
 */
class EJBSecurityConfigImpl implements EJBSecurityConfig {

    protected static final String CFG_KEY_USE_UNAUTH_FOR_EXPIRED_CREDS = "useUnauthenticatedForExpiredCredentials";
    protected static final String CFG_KEY_REALM_QUALIFY_USER_NAME = "useRealmQualifiedUserNames";

    // New attributes must update getChangedProperties method
    private final Boolean useUnauthenticatedForExpiredCredentials;
    private final Boolean useRealmQualifiedUserNames;

    EJBSecurityConfigImpl(Map<String, Object> newProperties) {
        useUnauthenticatedForExpiredCredentials = (Boolean) newProperties.get(CFG_KEY_USE_UNAUTH_FOR_EXPIRED_CREDS);
        useRealmQualifiedUserNames = (Boolean) newProperties.get(CFG_KEY_REALM_QUALIFY_USER_NAME);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getUseUnauthenticatedForExpiredCredentials() {
        return useUnauthenticatedForExpiredCredentials;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getUseRealmQualifiedUserNames() {
        return useRealmQualifiedUserNames;
    }

    private void appendToBufferIfDifferent(StringBuffer buffer, String name, Object thisValue, Object otherValue) {
        if ((thisValue != otherValue) && (thisValue != null) && (!thisValue.equals(otherValue))) {
            if (buffer.length() > 0) {
                buffer.append(",");
            }
            buffer.append(name);
            buffer.append("=");
            buffer.append(thisValue.toString());
        }
    }

    /**
     * {@inheritDoc}<p>
     * This method needs to be maintained when new attributes are added.
     * Order should be presented in alphabetical order.
     */
    @Override
    public String getChangedProperties(EJBSecurityConfig original) {
        // Bail out if it is the same object, or if this isn't of the right type.
        if (this == original) {
            return "";
        }
        if (!(original instanceof EJBSecurityConfigImpl)) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        EJBSecurityConfigImpl orig = (EJBSecurityConfigImpl) original;
        appendToBufferIfDifferent(buf, "useUnauthenticatedForExpiredCredentials",
                                  this.useUnauthenticatedForExpiredCredentials, orig.useUnauthenticatedForExpiredCredentials);
        appendToBufferIfDifferent(buf, "useRealmQualifiedUserNames",
                                  this.useRealmQualifiedUserNames, orig.useRealmQualifiedUserNames);
        return buf.toString();
    }
}
