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

    private PermissionsConfig parsePermissions(String ddText) throws Exception {
        return parsePermissions(ddText, null);
    }

    private PermissionsConfig parsePermissions(String ddText, String altMessage, String ... messages) throws Exception {
        // Permissions doesn't check the provisioning ...
        //
        // Both JavaEE and Jakarta versions are supported, regardless
        // of the provisioning.

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/myWar.war";
        String fragmentPath = null;
        
        return parse( appPath, modulePath, fragmentPath,
                      ddText, new PermissionsAdapter(), PermissionsConfig.DD_NAME,
                      altMessage, messages );
    }

    // Usual contents ...

    private static final String permissionsHead7 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
            " version=\"7\">";

    private static final String permissionsBody =
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
    
    private static final String permissionsTail =
        "</permissions>";
    
    private static final String permissions7 =
        permissionsHead7 + "\n" +
        permissionsBody + "\n" +
        permissionsTail;

    // TODO: Need to verify the schema location.

    private static final String permissionsHead9 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions" +
            " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/jakarta_9.xsd\"" +
            " version=\"9\">";

    private static final String permissions9 =
        permissionsHead9 + "\n" +
        permissionsBody + "\n" +    
        permissionsTail;

    // Specific errors ...

    private static final String permissions7InvalidClassName =
        permissionsHead7 + "\n" +
            "<permission>" +
                "<className>java.lang.AllPermission</className>" +
            "</permission>" + "\n" +
        permissionsTail;

    // Header errors ...

    private static final String permissionsInvalidRoot =
        "<permission>" +
            "<class-name>java.lang.AllPermission</class-name>" +
        "</permission>";

    private static final String permissionsInvalidVersion =    
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
            " version=\"6\">" + "\n" +
            permissionsBody + "\n" +
        permissionsTail;
    
    // This is now valid, because the version takes precedence.
    private static final String permissions7InvalidNamespace =    
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/java\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
            " version=\"7\">" + "\n" +
            permissionsBody + "\n" +
        permissionsTail;

    // This is is still invalid: There is no version, and the namespace cannot be used.
    private static final String permissionsInvalidNamespaceNoVersion =    
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/java\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                // " version=\"7\"" +
                ">" + "\n" +
                permissionsBody + "\n" +
            permissionsTail;

    // Missing one element ...

    private static final String permissions7NoSchema =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions" +
            // " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
            " version=\"7\">" + "\n" +
            permissionsBody + "\n" +
        permissionsTail;

    private static final String permissions7NoSchemaInstance =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                " version=\"7\">" + "\n" +
                permissionsBody + "\n" +
            permissionsTail;    
    
    private static final String permissions7NoSchemaLocation =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                " version=\"7\">" + "\n" +
                permissionsBody + "\n" +
            permissionsTail;        
    
    private static final String permissions7NoXSI =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                " version=\"7\">" + "\n" +
                permissionsBody + "\n" +
            permissionsTail;    
    
    private static final String permissions7NoVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<permissions" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd\"" +
                // " version=\"7\"" +
            ">" + "\n" +
                permissionsBody + "\n" +
            permissionsTail;        
    
    // Only one element ...

    private static final String permissions7SchemaOnly =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" + "\n" +
            permissionsBody + "\n" +
        permissionsTail;

    private static final String permissions7VersionOnly =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions version=\"7\">" +
            permissionsBody + "\n" +
        permissionsTail;

    private static final String permissions9SchemaOnly =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions xmlns=\"https://jakarta.ee/xml/ns/jakartaee\">" +
            permissionsBody + "\n" +
        permissionsTail;

    private static final String permissions9VersionOnly =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<permissions version=\"9\">" +
            permissionsBody + "\n" +
            permissionsTail;
    
    //

    @Test
    public void testPermissions7() throws Exception {
        PermissionsConfig permissionsConfig = parsePermissions(permissions7);
        verifyPermissions(permissionsConfig);
    }
    
    @Test
    public void testPermissions9() throws Exception {
        PermissionsConfig permissionsConfig = parsePermissions(permissions9);
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
        parsePermissions(permissionsInvalidRoot,
                "unexpected.root.element", "CWWKC2252E");
    }

    @Test
    public void testInvalidVersion() throws Exception {
        // parsePermissions(permissionsInvalidVersion,
        // "CWWKC2262E", "unsupported.deployment.descriptor.namespace");
        // The error code changed.
        parsePermissions(permissionsInvalidVersion,
                         UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                         UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }

    @Test
    public void testInvalidNamespace() throws Exception {
        // parsePermissions(permissionsInvalidNamespace,
        // "CWWKC2262E", "invalid.deployment.descriptor.namespace");
        // This is now valid, because the version has precedence.        
        parsePermissions(permissions7InvalidNamespace);
    }
    
    @Test
    public void testInvalidNamespaceNoVersion() throws Exception {
        parsePermissions(permissionsInvalidNamespaceNoVersion,
                         UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
                         UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }    

    @Test
    public void testInvalidClassNameAttr() throws Exception {
        parsePermissions(permissions7InvalidClassName,
                "CWWKC2259E", "unexpected.child.element");
    }
    
    //

    @Test
    public void testPermissions7NoSchema() throws Exception {
        // parsePermissions(permissions7NoSchema,
        // "CWWKC2262E", "invalid.deployment.descriptor.namespace");
        // This is now valid.
        parsePermissions(permissions7NoSchema);
    }    

    @Test
    public void testPermissions7NoSchemaInstance() throws Exception {
        parsePermissions(permissions7NoSchemaInstance, "xml.error", "CWWKC2272E");
    }    

    @Test
    public void testPermissions7NoSchemaLocation() throws Exception {
        parsePermissions(permissions7NoSchemaLocation); // Already passing
    }        

    @Test
    public void testPermissions7NoXSI() throws Exception {
        parsePermissions(permissions7NoXSI); // Already passing
    }        

    @Test
    public void testPermissions7NoVersion() throws Exception {
        // parsePermissions(permissions7NoVersion,
        // "CWWKC2262E", "invalid.deployment.descriptor.namespace");
        // This is now valid.        
        parsePermissions(permissions7NoVersion);
    }        
            
    //

    @Test
    public void testPermissions7SchemaOnly() throws Exception {
        // parsePermissions(permissions7SchemaOnly,
        // "CWWKC2262E", "invalid.deployment.descriptor.namespace");
        // This is now valid.
        parsePermissions(permissions7SchemaOnly);
    }

    @Test
    public void testPermissions7VersionOnly() throws Exception {
        // parsePermissions(permissions7VersionOnly,
        // "CWWKC2262E", "invalid.deployment.descriptor.namespace");
        // This is now valid.
        parsePermissions(permissions7VersionOnly);
    }
    
    @Test
    public void testPermissions9SchemaOnly() throws Exception {
        // parsePermissions(permissions9SchemaOnly,
        // "CWWKC2262E", "invalid.deployment.descriptor.namespace");
        // This is now valid.
        parsePermissions(permissions9SchemaOnly);
    }

    @Test
    public void testPermissions9VersionOnly() throws Exception {
        // parsePermissions(permissions9VersionOnly,
        // "CWWKC2262E", "invalid.deployment.descriptor.namespace");
        // This is now valid.
        parsePermissions(permissions9VersionOnly);
    }
}
