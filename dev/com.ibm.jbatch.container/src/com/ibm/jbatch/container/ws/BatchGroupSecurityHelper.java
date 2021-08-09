package com.ibm.jbatch.container.ws;

import java.util.Set;

import javax.security.auth.Subject;

public interface BatchGroupSecurityHelper {

    /**
     * @return Returns the list of group names
     */
    public Set<String> getGroupsForSubject(Subject subject);

}
