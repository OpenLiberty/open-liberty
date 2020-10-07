/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.annotation;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

public class DomainResolverObjectFactory /*TODO Liberty implements ObjectFactory*/{

    /**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(DomainResolverObjectFactory.class);	
	
	
    public static void registerSelf()  {
        /*TODO Liberty 
    	try {
            InjectionEngineAccessor.getInstance()
            		.registerObjectFactory(
            			Resource.class, 
            			DomainResolver.class, 
            			DomainResolverObjectFactory.class, 
            			false);
            
    	} catch (InjectionException e){
    		if (c_logger.isErrorEnabled()){
    			c_logger.error("Error registering DomainResolver object factory", null, null, e);
    		}    		
    	}*/
    }
	
	/*
	public Object getObjectInstance(Object obj, Name name, Context nameCtx,
			Hashtable<?, ?> environment) throws Exception {
		return DomainResolverImpl.getInstance();
	}
*/
}
