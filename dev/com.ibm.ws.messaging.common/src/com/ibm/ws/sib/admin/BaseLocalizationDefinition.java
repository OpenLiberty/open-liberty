/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin;

import com.ibm.ws.sib.utils.SIBUuid8;

public interface BaseLocalizationDefinition extends Cloneable {

    /**
     * Return the UUID of the localization. In the case where the object that
     * implements this interface is created using a WCCM configuration EObject,
     * then the UUID will be as set in the EObject. If a dynamic instance of a
     * LocalizationDefinition is created, then the UUID should be set by the
     * class that creates it.
     * 
     * @return
     */
    public String getUuid();

    public void setUUID(SIBUuid8 uuid);

    /**
     * Return the name of the localization.
     * 
     * @return
     */
    public String getName();

    /**
     * Return the ConfigId to use when instantiating a JMX MBean which represents
     * this localization. An object that implements this interface will derive the
     * configId from the matching WCCM configuration EObject instance. If that object
     * does not have a reference to that instance for any reason, then the configId
     * will be based on the name of the implementing class.
     * 
     * @return String the ConfigId
     */
//  public String getConfigId();

    public long getAlterationTime();

    public void setAlterationTime(long value);

    public Object clone();
}
