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
package com.ibm.jbatch.container.ws;

import javax.batch.runtime.JobExecution;

public interface WSRemotablePartitionExecution {
	
	public String getLogpath();
	
	public String getRestUrl();
	
	public String getServerId();
	
	public JobExecution getJobExecution();
	
	public String getStepName();
	
	public int getPartitionNumber();

}
