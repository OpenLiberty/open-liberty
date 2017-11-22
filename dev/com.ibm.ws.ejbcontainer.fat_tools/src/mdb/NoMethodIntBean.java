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
package mdb;

import java.util.logging.Logger;

import javax.ejb.MessageDriven;
import javax.resource.ResourceException;
import javax.resource.cci.Record;

import mdb.interceptors.CheckInvocation;

@MessageDriven
public class NoMethodIntBean extends NoMethodIntBeanParent implements NoMethodInterface {

    private final static Logger svLogger = Logger.getLogger("NoMethodIntBean");

    public Record ADD(Record record) throws ResourceException {
        svLogger.info("NoMethodIntBean.ADD record = " + record);
        return record;
    }

    public void INTERCEPTOR(String msg) throws ResourceException {
        CheckInvocation.getInstance().recordCallInfo("AroundInvoke", "NoMethodIntBean.INTERCEPTOR", this);
        svLogger.info("NoMethodIntBean.INTERCEPTOR " + msg);
    }

    private Record privateOnMessage(Record record) throws ResourceException {
        svLogger.info("Private method should not be reachable!");
        return record;
    }
}
