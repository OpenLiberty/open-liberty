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
package com.ibm.jbatch.container.modelresolver.impl;

import java.util.Properties;

import com.ibm.jbatch.container.modelresolver.PropertyResolverFactory;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.jsl.model.helper.ExecutionElement;

public class FlowPropertyResolverImpl extends AbstractPropertyResolver<Flow>  {


    public FlowPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	@Override
    public Flow substituteProperties(final Flow flow, final Properties submittedProps, final Properties parentProps) {

        // resolve all the properties used in attributes and update the JAXB model
    	flow.setId(this.replaceAllProperties(flow.getId(), submittedProps, parentProps));
    	flow.setNextFromAttribute(this.replaceAllProperties(flow.getNextFromAttribute(), submittedProps, parentProps));
    	
    	Properties currentProps = parentProps;

        for (final ExecutionElement next : flow.getExecutionElements()) {
            if (next instanceof Step) {
                PropertyResolverFactory.createStepPropertyResolver(this.isPartitionedStep).substituteProperties((Step)next, submittedProps, currentProps);
            } else if (next instanceof Decision) {
                PropertyResolverFactory.createDecisionPropertyResolver(this.isPartitionedStep).substituteProperties((Decision)next, submittedProps, currentProps);
            } else if (next instanceof Flow) {
                PropertyResolverFactory.createFlowPropertyResolver(this.isPartitionedStep).substituteProperties((Flow)next, submittedProps, currentProps);
           } else if (next instanceof Split) {
               PropertyResolverFactory.createSplitPropertyResolver(this.isPartitionedStep).substituteProperties((Split)next, submittedProps, currentProps);
          } 
        }
    	
        return flow;
    }

}
