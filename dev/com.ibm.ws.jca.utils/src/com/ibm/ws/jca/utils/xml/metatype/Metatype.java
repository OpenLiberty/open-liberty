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
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.Utils;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;

/**
 * Represents a metatype.xml containing Designates and OCDs.
 */
@Trivial
@XmlRootElement(name = "MetaData", namespace = "http://www.osgi.org/xmlns/metatype/v1.1.0")
public class Metatype {
    private static final TraceComponent tc = Tr.register(Metatype.class);

    @XmlElement(name = "Designate")
    private final List<MetatypeDesignate> designates = new LinkedList<MetatypeDesignate>();
    @XmlElement(name = "OCD")
    private final List<MetatypeOcd> ocds = new LinkedList<MetatypeOcd>();
    private RaConnector originatingRaConnector;
    private final List<String> ibmuiGroupOrder = new ArrayList<String>();

    public void setIbmuiGroupOrder(String order) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        if (order == null || (order.isEmpty()))
            return;

        if (!order.isEmpty() && !ibmuiGroupOrder.isEmpty() && trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "Attempted to set ibmui:group-order, but it's already been set.");

        String[] groups = order.split(",");
        boolean defaultFound = false;
        for (String group : groups) {
            if (group != null && !group.isEmpty()) {
                group = group.trim();
                if (group.equals("(default)"))
                    defaultFound = true;

                if (ibmuiGroupOrder.contains(group)) {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "There are duplications of group " + group + " listed in ibmui:group-order. Duplications will be ignored.");
                    continue;
                }

                ibmuiGroupOrder.add(group);
            }
        }

        if (!defaultFound)
            ibmuiGroupOrder.add(0, "(default)");
    }

    public List<String> getIbmuiGroupOrder() {
        return ibmuiGroupOrder;
    }

    public void addDesignate(MetatypeDesignate designate) {
        designates.add(designate);
    }

    public void addOcd(MetatypeOcd ocd) {
        ocds.add(ocd);
    }

    public MetatypeDesignate getDesignateByPid(String pid) {
        for (MetatypeDesignate designate : this.designates)
            if (designate.getPid() != null && designate.getPid().equals(pid))
                return designate;

        return null;
    }

    public MetatypeDesignate getDesignateByFactoryPid(String factoryPid) {
        for (MetatypeDesignate designate : this.designates)
            if (designate.getFactoryPid() != null && designate.getFactoryPid().equals(factoryPid))
                return designate;

        return null;
    }

    public List<MetatypeDesignate> getDesignates() {
        return this.designates;
    }

    public MetatypeOcd getOcdById(String id) {
        for (MetatypeOcd ocd : this.ocds)
            if (ocd.getId() != null && ocd.getId().equals(id))
                return ocd;

        return null;
    }

    public List<MetatypeOcd> getOcds() {
        return this.ocds;
    }

    public RaConnector getOriginatingRaConnector() {
        return originatingRaConnector;
    }

    @XmlTransient
    public void setOriginatingRaConnector(RaConnector originatingRaConnector) {
        this.originatingRaConnector = originatingRaConnector;
    }

    @Override
    public String toString() {
        for (MetatypeOcd ocd : ocds) {
            ocd.setParentMetatype(this);
            ocd.sort(true);
        }

        StringBuilder sb = new StringBuilder("Metatype{Designate=[");

        for (int i = 0; i < designates.size(); ++i) {
            String id = designates.get(i).getFactoryPid();
            if (id == null)
                id = designates.get(i).getPid();

            sb.append('\'').append(id).append('\'');

            if (i + 1 != designates.size())
                sb.append(',');
        }
        sb.append("] OCD=[");

        for (int i = 0; i < ocds.size(); ++i) {
            sb.append('\'').append(ocds.get(i).getId()).append('\'');

            if (i + 1 != ocds.size())
                sb.append(',');
        }
        sb.append("]}");

        return sb.toString();
    }

    /**
     * Returns the full metatype in the form of a structured String.<br>
     * <br>
     * Headers:
     * <pre>
     * &#60?xml ... ?&#62
     * &#60metatype:MetaData ... &#62
     * &#32&#60!-- metatype body --&#62
     * &#60/metatype:MetaData&#62
     * </pre>
     * 
     * @param includeHeaders if true, output includes the XML/metatype headers
     * @return String containing formatted metatype
     */
    public String toMetatypeString(boolean includeHeaders) {
        for (MetatypeOcd ocd : ocds) {
            ocd.setParentMetatype(this);
            ocd.sort(true);
        }

        int padSpaces = 0;
        StringBuilder sb = new StringBuilder();

        if (includeHeaders) {
            ++padSpaces;

            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(Utils.NEW_LINE);
            sb.append("<!-- Created by Metatype Generator (").append(new Date()).append(") -->").append(Utils.NEW_LINE);
            sb.append("<metatype:MetaData xmlns:metatype=\"http://www.osgi.org/xmlns/metatype/v1.1.0\"").append(Utils.NEW_LINE);
            sb.append("                   xmlns:ibm=\"http://www.ibm.com/xmlns/appservers/osgi/metatype/v1.0.0\"").append(Utils.NEW_LINE);
            sb.append("                   xmlns:ibmui=\"http://www.ibm.com/xmlns/appservers/osgi/metatype/ui/v1.0.0\"").append(Utils.NEW_LINE);
            sb.append("                   localization=\"OSGI-INF/l10n/metatype\">").append(Utils.NEW_LINE);
            sb.append(Utils.NEW_LINE);
        } else
            sb.append("<!-- Created by Metatype Generator (").append(new Date()).append(") -->").append(Utils.NEW_LINE);

        String buffer = Utils.getSpaceBufferString(padSpaces);

        for (MetatypeDesignate designate : this.designates) {
            if (designate.getPid() != null) {
                sb.append(buffer).append("<Designate pid=\"").append(designate.getPid()).append("\">").append(Utils.NEW_LINE);
                sb.append(designate.getObject().toMetatypeString(padSpaces + 1)).append(Utils.NEW_LINE);
                sb.append(buffer).append("</Designate>").append(Utils.NEW_LINE);
            } else if (designate.getFactoryPid() != null) {
                sb.append(buffer).append("<Designate factoryPid=\"").append(designate.getFactoryPid()).append("\">").append(Utils.NEW_LINE);
                sb.append(designate.getObject().toMetatypeString(padSpaces + 1)).append(Utils.NEW_LINE);
                sb.append(buffer).append("</Designate>").append(Utils.NEW_LINE);
            }

            sb.append(Utils.NEW_LINE);
            MetatypeOcd ocd = getOcdById(designate.getObject().getOcdref());
            sb.append(ocd.toMetatypeString(padSpaces)).append(Utils.NEW_LINE);
            sb.append(Utils.NEW_LINE);
        }

        if (includeHeaders) {
            sb.append("</metatype:MetaData>");
        }

        return sb.toString();
    }
}
