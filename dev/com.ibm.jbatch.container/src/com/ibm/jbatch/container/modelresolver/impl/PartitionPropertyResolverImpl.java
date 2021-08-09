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
import com.ibm.jbatch.jsl.model.Partition;

public class PartitionPropertyResolverImpl extends AbstractPropertyResolver<Partition> {



    public PartitionPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	@Override
    public Partition substituteProperties(final Partition partition, final Properties submittedProps, final Properties parentProps) {
    	/**
			<xs:complexType name="Partition">
				<xs:sequence>
				    <xs:element name="mapper" type="jsl:PartitionMapper" minOccurs="0" maxOccurs="1" />
				    <xs:element name="plan" type="jsl:PartitionPlan" minOccurs="0" maxOccurs="1" />
					<xs:element name="collector" type="jsl:Collector" minOccurs="0" maxOccurs="1"/>
					<xs:element name="analyzer" type="jsl:Analyzer" minOccurs="0" maxOccurs="1"/>
					<xs:element name="reducer " type="jsl:PartitionReducer" minOccurs="0" maxOccurs="1"/>
				</xs:sequence>
			</xs:complexType>
    	 */
    	
        // Resolve all the properties defined for a partition
        if (partition.getMapper() != null) {
        	PropertyResolverFactory.createPartitionMapperPropertyResolver(this.isPartitionedStep).substituteProperties(partition.getMapper(), submittedProps, parentProps);
        }
    	
        if (partition.getPlan() != null) {
        	PropertyResolverFactory.createPartitionPlanPropertyResolver(this.isPartitionedStep).substituteProperties(partition.getPlan(), submittedProps, parentProps);
        }
        
        if (partition.getCollector() != null) {
        	PropertyResolverFactory.createCollectorPropertyResolver(this.isPartitionedStep).substituteProperties(partition.getCollector(), submittedProps, parentProps);
        }
        
        if (partition.getAnalyzer() != null) {
        	PropertyResolverFactory.createAnalyzerPropertyResolver(this.isPartitionedStep).substituteProperties(partition.getAnalyzer(), submittedProps, parentProps);
        }
        
        if (partition.getReducer() != null) {
        	PropertyResolverFactory.createPartitionReducerPropertyResolver(this.isPartitionedStep).substituteProperties(partition.getReducer(), submittedProps, parentProps);
        }
        
        return partition;
    }

}
