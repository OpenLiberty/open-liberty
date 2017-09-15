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

import java.io.IOException;
import java.io.InputStream;

import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 *
 */
public class PlantObjectClassDefinition implements ObjectClassDefinition {

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.ObjectClassDefinition#getAttributeDefinitions(int)
     */
    @Override
    public AttributeDefinition[] getAttributeDefinitions(int arg0) {
        AttributeDefinition nameAttr = new PlantNameAttr();
        return new AttributeDefinition[] { nameAttr };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.ObjectClassDefinition#getDescription()
     */
    @Override
    public String getDescription() {
        return "A Plant.";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.ObjectClassDefinition#getID()
     */
    @Override
    public String getID() {
        return "test.metatype.provider.plant";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.ObjectClassDefinition#getIcon(int)
     */
    @Override
    public InputStream getIcon(int arg0) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.metatype.ObjectClassDefinition#getName()
     */
    @Override
    public String getName() {
        return "test.metatype.provider.plant";
    }

}
