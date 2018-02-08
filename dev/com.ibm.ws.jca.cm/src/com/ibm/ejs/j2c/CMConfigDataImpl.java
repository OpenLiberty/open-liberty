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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.ResourceRef;

/**
 * This class holds the ConnectionManager res-xxx configuration properties.
 * Note that there is one instance per ConnectionManager, but it is only used by the
 * relational resource adapter.
 * <p>Use the constants in <code>com.ibm.websphere.csi.ResRef</code> to evaluate returned values from the get methods.
 */

/*
 * Class name : CMConfigDataImpl
 *
 * Scope : Name server and EJB server and WEB server
 *
 * Object model : 1 per ConnectionManager instance
 */
public final class CMConfigDataImpl implements CMConfigData {

    private static final long serialVersionUID = 2034951388889375702L;

    private boolean res_sharing_scope = false; // true means SHAREABLE; false means UNSHAREABLE.
    private int res_isolation_level = 999; // TRANSACTION_xxx
    //  keeping res-resolution_control just to preserve serialization properties
    //   otherwise, it is no longer referenced.
    private int res_resolution_control = 999; // Application | ContainerAtBoundary
    private int res_auth = 999; // Application | Container
    private String cf_name = "undefined";
    private String jndiName;
    private String cfDetailsKey = "undefined";
    private String _CfKey;
    private String loginConfigurationName = null;
    private HashMap<String, String> loginConfigProperties = null;
    private String loginConfigPropsKeyString = null;
    private int _commitPriority = 0;
    private int _branchCoupling = 999; //  Loose|Tight
    private String qmid = null;

    /*
     * Transient fields are not saved when the object is serialized, and do not need to be accounted for
     * in the write/readObject methods.
     */

    /**
     * _ConfigDumpId is only used when displaying config dump data - it does not enter in to
     * the equals method and does not impact and runtime code outside of the configDump.
     */
    private transient String _ConfigDumpId = "";

    private transient String _resRefName = null;
    private transient String _containerAlias = null;

    private static final TraceComponent TC = Tr.register(CMConfigDataImpl.class,
                                                         J2CConstants.traceSpec,
                                                         J2CConstants.messageFile);

    /**
     * List of fields that will be serialized when the writeObject method
     * is called. We do this so that the implementation of this class can
     * change without breaking the serialization process.
     */

    static private final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[] {

                                                                                                new ObjectStreamField("cf_name", String.class),
                                                                                                new ObjectStreamField("cfDetailsKey", String.class),
                                                                                                new ObjectStreamField("_CfKey", String.class),
                                                                                                new ObjectStreamField("_PmiName", String.class),
                                                                                                new ObjectStreamField("res_auth", Integer.TYPE),
                                                                                                new ObjectStreamField("res_isolation_level", Integer.TYPE),
                                                                                                new ObjectStreamField("res_resolution_control", Integer.TYPE),
                                                                                                new ObjectStreamField("res_sharing_scope", Boolean.TYPE),
                                                                                                new ObjectStreamField("loginConfigurationName", String.class),
                                                                                                new ObjectStreamField("loginConfigProperties", HashMap.class),
                                                                                                new ObjectStreamField("loginConfigPropsKeyString", String.class),
                                                                                                new ObjectStreamField("_commitPriority", Integer.TYPE),
                                                                                                new ObjectStreamField("_branchCoupling", Integer.TYPE),
                                                                                                new ObjectStreamField("qmid", String.class)

    };

