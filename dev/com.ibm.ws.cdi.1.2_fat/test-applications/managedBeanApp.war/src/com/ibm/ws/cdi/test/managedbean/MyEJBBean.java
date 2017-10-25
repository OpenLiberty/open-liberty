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
package com.ibm.ws.cdi.test.managedbean;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.enterprise.context.ApplicationScoped;

/**
 *
 */
@Local
@Stateful
@ApplicationScoped
public class MyEJBBean implements MyEJBBeanLocal {

    List<String> msgList = new ArrayList<String>();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cdi.test.managedbean.MyEJBBeanLocal#addToMsgList(java.lang.String)
     */
    @Override
    public void addToMsgList(String item) {
        msgList.add(item);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cdi.test.managedbean.MyEJBBeanLocal#getMsgList()
     */
    @Override
    public List<String> getMsgList() {
        return msgList;
    }
}
