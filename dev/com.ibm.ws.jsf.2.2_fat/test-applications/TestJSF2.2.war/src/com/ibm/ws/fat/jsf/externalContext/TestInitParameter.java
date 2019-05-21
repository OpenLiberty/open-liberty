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
package com.ibm.ws.fat.jsf.externalContext;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * will check if NPE is thrown if name is null
 */
@ManagedBean
@RequestScoped
public class TestInitParameter {

    /**  */
    private final FacesContext context = FacesContext.getCurrentInstance();

    public boolean result = false;

    /**
     * @throws Exception
     */
    public boolean testInitParam() throws Exception {

        String badValue = null;
        ExternalContext ec = context.getExternalContext();
        try {
            ec.getInitParameter(badValue);
        } catch (NullPointerException e) {
            result = true;
        }
        return result;
    }

}
