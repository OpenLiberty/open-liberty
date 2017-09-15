package com.ibm.ws.sib.msgstore.persistence;
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

import com.ibm.ws.sib.msgstore.Configuration;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.transactions.impl.XidManager;

public class PersistenceFactory 
{
    private PersistenceFactory() {}
    
    /**
     * Factory method for {@link PersistentMessageStore} objects.
     * 
     * @param msi the {@link MessageStoreImpl} object
     * @param xidManager the {@link XidManager} object
     * @param configuration the {@link Configuration} object
     * @return a {@link PersistentMessageStore} object
     * @throws SevereMessageStoreException 
     */
    public static PersistentMessageStore getPersistentMessageStore(MessageStoreImpl msi, XidManager xidManager, Configuration configuration) throws SevereMessageStoreException
    {
        // Override configuration from property if present
        String pmsImplClassName = msi.getProperty(MessageStoreConstants.PROP_PERSISTENT_MESSAGE_STORE_CLASS,
                                                  configuration.getPersistentMessageStoreClassname());

        // No value for the implementation class yet? Use the default
        if (pmsImplClassName == null)
        {
            pmsImplClassName = MessageStoreConstants.PROP_PERSISTENT_MESSAGE_STORE_CLASS_DEFAULT;
        }

        PersistentMessageStore persistentMessageStore = null;
        try
        {
            Class clazz = Class.forName(pmsImplClassName);
            persistentMessageStore = (PersistentMessageStore)clazz.newInstance();
            persistentMessageStore.initialize(msi, xidManager, configuration);
        }
        catch (ClassNotFoundException e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.impl.PersistenceFactory.getPersistentMessageStore", "1:72:1.15.1.1");
            throw new SevereMessageStoreException(e);
        }
        catch (InstantiationException e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.impl.PersistenceFactory.getPersistentMessageStore", "1:77:1.15.1.1");
            throw new SevereMessageStoreException(e);
        }
        catch (IllegalAccessException e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.persistence.impl.PersistenceFactory.getPersistentMessageStore", "1:82:1.15.1.1");
            throw new SevereMessageStoreException(e);
        }
        
        return persistentMessageStore;
    }
}
