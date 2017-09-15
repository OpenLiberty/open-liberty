/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.pmi.server;

import java.util.ArrayList;
import java.util.StringTokenizer;

import com.ibm.websphere.pmi.PerfModules;
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

// TODO: NEED TO OPTIMIZE METHODS IN THIS CLASS. THERE IS SOME REDUNDANCY WHILE COMPRESSING
public class PmiUtil implements PmiConstants {
    private static final TraceComponent tc = Tr.register(PmiUtil.class);

    public final static String mySeperator = ">";
    public final static char mySeperatorChar = '>';

    // If pmiString is too long, it will prevent the appserver from starting.
    // So compress the parent and children if their levels are same 
    // when pmiStringMaxLength is exceeded.
    // Set 0 means that we always compress the level spec string. It will only
    // have module level info.
    public final static int pmiStringMaxLength = 0; // tested length 4050 on NT is okay

    // compress module names and levels to reduce the length of PmiAttributes

    // use single letter for level to compress the PerfLevelSpec string
    public static String SHORT_LEVEL_NONE_STRING = "N"; // none
    public static String SHORT_LEVEL_LOW_STRING = "L"; // low
    public static String SHORT_LEVEL_MEDIUM_STRING = "M"; // medium
    public static String SHORT_LEVEL_HIGH_STRING = "H"; // high
    public static String SHORT_LEVEL_MAX_STRING = "X"; // maximum
    public static String SHORT_LEVEL_FG_STRING = "F"; // fine-grained

    // compressed name of module names
    static String[] compressModuleNames = new String[PerfModules.moduleIDs.length];

    // static block for initializaiton
    static {
        for (int i = 0; i < PerfModules.moduleIDs.length; i++) {
            compressModuleNames[i] = PerfModules.moduleIDs[i].substring(0, 4);
        }
    }

    /**
     * Return the current time in microseconds.
     */
    public static long currentTime() {
        return System.currentTimeMillis() * JAVA_TIME_CONVERT_RATIO;
    }

    /**
     * convert PerfLevelSpec to string
     */
    public static String getStringFromPerfLevelSpecs(PerfLevelDescriptor[] plds) {
        return getStringFromPerfLevelSpecs(plds, true);
    }

    public static String getStringFromPerfLevelSpecs(PerfLevelDescriptor[] plds, boolean compressIfNeeded) {
        // create a string containing the plds
        StringBuffer res = null;
        if (plds == null)
            return null;

        for (int i = 0; i < plds.length; i++) {
            String morePld = createPmiSpecString(plds[i]);
            if (res == null) {
                res = new StringBuffer();
                res.append(morePld);
            } else if (morePld != null) {
                res.append(":").append(morePld);
            }
        }
        if (compressIfNeeded)
            return getCompressedPerfString(res.toString());
        else
            return res.toString();
    }

    public static PerfLevelDescriptor[] getPerfLevelSpecsFromString(String specString) {
        return getPerfLevelSpecsFromString(specString, true);
    }

