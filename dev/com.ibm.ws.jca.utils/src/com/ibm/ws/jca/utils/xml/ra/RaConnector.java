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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.connector.Connector;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10Connector;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10Icon;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpIbmuiGroups;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnector;

/**
 * ra.xml connector element. We are using this for now, until we get the STAX Parser implemented. If we do not go for the parser, then we can use
 * this with some refactoring such that there are implementations for all declared by Connector interface.
 */
@Trivial
@XmlRootElement(name = "connector")
@XmlType(name = "connectorType", propOrder = { "moduleName", "description", "displayName", "icon", "vendorName", "eisType",
                                              "resourceAdapterVersion", "license",
                                              "resourceAdapter", "requiredWorkContext" })
public class RaConnector implements Connector {
    private String version;
    private List<RaDescription> description = new LinkedList<RaDescription>();
    private List<RaDisplayName> displayName = new LinkedList<RaDisplayName>();
    private String vendorName;
    private String eisType;
    private String moduleName;
    private String resourceAdapterVersion;
    private RaResourceAdapter resourceAdapter;
    private List<String> requiredWorkContext = new LinkedList<String>();
    @XmlElement(name = "icon")
    private final List<RaIcon> icon = new LinkedList<RaIcon>();
    private RaLicense license;
    private Boolean metadataComplete;
    private String id;

    @XmlTransient
    private WlpIbmuiGroups wlp_ibmuiGroups;

    public WlpIbmuiGroups getWlpIbmuiGroups() {
        return wlp_ibmuiGroups;
    }

    @XmlAttribute(name = "version", required = true)
    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    @XmlElement(name = "description")
    public void setDescription(List<RaDescription> description) {
        this.description = description;
    }

    public List<RaDescription> getDescription() {
        return description;
    }

    @XmlElement(name = "display-name")
    public void setDisplayName(List<RaDisplayName> displayName) {
        this.displayName = displayName;
    }

    public List<RaDisplayName> getDisplayName() {
        return displayName;
    }

    @XmlElement(name = "vendor-name")
    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorName() {
        return vendorName;
    }

    @XmlElement(name = "eis-type")
    public void setEisType(String eisType) {
        this.eisType = eisType;
    }

    public String getEisType() {
        return eisType;
    }

    @XmlElement(name = "resourceadapter-version")
    public void setResourceAdapterVersion(String resourceAdapterVersion) {
        this.resourceAdapterVersion = resourceAdapterVersion;
    }

    public String getResourceAdapterVersion() {
        return resourceAdapterVersion;
    }

    @XmlElement(name = "resourceadapter", required = true)
    public void setResourceAdapter(RaResourceAdapter resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    public RaResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    @XmlElement(name = "module-name")
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @XmlElement(name = "required-work-context")
    public void setRequiredWorkContext(List<String> requiredWorkContext) {
        this.requiredWorkContext = requiredWorkContext;
    }

    public List<String> getRequiredWorkContext() {
        return requiredWorkContext;
    }

    @XmlElement(name = "license")
    public void setLicense(RaLicense license) {
        this.license = license;
    }

    public RaLicense getLicense() {
        return license;
    }

    public void copyWlpSettings(WlpRaConnector connector) {
        wlp_ibmuiGroups = connector.getWlpIbmuiGroups();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaConnector{display-name='");
        if (displayName != null)
            sb.append(displayName);
        sb.append("'}");
        return sb.toString();
    }

    public RaAdminObject getAdminObject(String adminObjectInterface, String adminObjectClass) {
        return resourceAdapter.getAdminObject(adminObjectInterface, adminObjectClass);
    }

    public RaConnectionDefinition getConnectionDefinition(String connectionFactoryInterface) {
        RaOutboundResourceAdapter outbound = resourceAdapter.getOutboundResourceAdapter();
        if (outbound == null)
            return null;
        else
            return outbound.getConnectionDefinitionByInterface(connectionFactoryInterface);
    }

    public RaMessageListener getMessageListener(String messageListenerType) {
        RaInboundResourceAdapter inbound = resourceAdapter.getInboundResourceAdapter();
        if (inbound == null)
            return null;
        else {
            RaMessageAdapter messageAdapter = inbound.getMessageAdapter();
            if (messageAdapter == null)
                return null;
            else {
                return messageAdapter.getMessageListenerByType(messageListenerType);
            }
        }
    }

    /**
     * @return &lt;display-name> as a read-only list
     */
    @Override
    public List<DisplayName> getDisplayNames() {
        List<DisplayName> names = new ArrayList<DisplayName>();
        for (RaDisplayName name : displayName) {
            names.add(name);
        }
        return names;
    }

    /**
     * @return &lt;icon> as a read-only list
     */
    @Override
    public List<Icon> getIcons() {
        List<Icon> icons = new ArrayList<Icon>();
        for (RaIcon raIcon : icon) {
            icons.add(raIcon);
        }
        return icons;
    }

    /**
     * @return &lt;description> as a read-only list
     */
    @Override
    public List<Description> getDescriptions() {
        List<Description> descs = new ArrayList<Description>();
        for (RaDescription raDesc : description) {
            descs.add(raDesc);
        }
        return descs;
    }

    /**
     * @return the metadataComplete
     */
    public Boolean getMetadataComplete() {
        if (metadataComplete == null)
            metadataComplete = Boolean.FALSE;
        return metadataComplete;
    }

    /**
     * @param metadataComplete the metadataComplete to set
     */
    @XmlAttribute(name = "metadata-complete")
    public void setMetadataComplete(Boolean metadataComplete) {
        this.metadataComplete = metadataComplete;
    }

    public void copyRa10Settings(Ra10Connector ra10Connector) {
        if (ra10Connector.getDescription() != null) {
            RaDescription desc = new RaDescription();
            desc.setValue(ra10Connector.getDescription());
            description.add(desc);
        }
        RaDisplayName name = new RaDisplayName();
        name.setValue(ra10Connector.getDisplayName());
        this.displayName.add(name);
        Ra10Icon ra10Icon = ra10Connector.getIcon();
        if (ra10Icon != null) {
            RaIcon raIcon = new RaIcon();
            raIcon.copyRa10Settings(ra10Icon);
            icon.add(raIcon);
        }
        this.eisType = ra10Connector.getEisType();
        if (ra10Connector.getLicense() != null) {
            this.license = new RaLicense();
            license.copyRa10Settings(ra10Connector.getLicense());
        }
        this.resourceAdapter = new RaResourceAdapter();
        resourceAdapter.copyRa10Settings(ra10Connector.getResourceAdapter());
        this.version = ra10Connector.getSpecVersion();
        this.vendorName = ra10Connector.getVendorName();
        this.resourceAdapterVersion = ra10Connector.getVersion();

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
