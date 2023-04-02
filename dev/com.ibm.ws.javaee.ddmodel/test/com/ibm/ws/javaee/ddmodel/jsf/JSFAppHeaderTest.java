/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.jsf;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.jsf.FacesConfig;

public class JSFAppHeaderTest extends JSFAppTestBase {

    // "1.0", FACES_CONFIG_DTD_PUBLIC_ID_10
    // "1.1", FACES_CONFIG_DTD_PUBLIC_ID_11
    //
    // "1.2", NAMESPACE_SUN_JAVAEE
    // "2.0", NAMESPACE_SUN_JAVAEE
    // "2.1", NAMESPACE_SUN_JAVAEE
    //
    // "2.2", NAMESPACE_JCP_JAVAEE
    // "2.3", NAMESPACE_JCP_JAVAEE
    //
    // "3.0", NAMESPACE_JAKARTA
    // "4.0", NAMESPACE_JAKARTA

    public String jsfWithout(int version, HeaderLine omit, String jsfBody) {
        return omit.adjust( jsf(version, jsfBody) );
    }
    
    protected static final String jsf12NamespaceOnly = 
            "<faces-config xmlns=\"http://java.sun.com/xml/ns/javaee\"/>";
    protected static final String jsf22NamespaceOnly =
            "<faces-config xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"/>";
    protected static final String jsf30NamespaceOnly =
            "<faces-config xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"/>";

    /**
     * Answer the namespace which is used by a specified version.
     *
     * Caution!  Multiple versions answer the same namespace.
     *
     * @param version The version used to select the namespace.
     *
     * @return The namespace used by the specified version.
     */
    public static String jsfNamespaceOnly(int version) {
        if ( version < FacesConfig.VERSION_2_2 ) {
            return jsf12NamespaceOnly;
        } else if ( version < FacesConfig.VERSION_3_0 ) {
            return jsf22NamespaceOnly;
        } else {
            return jsf30NamespaceOnly;
        }
    }
    
    protected static String jsfVersionOnly(int version) {
        return "<faces-config version=\"" + getDottedVersionText(version) + "\"/>";
    }

    //
    
    protected static final String jsf20VersionMismatch =
            "<faces-config" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " version=\"2.0\"/>";

    protected static final String jsfNamespaceOnlyUnknown =
            "<faces-config xmlns=\"junk\"/>";

    protected static final String jsfVersionOnlyUnknown =
            "<faces-config version=\"9.9\"/>";

    //

    @Test
    public void testJSF_NoNamespace() throws Exception {
        for ( int version : FacesConfig.SCHEMA_VERSIONS ) {
            String strVersion = getDottedVersionText(version);
            FacesConfig facesConfig = parse( jsfWithout(version, HeaderLine.NS, ""), version);
            Assert.assertEquals( strVersion, facesConfig.getVersion() );            
        }
    }

    @Test    
    public void testJSF_NoSchemaInstance() throws Exception {
        for ( int version : FacesConfig.SCHEMA_VERSIONS ) {
            parse( jsfWithout(version, HeaderLine.SI, ""),
                   version,
                   "xml.error", "CWWKC2272E" );
        }
    }
    
    @Test
    public void testJSF_NoSchemaLocation() throws Exception {
        for ( int version : FacesConfig.SCHEMA_VERSIONS ) {
            String strVersion = getDottedVersionText(version);
            FacesConfig facesConfig = parse( jsfWithout(version, HeaderLine.SL, ""), version);
            Assert.assertEquals( strVersion, facesConfig.getVersion() );            
        }
    }

    @Test
    public void testJSF_NoXSI() throws Exception {
        for ( int version : FacesConfig.SCHEMA_VERSIONS ) {
            String strVersion = getDottedVersionText(version);
            FacesConfig facesConfig = parse( jsfWithout(version, HeaderLine.XSI, ""), version);
            Assert.assertEquals( strVersion, facesConfig.getVersion() );            
        }
    }

