/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal.oauth20;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;

public class OAuthResultImpl implements OAuthResult {

    int _status;
    AttributeList _attributeList;
    OAuthException _cause;

    public OAuthResultImpl(int status, AttributeList attributeList) {
        _status = status;
        _attributeList = attributeList;
    }

    public OAuthResultImpl(int status, AttributeList attributeList, OAuthException cause) {
        _status = status;
        _attributeList = attributeList;
        _cause = cause;
    }

    public int getStatus() {
        return _status;
    }

    public AttributeList getAttributeList() {
        return _attributeList;
    }

    public OAuthException getCause() {
        return _cause;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{_status: ");
        sb.append(_status);
        sb.append(" _attributeList: ");
        sb.append(_attributeList);
        sb.append(" _cause: ");
        sb.append(_cause);
        sb.append("}");
        return sb.toString();
    }
}
