/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.schema;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;

/**
 * Custom metatypeinformation
 */
public class MetaTypeInformationSpecification implements MetaTypeInformation {

    public static final ResourceBundle messages = ResourceBundle.getBundle(XMLConfigConstants.NLS_PROPS);
    private final Map<String, ObjectClassDefinitionSpecification> objectClassMap;
    private final Map<String, DesignateSpecification> designateMap;

    private final Bundle bundle;

    public MetaTypeInformationSpecification(Bundle bundle) {
        this.objectClassMap = new HashMap<String, ObjectClassDefinitionSpecification>();
        this.designateMap = new HashMap<String, DesignateSpecification>();
        this.bundle = bundle;
    }

    public void addObjectClassSpecification(ObjectClassDefinitionSpecification ocd) {
        if (objectClassMap.containsKey(ocd.getID())) {
            throw new IllegalArgumentException(MessageFormat.format(messages.getString("error.ocdExists"), ocd.getID()));
        }
        objectClassMap.put(ocd.getID(), ocd);
    }

    public ObjectClassDefinitionSpecification getObjectClassSpecification(String id) {
        return objectClassMap.get(id);
    }

    public Collection<ObjectClassDefinitionSpecification> getObjectClassSpecifications() {
        return objectClassMap.values();
    }

    /**
     * @param parseDesignate
     */
    public void addDesignateSpecification(DesignateSpecification designate) {
        if (designateMap.containsKey(designate.getPid())) {
            throw new IllegalArgumentException(MessageFormat.format(messages.getString("error.dsExists"), designate.getPid()));
        }
        designateMap.put(designate.getPid(), designate);
    }

    public Collection<DesignateSpecification> getDesignateSpecifications() {
        return designateMap.values();
    }

    public boolean hasFactoryReference(String ocsId) {
        for (DesignateSpecification designate : designateMap.values()) {
            if (designate.isFactory() && ocsId.equals(designate.getOcdId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ObjectClassDefinition getObjectClassDefinition(String pid, String locale) {
        ObjectClassDefinitionSpecification ocd = null;
        DesignateSpecification designate = designateMap.get(pid);
        if (designate != null) {
            ocd = objectClassMap.get(designate.getOcdId());
        }
        return ocd;
    }

    @Override
    public String[] getLocales() {
        //not supported
        return null;
    }

    @Override
    public String[] getPids() {
        if (designateMap.size() == 0) {
            return new String[0];
        }

        Vector<String> pids = new Vector<String>();

        for (Map.Entry<String, DesignateSpecification> entry : designateMap.entrySet()) {
            DesignateSpecification ds = entry.getValue();
            if (!!!ds.isFactory()) {
                pids.add(ds.getPid());
            }
        }
        String[] retVal = new String[pids.size()];
        pids.toArray(retVal);
        return retVal;
    }

    @Override
    public String[] getFactoryPids() {
        if (designateMap.size() == 0) {
            return new String[0];
        }

        Vector<String> pids = new Vector<String>();

        for (Map.Entry<String, DesignateSpecification> entry : designateMap.entrySet()) {
            DesignateSpecification ds = entry.getValue();
            if (ds.isFactory()) {
                pids.add(ds.getPid());
            }
        }
        String[] retVal = new String[pids.size()];
        pids.toArray(retVal);
        return retVal;
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    public void validate() {
        Iterator<DesignateSpecification> iterator = designateMap.values().iterator();
        while (iterator.hasNext()) {
            DesignateSpecification designate = iterator.next();
            ObjectClassDefinitionSpecification ocd = objectClassMap.get(designate.getOcdId());
            if (ocd == null) {
                // log bad reference error
                System.out.println(MessageFormat.format(messages.getString("error.invalidOCDRef"), designate.getPid(), designate.getOcdId()));
                // remove bad reference from designate list
                iterator.remove();
            }
        }
    }
}
