/*
 * Copyright 2013 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.services.impl;

import com.ibm.jbatch.container.impl.ParallelStepBuilder;


/**
 * !!! NOTE !!! 
 * If you're going to uncomment any of these SQL statments, make sure you prepend
 * every table name and constraint name with {schema}.{tablePrefix}.  The {schema} 
 * and {tablePrefix} are configured by the user.
 * 
 * Also add unit tests (to com.ibm.jbatch.container_test) for resolving {schema}.{tablePrefix} 
 * for each SQL statement.
 *
 */
 interface JDBCPersistenceManagerSQLConstants {

	 // final String JOBSTATUS_TABLE = "JOBSTATUS";
	 // final String STEPSTATUS_TABLE = "STEPSTATUS";
	 // final String CHECKPOINTDATA_TABLE = "CHECKPOINTDATA";
	 // final String JOBINSTANCEDATA_TABLE = "JOBINSTANCEDATA";
	 // final String EXECUTIONINSTANCEDATA_TABLE = "EXECUTIONINSTANCEDATA";
	 // final String STEPEXECUTIONINSTANCEDATA_TABLE = "STEPEXECUTIONINSTANCEDATA";

	 // final String INSERT_STEPSTATUS = "insert into stepstatus (id, obj) values(?, ?)";
	
	 // final String UPDATE_STEPSTATUS = "update stepstatus set obj = ? where id = ?";

	 // final String SELECT_STEPSTATUS = "select id, obj from stepstatus where id = ?";
	
	 // final String DELETE_STEPSTATUS = "delete from stepstatus where id = ?";

	 final String INSERT_CHECKPOINTDATA = "insert into {schema}.{tablePrefix}checkpointdata (id, obj) values(?, ?)";

	 final String UPDATE_CHECKPOINTDATA = "update {schema}.{tablePrefix}checkpointdata set obj = ? where id = ?";

	 final String SELECT_CHECKPOINTDATA = "select id, obj from {schema}.{tablePrefix}checkpointdata where id = ?";
	
	 // final String CREATE_CHECKPOINTDATA_INDEX = "create index chk_index on checkpointdata(id)";
	
	 final String DELETE_CHECKPOINTDATA = "delete from {schema}.{tablePrefix}checkpointdata where id like ?";
	
	/// / JOB OPERATOR QUERIES
	 // final String INSERT_JOBINSTANCEDATA = "insert into jobinstancedata (name, submitter) values(?, ?)";
	
	 // final String INSERT_EXECUTIONDATA = "insert into executionInstanceData (jobinstanceid, parameters) values(?, ?)";
	
	 final String SELECT_JOBINSTANCEDATA_COUNT = "select count(jobinstanceid) as jobinstancecount from {schema}.{tablePrefix}jobinstancedata where name = ?";
	
	 final String SELECT_JOBINSTANCEDATA_IDS = "select jobinstanceid from {schema}.{tablePrefix}jobinstancedata where name = ? order by jobinstanceid desc";
	
	 // final String SELECT_JOBINSTANCEDATA_NAMES = "select name from jobinstancedata where submitter = ?";
	 final String SELECT_JOBINSTANCEDATA_SUBMITTER = "select submitter from {schema}.{tablePrefix}jobinstancedata where jobinstanceid = ?";
	
	 final String EXECUTIONINSTANCEDATA_CREATE_NEW_ENTRY ="INSERT INTO {schema}.{tablePrefix}executioninstancedata (jobinstanceid, createtime, updatetime, batchstatus, parameters, serverId) VALUES(?, ?, ?, ?, ?, ?)";
	 
	 final String EXECUTIONINSTANCEDATA_GET_EXECUTION_INSTANCE_BY_BATCH_STATUS_AND_SERVER = "select * from {schema}.{tablePrefix}executioninstancedata where serverId = ? and batchstatus in (?)";
	 
	 final String EXECUTIONINSTANCEDATA_UPDATE_STATUS_AND_ENDTIME_FOR_GROUP = "update {schema}.{tablePrefix}executioninstancedata set batchstatus = ?, exitstatus = ?, endtime = ?, updatetime = ? where jobexecid in (?)";


	// final String START_TIME = "starttime";
	// final String CREATE_TIME = "createtime";
	// final String END_TIME = "endtime";
	// final String UPDATE_TIME = "updatetime";
	// final String BATCH_STATUS = "batchstatus";
	// final String EXIT_STATUS = "exitstatus";
	// final String INSTANCE_ID = "instanceId";
	// final String JOBEXEC_ID = "jobexecid";
	// final String STEPEXEC_ID = "stepexecid";
	// final String STEPCONTEXT = "stepcontext";
	final String SUBMITTER = "submitter";

	final String STEPEXECUTIONINSTANCEDATA_GET_STEP_EXECUTION_INSTANCE_BY_ID_AND_BATCH_STATUS = "select * from {schema}.{tablePrefix}stepexecutioninstancedata where jobexecid = ? and batchstatus in (?)";

	final String STEPEXECUTIONINSTANCEDATA_UPDATE_BATCH_STATUS_EXIT_STATUS_ENDTIME =  "update {schema}.{tablePrefix}stepexecutioninstancedata set batchstatus = ?, exitstatus = ?, endtime = ? where stepexecid = ?";
	
	final String TOP_LEVEL_JOB_ONLY_NAME_FILTER = "name not like '" + ParallelStepBuilder.JOB_ID_SEPARATOR + "%'";

	/**
	 * @return the given sql with all {schema}/{tablePrefix} resolved.
	 */
	String resolveSql(String sql);

}
