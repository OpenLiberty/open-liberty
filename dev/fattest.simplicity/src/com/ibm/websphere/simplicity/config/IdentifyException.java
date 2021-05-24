/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents the <identifyException> element in server.xml
 */
public class IdentifyException extends ConfigElement {
    // attributes
    private String as;
    private String errorCode;
    private String sqlState;

    public String getAs() {
        return as;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getSqlState() {
        return sqlState;
    }

    // setters for attributes

    @XmlAttribute
    public void setAs(String value) {
        as = value;
    }

    @XmlAttribute
    public void setErrorCode(String value) {
        errorCode = value;
    }

    @XmlAttribute
    public void setSqlState(String value) {
        sqlState = value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (as != null)
            buf.append("as=").append(as).append(' ');
        if (errorCode != null)
            buf.append("errorCode=").append(errorCode).append(' ');
        if (sqlState != null)
            buf.append("sqlState=").append(sqlState).append(' ');
        // nested elements - none
        buf.append('}');
        return buf.toString();
    }
}
