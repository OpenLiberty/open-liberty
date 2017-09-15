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

package com.ibm.ws.pmi.server;

import com.ibm.websphere.pmi.*;

/**
 * The class to implement DataDescriptor interface. It is used for data retrieval
 * through PmiRegistry.
 * 
 * Possible formats for a data descriptor:
 * module, [dataId, dataId, ...]
 * module, collecction, ..., [dataId, dataId, ...]
 * 
 * Note that it may include one dataId or multiple dataIds under same dataPath.
 * Multiple dataIds under same dataPath could save communication overhead (all data
 * will share same base info).
 * 
 * @ibm-spi
 */

public class DataDescriptor implements java.io.Serializable, PmiConstants {
    private static final long serialVersionUID = -5571274779240562143L;

    String[] dataPath = null;
    int[] dataIds = null; // it may include null, one or multiple dataIds
    int type = TYPE_INVALID;
    final int SUBMODULE_INDEX = 2;

    // constructors:
    public DataDescriptor() {
        // default constructor required to avoid ORB marshalling exception
    }

    /**
     * Constructor
     * 
     * @param path Path of the Stats in the PMI tree. A null indicates the root of PMI tree (server).
     */
    public DataDescriptor(String[] path) {
        init(path, ALL_DATA);
    }

    /**
     * Constructor
     * 
     * @param path Path of the Stats in the PMI tree. A null indicates the root of PMI tree (server).
     * @param dataID ID of the statistic in the stats module.
     */
    public DataDescriptor(String[] path, int dataId) {
        init(path, dataId);
    }

    /**
     * Constructor
     * 
     * @param path Path of the Stats in the PMI tree. A null indicates the root of PMI tree (server).
     * @param dataID Array of statistic IDs.
     */
    public DataDescriptor(String[] path, int[] dataIds) {
        init(path, dataIds);
    }

    /**
     * Constructor
     * 
     * @param parent DataDescriptor
     * @param name of the PMI module
     */
    public DataDescriptor(DataDescriptor parent, String name) {
        if ((parent == null) || (parent.getType() == TYPE_INVALID) ||
            (parent.getType() == TYPE_DATA)) {
            type = TYPE_INVALID;
        } else {
            String[] path = parent.getPath();

            // server dd
            if (path.length == 1 && path[0].equals(PmiConstants.APPSERVER_MODULE)) {
                init(new String[] { name }, ALL_DATA);
            } else {
                String[] myPath = new String[path.length + 1];
                System.arraycopy(path, 0, myPath, 0, path.length);
                myPath[myPath.length - 1] = name;
                init(myPath, ALL_DATA);
            }
        }
    }

    /**
     * Constructor
     * 
     * @param parent DataDescriptor
     * @param ID of the statistic
     */
    public DataDescriptor(DataDescriptor parent, int dataId) {
        if ((parent == null) || (parent.getType() == TYPE_INVALID) ||
            (parent.getType() == TYPE_DATA)) {
            type = TYPE_INVALID;
        } else {
            init(parent.getPath(), dataId);
        }
    }

    /**
     * Constructor
     * 
     * @param parent DataDescriptor
     * @param array of statistic IDs
     */
    public DataDescriptor(DataDescriptor parent, int[] dataIds) {
        if ((parent == null) || (parent.getType() == TYPE_INVALID) ||
            (parent.getType() == TYPE_DATA)) {
            type = TYPE_INVALID;
        } else {
            init(parent.getPath(), dataIds);
        }
    }

    private void init(String[] path, int dataId) {
        if (dataId == ALL_DATA)
            init(path, null);
        else
            init(path, new int[] { dataId });
    }

