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
package com.ibm.ws.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * @author asisin
 * 
 */
@SuppressWarnings("unchecked")
public class VirtualHostMapper {

    private final static String CLASSNAME = VirtualHostMapper.class.getName();
    private final static TraceComponent tc = Tr.register(CLASSNAME, "Runtime");
    protected Map vHostTable;

    public VirtualHostMapper() {
        // System.out.println("New Map");
        // LIBERTY - switch to concurrent map
        vHostTable = /* new HashMap(); */new ConcurrentHashMap();
    }

    /**
     * method addMapping()
     * 
     * This method is wildcard aware. It searches for wildcard characters {*}
     * and normalizes the string so that it forms a valid regular expression
     * 
     * @param path
     * @param target
     */
    public void addMapping(String path, Object target) {
        vHostTable.put(normalize(path), target);
    }

    /**
     * This method normalizes the alias into a valid regular expression
     */
    private String normalize(String alias) {
        // replace all the '.'s to '\.'s
        String regExp = alias.replaceAll("[\\.]", "\\\\\\.");

        // replace all the '*'s to '.*'s
        regExp = regExp.replaceAll("[*]", "\\.\\*");

        // System.out.println("Normalized "+alias+" to "+regExp);

        return regExp;
    }

    /**
     * @param path
     */
    public void removeMapping(String key) {
        vHostTable.remove(normalize(key));
    }

    /**
     * Returns an enumeration of all the target mappings added
     * to this mapper
     */
    public Iterator targetMappings() {
        // System.out.println("TargetMappings called");
        // return vHostTable.values().iterator(); 316624
        Collection vHosts = vHostTable.values(); // 316624
        List l = new ArrayList(); // 316624
        l.addAll(vHosts); // 316624
        return l.listIterator(); // 316624
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.ws.webcontainer.core.RequestMapper#replaceMapping(java.lang.String,
     * java.lang.Object)
     */
    public Object replaceMapping(String path, Object target) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param vHostKey
     *            - the alias:port string
     * @return RequestProcessor
     */
    public Object getMapping(String vHostKey) {
        Iterator i = vHostTable.keySet().iterator();

        String bestMatchingPattern = null;
        int bestMatchingPatternLength = -1;

        while (i.hasNext()) {
            try {
                String pattern = (String) i.next();
                if (vHostKey.matches(pattern)) {
                    // PK77176 Start
                    if (vHostKey.equals(pattern)) {
                        bestMatchingPattern = pattern;
                        break;
                    }
                    //PM37645
                    if (pattern.equals("_ws_eh.*")) {
                        bestMatchingPattern = pattern;
                        break;
                    }
                    //PM37645

                    // PK77176 End
                    if (bestMatchingPattern == null) {
                        bestMatchingPattern = pattern;
                        bestMatchingPatternLength = pattern.length();
                    } else {
                        // another match. Figure out from length which one is better
                        if (pattern.length() >= bestMatchingPatternLength) {
                            bestMatchingPattern = pattern;
                            bestMatchingPatternLength = pattern.length();
                        }
                    }
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught an exception in getMapping():", e);
                }
                return null;
            }
        }

        if (bestMatchingPattern != null)
            return vHostTable.get(bestMatchingPattern);
        else
            return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.core.RequestMapper#exists(java.lang.String)
     */
    public boolean exists(String path) {
        Iterator i = vHostTable.keySet().iterator();

        while (i.hasNext()) {
            try {
                String pattern = (String) i.next();
                // System.out.println("Pattern = "+pattern);
                if (path.matches(pattern))
                    return true;
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught an exception in exists():", e);
                }
                return false;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * 
     * com.ibm.ws.webcontainer.core.RequestMapper#exactMatchExists(java.lang.String
     * )
     */
    public boolean exactMatchExists(String path) {
        String _path = normalize(path);

        Iterator i = vHostTable.keySet().iterator();

        while (i.hasNext()) {
            try {
                String pattern = (String) i.next();
                // System.out.println("Pattern = "+pattern);
                if (_path.equals(pattern))
                    return true;
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught an exception in getMapping():", e);
                }
                return false;
            }
        }

        return false;
    }

    // PK65158
    protected Object findExactMatch(String path) {
        String _path = normalize(path);

        Iterator i = vHostTable.keySet().iterator();

        while (i.hasNext()) {
            try {
                String pattern = (String) i.next();
                // System.out.println("Pattern = "+pattern);
                if (_path.equals(pattern))
                    return vHostTable.get(pattern);
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught an exception in getMapping():", e);
                }
                return null;
            }
        }

        return null;
    }

}
