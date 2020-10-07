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
package com.ibm.ws.jbatch.rest.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Constants used during search queries
 */
public class WSSearchConstants {

    // The list of valid fields to sort by.  Temporary until we can use the entity class itself to determine validity
    static final public List<String> VALID_SORT_FIELDS = Arrays.asList("createTime", "lastUpdatedTime", "submitter", "amcName",
                                                                 "jobXMLName", "batchStatus", "exitStatus");

    // Valid search parameters.  Search parameters specified other than these (excepting job parameters) will end up in the X-IBM-Unrecognized-Fields response header
    static final public List<String> VALID_SEARCH_PARAMS_V3 = Arrays.asList("jobInstanceId", "createTime", "instanceState", "exitStatus",
                                                                      "lastUpdatedTime", "page", "pageSize", "sort", "permitRedirect");

    // Valid search parameters.  Search parameters specified other than these (excepting job parameters) will end up in the X-IBM-Unrecognized-Fields response header
    static final public List<String> VALID_SEARCH_PARAMS_V4 = Arrays.asList("jobInstanceId", "createTime", "instanceState", "exitStatus",
                                                                      "lastUpdatedTime", "page", "pageSize", "sort",
                                                                      "submitter", "appName", "jobName", "ignoreCase", "permitRedirect");
}
