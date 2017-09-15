/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.web.impl;

/*
 *   This class is used for constructing WebResourcePermissions and WebRoleRefPermissions object.
 *   Mainly for creating the qualified url patterns and actions of http methods.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class URLMap {
    private static TraceComponent tc = Tr.register(URLMap.class);
    private StringBuffer newURLPattern;
    private Map<String, MethodConstraint> mapMethod = null;
    private Map<String, MethodConstraint> mapMethodOmission = null;
    private Map<String, MethodConstraint> mapAllMethods = null;
    // following List is used to compose unchecked string of http-method-omission.
    // this list has intersection of methods which are assigned to role.
    // for example, if there are two webresources which have the same urlpattern but different http-method-omission below:
    // a. http method omission GET, POST, assigned ROLE R1. 
    // b. http method omission GET, assigned ROLE R2.
    // these two should yield unchecked list GET.
    // In here after a. is processed, unchkResourceForOmissionList has GET, POST, then after b. GET since POST isn't an intersection of a. and b..
    // Note that List isn't syncrhonized, but I don't think it'll be a cause of any issue since this list is only used during initialization of each apps.
    private List<String> unchkResourceForOmissionList = null;
    private List<String> excludedForOmissionList = null;
    static String ALL_METHODS = "AllMethods";

    // attributes for searching the tables
    private static final int UNCHECKED = 1;
    private static final int EXCLUDED = 2;
    private static final int ROLE = 4;
    private static final int ROLE_NO_CHECK = 16;
    private static final int USERDATA = 8;
    private static final int EXCLUDED_OR_ROLE = 0x100;
    private static final int EXCLUDED_AND_ROLE = 0x200;
    private static final int EXCLUDED_OR_UNCHECKED = 0x400;
    private static final int EXCLUDED_OR_UNCHECKED_NO_ROLE = 0x800;
    private static final int UD_CONFIDENTIAL_OR_INTEGRAL_NON_NONE = 0x3001;
    private static final int UD_CONFIDENTIAL_OR_INTEGRAL_NO_EX_CHECK = 0x3002;
    private static final int UD_NONE = 0x4000;
    private static final int UD_NONE_AND_NON_CONF_INTEG_NO_EX_CHECK = 0x4001;
    private static final int UD_NONE_NO_EX_CHECK = 0x8002;
    private static final int EXCLUDED_OR_UD_NON_NONE = 0x8004;

    public URLMap(String url) {
        if (url != null) {
            newURLPattern = new StringBuffer(url);
        } else {
            newURLPattern = new StringBuffer();
        }
    }

    public void appendURLPattern(String urlPattern) {
        if (urlPattern != null && urlPattern.length() != 0) {
            newURLPattern.append(":" + urlPattern);
        }
    }

    public String getURLPattern() {
        return newURLPattern.toString();
    }

    public void setExcludedSet(List<String> mList) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting excluded methods");
        setMethodsAttribute(mList, false, EXCLUDED, null);
        return;
    }

    public void setExcludedSet(List<String> mList, boolean omission) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting excluded methods with omission");
        setMethodsAttribute(mList, omission, EXCLUDED, null);
        if (omission) {
            excludedForOmissionList = updateList(excludedForOmissionList, mList);
        }
        return;
    }

    public void setUncheckedSet(List<String> mList) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting unchecked methods");
        setMethodsAttribute(mList, false, UNCHECKED, null);
        return;
    }

    public void setUncheckedSet(List<String> mList, boolean omission) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting unchecked methods with omission");
        setMethodsAttribute(mList, omission, UNCHECKED, null);
        return;
    }

    public void setRoleMap(String roleName, List<String> mList) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting rolemap");
        setMethodsAttribute(mList, false, ROLE, roleName);
        return;
    }

    // if omission is true, put the item to omission list.
    public void setRoleMap(String roleName, List<String> mList, boolean omission) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting rolemap with omission");
        setMethodsAttribute(mList, omission, ROLE, roleName);
        if (omission) {
            unchkResourceForOmissionList = updateList(unchkResourceForOmissionList, mList);
        }
        return;
    }

    public void setUserDataMap(String constraintType, List<String> mList) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting userdata");
        setMethodsAttribute(mList, false, USERDATA, constraintType);
        return;
    }

    public void setUserDataMap(String constraintType, List<String> mList, boolean omission) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting userdata with omission");
        setMethodsAttribute(mList, omission, USERDATA, constraintType);
        return;
    }

    public ActionString getExcludedString() {
        if (mapAllMethods != null) {
            // all methods exists, return null (which means all)
            if (getMethod(mapAllMethods, EXCLUDED, true) != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "all methods - excluded (1)");
                return new ActionString();
            }
            // in the rest of the cases, no unchecked string exists.
            return null;
        }
        Map<String, MethodConstraint> outputNormal = null;
        Map<String, MethodConstraint> outputOmission = null;
        if (mapMethod != null) {
            outputNormal = getMethod(mapMethod, EXCLUDED, true);
        }
        if (mapMethodOmission != null) {
            outputOmission = getMethod(mapMethodOmission, EXCLUDED, true);
            if (outputOmission != null) {
                outputOmission = getMethod(mapMethodOmission, excludedForOmissionList);
                if (outputOmission == null) {
                    // this means that all methods are excluded. 
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "all methods - excluded (1)");
                    return new ActionString();
                }
            }
        }

        if ((outputNormal != null && outputNormal.size() > 0) || (outputOmission != null && outputOmission.size() > 0)) {
            // if one or more tables are avaiable, get merged string.
            ActionString output = getMergedMethod(outputNormal, outputOmission, false);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getExcludedMethod : " + output);
            return output;
        } else {
            // this means that there is  no exclude list in this url;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "no method - excluded (2)");
            return null;
        }
    }

    public ActionString getUncheckedString() {
        if (mapAllMethods != null) {
            // all methods, which is marked as unchecked, exist, return null (which means all)
            if (getMethod(mapAllMethods, UNCHECKED, true) != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "getUncheckedMethod : all methods");
                return new ActionString();
            }
            // in the rest of the cases, no unchecked string exists.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getUncheckedMethod : no match in all methods");
            return null;
        }
        Map<String, MethodConstraint> outputNormal = null;
        Map<String, MethodConstraint> outputOmission = null;
        if (mapMethod != null) {
            outputNormal = getMethod(mapMethod, EXCLUDED_OR_ROLE, true);
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "outputNormal: " + outputNormal);
        if (mapMethodOmission != null) {
            // to support following scenarios, search items which are excluded and assigned any role first, 
            // then if there is no match, exclude list. At last search role.
            // scenario1: Two identical URL pattern "/c/*", but different method.
            //            one is method omission of (!GET,POST,PUT) with  auth constraint "R1"
            //            another is method omission of (!GET,POST) with  empty auth constraints
            //            they should generates following unchecked list:
            //            (javax.security.jacc.WebResourcePermission /c/* GET,POST)
            // scenario2: Two identical URL pattern "/c/*"
            //            one is method omission of (!GET) with  auth constraint "R1"
            //            another is method omission of (!POST) with  empty auth constraints
            //            they should not generate any unchecked list, because method omission of !GET and !POST yield nothing.
            // scenario3: Two identical URL pattern "/c/*"
            //            one is method omission of (!GET) with  auth constraint "R1"
            //            another is method omission of (!POST,GET) with  empty auth constraints
            //            they should generates following unchecked list:
            //            (javax.security.jacc.WebResourcePermission /c/* GET)
            //            and POST should be protected.
            // if exclude exists, uncheck doesn't matter, since exclude takes precedence.
            // in this case, if role exists, search union of excluded and role.
            // if role doesn't exist, just use excluded.

            outputOmission = getMethod(mapMethodOmission, EXCLUDED, true);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "outputOmission(EXCLUDED): " + outputOmission);
            if (outputOmission != null) {
                if (existMethod(mapMethodOmission, ROLE_NO_CHECK, true)) {
                    outputOmission = getMethod(mapMethodOmission, unchkResourceForOmissionList);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "outputOmission(uncheckResource): " + outputOmission);
                    outputOmission = getMethod(outputOmission, EXCLUDED_AND_ROLE, true);
                    // then remove role which doesn't exist in a sum of unchecked role name.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "outputOmission(EXCLUDED_AND_ROLE): " + outputOmission);
                }
            } else {
                outputOmission = getMethod(mapMethodOmission, UNCHECKED, true);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "outputOmission(UNCHECKED): " + outputOmission);
                if (outputOmission == null) {
                    // else role exists, use it.
                    outputOmission = getMethod(mapMethodOmission, unchkResourceForOmissionList);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "outputOmission(unchkResource): " + outputOmission);
                } else {
                    // else if unchecked exists, merge with outputNormal, otherwise, all methods are unchecked.
                    if (outputNormal != null) {
                        // in this case, outputNormal takes precedence except role.
                        Map<String, MethodConstraint> roleOnly = getMethod(mapMethodOmission, ROLE, true);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "roleOnly: " + roleOnly);
                        if (roleOnly != null) {
                            // unless the same role name exists in outputOmission, remove it from outputNormal since it'll be overridden
                            // by the unchecked of outputOmission.
                            String method = null;
                            for (Entry<String, MethodConstraint> e : roleOnly.entrySet()) {
                                method = e.getKey();
                                if (!outputOmission.containsKey(method)) {
                                    outputNormal.remove(method);
                                    if (tc.isDebugEnabled())
                                        Tr.debug(tc, "removed from outputNormal : " + method);
                                }
                            }
                        }
                    }
                    outputOmission = null;
                }
            }
        }
        if ((outputNormal != null && outputNormal.size() > 0) || (outputOmission != null && outputOmission.size() > 0)) {
            // if one or more tables are avaiable, get merged string.
            ActionString output = getMergedMethod(outputNormal, outputOmission, true);
            if (output != null && output.getActions() == null) {
                // when all methods are returned, then all of the methods are assgined to something.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "all methods are returned, reset to null");
                output = null;
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getUncheckedMethod : " + output);
            return output;
        } else {
            // this means that there is  no unchecked list in this url;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getUncheckedMethod : no match");
            return null;
        }
    }

    // in all methods case, return one of followings: ":CONFIDENTIAL", ":INTEGRAL", or ":NONE"
    // value: CONFIDENTIAL, INTEGRAL or REST. In case of REST, returns unchecked methods.
    public ActionString getUserDataString(String value) {
        if (value == null) {
            return null;
        }
        ActionString output = null;
        if (mapAllMethods != null) {
            // handle all methods case.
            output = getUserDataStringFromAllMap(mapAllMethods, value);
        } else {
            output = getUserDataStringFromMethodsMap(value);
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getUserDataString output: " + output);
        return output;
    }

    // Returns HashMap which elements consist of (String role name, String methods). 
    // In here, null is used to represent all methods.

    public Map<String, String> getRoleMap() {
        if (mapAllMethods != null) {
            return getRoleMapFromAllMap();
        }
        Map<String, List<String>> outputRTMNormal = null;
        Map<String, List<String>> outputRTMOmission = null;

        if (mapMethod != null) {
            outputRTMNormal = getRoleToMethodMap(getMethod(mapMethod, ROLE, true));
            if (tc.isDebugEnabled())
                Tr.debug(tc, "outputRTMNormal: " + outputRTMNormal);
        }
        if (mapMethodOmission != null) {
            // it genereate output only following condition:
            // 1.role is assigned.
            // 2.neither unchecked or excluded.
            Map<String, MethodConstraint> omissionRole = getMethod(mapMethodOmission, ROLE_NO_CHECK, true);
            Map<String, MethodConstraint> validRole = getMethod(mapMethodOmission, EXCLUDED_OR_UNCHECKED_NO_ROLE, true);
            Map<String, MethodConstraint> uncheckedOrExcluded = getMethod(mapMethodOmission, EXCLUDED_OR_UNCHECKED, true);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "omissionRole: " + omissionRole + " validRole : " + validRole + " uncheckedOrExcluded : " + uncheckedOrExcluded);
            if (omissionRole != null && ((uncheckedOrExcluded != null && validRole != null) || uncheckedOrExcluded == null)) {
                outputRTMOmission = getRoleToMethodMap(omissionRole);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "outputRTMOmission: " + outputRTMOmission);
            }
        }

        // now merge both tables and generate method strings.
        Map<String, String> output = mergeRTM(outputRTMNormal, outputRTMOmission);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRoleMap: output: " + output);
        return output;
    }

// --------- private methods --------- 

    private void setMethodsAttribute(List<String> mList, boolean omission, int attribute, String value) {
        Map<String, MethodConstraint> mapWork = null;
        if (mList == null || mList.isEmpty()) {
            mList = new ArrayList<String>();
            mList.add(ALL_METHODS);
            if (mapAllMethods == null) {
                mapAllMethods = new HashMap<String, MethodConstraint>();
            }
            mapWork = mapAllMethods;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "all methods");
        } else if (!omission) {
            // http method
            if (mapMethod == null) {
                mapMethod = new HashMap<String, MethodConstraint>();
            }
            mapWork = mapMethod;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "http methods");
        } else {
            // http method omission
            if (mapMethodOmission == null) {
                mapMethodOmission = new HashMap<String, MethodConstraint>();
            }
            mapWork = mapMethodOmission;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "http methods omission");
        }

        for (String httpMethod : mList) {
            httpMethod = httpMethod.toUpperCase();
            MethodConstraint mc = null;
            if (mapWork.containsKey(httpMethod)) {
                mc = (mapWork.get(httpMethod));
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "method exists : " + httpMethod);
            } else {
                mc = new MethodConstraint();
                mapWork.put(httpMethod, mc);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "method created : " + httpMethod);
            }
            switch (attribute) {
                case UNCHECKED:
                    mc.setUnchecked();
                    break;
                case EXCLUDED:
                    mc.setExcluded();
                    break;
                case ROLE:
                    mc.setRole(value);
                    break;
                case USERDATA:
                    mc.setUserData(value);
                    break;
                default:
                    break;

            }
        }
    }

    private List<String> updateList(List<String> list, List<String> newItems) {
        if (newItems == null) { // this means all methods. do nothing.
            return list;
        }
        if (list == null || list.size() == 0) {
            list = new ArrayList<String>();
            // put everything without any check since this is the first data.
            for (String newItem : newItems) {
                list.add(newItem);
            }
        } else if (newItems.size() > 0) {
            for (int i = list.size() - 1; i >= 0; i--) {
                // since this removes items from list, start from the last item.
                String method = list.get(i);
                if (!newItems.contains(method)) {
                    list.remove(i);
                }
            }
        }
        return list;
    }

    private boolean existMethod(Map<String, MethodConstraint> methodMap, int attribute, boolean condition) {
        if (methodMap == null) {
            return false;
        }
        MethodConstraint mc = null;

        boolean result = false;
        boolean output = false;

        for (Entry<String, MethodConstraint> e : methodMap.entrySet()) {
            mc = e.getValue();
            result = false;
            switch (attribute) {
                case EXCLUDED:
                    result = mc.isExcluded();
                    break;

                case ROLE_NO_CHECK:
                    // role exists
                    if (!mc.isRoleSetEmpty()) {
                        result = true;
                    }
                    break;
                case UNCHECKED:
                    result = mc.isUnchecked();
                    break;

                case UD_NONE:
                    if (!mc.isExcluded()) {
                        String ud = mc.getUserData();
                        result = (ud == null) || mc.isUserDataNone();
                    }
                    break;
                case UD_CONFIDENTIAL_OR_INTEGRAL_NO_EX_CHECK:
                    String ud = mc.getUserData();
                    result = (ud != null) && (ud.equalsIgnoreCase("CONFIDENTIAL") || ud.equalsIgnoreCase("INTEGRAL"));
                    break;
                default:
                    break;
            }
            if (result == condition) {
                output = true;
                break;
            }
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "existMethod : " + output);
        return output;
    }

    private Map<String, MethodConstraint> getMethod(Map<String, MethodConstraint> methodMap, int attribute, boolean condition) {
        if (methodMap == null) {
            return null;
        }
        Map<String, MethodConstraint> output = new HashMap<String, MethodConstraint>();
        MethodConstraint mc = null;

        String method = null;
        boolean result = false;
        String ud = null;
        for (Entry<String, MethodConstraint> e : methodMap.entrySet()) {
            result = false;
            method = e.getKey();
            mc = (e.getValue());

            switch (attribute) {
                case ROLE:
                    // role exists
                    if (!mc.isExcluded() && !mc.isUnchecked() && !mc.isRoleSetEmpty()) {
                        result = true;
                    }
                    break;
                case ROLE_NO_CHECK:
                    // role exists
                    if (!mc.isRoleSetEmpty()) {
                        result = true;
                    }
                    break;
                case UNCHECKED:
                    result = mc.isUnchecked();
                    break;
                case EXCLUDED:
                    result = mc.isExcluded();
                    break;
                case EXCLUDED_OR_ROLE:
                    // per servlet 3.0 spec, union of role and unchecked is unchecked.
                    result = mc.isExcluded() || (!mc.isUnchecked() && !mc.isRoleSetEmpty());
                    break;
                case EXCLUDED_AND_ROLE:
                    result = mc.isExcluded() && !mc.isRoleSetEmpty(); // new code
                    break;

                case EXCLUDED_OR_UNCHECKED:
                    result = mc.isExcluded() || mc.isUnchecked();
                    break;

                case EXCLUDED_OR_UNCHECKED_NO_ROLE:
                    result = (mc.isExcluded() || mc.isUnchecked()) && mc.isRoleSetEmpty();
                    break;

                case UD_CONFIDENTIAL_OR_INTEGRAL_NON_NONE:
                    if (!mc.isExcluded() && !mc.isUserDataNone()) {
                        ud = mc.getUserData();
                        result = (ud != null) && (ud.equalsIgnoreCase("CONFIDENTIAL") || ud.equalsIgnoreCase("INTEGRAL"));
                    }
                    break;
                case UD_CONFIDENTIAL_OR_INTEGRAL_NO_EX_CHECK:
                    ud = mc.getUserData();
                    result = (ud != null) && (ud.equalsIgnoreCase("CONFIDENTIAL") || ud.equalsIgnoreCase("INTEGRAL"));
                    break;

                case UD_NONE:
                    if (!mc.isExcluded()) {
                        result = mc.isUserDataNone();
                    }
                    break;

                case UD_NONE_AND_NON_CONF_INTEG_NO_EX_CHECK:
                    if (!mc.isExcluded()) {
                        ud = mc.getUserData();
                        result = mc.isUserDataNone() && ((ud == null) || (!ud.equalsIgnoreCase("CONFIDENTIAL") && !ud.equalsIgnoreCase("INTEGRAL")));
                    }
                    break;

                case EXCLUDED_OR_UD_NON_NONE:
                    // excluded or userdata is set as confidential or integral
                    if (mc.isExcluded()) {
                        result = true;
                    }
                    else if (!mc.isUserDataNone()) {
                        ud = mc.getUserData();
                        result = (ud != null) && (ud.equalsIgnoreCase("CONFIDENTIAL") || ud.equalsIgnoreCase("INTEGRAL"));
                    }
                    break;

                case UD_NONE_NO_EX_CHECK:
                    //  none isn't assigned.
                    result = mc.isUserDataNone();
                    break;

                default:
                    return null; // invalid case.
            }
            if (result == condition) {
                output.put(method, mc);
            }
        }
        if (output.size() == 0) {
            output = null;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getMethod : attribute : " + attribute + " value : " + output);
        return output;
    }

    private ActionString getMergedMethod(Map<String, MethodConstraint> outputNormal, Map<String, MethodConstraint> outputOmission, boolean negative) {
        StringBuffer outputSB = new StringBuffer();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getMergedMethod : Normal :" + outputNormal + " Omission : " + outputOmission + " flag : " + negative);
        if (outputNormal != null && outputOmission == null && outputNormal.size() > 0) {
            Set<String> set = outputNormal.keySet();
            Iterator<String> iterator = set.iterator();
            boolean first = true;
            while (iterator.hasNext()) {
                if (first) {
                    first = false;
                    if (negative) {
                        outputSB.append("!");
                    }
                } else {
                    outputSB.append(",");
                }
                outputSB.append(iterator.next());
            }
        } else if (outputNormal == null && outputOmission != null && outputOmission.size() > 0) {
            Set<String> set = outputOmission.keySet();
            Iterator<String> iterator = set.iterator();
            boolean first = true;
            while (iterator.hasNext()) {
                if (first) {
                    first = false;
                    if (!negative) {
                        outputSB.append("!");
                    }
                } else {
                    outputSB.append(",");
                }
                outputSB.append(iterator.next());
            }
        } else if (outputNormal != null && outputNormal.size() > 0 && outputOmission != null && outputOmission.size() > 0) {
            // in this case, generate omission table which contains list of omission minus list of normal table.
            Set<String> set = outputOmission.keySet();
            Iterator<String> iterator = set.iterator();
            boolean found = false;
            while (iterator.hasNext()) {
                String method = iterator.next();
                if (!outputNormal.containsKey(method)) {
                    if (!found) {
                        found = true;
                        if (!negative) {
                            outputSB.append("!");
                        }
                    } else {
                        outputSB.append(",");
                    }
                    outputSB.append(method);
                }
            }
            if (!found) {
                // all of elements in the omission table has matching elements in the normal table.
                // this condition needs to be treated as all methods.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "getMergedMethod : all methods");
                return new ActionString();
            }
        } else {
            // this means that there is  nothing in the hashmap.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getMergedMethod : no output.");
            return null;
        }
        String output = outputSB.toString();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getMergedMethod : " + output);
        return new ActionString(output);
    }

    private ActionString getUserDataStringFromMethodsMap(String value) {
        if (value == null) {
            return null;
        }
        // both tables

        int attribute = 0;
        boolean negative = false;

        // first, check whether excluded exists.
        boolean excluded = existMethod(mapMethodOmission, EXCLUDED, true);
        Map<String, MethodConstraint> mapMethodOmissionNoExcluded = null;
        Map<String, MethodConstraint> outputNormal = null;
        Map<String, MethodConstraint> outputOmission = null;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "excluded : " + excluded + "\nexcludedForOmissionList: " + excludedForOmissionList + "\nmapMethodOmission: " + mapMethodOmission);
        if (excluded) {
            if (excludedForOmissionList == null || excludedForOmissionList.size() == 0) {
                // this means every methods are mapped to excluded. return none.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "getUserDataStringFromMethodsMap (" + value + ") everthing is excluded");
                return null;
            } else {
                mapMethodOmissionNoExcluded = getMethod(mapMethodOmission, excludedForOmissionList);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "mapMethodOmissionNoExcluded: " + mapMethodOmissionNoExcluded);
            }
        }

        // second, compose normal table.
        if (mapMethod != null) {
            if (value.equals("REST")) {
                attribute = EXCLUDED_OR_UD_NON_NONE;
                negative = true;
            } else {
                // assumes that CONFIDENTIAL and INTEGRAL 
                attribute = UD_CONFIDENTIAL_OR_INTEGRAL_NON_NONE;
            }
            outputNormal = getMethod(mapMethod, attribute, true);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "outputNormal: " + outputNormal);
        }
        // third, compose omission table.
        if (mapMethodOmission != null) {
            boolean noneExist = existMethod(mapMethodOmission, UD_NONE, true);
            boolean putNormal = false;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "noneExist: " + noneExist);

            if (value.equals("REST")) {
                if (noneExist) {
                    if (existMethod(mapMethodOmission, UD_CONFIDENTIAL_OR_INTEGRAL_NO_EX_CHECK, true)) {
                        putNormal = true;
                        if (mapMethodOmissionNoExcluded != null) {
                            outputOmission = getMethod(mapMethodOmissionNoExcluded, UD_NONE_AND_NON_CONF_INTEG_NO_EX_CHECK, true);
                        } else {
                            outputOmission = getMethod(mapMethodOmission, UD_NONE_AND_NON_CONF_INTEG_NO_EX_CHECK, true);
                        }
                    } else {
                        if (mapMethodOmissionNoExcluded != null) {
                            outputOmission = getMethod(mapMethodOmissionNoExcluded, UD_NONE_NO_EX_CHECK, true);
                        } else {
                            outputOmission = null;
                        }
                    }
                } else {
                    if (mapMethodOmissionNoExcluded != null) {
                        outputOmission = getMethod(mapMethodOmissionNoExcluded, EXCLUDED_OR_UD_NON_NONE, true);
                    } else {
                        outputOmission = getMethod(mapMethodOmission, EXCLUDED_OR_UD_NON_NONE, true);
                    }
                }
                negative = true;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "outputOmission(REST) : " + outputOmission);
            } else {
                // assumes that CONFIDENTIAL and INTEGRAL
                if (mapMethodOmissionNoExcluded == null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "set mapMethodOmissionNoExcluded :" + mapMethodOmission);
                    mapMethodOmissionNoExcluded = mapMethodOmission;
                }
                if (noneExist) {
                    // check if any confidential/integral exist
                    if (existMethod(mapMethodOmission, UD_CONFIDENTIAL_OR_INTEGRAL_NO_EX_CHECK, true)) {
                        // NONE & !CONF
                        putNormal = true;
                        outputOmission = getMethod(mapMethodOmissionNoExcluded, UD_NONE_AND_NON_CONF_INTEG_NO_EX_CHECK, true);
                    } else {
                        // no confidential/integral
                        outputOmission = null;
                    }
                } else {
                    outputOmission = getMethod(mapMethodOmissionNoExcluded, UD_CONFIDENTIAL_OR_INTEGRAL_NON_NONE, true);
                }
                negative = false;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "outputOmission(CONF) : " + outputOmission);
            }
            // handle putNormal.
            if (putNormal) {
                if (outputNormal != null && outputNormal.size() > 0) {
                    if (outputOmission != null) {
                        outputNormal.putAll(outputOmission);
                    }
                } else {
                    outputNormal = outputOmission;
                }
                outputOmission = null;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "putNormal outputNormal : " + outputNormal);
            }
        }
        if ((outputNormal != null && outputNormal.size() > 0) || (outputOmission != null && outputOmission.size() > 0)) {
            ActionString actionString = getMergedMethod(outputNormal, outputOmission, negative);
            String output = null;
            if (actionString != null) {
                if (!value.equals("REST")) {
                    // confidential or integral
                    if (actionString.getActions() == null) {
                        output = ":CONFIDENTIAL";
                    } else {
                        output = actionString.getActions() + ":CONFIDENTIAL";
                    }
                } else {
                    output = actionString.getActions();
                    if (output == null) {
                        // this means that all methods are mapped to CONFIDENTIAL, or EXCLUDED
                        return null;
                    }
                }
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getUserDataStringFromMethodsMap (" + value + ") : " + output);
            return new ActionString(output);
        } else {
            // no match.
            if (value.equals("REST")) { // in this case, return null to make all methods.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "getUserDataStringFromMethodsMap (" + value + ") return all method.");
                return new ActionString(":NONE");
            } else {
                // this means that there is no userdata defined.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "getUserDataStringFromMethodsMap (" + value + ") No match.");
                return null;
            }
        }
    }

    private Map<String, MethodConstraint> getMethod(Map<String, MethodConstraint> allMap, List<String> methods) {
        Map<String, MethodConstraint> output = null;
        if (methods != null && methods.size() > 0) {
            if (allMap != null) {
                output = new HashMap<String, MethodConstraint>();
                for (String method : methods) {
                    MethodConstraint mc = allMap.get(method);
                    if (mc != null) {
                        output.put(method, mc);
                    }
                }
                if (output.size() == 0) {
                    output = null;
                }
            }
        }
        return output;
    }

    protected ActionString getUserDataStringFromAllMap(Map<String, MethodConstraint> allMap, String value) {
        if (value == null || allMap == null) {
            return null;
        }
        boolean isRest = "REST".equals(value);
        String output = null;
        boolean done = false;
        if (isRest && (getMethod(allMap, EXCLUDED, true) != null)) {
            // if all methods is set as excluded, no need to add unchecked, then done.
            return null;
        }
        Map<String, MethodConstraint> methodMap = getMethod(allMap, EXCLUDED, false);
        if (methodMap != null) {
            for (Entry<String, MethodConstraint> e : methodMap.entrySet()) {
                // since this is for all methods, it's ok to get only one.
                MethodConstraint mc = e.getValue();
                String userDataConstraint = mc.getUserData();

                // userDataConstraint is one of following, CONFIDENTIAL, INTEGRAL, NONE, or null.
                if (userDataConstraint != null) {
                    if (userDataConstraint.equals("CONFIDENTIAL") || userDataConstraint.equals("INTEGRAL")) {
                        if ("CONFIDENTIAL_OR_INTEGRAL".equalsIgnoreCase(value)) {
                            // if either one is allowed:
                            output = ":CONFIDENTIAL";
                            done = true;
                        } else if (userDataConstraint.equalsIgnoreCase(value)) {
                            output = ":" + value; // create either :CONFIDENTIAL or :INTEGRAL
                            done = true;
                        } else if (isRest) {
                            return null;
                        }
                    }
                } else {
                    // if null
                    if (isRest) {
                        done = true;
                    } else {
                        return null;
                    }
                }
            }
            if (!done) {
                return null;
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getUserDataStringFromAllMap(HashMap output: " + output);
            return new ActionString(output);
        } else {
            // this condition should be internal error...
            return null;
        }
    }

    private Map<String, List<String>> getRoleToMethodMap(Map<String, MethodConstraint> input) {
        if (input == null || input.size() == 0) {
            return null;
        }
        Map<String, List<String>> output = new HashMap<String, List<String>>();
        String method = null;
        for (Entry<String, MethodConstraint> e : input.entrySet()) {
            method = e.getKey();
            MethodConstraint mc = e.getValue();
            List<String> roleList = mc.getRoleList();
            List<String> methodList = null;
            for (String role : roleList) {
                methodList = output.get(role);
                if (methodList == null) {
                    methodList = new ArrayList<String>();
                    output.put(role, methodList);
                }
                methodList.add(method);
            }
        }
        if (output.size() == 0) {
            output = null;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRoleToMethodMap : " + output);
        return output;
    }

    private Map<String, String> getRoleMapFromAllMap() {
        if (mapAllMethods != null) {
            // all methods, which is marked as unchecked, exist, return null (which means all)
            Map<String, MethodConstraint> methodMap = getMethod(mapAllMethods, ROLE, true);
            Map<String, String> output = new HashMap<String, String>();
            if (methodMap != null) {
                Set<String> set = methodMap.keySet();
                Iterator<String> iterator = set.iterator();
                if (iterator.hasNext()) {
                    // it's OK to get only one since only one extry exists in this map.
                    MethodConstraint mc = (methodMap.get(iterator.next()));
                    List<String> roleList = mc.getRoleList();
                    for (String role : roleList) {
                        output.put(role, null);
                    }
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "all methods - role : " + output);
                    return output;
                }
            }
        }
        return null;
    }

    private Map<String, String> mergeRTM(Map<String, List<String>> RTMNormal, Map<String, List<String>> RTMOmission) {
        boolean isNormal = (RTMNormal != null) && (RTMNormal.size() > 0);
        boolean isOmission = (RTMOmission != null) && (RTMOmission.size() > 0);

        if (isNormal && !isOmission) {
            return convertRTM(RTMNormal, false);
        } else if (!isNormal && isOmission) {
            return convertRTM(RTMOmission, true);
        } else if (isNormal && isOmission) {
            // this case won't happen in a real world, but implement the code anyway.
            return convertRTM(RTMNormal, RTMOmission);
        } else {
            return null;
        }
    }

    // convert array of method to string
    private String convertMethod(List<String> methodList, boolean negative) {
        boolean first = true;
        StringBuffer methodSB = new StringBuffer();
        for (String method : methodList) {
            if (first) {
                first = false;
                if (negative) {
                    methodSB.append("!");
                }
            } else {
                methodSB.append(",");
            }
            methodSB.append(method);
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "convertMethod : " + methodSB.toString());
        return methodSB.toString();
    }

    private Map<String, String> convertRTM(Map<String, List<String>> input, boolean negative) {
        Map<String, String> output = new HashMap<String, String>();
        for (Entry<String, List<String>> e : input.entrySet()) {
            String role = e.getKey();
            String methodString = convertMethod(e.getValue(), negative);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "role: " + role + " method: " + methodString);
            output.put(role, methodString);
        }
        if (output.size() == 0) {
            output = null;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "convertRTM(Map, boolean) : " + output);
        return output;
    }

    private Map<String, String> convertRTM(Map<String, List<String>> RTMNormal, Map<String, List<String>> RTMOmission) {
        Map<String, String> output = new HashMap<String, String>();
        // process omisstion table first.
        for (Entry<String, List<String>> e : RTMOmission.entrySet()) {
            String role = e.getKey();
            List<String> methodListOmission = e.getValue();
            List<String> methodList = RTMNormal.get(role);

            List<String> methodListMergedOmission = mergeMethodList(methodListOmission, methodList);

            String methodString = convertMethod(methodListMergedOmission, true);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "role: " + role + " method: " + methodString);
            output.put(role, methodString);
        }
        // process normal table.
        for (Entry<String, List<String>> e : RTMNormal.entrySet()) {
            String role = e.getKey();
            List<String> methodListOmission = RTMOmission.get(role);
            if (methodListOmission == null) {
                List<String> methodList = e.getValue();
                String methodString = convertMethod(methodList, false);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "role: " + role + " method: " + methodString);
                output.put(role, methodString);
            }
        }
        if (output.size() == 0) {
            output = null;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "convertRTM(Map, Map) : " + output);
        return output;
    }

    private List<String> mergeMethodList(List<String> methodListOmission, List<String> methodList)
    {
        List<String> output = new ArrayList<String>();
        for (int i = 0; i < methodListOmission.size(); i++)
        {
            String method = methodListOmission.get(i);
            if (methodList == null || (!methodList.contains(method))) {
                output.add(method);
            }
        }
        if (output.size() == 0) {
            output = null;
        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "mergeMethodList : " + output);
        return output;
    }

// getter methods for unit tests

    public static final int METHODS_NORMAL = 1;
    public static final int METHODS_OMISSION = 2;
    public static final int METHODS_ALL = 3;

    private Map<String, MethodConstraint> getMethodSet(int table, int attribute) {
        Map<String, MethodConstraint> map = null;
        switch (table) {
            case METHODS_NORMAL:
                map = mapMethod;
                break;
            case METHODS_OMISSION:
                map = mapMethodOmission;
                break;
            case METHODS_ALL:
                map = mapAllMethods;
                break;
            default:
                break;
        }
        return getMethod(map, attribute, true);
    }

    public List<String> getExcludedSet(int table) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Getting excluded methods");
        Map<String, MethodConstraint> mMap = getMethodSet(table, EXCLUDED);
        if (mMap != null) {
            return new ArrayList<String>(mMap.keySet());
        } else {
            return null;
        }
    }

    public List<String> getUncheckedSet(int table) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Getting unchecked methods");
        Map<String, MethodConstraint> mMap = getMethodSet(table, UNCHECKED);
        if (mMap != null) {
            return new ArrayList<String>(mMap.keySet());
        } else {
            return null;
        }
    }

    //returns entries which has a role
    public Map<String, MethodConstraint> getRoleMap(int table) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Getting role methods map");
        return getMethodSet(table, ROLE_NO_CHECK);
    }

    // returns entires which has any user data constraint other than NONE
    // to get entries with user data constraint is set as NONE, use getUserDataMapNone method
    public Map<String, MethodConstraint> getUserDataMap(int table) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Getting user data map of which attribute is either confidential or integral");
        return getMethodSet(table, UD_CONFIDENTIAL_OR_INTEGRAL_NO_EX_CHECK);
    }

    public Map<String, MethodConstraint> getUserDataMapNone(int table) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Getting role methods map of which attribute is none");
        return getMethodSet(table, UD_NONE_NO_EX_CHECK);
    }
// end of getter methods

}
