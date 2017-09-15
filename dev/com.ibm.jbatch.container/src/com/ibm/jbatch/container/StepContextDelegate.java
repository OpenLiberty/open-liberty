/**
 * Copyright 2014 International Business Machines Corp.
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
package com.ibm.jbatch.container;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

public interface StepContextDelegate {

	String getStepName();

	Properties getProperties();

	// Delegate to this special method to distinguish between "sub id" and "top level id"
	long getTopLevelStepExecutionId();

	void setPersistentUserDataObject(Serializable data);

	List<Metric> getMetrics();

	Exception getException();

	void setExitStatus(String status);

	String getExitStatus();

	BatchStatus getBatchStatus();

	Serializable getPersistentUserDataObject();

}
