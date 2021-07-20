/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public abstract class DDParserSpec extends DDParser {
    public DDParserSpec(
        Container ddRootContainer, Entry ddEntry,
        String expectedRootName) throws ParseException {

        this(ddRootContainer, ddEntry,
             DDParser.UNUSED_MAX_SCHEMA_VERSION,
             !TRIM_SIMPLE_CONTENT,
             expectedRootName);
    }

    public DDParserSpec(
            Container ddRootContainer, Entry ddEntry,
            int maxSchemaVersion,
            String expectedRootName) throws ParseException {

        this(ddRootContainer, ddEntry,
             maxSchemaVersion,
             !TRIM_SIMPLE_CONTENT,
             expectedRootName);
    }
    
    public static final boolean TRIM_SIMPLE_CONTENT = true;    

    public DDParserSpec(
            Container ddRootContainer, Entry ddEntry,
            int maxSchemaVersion,
            boolean trimSimpleContent,
            String expectedRootName) throws ParseException {

        super(ddRootContainer, ddEntry,
              maxSchemaVersion,
              expectedRootName);
        
        this.trimSimpleContent = trimSimpleContent; 
    }

    private final boolean trimSimpleContent;
    
    /**
     * Answer the accumulated content, conditionally
     * trimming whitespace.  Reset the content accumulator.
     *
     * If trimming is enabled for this type, per the
     * constructor parameter, and if trimming is requested,
     * trim the accumulated content.
     * 
     * @param untrimmed Control parameter: Tell if trimming
     *    (if enabled for this type), is to not be performed.
     * @return The accumulated content.
     */
    @Trivial
    public String getContentString(boolean untrimmed) {
        String content = getContentString();
        // If trimming has not been turned off,
        // and if trimming is usually to be performed,
        // then trim the content.
        if ( !untrimmed && trimSimpleContent ) {
            content = content.trim();
        }
        return content;
    }
    
    protected abstract VersionData[] getVersionData();
    protected abstract ParsableElement createRootElement();

    protected ParsableElement createRootParsable() throws ParseException {
        validateRootElementName();

        String versionAttr = getAttributeValue("", "version");

        // Need either a version, or a namespace, or a public ID.

        if ( versionAttr == null ) { 
            if ( namespace == null ) {
                if ( dtdPublicId == null ) {
                    throw new ParseException( missingDescriptorVersion() );
                }
            }
        }

        VersionData versionData = selectVersion( getVersionData(),
                versionAttr, dtdPublicId, namespace,
                maxVersion );
        
        if ( versionData == null ) {
            // Version has priority, next namespace, and finally public ID.
            // One of these must be set, per the prior test.
            if ( versionAttr != null ) {
                throw new ParseException( unsupportedDescriptorVersion(versionAttr) );
            } else if ( namespace != null ) {
                throw new ParseException( unsupportedDescriptorNamespace(namespace) );
            } else { // ( dtdPublicId != null )
                throw new ParseException( unsupportedDescriptorPublicId(dtdPublicId) );
            }
        }            

        // Allow the selection of the version even if it is not provisioned.
        // This is slightly improper, as it changes an 'unsupported' exception
        // into an 'unprovisioned' exception across releases, as the product adds
        // support for new specifications.

        if ( versionData.version > maxVersion ) {
            throw new ParseException( unprovisionedDescriptorVersion(versionData.version, maxVersion) );
        }

        // Version has precedence over namespace.  That creates a possibility
        // of the namespace not matching the version.
        //
        // In either case of a namespace mis-match, or the namespace being
        // entirely absent, patch in the correct namespace.

        if ( namespace != null ) {
            if ( versionAttr != null ) {
                if ( !namespace.equals(versionData.namespace) ) {
                    warning( incorrectDescriptorNamespace(versionAttr, namespace, versionData.namespace) );
                    patchNamespace(versionData.namespace);                
                }
            }
        } else {
            if ( versionData.namespace != null ) {
                patchNamespace(versionData.namespace);
            }
        }

        // Note that parsing will reassign the version upon parsing
        // the 'version' attribute of the header.
        version = versionData.version;
        eePlatformVersion = versionData.platformVersion;

        return createRootElement();
    }  
}
