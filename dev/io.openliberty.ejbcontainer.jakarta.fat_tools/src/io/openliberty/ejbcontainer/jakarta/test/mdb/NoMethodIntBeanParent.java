/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.mdb;

import java.util.logging.Logger;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.Record;

public class NoMethodIntBeanParent {

    private final static Logger svLogger = Logger.getLogger("NoMethodIntBeanParent");

    public Record REMOVE(Record record) throws ResourceException {
        svLogger.info("NoMethodIntBean.REMOVE record = " + record);
        return record;
    }
}
