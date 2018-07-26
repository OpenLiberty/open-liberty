/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.utils.encapsulation;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.ibm.ws.jpa.diagnostics.utils.encapsulation.xsd10.EncapsulatedDataGroupType;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.xsd10.EncapsulatedDataType;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.xsd10.PropertiesType;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.xsd10.PropertyType;

public class EncapsulatedDataGroup {
    public static EncapsulatedDataGroup createEncapsulatedDataGroup(String name, String id) {
        EncapsulatedDataGroup edg = new EncapsulatedDataGroup(new EncapsulatedDataGroupType());
        edg.setId(id);
        edg.setName(name);
        return edg;
    }

    public static EncapsulatedDataGroup createEncapsulatedDataGroup(InputStream is) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(EncapsulatedDataGroupType.class);
        Unmarshaller um = jc.createUnmarshaller();

        EncapsulatedDataGroupType edt = (EncapsulatedDataGroupType) um.unmarshal(is);
        EncapsulatedDataGroup edg = new EncapsulatedDataGroup(edt);
        return edg;
    }

    private final EncapsulatedDataGroupType edgt;
    private final Map<String, EncapsulatedDataGroup> childDataGroupsMap = new HashMap<String, EncapsulatedDataGroup>();
    private final Map<String, EncapsulatedData> childDataMap = new HashMap<String, EncapsulatedData>();

    private EncapsulatedDataGroup(EncapsulatedDataGroupType edgt) {
        this.edgt = edgt;

        List<EncapsulatedDataGroupType> dataGroups = edgt.getDataGroup();
        if (dataGroups.size() > 0) {
            for (EncapsulatedDataGroupType subedgt : dataGroups) {
                EncapsulatedDataGroup edg = new EncapsulatedDataGroup(subedgt);
                childDataGroupsMap.put(edg.getId(), edg);
            }
        }

        List<EncapsulatedDataType> dataItems = edgt.getDataItem();
        if (dataItems.size() > 0) {
            for (EncapsulatedDataType dataItem : dataItems) {
                EncapsulatedData ed = EncapsulatedData.createEncapsulatedData(dataItem);
                childDataMap.put(ed.getId(), ed);
            }
        }
    }

    public String getId() {
        return edgt.getId();
    }

    public void setId(String id) {
        edgt.setId(id);
    }

    public String getName() {
        return edgt.getName();
    }

    public void setName(String name) {
        edgt.setName(name);
    }

    public Set<String> getDataSubGroupNames() {
        return Collections.unmodifiableSet(childDataGroupsMap.keySet());
    }

    public EncapsulatedDataGroup putDataSubGroup(EncapsulatedDataGroup group) {
        if (group == null) {
            return null;
        }

        internalRemoveDataGroup(group.getId());
        List<EncapsulatedDataGroupType> dataGroups = edgt.getDataGroup();
        dataGroups.add(group.edgt);

        return childDataGroupsMap.put(group.getId(), group);
    }

    public EncapsulatedDataGroup getDataSubGroup(String id) {
        return childDataGroupsMap.get(id);
    }

    public void removeDataSubGroup(String id) {
        if (id == null) {
            return;
        }
        internalRemoveDataGroup(id);
        childDataGroupsMap.remove(id);
    }

    public Set<String> getDataItemsNames() {
        return Collections.unmodifiableSet(childDataMap.keySet());
    }

    public EncapsulatedData putDataItem(EncapsulatedData item) {
        EncapsulatedData retVal = childDataMap.put(item.getId(), item);

        internalRemoveDataItem(item.getId());
        edgt.getDataItem().add(item.getEncapsulatedDataType());

        return retVal;
    }

    public EncapsulatedData getDataItem(String id) {
        return childDataMap.get(id);
    }

    public void removeDataItem(String id) {
        childDataMap.remove(id);
        internalRemoveDataItem(id);
    }

    private void internalRemoveDataGroup(String id) {
        List<EncapsulatedDataGroupType> dataGroups = edgt.getDataGroup();
        List<EncapsulatedDataGroupType> removeList = new ArrayList<EncapsulatedDataGroupType>();

        for (EncapsulatedDataGroupType dgItem : dataGroups) {
            if (dgItem.getId().equals(id)) {
                removeList.add(dgItem);
            }
        }

        if (removeList.size() > 0) {
            for (EncapsulatedDataGroupType dgItem : removeList) {
                dataGroups.remove(dgItem);
            }
        }

    }

    private void internalRemoveDataItem(String id) {
        List<EncapsulatedDataType> dataItems = edgt.getDataItem();
        List<EncapsulatedDataType> removeList = new ArrayList<EncapsulatedDataType>();
        for (EncapsulatedDataType dtItem : dataItems) {
            if (dtItem.getId().equals(id)) {
                removeList.add(dtItem);
            }
        }
        if (removeList.size() > 0) {
            for (EncapsulatedDataType dtItem : removeList) {
                dataItems.remove(dtItem);
            }
        }
    }

    public Map<String, String> getProperties() {
        HashMap<String, String> propertiesMap = new HashMap<String, String>();

        PropertiesType pt = edgt.getProperties();
        if (pt != null) {
            List<PropertyType> propList = pt.getProperty();
            for (PropertyType ptEntry : propList) {
                propertiesMap.put(ptEntry.getName(), ptEntry.getValue());
            }
        }

        return propertiesMap;
    }

    public void setProperty(String name, String value) {
        if (name == null || value == null) {
            return; // No null keys or values allowed.
        }

        PropertiesType pt = edgt.getProperties();
        if (pt == null) {
            pt = new PropertiesType();
            edgt.setProperties(pt);
        }

        List<PropertyType> propList = pt.getProperty();

        PropertyType newPt = new PropertyType();
        propList.add(newPt);
        newPt.setName(name);
        newPt.setValue(value);;
        newPt.setType(value.getClass().getCanonicalName());
    }

    public void clearProperties() {
        PropertiesType pt = edgt.getProperties();
        if (pt != null) {
            pt.getProperty().clear();
        }
    }

    public void removeProperty(String name) {
        if (name == null) {
            return;
        }

        PropertiesType pt = edgt.getProperties();
        if (pt != null) {
            List<PropertyType> propToRemove = new ArrayList<PropertyType>();
            List<PropertyType> propList = pt.getProperty();

            for (PropertyType p : propList) {
                if (p.getName().equals(name)) {
                    propToRemove.add(p);
                }
            }

            for (PropertyType p : propToRemove) {
                propList.remove(p);
            }
        }
    }

    public void write(PrintWriter pw) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(EncapsulatedDataGroupType.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(edgt, pw);
    }

    public void writeToString(OutputStream os) throws Exception {
        JAXBContext jc = JAXBContext.newInstance(EncapsulatedDataGroupType.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(edgt, os);
    }
}
