/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
