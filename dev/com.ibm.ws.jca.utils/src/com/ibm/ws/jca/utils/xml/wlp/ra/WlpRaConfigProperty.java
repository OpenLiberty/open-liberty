/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.wlp.ra;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.metagen.MetatypeGenerator;

/**
 * wlp-ra.xml config-property
 */
@Trivial
@XmlType
public class WlpRaConfigProperty {

    private String wlp_propertyName;
    @XmlAttribute(name = "default")
    private String wlp_default;
    @XmlAttribute(name = "action")
    private String wlp_action;
    @XmlAttribute(name = "min")
    private String wlp_min;
    @XmlAttribute(name = "max")
    private String wlp_max;
    @XmlAttribute(name = "final")
    private Boolean wlp_ibmFinal;
    @XmlAttribute(name = "required")
    private Boolean wlp_required;
    @XmlAttribute(name = "group")
    private String wlp_ibmuiGroup;
    @XmlAttribute(name = "type")
    private String wlp_type;
    @XmlElement(name = "option")
    private final List<WlpConfigOption> wlp_options = new LinkedList<WlpConfigOption>();
    @XmlAttribute(name = "dynamicImportPackages")
    private String wlp_dynamicImportPackages;
    @XmlAttribute(name = "disableOptionLabelNLS")
    private Boolean wlp_disableOptionLabelNLS;
    @XmlAttribute(name = "nlsKey")
    private String wlp_nlsKey;

    public String getWlpPropertyName() {
        return wlp_propertyName;
    }

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

    @XmlAttribute(name = "config-property-name")
    public void setWlpPropertyName(String name) {
        wlp_propertyName = MetatypeGenerator.toCamelCase(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
        sb.append("wlp_propertyName='").append(wlp_propertyName).append("' ");
        if (wlp_action != null)
            sb.append("wlp_action='").append(wlp_action).append("' ");
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

}
