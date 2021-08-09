package com.ibm.ws.sib.msgstore;
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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Holds the configuration information needed by the Message Store.
 * 
 * @author kschloss
 * @author pradine
 */
public class WASConfiguration extends Configuration {
    private static TraceComponent tc = SibTr.register(WASConfiguration.class, MessageStoreConstants.MSG_GROUP, MessageStoreConstants.MSG_BUNDLE);
    
    protected String datasourceJndiName = "jdbc/DefaultDataSource";
    protected String authAlias;

    /**
     * Constructor
     *
     */
    protected WASConfiguration() {
        super();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "<ctor>()");
            SibTr.exit(tc, "<ctor>()", this);
        }
    }

    /**
     * Create a new WASConfiguration object
     * 
     * @return a new WASConfiguration object
     */
    public static WASConfiguration getDefaultWasConfiguration() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDefaultWasConfiguration()");
            
        WASConfiguration config = new WASConfiguration();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDefaultWasConfiguration()", config);

        return(config);
    }

    /**
     * Returns the jndi name of the data source that the message store will use.
     * 
     * @return the jndi name of the data source
     */
    public String getDatasourceJndiName() {
        //No trace required
        return datasourceJndiName;
    }

    /**
     * Sets the data source jndi name.
     * 
     * @param datasourceJndiName the data source jndi name
     */
    public void setDatasourceJndiName(String datasourceJndiName) {
        //No trace required
        this.datasourceJndiName = datasourceJndiName;
    }

    /**
     * Sets the J2C authentication alias that will be used to connect to the
     * data source.
     * 
     * @param authAlias the J2C authentication alias
     */
    public void setAuthenticationAlias(String authAlias) {
        //No trace required
        this.authAlias = authAlias; 
    }

    /**
     * Returns the J2C authentication alias.
     * 
     * @return the J2C authentication alias
     */
    public String getAuthenticationAlias() {
        //No trace required
        return authAlias;
    }
    
    public String toString() {
        return super.toString()
               + ", JNDI name: " + datasourceJndiName
               + ", Auth alias: " + authAlias;
    }
}
