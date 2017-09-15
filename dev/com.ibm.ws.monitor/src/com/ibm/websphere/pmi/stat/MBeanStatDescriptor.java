/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.pmi.stat;

import javax.management.ObjectName;

/**
 * The <code> MBeanStatDescriptor </code> includes an MBean ObjectName and an optional StatDescriptor.
 * PMI uses MBeanStatDescriptor to map the PMI modules and submodules so that the client can
 * request a subset of PMI data available in an MBean.
 * 
 * <p>
 * When requesting PMI data, the server has to know the MBean ObjectName that provides the data.
 * For most PMI modules and submodules, there are MBeans directly mapping to them.
 * However, for some PMI modules and submodules, there are no mapping MBeans.
 * In this case, PMI uses the MBeanStatDescriptor to map the PMI modules and submodules so that
 * the client can request a subset of PMI data available in an MBean.
 * 
 * @ibm-api
 */
public class MBeanStatDescriptor implements java.io.Serializable {
    public static final long serialVersionUID = 8434304601722208723L;

    ObjectName mName = null;
    StatDescriptor sd = null;

    // The identifier instance is no longer used
    // private static String identifier = null;

    /**
     * Constructor
     * 
     * @param mName should be a valid ObjectName (not null).
     */
    public MBeanStatDescriptor(ObjectName mName) {
        this(mName, null);
    }

    /**
     * Constructor
     * 
     * @param mName should be a valid ObjectName (not null).
     * @param sd could be null.
     */
    public MBeanStatDescriptor(ObjectName mName, StatDescriptor sd) {
        this.mName = mName;
        this.sd = sd;

        // The following method call is no longer used
        //        setMBeanIdentifier();
    }

    // No longer caching the identifier instance. Retrieve this data dynamically.
    /*
     * private void setMBeanIdentifier() {
     * if(mName == null)
     * return;
     * if(sd == null)
     * identifier = mName.getKeyProperty("name")+"///"+mName.getKeyProperty("mbeanIdentifier");
     * else
     * identifier = mName.getKeyProperty("name")+"///"+mName.getKeyProperty("mbeanIdentifier") + "///" + sd.toString();
     * 
     * }
     */

    /**
     * Returns the ObjectName of this MBeanStatDescriptor.
     */
    public ObjectName getObjectName() {
        return mName;
    }

    /**
     * Returns the StatDescriptor of this MBeanStatDescriptor.
     */
    public StatDescriptor getStatDescriptor() {
        return sd;
    }

    /**
     * @return true if this MBeanStatDescriptor is same as msd
     */
    public boolean isSame(MBeanStatDescriptor msd) {
        // TODO: how to know two ObjectName are same or not?
        if (msd == null || msd.getObjectName() == null || mName == null)
            return false;

        if (!msd.getObjectName().toString().equals(mName.toString()))
            return false;

        if (sd == null && msd.getStatDescriptor() == null)
            return true;
        else if (sd == null)
            return false;
        else
            return sd.isSame(msd.getStatDescriptor());
    }

    /**
     * @return a unique identifier for the MBeanStatDescriptor
     */
    public String getIdentifier() {

        // The following code was taken from the code initially used to cache the identifier string

        if (mName == null)
            return null;
        if (sd == null)
            return (mName.getKeyProperty("name") + "///" + mName.getKeyProperty("mbeanIdentifier"));
        else
            return (mName.getKeyProperty("name") + "///" + mName.getKeyProperty("mbeanIdentifier") + "///" + sd.toString());

        //    	return identifier;
        /*
         * if(mName == null)
         * return null;
         * if(sd == null)
         * return mName.getKeyProperty("mbeanIdentifier");
         * else
         * return mName.getKeyProperty("mbeanIdentifier") + "///" + sd.toString();
         */

    }

    /**
     * @return the name of its StatDescriptor if it is not null or the name of the MBean ObjectName if StatDescriptor is null
     */
    public String getName() {
        if (mName == null)
            return null;

        if (sd == null)
            return mName.getKeyProperty("name");
        else
            return sd.getName();
    }

    /**
     * Returns a String representation of MBeanStatDescriptor for debug.
     */
    public String toString() {
        if (mName == null) {
            if (sd == null)
                return null;
            else
                return "///" + sd.toString();
        }

        if (sd == null)
            return mName.toString();
        else
            return mName.toString() + "///" + sd.toString();
    }
}
