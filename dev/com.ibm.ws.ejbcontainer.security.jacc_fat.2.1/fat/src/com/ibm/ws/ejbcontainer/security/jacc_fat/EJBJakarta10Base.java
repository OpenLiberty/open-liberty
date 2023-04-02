/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.authorization.jacc.provider.WSPolicyConfigurationFactoryImpl;
import com.ibm.ws.security.authorization.jacc.provider.WSPolicyConfigurationImpl;
import com.ibm.ws.security.authorization.jacc.provider.AllPolicyConfigs;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.security.jacc.EJBMethodPermission;
import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyContext;

/**
 * Base class for EJBJakarta10Test. This class is extended for EJB in WAR testing.
 */
public abstract class EJBJakarta10Base extends EJBAnnTestBase {

    protected static Class<?> logClass = EJBJakarta10Base.class;

    protected abstract TestName getName();

    /**
     * Verify the following:
     * <OL>
     * <LI> Add excluded permissions to the Policy
     * <LI> Extract those exclded permissions by invoking the new method getExcludedPermissions
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> The input and output excluded permissions should be the same
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testGetExcludedPermissionsMethod() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        WSPolicyConfigurationFactoryImpl pcf = new WSPolicyConfigurationFactoryImpl();
        String contextID = "contextID";
        WSPolicyConfigurationImpl policyConfig = new WSPolicyConfigurationImpl(contextID);
        String cId = policyConfig.getContextID();
        Log.info(logClass, getName().getMethodName(), "context id = " + cId);
        if (!cId.equals(contextID)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        // getExludedPermissions test

        EJBMethodPermission ejbMethodPerm1 = new EJBMethodPermission("PolicyTestEJBExcluded", "denyAll,Local,java.lang.String");
        EJBMethodPermission ejbMethodPerm2 = new EJBMethodPermission("PolicyTestEJBExcluded", "denyAll,ServiceEndPoint,java.lang.String");
        Permissions inputPermCollection = new Permissions();
        inputPermCollection.add(ejbMethodPerm1);
        inputPermCollection.add(ejbMethodPerm2);
        policyConfig.addToExcludedPolicy(inputPermCollection);
        policyConfig.commit();

        PermissionCollection outputPermCollection = policyConfig.getExcludedPermissions();
        Log.info(logClass, getName().getMethodName(), "outputPermCollection = " + outputPermCollection.toString());
        Log.info(logClass, getName().getMethodName(), "inputPermCollection = " + inputPermCollection.toString());

        int numberOfInputPerms = 0;
        ArrayList<Permission> inputs = new ArrayList();
        for (Enumeration<Permission> enumInput = inputPermCollection.elements(); enumInput.hasMoreElements();) {
            numberOfInputPerms++;
            inputs.add(enumInput.nextElement());
        }

        int numberOfOutputPerms = 0;
        ArrayList<Permission> outputs = new ArrayList();
        for (Enumeration<Permission> enumOutput = outputPermCollection.elements(); enumOutput.hasMoreElements();) {
            numberOfOutputPerms++;
            outputs.add(enumOutput.nextElement());
        }

        if (inputs.size() != 2 || outputs.size() != 2) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        Log.info(logClass, getName().getMethodName(), "inputs = " + inputs.toString());
        Log.info(logClass, getName().getMethodName(), "outputs = " + outputs.toString());

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Add unchecked permissions to the Policy
     * <LI> Extract those unchecked permissions by invoking the new method getUncheckedPermissions
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> The input and output unchecked permissions should be the same
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testGetUncheckedPermissionsMethod() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        WSPolicyConfigurationFactoryImpl pcf = new WSPolicyConfigurationFactoryImpl();
        String contextID = "contextID";
        WSPolicyConfigurationImpl policyConfig = new WSPolicyConfigurationImpl(contextID);
        String cId = policyConfig.getContextID();
        Log.info(logClass, getName().getMethodName(), "context id = " + cId);
        if (!cId.equals(contextID)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        // getUncheckedPermissions test

        EJBMethodPermission ejbMethodPerm1 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,Local,java.lang.String");
        EJBMethodPermission ejbMethodPerm2 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,ServiceEndPoint,java.lang.String");
        Permissions inputPermCollection = new Permissions();
        inputPermCollection.add(ejbMethodPerm1);
        inputPermCollection.add(ejbMethodPerm2);
        policyConfig.addToUncheckedPolicy(inputPermCollection);
        policyConfig.commit();

        if (!verifyPolicyConfig(inputPermCollection, policyConfig)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Add permissions to several roles permissions to the Policy
     * <LI> Extract those permissions by invoking the new method getPerRolePermissions
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> The input and output unchecked permissions per roleshould be the same
     * <LI>
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testGetPerRoleMethod() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        WSPolicyConfigurationFactoryImpl pcf = new WSPolicyConfigurationFactoryImpl();
        String contextID = "contextID";
        WSPolicyConfigurationImpl policyConfig = new WSPolicyConfigurationImpl(contextID);
        String cId = policyConfig.getContextID();
        Log.info(logClass, getName().getMethodName(), "context id = " + cId);
        if (!cId.equals(contextID)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        // getPerRolePermissions test

        EJBMethodPermission ejbMethodPerm1 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,Local,java.lang.String");
        EJBMethodPermission ejbMethodPerm2 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,ServiceEndPoint,java.lang.String");
        EJBMethodPermission ejbMethodPerm3 = new EJBMethodPermission("PolicyTestEJBPerRolePermissions", "Manager,denyAll,ServiceEndPoint,java.lang.String");
        Permissions inputPermCollectionManager = new Permissions();
        Permissions inputPermCollectionEmployee = new Permissions();
        Permissions inputPermCollectionStarStar = new Permissions();
        inputPermCollectionManager.add(ejbMethodPerm3);
        inputPermCollectionEmployee.add(ejbMethodPerm2);
        inputPermCollectionStarStar.add(ejbMethodPerm1);
        policyConfig.addToRole("Manager", inputPermCollectionManager);
        policyConfig.addToRole("Employee", inputPermCollectionEmployee);
        policyConfig.addToRole("StarPlayer", inputPermCollectionStarStar);
        policyConfig.commit();

        Map<String, PermissionCollection> perRolePermissions = policyConfig.getPerRolePermissions();

        for (Map.Entry<String, PermissionCollection> entry : perRolePermissions.entrySet()) {

            ArrayList<Permission> inputs = new ArrayList();
            if (entry.getKey().equals("Manager")) {

                for (Enumeration<Permission> enumInput = entry.getValue().elements(); enumInput.hasMoreElements();) {
                    inputs.add(enumInput.nextElement());
                }
                if (inputs.size() != 1) {
                    throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
                }
                if (!inputs.get(0).getName().equals("PolicyTestEJBUnchecked") || !inputs.get(0).getActions().equals("denyAll,Local,java.lang.String")) {
                    throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
                }
            } else if (entry.getKey().equals("Employee")) {

                for (Enumeration<Permission> enumInput = entry.getValue().elements(); enumInput.hasMoreElements();) {
                    inputs.add(enumInput.nextElement());
                }
                if (inputs.size() != 1) {
                    throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
                }
                if (!inputs.get(0).getName().equals("PolicyTestEJBUnchecked") || !inputs.get(0).getActions().equals("denyAll,Local,java.lang.String")) {
                    throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
                }
            } else if (entry.getKey().equals("StarPlayer")) {

                for (Enumeration<Permission> enumInput = entry.getValue().elements(); enumInput.hasMoreElements();) {
                    inputs.add(enumInput.nextElement());
                }
                if (inputs.size() != 1) {
                    throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
                }
                if (!inputs.get(0).getName().equals("PolicyTestEJBUnchecked") || !inputs.get(0).getActions().equals("Manager,denyAll,ServiceEndPoint,java.lang.String")) {
                    throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
                }
            }
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());
    }

    @Mode(TestMode.LITE)
    @Test
    public void testGetPolicyConfigWithNoContextId() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        WSPolicyConfigurationFactoryImpl pcf = new WSPolicyConfigurationFactoryImpl();
        String contextID = "contextID";
        WSPolicyConfigurationImpl policyConfig = new WSPolicyConfigurationImpl(contextID);
        String cId = policyConfig.getContextID();
        Log.info(logClass, getName().getMethodName(), "context id = " + cId);
        if (!cId.equals(contextID)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        EJBMethodPermission ejbMethodPerm1 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,Local,java.lang.String");
        EJBMethodPermission ejbMethodPerm2 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,ServiceEndPoint,java.lang.String");
        Permissions inputPermCollection = new Permissions();
        inputPermCollection.add(ejbMethodPerm1);
        inputPermCollection.add(ejbMethodPerm2);
        policyConfig.addToUncheckedPolicy(inputPermCollection);

        policyConfig.commit();

        PolicyContext.setContextID(contextID);
        AllPolicyConfigs policyConfigs = AllPolicyConfigs.getInstance();
        policyConfigs.setPolicyConfig(contextID, policyConfig);

        PolicyConfiguration policyConfigNoContextID = pcf.getPolicyConfiguration();
        if (policyConfigNoContextID == null) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        if (!verifyPolicyConfig(inputPermCollection, policyConfig)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());

    }


    @Mode(TestMode.LITE)
    @Test
    public void testGetPolicyConfigWithNonExistantContextId() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        WSPolicyConfigurationFactoryImpl pcf = new WSPolicyConfigurationFactoryImpl();
        String contextID = "nonExistantContextID";
        WSPolicyConfigurationImpl policyConfig = new WSPolicyConfigurationImpl(contextID);
        String cId = policyConfig.getContextID();
        Log.info(logClass, getName().getMethodName(), "context id = " + cId);
        if (!cId.equals(contextID)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        EJBMethodPermission ejbMethodPerm1 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,Local,java.lang.String");
        EJBMethodPermission ejbMethodPerm2 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,ServiceEndPoint,java.lang.String");
        Permissions inputPermCollection = new Permissions();
        inputPermCollection.add(ejbMethodPerm1);
        inputPermCollection.add(ejbMethodPerm2);
        policyConfig.addToUncheckedPolicy(inputPermCollection);

        policyConfig.commit();

        PolicyContext.setContextID(contextID);
        AllPolicyConfigs policyConfigs = AllPolicyConfigs.getInstance();
        policyConfigs.setPolicyConfig(contextID, policyConfig);

        PolicyConfiguration policyConfigNoContextID = pcf.getPolicyConfiguration();
        if (policyConfigNoContextID == null) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        if (!verifyPolicyConfig(inputPermCollection, policyConfig)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());

    }

    @Mode(TestMode.LITE)
    @Test
    public void testGetPolicyConfigWithContextIdWithNoExistingPolicyConig () throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        WSPolicyConfigurationFactoryImpl pcf = new WSPolicyConfigurationFactoryImpl();
        String contextID = "contextIDWithNoPolicyConfig";

        PolicyContext.setContextID(contextID);
        AllPolicyConfigs policyConfigs = AllPolicyConfigs.getInstance();

        PolicyConfiguration policyConfigForContextIDWithoutPolicyConfig = pcf.getPolicyConfiguration();
        if (policyConfigForContextIDWithoutPolicyConfig != null) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());

    }


    @Mode(TestMode.LITE)
    @Test
    public void testGetPolicyConfigWithOnlyContextId() throws Exception {
        Log.info(logClass, getName().getMethodName(), "**Entering " + getName().getMethodName());

        WSPolicyConfigurationFactoryImpl pcf = new WSPolicyConfigurationFactoryImpl();
        String contextID = "contextID";
        WSPolicyConfigurationImpl policyConfig = new WSPolicyConfigurationImpl(contextID);
        String cId = policyConfig.getContextID();
        Log.info(logClass, getName().getMethodName(), "context id = " + cId);
        if (!cId.equals(contextID)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        EJBMethodPermission ejbMethodPerm1 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,Local,java.lang.String");
        EJBMethodPermission ejbMethodPerm2 = new EJBMethodPermission("PolicyTestEJBUnchecked", "denyAll,ServiceEndPoint,java.lang.String");
        Permissions inputPermCollection = new Permissions();
        inputPermCollection.add(ejbMethodPerm1);
        inputPermCollection.add(ejbMethodPerm2);
        policyConfig.addToUncheckedPolicy(inputPermCollection);
        policyConfig.commit();

        PolicyContext.setContextID(contextID);
        AllPolicyConfigs policyConfigs = AllPolicyConfigs.getInstance();
        policyConfigs.setPolicyConfig(contextID, policyConfig);

        PolicyConfiguration policyConfigOnlyContextID = pcf.getPolicyConfiguration(contextID);
        if (policyConfigOnlyContextID.getContextID() != contextID) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        if (!verifyPolicyConfig(inputPermCollection, policyConfig)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        Log.info(logClass, getName().getMethodName(), "**Exiting " + getName().getMethodName());

    }

    public boolean verifyPolicyConfig(PermissionCollection inputPermCollection, WSPolicyConfigurationImpl policyConfig) throws Exception {

        PermissionCollection outputPermCollection = policyConfig.getUncheckedPermissions();
        Log.info(logClass, getName().getMethodName(), "outputPermCollection = " + outputPermCollection.toString());
        Log.info(logClass, getName().getMethodName(), "inputPermCollection = " + inputPermCollection.toString());

        int numberOfInputPerms = 0;
        ArrayList<Permission> inputs = new ArrayList();
        for (Enumeration<Permission> enumInput = inputPermCollection.elements(); enumInput.hasMoreElements();) {
            numberOfInputPerms++;
            inputs.add(enumInput.nextElement());
        }

        int numberOfOutputPerms = 0;
        ArrayList<Permission> outputs = new ArrayList();
        for (Enumeration<Permission> enumOutput = outputPermCollection.elements(); enumOutput.hasMoreElements();) {
            numberOfOutputPerms++;
            outputs.add(enumOutput.nextElement());
        }

        if (inputs.size() != 2 || outputs.size() != 2) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        Log.info(logClass, getName().getMethodName(), "inputs = " + inputs.toString());
        Log.info(logClass, getName().getMethodName(), "outputs = " + outputs.toString());

        if (!inputs.equals(outputs)) {
            throw new Exception(MessageConstants.EJB_ACCESS_EXCEPTION);
        }

        return true;
    }
}
