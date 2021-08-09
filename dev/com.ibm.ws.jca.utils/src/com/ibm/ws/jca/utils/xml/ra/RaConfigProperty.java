/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.ra;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.metagen.MetatypeGenerator;
import com.ibm.ws.jca.utils.xml.metatype.MetatypeAd;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10ConfigProperty;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpConfigOption;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConfigProperty;

/**
 * ra.xml config-property
 */
@Trivial
@XmlType(propOrder = { "description", "name", "type", "default", "ignore", "supportsDynamicUpdates", "confidential" })
public class RaConfigProperty {
    private static final TraceComponent tc = Tr.register(RaConfigProperty.class);

    @XmlTransient
    public boolean isProcessed = false;

    private List<RaDescription> description = new LinkedList<RaDescription>();

    private String name;
    private String type;
    private String defaultValue;
    private Boolean ignore = null;
    private Boolean supportsDynamicUpdates = null;
    private Boolean confidential = null;
    private String id;

    // wlp-ra.xml settings
    @XmlTransient
    private String wlp_propertyName;
    @XmlTransient
    private String wlp_default;
    @XmlTransient
    private String wlp_action;
    @XmlTransient
    private String wlp_min;
    @XmlTransient
    private String wlp_max;
    @XmlTransient
    private Boolean wlp_ibmFinal;
    @XmlTransient
    private Boolean wlp_required;
    @XmlTransient
    private String wlp_ibmuiGroup;
    @XmlTransient
    private String wlp_type;
    @XmlTransient
    private List<WlpConfigOption> wlp_options = new LinkedList<WlpConfigOption>();
    @XmlTransient
    private String wlp_dynamicImportPackages;
    @XmlTransient
    private Boolean wlp_disableOptionLabelNLS;
    @XmlTransient
    private String wlp_nlsKey;

    public String getNLSKey() {
        return wlp_nlsKey;
    }

    public boolean isOptionLabelNLSDisabled() {
        return wlp_disableOptionLabelNLS != null ? wlp_disableOptionLabelNLS : false;
    }

    /**
     * Should this property be added to the generated metatype? Based on
     * "action" property in wlp-ra.xml. If the property is not specified then
     * false is returned. True is only returned when the value is "add".
     * 
     * @return true if this property should be added the generated metatype, else false
     */
    public boolean addWlpPropertyToMetatype() {
        if (wlp_action == null)
            return false;

        return wlp_action.equals("add");
    }

    public String getWlpAction() {
        return wlp_action;
    }

    public String getWlpPropertyName() {
        return wlp_propertyName;
    }

    public String getWlpDefault() {
        return wlp_default;
    }

    public String getDynamicImportPackages() {
        return wlp_dynamicImportPackages;
    }

    public String getMin() {
        return wlp_min;
    }

    public String getMax() {
        return wlp_max;
    }

    public Boolean getIbmFinal() {
        return wlp_ibmFinal;
    }

    public Boolean getRequired() {
        return wlp_required;
    }

    public String getIbmuiGroup() {
        return wlp_ibmuiGroup;
    }

    public String getWlpType() {
        return wlp_type;
    }

    public List<WlpConfigOption> getConfigOptions() {
        return wlp_options;
    }

    @XmlElement(name = "description")
    public void setDescription(List<RaDescription> description) {
        this.description = description;
    }

    public List<RaDescription> getDescription() {
        return description;
    }

    @XmlElement(name = "config-property-name", required = true)
    public void setName(String name) {
        this.name = MetatypeGenerator.toCamelCase(name);
    }

    public String getName() {
        return name != null ? name : wlp_propertyName;
    }

