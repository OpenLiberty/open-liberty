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
package com.ibm.oauth.core.internal.audit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.oauth.core.api.attributes.Attribute;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.audit.OAuthAuditEntry;
import com.ibm.oauth.core.api.error.OAuthException;

public class OAuthAuditEntryImpl implements OAuthAuditEntry {
    final static SimpleDateFormat DATE_FORMAT;
    final static String SDF_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    final static String TZ_UTC = "UTC";

    static {
        SimpleDateFormat sdf = new SimpleDateFormat(SDF_FORMAT);
        sdf.setLenient(false);
        sdf.setTimeZone(TimeZone.getTimeZone(TZ_UTC));
        DATE_FORMAT = sdf;
    }

    private AttributeList _attributes;
    private Date _timestamp;
    private OAuthException _error;

    public OAuthAuditEntryImpl(AttributeList attributes) {
        this(attributes, null);
    }

    public OAuthAuditEntryImpl(AttributeList attributes, OAuthException error) {
        this._attributes = attributes;
        this._timestamp = new Date();
        this._error = error;
    }

    public AttributeList getAttributes() {
        return _attributes;
    }

    public Date getTimeStamp() {
        return _timestamp;
    }

    public OAuthException getError() {
        return _error;
    }

    public Element toXML(Document document) {
        Element retVal = document.createElement("entry");
        retVal.setAttribute("timestamp", DATE_FORMAT.format(_timestamp));
        if (_error != null) {
            Element errorEle = document.createElement("error");
            errorEle.setAttribute("type", _error.getError());
            Element msgEle = document.createElement("message");
            msgEle.appendChild(document.createCDATASection(_error.getMessage()));
            errorEle.appendChild(msgEle);
            retVal.appendChild(errorEle);
        }
        Element eleAttributes = document.createElement("attributes");
        List<Attribute> attrs = _attributes.getAllAttributes();
        for (Attribute attr : attrs) {
            Element eleAttr = document.createElement("attribute");
            eleAttr.setAttribute("name", attr.getName());
            eleAttr.setAttribute("type", attr.getType());
            for (String value : attr.getValues()) {
                Element eleValue = document.createElement("value");
                eleValue.appendChild(document.createTextNode(value));
                eleAttr.appendChild(eleValue);
            }
            eleAttributes.appendChild(eleAttr);
        }
        retVal.appendChild(eleAttributes);

        return retVal;
    }

}
