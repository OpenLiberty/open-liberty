/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.service.model;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaElement;

public final class MessagePartInfo extends AbstractPropertiesHolder implements NamedItem {

    private QName pname;
    private AbstractMessageContainer mInfo;

    private boolean isElement;
    private QName typeName;
    private QName elementName;
    private QName concreteName;
    private XmlSchemaAnnotated xmlSchema;
    private Class<?> typeClass;
    private int index;

    public MessagePartInfo(QName n, AbstractMessageContainer info) {
        mInfo = info;
        pname = n;
    }

    public void setMessageContainer(AbstractMessageContainer info) {
        mInfo = info;
    }

    /**
     * @return Returns the name.
     */
    public QName getName() {
        return pname;
    }
    /**
     * @param n The name to set.
     */
    public void setName(QName n) {
        pname = n;
    }

    public QName getConcreteName() {
        return concreteName;
    }

    public void setConcreteName(QName concreteName) {
        this.concreteName = concreteName;
    }

    public boolean isElement() {
        return isElement;
    }
    public void setElement(boolean b) {
        isElement = b;
    }

    public QName getElementQName() {
        if (isElement) {
            return elementName;
        }
        return null;
    }
    public QName getTypeQName() {
        if (!isElement) {
            return typeName;
        } else if (xmlSchema instanceof XmlSchemaElement) {
            return ((XmlSchemaElement)xmlSchema).getSchemaTypeName();
        }
        return null;
    }
    public void setTypeQName(QName qn) {
        isElement = false;
        if (concreteName == null) {
            concreteName = new QName(null, pname.getLocalPart());
        }
        typeName = qn;
    }
    public void setElementQName(QName qn) {
        isElement = true;
        elementName = qn;
        concreteName = qn;
    }

    public AbstractMessageContainer getMessageInfo() {
        return mInfo;
    }

    public XmlSchemaAnnotated getXmlSchema() {
        return xmlSchema;
    }

    public void setXmlSchema(XmlSchemaAnnotated xmlSchema) {
        this.xmlSchema = xmlSchema;
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(Class<?> typeClass) {
        this.typeClass = typeClass;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return new StringBuilder("[MessagePartInfo name=")
            .append(getName())
            .append(", ConcreteName=")
            .append(getConcreteName()).toString();
    }

    public int hashCode() {
        return pname == null ? -1 : pname.hashCode();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MessagePartInfo)) {
            return false;
        }
        MessagePartInfo oi = (MessagePartInfo)o;
        return equals(pname, oi.pname)
            && isElement == oi.isElement
            && equals(typeName, oi.typeName)
            && equals(elementName, oi.elementName)
            && equals(concreteName, oi.concreteName)
            && equals(typeClass, oi.typeClass);
    }


}
