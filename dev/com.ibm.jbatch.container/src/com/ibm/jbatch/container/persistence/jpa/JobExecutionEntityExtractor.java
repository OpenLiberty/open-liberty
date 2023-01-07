/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.persistence.jpa;

import javax.batch.operations.BatchRuntimeException;

import com.ibm.jbatch.container.persistence.jpa.extractor.AbstractJobExecutionEntityExtractor;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;

/**
 *
 */
public class JobExecutionEntityExtractor extends AbstractJobExecutionEntityExtractor {

    /** {@inheritDoc} */
    @Override
    public Class getExecutionEntityType() {

        Integer entityVersion = null;

        try {
            entityVersion = ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService().getJobExecutionEntityVersionField();
        } catch (Exception ex) {
            throw new BatchRuntimeException(ex);
        }

        if (entityVersion == 3) {
            return JobExecutionEntityV3.class;
        } else if (entityVersion == 2) {
            return JobExecutionEntityV2.class;
        } else {
            return JobExecutionEntity.class;
        }
    }

/// OLD CODE we tried once:
//        try {
//            Vector result = session.executeSelectingCall(new SQLCall("SELECT * FROM JBATCH.JOBPARAMETER"));
//        } catch (Exception e) {
//            return JobExecutionEntity.class;
//        }
    //return JobExecutionEntityV2.class;
}
