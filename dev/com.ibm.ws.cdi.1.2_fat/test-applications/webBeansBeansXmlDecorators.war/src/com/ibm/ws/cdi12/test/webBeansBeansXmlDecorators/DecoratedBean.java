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
package com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A bean with a simple message.
 */
@RequestScoped
@Named
public class DecoratedBean implements Bean {

    @Override
    public String getMessage() {
        return "message";
    }

}
