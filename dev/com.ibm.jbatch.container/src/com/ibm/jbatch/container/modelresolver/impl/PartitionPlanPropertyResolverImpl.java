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
package com.ibm.jbatch.container.modelresolver.impl;

import java.util.List;
import java.util.Properties;

import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.PartitionPlan;


public class PartitionPlanPropertyResolverImpl extends
		AbstractPropertyResolver<PartitionPlan> {

	public PartitionPlanPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	@Override
	public PartitionPlan substituteProperties(PartitionPlan partitionPlan,
			Properties submittedProps, Properties parentProps) {
    
		/*
		<xs:complexType name="PartitionPlan">
			<xs:sequence>
				<xs:element name="properties" type="jsl:Properties" minOccurs="0" maxOccurs="unbounded" />
			</xs:sequence>
			<xs:attribute name="instances" use="optional" type="xs:string" />
			<xs:attribute name="threads" use="optional" type="xs:string" />
		</xs:complexType>
		*/
		
		partitionPlan.setPartitions(this.replaceAllProperties(partitionPlan.getPartitions(), submittedProps, parentProps));
		partitionPlan.setThreads(this.replaceAllProperties(partitionPlan.getThreads(), submittedProps, parentProps));
		
        // Resolve all the properties defined for this plan
		Properties currentProps = parentProps;
        if (partitionPlan.getProperties() != null) {
        	
        	List<JSLProperties> jslProps = partitionPlan.getProperties();
        	
        	if (jslProps != null) {
        		for (JSLProperties jslProp : jslProps) {
        		    //for partition properties perform substitution on the partition attribute
        		    if (jslProp.getPartition() != null) {
        		        jslProp.setPartition(this.replaceAllProperties(jslProp.getPartition(), submittedProps, parentProps));
        		    }
                    currentProps = this.resolveElementProperties(jslProp.getPropertyList(), submittedProps, parentProps);            		
            	}	
        	}
        	
        	

        }
		
		return partitionPlan;
		

	}

}
