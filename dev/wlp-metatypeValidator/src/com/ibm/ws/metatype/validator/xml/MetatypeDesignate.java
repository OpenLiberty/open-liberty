/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metatype.validator.xml;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.ws.metatype.validator.MetatypeValidator.ValidityState;
import com.ibm.ws.metatype.validator.ValidatorMessage.MessageType;

public class MetatypeDesignate extends MetatypeBase {
    private MetatypeRoot root;

    @XmlAttribute(name = "factoryPid")
    private String factoryPid;
    @XmlAttribute(name = "pid")
    private String pid;
    @XmlElement(name = "Object")
    private final List<MetatypeObject> objects = new LinkedList<MetatypeObject>();

    public List<String> getFactoryPidAndPid() {
        List<String> pids = new ArrayList<String>(2);

        if (pid != null)
            pids.add(pid);
        if (factoryPid != null)
            pids.add(factoryPid);

        return pids;
    }

    public List<MetatypeObject> getObjects() {
        return objects;
    }

    public String getPid() {
        return pid;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    private void validateFactoryPidAndPid() {
        if (pid == null && factoryPid == null)
            logMsg(MessageType.Error, "missing.attribute", "pid|factoryPid");
        else if (pid != null && factoryPid != null)
            logMsg(MessageType.Error, "pid.and.factorypid.coexist", pid, factoryPid);
        else if (pid != null) {
            String trimmed = pid.trim();
            if (trimmed.length() != pid.length())
                logMsg(MessageType.Info, "white.space.found", "pid", pid);
        } else if (factoryPid != null) {
            String trimmed = factoryPid.trim();
            if (trimmed.length() != factoryPid.length())
                logMsg(MessageType.Info, "white.space.found", "factoryPid", factoryPid);
        }
    }

    @Override
    public void validate(boolean validateRefs) {
        setValidityState(ValidityState.Pass);
        validateFactoryPidAndPid();

        checkIfUnknownElementsPresent();
        checkIfUnknownAttributesPresent();

        for (MetatypeObject object : objects) {
            object.setParentDesignate(this);
            object.setRoot(root);
            object.setOcdStats(getOcdStats());
            object.setNlsKeys(getNlsKeys());
            object.validate(validateRefs);
            setValidityState(object.getValidityState());
        }
    }

    @Override
    public void setRoot(MetatypeRoot root) {
        this.root = root;
    }
}
