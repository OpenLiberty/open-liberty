/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
import com.ibm.ws.javaee.ddmodel.DDTestBase;

public class PermissionsTest extends DDTestBase {

    private PermissionsConfig parsePermissions(String xml, String ... messages) throws Exception {
        // Permissions doesn't check the provisioning ...
        //
        // Both JavaEE and Jakarta versions are supported, regardless
        // of the provisioning.

        return parse( xml, new PermissionsAdapter(), PermissionsConfig.DD_NAME, messages );
    }

    // Usual contents ...

    private static final String permissionsXMLHead7 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
            " version=\"7\">";

    private static final String permissionsXMLBody =
        "<permission>"  +
            "<class-name>java.io.FilePermission</class-name>" +
            "<name>/tmp/abc</name>" +
            "<actions>read,write</actions>" +
        "</permission>" + "\n" +
        "<permission>" +
            "<class-name>java.lang.RuntimePermission</class-name>" +
            "<name>createClassLoader</name>" +
        "</permission>" + "\n" +
        "<permission>" +
            "<class-name>java.lang.AllPermission</class-name>" +
        "</permission>";
    
    private static final String permissionsXMLTail =
        "</permissions>";
    
    private static final String permissionsXML7 =
        permissionsXMLHead7 + "\n" +
        permissionsXMLBody + "\n" +
        permissionsXMLTail;

    // TODO: Need to verify the schema location.

    private static final String permissionsXMLHead9 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions" +
            " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/jakarta_9.xsd\"" +
            " version=\"9\">";

    private static final String permissionsXML9 =
        permissionsXMLHead9 + "\n" +
        permissionsXMLBody + "\n" +    
        permissionsXMLTail;

    // Specific errors ...

    private static final String permissionsXMLInvalidClassName =
        permissionsXMLHead7 + "\n" +
            "<permission>" +
                "<className>java.lang.AllPermission</className>" +
            "</permission>" + "\n" +
        permissionsXMLTail;

    // Header errors ...

    private static final String permissionsXMLInvalidRoot =
        "<permission>" +
            "<class-name>java.lang.AllPermission</class-name>" +
        "</permission>";

    private static final String permissionsXMLInvalidVersion =    
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                " version=\"6\">" + "\n" +
        permissionsXMLTail;
    
    private static final String permissionsXMLInvalidNamespace =    
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/java\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
            " version=\"7\">" + "\n" +
        permissionsXMLTail;

    // Missing one element ...

    private static final String permissionsXML7NoSchema =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions" +
            // " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
            " version=\"7\">" + "\n" +
        permissionsXMLTail;

    private static final String permissionsXML7NoSchemaInstance =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                " version=\"7\">" + "\n" +
            permissionsXMLTail;    
    
    private static final String permissionsXML7NoSchemaLocation =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                " version=\"7\">" + "\n" +
            permissionsXMLTail;        
    
    private static final String permissionsXML7NoXSI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                " version=\"7\">" + "\n" +
            permissionsXMLTail;    
    
    private static final String permissionsXML7NoVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                // " version=\"7\">" + "\n" +
            permissionsXMLTail;        
    
    // Only one element ...

    private static final String permissionsXML7SchemaOnly =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"/>";

    private static final String permissionsXML7VersionOnly =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions version=\"7\"/>";

    private static final String permissionsXML9SchemaOnly =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions xmlns=\"https://jakarta.ee/xml/ns/jakartaee\">";

    private static final String permissionsXML9VersionOnly =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions version=\"9\"/>";    
    
    //

    @Test
    public void testPermissions7() throws Exception {
        PermissionsConfig permissionsConfig = parsePermissions(permissionsXML7);
        verifyPermissions(permissionsConfig);
    }
    
    @Test
    public void testPermissions9() throws Exception {
        PermissionsConfig permissionsConfig = parsePermissions(permissionsXML9);
        verifyPermissions(permissionsConfig);
    }    

    protected void verifyPermissions(PermissionsConfig permissionsConfig) {
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
        parsePermissions(permissionsXMLInvalidRoot, "CWWKC2252E", "invalid.root.element");
    }

    @Test
    public void testInvalidVersion() throws Exception {
        parsePermissions(permissionsXMLInvalidVersion, "CWWKC2262E", "invalid.deployment.descriptor.namespace");
    }

    @Test
    public void testInvalidNamespace() throws Exception {
        parsePermissions(permissionsXMLInvalidNamespace, "CWWKC2262E", "invalid.deployment.descriptor.namespace");
    }

    @Test
    public void testInvalidClassNameAttr() throws Exception {
        parsePermissions(permissionsXMLInvalidClassName, "CWWKC2259E", "unexpected.child.element");
    }
    
    //

    @Test
    public void testPermissions7NoSchema() throws Exception {
        parsePermissions(permissionsXML7NoSchema, "CWWKC2262E", "invalid.deployment.descriptor.namespace"); // Previously failing.
    }    

    @Test
    public void testPermissions7NoSchemaInstance() throws Exception {
        parsePermissions(permissionsXML7NoSchemaInstance, "CWWKC2272E", "xml.error");
    }    

    @Test
    public void testPermissions7NoSchemaLocation() throws Exception {
        parsePermissions(permissionsXML7NoSchemaLocation); // Already passing
    }        

    @Test
    public void testPermissions7NoXSI() throws Exception {
        parsePermissions(permissionsXML7NoXSI); // Already passing
    }        

    @Test
    public void testPermissions7NoVersion() throws Exception {
        parsePermissions(permissionsXML7NoVersion, "CWWKC2272E", "xml.error"); // Previously failing.
    }        
            
    //

    @Test
    public void testPermissions7SchemaOnly() throws Exception {
        parsePermissions(permissionsXML7SchemaOnly, "CWWKC2262E", "invalid.deployment.descriptor.namespace"); // Previously failing.
    }

    @Test
    public void testPermissions7VersionOnly() throws Exception {
        parsePermissions(permissionsXML7VersionOnly, "CWWKC2262E", "invalid.deployment.descriptor.namespace"); // Previously failing.
    }
    
    @Test
    public void testPermissions9SchemaOnly() throws Exception {
        parsePermissions(permissionsXML9SchemaOnly, "CWWKC2262E", "invalid.deployment.descriptor.namespace"); // Previously failing.
    }

    @Test
    public void testPermissions9VersionOnly() throws Exception {
        parsePermissions(permissionsXML9VersionOnly, "CWWKC2262E", "invalid.deployment.descriptor.namespace"); // Previously failing.
    }
}
