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
package com.ibm.jbatch.container.util.artifacts;

import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

public class BaseClass extends SuperClass {

	@Inject
	JobContext jobCtx;

	@Inject
	@BatchProperty
	String baseString;

	/**
	 * @return the baseString
	 */
	public String getBaseString() {
		return baseString;
	}

	/**
	 * @return the jobCtx
	 */
	public JobContext getJobCtx() {
		return jobCtx;
	}

}
