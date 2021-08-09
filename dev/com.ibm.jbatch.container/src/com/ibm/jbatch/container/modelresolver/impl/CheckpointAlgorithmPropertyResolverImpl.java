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

import java.util.List;
import java.util.Properties;

import com.ibm.jbatch.jsl.model.CheckpointAlgorithm;
import com.ibm.jbatch.jsl.model.Property;


public class CheckpointAlgorithmPropertyResolverImpl extends AbstractPropertyResolver<CheckpointAlgorithm> {



    public CheckpointAlgorithmPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	@Override
    public CheckpointAlgorithm substituteProperties(final CheckpointAlgorithm checkpointalgorithm, final Properties submittedProps, final Properties parentProps) {
    	
    	//resolve all the properties used in attributes and update the JAXB model
        checkpointalgorithm.setRef(this.replaceAllProperties(checkpointalgorithm.getRef(), submittedProps, parentProps));
    	
    	// Resolve all the properties defined for this checkpoint algorithm
        if (checkpointalgorithm.getProperties() != null) {
            this.resolveElementProperties((List<Property>) checkpointalgorithm.getProperties().getPropertyList(), submittedProps, parentProps);
        }
        
        return checkpointalgorithm;
        
    }

}