    // When no version is provided, the namespace is used to determine the
    // descriptor version.
    //
    // That yields an ambiguous version for schemas which re-used the namespace:
    // 
    // 1.2, 2.0, 2.1, NAMESPACE_SUN_JAVAEE
    // 2.2, 2.3, NAMESPACE_JCP_JAVAEE
    // 3.0, 4.0, NAMESPACE_JAKARTA
    //
    // The version which is selected is the maximum available which corresponds
    // to the namespace value.

    /**
     * Adjust for version ambiguity when using namespace
     * to assign the descriptor version.
     * 
     * The rule is to assign the maximum supported version which uses
     * the specified namespace.
     * 
     * As a special case, the provisioned version is never less than 2.1.
     * Any namespace version less than 2.1 is directly changed to 2.1.
     * 
     * @param namespaceVersion The version used to select the descriptor namespace.
     * @param provisionedVersion The provisioned version.
     * @return The version which is to be assigned to the descriptor.
     */
    public static int adjustVersion(int namespaceVersion, int provisionedVersion) {
        if ( namespaceVersion < FacesConfig.VERSION_2_1 ) {
            return FacesConfig.VERSION_2_1;

        } else if ( namespaceVersion == FacesConfig.VERSION_2_2 ) {
            if ( provisionedVersion > FacesConfig.VERSION_2_2 ) {
                return FacesConfig.VERSION_2_3;
            } else {
                return FacesConfig.VERSION_2_2;
            }

        } else if ( namespaceVersion == FacesConfig.VERSION_3_0 ) {
            if ( provisionedVersion > FacesConfig.VERSION_3_0 ) {
                return FacesConfig.VERSION_4_0;
            } else {
                return FacesConfig.VERSION_3_0;
            }
        } else {
            return namespaceVersion;
        }
    }

    @Test
    public void testJSF_NoVersion() throws Exception {
        for ( int version : FacesConfig.SCHEMA_BREAKPOINTS ) {
            for ( int maxVersion : FacesConfig.SCHEMA_VERSIONS ) {
                int effectiveMax = maxVersion;
                if ( effectiveMax < FacesConfig.VERSION_2_1 ) {
                    effectiveMax = FacesConfig.VERSION_2_1;
                }

                if ( effectiveMax < version ) {
                    parse( jsfWithout(version, HeaderLine.V, ""),
                           maxVersion,
                           UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                           UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES );

                } else {
                    int expectedVersion = adjustVersion(version, effectiveMax);
                    String strVersion = getDottedVersionText(expectedVersion); 
            
                    FacesConfig facesConfig =
                        parse( jsfWithout(version, HeaderLine.V, ""), maxVersion);
                    Assert.assertEquals( strVersion, facesConfig.getVersion() );
                }
            }
        }
    }

    @Test
    public void testJSF_NamespaceOnly() throws Exception {
        for ( int version : FacesConfig.SCHEMA_BREAKPOINTS ) {
            for ( int maxVersion : FacesConfig.SCHEMA_VERSIONS ) {
                int effectiveMax = maxVersion;
                if ( effectiveMax < FacesConfig.VERSION_2_1 ) {
                    effectiveMax = FacesConfig.VERSION_2_1;
                }

                if ( effectiveMax < version ) {
                    parse( jsfNamespaceOnly(version),
                           maxVersion,
                           UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                           UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES );
                    
                } else {
                    int expectedVersion = adjustVersion(version, effectiveMax);                    
                    String strVersion = getDottedVersionText(expectedVersion); 

                    FacesConfig facesConfig = parse( jsfNamespaceOnly(version), maxVersion);
                    Assert.assertEquals( strVersion, facesConfig.getVersion() );
                }
            }
        }
    }
    
    //

    @Test
    public void testJSF20_VersionMismatch() throws Exception {
        FacesConfig facesConfig = parse(jsf20VersionMismatch, FacesConfig.VERSION_2_3);
        Assert.assertEquals( "2.0", facesConfig.getVersion() );
    }

    @Test
    public void testJSF_NamespaceOnlyUnknown() throws Exception {
        parse(jsfNamespaceOnlyUnknown, FacesConfig.VERSION_2_3,
              UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
              UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }

    @Test
    public void testJSF_VersionOnlyUnknown() throws Exception {
        parse(jsfVersionOnlyUnknown, FacesConfig.VERSION_2_3,
              UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
              UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
