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
package com.ibm.ws.javaee.ddmodel.common;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.LifecycleCallback;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:group name="jndiEnvironmentRefsGroup">
 <xsd:sequence>
 <xsd:element name="env-entry"
 type="javaee:env-entryType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="ejb-ref"
 type="javaee:ejb-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="ejb-local-ref"
 type="javaee:ejb-local-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:group ref="javaee:service-refGroup"/>
 <xsd:element name="resource-ref"
 type="javaee:resource-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="resource-env-ref"
 type="javaee:resource-env-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="message-destination-ref"
 type="javaee:message-destination-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="persistence-context-ref"
 type="javaee:persistence-context-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="persistence-unit-ref"
 type="javaee:persistence-unit-refType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="post-construct"
 type="javaee:lifecycle-callbackType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="pre-destroy"
 type="javaee:lifecycle-callbackType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="data-source"
 type="javaee:data-sourceType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 </xsd:sequence>
 </xsd:group>
 */

public class JNDIEnvironmentRefsGroup extends JNDIEnvironmentRefs implements com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefsGroup {

    @Override
    public List<LifecycleCallback> getPostConstruct() {
        if (post_construct != null) {
            return post_construct.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<LifecycleCallback> getPreDestroy() {
        if (pre_destroy != null) {
            return pre_destroy.getList();
        } else {
            return Collections.emptyList();
        }
    }

    LifecycleCallbackType.ListType post_construct;
    LifecycleCallbackType.ListType pre_destroy;

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("post-construct".equals(localName)) {
            LifecycleCallbackType post_construct = new LifecycleCallbackType();
            parser.parse(post_construct);
            addPostConstruct(post_construct);
            return true;
        }
        if ("pre-destroy".equals(localName)) {
            LifecycleCallbackType pre_destroy = new LifecycleCallbackType();
            parser.parse(pre_destroy);
            addPreDestroy(pre_destroy);
            return true;
        }
        return false;
    }

    private void addPostConstruct(LifecycleCallbackType post_construct) {
        if (this.post_construct == null) {
            this.post_construct = new LifecycleCallbackType.ListType();
        }
        this.post_construct.add(post_construct);
    }

    private void addPreDestroy(LifecycleCallbackType pre_destroy) {
        if (this.pre_destroy == null) {
            this.pre_destroy = new LifecycleCallbackType.ListType();
        }
        this.pre_destroy.add(pre_destroy);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describeIfSet("post-construct", post_construct);
        diag.describeIfSet("pre-destroy", pre_destroy);
    }
}
