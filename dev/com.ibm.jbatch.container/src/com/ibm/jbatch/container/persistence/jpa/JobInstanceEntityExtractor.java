/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.persistence.jpa;

import javax.batch.operations.BatchRuntimeException;

import org.eclipse.persistence.descriptors.ClassExtractor;
import org.eclipse.persistence.sessions.Record;
import org.eclipse.persistence.sessions.Session;

import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;

/**
 *
 */
public class JobInstanceEntityExtractor extends ClassExtractor {

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public Class extractClassFromRow(Record record, Session session) {

        Integer tableversion = null;

        try {
            tableversion = ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService().getJobInstanceTableVersion();
        } catch (Exception ex) {
            throw new BatchRuntimeException(ex);
        }

        if (tableversion == 3) {
            //if update time is found we can assume v3 (groupnames)
            return JobInstanceEntityV3.class;
        } else if (tableversion == 2) {
            return JobInstanceEntityV2.class;
        } else {
            return JobInstanceEntity.class;
        }
    }

}
