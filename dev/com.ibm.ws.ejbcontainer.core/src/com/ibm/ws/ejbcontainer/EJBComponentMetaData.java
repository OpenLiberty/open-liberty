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
package com.ibm.ws.ejbcontainer;

import java.util.List;

import com.ibm.ws.runtime.metadata.ComponentMetaData;

public interface EJBComponentMetaData
                extends ComponentMetaData
{
    /**
     * @return the EJB type
     */
    EJBType getEJBType();

    /**
     * @return the EJB implementation class name
     */
    String getBeanClassName();

    /**
     * @return true if this is a reentrant entity bean
     */
    boolean isReentrant();

    /**
     * @param type the interface type
     * @return the list of methods for the interface type, or null if the EJB has
     *         no interfaces of that type
     */
    List<EJBMethodMetaData> getEJBMethodMetaData(EJBMethodInterface type);
}