    private void init(String[] path, int[] dataIds) {
        this.dataPath = path;
        this.dataIds = dataIds;

        // change submodule shortname to module.submodule name
        if (dataPath != null && dataPath.length > SUBMODULE_INDEX) {
            if (dataPath[SUBMODULE_INDEX].equals(METHODS_SUBMODULE_SHORTNAME))
                dataPath[SUBMODULE_INDEX] = BEAN_METHODS_SUBMODULE;
            else if (dataPath[SUBMODULE_INDEX].equals(SERVLETS_SUBMODULE_SHORTNAME))
                dataPath[SUBMODULE_INDEX] = SERVLET_SUBMODULE;
        }

        // set the type: module, instance, and data
        if (dataPath == null || dataPath.length == 0) {
            type = TYPE_INVALID;
            if (dataPath == null)
                dataPath = new String[0];
        } else {
            for (int i = 0; i < dataPath.length; i++) {
                if (dataPath[i] == null) { // dataPath name must not be null
                    type = TYPE_INVALID;
                    return;
                }
            }

            // set the type now
            // The first level is always module, and then we have TYPE_COLLECTION
            if (dataIds != null) {
                type = TYPE_DATA;
            } else {
                if (dataPath.length == 1 && dataPath[0].equals(PmiConstants.APPSERVER_MODULE))
                    type = TYPE_SERVER;
                if (dataPath.length == 1)
                    type = TYPE_MODULE;
                else
                    type = TYPE_COLLECTION;
            }
        }
    }

    /**
     * 
     * Returns Stats path represented by this DataDescriptor
     */
    public String[] getPath() {
        return dataPath;
    }

    /**
     * Returns the type of this data i.e. whether module, instance or statistic.
     */
    public int getType() { // module, instance, and data
        return type;
    }

    // This method should be rewritten if we allow arbitrary module structure
    public int getType(int pathLength) {
        if (type == TYPE_INVALID || dataPath == null || pathLength >= dataPath.length)
            return TYPE_INVALID;

        if (pathLength == 1)
            return TYPE_MODULE;
        else
            return TYPE_COLLECTION;
    }

    /**
     * Returns the PMI module name of this DataDescriptor.
     */
    public String getModuleName() {
        if (type == TYPE_INVALID)
            return null;
        return dataPath[0];
    }

    /**
     * Returns the instance name.
     */
    public String getName() {
        if (type == TYPE_INVALID)
            return null;
        return dataPath[dataPath.length - 1];
    }

    /**
     * Returns the ID of the statistics.
     * This method is only meaningful for null or single data.
     */
    public int getDataId() {
        if (dataIds == null)
            return ALL_DATA;
        else
            return dataIds[0];
    }

    /**
     * Returns the array of statistic IDs
     */
    public int[] getDataIds() {
        return dataIds;
    }

    /**
     * Returns true if this descriptor has same path
     * same type, same moduleName, instanceName, and dataId as the other DataDescriptor.
     */
    public boolean isSamePath(DataDescriptor other) {
        if (other == null)
            return false;
        if (type == TYPE_INVALID || other.getType() == TYPE_INVALID)
            return false;
        if (type != other.getType())
            return false;
        if (dataPath == null || other.getPath() == null)
            return false;

        String[] otherPath = other.getPath();
        if (dataPath.length != otherPath.length)
            return false;
        for (int i = 0; i < dataPath.length; i++) {
            if (!dataPath[i].equals(otherPath[i]))
                return false;
        }
        return true;
    }

    /**
     * Returns true if this descriptor is descendant of other descriptor
     */
    public boolean isDescendant(DataDescriptor other) {
        if (other == null)
            return false;

        if (type == TYPE_INVALID || other.getType() == TYPE_INVALID)
            return false;

        String[] otherPath = other.getPath();
        if (otherPath.length >= dataPath.length)
            return false;

        for (int i = 0; i < otherPath.length; i++) {
            if (!dataPath[i].equals(otherPath[i]))
                return false;
        }
        return true;
    }

    /**
     * Returns the parentDescriptor
     */
    public DataDescriptor parentDescriptor() {
        if (type == TYPE_INVALID) {
            return null;
        } else if (dataIds != null) {
            return new DataDescriptor(dataPath);
        } else {
            if (dataPath.length == 1)
                return null;
            String[] myPath = new String[dataPath.length - 1];
            System.arraycopy(dataPath, 0, myPath, 0, myPath.length);
            return new DataDescriptor(myPath);
        }
    }

    /**
     * Returns the string representation of this datadescriptor for debug.
     */
    public String toString() {
        StringBuffer ret = new StringBuffer();
        for (int i = 0; i < dataPath.length; i++)
            ret.append(dataPath[i]).append("/");
        if (dataIds != null)
            ret.append(dataIds[0]);
        return ret.toString();
    }
}
