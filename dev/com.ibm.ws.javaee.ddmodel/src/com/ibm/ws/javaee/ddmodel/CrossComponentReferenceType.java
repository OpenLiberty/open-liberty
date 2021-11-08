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

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/**
 * Type used to encode bindings and extensions references to
 * primary descriptors.
 * 
 * Bindings and extensions references are always to elements of
 * the primary deployment descriptor.  Any HREF value which is
 * processed by {@link #resolveReferent} must have a prefix which
 * matches the path of the primary deployment descriptor.  The
 * HREF prefix is not actually used in the normal fashion to locate
 * a referenced document.
 */
public class CrossComponentReferenceType extends DDParser.ElementContentParsable {
    /**
     * Create an object used to deserialize a cross-component
     * reference.
     * 
     * @param hrefElementName The name of the element which is being
     *     deserialized.
     * @param primaryDDType The type of the primary descriptor which must
     *     be the referent of the HREF.
     */
    public CrossComponentReferenceType(String hrefElementName, Class<?> primaryDDType) {
        this.hrefElementName = hrefElementName;
        this.primaryDDType = primaryDDType;
    }

    /** The name of the element which is being processed. */
    private final String hrefElementName;
    
    /** The type of the primary descriptor which must be referenced. */
    private final Class<?> primaryDDType;

    //

    /** An HREF value which was parsed out of the element which is being processed. */
    protected StringType href;

    public String getReferenceString() {
        return ( (href != null) ? href.getValue() : null );
    }

    //

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if ( nsURI == null ) {
            if ( "href".equals(localName) ) {
                href = parser.parseStringAttributeValue(index);
                return true;
            }
        }

        if ( "http://www.omg.org/XMI".equals(nsURI) ) {
            if ( "type".equals(localName) ) {
                // Allowed but unused.  In theory, we should require the target
                // element to be of the specified type, but it's not worth the
                // effort to implement, so just assume the .xmi is well-formed.
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        return false;
    }

    // @Override
    // public void finish(DDParser parser) throws ParseException {
        // Should 'href' be validated here, instead of when resolving?
        // See the 'href == null' block in 'resolveReferent.
    // }

    //

    public <R> R resolveReferent(DDParser parser, Class<R> referentClass) throws ParseException {
        if ( href == null ) {
            // This is a new warning: Previously, a null pointer exception would occur.
            parser.error( parser.missingHRef(hrefElementName) );
            return null;
        }
        String hrefValue = href.getValue();

        int hashIndex = hrefValue.indexOf('#');
        if ( (hashIndex == -1) ||
             (hashIndex == 0) || (hashIndex == hrefValue.length() - 1) ) {
            // This is a new warning: Previously, an index out of bounds exception would occur.
            parser.fatal( parser.invalidHRef(hrefElementName, hrefValue) );
            return null;
        }
        String hrefPath = hrefValue.substring(0, hashIndex);
        String hrefId = hrefValue.substring(hashIndex + 1);

        // When processing an XMI bindings or extensions, the reference must
        // be to an element of the primary descriptor.  Use the supplied
        // DD type to locate the primary descriptor.  Do not use the HREF path,
        // except to validate the HREF value.

        DeploymentDescriptor primaryDD = (DeploymentDescriptor)
            parser.adaptRootContainer(primaryDDType);
        String primaryDDPath = primaryDD.getDeploymentDescriptorPath();

        if ( !primaryDDPath.equals(hrefPath) ) {
            // This error was generated before.  Although, it a strange
            // case to check for, since it is very very unlikely.
            parser.fatal( parser.invalidHRefPrefix(hrefElementName, hrefValue, hrefPath, primaryDDPath) );
            return null;
        }

        Object primaryDDElement = primaryDD.getComponentForId(hrefId);
        if ( primaryDDElement == null ) {
            // New warning.
            // This warning is disabled pending resolution of issue 19207.
            // parser.warning( parser.unresolvedReference(hrefElementName, hrefValue, hrefId, hrefPath) );
            return null;
        }

        try {
            return referentClass.cast(primaryDDElement);
        } catch ( ClassCastException e ) {
            // This is a new error.  Previously, a class cast exception would occur.
            parser.fatal( parser.incorrectHRefType(hrefElementName, hrefValue, referentClass, primaryDDElement) );
            return null;
        }
    }

    //

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("href", href);
    }
}