    protected CMConfigDataImpl(String jndiName,
                               int sharingScope,
                               int isolationLevel,
                               int auth,
                               String cfKey,
                               String loginConfigurationName,
                               HashMap<String, String> loginConfigProperties,
                               String resRefName,
                               int commitPriority,
                               int branchCoupling,
                               Properties mmProps) {

        this.jndiName = jndiName;

        /*
         * ConfidDumpId need only contain the pmiName (which should be the jndiName).
         * This will be consistent for all ConnectionManagers for a given ConnectionPool.
         * In order to differentiate between the different CMs the cfDetailsKey (which may be
         * displayed in the configDump) should be used.
         */
        _ConfigDumpId = jndiName;

        if (cfKey != null) {
            _CfKey = cfKey;
        } else {
            _CfKey = jndiName;
        }

        this._resRefName = resRefName;

        setSharingScope(sharingScope);
        res_isolation_level = isolationLevel;
        res_auth = auth;

        this.loginConfigurationName = loginConfigurationName;
        this.loginConfigProperties = loginConfigProperties;
        if ((loginConfigProperties != null) && (!loginConfigProperties.isEmpty())) {
            loginConfigPropsKeyString = loginConfigProperties.toString();
        }

        if (mmProps != null) {
            this._containerAlias = mmProps.getProperty(J2CConstants.MAPPING_MODULE_authDataAlias);
        }
        this._commitPriority = commitPriority;
        setBranchCoupling(branchCoupling);

        // 128 is a compiler help.  Not a critical value.
        StringBuffer sb = new StringBuffer(128);
        sb.append(Integer.toString(sharingScope));
        sb.append(Integer.toString(res_isolation_level));
        sb.append(Integer.toString(res_auth));
        sb.append(Integer.toString(0)); // CMP
        sb.append(Integer.toString(commitPriority));
        sb.append(Integer.toString(branchCoupling));

        if (loginConfigurationName != null) {
            sb.append(loginConfigurationName);
        }
        if (loginConfigPropsKeyString != null) {
            sb.append(loginConfigPropsKeyString);
        }

        // (resRefName may be null, as determined in CFBI.getCMConfigData)
        if (resRefName != null) {
            sb.append(resRefName);
        }

        cfDetailsKey = _CfKey + sb.toString();

        if (TC.isDebugEnabled()) {
            Tr.debug(this, TC, "cfDetailsKey = " + cfDetailsKey + " for " + jndiName);
        }

    }

    /**
     * The <b>res-sharing-scope</b> resource-ref element specifies whether the component is to use shared
     * connections.
     *
     * @return sharing scope constant
     */
    @Override
    public int getSharingScope() {
        return res_sharing_scope ? ResourceRef.SHARING_SCOPE_SHAREABLE : ResourceRef.SHARING_SCOPE_UNSHAREABLE;
    }

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
    @Override
    public int getIsolationLevel() {
        return res_isolation_level;
    }

    /**
     * The <b>res-auth</b> resource-ref element specifies whether the component code signs on
     * programmatically to the resource manager (<code>APPLICATION</code>), or the container
     * handles sign-on (<code>CONTAINER</code>).
     *
     * @return Either <code>APPLICATION</code> or <code>CONTAINER</code>, as defined
     *         by the constants in <code>com.ibm.websphere.csi.ResRef</code>.
     */
    @Override
    public int getAuth() {
        return res_auth;
    }

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

    @Override
    public String getCFDetailsKey() {
        return cfDetailsKey;
    }

    /** {@inheritDoc} */
    @Override
    public String getJNDIName() {
        return jndiName;
    }

    /**
     * Login Configuration name
     *
     * @return String
     */
    @Override
    public String getLoginConfigurationName() {
        return loginConfigurationName;
    }

    /**
     * Properties associated with the login configuration.
     *
     * @return HashMap
     */
    @Override
    public HashMap<String, String> getLoginConfigProperties() {
        return loginConfigProperties;
    }

    /** {@inheritDoc} */
    @Override
    public List<? extends Property> getLoginPropertyList() {
        List<Property> props = new LinkedList<Property>();
        if (loginConfigProperties == null) {
            return props;
        } else {
            for (Map.Entry<String, String> entry : loginConfigProperties.entrySet())
                props.add(new PropertyImpl(entry.getKey(), entry.getValue()));
            return props;
        }
    }

    /**
     * Returns the container-managed auth alias
     * that may be specified on the res-ref-properties or cmp-bean-properties
     * in j2c.properties
     * Used only by the SIB RA
     */
    @Override
    public String getContainerAlias() {
        return _containerAlias;
    }

