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
package com.ibm.websphere.ejbcontainer.test.osgi.pmi.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.EJBPMICollaboratorFactory;

@Component
public class TestEJBPMICollaboratorFactory implements EJBPMICollaboratorFactory {
    private static Map<String, TestEJBPMICollaborator> collaborators = Collections.synchronizedMap(new HashMap<String, TestEJBPMICollaborator>());

    public static TestEJBPMICollaborator getCollaborator(String beanName) {
        return collaborators.get(beanName);
    }

    private String getKey(EJBComponentMetaData cmd) {
        return cmd.getJ2EEName().getComponent();
    }

    @Override
    public EJBPMICollaborator createPmiBean(EJBComponentMetaData data, String containerName) {
        TestEJBPMICollaborator collaborator = new TestEJBPMICollaborator(data);
        collaborators.put(getKey(data), collaborator);
        return collaborator;
    }

    @Override
    public EJBPMICollaborator getPmiBean(String uniqueJ2eeName, String containerName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removePmiModule(Object mod) {
        TestEJBPMICollaborator collaborator = (TestEJBPMICollaborator) mod;
        collaborators.remove(getKey(collaborator.cmd));
    }
}
