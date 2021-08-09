/*
 * Copyright 2012 International Business Machines Corp.
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
package com.ibm.jbatch.container.transaction.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.exception.TransactionManagementException;
import com.ibm.jbatch.spi.services.TransactionManagerAdapter;

public class DefaultNonTransactionalManager implements TransactionManagerAdapter {
	
	private static final String CLASSNAME = DefaultNonTransactionalManager.class.getName();
	
	private static final Logger logger = Logger.getLogger(CLASSNAME);

	/**
	 * transaction status
	 */
	private int status = 6; // javax.transaction.Status.STATUS_NO_TRANSACTION
	
	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#begin()
	 */
	@Override
	public void begin() throws TransactionManagementException {
		logger.entering(CLASSNAME, "begin");
		status = 0; // javax.transaction.Status.STATUS_ACTIVE
		logger.log(Level.FINE, "javax.transaction.Status.ACTIVE: {0}", status);
		logger.exiting(CLASSNAME, "begin");
	}

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#commit()
	 */
	@Override
	public void commit() throws TransactionManagementException {
		logger.entering(CLASSNAME, "commit");
		status = 3; // javax.transaction.Status.STATUS_COMMITTED
		logger.log(Level.FINE, "javax.transaction.Status.STATUS_COMMITTED: {0}", status);
		logger.exiting(CLASSNAME, "commit");
	}

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#rollback()
	 */
	@Override
	public void rollback() throws TransactionManagementException {
		logger.entering(CLASSNAME, "rollback");
		status = 4; // javax.transaction.Status.STATUS_ROLLEDBACK
		logger.log(Level.FINE, "javax.transaction.Status.STATUS_ROLLEDBACK: {0}", status);
		logger.exiting(CLASSNAME, "rollback");
	}

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#getStatus()
	 */
	@Override
	public int getStatus() throws TransactionManagementException {
		logger.entering(CLASSNAME, "getStatus");
		logger.exiting(CLASSNAME, "getStatus", status);
		return status;
	}

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#setRollbackOnly()
	 */
	@Override
	public void setRollbackOnly() throws TransactionManagementException {
		logger.entering(CLASSNAME, "setRollbackOnly");
		status = 9;  // javax.transaction.Status.STATUS_ROLLING_BACK
		logger.log(Level.FINE, "javax.transaction.Status.STATUS_ROLLING_BACK: {0}", status);
		logger.exiting(CLASSNAME, "setRollbackOnly");
	}

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#setTransactionTimeout(int)
	 */
	@Override
	public void setTransactionTimeout(int seconds) throws TransactionManagementException {
		logger.entering(CLASSNAME, "setTransactionTimeout", seconds);
		logger.fine("do nothing");
		logger.exiting(CLASSNAME, "setTransactionTimeout");
	}
}