    public static PerfLevelDescriptor[] getPerfLevelSpecsFromString(String specString, boolean expandIfNeeded) {
        if (specString == null || specString.equals("") || specString.equals(PMI_DISABLE_STRING))
            return null;
        else {
            ArrayList res = new ArrayList();
            // create an array of PerfLevelDescriptor from string
            //tokens splits by ':'
            // beanModule=H:threadPool=L:
            StringTokenizer st = new StringTokenizer(specString, ":");
            while (st.hasMoreTokens()) {
                PerfLevelDescriptor pld = getSpecFromString(st.nextToken());
                if (pld != null) {
                    res.add(pld);

                    if (expandIfNeeded) {
                        String mName = pld.getModuleName();
                        if (mName != null && mName.equals(PmiConstants.RUNTIME_MODULE) && pld.getPath().length == 2) {
                            // add JVMPI modules
                            String lvl = null; //PmiConfigManager.getLevelString (pld.getLevel());
                            if (!lvl.equals("X"))
                                lvl = "N";
                            res.add(getSpecFromString("jvmRuntimeModule>GC=" + lvl));
                            res.add(getSpecFromString("jvmRuntimeModule>Object=" + lvl));
                            res.add(getSpecFromString("jvmRuntimeModule>Thread=" + lvl));
                            res.add(getSpecFromString("jvmRuntimeModule>Monitor=" + lvl));
                        }
                    }
                }
            }

            if (expandIfNeeded) { // add missing parent plds in between so that caller can easily
                // indentify the level for missing plds.
                PerfLevelDescriptor pld1 = null;
                PerfLevelDescriptor pld2 = null;
                int numPlds = res.size();
                PerfLevelDescriptor parent = null;
                for (int i = 0; i < numPlds; i++) {
                    parent = null;
                    pld1 = (PerfLevelDescriptor) res.get(i);
                    if (pld1 == null || pld1.getPath().length <= 2)
                        continue;
                    else {
                        for (int j = numPlds - 1; j >= 0; j--) { // find its parent
                            if (i == j)
                                continue;
                            pld2 = (PerfLevelDescriptor) res.get(j);
                            if (pld2.isSubPath(pld1)) { // parent
                                if (pld2.getPath().length == pld1.getPath().length - 1) { // immediate parent
                                    parent = null;
                                    break;
                                } else if (parent == null || pld2.getPath().length > parent.getPath().length)
                                    parent = pld2;
                            }
                        }
                        // add parent if needed
                        if (parent != null) {
                            for (int j = parent.getPath().length + 1; j < pld1.getPath().length; j++) {
                                String[] path = new String[j];
                                for (int k = 0; k < j; k++)
                                    path[k] = pld1.getPath()[k];
                                res.add(new PerfLevelDescriptor(path, parent.getLevel()));
                            }
                        }
                    }
                }
            }

            // convert to the returned array
            PerfLevelDescriptor[] resArray = new PerfLevelDescriptor[res.size()];
            for (int i = 0; i < resArray.length; i++) {
                resArray[i] = (PerfLevelDescriptor) res.get(i);
                String[] path = resArray[i].getPath();
            }
            return resArray;
        }
    }

    /*
     * public static String updatePerfLevelSpecString(PmiJmxMapper mapper,
     * String oldSpecString,
     * MBeanLevelSpec[] newSpecs,
     * boolean recursive)
     * {
     * if(newSpecs == null || newSpecs.length == 0)
     * return oldSpecString;
     * 
     * PerfLevelDescriptor[] newPlds = new PerfLevelDescriptor[newSpecs.length];
     * for(int i=0; i<newPlds.length; i++){}
     * //newPlds[i] = mapper.getPerfLevelDescriptor(newSpecs[i]);
     * return updatePerfLevelSpecString(oldSpecString, newPlds, recursive);
     * }
     */

    public static String updatePerfLevelSpecString(String oldSpecString, PerfLevelDescriptor[] newPlds, boolean recursive) {
        PerfLevelDescriptor[] currentPlds = getPerfLevelSpecsFromString(oldSpecString, false);

        if (currentPlds == null)
            currentPlds = PmiRegistry.getDefaultPerfLevelSpecs();

        if (newPlds == null)
            return oldSpecString;

        ArrayList tmpPds = new ArrayList(newPlds.length);
        for (int i = 0; i < newPlds.length; i++) {
            // skip plds like pmi=level if not recursive
            if (newPlds[i] == null)
                continue;

            String[] newPath = newPlds[i].getPath();
            if (!recursive && newPath.length == 1 && newPath[0].equalsIgnoreCase("pmi"))
                continue;

            boolean found = false;
            for (int j = 0; j < currentPlds.length; j++) {
                if (recursive) {
                    // update the subtree in currentPlds
                    if (newPlds[i].isSubPath(currentPlds[j])) {
                        currentPlds[j].setLevel(newPlds[i].getLevel()); // update it
                    }
                } else { // not recursive

                }

                // update itself in currentPlds
                if (newPlds[i].comparePath(currentPlds[j]) == 0) {
                    found = true;
                    currentPlds[j].setLevel(newPlds[i].getLevel()); // update it
                    if (!recursive)
                        break;
                }

            }
            if (!found) { // not included in subtree
                tmpPds.add(newPlds[i]);
            }
        }

        String ret = getStringFromPerfLevelSpecs(currentPlds, false);

        if (tmpPds.size() > 0) {
            PerfLevelDescriptor[] extraPlds = new PerfLevelDescriptor[tmpPds.size()];
            for (int i = 0; i < extraPlds.length; i++)
                extraPlds[i] = (PerfLevelDescriptor) tmpPds.get(i);

            String newString = getStringFromPerfLevelSpecs(extraPlds, false);
            if (newString != null)
                ret += ":" + newString;
        }

        return getCompressedPerfString(ret);
    }

