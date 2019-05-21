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
 * Class used to test the c:forEach tag when object is serializable
 */
public class SimpleSerializableObject implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String value;

    public SimpleSerializableObject() {
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

}
