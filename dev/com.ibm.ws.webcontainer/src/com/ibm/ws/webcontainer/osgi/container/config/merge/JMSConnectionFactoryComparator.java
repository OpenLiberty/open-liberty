/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.container.config.merge;

import com.ibm.ws.javaee.dd.common.JMSConnectionFactory;

public class JMSConnectionFactoryComparator extends AbstractBaseComparator<JMSConnectionFactory> {
    
    @Override
    public boolean compare(JMSConnectionFactory o1, JMSConnectionFactory o2) {
        // Name
        if (o1.getName() == null) {
            if (o2.getName() != null)
                return false;
        } else if (!o1.getName().equals(o2.getName())) {
            return false;
        }
        
        // Interface Name
        if (o1.getInterfaceNameValue() == null) {
            if (o2.getInterfaceNameValue() != null)
                return false;
        } else if (!o1.getInterfaceNameValue().equals(o2.getInterfaceNameValue())) {
            return false;
        }
        
        // Class Name
        if (o1.getClassNameValue() == null) {
            if (o2.getClassNameValue() != null)
                return false;
        } else if (!o1.getClassNameValue().equals(o2.getClassNameValue())) {
            return false;
        }
        
        //Resource Adapter
        if (o1.getResourceAdapter() == null) {
            if (o2.getResourceAdapter() != null)
                return false;
        } else if (!o1.getResourceAdapter().equals(o2.getResourceAdapter())) {
            return false;
        }
        
        //User
        if (o1.getUser() == null) {
            if (o2.getUser() != null)
                return false;
        } else if (!o1.getUser().equals(o2.getUser())) {
            return false;
        }
        
        //Password
        if (o1.getPassword() == null) {
            if (o2.getPassword() != null)
                return false;
        } else if (!o1.getPassword().equals(o2.getPassword())) {
            return false;
        }
        
        //Client Id
        if (o1.getClientId() == null) {
            if (o2.getClientId() != null)
                return false;
        } else if (!o1.getClientId().equals(o2.getClientId())) {
            return false;
        }
        
        //Is Transactional?
        if (o1.isTransactional() !=o2.isTransactional()) {
            return false;
        }


        //Max Pool Size
        if (o1.getMaxPoolSize()!=o2.getMaxPoolSize()) {
            return false;
        }
        
        //Min Pool Size
        if (o1.getMinPoolSize()!=o2.getMinPoolSize()) {
            return false;
        }

        //Properties
        if (!compareProperties(o1.getProperties(), o2.getProperties())) {
            return false;
        }
        
        //Description
        if (!compareDescriptions(o1.getDescriptions(), o2.getDescriptions())) {
            return false;
        }

        return true;
    }

    
    
}
