/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.services;

import javax.persistence.TypedQuery;

import com.ibm.jbatch.container.persistence.jpa.JobInstanceEntity;

/**
 * Helper to translate search parameters into usable JPQL
 */
public interface IJPAQueryHelper {

    /**
     * Get the JPQL query string
     */
    String getQuery();

    /**
     * Populate the query parameters
     */
    void setQueryParameters(TypedQuery<JobInstanceEntity> query);

    /**
     * Set a submitter id to handle authorization
     */
    void setAuthSubmitter(String submitter);

}