    /*
     * Set methods for all fields
     */

    /**
     * Sets the sharing scope.
     *
     * @param i Expected to be one of <code>SHAREABLE</code> or <code>UNSHAREABLE</code>
     *            as defined by the constants in <code>com.ibm.websphere.csi.ResRef</code>
     */
    protected void setSharingScope(int i) {

        if (i == J2CConstants.CONNECTION_SHAREABLE) {
            res_sharing_scope = true;
        } else {

            if (i == J2CConstants.CONNECTION_UNSHAREABLE) {
                res_sharing_scope = false;
            } else {
                Tr.warning(TC, "INVALID_OR_UNEXPECTED_SETTING_J2CA0067", "sharing scope", i, false);
                res_sharing_scope = false; // default to false if unexpected int value
            }

        }

    }

    /**
     * Returns a readable view of the ConnectionManager res-xxx config data
     *
     * @return String representation of this CMConfigDataImpl instance
     */

    @Override
    public String toString() {

        StringBuffer buf = new StringBuffer(256);
        final String nl = CommonFunction.nl;

        String res_sharing_scopeString = "UNSHAREABLE";
        String res_isolation_levelString = "undefined";
        String res_authString = "undefined";

        if (res_sharing_scope == true) {
            res_sharing_scopeString = "SHAREABLE";
        }

        switch (res_isolation_level) {
            case java.sql.Connection.TRANSACTION_NONE:
                res_isolation_levelString = "TRANSACTION_NONE";
                break;

            case java.sql.Connection.TRANSACTION_READ_UNCOMMITTED:
                res_isolation_levelString = "TRANSACTION_READ_UNCOMMITTED";
                break;

            case java.sql.Connection.TRANSACTION_READ_COMMITTED:
                res_isolation_levelString = "TRANSACTION_READ_COMMITTED";
                break;

            case java.sql.Connection.TRANSACTION_REPEATABLE_READ:
                res_isolation_levelString = "TRANSACTION_REPEATABLE_READ";
                break;

            case java.sql.Connection.TRANSACTION_SERIALIZABLE:
                res_isolation_levelString = "TRANSACTION_SERIALIZABLE";
                break;

            default:
                break;
        }

        if (res_auth == J2CConstants.AUTHENTICATION_CONTAINER) {
            res_authString = "CONTAINER";
        } else {
            if (res_auth == J2CConstants.AUTHENTICATION_APPLICATION) {
                res_authString = "APPLICATION";
            }
        }

        // key items

        buf.append(nl);
        buf.append("[Resource-ref CMConfigData key items]" + nl);

        buf.append(nl);

        // put buf.appends on multiple lines.

        buf.append("\tres-sharing-scope:        ");

        int ss = (res_sharing_scope ? J2CConstants.CONNECTION_SHAREABLE : J2CConstants.CONNECTION_UNSHAREABLE);
        buf.append(ss);

        buf.append(" (");
        buf.append(res_sharing_scopeString);
        buf.append(")");
        buf.append(nl);

        buf.append("\tres-isolation-level:      ");
        buf.append(res_isolation_level);
        buf.append(" (");
        buf.append(res_isolation_levelString);
        buf.append(")");
        buf.append(nl);

        buf.append("\tres-auth:                 ");
        buf.append(res_auth);
        buf.append(" (");
        buf.append(res_authString);
        buf.append(")");
        buf.append(nl);

        buf.append("\tcommitPriority            ");
        buf.append(_commitPriority);
        buf.append(nl);

        buf.append("\tbranchCoupling            ");
        buf.append(_branchCoupling);
        buf.append(nl);

        buf.append("\tloginConfigurationName:   ");
        buf.append(loginConfigurationName);
        buf.append(nl);

        buf.append("\tloginConfigProperties:    ");
        buf.append(loginConfigProperties);
        buf.append(nl);

        String resRefNameString = ((_resRefName == null) ? "not set" : _resRefName);
        buf.append("\tResource ref name:        ");
        buf.append(resRefNameString);
        buf.append(nl);

        String qmidString = ((qmid == null) ? "not set" : qmid);
        buf.append("\tQueue manager id:        ");
        buf.append(qmidString);
        buf.append(nl);

        return buf.toString();

    }

