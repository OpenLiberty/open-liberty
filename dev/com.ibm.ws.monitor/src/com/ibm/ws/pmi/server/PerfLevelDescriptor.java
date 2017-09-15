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

import com.ibm.websphere.pmi.client.PerfLevelSpec;
import com.ibm.websphere.pmi.PmiConstants;

/**
 * The class is the descriptor for setting/getting instrumentation level of PMI modules.
 * Does not support 6.0 fine-grained control
 * 
 * @ibm-spi
 */
public class PerfLevelDescriptor implements PerfLevelSpec, PmiConstants {
    //PQ81321: using 4.0 serialVersionUID
    private static final long serialVersionUID = -5934872678821227124L; //5.0: 2807497595173060747L;
    final static int MODULE_INDEX = 1;
    final static int SUBMODULE_INDEX = MODULE_INDEX + 2;
    String[] path;
    int level;

    final static String pmiroot = "pmi";

    // from CustomPerfLevelDescriptor
    private String moduleID;

    //private int[] enable = new int[0];
    //private int[] enableSync;

    /**
     * Constructor
     * 
     * @param path Path of the Stats in the PMI tree.
     * @param level instrumentation level for the path
     * @param modID module ID
     */
    public PerfLevelDescriptor(String[] path, int level, String modID) {
        this(path, level);
        this.moduleID = modID;
    }

    /**
     * Constructor
     * 
     * @param path Path of the Stats in the PMI tree.
     * @param level instrumentation level for the path
     */
    public PerfLevelDescriptor(String[] path, int level) {
        // TODO: level need to be changed when enable/disable data has a good
        //       integration with instrumentation level
        if (level == LEVEL_ENABLE)
            level = LEVEL_NONE;
        this.level = level;

        if (path == null) {
            this.path = new String[] { pmiroot };
        } else if (path[0].equals(pmiroot)) {
            this.path = path;
        } else { // path[0] is not equal to pmiroot
            String[] newPath = new String[path.length + 1];
            newPath[0] = pmiroot;
            System.arraycopy(path, 0, newPath, 1, path.length);
            this.path = newPath;
        }

        // convert submodule shortname to module.submodule name
        // change submodule shortname to module.submodule name
        if (this.path.length > SUBMODULE_INDEX) {
            if (this.path[SUBMODULE_INDEX].equals(METHODS_SUBMODULE_SHORTNAME))
                this.path[SUBMODULE_INDEX] = BEAN_METHODS_SUBMODULE;
            else if (this.path[SUBMODULE_INDEX].equals(SERVLETS_SUBMODULE_SHORTNAME))
                this.path[SUBMODULE_INDEX] = SERVLET_SUBMODULE;
        }
    }

    /**
     * Returns the path of the PerfLevelDescriptor.
     * It has preleading root "pmi".
     */
    public String[] getPath() {
        return path;
    }

    /**
     * Returns the path of the PerfLevelDescriptor without the preleading "pmi".
     */
    public String[] getShortPath() {
        if (path == null || path.length == 0)
            return null;
        int index = 0;
        if (path[0].equals(pmiroot)) {
            index = 1;
        }
        if (index == 0)
            return path;

        String[] ret = new String[path.length - index];
        for (int i = 0; i < ret.length; i++)
            ret[i] = path[i + index];

        return ret;
    }

    /**
     * Returns 0 if exactly same
     */
    public int comparePath(PerfLevelSpec otherDesc) {
        return comparePath(otherDesc.getPath());
    }

    /**
     * Returns 0 if exactly same
     */
    public int comparePath(String[] otherPath) {
        if (path == null)
            return -1;
        else if (otherPath == null)
            return 1;

        int minLen = (path.length < otherPath.length) ? path.length : otherPath.length;

        for (int i = 0; i < minLen; i++) {
            int result = path[i].compareTo(otherPath[i]);
            if (result != 0)
                return result;
        }

        return path.length - otherPath.length;
    }

    /**
     * Returns true if it's path is a subpath of otherDesc
     */
    public boolean isSubPath(PerfLevelSpec otherDesc) {
        return isSubPath(otherDesc.getPath());
    }

    /**
     * Returns true if it's path is a subpath of otherPath
     */
    public boolean isSubPath(String[] otherPath) {
        if (path == null || otherPath == null)
            return false;

        if (path.length >= otherPath.length)
            return false;

        for (int i = 0; i < path.length; i++) {
            int result = path[i].compareTo(otherPath[i]);
            if (result != 0)
                return false;
        }

        return true;
    }

    /**
     * Returns the module name in the path
     */
    public String getModuleName() {
        if (moduleID != null) {
            return moduleID;
        }

        if (path != null && path.length > MODULE_INDEX)
            return path[1];
        else
            return null;
    }

    /**
     * Returns the submodule name in the path
     */
    public String getSubmoduleName() {
        if (path != null && path.length > SUBMODULE_INDEX)
            return path[SUBMODULE_INDEX];
        else
            return null;
    }

    /**
     * Returns instrumentation level for the path
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets instrumentation level for the path
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Returns the string representaion of PerfLevelDescriptor for debug.
     */
    public String toString() {
        if (path == null || path.length == 0)
            return "null=" + level;
        else {
            StringBuffer retStr = new StringBuffer();
            for (int i = 0; i < path.length; i++)
                retStr.append(path[i]).append(",");
            retStr.append("=").append(level);
            return retStr.toString();
        }
    }

    // this method is used when reading 5.0 spec by PmiConfigManager
    public String getWCCMType() {
        if (path == null)
            return "";

        String m = getModuleName();
        if (m == null)
            return "";

        // pmi/beanModule/jar-name/category/bean-name/method/method_1
        if (m.equals(PmiConstants.BEAN_MODULE)) {
            if (path.length > 5)
                return m + "#" + PmiConstants.BEAN_METHODS_SUBMODULE;
            else
                return m + "#";
        } else if (m.equals(PmiConstants.RUNTIME_MODULE)) {
            if (path.length > 2)
                return path[1] + "#" + path[2];
            else
                return path[1] + "#";
        }

        // pmi/module/instance/sub-module/sub-instance
        // module_index = 1; sub-module_index = 3
        if (path.length == MODULE_INDEX + 1) {
            return m;
        }

        if (path.length > SUBMODULE_INDEX)
            return m + "#" + getSubmoduleName();
        else
            return m + "#";
    }
}
