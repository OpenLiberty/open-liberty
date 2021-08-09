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
package com.ibm.jbatch.container.context.impl;

import java.io.Serializable;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;

import com.ibm.jbatch.container.StepContextDelegate;

public class StepContextImpl implements StepContext {

	StepContextDelegate delegate = null;
	
	private Object transientUserData = null;
	
	public StepContextImpl(StepContextDelegate delegate) {
		this.delegate = delegate;
	}

	@Override
	public String getStepName() {
		return delegate.getStepName();
	}

	@Override
	public Object getTransientUserData() {
		return transientUserData;
	}

	@Override
	public void setTransientUserData(Object data) {
		this.transientUserData = data;
	}

	@Override
	public long getStepExecutionId() {
		return delegate.getTopLevelStepExecutionId();
	}

	@Override
	public Properties getProperties() {
		return delegate.getProperties();
	}

	@Override
	public Serializable getPersistentUserData() {
		return delegate.getPersistentUserDataObject();
	}

	@Override
	public void setPersistentUserData(Serializable data) {
		delegate.setPersistentUserDataObject(data);
	}

	@Override
	public BatchStatus getBatchStatus() {
		return delegate.getBatchStatus();
	}

	@Override
	public String getExitStatus() {
		return delegate.getExitStatus();
	}

	@Override
	public void setExitStatus(String status) {
		delegate.setExitStatus(status);
	}

	@Override
	public Exception getException() {
		return delegate.getException();
	}

	@Override
	public Metric[] getMetrics() {
		return delegate.getMetrics().toArray(new Metric[0]);
	}

}
