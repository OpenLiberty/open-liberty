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
package com.ibm.jbatch.spi;

import java.util.logging.Logger;

public final class BatchSPIManager {

	private final static String sourceClass = BatchSPIManager.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private BatchSPIManager() {}
	
	// Eager init OK.
	private static final BatchSPIManager INSTANCE = new BatchSPIManager();

	/**
	 * @return singleton instance
	 */
	public static BatchSPIManager getInstance() {
		return INSTANCE;
	}

	private BatchJobUtil batchJobUtil = null;
	
	private BatchSecurityHelper batchSecurityHelper = null;
	
	private ExecutorServiceProvider executorServiceProvider = null;

	/**
	 * @return The most recently set BatchJobUtil
	 */
	public BatchJobUtil getBatchJobUtil() {
		return batchJobUtil;
	}
	
	/**
	 * @return The most recently set BatchSecurityHelper
	 */
	public BatchSecurityHelper getBatchSecurityHelper() {
		return batchSecurityHelper;
	}

	/**
	 * @return The most recently set ExecutorServiceProvider 
	 */
	public ExecutorServiceProvider getExecutorServiceProvider() {
		return executorServiceProvider;
	}

	/**
	 * May be called at any point and will be immediately reflected in the singleton,
	 * i.e. getBatchJobUtil() will return this.
	 * @param helper impl
	 */
	public void registerBatchJobUtil(BatchJobUtil helper) {
		this.batchJobUtil = helper;
	}
	
	/**
	 * May be called at any point and will be immediately reflected in the singleton,
	 * i.e. getBatchSecurityHelper() will return this.
	 * @param helper impl
	 */
	public void registerBatchSecurityHelper(BatchSecurityHelper helper) {
		this.batchSecurityHelper = helper;
	}
	
	/**
	 * May be called at any point and will be immediately reflected in the singleton,
	 * i.e. getExecutorServiceProvider() will return this.
	 * @param provider impl
	 */
	public void registerExecutorServiceProvider(ExecutorServiceProvider provider) {
		this.executorServiceProvider = provider;
	}
	
	private final byte[] databaseConfigurationCompleteLock = new byte[0];
	private Boolean databaseConfigurationComplete = Boolean.FALSE;
	
    private DatabaseConfigurationBean dataBaseConfigurationBean = null;

    /**
     * This is not the method that the 352 RI will call to get the 
     * final configuration, and lock off further updates.  This is just
     * a normal getter which is more use while the configuration is still
     * being set, before it is hardened.
     * 
     * @return the last-set DatabaseConfigurationBean
     * 
     * @see getFinalDatabaseConfiguration()
     */
	public DatabaseConfigurationBean getDataBaseConfigurationBean() {
		return dataBaseConfigurationBean;
	}

	/**
     * This only will have an impact if the batch container has not already hardened its
     * persistent store database configuration.   There is no ability to dynamically update
     * the configuration, so if this call comes in after the lazy initialization, it is too late.
     * @param bean 
     * @throws DatabaseAlreadyInitializedException If configuration has already been queried by the batch runtime. 
     */
	public void registerDatabaseConfigurationBean(DatabaseConfigurationBean bean) 
			throws DatabaseAlreadyInitializedException { 
		synchronized (databaseConfigurationCompleteLock) {
			if (!databaseConfigurationComplete) {
				this.dataBaseConfigurationBean = bean;
			} else {
				throw new DatabaseAlreadyInitializedException("Tried to set database configuration but it's too late, since configuration has already been hardened.");
			}
		}
	}
	
	/**
	 * Called by the core batch runtime at the point when it is ready to harden the database
	 * configuration.
	 * 
	 * @return The batch runtime database configuration, if set, or <b>null</b> if not set.
	 */
	public DatabaseConfigurationBean getFinalDatabaseConfiguration() {
		synchronized (databaseConfigurationCompleteLock) {
			databaseConfigurationComplete = Boolean.TRUE;
			return dataBaseConfigurationBean;
		}
	}
	
	/*  This is commented out so it doesn't trigger loading of ServicesManagerImpl.   It's included
	 * as a comment to show that we have the capability in the services manager impl to support
	 * getting feedback on whether the initialization has completed or not. 
	 * 
     * This method is not currently expected to be used by Glassfish, since it will 
     * force a load of the rest of the batch runtime.  It has the virtue of throwing an 
     * exception if initialization of the batch runtime database has already been performed,
     * in which case the bean passed as input is ignored.
     * @param bean 
     
	public void initalizeDatabaseConfigurationBean(DatabaseConfigurationBean bean) 
			{
		ServicesManagerImpl servicesMgr = ServicesManagerImpl.getInstance();
		servicesMgr.initalizeDatabaseConfigurationBean(bean);
	}
	*/
}