/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.fat.jsf.cforeach;

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
