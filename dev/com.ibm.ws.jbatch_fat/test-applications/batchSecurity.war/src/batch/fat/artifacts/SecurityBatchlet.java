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
package batch.fat.artifacts;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.security.auth.Subject;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;

public class SecurityBatchlet extends AbstractBatchlet {

    private final static Logger logger = Logger.getLogger(SecurityBatchlet.class.getName());

    /**
     * For setting the job's exit status.
     */
    @Inject
    private final JobContext jobContext = null;

    @Inject
    @BatchProperty(name = "force.failure")
    String forceFailureProp;
    private boolean forceFailure = false;

    /**
     * Main entry point.
     */
    @Override
    public String process() throws Exception {

        logger.fine("process: entry");

        if ("true".equalsIgnoreCase(forceFailureProp)) {
            forceFailure = true;
        }

        String userId = getUserId(getRunAsSubject());

        logger.fine("process: userId/exitStatus: " + userId);

        // Note: the returned string is set as the STEP's exitStatus.
        // Set the JOB's exitStatus via JobContext
        jobContext.setExitStatus(userId);

        if (forceFailure) {
            throw new Exception("Fail on purpose in SecurityBatchlet.process()");
        }

        return userId;
    }

    /**
     * Called if the batchlet is stopped by the container.
     */
    @Override
    public void stop() throws Exception {
        logger.fine("stop:");
    }

    /**
     * @return the Subject currently active on the thread.
     */
    protected Subject getRunAsSubject() {
        try {
            return WSSubject.getRunAsSubject();
        } catch (WSSecurityException wse) {
            throw new RuntimeException(wse);
        }
    }

    /**
     * @return the userId from the WSPrincipal in the given Subject.
     */
    protected String getUserId(Subject subject) {
        Principal wsprincipal = getWSPrincipal(subject);
        return (wsprincipal != null) ? wsprincipal.getName() : "null";
    }

    /**
     * @return the WSPrincipal in the given Subject (there should only be one).
     */
    protected Principal getWSPrincipal(Subject subject) {
        if (subject == null) {
            return null;
        }

        Set<Principal> principals = subject.getPrincipals();
        return (principals.isEmpty()) ? null : principals.iterator().next();
    }

}
