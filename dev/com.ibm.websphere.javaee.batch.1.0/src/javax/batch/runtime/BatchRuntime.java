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
package javax.batch.runtime;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobOperator;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * BatchRuntime represents the JSR 352 Batch Runtime.
 * It provides factory access to the JobOperator interface.
 *
 */
public class BatchRuntime {

    private final static String sourceClass = BatchRuntime.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    private static ServiceTracker<JobOperator, JobOperator> jobOperatorTracker = null;
    private static JobOperator jo;
    
    static {
        
        try {
            jobOperatorTracker = new ServiceTracker<JobOperator, JobOperator>(
                    FrameworkUtil.getBundle(JobOperator.class).getBundleContext(), 
                    FrameworkUtil.createFilter("(component.name=com.ibm.jbatch.container.api.impl.JobOperatorImplSuspendTran)"),
                    null);
            jobOperatorTracker.open();
        } catch (InvalidSyntaxException ise) {
            throw new BatchRuntimeException("Failed to load ServiceTracker for JobOperator", ise);
        }
      
    }
    

    
    /**
     * The getJobOperator factory method returns
     * an instance of the JobOperator interface.
     *
     * @return JobOperator instance.
     * 
     * @throws BatchRuntimeException if job operator is not available
     */
    public static JobOperator getJobOperator() {
        
        jo = jobOperatorTracker.getService();
        
        if (jo == null) {
            
            String msg = getFormattedMessage("batch.container.unavailable",
                                             new Object[] { "<batchPersistence/>"},
                                             "CWWKY0350E: The batch container is not activated. Ensure that batch persistence has been configured via configuration element <batchPersistence />");
            
        	throw new BatchRuntimeException(msg);
        }
        
        return jo;
        
    } 
    
    /**
     * @return a formatted msg with the given key from the resource bundle.
     */
    private static String getFormattedMessage(String msgKey, Object[] fillIns, String defaultMsg) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("com.ibm.jbatch.javax.batch.runtime.internal.BatchMessages");
        
        if (resourceBundle == null) {
            return defaultMsg;
        }
        
        String msg = resourceBundle.getString(msgKey);
        
        return (msg != null) ? MessageFormat.format( msg, fillIns ) : defaultMsg;
    }
        
}
