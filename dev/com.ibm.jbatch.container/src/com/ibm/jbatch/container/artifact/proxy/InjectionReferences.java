/**
 * Copyright 2013 International Business Machines Corp.
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
package com.ibm.jbatch.container.artifact.proxy;

import java.util.List;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import com.ibm.jbatch.container.execution.impl.RuntimeStepExecution;
import com.ibm.jbatch.jsl.model.Property;



/**
 * This is a container class that holds on to the property and context injections
 * that should be injected into a batch artifact. 
 * 
 */
public class InjectionReferences {

    private final JobContext jobContext;
    private final RuntimeStepExecution stepExec;
    
    private List<Property> props;
    
    public InjectionReferences(JobContext jobContext, RuntimeStepExecution stepExec, 
            List<Property> props) {

        this.jobContext = jobContext;
        this.stepExec = stepExec;
        this.props = props;
    }

    public JobContext getJobContext() {
        return jobContext;
    }

    public StepContext getStepContext() {
    	return (stepExec == null ? null : stepExec.getStepContext());
    }

    public List<Property> getProps() {
        return props;
    }

    public void setProps(List<Property> props) {
        this.props = props;
    }
    
}
