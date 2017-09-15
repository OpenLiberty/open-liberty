/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.metadata.context.ejb;

import java.util.List;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBType;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

public class EJBComponentMetaDataWrapper implements EJBComponentMetaData, IdentifiableComponentMetaData {
    private final EJBComponentMetaData ejbComponentMetaData;

    public EJBComponentMetaDataWrapper(EJBComponentMetaData metadata) {
        this.ejbComponentMetaData = metadata;
    }

    /**
     * @see com.ibm.ws.ejbcontainer.EJBComponentMetaData#getBeanClassName()
     */
    @Override
    public String getBeanClassName() {
        return ejbComponentMetaData.getBeanClassName();
    }

    /**
     * @see com.ibm.ws.ejbcontainer.EJBComponentMetaData#getEJBMethodMetaData(com.ibm.ws.ejbcontainer.EJBMethodInterface)
     */
    @Override
    public List<EJBMethodMetaData> getEJBMethodMetaData(EJBMethodInterface type) {
        return ejbComponentMetaData.getEJBMethodMetaData(type);
    }

    /**
     * @see com.ibm.ws.ejbcontainer.EJBComponentMetaData#getEJBType()
     */
    @Override
    public EJBType getEJBType() {
        return ejbComponentMetaData.getEJBType();
    }

    /**
     * @see com.ibm.ws.runtime.metadata.ComponentMetaData#getJ2EEName()
     */
    @Override
    public J2EEName getJ2EEName() {
        return ejbComponentMetaData.getJ2EEName();
    }

    /**
     * @see com.ibm.ws.runtime.metadata.MetaData#getMetaData(com.ibm.ws.runtime.metadata.MetaDataSlot)
     */
    @Override
    public Object getMetaData(MetaDataSlot slot) {
        return ejbComponentMetaData.getMetaData(slot);
    }

    /**
     * @see com.ibm.ws.runtime.metadata.ComponentMetaData#getModuleMetaData()
     */
    @Override
    public ModuleMetaData getModuleMetaData() {
        return ejbComponentMetaData.getModuleMetaData();
    }

    /**
     * @see com.ibm.ws.runtime.metadata.MetaData#getName()
     */
    @Override
    public String getName() {
        return ejbComponentMetaData.getName();
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData#getPersistentIdentifier()
     */
    @Override
    public String getPersistentIdentifier() {
        return "EJB#" + getJ2EEName().toString();
    }

    /**
     * @see com.ibm.ws.ejbcontainer.EJBComponentMetaData#isReentrant()
     */
    @Override
    public boolean isReentrant() {
        return ejbComponentMetaData.isReentrant();
    }

    /**
     * @see com.ibm.ws.runtime.metadata.MetaData#release()
     */
    @Override
    public void release() {
        ejbComponentMetaData.release();
    }

    /**
     * @see com.ibm.ws.runtime.metadata.MetaData#setMetaData(com.ibm.ws.runtime.metadata.MetaDataSlot, java.lang.Object)
     */
    @Override
    public void setMetaData(MetaDataSlot slot, Object metadata) {
        ejbComponentMetaData.setMetaData(slot, metadata);
    }
}
