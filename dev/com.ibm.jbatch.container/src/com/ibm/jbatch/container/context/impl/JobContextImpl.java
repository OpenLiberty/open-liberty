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
package com.ibm.jbatch.container.context.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;

import com.ibm.jbatch.container.annotation.TCKExperimentProperty;
import com.ibm.jbatch.container.instance.WorkUnitDescriptor;

public class JobContextImpl implements JobContext {

    private final static String sourceClass = JobContextImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    private Object transientUserData = null;

    @TCKExperimentProperty
    private final static boolean cloneContextProperties = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            return Boolean.getBoolean("clone.context.properties");
        }
    });

    private WorkUnitDescriptor delegate = null;

    public JobContextImpl(WorkUnitDescriptor rwue) {
        this.delegate = rwue;
    }

    @Override
    public String getExitStatus() {
        return delegate.getExitStatus();
    }

    @Override
    public void setExitStatus(String exitStatus) {
        logger.fine("Setting exitStatus = " + exitStatus);
        delegate.setExitStatus(exitStatus);
    }

    @Override
    public BatchStatus getBatchStatus() {
        return delegate.getBatchStatus();
    }

    @Override
    public Properties getProperties() {
        Properties properties = delegate.getTopLevelJobProperties();

        if (cloneContextProperties) {
            logger.fine("Cloning job context properties");
            return (Properties) properties.clone();
        } else {
            logger.fine("Returing ref (non-clone) to job context properties");
            return properties;
        }
    }

    @Override
    public String getJobName() {
        return delegate.getTopLevelJobName();
    }

    @Override
    public long getExecutionId() {
        return delegate.getTopLevelExecutionId();
    }

    @Override
    public long getInstanceId() {
        return delegate.getTopLevelInstanceId();
    }

    // No need to push down to the runtime execution
    @Override
    public Object getTransientUserData() {
        return transientUserData;
    }

    // No need to push down to the runtime execution
    @Override
    public void setTransientUserData(Object data) {
        this.transientUserData = data;
    }

}
