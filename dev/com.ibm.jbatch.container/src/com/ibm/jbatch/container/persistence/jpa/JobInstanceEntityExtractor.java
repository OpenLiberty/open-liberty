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

import com.ibm.jbatch.container.persistence.jpa.extractor.AbstractJobInstanceEntityExtractor;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerStaticAnchor;

/**
 *
 */
public class JobInstanceEntityExtractor extends AbstractJobInstanceEntityExtractor {

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public Class getInstanceEntityType() {

        //
        // If we understood the lifecycle of ClassExtractor within EclipseLink we
        // might want to cache the entityVersion here, but to be safe, let's call
        // each time, (and there's no particular reason to be concerned about performance here).
        //

        Integer entityVersion = null;

        try {
            entityVersion = ServicesManagerStaticAnchor.getServicesManager().getPersistenceManagerService().getJobInstanceEntityVersionField();
        } catch (Exception ex) {
            throw new BatchRuntimeException(ex);
        }

        if (entityVersion == 3) {
            return JobInstanceEntityV3.class;
        } else if (entityVersion == 2) {
            return JobInstanceEntityV2.class;
        } else {
            return JobInstanceEntity.class;
        }
    }

}
