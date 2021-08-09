/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.resource.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefInfo.Property;

public class ResourceRefConfigTest {
    private static ResourceRefConfigImpl serializeAndDeserialize(ResourceRefConfigImpl rrc) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(rrc);
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (ResourceRefConfigImpl) ois.readObject();
    }

    private static ResourceRefConfigImpl[] serializeAndDeserializePair(ResourceRefConfigImpl rrc) throws IOException, ClassNotFoundException {
        return new ResourceRefConfigImpl[] { rrc, serializeAndDeserialize(rrc) };
    }

    @Test
    public void testCtor() throws Exception {
        for (ResourceRefConfigImpl rrc : serializeAndDeserializePair(new ResourceRefConfigImpl(null, null))) {
            Assert.assertEquals(null, rrc.getName());
            Assert.assertEquals(null, rrc.getType());
        }

        for (ResourceRefConfigImpl rrc : serializeAndDeserializePair(new ResourceRefConfigImpl("name", "type"))) {
            Assert.assertEquals("name", rrc.getName());
            Assert.assertEquals("type", rrc.getType());
        }
    }

    @Test
    public void testToString() {
        // Ensure it doesn't throw.
        new ResourceRefConfigImpl(null, null).toString();
    }

    @Test
    public void testDescription() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertNull(rrcCopy.getDescription());
        }

        rrc.setDescription("desc");
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals("desc", rrcCopy.getDescription());
        }
    }

    @Test
    public void testType() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        rrc.setType("type");
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals("type", rrcCopy.getType());
        }
    }

    @Test
    public void testAuth() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(ResourceRef.AUTH_APPLICATION, rrcCopy.getAuth());
        }

        rrc.setResAuthType(ResourceRef.AUTH_CONTAINER);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(ResourceRef.AUTH_CONTAINER, rrcCopy.getAuth());
        }
    }

    @Test
    public void testSharingScope() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(ResourceRef.SHARING_SCOPE_SHAREABLE, rrcCopy.getSharingScope());
        }

        rrc.setSharingScope(ResourceRef.SHARING_SCOPE_UNSHAREABLE);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(ResourceRef.SHARING_SCOPE_UNSHAREABLE, rrcCopy.getSharingScope());
        }
    }

    @Test
    public void testBindigName() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertNull(rrcCopy.getJNDIName());
        }

        rrc.setJNDIName("bind");
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals("bind", rrcCopy.getJNDIName());
        }
    }

    @Test
    public void testLoginConfigurationName() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertNull(rrcCopy.getLoginConfigurationName());
        }

        rrc.setLoginConfigurationName("lcn");
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals("lcn", rrcCopy.getLoginConfigurationName());
        }
    }

    @Test
    public void testLoginProperties() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(Collections.emptyList(), rrcCopy.getLoginPropertyList());
        }

        rrc.addLoginProperty("n1", "v1");
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(1, rrcCopy.getLoginPropertyList().size());
            Assert.assertEquals("n1", rrcCopy.getLoginPropertyList().get(0).getName());
            Assert.assertEquals("v1", rrcCopy.getLoginPropertyList().get(0).getValue());
        }

        rrc.addLoginProperty("n2", "v2");
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(2, rrcCopy.getLoginPropertyList().size());
            Assert.assertEquals("n2", rrcCopy.getLoginPropertyList().get(1).getName());
            Assert.assertEquals("v2", rrcCopy.getLoginPropertyList().get(1).getValue());
        }

        rrc.clearLoginProperties();
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(Collections.emptyList(), rrcCopy.getLoginPropertyList());
        }
    }

    @Test
    public void testIsolationLevel() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(Connection.TRANSACTION_NONE, rrcCopy.getIsolationLevel());
        }

        for (int isoLevel : new int[] { Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_READ_UNCOMMITTED, Connection.TRANSACTION_REPEATABLE_READ,
                                       Connection.TRANSACTION_SERIALIZABLE }) {
            rrc.setIsolationLevel(isoLevel);
            for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
                Assert.assertEquals(isoLevel, rrcCopy.getIsolationLevel());
            }
        }
    }

    @Test
    public void testCommitPriority() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(0, rrcCopy.getCommitPriority());
        }

        rrc.setCommitPriority(1);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(1, rrcCopy.getCommitPriority());
        }
    }

    @Test
    public void testBranchCoupling() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl(null, null);
        for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
            Assert.assertEquals(ResourceRefConfig.BRANCH_COUPLING_UNSET, rrcCopy.getBranchCoupling());
        }

        for (int bc : new int[] { ResourceRefConfig.BRANCH_COUPLING_LOOSE, ResourceRefConfig.BRANCH_COUPLING_TIGHT }) {
            rrc.setBranchCoupling(bc);
            for (ResourceRefConfigImpl rrcCopy : serializeAndDeserializePair(rrc)) {
                Assert.assertEquals(bc, rrcCopy.getBranchCoupling());
            }
        }
    }

    @Test
    public void testBranchCouplingMerge() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl("baseref", "type");

        ResourceRefConfigImpl[] mrrcs = new ResourceRefConfigImpl[2];
        mrrcs[0] = new ResourceRefConfigImpl("ref2", "type");
        mrrcs[0].setBranchCoupling(ResourceRefConfig.BRANCH_COUPLING_TIGHT);

        mrrcs[1] = serializeAndDeserialize(mrrcs[0]);
        List<ResourceRefConfig.MergeConflict> conflicts = merge(rrc, mrrcs);
        // assert no conflicts
        Assert.assertEquals(0, conflicts.size());

        mrrcs[1].setBranchCoupling(ResourceRefConfig.BRANCH_COUPLING_LOOSE);

        conflicts = merge(rrc, mrrcs);
        // assert conflict
        Assert.assertEquals(1, conflicts.size());
        ResourceRefConfig.MergeConflict conflict = conflicts.get(0);
        Assert.assertEquals("branch-coupling", conflict.getAttributeName());
        Assert.assertEquals("TIGHT", conflict.getValue1());
        Assert.assertEquals("LOOSE", conflict.getValue2());

        // assert resulting config (first value should be used)
        Assert.assertEquals(ResourceRefConfig.BRANCH_COUPLING_TIGHT, rrc.getBranchCoupling());
    }

    @Test
    public void testCommitPriorityMerge() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl("baseref", "type");

        ResourceRefConfigImpl[] mrrcs = new ResourceRefConfigImpl[2];
        mrrcs[0] = new ResourceRefConfigImpl("ref2", "type");
        mrrcs[0].setCommitPriority(1);

        mrrcs[1] = serializeAndDeserialize(mrrcs[0]);
        List<ResourceRefConfig.MergeConflict> conflicts = merge(rrc, mrrcs);
        // assert no conflicts
        Assert.assertEquals(0, conflicts.size());

        mrrcs[1].setCommitPriority(2);

        conflicts = merge(rrc, mrrcs);
        // assert conflict
        Assert.assertEquals(1, conflicts.size());
        ResourceRefConfig.MergeConflict conflict = conflicts.get(0);
        Assert.assertEquals("commit-priority", conflict.getAttributeName());
        Assert.assertEquals("1", conflict.getValue1());
        Assert.assertEquals("2", conflict.getValue2());

        // assert resulting config (first value should be used)
        Assert.assertEquals(1, rrc.getCommitPriority());
    }

    @Test
    public void testIsolationLevelMerge() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl("baseref", "type");

        ResourceRefConfigImpl[] mrrcs = new ResourceRefConfigImpl[2];
        mrrcs[0] = new ResourceRefConfigImpl("ref2", "type");
        mrrcs[0].setIsolationLevel(Connection.TRANSACTION_SERIALIZABLE);

        mrrcs[1] = serializeAndDeserialize(mrrcs[0]);
        List<ResourceRefConfig.MergeConflict> conflicts = merge(rrc, mrrcs);
        // assert no conflicts
        Assert.assertEquals(0, conflicts.size());

        mrrcs[1].setIsolationLevel(Connection.TRANSACTION_NONE);

        conflicts = merge(rrc, mrrcs);
        // assert conflicts
        Assert.assertEquals(1, conflicts.size());
        ResourceRefConfig.MergeConflict conflict = conflicts.get(0);
        Assert.assertEquals("isolation-level", conflict.getAttributeName());
        Assert.assertEquals("TRANSACTION_SERIALIZABLE", conflict.getValue1());
        Assert.assertEquals("TRANSACTION_NONE", conflict.getValue2());

        // assert resulting config (first value should be used)
        Assert.assertEquals(Connection.TRANSACTION_SERIALIZABLE, rrc.getIsolationLevel());
    }

    @Test
    public void testBindingNameMerge() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl("baseref", "type");

        ResourceRefConfigImpl[] mrrcs = new ResourceRefConfigImpl[2];
        mrrcs[0] = new ResourceRefConfigImpl("ref2", "type");
        mrrcs[0].setJNDIName("jndiName0");

        mrrcs[1] = serializeAndDeserialize(mrrcs[0]);
        List<ResourceRefConfig.MergeConflict> conflicts = merge(rrc, mrrcs);
        // assert no conflicts
        Assert.assertEquals(0, conflicts.size());

        mrrcs[1].setJNDIName("jndiName1");

        conflicts = merge(rrc, mrrcs);

        // assert conflicts
        Assert.assertEquals(1, conflicts.size());
        ResourceRefConfig.MergeConflict conflict = conflicts.get(0);
        Assert.assertEquals("binding-name", conflict.getAttributeName());
        Assert.assertEquals("jndiName0", conflict.getValue1());
        Assert.assertEquals("jndiName1", conflict.getValue2());

        // assert resulting config (first value should be used)
        Assert.assertEquals("jndiName0", rrc.getJNDIName());
    }

    @Test
    public void testLoginConfigurationNameMerge() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl("baseref", "type");

        ResourceRefConfigImpl[] mrrcs = new ResourceRefConfigImpl[2];
        mrrcs[0] = new ResourceRefConfigImpl("ref2", "type");
        mrrcs[0].setLoginConfigurationName("loginCfg0");

        mrrcs[1] = serializeAndDeserialize(mrrcs[0]);
        List<ResourceRefConfig.MergeConflict> conflicts = merge(rrc, mrrcs);
        // assert no conflicts
        Assert.assertEquals(0, conflicts.size());

        mrrcs[1].setLoginConfigurationName("loginCfg1");

        // assert conflicts
        conflicts = merge(rrc, mrrcs);
        Assert.assertEquals(1, conflicts.size());
        ResourceRefConfig.MergeConflict conflict = conflicts.get(0);
        Assert.assertEquals("custom-login-configuration", conflict.getAttributeName());
        Assert.assertEquals("loginCfg0", conflict.getValue1());
        Assert.assertEquals("loginCfg1", conflict.getValue2());

        // assert resulting config (first value should be used)
        Assert.assertEquals("loginCfg0", rrc.getLoginConfigurationName());
    }

    @Test
    public void testAuthenticationAliasNameMerge() throws Exception {

        final String AUTHENTICATION_ALIAS_LOGIN_NAME = "DefaultPrincipalMapping";

        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl("baseref", "type");

        ResourceRefConfigImpl[] mrrcs = new ResourceRefConfigImpl[2];
        mrrcs[0] = new ResourceRefConfigImpl("ref2", "type");
        mrrcs[0].addLoginProperty(AUTHENTICATION_ALIAS_LOGIN_NAME, "bob");

        mrrcs[1] = serializeAndDeserialize(mrrcs[0]);
        List<ResourceRefConfig.MergeConflict> conflicts = merge(rrc, mrrcs);
        // assert no conflicts
        Assert.assertEquals(0, conflicts.size());

        mrrcs[1].addLoginProperty(AUTHENTICATION_ALIAS_LOGIN_NAME, "joe");

        // assert conflicts
        conflicts = merge(rrc, mrrcs);
        Assert.assertEquals(1, conflicts.size());
        ResourceRefConfig.MergeConflict conflict = conflicts.get(0);
        Assert.assertEquals("authentication-alias", conflict.getAttributeName());
        Assert.assertEquals("bob", conflict.getValue1());
        Assert.assertEquals("joe", conflict.getValue2());

        // assert resulting config (first value should be used)
        Property prop = rrc.getLoginPropertyList().get(0);
        Assert.assertNotNull(prop);
        Assert.assertEquals(AUTHENTICATION_ALIAS_LOGIN_NAME, prop.getName());
        Assert.assertEquals("bob", prop.getValue());
    }

    @Test
    public void testLoginPropertyMerge() throws Exception {

        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl("baseref", "type");

        ResourceRefConfigImpl[] mrrcs = new ResourceRefConfigImpl[2];
        mrrcs[0] = new ResourceRefConfigImpl("ref2", "type");
        mrrcs[0].addLoginProperty("prop1", "bob");
        mrrcs[0].addLoginProperty("prop2", "bob2");

        mrrcs[1] = serializeAndDeserialize(mrrcs[0]);
        List<ResourceRefConfig.MergeConflict> conflicts = merge(rrc, mrrcs);
        // assert no conflicts
        Assert.assertEquals(0, conflicts.size());

        mrrcs[1].addLoginProperty("prop1", "joe");

        // assert conflict
        conflicts = merge(rrc, mrrcs);
        Assert.assertEquals(1, conflicts.size());
        ResourceRefConfig.MergeConflict conflict = conflicts.get(0);
        Assert.assertEquals("custom-login-configuration prop1", conflict.getAttributeName());
        Assert.assertEquals("bob", conflict.getValue1());
        Assert.assertEquals("joe", conflict.getValue2());

        // assert resulting config (first value should be used)
        Property prop = rrc.getLoginPropertyList().get(0);
        Assert.assertNotNull(prop);
        Assert.assertEquals("prop1", prop.getName());
        Assert.assertEquals("bob", prop.getValue());
    }

    @Test
    public void testMultiMergeConflict() throws Exception {
        ResourceRefConfigImpl rrc = new ResourceRefConfigImpl("baseref", "type");

        ResourceRefConfigImpl[] mrrcs = new ResourceRefConfigImpl[3];
        mrrcs[0] = new ResourceRefConfigImpl("ref2", "type");
        mrrcs[0].setLoginConfigurationName("loginCfg0");

        mrrcs[1] = serializeAndDeserialize(mrrcs[0]);
        mrrcs[2] = serializeAndDeserialize(mrrcs[0]);
        List<ResourceRefConfig.MergeConflict> conflicts = merge(rrc, mrrcs);
        // assert no conflicts
        Assert.assertEquals(0, conflicts.size());

        mrrcs[1].setLoginConfigurationName("loginCfg1");
        mrrcs[2].setLoginConfigurationName("loginCfg2");

        // assert conflicts
        conflicts = merge(rrc, mrrcs);
        Assert.assertEquals(2, conflicts.size());
        ResourceRefConfig.MergeConflict conflict = conflicts.get(0);
        Assert.assertEquals("custom-login-configuration", conflict.getAttributeName());
        Assert.assertEquals("loginCfg0", conflict.getValue1());
        Assert.assertEquals("loginCfg1", conflict.getValue2());

        conflict = conflicts.get(1);
        Assert.assertEquals("custom-login-configuration", conflict.getAttributeName());
        Assert.assertEquals("loginCfg0", conflict.getValue1());
        Assert.assertEquals("loginCfg2", conflict.getValue2());

        // assert resulting config (first value should be used)
        Assert.assertEquals("loginCfg0", rrc.getLoginConfigurationName());
    }

    private List<ResourceRefConfig.MergeConflict> merge(ResourceRefConfig rrc, ResourceRefConfig[] rrcs) {
        List<ResourceRefConfig.MergeConflict> conflicts = new ArrayList<ResourceRefConfig.MergeConflict>();
        rrc.mergeBindingsAndExtensions(rrcs, conflicts);
        return conflicts;
    }

    @Test
    public void testCompareDefaults() throws Exception {
        ResourceRefConfigImpl rrc1 = new ResourceRefConfigImpl("name", "type");
        ResourceRefConfigImpl rrc2 = new ResourceRefConfigImpl("name", "type");
        List<ResourceRefConfig.MergeConflict> conflicts = rrc1.compareBindingsAndExtensions(rrc2);
        Assert.assertTrue(conflicts.toString(), conflicts.isEmpty());
    }

    @Test
    public void testCompareSetDefaults() throws Exception {
        ResourceRefConfigImpl rrc1 = new ResourceRefConfigImpl("name", "type");
        ResourceRefConfigImpl rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.setLoginConfigurationName(null);
        rrc2.setIsolationLevel(Connection.TRANSACTION_NONE);
        rrc2.setCommitPriority(0);

        List<ResourceRefConfig.MergeConflict> conflicts = rrc1.compareBindingsAndExtensions(rrc2);
        Assert.assertTrue(conflicts.toString(), conflicts.isEmpty());
    }

    @Test
    public void testCompareEqual() throws Exception {
        List<ResourceRefConfigImpl> rrcs = new ArrayList<ResourceRefConfigImpl>();
        for (int i = 0; i < 2; i++) {
            ResourceRefConfigImpl rrc = new ResourceRefConfigImpl("name", "type");
            rrc.setLoginConfigurationName("lcn");
            rrc.addLoginProperty("name1", "value1");
            rrc.addLoginProperty("name2", "value2");
            rrc.setIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
            rrc.setCommitPriority(1);
            rrc.setBranchCoupling(ResourceRefConfig.BRANCH_COUPLING_LOOSE);
            rrcs.add(rrc);
        }

        List<ResourceRefConfig.MergeConflict> conflicts = rrcs.get(0).compareBindingsAndExtensions(rrcs.get(1));
        Assert.assertTrue(conflicts.toString(), conflicts.isEmpty());
    }

    private static void assertCompareConflict(ResourceRefConfig rrc1, ResourceRefConfig rrc2, String attributeName, Object value1, Object value2) {
        List<ResourceRefConfig.MergeConflict> conflicts = rrc1.compareBindingsAndExtensions(rrc2);
        Assert.assertEquals(conflicts.toString(), 1, conflicts.size());
        ResourceRefConfig.MergeConflict conflict = conflicts.get(0);
        Assert.assertEquals(conflict.toString(), attributeName, conflict.getAttributeName());
        Assert.assertEquals(conflict.toString(), 0, conflict.getIndex1());
        Assert.assertEquals(conflict.toString(), value1, conflict.getValue1());
        Assert.assertEquals(conflict.toString(), 1, conflict.getIndex2());
        Assert.assertEquals(conflict.toString(), value2, conflict.getValue2());
    }

    @Test
    public void testCompareLoginConfigurationNameConflict() throws Exception {
        ResourceRefConfigImpl rrc1 = new ResourceRefConfigImpl("name", "type");
        ResourceRefConfigImpl rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.setLoginConfigurationName("lcn");
        assertCompareConflict(rrc1, rrc2, "custom-login-configuration", "null", "lcn");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.setLoginConfigurationName("lcn");
        rrc2 = new ResourceRefConfigImpl("name", "type");
        assertCompareConflict(rrc1, rrc2, "custom-login-configuration", "lcn", "null");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.setLoginConfigurationName("lcn1");
        rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.setLoginConfigurationName("lcn2");
        assertCompareConflict(rrc1, rrc2, "custom-login-configuration", "lcn1", "lcn2");
    }

    @Test
    public void testCompareLoginPropertyConflict() throws Exception {
        ResourceRefConfigImpl rrc1 = new ResourceRefConfigImpl("name", "type");
        ResourceRefConfigImpl rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.addLoginProperty("name", "value");
        assertCompareConflict(rrc1, rrc2, "custom-login-configuration name", "null", "value");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.addLoginProperty("name", "value");
        rrc2 = new ResourceRefConfigImpl("name", "type");
        assertCompareConflict(rrc1, rrc2, "custom-login-configuration name", "value", "null");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.addLoginProperty("name", "value1");
        rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.addLoginProperty("name", "value2");
        assertCompareConflict(rrc1, rrc2, "custom-login-configuration name", "value1", "value2");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.addLoginProperty("name1", "value");
        rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.addLoginProperty("name2", "value");
        List<ResourceRefConfig.MergeConflict> conflicts = rrc1.compareBindingsAndExtensions(rrc2);
        Assert.assertEquals(conflicts.toString(), 2, conflicts.size());
        for (int i = 0; i < 2; i++) {
            ResourceRefConfig.MergeConflict conflict = conflicts.get(i);
            Assert.assertEquals(conflict.toString(), "custom-login-configuration name" + (i + 1), conflict.getAttributeName());
            Assert.assertEquals(conflict.toString(), 0, conflict.getIndex1());
            Assert.assertEquals(conflict.toString(), i == 0 ? "value" : "null", conflict.getValue1());
            Assert.assertEquals(conflict.toString(), 1, conflict.getIndex2());
            Assert.assertEquals(conflict.toString(), i == 0 ? "null" : "value", conflict.getValue2());
        }
    }

    @Test
    public void testCompareIsolationLevelConflict() throws Exception {
        ResourceRefConfigImpl rrc1 = new ResourceRefConfigImpl("name", "type");
        ResourceRefConfigImpl rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.setIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
        assertCompareConflict(rrc1, rrc2, "isolation-level", "TRANSACTION_NONE", "TRANSACTION_REPEATABLE_READ");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.setIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
        rrc2 = new ResourceRefConfigImpl("name", "type");
        assertCompareConflict(rrc1, rrc2, "isolation-level", "TRANSACTION_REPEATABLE_READ", "TRANSACTION_NONE");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.setIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
        rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.setIsolationLevel(Connection.TRANSACTION_SERIALIZABLE);
        assertCompareConflict(rrc1, rrc2, "isolation-level", "TRANSACTION_REPEATABLE_READ", "TRANSACTION_SERIALIZABLE");
    }

    @Test
    public void testCompareCommitPriorityConflict() throws Exception {
        ResourceRefConfigImpl rrc1 = new ResourceRefConfigImpl("name", "type");
        ResourceRefConfigImpl rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.setCommitPriority(1);
        assertCompareConflict(rrc1, rrc2, "commit-priority", "0", "1");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.setCommitPriority(1);
        rrc2 = new ResourceRefConfigImpl("name", "type");
        assertCompareConflict(rrc1, rrc2, "commit-priority", "1", "0");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.setCommitPriority(1);
        rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.setCommitPriority(2);
        assertCompareConflict(rrc1, rrc2, "commit-priority", "1", "2");
    }

    @Test
    public void testCompareBranchCouplingConflict() throws Exception {
        ResourceRefConfigImpl rrc1 = new ResourceRefConfigImpl("name", "type");
        ResourceRefConfigImpl rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.setBranchCoupling(ResourceRefConfig.BRANCH_COUPLING_LOOSE);
        assertCompareConflict(rrc1, rrc2, "branch-coupling", "null", "LOOSE");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.setBranchCoupling(ResourceRefConfig.BRANCH_COUPLING_LOOSE);
        rrc2 = new ResourceRefConfigImpl("name", "type");
        assertCompareConflict(rrc1, rrc2, "branch-coupling", "LOOSE", "null");

        rrc1 = new ResourceRefConfigImpl("name", "type");
        rrc1.setBranchCoupling(ResourceRefConfig.BRANCH_COUPLING_LOOSE);
        rrc2 = new ResourceRefConfigImpl("name", "type");
        rrc2.setBranchCoupling(ResourceRefConfig.BRANCH_COUPLING_TIGHT);
        assertCompareConflict(rrc1, rrc2, "branch-coupling", "LOOSE", "TIGHT");
    }
}