    /**
     * @return _CfKey
     */
    @Override
    public String getCfKey() {
        return _CfKey;
    }

    /**
     * @param o A CMConfigDataImpl Object that will be compared.
     *
     * @return true if object o is equal to this CMConfigDataImpl.
     */
    @Override
    public boolean equals(Object o) {
        boolean rVal = false;

        if (o != null) { // obviously this cannot be null.

            if (this == o) {
                rVal = true;
            } else {

                try {

                    CMConfigDataImpl compare = (CMConfigDataImpl) o;

                    /*
                     * If the addresses are not equal and the cast is sucessful we need to check the individual members, otherwise continue on.
                     * Primitives are checked with ==, Objects are checked with == then .equals (if one of them is not null).
                     */

                    //  collapsed all the checks into just this comparison of cfDetailsKey, since that already included all the other constituents
                    if (this.cfDetailsKey == compare.cfDetailsKey || (this.cfDetailsKey != null && this.cfDetailsKey.equals(compare.cfDetailsKey))) {
                        rVal = true;
                    }

                } catch (ClassCastException cce) {
                    rVal = false;
                }

            }

        }

        return rVal;
    }

    /**
     * Generates a hashcode for the CMConfigData object. The hashcode is based on each member's
     * hashcode (or value, for int members).
     *
     * @return the hashcode for this object.
     */
    /* */
    @Override
    public int hashCode() {
        int rVal = 0;
        long tempHC = 0;

        // collapsed hashCode into just that of cfDetailsKey, since it already included the other constituents
        tempHC += cfDetailsKey.hashCode();

        rVal = (Long.valueOf(tempHC / 10)).intValue();

        return rVal;
    }

    /*
     * This method rereates the CMConfigData Object from a stream - all the members will be
     * re-initialized.
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {

        /*
         * Since this is a simple class all we have to do is read each member from the stream.
         * the order has to remain identical to writeObject, so I've just gone alphabetically.
         *
         * If we change the serialversionUID, we may need to modify this method as well.
         */

        if (TC.isEntryEnabled()) {
            Tr.entry(this, TC, "readObject", stream);
        }

        ObjectInputStream.GetField getField = stream.readFields();

        if (TC.isDebugEnabled()) {

            for (int i = 0; i < serialPersistentFields.length; i++) {

                String fieldName = serialPersistentFields[i].getName();

                if (getField.defaulted(fieldName))
                    Tr.debug(this, TC, "Could not de-serialize field " + fieldName + " in class " +
                                       getClass().getName() + "; default value will be used");

            }

        }

        cf_name = (String) getField.get("cf_name", null);
        cfDetailsKey = (String) getField.get("cfDetailsKey", null);
        _CfKey = (String) getField.get("_CfKey", null);
        jndiName = (String) getField.get("_PmiName", null);

        res_auth = getField.get("res_auth", 999);
        res_isolation_level = getField.get("res_isolation_level", 999);
        res_resolution_control = getField.get("res_resolution_control", 999);
        res_sharing_scope = getField.get("res_sharing_scope", false);
        loginConfigurationName = (String) getField.get("loginConfigurationName", null);
        loginConfigProperties = (HashMap<String, String>) getField.get("loginConfigProperties", null);
        loginConfigPropsKeyString = (String) getField.get("loginConfigPropsKeyString", null);
        _commitPriority = getField.get("_commitPriority", 0);
        _branchCoupling = getField.get("_branchCoupling", 999);
        qmid = (String) getField.get("qmid", null);

