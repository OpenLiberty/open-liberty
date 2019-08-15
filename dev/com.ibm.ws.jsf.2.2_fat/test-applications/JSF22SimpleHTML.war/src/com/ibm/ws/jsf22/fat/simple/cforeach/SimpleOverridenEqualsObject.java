/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.simple.cforeach;

import java.io.Serializable;

/**
 * Class used to test the c:forEach tag when object has an overriden equals method
 */
public class SimpleOverridenEqualsObject implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String value;

    public SimpleOverridenEqualsObject() {
        id = new Long(0L);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        else {
            SimpleOverridenEqualsObject o = (SimpleOverridenEqualsObject) obj;
            return getId().equals(o.getId());
        }
    }

}
