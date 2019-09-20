/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.sl.provider;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.SecurityPermission;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyContextException;

public class WSPolicyConfigurationImpl implements PolicyConfiguration {

    public static enum ContextState {
        STATE_OPEN,
        STATE_IN_SERVICE,
        STATE_DELETED,
    };

    String contextID = null;
    List<Permission> excludedList = null;
    List<Permission> uncheckedList = null;
    Map<String, List<Permission>> roleToPermMap = new HashMap<String, List<Permission>>();
    ContextState state = ContextState.STATE_OPEN;

    public WSPolicyConfigurationImpl(String s) throws PolicyContextException {
        contextID = s;
        System.out.println("WSPolicyConfigurationImpl:<ctor> " + this);
    }

    @Override
    public String getContextID() throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }
        return contextID;
    }

    @Override
    public void addToRole(String s, PermissionCollection permissioncollection) throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("addToRole called when the PolicyConfiguration is not in the open state. The current state is = "
                                                              + getStateString(state));
        }
        if (permissioncollection != null) {
            List<Permission> permList = roleToPermMap.get(s);
            if (permList == null) {
                List<Permission> newPermList = new ArrayList<Permission>();
                for (Enumeration<Permission> e = permissioncollection.elements(); e.hasMoreElements();) {
                    newPermList.add(e.nextElement());
                }
                roleToPermMap.put(s, newPermList);
            } else {
                for (Enumeration<Permission> e = permissioncollection.elements(); e.hasMoreElements();) {
                    permList.add(e.nextElement());
                }
            }
        }
    }

    @Override
    public void addToRole(String s, Permission permission) throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("addToRole called when the PolicyConfiguration is not in the open state. The current state is = "
                                                              + getStateString(state));
        }
        if (permission != null) {
            List<Permission> permList = roleToPermMap.get(s);
            if (permList == null) {
                List<Permission> newPermList = new ArrayList<Permission>();
                newPermList.add(permission);
                roleToPermMap.put(s, newPermList);
            } else {
                permList.add(permission);
            }
        }
    }

    @Override
    public void addToUncheckedPolicy(PermissionCollection permissioncollection) throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("addToUncheckedPolicy called when the PolicyConfiguration is not in the open state. The current state is = "
                                                              + getStateString(state));
        }
        if (uncheckedList == null && permissioncollection != null) {
            uncheckedList = new ArrayList<Permission>();
        }
        if (permissioncollection != null) {
            for (Enumeration<Permission> e = permissioncollection.elements(); e.hasMoreElements();) {
                uncheckedList.add(e.nextElement());
            }
        }
    }

    @Override
    public void addToUncheckedPolicy(Permission permission) throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("addToUncheckedPolicy called when the PolicyConfiguration is not in the open state. The current state is = "
                                                              + getStateString(state));
        }
        if (uncheckedList == null && permission != null) {
            uncheckedList = new ArrayList<Permission>();
        }
        if (permission != null) {
            uncheckedList.add(permission);
        }
    }

    @Override
    public void addToExcludedPolicy(PermissionCollection permissioncollection) throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("addToExcludedPolicy called when the PolicyConfiguration is not in the open state. The current state is = "
                                                              + getStateString(state));
        }
        if (excludedList == null && permissioncollection != null) {
            excludedList = new ArrayList<Permission>();
        }
        if (permissioncollection != null) {
            for (Enumeration<Permission> e = permissioncollection.elements(); e.hasMoreElements();) {
                excludedList.add(e.nextElement());
            }
        }
    }

    @Override
    public void addToExcludedPolicy(Permission permission) throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("addToExcludedPolicy called when the PolicyConfiguration is not in the open state. The current state is = "
                                                              + getStateString(state));
        }
        if (excludedList == null && permission != null) {
            excludedList = new ArrayList<Permission>();
        }
        if (permission != null) {
            excludedList.add(permission);
        }
    }

    @Override
    public void removeRole(String s) throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("removeRole called when the PolicyConfiguration is not in the open state. The current state is = "
                                                              + getStateString(state));
        }
    }

    @Override
    public void removeUncheckedPolicy() throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("removeUncheckedPolicy called when the PolicyConfiguration is not in the open state. The current state is = "
                                                              + getStateString(state));
        }
    }

    @Override
    public void removeExcludedPolicy() throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("removeExcludedPolicy called when the PolicyConfiguration is not in the open state. The current state is : "
                                                              + getStateString(state));
        }
    }

    @Override
    public void linkConfiguration(PolicyConfiguration policyconfiguration) throws PolicyContextException {
        System.out.println("WSPolicyConfigurationImpl : linkConfiguration : " + policyconfiguration + " this : " + this);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state != ContextState.STATE_OPEN) {
            throw new java.lang.UnsupportedOperationException("linkConfiguration called when the PolicyConfiguration is not in the open state. The current state is : "
                                                              + getStateString(state));
        }
    }

    @Override
    public void delete() throws PolicyContextException {
        System.out.println("WSPolicyConfigurationImpl : delete : " + this);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        state = ContextState.STATE_DELETED;
        AllPolicyConfigs.getInstance().remove(contextID);
    }

    @Override
    public void commit() throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (state == ContextState.STATE_DELETED) {
            throw new java.lang.UnsupportedOperationException("commit called when the PolicyConfiguration is in the deleted state");
        }
        System.out.println("WSPolicyConfigurationImpl : commit : " + this);
        state = ContextState.STATE_IN_SERVICE;
    }

    @Override
    public boolean inService() throws PolicyContextException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }
        if (state == ContextState.STATE_IN_SERVICE) {
            return true;
        } else {
            return false;
        }
    }

    public void setState(ContextState newState) throws PolicyContextException {
        System.out.println("WSPolicyConfigurationImpl : setState : " + newState + " : " + this);
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SecurityPermission("setPolicy"));
        }

        if (newState != ContextState.STATE_OPEN) {
            throw new PolicyContextException("The PolicyConfiguration state can only be set to open.");
        }
        state = newState;
    }

    public List<Permission> getExcludedList() {
        return excludedList;
    }

    public List<Permission> getUncheckedList() {
        return uncheckedList;
    }

    public Map<String, List<Permission>> getRoleToPermMap() {
        return roleToPermMap;
    }

    private String getStateString(ContextState state) {
        String output = null;
        switch (state) {
            case STATE_OPEN:
                output = "open";
                break;
            case STATE_IN_SERVICE:
                output = "inService";
                break;

            case STATE_DELETED:
                output = "deleted";
                break;
            default:
                output = "unknown";
        }
        return output;
    }
}
