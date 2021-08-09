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
package com.ibm.ws.javaee.ddmodel.permissions;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.ibm.ws.javaee.dd.permissions.Permission;
import com.ibm.ws.javaee.dd.permissions.PermissionsConfig;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.DDTestBase;

public class PermissionsTest extends DDTestBase {

    private PermissionsConfig parsePermissions(final String xml) throws Exception {

        return parse(xml, new PermissionsAdapter(), PermissionsConfig.DD_NAME);
    }

    @Test
    public void testPermissionsAdapter() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
                     + "<permissions xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" "
                     + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                     + "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee "
                     + "http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\" "
                     + "version=\"7\">"
                     + "<permission><class-name>java.io.FilePermission</class-name><name>/tmp/abc</name><actions>read,write</actions></permission>"
                     + "<permission><class-name>java.lang.RuntimePermission</class-name><name>createClassLoader</name></permission>"
                     + "<permission><class-name>java.lang.AllPermission</class-name></permission></permissions>";

        PermissionsConfig permissionsConfig = parsePermissions(xml);

        List<Permission> permissionList = permissionsConfig.getPermissions();

        Permission permission = permissionList.get(0);

        Assert.assertEquals("java.io.FilePermission", permission.getClassName());
        Assert.assertEquals("/tmp/abc", permission.getName());
        Assert.assertEquals("read,write", permission.getActions());

        permission = permissionList.get(1);

        Assert.assertEquals("java.lang.RuntimePermission", permission.getClassName());
        Assert.assertEquals("createClassLoader", permission.getName());
        Assert.assertEquals(null, permission.getActions());

        permission = permissionList.get(2);

        Assert.assertEquals("java.lang.AllPermission", permission.getClassName());
        Assert.assertEquals(null, permission.getName());
        Assert.assertEquals(null, permission.getActions());
    }

    @Test
    public void testInvalidRootElement() throws Exception {
        try {
            parsePermissions("<permission><class-name>java.lang.AllPermission</class-name></permission>");
            Assert.fail("Invalid Root Element accepted");
        } catch (ParseException ex) {
            // NO-OP
        }
    }

    @Test
    public void testInvalidVersion() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
                     + "<permissions xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" "
                     + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                     + "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee "
                     + "http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\" "
                     + "version=\"6\" />";
        try {
            parsePermissions(xml);
            Assert.fail("Invalid version accepted");
        } catch (ParseException ex) {
            // NO-OP
        }
    }

    @Test
    public void testInvalidNamespace() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
                     + "<permissions xmlns=\"http://xmlns.jcp.org/xml/ns/java\" " // Invalid NS
                     + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                     + "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee "
                     + "http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\" "
                     + "version=\"7\" />";
        try {
            parsePermissions(xml);
            Assert.fail("Invalid Namespace accepted");
        } catch (ParseException ex) {
            // NO-OP
        }
    }

    @Test
    public void testInvalidClassNameAttr() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
                     + "<permissions xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" "
                     + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                     + "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee "
                     + "http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\" "
                     + "version=\"7\">"
                     + "<permission><className>java.lang.AllPermission</className></permission></permissions>";
        try {
            parsePermissions(xml);
            Assert.fail("Invalid class name attribute accepted");
        } catch (ParseException ex) {
            // NO-OP
        }
    }
}
