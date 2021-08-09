/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.rest.internal.resources;

import com.ibm.wsspi.rest.handler.RESTHandler;

public interface APIConstants {
    String BATCH_API_ROOT_PATH = "/batch";
    String BATCH_API_ROOT_V1_PATH = "/batch/v1";
    String BATCH_API_ROOT_V2_PATH = "/batch/v2";
    String BATCH_API_ROOT_V3_PATH = "/batch/v3";
    String BATCH_API_ROOT_V4_PATH = "/batch/v4";

    String BATCH_SWG_BASE_PATH = RESTHandler.PROPERTY_REST_HANDLER_DEFAULT_CONTEXT_ROOT + "/batch";
    String BATCH_SWG_ROOT_PATH = "";
    String BATCH_SWG_ROOT_PATH_1 = "/"; // Special: in Swagger when specifying the root path use '/'
    String BATCH_SWG_ROOT_V1_PATH = "/v1";
    String BATCH_SWG_ROOT_V2_PATH = "/v2";
    String BATCH_SWG_ROOT_V3_PATH = "/v3";
    String BATCH_SWG_ROOT_V4_PATH = "/v4";

    /**
     * /batch end point definitions for swagger file
     */
    String BATCH_SWG_JOBINSTANCES = BATCH_SWG_ROOT_PATH + "/jobinstances";
    String BATCH_SWG_JOBINSTANCES_ID = BATCH_SWG_ROOT_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_SWG_JOBINSTANCES_ID_JOBLOGS = BATCH_SWG_ROOT_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_SWG_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_PATH + "/jobexecutions";
    String BATCH_SWG_JOBEXECUTIONS_ID_PATH = BATCH_SWG_ROOT_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_SWG_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_SWG_ROOT_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_SWG_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_SWG_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_SWG_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_SWG_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_SWG_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_SWG_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_SWG_ROOT_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_SWG_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_SWG_ROOT_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_SWG_STEPEXECUTIONS_ID = BATCH_SWG_ROOT_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_SWG_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_SWG_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_SWG_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
                    BATCH_SWG_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";

    /**
     * /batch end point definitions
     */
    String BATCH_API_JOBINSTANCES = BATCH_API_ROOT_PATH + "/jobinstances";
    String BATCH_API_JOBINSTANCES_ID = BATCH_API_ROOT_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_API_JOBINSTANCES_ID_JOBLOGS = BATCH_API_ROOT_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_API_JOBEXECUTIONS_PATH = BATCH_API_ROOT_PATH + "/jobexecutions";
    String BATCH_API_JOBEXECUTIONS_ID_PATH = BATCH_API_ROOT_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_API_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_API_ROOT_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_API_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_API_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_API_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_API_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_API_ROOT_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_API_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_API_ROOT_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_API_STEPEXECUTIONS_ID = BATCH_API_ROOT_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_API_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_API_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
    		BATCH_API_ROOT_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";

    /**
     * /batch/v1 end point definitions for swagger file
     */
    String BATCH_SWG_V1_JOBINSTANCES = BATCH_SWG_ROOT_V1_PATH + "/jobinstances";
    String BATCH_SWG_V1_JOBINSTANCES_ID = BATCH_SWG_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_SWG_V1_JOBINSTANCES_ID_JOBLOGS = BATCH_SWG_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_SWG_V1_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_V1_PATH + "/jobexecutions";
    String BATCH_SWG_V1_JOBEXECUTIONS_ID_PATH = BATCH_SWG_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_SWG_V1_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_SWG_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_SWG_V1_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_V1_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_SWG_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_SWG_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_SWG_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_SWG_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_SWG_V1_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_V1_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_SWG_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_SWG_V1_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_SWG_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_SWG_V1_STEPEXECUTIONS_ID = BATCH_SWG_ROOT_V1_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_SWG_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_SWG_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_SWG_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
                    BATCH_SWG_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";

