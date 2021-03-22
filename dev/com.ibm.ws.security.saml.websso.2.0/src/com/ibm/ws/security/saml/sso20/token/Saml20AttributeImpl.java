/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.token;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;

import net.shibboleth.utilities.java.support.xml.XMLParserException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.saml2.Saml20Attribute;
import com.ibm.ws.security.saml.sso20.metadata.TraceConstants;

import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;

/**
 *
 */
public class Saml20AttributeImpl implements Saml20Attribute {

    private static final long serialVersionUID = -862850937499495720L;

    private transient static TraceComponent tc = Tr.register(Saml20AttributeImpl.class,
                                                             TraceConstants.TRACE_GROUP,
                                                             TraceConstants.MESSAGE_BUNDLE);

    String seializedAttribute = null;
    transient Attribute attribute = null;
    List<QName> namespaces = new ArrayList<QName>();
    QName schemaType = null;
    List<String> stringValues = new ArrayList<String>();
    List<String> simpleStringValues = new ArrayList<String>();
    String name = null;
    String friendlyName = null;
    String format = null;

    public Saml20AttributeImpl(Attribute attribute) {
        attribute.detach();
        this.attribute = attribute;
        //this.seializedAttribute = XMLHelper.nodeToString(attribute.getDOM());
        this.seializedAttribute = SerializeSupport.nodeToString(attribute.getDOM());//v3
        init();
    }

    public Saml20AttributeImpl(String attribute) {

        this.seializedAttribute = attribute;
        deserialize();
        init();
    }

    protected void deserialize() {

        //StaticBasicParserPool ppMgr = (StaticBasicParserPool) Configuration.getParserPool();
        ParserPool ppMgr = XMLObjectProviderRegistrySupport.getParserPool();//v3
        try {
            StringReader reader = new StringReader(this.seializedAttribute);
            this.attribute = (Attribute) XMLObjectSupport.unmarshallFromReader(ppMgr, reader);

        } catch (XMLParserException xpe) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Fail to deserialize Attribute", xpe.toString());
            }
        } catch (UnmarshallingException ue) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Fail to deserialize Attribute", ue.toString());
            }
        }
    }

    protected void init() {
        this.name = this.attribute.getName();
        this.format = this.attribute.getNameFormat();
        this.friendlyName = this.attribute.getFriendlyName();

        initSchemaType();
        initNameSpaces();
        serializeValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.security.saml.token.Saml20Attribute#getNameSpaces()
     */
    @Override
    public List<QName> getNameSpaces() {

        return Collections.unmodifiableList(this.namespaces);
    }

    protected void initNameSpaces() {
        // TODO Auto-generated method stub

        Set<Namespace> nsset = this.attribute.getNamespaces();
        if (!nsset.isEmpty()) {
            Iterator<Namespace> it = nsset.iterator();
            while (it.hasNext()) {
                QName qn;
                Namespace ns = it.next();
                String uri = ns.getNamespaceURI();
                String prefix = ns.getNamespacePrefix();
                if (prefix == null) {
                    qn = new QName(uri, "");
                }
                else {
                    qn = new QName(uri, "", prefix);
                }
                this.namespaces.add(qn);
            }
        }
    }

    @Override
    public QName getSchemaType() {

        return this.schemaType;
    }

    protected void initSchemaType() {
        List<XMLObject> attributevalues = this.attribute.getAttributeValues();
        Iterator<XMLObject> it = attributevalues.listIterator();
        while (it.hasNext()) {
            XMLObject xml = it.next(); //AttributeValue
            this.schemaType = xml.getSchemaType();
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.security.saml.token.Saml20Attribute#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.security.saml.token.Saml20Attribute#getNameFormat()
     */
    @Override
    public String getNameFormat() {

        return this.format;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.security.saml.token.Saml20Attribute#getFriendlyName()
     */
    @Override
    public String getFriendlyName() {

        return this.friendlyName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.security.saml.token.Saml20Attribute#getValueString()
     */
    @Override
    public List<String> getValuesAsString() {
        return Collections.unmodifiableList(this.simpleStringValues);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.security.saml.token.Saml20Attribute#getSerializedValue()
     */
    @Override
    public List<String> getSerializedValues() {

        return Collections.unmodifiableList(this.stringValues);
    }

    protected void serializeValue() {

        List<XMLObject> valueXmls = this.attribute.getAttributeValues();
        Iterator<XMLObject> it = valueXmls.iterator();
        while (it.hasNext()) {
            XMLObject valueXml = it.next();
            //String stringValue = XMLHelper.nodeToString(valueXml.getDOM());
            String stringValue = SerializeSupport.nodeToString(valueXml.getDOM());//v3
            this.stringValues.add(stringValue);
            this.simpleStringValues.add(valueXml.getDOM().getTextContent());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.security.saml.token.Saml20Attribute#getSerializedAttribute()
     */
    @Override
    public String getSerializedAttribute() {
        // TODO Auto-generated method stub
        return seializedAttribute;
    }

    @Override
    public String toString() {
        String result = "Attribute\n name:" + this.name +
                        "\n format:" + this.format +
                        "\n friendlyName:" + this.friendlyName +
                        "\n schemaType:" + this.schemaType +
                        "\n value:" + this.stringValues;
        return result;
    }

}
