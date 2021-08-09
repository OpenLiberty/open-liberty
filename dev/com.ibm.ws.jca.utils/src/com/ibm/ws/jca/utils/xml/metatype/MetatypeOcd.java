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
package com.ibm.ws.jca.utils.xml.metatype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.metatype.ObjectClassDefinitionProperties;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.Utils;

/**
 * Metatype Object Class Definition (OCD)
 */
@Trivial
public class MetatypeOcd implements Comparator<MetatypeAd> {
    private String id;
    private String ibmAlias;
    private String ibmChildAlias;
    private String ibmParentPid;
    private String ibmExtendsAlias;
    private String ibmExtends;
    private String name;
    private String description;
    private List<MetatypeAd> metatypeAds = new LinkedList<MetatypeAd>();
    private Metatype parent;
    private final MetaTypeFactory metaTypeProviderFactory;

    public MetatypeOcd(MetaTypeFactory mtpService) {
        this.metaTypeProviderFactory = mtpService;
    }

    @Override
    public int compare(MetatypeAd ad1, MetatypeAd ad2) {
        return ad1.getID().toLowerCase().compareTo(ad2.getID().toLowerCase());
    }

    public void sort(boolean sortByGroup) {
        if (sortByGroup && parent != null) {
            HashMap<String, List<MetatypeAd>> adsByGroup = new HashMap<String, List<MetatypeAd>>();

            for (MetatypeAd ad : metatypeAds) {
                String group = ad.getIbmUigroup();
                if (group == null)
                    group = "(default)";

                List<MetatypeAd> list = adsByGroup.get(group);
                if (list == null) {
                    list = new LinkedList<MetatypeAd>();
                    adsByGroup.put(group, list);
                }

                list.add(ad);
            }

            for (Entry<String, List<MetatypeAd>> entry : adsByGroup.entrySet()) {
                Collections.sort(entry.getValue(), this);
            }

            metatypeAds.clear();
            List<String> orderedGroupNames = parent.getIbmuiGroupOrder();
            if (orderedGroupNames != null && !orderedGroupNames.isEmpty()) {
                for (String groupName : orderedGroupNames) {
                    List<MetatypeAd> group = adsByGroup.remove(groupName);
                    if (group != null)
                        metatypeAds.addAll(group);
                }
            } else {
                metatypeAds.addAll(adsByGroup.remove("(default)"));
                for (List<MetatypeAd> adGroup : adsByGroup.values())
                    metatypeAds.addAll(adGroup);
            }
        } else {
            Collections.sort(metatypeAds, this);
        }
    }

    public void setParentMetatype(Metatype parent) {
        this.parent = parent;
    }

    @XmlAttribute(name = "id")
    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute(name = "alias", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0")
    public void setAlias(String ibmAlias) {
        this.ibmAlias = ibmAlias;
    }

    @XmlAttribute(name = "childAlias", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0")
    public void setChildAlias(String ibmChildAlias) {
        this.ibmChildAlias = ibmChildAlias;
    }

    @XmlAttribute(name = "parentPid", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0")
    public void setParentPID(String ibmParentPid) {
        this.ibmParentPid = ibmParentPid;
    }

    @XmlAttribute(name = "extendsAlias", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0")
    public void setExtendsAlias(String ibmExtendsAlias) {
        this.ibmExtendsAlias = ibmExtendsAlias;
    }

    @XmlAttribute(name = "extends", namespace = "http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0")
    public void setExtends(String ibmExtends) {
        this.ibmExtends = ibmExtends;
    }

    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    @XmlAttribute(name = "description")
    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement(name = "AD")
    public void setMetatypeAds(List<MetatypeAd> metatypeAds) {
        this.metatypeAds = metatypeAds;
    }

    /**
     * Adds an internal, ibm:final metatype ad
     *
     * @param id id
     * @param value default value
     */
    public void addInternalMetatypeAd(String id, String value) {
        MetatypeAd ad = new MetatypeAd(metaTypeProviderFactory);
        ad.setId(id);
        ad.setType("String");
        ad.setDefault(value);
        ad.setFinal(true);
        ad.setName("internal");
        ad.setDescription("internal use only");
        addMetatypeAd(ad);
    }

    /**
     * Adds a metatype AD.
     *
     * @param metatypeAd the MetatypeAd to add
     * @return true if the MetatypeAd was added, else false if it already
     *         exists in the list
     */
    public boolean addMetatypeAd(MetatypeAd metatypeAd) {
        if (this.metatypeAds == null)
            this.metatypeAds = new LinkedList<MetatypeAd>();

        for (MetatypeAd ad : metatypeAds)
            if (ad.getID().equals(metatypeAd.getID()))
                return false;

        this.metatypeAds.add(metatypeAd);
        return true;
    }

    public String getId() {
        return id;
    }

    public ObjectClassDefinition getObjectClassDefinition() {
        ObjectClassDefinitionProperties props = new ObjectClassDefinitionProperties(id);
        props.setAlias(ibmAlias);
        props.setChildalias(ibmChildAlias);
        props.setParentPID(ibmParentPid);
        props.setExtendsAlias(ibmExtendsAlias);
        props.setExtends(ibmExtends);
        props.setName(name);
        props.setDescription(description);

        List<AttributeDefinition> attributes = new ArrayList<AttributeDefinition>();
        for (MetatypeAd ad : getMetatypeAds()) {
            attributes.add(ad.getAttributeDefinition());
        }

        return metaTypeProviderFactory.createObjectClassDefinition(props, attributes, Collections.<AttributeDefinition> emptyList());
    }

    public List<MetatypeAd> getMetatypeAds() {
        return metatypeAds;
    }

    public MetatypeAd getMetatypeAdByID(String id) {
        for (MetatypeAd ad : metatypeAds) {
            if (ad.getID().equals(id))
                return ad;
        }

        return null;
    }

    @Override
    public String toString() {
        return "MetatypeOcd{id='" + id + "'}";
    }

    public String toMetatypeString(int padSpaces) {
        String buffer = Utils.getSpaceBufferString(padSpaces);
        StringBuilder sb = new StringBuilder(buffer).append("<OCD ");

        sb.append("id=\"").append(id).append("\" ");

        if (ibmAlias != null)
            sb.append("ibm:alias=\"").append(ibmAlias).append("\" ");
        if (ibmChildAlias != null)
            sb.append("ibm:childAlias=\"").append(ibmChildAlias).append("\" ");
        if (ibmParentPid != null)
            sb.append("ibm:parentPid=\"").append(ibmParentPid).append("\" ");
        if (ibmExtendsAlias != null)
            sb.append("ibm:extendsAlias=\"").append(ibmExtendsAlias).append("\" ");
        if (ibmExtends != null)
            sb.append("ibm:extends=\"").append(ibmExtends).append("\" ");
        if (name != null)
            sb.append("name=\"").append(name).append("\" ");
        if (description != null)
            sb.append("description=\"").append(description).append("\" ");
        sb.append(">" + Utils.NEW_LINE);

        for (MetatypeAd ad : this.metatypeAds) {
            if (ad != null)
                sb.append(ad.toMetatypeString(padSpaces + 1) + Utils.NEW_LINE);
        }

        sb.append(buffer).append("</OCD>");
        return sb.toString();
    }

    /**
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return
     */
    public String getChildAlias() {
        return this.ibmChildAlias;
    }

    /**
     * @return
     */
    public String getExtendsAlias() {
        return this.ibmExtendsAlias;
    }

    /**
     * @return
     */
    public String getAlias() {
        return this.ibmAlias;
    }

}