    /**
     * /batch/V1
     */
    String BATCH_API_V1_JOBINSTANCES = BATCH_API_ROOT_V1_PATH + "/jobinstances";
    String BATCH_API_V1_JOBINSTANCES_ID = BATCH_API_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_API_V1_JOBINSTANCES_ID_JOBLOGS = BATCH_API_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_API_V1_JOBEXECUTIONS_PATH = BATCH_API_ROOT_V1_PATH + "/jobexecutions";
    String BATCH_API_V1_JOBEXECUTIONS_ID_PATH = BATCH_API_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_API_V1_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_API_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_API_V1_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_API_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_API_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_API_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_V1_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_API_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_API_V1_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_API_ROOT_V1_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_API_V1_STEPEXECUTIONS_ID = BATCH_API_ROOT_V1_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_API_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_API_V1_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
    		BATCH_API_ROOT_V1_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";

    /**
     * /batch/v2 end point definitions for swagger file
     */
    String BATCH_SWG_V2_JOBINSTANCES = BATCH_SWG_ROOT_V2_PATH + "/jobinstances";
    String BATCH_SWG_V2_JOBINSTANCES_ID = BATCH_SWG_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_SWG_V2_JOBINSTANCES_ID_JOBLOGS = BATCH_SWG_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_SWG_V2_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_V2_PATH + "/jobexecutions";
    String BATCH_SWG_V2_JOBEXECUTIONS_ID_PATH = BATCH_SWG_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_SWG_V2_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_SWG_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_SWG_V2_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_V2_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_SWG_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_SWG_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_SWG_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_SWG_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_SWG_V2_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_V2_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_SWG_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_SWG_V2_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_SWG_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_SWG_V2_STEPEXECUTIONS_ID = BATCH_SWG_ROOT_V2_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_SWG_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_SWG_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_SWG_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
                    BATCH_SWG_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";
    
    /**
     * /batch/V2
     */
    String BATCH_API_V2_JOBINSTANCES = BATCH_API_ROOT_V2_PATH + "/jobinstances";
    String BATCH_API_V2_JOBINSTANCES_ID = BATCH_API_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_API_V2_JOBINSTANCES_ID_JOBLOGS = BATCH_API_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_API_V2_JOBEXECUTIONS_PATH = BATCH_API_ROOT_V2_PATH + "/jobexecutions";
    String BATCH_API_V2_JOBEXECUTIONS_ID_PATH = BATCH_API_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_API_V2_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_API_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_API_V2_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_API_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_API_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_API_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_V2_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_API_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_API_V2_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_API_ROOT_V2_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_API_V2_STEPEXECUTIONS_ID = BATCH_API_ROOT_V2_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_API_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_API_V2_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
    		BATCH_API_ROOT_V2_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";

    /**
     * /batch/v3 end point definitions for swagger file
     */
    String BATCH_SWG_V3_JOBINSTANCES = BATCH_SWG_ROOT_V3_PATH + "/jobinstances";
    String BATCH_SWG_V3_JOBINSTANCES_ID = BATCH_SWG_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_SWG_V3_JOBINSTANCES_ID_JOBLOGS = BATCH_SWG_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_SWG_V3_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_V3_PATH + "/jobexecutions";
    String BATCH_SWG_V3_JOBEXECUTIONS_ID_PATH = BATCH_SWG_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_SWG_V3_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_SWG_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_SWG_V3_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_V3_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_SWG_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_SWG_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_SWG_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_SWG_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_SWG_V3_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_V3_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_SWG_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_SWG_V3_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_SWG_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_SWG_V3_STEPEXECUTIONS_ID = BATCH_SWG_ROOT_V3_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_SWG_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_SWG_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_SWG_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
                    BATCH_SWG_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";
    
