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
package com.ibm.ws.jaxws.metadata;

import java.io.Serializable;

import javax.xml.namespace.QName;

/**
 *
 */
public class XsdQNameInfo implements Serializable {

    /**  */
    private static final long serialVersionUID = 2825225331176598001L;
    private QName value;
    private String id;

    public XsdQNameInfo() {}

    /**
     * @param value
     * @param id
     */
    public XsdQNameInfo(QName value, String id) {
        super();
        this.value = value;
        this.id = id;
    }

    /**
     * @return the value
     */
    public QName getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(QName value) {
        this.value = value;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        XsdQNameInfo other = (XsdQNameInfo) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "XsdQNameInfo [value=" + value + ", id=" + id + "]";
    };

}
