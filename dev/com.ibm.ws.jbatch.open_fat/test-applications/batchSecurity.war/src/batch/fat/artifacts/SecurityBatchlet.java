/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
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