        if (TC.isEntryEnabled())
            Tr.exit(this, TC, "readObject", new Object[] {
                                                           cf_name,
                                                           cfDetailsKey,
                                                           _CfKey,
                                                           jndiName,
                                                           res_auth,
                                                           res_isolation_level,
                                                           res_resolution_control,
                                                           res_sharing_scope,
                                                           loginConfigurationName,
                                                           loginConfigProperties,
                                                           loginConfigPropsKeyString,
                                                           _containerAlias,
                                                           _commitPriority,
                                                           _branchCoupling
            });
    }

    /*
     * this method writes the CMConfigDataOjbect to a stream
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {

        /*
         * Since this is a simple class all we have to do is write each member to the stream.
         * the order has to remain identical to readObject, so I've just gone alphabetically.
         *
         * If we change the serialversionUID, we may need to modify this method as well.
         */

        if (TC.isEntryEnabled()) {
            Tr.entry(this, TC, "writeObject");
        }

        ObjectOutputStream.PutField putField = stream.putFields();

        putField.put("cf_name", cf_name);
        putField.put("cfDetailsKey", cfDetailsKey);
        putField.put("_CfKey", _CfKey);
        putField.put("_PmiName", jndiName);

        putField.put("res_auth", res_auth);
        putField.put("res_isolation_level", res_isolation_level);
        putField.put("res_resolution_control", res_resolution_control);
        putField.put("res_sharing_scope", res_sharing_scope);
        putField.put("loginConfigurationName", loginConfigurationName);
        putField.put("loginConfigProperties", loginConfigProperties);
        putField.put("loginConfigPropsKeyString", loginConfigPropsKeyString);
        putField.put("_commitPriority", _commitPriority);
        putField.put("_branchCoupling", _branchCoupling);
        putField.put("qmid", qmid);

        stream.writeFields();

        if (TC.isEntryEnabled()) {
            Tr.exit(this, TC, "writeObject");
        }

    }

    /**
     * Generate a config dump containing all the attributes which match the regular expression.
     *
     * @param aLocalId The regular expression which will be used to determine which attributes to display. Ie, .* will enable everything,
     *            current-resourceAdapterDD-transactionSupport enables just the transactionSupport level.
     * @param aRegisteredOnly If true only registered attributes will be displayed.
     */
    @Override
    public LinkedHashMap<String, Object> getConfigDump(String aLocalId, boolean aRegisteredOnly) { // 327843 removed unused parameter

        if (TC.isEntryEnabled()) {
            Tr.entry(this, TC, "getConfigDump");
        }

        LinkedHashMap<String, Object> cp = new LinkedHashMap<String, Object>();

        if (TC.isEntryEnabled()) {
            Tr.exit(this, TC, "getConfigDump");
        }

        return cp;
    };

    @Override
    public String getConfigDumpId() {
        return _ConfigDumpId;
    }

    public void setConfigDumpId(String configDumpId) {
        _ConfigDumpId = configDumpId;
    }

    /**
     * Returns the res-ref name obtained during lookup.
     * For use by the adapter for heterogeneous pooling.
     *
     * @return String
     */
    @Override
    public String getName() {
        return _resRefName;
    }

    /**
     * Returns the transaction commit priority.
     *
     * @return int
     */
    @Override
    public int getCommitPriority() {
        return _commitPriority;
    }

    /**
     * Returns the transaction branch coupling value.
     *
     * @return int
     */
    @Override
    public int getBranchCoupling() {
        return _branchCoupling;
    }

    /**
     * Set the transaction branch coupling value.
     *
     * @return void
     */
    public void setBranchCoupling(int coupling) {
        _branchCoupling = coupling;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        throw new UnsupportedOperationException(); // we don't currently serialize the description
    }

    /** {@inheritDoc} */
    @Override
    public String getType() {
        throw new UnsupportedOperationException(); // we don't currently serialize the type
    }

    /**
     * Login config property - Copied from ResourceRefConfigImpl
     */
    private static class PropertyImpl implements Property {
        private final String name;
        private final String value;

        PropertyImpl(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return '[' + name + '=' + value + ']';
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }
    }

    /**
     * @return the qmid
     */
    public String getQmid() {
        return qmid;
    }

    /**
     * @param qmid the qmid to set
     */
    public void setQmid(String qmid) {
        this.qmid = qmid;
    }
}