    public static String getCompressedPerfString(String str) {
        //if ret is too long, convert back to pld and compress it.
        int length = str.lastIndexOf('=');
        if (length < pmiStringMaxLength) {
            return str;
        } else {
            PerfLevelDescriptor[] currentPlds = getPerfLevelSpecsFromString(str, false);
            ArrayList tmpPds = new ArrayList(currentPlds.length);
            for (int i = 0; i < currentPlds.length; i++) {
                // Don't include it if its parent has same level as it
                int topParentIndex = -1;
                for (int j = currentPlds.length - 1; j >= 0; j--) { // may be faster to find immediate parent
                    if (currentPlds[i].getPath().length <= 2)
                        break;
                    if (i == j)
                        continue;
                    if (currentPlds[j].isSubPath(currentPlds[i])) { // pld j is parent of pld i
                        if (currentPlds[j].getPath().length == currentPlds[i].getPath().length - 1) {
                            // immediate parent
                            if (currentPlds[j].level == currentPlds[i].level)
                                topParentIndex = j;
                            else
                                topParentIndex = -1;
                            break;
                        } else if (topParentIndex == -1 ||
                                   (currentPlds[topParentIndex].isSubPath(currentPlds[j]))) {
                            // find the closet parent in the tree
                            topParentIndex = j;
                        }
                    }

                }
                if (topParentIndex == -1 || currentPlds[topParentIndex].level != currentPlds[i].level)
                    tmpPds.add(currentPlds[i]);
            }
            currentPlds = new PerfLevelDescriptor[tmpPds.size()];
            for (int i = 0; i < currentPlds.length; i++)
                currentPlds[i] = (PerfLevelDescriptor) tmpPds.get(i);

            String res = getStringFromPerfLevelSpecs(currentPlds, false);
            //System.err.println("compressed string: " + res);
            return res;
        }

    }

    private static String createPmiSpecString(PerfLevelDescriptor pld) {
        if (pld == null)
            return null;

        StringBuffer pathString = new StringBuffer();
        String[] path = pld.getPath();
        if (path.length == 0)
            return null;
        if (path.length == 1 && path[0].equalsIgnoreCase("pmi")) {
            pathString.append("pmi");
        } else {
            for (int i = 0; i < path.length; i++) {
                if (i == 0 && path[i].equalsIgnoreCase("pmi")) // remove pmi prefix
                    continue;

                // now convert known module name to be compressModuleName
                // in order to reduce the string length in PmiAttributes
                boolean isModule = false;
                if (i == 0 || i == 1) {
                    for (int k = 0; k < PerfModules.moduleIDs.length; k++) {
                        if (path[i].equals(PerfModules.moduleIDs[k])) {
                            isModule = true;
                            // do not use compressModuleNames since it will be displayed on adminconsole
                            pathString.append(PerfModules.moduleIDs[k]);
                            break;
                        }
                    }
                }

                if (!isModule) {
                    pathString.append(path[i]);
                }
                if (i < path.length - 1)
                    pathString.append(mySeperator);
            }
        }

        if (pld.getLevel() == LEVEL_UNDEFINED)
            return null;

        String levelString = LEVEL_NONE_STRING;
        switch (pld.getLevel()) {
            case LEVEL_NONE:
                levelString = SHORT_LEVEL_NONE_STRING;
                break;
            case LEVEL_LOW:
                levelString = SHORT_LEVEL_LOW_STRING;
                break;
            case LEVEL_MEDIUM:
                levelString = SHORT_LEVEL_MEDIUM_STRING;
                break;
            case LEVEL_HIGH:
                levelString = SHORT_LEVEL_HIGH_STRING;
                break;
            case LEVEL_MAX:
                levelString = SHORT_LEVEL_MAX_STRING;
                break;
            case LEVEL_FINEGRAIN:
                levelString = SHORT_LEVEL_FG_STRING;
                break;
            default:
                Tr.warning(tc, "PMI0011W", "level=" + pld.getLevel());
        }
        pathString.append("=").append(levelString);
        return pathString.toString();
    }

