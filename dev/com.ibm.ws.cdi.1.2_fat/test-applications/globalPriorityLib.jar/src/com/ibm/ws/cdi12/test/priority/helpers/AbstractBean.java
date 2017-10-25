/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.priority.helpers;

import javax.enterprise.context.RequestScoped;

import com.ibm.ws.cdi12.test.utils.ChainableList;
import com.ibm.ws.cdi12.test.utils.ChainableListImpl;
import com.ibm.ws.cdi12.test.utils.Intercepted;


@RequestScoped
public abstract class AbstractBean implements Bean {

    @Override
    public ChainableList<String> getDecorators() {
        return new ChainableListImpl<String>();
    }

    @Intercepted
    @Override
    public ChainableList<String> getInterceptors() {
        return new ChainableListImpl<String>();
    }

    public Class<? extends AbstractBean> getBean() {
        return this.getClass();
    }

}
