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
package com.ibm.ws.config.metatype.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.ibm.websphere.metatype.AttributeDefinitionProperties;
import com.ibm.websphere.metatype.MetaTypeFactory;
import com.ibm.websphere.metatype.ObjectClassDefinitionProperties;

/**
 *
 */
@Component(service = { Animal.class, ManagedServiceFactory.class, MetaTypeProvider.class }, immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { Constants.SERVICE_VENDOR + "=" + "IBM",
                       Constants.SERVICE_PID + "=" + Animal.ANIMAL_PID })
public class Animal implements ManagedServiceFactory, MetaTypeProvider {

    public static final String ANIMAL_PID = "test.metatype.provider.animal";
    private ObjectClassDefinition ocd;
    private MetaTypeFactory metaTypeFactoryService;

    @Reference(service = MetaTypeFactory.class,
               cardinality = ReferenceCardinality.MANDATORY)
    protected void setMetaTypeFactoryService(MetaTypeFactory mtpService) {
        this.metaTypeFactoryService = mtpService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
     */
    @Override
    public String[] getLocales() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String, java.lang.String)
     */
    @Override
    public ObjectClassDefinition getObjectClassDefinition(String arg0, String arg1) {
        if (this.ocd == null) {
            AttributeDefinition nameAttr = new AnimalNameAttr();
            AttributeDefinition lifespanAttr = metaTypeFactoryService.createAttributeDefinition(getLifespanProps());
            AttributeDefinition eatsAttr = metaTypeFactoryService.createAttributeDefinition(getEatsProperties());
            List<AttributeDefinition> attributeList = new ArrayList<AttributeDefinition>();
            attributeList.add(lifespanAttr);
            attributeList.add(nameAttr);
            attributeList.add(eatsAttr);
            this.ocd = metaTypeFactoryService.createObjectClassDefinition(getOCDProperties(), attributeList, Collections.<AttributeDefinition> emptyList());

        }

        return this.ocd;

    }

    /**
     * @return
     */
    private AttributeDefinitionProperties getEatsProperties() {
        AttributeDefinitionProperties props = new AttributeDefinitionProperties("eats");
        props.setName("eats");
        props.setDescription("What an animal eats");
        props.setType(MetaTypeFactory.PID_TYPE);
        props.setReferencePid(Plant.PLANT_PID);
        props.setCardinality(10);
        return props;
    }

    /**
     * @return
     */
    private AttributeDefinitionProperties getLifespanProps() {
        AttributeDefinitionProperties props = new AttributeDefinitionProperties("lifespan");
        props.setName("Lifespan");
        props.setDescription("The animal's lifespan");
        props.setType(MetaTypeFactory.DURATION_TYPE);
        props.setCardinality(0);

        return props;
    }

    /**
     * @param ocd2
     */
    private ObjectClassDefinitionProperties getOCDProperties() {
        ObjectClassDefinitionProperties properties = new ObjectClassDefinitionProperties("test.metatype.provider.animal");
        properties.setDescription("An animal");
        properties.setName("test.metatype.provider.animal");
        properties.setAlias("animal");

        return properties;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
     */
    @Override
    public void deleted(String arg0) {
        System.out.println("Deleted");

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ManagedServiceFactory#getName()
     */
    @Override
    public String getName() {
        return "Animal MetatypeProvider";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
     */
    @Override
    public void updated(String arg0, Dictionary<String, ?> arg1) throws ConfigurationException {
        System.out.println("Updated");

    }

}
