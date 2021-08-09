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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.ws.metatype.validator.MetatypeValidator.MetatypeOcdStats;
import com.ibm.ws.metatype.validator.MetatypeValidator.ValidityState;
import com.ibm.ws.metatype.validator.ValidatorMessage.MessageType;

public class MetatypeOcd extends MetatypeBase {
    /**  */
    static final String INTERNAL = "internal";
    /**  */
    static final String INTERNAL_USE_ONLY = "internal use only";
    @XmlAttribute(name = "id")
    private String id;
    @XmlAttribute(name = "name")
    private String name;
    @XmlAttribute(name = "description")
    private String description;
    @XmlAttribute(name = "alias", namespace = IBM_NAMESPACE)
    private String ibmAlias;
    @XmlAttribute(name = "localization", namespace = IBMUI_NAMESPACE)
    private String ibmuiLocalization;
    @XmlAttribute(name = "supportExtensions", namespace = IBM_NAMESPACE)
    private String ibmSupportExtensions;
    @XmlAttribute(name = "supportHiddenExtensions", namespace = IBM_NAMESPACE)
    private String ibmSupportHiddenExtensions;
    @XmlAttribute(name = "extraProperties", namespace = IBMUI_NAMESPACE)
    private String ibmExtraProperties;
    @XmlAttribute(name = "action", namespace = IBM_NAMESPACE)
    private String ibmAction;
    @XmlAttribute(name = "childAlias", namespace = IBM_NAMESPACE)
    private String ibmChildAlias;
    @XmlAttribute(name = "parentPid", namespace = IBM_NAMESPACE)
    private String ibmParentPid;
    @XmlAttribute(name = "extends", namespace = IBM_NAMESPACE)
    private String ibmExtends;
    @XmlAttribute(name = "extendsAlias", namespace = IBM_NAMESPACE)
    private String ibmExtendsAlias;
    @XmlAttribute(name = "excludeChildren", namespace = IBM_NAMESPACE)
    private String ibmExcludeChildren;
    @XmlAttribute(name = "objectClass", namespace = IBM_NAMESPACE)
    private String ibmObjectClass;
    @XmlAttribute(name = "requireExplicitConfiguration", namespace = IBM_NAMESPACE)
    private String ibmRequireExplicitConfiguration;
    @XmlAttribute(name = "any", namespace = IBM_NAMESPACE)
    private String ibmAny;
    @XmlAttribute(name = "beta", namespace = IBM_NAMESPACE)
    private boolean beta;

    @XmlElement(name = "AD")
    List<MetatypeAd> ads = new LinkedList<MetatypeAd>();

    public String getId() {
        return id;
    }

    public String getIbmAlias() {
        return ibmAlias;
    }

    public String getIbmParentPid() {
        return ibmParentPid;
    }

    public boolean areExtensionsSupported() {
        boolean result;
        if (ibmSupportExtensions == null)
            result = false;
        else
            result = Boolean.valueOf(ibmSupportExtensions.trim());
        return result || areHiddenExtensionsSupported();
    }

    public boolean areHiddenExtensionsSupported() {
        if (ibmSupportHiddenExtensions == null)
            return false;
        else
            return Boolean.valueOf(ibmSupportHiddenExtensions.trim());
    }

    public boolean doesAdElementExist(String id) {
        if (id == null)
            return false;

        for (MetatypeAd ad : ads)
            if (id.equals(ad.getId()))
                return true;

        return false;
    }

