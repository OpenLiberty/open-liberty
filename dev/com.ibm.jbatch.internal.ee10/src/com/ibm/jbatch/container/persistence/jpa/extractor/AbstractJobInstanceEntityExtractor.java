/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.persistence.jpa.extractor;

import org.eclipse.persistence.descriptors.ClassExtractor;
import org.eclipse.persistence.sessions.DataRecord;
import org.eclipse.persistence.sessions.Session;

/**
 *
 */
public abstract class AbstractJobInstanceEntityExtractor extends ClassExtractor {

    /** {@inheritDoc} */
    @Override
    public Class extractClassFromRow(DataRecord record, Session session) {
        return getInstanceEntityType();
    }

    public abstract Class getInstanceEntityType();

}
