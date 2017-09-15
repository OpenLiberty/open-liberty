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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.ibm.jbatch.container.exception.TransactionManagementException;
import com.ibm.jbatch.spi.services.TransactionManagerAdapter;

/**
 * The JTA Transaction Adapter is used in a J2EE environment where a JTA tran
 * manager is available. *
 */
public class JTAUserTransactionAdapter implements TransactionManagerAdapter {
	
	private static final String CLASSNAME = JTAUserTransactionAdapter.class.getName();
	
	private static final Logger logger = Logger.getLogger(CLASSNAME);
	
	/**
	 * JTA transaction manager
	 */
	protected UserTransaction userTran = null;
	protected String jndiLoc = null;
	
	/**
	 * Should get a new one of these per thread I think.
	 */
	public JTAUserTransactionAdapter(String jndiLoc) {
		logger.entering(CLASSNAME, "init", jndiLoc);
		
		this.jndiLoc = jndiLoc;

		InitialContext ctxt;
		try {
			ctxt = new InitialContext();
			// Imagine this should only be cached only per-thread 
			userTran = (UserTransaction) ctxt.lookup(jndiLoc);			
			logger.fine("JNDI user transaction manager found");
		} catch (NamingException ne) {
			throw new TransactionManagementException("Caught exception: " + ne.getMessage() + " looking up user transaction at JNDI location:  " + jndiLoc, ne);
		}
		logger.exiting(CLASSNAME, "init");
	}
	
	public JTAUserTransactionAdapter() {
		this("java:comp/UserTransaction");
	}
	

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#begin()
	 */
	@Override
	public void begin() throws TransactionManagementException {
		logger.entering(CLASSNAME, "begin");
		
		try {
			userTran.begin();
			logger.log(Level.FINE, "javax.transaction.Status: {0}", userTran.getStatus());
		} catch (NotSupportedException e) {
			throw new TransactionManagementException(e);
		} catch (SystemException e) {
			throw new TransactionManagementException(e);
		}
		logger.exiting(CLASSNAME, "begin");
	}

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#commit()
	 */
	@Override
	public void commit() throws TransactionManagementException {
		logger.entering(CLASSNAME, "commit");
		try {
			userTran.commit();
			logger.log(Level.FINE, "javax.transaction.Status: {0}", userTran.getStatus());
		} catch (SecurityException e) {
			throw new TransactionManagementException(e);
		} catch (IllegalStateException e) {
			throw new TransactionManagementException(e);
		} catch (RollbackException e) {
			throw new TransactionManagementException(e);
		} catch (HeuristicMixedException e) {
			throw new TransactionManagementException(e);
		} catch (HeuristicRollbackException e) {
			throw new TransactionManagementException(e);
		} catch (SystemException e) {
			throw new TransactionManagementException(e);
		}
		logger.exiting(CLASSNAME, "commit");
	}

	/* 
	 * Let the client be a bit lazy and not have to check the status
	 * before issuing rollback.  If there's no active tran then just no-op.
	 * This is helpful given that our OSS code is common with the SE, non-tran env.
	 */
	@Override
	public void rollback() throws TransactionManagementException {
		logger.entering(CLASSNAME, "rollback");
		try {
			if (userTran.getStatus() != Status.STATUS_NO_TRANSACTION) {
				userTran.rollback();
			}
			logger.log(Level.FINE, "javax.transaction.Status: {0}", userTran.getStatus());
		} catch (IllegalStateException e) {
			throw new TransactionManagementException(e);
		} catch (SecurityException e) {
			throw new TransactionManagementException(e);
		} catch (SystemException e) {
			throw new TransactionManagementException(e);
		}
		logger.exiting(CLASSNAME, "rollback");
	}

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#getStatus()
	 */
	@Override
	public int getStatus() throws TransactionManagementException {
		logger.entering(CLASSNAME, "getStatus");
		int status = 0;
		
		try {
			status = userTran.getStatus();
			logger.log(Level.FINE, "javax.transaction.Status: {0}", status);
		} catch (SystemException e) {
			throw new TransactionManagementException(e);
		}
		logger.exiting(CLASSNAME, "getStatus", status);
		return status;
	}

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#setRollbackOnly()
	 */
	@Override
	public void setRollbackOnly() throws TransactionManagementException {
		logger.entering(CLASSNAME, "setRollbackOnly");
		try {
			userTran.setRollbackOnly();
			logger.log(Level.FINE, "javax.transaction.Status: {0}", userTran.getStatus());
		} catch (IllegalStateException e) {
			throw new TransactionManagementException(e);
		} catch (SystemException e) {
			throw new TransactionManagementException(e);
		}
		logger.exiting(CLASSNAME, "setRollbackOnly");
	}

	/* (non-Javadoc)
	 * @see javax.batch.spi.TransactionManagerSPI#setTransactionTimeout(int)
	 */
	@Override
	public void setTransactionTimeout(int seconds) throws TransactionManagementException {
		logger.entering(CLASSNAME, "setTransactionTimeout", seconds);
		try {
			userTran.setTransactionTimeout(seconds);
		} catch (SystemException e) {
			throw new TransactionManagementException(e);
		}
		logger.exiting(CLASSNAME, "setTransactionTimeout");
	}

}
