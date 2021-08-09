/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.osgi.pmi.internal;

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