    boolean localizationNeeded() {
        //any non-magic string means we need localization
        if (!INTERNAL.equals(name) || !INTERNAL_USE_ONLY.equals(description)) {
            return true;
        }
        for (MetatypeAd ad : ads) {
            if (ad.localizationNeeded()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void validate(boolean validateRefs) {
        setValidityState(ValidityState.Pass);
        validateId();
        validateName();
        validateDescription();
        validateIbmuiLocalization();
        validateIbmAlias();
        validateIbmSupportExtensions();
        validateIbmSupportHiddenExtensions();
        validateIbmRequireExplicitConfiguration();
        validateIbmExtraProperties();
        validateIbmAction();
        validateIbmChildAlias();
        if (validateRefs) {
            validateIbmParentPid();
            validateIbmExtends();
            validateIbmExcludeChildren();
        }
        validateIbmAny();

        // check if there are unknown elements
        checkIfUnknownElementsPresent();

        // check if there are unknown attributes
        checkIfUnknownAttributesPresent();

        validateUniqueness(); // validate id, name, and description uniqueness

        // validate individual ADs
        for (MetatypeAd ad : ads) {
            ad.setParentOcd(this);
            ad.setOcdStats(getOcdStats());
            ad.setNlsKeys(getNlsKeys());
            ad.validate(validateRefs);
            setValidityState(ad.getValidityState());
        }
    }

    /**
     * Checks the ibm:excludeChildren to ensure the specified values are pointing to valid reference pids or attributes
     */
    private void validateIbmExcludeChildren() {
        if (ibmExcludeChildren != null) {
            String trimmed = ibmExcludeChildren.trim();
            if (trimmed.length() != ibmExcludeChildren.length())
                logMsg(MessageType.Info, "white.space.found", "ibm:excludeChildren", ibmExcludeChildren);

            for (String excluded : ibmExcludeChildren.split(",")) {
                if (getOcdStats() != null) {
                    excluded = excluded.trim();
                    boolean pidOrAttributeFound = false;

                    for (MetatypeOcdStats ocdStat : getOcdStats()) {
                        if (excluded.equals(ocdStat.designateId)) {
                            pidOrAttributeFound = true;
                            break;
                        }
                    }

                    // We may be excluding attributes that are specified on the parent. We don't have that information here.
                    if (!pidOrAttributeFound && ibmExtends != null) {
                        logMsg(MessageType.Info, "ref.not.found", excluded, "ibm:excludeChildren");
                        pidOrAttributeFound = true;
                    }

                    if (!pidOrAttributeFound)
                        logMsg(MessageType.Error, "ref.not.found", excluded, "ibm:excludeChildren");
                }
            }
        }
    }

    /**
     *
     */
    private void validateIbmAny() {
        if (ibmAny != null) {
            String trimmed = ibmAny.trim();
            if (trimmed.length() != ibmAny.length())
                logMsg(MessageType.Info, "white.space.found", "ibm:any", ibmAny);

            try {
                Integer.parseInt(trimmed);
            } catch (NumberFormatException nfe) {
                logMsg(MessageType.Error, "not.number", "ibm:any", trimmed);
            }
        }
    }

    private void validateIbmExtends() {
        if (ibmExtends != null) {
            String trimmed = ibmExtends.trim();
            if (trimmed.length() != ibmExtends.length())
                logMsg(MessageType.Info, "white.space.found", "ibm:extends", ibmExtends);

            if (getOcdStats() != null) {
                boolean refFound = false;

                for (MetatypeOcdStats ocdStat : getOcdStats()) {
                    if (trimmed.equals(ocdStat.designateId)) {
                        refFound = true;
                        break;
                    }
                }

                if (!refFound)
                    logMsg(MessageType.Error, "ref.not.found", trimmed, "ibm:extends");
            }
            if (ibmExtendsAlias != null) {
                if (ibmExtendsAlias.length() != ibmExtendsAlias.trim().length()) {
                    logMsg(MessageType.Info, "white.space.found", "ibm:extendsAlias", ibmExtendsAlias);
                }
            }
        } else if (ibmExtendsAlias != null) {
            logMsg(MessageType.Error, "extendsalias.requires.extends", ibmExtendsAlias);
        }
    }

    private void validateIbmChildAlias() {
        if (ibmChildAlias != null) {
            String trimmed = ibmChildAlias.trim();
            if (trimmed.length() != ibmChildAlias.length())
                logMsg(MessageType.Info, "white.space.found", "ibm:childAlias", ibmChildAlias);
        }
    }

    private void validateIbmObjectClass() {
        if (ibmObjectClass != null) {
            Set<String> ocs = getIbmObjectClass();
            for (String objectClass : ocs) {
                if (objectClass.isEmpty()) {
                    logMsg(MessageType.Info, "invalid.service.in.objectclass", objectClass, ibmObjectClass);
                }
            }
        }
    }

    private void validateIbmParentPid() {
        if (ibmParentPid != null) {
            String trimmed = ibmParentPid.trim();
            if (trimmed.length() != ibmParentPid.length())
                logMsg(MessageType.Info, "white.space.found", "ibm:parentPid", ibmParentPid);

            if (getOcdStats() != null) {
                boolean refFound = false;

                for (MetatypeOcdStats ocdStat : getOcdStats()) {
                    if (trimmed.equals(ocdStat.designateId)) {
                        refFound = true;
                        break;
                    }
                }

                if (!refFound)
                    logMsg(MessageType.Error, "ref.not.found", trimmed, "ibm:parentPid");
            }
        }
    }

    private void validateUniqueness() {
        TreeSet<String> values = new TreeSet<String>();
        TreeSet<String> badValues = new TreeSet<String>();

        // check for AD 'id' unique values
        for (MetatypeAd ad : ads) {
            String id = ad.getId();

            if (id != null && !values.add(id))
                badValues.add(id);
        }

        if (!badValues.isEmpty()) {
            for (String id : badValues)
                logMsg(MessageType.Error, "multiple.instances.found", "id", id);
        }

        values.clear();
        badValues.clear();

        // check for AD 'name' unique values
        for (MetatypeAd ad : ads) {
            String name = ad.getName();

            if (name != null && !INTERNAL.equals(name) && !values.add(name))
                badValues.add(name);
        }

        if (!badValues.isEmpty())
            for (String name : badValues)
                logMsg(MessageType.Warning, "multiple.instances.found", "name", name);

        values.clear();
        badValues.clear();

        // check for AD 'description' unique values
        for (MetatypeAd ad : ads) {
            String description = ad.getDescription();
            String name = ad.getName();

            if (description != null && !INTERNAL.equals(name) && !values.add(description))
                badValues.add(description);
        }

        if (!badValues.isEmpty())
            for (String description : badValues)
                logMsg(MessageType.Warning, "multiple.instances.found", "description", description);

        values.clear();
        badValues.clear();
    }

    private void validateIbmAlias() {
        if (ibmAlias != null) {
            String trimmed = ibmAlias.trim();
            if (trimmed.length() != ibmAlias.length())
                logMsg(MessageType.Info, "white.space.found", "ibm:alias", ibmAlias);

            if (Character.isUpperCase(trimmed.charAt(0)))
                logMsg(MessageType.Error, "alias.wrong.case", "ibm:alias", ibmAlias);
        }
    }

    private void validateIbmSupportExtensions() {
        if (ibmSupportExtensions != null) {
            if (!"true".equals(ibmSupportExtensions) && !"false".equals(ibmSupportExtensions)) {
                String trimmed = ibmSupportExtensions.trim();
                if ("true".equals(trimmed) || "false".equals(trimmed))
                    logMsg(MessageType.Info, "white.space.found", "ibm:supportExtensions", ibmSupportExtensions);
                else
                    logMsg(MessageType.Error, "invalid.value", "ibm:supportExtensions", "true|false", ibmSupportExtensions);
            }
        }
    }

    private void validateIbmSupportHiddenExtensions() {
        if (ibmSupportHiddenExtensions != null) {
            if (!"true".equals(ibmSupportHiddenExtensions) && !"false".equals(ibmSupportHiddenExtensions)) {
                String trimmed = ibmSupportHiddenExtensions.trim();
                if ("true".equals(trimmed) || "false".equals(trimmed))
                    logMsg(MessageType.Info, "white.space.found", "ibm:ibmSupportHiddenExtensions", ibmSupportHiddenExtensions);
                else
                    logMsg(MessageType.Error, "invalid.value", "ibm:ibmSupportHiddenExtensions", "true|false", ibmSupportHiddenExtensions);
            }
        }
    }

    private void validateIbmRequireExplicitConfiguration() {
        if (ibmRequireExplicitConfiguration != null) {
            if (!"true".equals(ibmRequireExplicitConfiguration) && !"false".equals(ibmRequireExplicitConfiguration)) {
                String trimmed = ibmRequireExplicitConfiguration.trim();
                if ("true".equals(trimmed) || "false".equals(trimmed))
                    logMsg(MessageType.Info, "white.space.found", "ibm:ibmRequireExplicitConfiguration", ibmRequireExplicitConfiguration);
                else
                    logMsg(MessageType.Error, "invalid.value", "ibm:ibmRequireExplicitConfiguration", "true|false", ibmRequireExplicitConfiguration);
            }
        }
    }

    private void validateIbmExtraProperties() {
        if (ibmExtraProperties != null) {
            if (!"true".equals(ibmExtraProperties) && !"false".equals(ibmExtraProperties)) {
                String trimmed = ibmExtraProperties.trim();
                if ("true".equals(trimmed) || "false".equals(trimmed))
                    logMsg(MessageType.Info, "white.space.found", "ibm:extraProperties", ibmExtraProperties);
                else
                    logMsg(MessageType.Error, "invalid.value", "ibm:extraProperties", "true|false", ibmExtraProperties);
            }
        }
    }

    private void validateIbmAction() {
        if (ibmAction != null) {
            if (!"generateSchema".equals(ibmAction)) {
                String trimmed = ibmAction.trim();
                if ("generateSchema".equals(trimmed))
                    logMsg(MessageType.Info, "white.space.found", "ibmui:action", ibmAction);
                else
                    logMsg(MessageType.Error, "invalid.value", "ibmui:action", "generateSchema", ibmAction);
            }
        }
    }

    private void validateIbmuiLocalization() {
        // NOTE: There are valid reasons for ibmui:localization to be something other than the metatype
        // localization, but they are rare enough to be covered by validator.excludes.
        if (ibmuiLocalization != null) {
            String localization = root.getLocalization();
            if (!localization.equals(ibmuiLocalization)) {
                String trimmed = ibmuiLocalization.trim();
                if (localization.length() != trimmed.length())
                    logMsg(MessageType.Info, "white.space.found", "ibmui:localization", ibmuiLocalization);

                if (!localization.equals(trimmed))
                    logMsg(MessageType.Error, "invalid.value", "ibmui:localization", "OSGI-INF/l10n/metatype", ibmuiLocalization);
            }
        }
    }

    private void validateId() {
        if (id != null) {
            String trimmed = id.trim();

            if (trimmed.length() != id.length())
                logMsg(MessageType.Info, "white.space.found", "id", id);

            if (trimmed.isEmpty())
                logMsg(MessageType.Error, "missing.attribute", "id");
        } else
            logMsg(MessageType.Error, "missing.attribute", "id");
    }

    private void validateName() {
        if (name != null) {
            String trimmed = name.trim();
            if (trimmed.length() != name.length())
                logMsg(MessageType.Info, "white.space.found", "name", name);

            if (trimmed.isEmpty())
                logMsg(MessageType.Error, "missing.attribute", "name");
            else if (!INTERNAL.equals(trimmed)) {
                if (!trimmed.startsWith("%"))
                    logMsg(MessageType.Error, "needs.translation", "name", name);
                else {
                    String key = trimmed.substring(1);
                    if (!isNlsKeyValid(key))
                        logMsg(MessageType.Error, "invalid.nls.key", key);
                }
            }
        } else
            logMsg(MessageType.Error, "missing.attribute", "name");
    }

    private void validateDescription() {
        if (name != null && INTERNAL.equals(name.trim()))
            return;

        if (description != null) {
            String trimmed = description.trim();
            if (trimmed.length() != description.length())
                logMsg(MessageType.Info, "white.space.found", "description", description);

            if (trimmed.isEmpty())
                logMsg(MessageType.Error, "missing.attribute", "description");
            else if (!INTERNAL_USE_ONLY.equals(trimmed)) {
                if (!trimmed.startsWith("%"))
                    logMsg(MessageType.Error, "needs.translation", "description", description);
                else {
                    String key = trimmed.substring(1);
                    if (!isNlsKeyValid(key))
                        logMsg(MessageType.Error, "invalid.nls.key", key);
                }
            }
        } else
            logMsg(MessageType.Error, "missing.attribute", "description");
    }

    public List<MetatypeAd> getAds() {
        return ads;
    }

    /**
     * @return
     */
    public Set<String> getIbmObjectClass() {
        if (ibmObjectClass != null) {
            return new HashSet<String>(Arrays.asList(ibmObjectClass.split("[, ]+")));
        }
        return Collections.emptySet();
    }

}