    private static PerfLevelDescriptor getSpecFromString(String str) {
        String[] pathNlevel = null;//StrUtils.split(str, '=');
        if (str != null && !str.equals("") && pathNlevel.length != 2) {
            Tr.warning(tc, "PMI0011W", str);
            return null;
        }

        // module name and instance name are seperated by ">"
        // Note: originally use "/". However, it is easy to be used by
        //       EJB names. Use ">" instead. 
        String[] path = null;//StrUtils.split(pathNlevel[0], mySeperatorChar);
        if (path == null || path.length == 0) {
            Tr.warning(tc, "PMI0011W", str);
            return null;
        }

        // get module name
        String moduleName = null;
        int moduleIndex = -1;
        if (!path[0].equalsIgnoreCase("pmi")) {
            moduleName = path[0];
            moduleIndex = 0;
        } else if (path.length > 1) {
            moduleName = path[1];
            moduleIndex = 1;
        } else if (path.length == 1 && path[0].equalsIgnoreCase("pmi")) {
            moduleName = "pmi";
            path = null;
        }

        // check the correctness of moduleName
        if (moduleName != null) {
            boolean existingModule = false;
            if (moduleName.equals("pmi"))
                existingModule = true;

            if (!existingModule) {

                for (int i = 0; i < PerfModules.moduleIDs.length; i++) {
                    if (moduleName.equals(PerfModules.moduleIDs[i])
                        || moduleName.equals(compressModuleNames[i])) {
                        existingModule = true;
                        path[moduleIndex] = PerfModules.moduleIDs[i];
                        break;
                    }
                }

            }

        }

        // set the level
        String strLevel = pathNlevel[1];
        int level = LEVEL_UNDEFINED;

        if (strLevel.equalsIgnoreCase(SHORT_LEVEL_NONE_STRING)
            || strLevel.equalsIgnoreCase(LEVEL_NONE_STRING))
            level = LEVEL_NONE;
        else if (strLevel.equalsIgnoreCase(SHORT_LEVEL_LOW_STRING)
                 || strLevel.equalsIgnoreCase(LEVEL_LOW_STRING))
            level = LEVEL_LOW;
        else if (strLevel.equalsIgnoreCase(SHORT_LEVEL_MEDIUM_STRING)
                 || strLevel.equalsIgnoreCase(LEVEL_MEDIUM_STRING))
            level = LEVEL_MEDIUM;
        else if (strLevel.equalsIgnoreCase(SHORT_LEVEL_HIGH_STRING)
                 || strLevel.equalsIgnoreCase(LEVEL_HIGH_STRING))
            level = LEVEL_HIGH;
        else if (strLevel.equalsIgnoreCase(SHORT_LEVEL_MAX_STRING)
                 || strLevel.equalsIgnoreCase(LEVEL_MAX_STRING))
            level = LEVEL_MAX;
        // Custom/fg sepc can't be set via setInstrumentationString            
        else if (strLevel.equalsIgnoreCase(SHORT_LEVEL_FG_STRING))
            level = LEVEL_FINEGRAIN;

        // check if level is right (none, low, medium, high, maximum)
        if (level == LEVEL_UNDEFINED) {
            Tr.warning(tc, "PMI0011W", str);
        }

        return new PerfLevelDescriptor(path, level);
    }

    public static String normalizeName(String name) {
        if (name.indexOf('/') != -1) {
            return name.replace('/', '_');
        }
        return name;
    }

    // moduleID is used to create statistics, etc.
    // statsType is new is 6.0 and it indicates what statistics are in a given Stats object
    // statsType is used only in Stats object.
    // for example: moduleID = jvmRuntimeModule and JVM stats type is jvmRuntimeModule#
    // '#' indicates statistics only in that module. jvmRuntimeModule has 4 JVMPI sub-modules
    public static String statsTypeToModuleID(String statsType) {
        if (statsType != null) {
            int hashPos = statsType.indexOf('#');
            if (hashPos > 0)
                return statsType.substring(0, hashPos);
            else
                return statsType;
        } else
            return null;
    }
}
