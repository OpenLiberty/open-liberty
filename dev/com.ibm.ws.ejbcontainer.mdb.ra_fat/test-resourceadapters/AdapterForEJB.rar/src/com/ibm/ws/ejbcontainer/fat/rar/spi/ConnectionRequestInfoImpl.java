/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.fat.rar.spi;

import java.util.Map;
import java.util.logging.Logger;

import javax.resource.spi.ConnectionRequestInfo;

import com.ibm.ws.ejbcontainer.fat.rar.core.AdapterUtil;
import com.ibm.ws.rsadapter.FFDCLogger;

/**
 * Implementation class for ConnectionRequestInfo.<p>
 */
public class ConnectionRequestInfoImpl implements ConnectionRequestInfo {
    private final static String CLASSNAME = ConnectionRequestInfoImpl.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private static final int NUM_CRI_PROPERTIES = 6;

    String ivUserName;
    String ivPassword;
    int ivIsoLevel;
    String ivCatalog;
    Boolean ivReadOnly;
    Map ivTypeMap;

    // Cache this hashcode as much as possible for performance
    private final int hashcode;

    /** Indicates if this CRI has only the isolation level property specified. */
    private boolean hasIsolationLevelOnly;

    /**
     * Create a ConnectionRequestInfo with no properties specified.
     */
    ConnectionRequestInfoImpl() {
        hashcode = ivIsoLevel = java.sql.Connection.TRANSACTION_READ_COMMITTED;
    }

    /**
     * Creates a ConnectionRequestInfo with isolation level as the only property provided.
     * All other properties remain null, allowing the .equals method to be optimized when two
     * such CRIs are compared. [d139351.15]
     *
     * @param isolationLevel the transaction isolation level.
     */
    public ConnectionRequestInfoImpl(int isolationLevel) {
        hashcode = ivIsoLevel = isolationLevel;
        hasIsolationLevelOnly = true;
        svLogger.info("ConnectionRequestInfo created: " + this);
    }

    /**
     * Creates a ConnectionRequestInfo object for the parameters provided. When this
     * constructor is used, a JDBC connection handle is always requested.
     *
     * @param user the user name, or null if none.
     * @param password the password, or null if none.
     * @param isolationLevel the transaction isolation level.
     */
    public ConnectionRequestInfoImpl(String user, String password, int isolationLevel) {
        ivUserName = user;
        ivPassword = password;
        ivIsoLevel = isolationLevel;

        // In order to obtain a statistically random distribution, each hash code that
        // comprises this hashcode is divided by the number of related items.  d121047

        hashcode = ivIsoLevel + (ivUserName == null ? 0 : ivUserName.hashCode() / NUM_CRI_PROPERTIES)
                   + (ivPassword == null ? 0 : ivPassword.hashCode() / NUM_CRI_PROPERTIES);

        svLogger.info("ConnectionRequestInfo created: " + this);
    }

    // d129064.1 - Remove the setter methods; everything must be set in the constructor.
    /**
     * Creates a ConnectionRequestInfo object for the parameters provided.
     *
     * @param user the user name, or null if none.
     * @param password the password, or null if none.
     * @param isolationLevel the transaction isolation level.
     * @param catalog the catalog name.
     * @param isReadOnly indicator of whether the connection is read only.
     * @param typeMap a type mapping for custom SQL structured types and distinct types.
     */
    public ConnectionRequestInfoImpl(String user, String password, int isolationLevel, String catalog, Boolean isReadOnly, Map typeMap) {
        ivUserName = user;
        ivPassword = password;
        ivIsoLevel = isolationLevel;
        ivCatalog = catalog;
        ivReadOnly = isReadOnly;
        ivTypeMap = typeMap;

        // In order to obtain a statistically random distribution, each hash code that
        // comprises this hashcode is divided by the number of related items.  d121047

        hashcode = ivIsoLevel + (ivUserName == null ? 0 : ivUserName.hashCode() / NUM_CRI_PROPERTIES)
                   + (ivPassword == null ? 0 : ivPassword.hashCode() / NUM_CRI_PROPERTIES)
                   + (ivCatalog == null ? 0 : ivCatalog.hashCode() / NUM_CRI_PROPERTIES)
                   + (ivReadOnly == null ? 0 : ivReadOnly.hashCode() / NUM_CRI_PROPERTIES)
                   + (ivTypeMap == null ? 0 : ivTypeMap.hashCode() / NUM_CRI_PROPERTIES);

        svLogger.info("ConnectionRequestInfo created: " + this);
    }

    /**
     * Return a user name
     *
     * @return a user name
     */
    public final String getUserName() {
        return ivUserName;
    }

    /**
     * Return a password
     *
     * @return a user password
     */
    public final String getPassword() {
        return ivPassword;
    }

