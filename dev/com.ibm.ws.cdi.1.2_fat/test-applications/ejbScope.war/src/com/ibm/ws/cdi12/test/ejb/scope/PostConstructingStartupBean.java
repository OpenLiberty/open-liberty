/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.test.ejb.scope;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Singleton bean which checks whether request scope is active during postConstruct
 */
@Singleton
@Startup
public class PostConstructingStartupBean {

    @Inject
    RequestScopedBean bean;

    private boolean wasRequestScopeActive;

    /**
     * @return whether the request scope was active during postConstruct
     */
    public boolean getWasRequestScopeActive() {
        return wasRequestScopeActive;
    }

    @PostConstruct
    private void init() {
        try {
            bean.doNothing();
            wasRequestScopeActive = true;
        } catch (Throwable ex) {
            wasRequestScopeActive = false;
        }

    }

}
