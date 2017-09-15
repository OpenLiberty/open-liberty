/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.util.HashMap;
import java.util.LinkedHashMap; 

import com.ibm.ws.resource.ResourceRefInfo;

/**
 * This interface provides access to the ConnectionManager res-xxx configuration
 * properties. Note that there is one instance per ConnectionManager, but it is
 * only used by the relational resource adapter.
 * <p>Use the constants in <code>com.ibm.websphere.csi.ResRef</code> to evaluate
 * returned values from the get methods.
 */

/*
 * Interface name : CMConfigData
 * 
 * Scope : Name server and EJB server and WEB server
 * 
 * Object model : 1 per ConnectionManager instance
 */

public interface CMConfigData extends ResourceRefInfo, java.io.Serializable { 

    /*
     * Get methods for all fields
     */

    /**
     * The <b>res-isolation-level</b> resource-ref element specifies for relational resource adapters the
     * isolation level to be used by the backend database.
     * 
     * @return One of the constants defined in <code>com.ibm.websphere.csi.ResRef</code>.<br>
     *         <table border=1>
     *         <TR><TD>TRANSACTION_NONE
     *         <TR><TD>TRANSACTION_READ_UNCOMMITTED
     *         <TR><TD>TRANSACTION_READ_COMMITTED
     *         <TR><TD>TRANSACTION_REPEATABLE_READ
     *         <TR><TD>TRANSACTION_SERIALIZABLE
     *         </table>
     *         <br>
     *         <p>Note that the corresponding <code>java.sql.Connector</code> constants may be used instead, since these should
     *         be the same as those in <code>com.ibm.websphere.csi.ResRef</code>.
     */
    public int getIsolationLevel();

    /**
     * The <b>res-auth</b> resource-ref element specifies whether the component code signs on
     * programmatically to the resource manager (<code>APPLICATION</code>), or the container
     * handles sign-on (<code>CONTAINER</code>).
     * 
     * @return Either <code>APPLICATION</code> or <code>CONTAINER</code>, as defined
     *         by the constants in <code>com.ibm.websphere.csi.ResRef</code>.
     */
    public int getAuth();

    /**
     * The CF details key is used to access a particular ConnectionFactoryDetails object. The key
     * is made up of the xmi:id found in resources.xml, concatenated with the pmiName and concatenated with the res-xxx settings
     * and other flags.
     * 
     * @return Connection factory details key. For example, a Connection Factory with name:
     *         "cells/myhost/nodes/myhost/resources.xml#MyDataSource02100" indicates <br><br>
     *         <table border=1>
     *         <TR><TD><b>Setting</b><TD><b>Sample value</b><TD><b>Enumeration type</b>
     *         <TR><TD>res-sharing-scope<TD>0<TD>ResRef.SHAREABLE
     *         <TR><TD>res-isolation-level<TD>2<TD>ResRef.TRANSACTION_READ_COMMITTED
     *         <TR><TD>res-auth<TD>1<TD>ResRef.APPLICATION
     *         <TR><TD>isCMP<TD>0<TD>false
     *         </table>
     */
    public String getCFDetailsKey();

    /**
     * Login Configuration name
     * 
     * @return String
     */
    public String getLoginConfigurationName(); 

    /**
     * Properties associated with the login configuration.
     * 
     * @return HashMap
     */
    public HashMap<String, String> getLoginConfigProperties(); 

    /**
     * Returns a readable view of the ConnectionManager res-xxx config data
     */

    public String toString();

    /**
     * Returns the CfKey. For most ConnectionFactories the CfKey is the cfDetailsKey
     * without the res_xxx settings (xmi:id+pmiName). For JMS Connection
     * factories (or if an error occurs) the CfKey will be the pmiName (in the case of JMS Session
     * "Factories" the pmiName is the connectionFactory name).
     */
    public String getCfKey(); 

    /**
     * Returns the container-managed auth alias that may be specified
     * on the res-ref-properties or cmp-bean-properties in j2c.properties
     * Used only by the SIB RA
     * 
     * @return String
     */
    public String getContainerAlias(); 

    public LinkedHashMap<String, Object> getConfigDump(String aLocalId, boolean aRegisteredOnly); 

    public String getConfigDumpId(); 

    /**
     * Returns the transaction commit priority.
     * 
     * @return int
     */
    public int getCommitPriority(); 

    /**
     * Returns the transaction branch coupling.
     * 
     * @return int
     */
    public int getBranchCoupling(); 

}