    /**
     * /batch/V3
     */
    String BATCH_API_V3_JOBINSTANCES = BATCH_API_ROOT_V3_PATH + "/jobinstances";
    String BATCH_API_V3_JOBINSTANCES_ID = BATCH_API_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_API_V3_JOBINSTANCES_ID_JOBLOGS = BATCH_API_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_API_V3_JOBEXECUTIONS_PATH = BATCH_API_ROOT_V3_PATH + "/jobexecutions";
    String BATCH_API_V3_JOBEXECUTIONS_ID_PATH = BATCH_API_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_API_V3_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_API_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_API_V3_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_API_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_API_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_API_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_V3_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_API_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_API_V3_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_API_ROOT_V3_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_API_V3_STEPEXECUTIONS_ID = BATCH_API_ROOT_V3_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_API_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_API_V3_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
    		BATCH_API_ROOT_V3_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";
    
    /**
     * /batch/v4 end point definitions for swagger file
     */
    String BATCH_SWG_V4_JOBINSTANCES = BATCH_SWG_ROOT_V4_PATH + "/jobinstances";
    String BATCH_SWG_V4_JOBINSTANCES_ID = BATCH_SWG_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_SWG_V4_JOBINSTANCES_ID_JOBLOGS = BATCH_SWG_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_SWG_V4_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_V4_PATH + "/jobexecutions";
    String BATCH_SWG_V4_JOBEXECUTIONS_ID_PATH = BATCH_SWG_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_SWG_V4_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_SWG_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_SWG_V4_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_V4_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_SWG_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_SWG_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_SWG_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_SWG_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_SWG_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_SWG_V4_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_SWG_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_SWG_V4_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_SWG_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_SWG_V4_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_SWG_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_SWG_V4_STEPEXECUTIONS_ID = BATCH_SWG_ROOT_V4_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_SWG_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_SWG_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_SWG_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
                    BATCH_SWG_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";
    
    /**
     * /batch/V4
     */
    String BATCH_API_V4_JOBINSTANCES = BATCH_API_ROOT_V4_PATH + "/jobinstances";
    String BATCH_API_V4_JOBINSTANCES_ID = BATCH_API_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}";
    String BATCH_API_V4_JOBINSTANCES_ID_JOBLOGS = BATCH_API_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/joblogs";
    String BATCH_API_V4_JOBEXECUTIONS_PATH = BATCH_API_ROOT_V4_PATH + "/jobexecutions";
    String BATCH_API_V4_JOBEXECUTIONS_ID_PATH = BATCH_API_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}";
    String BATCH_API_V4_JOBEXECUTIONS_ID_JOBINSTANCE_PATH = BATCH_API_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}/jobinstance";
    String BATCH_API_V4_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_PATH = BATCH_API_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecutions";
    String BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH = BATCH_API_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}";
    String BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_PATH_DESCRIPTIVE = BATCH_API_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}";
    String BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_ID_JOBLOGS_PATH = BATCH_API_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionid}/joblogs";
    String BATCH_API_V4_JOBEXECUTIONS_ID_STEPEXECUTIONS = BATCH_API_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions";
    String BATCH_API_V4_JOBEXECUTIONS_ID_STEPEXECUTIONS_NAME = BATCH_API_ROOT_V4_PATH + "/jobexecutions/{jobexecutionid}/stepexecutions/{stepname}";
    String BATCH_API_V4_STEPEXECUTIONS_ID = BATCH_API_ROOT_V4_PATH + "/stepexecutions/{stepexecutionid}";
    String BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME =
                    BATCH_API_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecutions/{jobexecutionnumber}/stepexecutions/{stepname}";
    String BATCH_API_V4_JOBINSTANCES_ID_JOBEXECUTIONS_NUMBER_STEPEXECUTIONS_NAME_DESCRIPTIVE =
    		BATCH_API_ROOT_V4_PATH + "/jobinstances/{jobinstanceid}/jobexecnum/{jobexecutionnumber}/stepexecutions/{stepname}";
}