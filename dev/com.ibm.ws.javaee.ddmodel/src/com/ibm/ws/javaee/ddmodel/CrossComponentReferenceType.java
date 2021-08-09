/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

public class CrossComponentReferenceType extends DDParser.ElementContentParsable {
    public <R> R resolveReferent(DDParser parser, Class<R> referentClass) throws ParseException {
        String hrefString = href.getValue();
        int hashIndex = hrefString.indexOf('#');
        String path = hrefString.substring(0, hashIndex);
        String id = hrefString.substring(hashIndex + 1);

        // XMI processing would read the document with the specified path, but
        // this processing is more strict: assume the container only has one
        // document of the specified type, and then ensure that its path matches
        // what was specified in the href.
        DeploymentDescriptor target = (DeploymentDescriptor) parser.adaptRootContainer(adaptTarget);
        if (!target.getDeploymentDescriptorPath().equals(path)) {
            throw new ParseException(parser.invalidHRefPrefix(hrefElementName, target.getDeploymentDescriptorPath() + '#'));
        }

        Object component = target.getComponentForId(id);
        return referentClass.cast(component);
    }

    public String getReferenceString() {
        return href != null ? href.getValue() : null;
    }

    // attrs
    protected StringType href;

    private final String hrefElementName;
    private final Class<?> adaptTarget;

    public CrossComponentReferenceType(String hrefElementName, Class<?> adaptTarget) {
        this.hrefElementName = hrefElementName;
        this.adaptTarget = adaptTarget;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (nsURI == null) {
            if ("href".equals(localName)) {
                href = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        if ("http://www.omg.org/XMI".equals(nsURI)) {
            if ("type".equals(localName)) {
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

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("href", href);
    }
}