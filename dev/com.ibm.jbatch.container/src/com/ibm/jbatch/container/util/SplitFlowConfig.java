/**
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
package com.ibm.jbatch.container.util;

import com.ibm.jbatch.container.persistence.jpa.RemotableSplitFlowKey;
import com.ibm.jbatch.container.ws.TopLevelNameInstanceExecutionInfo;

public class SplitFlowConfig {

    private TopLevelNameInstanceExecutionInfo topLevelNameInstanceExecutionInfo;

	private String splitName;
    private String flowName;
    private String correlationId;

    /**
	 * @param topLevelNameInstanceExecutionInfo
	 * @param splitName
	 * @param flowName
	 */
	public SplitFlowConfig(
			TopLevelNameInstanceExecutionInfo topLevelNameInstanceExecutionInfo,
			String splitName, String flowName, String correlationId) {
		super();
		this.topLevelNameInstanceExecutionInfo = topLevelNameInstanceExecutionInfo;
		this.splitName = splitName;
		this.flowName = flowName;
		this.correlationId = correlationId;
	}
	public TopLevelNameInstanceExecutionInfo getTopLevelNameInstanceExecutionInfo() {
		return topLevelNameInstanceExecutionInfo;
	}

	public String getSplitName() {
		return splitName;
	}

	public String getFlowName() {
		return flowName;
	}
	
	public String getCorrelationId() {
		return correlationId;
	}

	public RemotableSplitFlowKey getRemotableSplitFlowKey() {
		return new RemotableSplitFlowKey(topLevelNameInstanceExecutionInfo.getExecutionId(), flowName);
	}
}