    /**
     * Return an Isolation Level that is used for a connection. The return isolation level
     * has one of the following values:
     * <ul>
     * <li>READ_UNCOMMITTED; </li>
     * <li>READ_COMMITTED; </li>
     * <li>REPEATABLE_READ; </li>
     * <li>SERIALABLE</li>
     * </ul>
     *
     * @return an Isolation Level
     */
    public final int getIsolationLevel() {
        return ivIsoLevel;
    }

    /**
     * @return the catalog name.
     */
    public final String getCatalog() {
        return ivCatalog;
    }

    /**
     * @return the type map used for the custom mapping of SQL structured types and distinct
     *         types.
     */
    public final Map getTypeMap() {
        return ivTypeMap;
    }

    /**
     * @return relevant FFDC information for this class, formatted as a String array.
     */
    public String[] introspectSelf() {
        FFDCLogger info = new FFDCLogger(this);

        info.append("User Name:", ivUserName);
        info.append("Password:", ivPassword == null ? null : "******");
        info.append("Isolation Level:", AdapterUtil.getIsolationLevelString(ivIsoLevel));
        info.append("Catalog:", ivCatalog);
        info.append("Is Read Only?", ivReadOnly);
        info.append("Type Map:", ivTypeMap);
        info.append("Hash Code:", Integer.toHexString(hashcode));
        return info.toStringArray();
    }

    /**
     * @return true if the connection is read only; otherwise false. Value will be null if the
     *         readOnly property was never set, which means the database default will be used.
     */
    public final Boolean isReadOnly() {
        return ivReadOnly;
    }

    /**
     * Checks whether this instance is equal to another.
     * <p>Overrides: equals in class java.lang.Object
     *
     * <P><B>Note</b> We do not check the connection type as part of the equals
     * don't want to do this because we want to make sure that both BMP and CMP can
     * match to the same managed connection as per the WAB's request.
     *
     * <P>We cannot just compare the hashcode because it is not guaranteed to be
     * unique.
     *
     * @param arg0 WSCRI object to compare
     * @return boolean
     */
    @Override
    public final boolean equals(Object arg0) {
        boolean result;

        if (arg0 == this)
            result = true;
        else
            try {
                ConnectionRequestInfoImpl cri = (ConnectionRequestInfoImpl) arg0;
                result = ivIsoLevel == cri.ivIsoLevel &&
                         ((hasIsolationLevelOnly && cri.hasIsolationLevelOnly) ||
                          (hashcode == cri.hashcode && match(ivUserName, cri.ivUserName) && match(ivPassword, cri.ivPassword) &&
                           match(ivCatalog, cri.ivCatalog) && match(ivReadOnly, cri.ivReadOnly) && match(ivTypeMap, cri.ivTypeMap)));

            } catch (RuntimeException runtimeX) {
                result = false;
            }

        svLogger.exiting(CLASSNAME, "equals?", new Object[] {
                                                              AdapterUtil.toString(this),
                                                              AdapterUtil.toString(arg0),
                                                              result ? Boolean.TRUE : Boolean.FALSE });

        return result;
    }

    /**
     * Determine if two objects, either of which may be null, are equal.
     *
     * @param obj1 one object.
     * @param obj2 another object.
     *
     * @return true if the objects are equal or are both null, otherwise false.
     */
    private static final boolean match(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1 != null && obj1.equals(obj2));
    }

    /**
     * write out the contents of this object
     *
     * @return a String containing the contents of this object
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(300).append(AdapterUtil.toString(this)).append("\n  UserName     = ").append(ivUserName).append("\n  Password     = ").append(ivPassword == null ? "null" : "******").append("\n  Catalog      = ").append(ivCatalog).append("\n  IsReadOnly   = ").append(ivReadOnly).append("\n  TypeMap      = ").append(ivTypeMap).append("\n  Isolation    = ").append(AdapterUtil.getIsolationLevelString(ivIsoLevel)).append("\n  Handle type  = ");

        return new String(sb);
    }

    /**
     * Returns the hashCode of the ConnectionRequestInfo.
     * If two objects are equal according to the equals(Object) method,
     * then calling the hashCode method on each of the two objects must
     * produce the same integer result.
     *
     * <p>We cannot just return super.hashCode() because CM is using this
     * method to uniquely identify connection handles that are stored into
     * a hash table. So the hashCode() is being used in the sense of getPrimaryKey()
     *
     * <p>Overrides: hashCode in class java.lang.Object
     *
     * @return a hash code of this instance
     * @see http://java.sun.com/products/jdk/1.2/docs/api/java/lang/Object.html
     */
    @Override
    public final int hashCode() {
        return hashcode;
    }
}