    @XmlElement(name = "config-property-type", required = true)
    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        if (type != null && !MetatypeAd.isTypeClassName(type)) {
            throw new IllegalArgumentException(Tr.formatMessage(tc, "J2CA9906.invalid.type",
                                                                type, name));
        }
        return type;
    }

    @XmlElement(name = "config-property-value")
    public void setDefault(String defaultValue) {
        if (defaultValue != null)
            this.defaultValue = defaultValue.replaceAll("\\,", "\\\\,");
    }

    public String getDefault() {
        return defaultValue;
    }

    @XmlElement(name = "config-property-ignore")
    public void setIgnore(Boolean ignore) {
        this.ignore = ignore;
    }

    public Boolean getIgnore() {
        if (wlp_action != null)
            return wlp_action.equals("ignore");
        else
            return ignore;
    }

    @XmlElement(name = "config-property-supports-dynamic-updates")
    public void setSupportsDynamicUpdates(Boolean supportsDynamicUpdates) {
        this.supportsDynamicUpdates = supportsDynamicUpdates;
    }

    public Boolean getSupportsDynamicUpdates() {
        return supportsDynamicUpdates;
    }

    @XmlElement(name = "config-property-confidential")
    public void setConfidential(Boolean confidential) {
        this.confidential = confidential;
    }

    public Boolean getConfidential() {
        return confidential;
    }

    public void copyRa10Settings(Ra10ConfigProperty ra10ConfigProperty) {
        name = ra10ConfigProperty.getConfigPropertyName();
        setType(ra10ConfigProperty.getConfigPropertyType());
        defaultValue = ra10ConfigProperty.getConfigPropertyValue();
        if (ra10ConfigProperty.getDescription() != null) {
            RaDescription desc = new RaDescription();
            desc.setValue(ra10ConfigProperty.getDescription());
            description.add(desc);
        }
    }

    public void copyRaSettings(RaConfigProperty configProperty) {
        description = configProperty.getDescription();
        name = configProperty.getName();
        type = configProperty.getType();
        defaultValue = configProperty.getDefault();
        ignore = configProperty.getIgnore();
        supportsDynamicUpdates = configProperty.getSupportsDynamicUpdates();
        confidential = configProperty.getConfidential();
        id = configProperty.getId();
        wlp_action = configProperty.getWlpAction();
        wlp_default = configProperty.getWlpDefault();
        wlp_dynamicImportPackages = configProperty.getDynamicImportPackages();
        wlp_ibmFinal = configProperty.getIbmFinal();
        wlp_ibmuiGroup = configProperty.getIbmuiGroup();
        wlp_max = configProperty.getMax();
        wlp_min = configProperty.getMin();
        wlp_options = configProperty.getConfigOptions();
        wlp_required = configProperty.getRequired();
        wlp_type = configProperty.getWlpType();
        wlp_disableOptionLabelNLS = configProperty.isOptionLabelNLSDisabled();
        wlp_nlsKey = configProperty.getNLSKey();
        wlp_propertyName = configProperty.getWlpPropertyName();

    }

    public void copyWlpSettings(WlpRaConfigProperty configProperty) {
        wlp_action = configProperty.getWlpAction();
        wlp_default = configProperty.getWlpDefault();
        wlp_dynamicImportPackages = configProperty.getDynamicImportPackages();
        wlp_ibmFinal = configProperty.getIbmFinal();
        wlp_ibmuiGroup = configProperty.getIbmuiGroup();
        wlp_max = configProperty.getMax();
        wlp_min = configProperty.getMin();
        wlp_options = configProperty.getConfigOptions();
        wlp_required = configProperty.getRequired();
        wlp_type = configProperty.getWlpType();
        wlp_disableOptionLabelNLS = configProperty.isOptionLabelNLSDisabled();
        wlp_nlsKey = configProperty.getNLSKey();
        wlp_propertyName = configProperty.getWlpPropertyName();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
        if (name != null)
            sb.append("name='").append(name).append("' ");
        else
            sb.append("wlp_propertyName='").append(wlp_propertyName).append("' ");
        if (type != null)
            sb.append("type='").append(type).append("' ");
        if (wlp_action != null)
            sb.append("wlp_action='").append(wlp_action).append("' ");
        if (description != null)
            sb.append("description='").append(description).append("' ");
        if (confidential != null)
            sb.append("confidential='").append(confidential).append("' ");
        if (ignore != null)
            sb.append("ignore='").append(ignore).append("' ");
        if (supportsDynamicUpdates != null)
            sb.append("supportsDynamicUpdates='").append(supportsDynamicUpdates).append("' ");
        if (defaultValue != null)
            sb.append("defaultValue='").append(defaultValue).append("' ");
        sb.append("isProcessed='").append(isProcessed).append("' ");
        if (wlp_min != null)
            sb.append("wlp_min='").append(wlp_min).append("' ");
        if (wlp_max != null)
            sb.append("wlp_max='").append(wlp_max).append("' ");
        if (wlp_ibmFinal != null)
            sb.append("wlp_ibmFinal='").append(wlp_ibmFinal).append("' ");
        if (wlp_required != null)
            sb.append("wlp_required='").append(wlp_required).append("' ");
        if (wlp_ibmuiGroup != null)
            sb.append("wlp_ibmuiGroup='").append(wlp_ibmuiGroup).append("' ");
        if (wlp_type != null)
            sb.append("wlp_type='").append(wlp_type).append("' ");
        if (wlp_default != null)
            sb.append("wlp_default='").append(wlp_default).append("' ");
        if (wlp_disableOptionLabelNLS != null)
            sb.append("wlp_disableOptionLabelNLS='").append(wlp_disableOptionLabelNLS).append("' ");
        if (wlp_nlsKey != null)
            sb.append("wlp_nlsKey='").append(wlp_nlsKey).append("' ");
        if (!wlp_options.isEmpty()) {
            sb.append("wlp_options=[");
            for (int i = 0; i < wlp_options.size(); ++i) {
                sb.append(wlp_options.get(i));

                if (i + 1 != wlp_options.size())
                    sb.append(',');
            }
            sb.append("] ");
        }

        sb.append('}');
        return sb.toString();
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    @XmlID
    @XmlAttribute(name = "id")
    public void setId(String id) {
        this.id = id;
    }
}
