/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.implicit.beans;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.ibm.ws.cdi12.test.beansXML.UnannotatedBeanInAllModeBeanArchive;

@ApplicationScoped
public class ApplicationScopedBean {

    @Inject
    private UnannotatedBeanInAllModeBeanArchive unannotatedBean;

    public void setData(String data) {
        unannotatedBean.setData(data);
    }

}
