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

import org.osgi.service.metatype.AttributeDefinition;

/**
 *
 */
public class PlantNameAttr implements AttributeDefinition {

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.AttributeDefinition#getCardinality()
     */
    @Override
    public int getCardinality() {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.AttributeDefinition#getDefaultValue()
     */
    @Override
    public String[] getDefaultValue() {
        return new String[] { "orchid" };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.AttributeDefinition#getDescription()
     */
    @Override
    public String getDescription() {
        return "The plant name";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.AttributeDefinition#getID()
     */
    @Override
    public String getID() {
        return "name";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.AttributeDefinition#getName()
     */
    @Override
    public String getName() {
        return "Name";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.AttributeDefinition#getOptionLabels()
     */
    @Override
    public String[] getOptionLabels() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.AttributeDefinition#getOptionValues()
     */
    @Override
    public String[] getOptionValues() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.AttributeDefinition#getType()
     */
    @Override
    public int getType() {
        return AttributeDefinition.STRING;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.AttributeDefinition#validate(java.lang.String)
     */
    @Override
    public String validate(String arg0) {
        return "";
    }

}
