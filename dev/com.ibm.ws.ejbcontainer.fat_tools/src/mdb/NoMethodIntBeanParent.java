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

import javax.resource.ResourceException;
import javax.resource.cci.Record;

public class NoMethodIntBeanParent {

    private final static Logger svLogger = Logger.getLogger("NoMethodIntBeanParent");

    public Record REMOVE(Record record) throws ResourceException {
        svLogger.info("NoMethodIntBean.REMOVE record = " + record);
        return record;
    }
}
