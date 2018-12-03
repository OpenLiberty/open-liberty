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

import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.ibm.ws.metatype.validator.MetatypeValidator.MetatypeOcdStats;
import com.ibm.ws.metatype.validator.MetatypeValidator.ValidityState;
import com.ibm.ws.metatype.validator.ValidatorMessage.MessageType;

@XmlRootElement(name = "MetaData", namespace = "http://www.osgi.org/xmlns/metatype/vANY")
public class MetatypeRoot extends MetatypeBase {
    @XmlElement(name = "Designate")
    private final List<MetatypeDesignate> designates = new LinkedList<MetatypeDesignate>();
    @XmlElement(name = "OCD")
    private final List<MetatypeOcd> ocds = new LinkedList<MetatypeOcd>();
    @XmlAttribute(name = "localization")
    private String localization;

    private String metatypeFileName;

    public String getLocalization() {
        return localization;
    }

    public List<MetatypeOcdStats> getMetatypeOcdStats() {
        LinkedList<MetatypeOcdStats> stats = new LinkedList<MetatypeOcdStats>();

        for (MetatypeDesignate designate : designates) {
            MetatypeOcdStats stat = new MetatypeOcdStats();
            stat.designateId = designate.getFactoryPid();
            if (stat.designateId == null)
                stat.designateId = designate.getPid();

            List<MetatypeObject> objects = designate.getObjects();
            if (!objects.isEmpty()) {
                String ocdref = objects.get(0).getOcdref();

                for (MetatypeOcd ocd : ocds) {
                    if (ocd.getId().equals(ocdref)) {
                        stat.ocdId = ocd.getId();
                        stat.ibmParentPid = ocd.getIbmParentPid();
                        stat.ibmObjectClass = ocd.getIbmObjectClass();
                        break;
                    }
                }
            }

            stats.add(stat);
        }

        return stats;
    }

    public void setMetatypeFileName(String name) {
        metatypeFileName = name;
    }

    private void validateLocalization() {
        if (localization == null)
            for (MetatypeOcd ocd : ocds) {
                if (ocd.localizationNeeded()) {
                    logMsg(MessageType.Error, "missing.attribute", "localization");
                    break;
                }
            }
        else {
            String trimmed = localization.trim();
            if (trimmed.length() != localization.length())
                logMsg(MessageType.Info, "white.space.found", "localization", localization);
        }
    }

    public MetatypeOcd getMatchingOcd(String ocdref) {
        if (ocdref == null)
            return null;
        else
            ocdref = ocdref.trim();

        for (MetatypeOcd ocd : ocds)
            if (ocdref.equals(ocd.getId().trim()))
                return ocd;

        return null;
    }

    /**
     * Validates that each Designate has a unique factoryPid or pid value.
     *
     * @return
     */
    private void validateUniqueness() {
        TreeSet<String> values = new TreeSet<String>();
        TreeSet<String> badValues = new TreeSet<String>();

        // check Designate pid
        for (MetatypeDesignate designate : designates) {
            String pid = designate.getPid();

            if (pid != null && !values.add(pid))
                badValues.add(pid);
        }

        if (!badValues.isEmpty()) {
            for (String pid : badValues)
                logMsg(MessageType.Warning, "multiple.instances.found", "pid", pid);
        }

        values.clear();
        badValues.clear();

        // check Designate factoryPid
        for (MetatypeDesignate designate : designates) {
            String factoryPid = designate.getFactoryPid();

            if (factoryPid != null && !values.add(factoryPid))
                badValues.add(factoryPid);
        }

        if (!badValues.isEmpty()) {
            for (String factoryPid : badValues)
                logMsg(MessageType.Warning, "multiple.instances.found", "factoryPid", factoryPid);
        }
    }

    @Override
    public void validate(boolean validateRefs) {
        setValidityState(ValidityState.Pass);

        validateLocalization();
        validateUniqueness();

        // check if there are unknown elements
        checkIfUnknownElementsPresent();

        // check if there are unknown attributes
        checkIfUnknownAttributesPresent();

        // validate all the Designates (and by extension the OCDs)
        for (MetatypeDesignate designate : designates) {
            designate.setRoot(this);
            designate.setOcdStats(getOcdStats());
            designate.setNlsKeys(getNlsKeys());
            designate.validate(validateRefs);
            setValidityState(designate.getValidityState());
        }

        // check if there are any OCDs that were not validated or start with upper case.
        for (MetatypeOcd ocd : ocds) {
            if (ocd.getValidityState() == ValidityState.NotValidated) {
                logMsg(MessageType.Error, "ocd.not.validated", ocd.getId(), "this OCD is not referenced by any 'ocdref' attributes");
            }
            if (ocd.getIbmAlias() != null && java.lang.Character.isUpperCase(ocd.getIbmAlias().charAt(0))) {
                logMsg(MessageType.Error, "ocd.not.validated", ocd.getId(), "the OCD alias " +
                                                                            ocd.getIbmAlias() + " begins with upper case, should be lower case");
            }
        }
    }

    public List<MetatypeDesignate> getDesignates() {
        return designates;
    }
}
