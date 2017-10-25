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

import com.ibm.ws.cdi12.test.utils.ChainableList;
import com.ibm.ws.cdi12.test.utils.Utils;


public abstract class AbstractDecorator implements Bean {

    private final String name;
    protected final Bean decoratedBean;

    public AbstractDecorator(final Bean decoratedBean, final Class<?> subclass) {
        this.decoratedBean = decoratedBean;
        this.name = Utils.id(subclass);
    }

    @Override
    public ChainableList<String> getDecorators() {
        return decoratedBean.getDecorators().chainAdd(name);
    }

}
