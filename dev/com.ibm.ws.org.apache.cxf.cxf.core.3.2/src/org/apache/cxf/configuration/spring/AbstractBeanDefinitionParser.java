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
package org.apache.cxf.configuration.spring;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.websphere.ras.annotation.Trivial;

import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;

@Trivial
public abstract class AbstractBeanDefinitionParser
    extends org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser {
    public static final String WIRE_BUS_ATTRIBUTE = AbstractBeanDefinitionParser.class.getName() + ".wireBus";
    public static final String WIRE_BUS_NAME = AbstractBeanDefinitionParser.class.getName() + ".wireBusName";
    public static final String WIRE_BUS_CREATE
        = AbstractBeanDefinitionParser.class.getName() + ".wireBusCreate";
    public static final String WIRE_BUS_HANDLER
        = "org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor";
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractBeanDefinitionParser.class);

    private Class<?> beanClass;
    private JAXBContext context;
    private Set<Class<?>> classes;

    public AbstractBeanDefinitionParser() {
    }

    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        boolean setBus = parseAttributes(element, ctx, bean);
        if (!setBus && hasBusProperty()) {
            addBusWiringAttribute(bean, BusWiringType.PROPERTY);
        }
        parseChildElements(element, ctx, bean);
    }

    protected boolean parseAttributes(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        NamedNodeMap atts = element.getAttributes();
        boolean setBus = false;
        for (int i = 0; i < atts.getLength(); i++) {
            Attr node = (Attr) atts.item(i);

            setBus |= parseAttribute(element, node, ctx, bean);
        }
        return setBus;
    }
    protected boolean parseAttribute(Element element, Attr node,
                                     ParserContext ctx, BeanDefinitionBuilder bean) {
        String val = node.getValue();
        String pre = node.getPrefix();
        String name = node.getLocalName();
        String prefix = node.getPrefix();

        // Don't process namespaces
        if (isNamespace(name, prefix)) {
            return false;
        }

        if ("createdFromAPI".equals(name)) {
            bean.setAbstract(true);
        } else if ("abstract".equals(name)) {
            bean.setAbstract(true);
        } else if ("depends-on".equals(name)) {
            bean.addDependsOn(val);
        } else if ("name".equals(name)) {
            processNameAttribute(element, ctx, bean, val);
        } else if ("bus".equals(name)) {
            return processBusAttribute(element, ctx, bean, val);
        } else if (!"id".equals(name) && isAttribute(pre, name)) {
            mapAttribute(bean, element, name, val);
        }
        return false;
    }


    protected boolean processBusAttribute(Element element, ParserContext ctx,
                                        BeanDefinitionBuilder bean,
                                        String val) {
        if (val != null && val.trim().length() > 0) {
            if (ctx.getRegistry().containsBeanDefinition(val)) {
                bean.addPropertyReference("bus", val);
            } else {
                addBusWiringAttribute(bean, BusWiringType.PROPERTY,
                                      val, ctx);
            }
            return true;
        }
        return false;
    }

    protected void processNameAttribute(Element element,
                                        ParserContext ctx,
                                        BeanDefinitionBuilder bean,
                                        String val) {
        //nothing
    }

    private boolean isNamespace(String name, String prefix) {
        return "xmlns".equals(prefix) || prefix == null && "xmlns".equals(name);
    }

    protected void parseChildElements(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        Element el = DOMUtils.getFirstElement(element);
        while (el != null) {
            String name = el.getLocalName();
            mapElement(ctx, bean, el, name);
            el = DOMUtils.getNextElement(el);
        }
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    protected Class<?> getBeanClass(Element e) {
        return beanClass;
    }

    protected void mapAttribute(BeanDefinitionBuilder bean, Element e, String name, String val) {
        mapAttribute(bean, name, val);
    }

    protected void mapAttribute(BeanDefinitionBuilder bean, String name, String val) {
        mapToProperty(bean, name, val);
    }

    protected void mapElement(ParserContext ctx, BeanDefinitionBuilder bean, Element e, String name) {
    }

    @Override
    protected String resolveId(Element elem, AbstractBeanDefinition definition, ParserContext ctx) {

        // REVISIT: use getAttributeNS instead

        String id = getIdOrName(elem);
        String createdFromAPI = elem.getAttribute("createdFromAPI");

        if (null == id || id.isEmpty()) {
            return super.resolveId(elem, definition, ctx);
        }

        if (createdFromAPI != null && Boolean.parseBoolean(createdFromAPI)) {
            return id + getSuffix();
        }
        return id;
    }

    protected boolean hasBusProperty() {
        return false;
    }

    protected String getSuffix() {
        return "";
    }

    protected void setFirstChildAsProperty(Element element, ParserContext ctx,
                                         BeanDefinitionBuilder bean, String propertyName) {

        Element first = getFirstChild(element);

        if (first == null) {
            throw new IllegalStateException(propertyName + " property must have child elements!");
        }

        String id;
        BeanDefinition child;
        if (first.getNamespaceURI().equals(BeanDefinitionParserDelegate.BEANS_NAMESPACE_URI)) {
            String name = first.getLocalName();
            if ("ref".equals(name)) {
                id = first.getAttribute("bean");
                if (id == null) {
                    throw new IllegalStateException("<ref> elements must have a \"bean\" attribute!");
                }
                bean.addPropertyReference(propertyName, id);
                return;
            } else if ("bean".equals(name)) {
                BeanDefinitionHolder bdh = ctx.getDelegate().parseBeanDefinitionElement(first);
                child = bdh.getBeanDefinition();
                bean.addPropertyValue(propertyName, child);
                return;
            } else {
                throw new UnsupportedOperationException("Elements with the name " + name
                                                        + " are not currently "
                                                        + "supported as sub elements of "
                                                        + element.getLocalName());
            }
        }
        child = ctx.getDelegate().parseCustomElement(first, bean.getBeanDefinition());
        bean.addPropertyValue(propertyName, child);
    }

    protected Element getFirstChild(Element element) {
        return DOMUtils.getFirstElement(element);
    }

    protected void addBusWiringAttribute(BeanDefinitionBuilder bean,
                                         BusWiringType type) {
        addBusWiringAttribute(bean, type, null, null);
    }

    protected void addBusWiringAttribute(BeanDefinitionBuilder bean,
                                         BusWiringType type,
                                         String busName,
                                         ParserContext ctx) {
        LOG.fine("Adding " + WIRE_BUS_ATTRIBUTE + " attribute " + type + " to bean " + bean);
        bean.getRawBeanDefinition().setAttribute(WIRE_BUS_ATTRIBUTE, type);
        if (!StringUtils.isEmpty(busName)) {
            bean.getRawBeanDefinition().setAttribute(WIRE_BUS_NAME,
                    busName.charAt(0) == '#' ? busName.substring(1) : busName);
        }

        if (ctx != null
            && !ctx.getRegistry().containsBeanDefinition(WIRE_BUS_HANDLER)) {
            BeanDefinitionBuilder b
                = BeanDefinitionBuilder.rootBeanDefinition(WIRE_BUS_HANDLER);
            ctx.getRegistry().registerBeanDefinition(WIRE_BUS_HANDLER, b.getBeanDefinition());
        }
    }

    protected void mapElementToJaxbProperty(Element parent,
                                            BeanDefinitionBuilder bean,
                                            QName name,
                                            String propertyName) {
        mapElementToJaxbProperty(parent, bean, name, propertyName, null);
    }

    protected void mapElementToJaxbProperty(Element parent,
                                            BeanDefinitionBuilder bean,
                                            QName name,
                                            String propertyName,
                                            Class<?> c) {
        Element data = null;

        Node node = parent.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && name.getLocalPart().equals(node.getLocalName())
                && name.getNamespaceURI().equals(node.getNamespaceURI())) {
                data = (Element)node;
                break;
            }
            node = node.getNextSibling();
        }

        if (data == null) {
            return;
        }
        mapElementToJaxbProperty(data, bean, propertyName, c);
    }

    private synchronized JAXBContext getContext(Class<?> cls) {
        if (context == null || classes == null || !classes.contains(cls)) {
            try {
                Set<Class<?>> tmp = new HashSet<>();
                if (classes != null) {
                    tmp.addAll(classes);
                }
                JAXBContextCache.addPackage(tmp, getJaxbPackage(),
                                            cls == null
                                            ? getClass().getClassLoader()
                                                : cls.getClassLoader());
                if (cls != null) {
                    boolean hasOf = false;
                    for (Class<?> c : tmp) {
                        if (c.getPackage() == cls.getPackage()
                            && "ObjectFactory".equals(c.getSimpleName())) {
                            hasOf = true;
                        }
                    }
                    if (!hasOf) {
                        tmp.add(cls);
                    }
                }
                JAXBContextCache.scanPackages(tmp);
                CachedContextAndSchemas ccs
                    = JAXBContextCache.getCachedContextAndSchemas(tmp, null, null, null, false);
                classes = ccs.getClasses();
                context = ccs.getContext();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return context;
    }

    protected void mapElementToJaxbProperty(Element data,
                                            BeanDefinitionBuilder bean,
                                            String propertyName,
                                            Class<?> c) {
        try {
            XMLStreamWriter xmlWriter = null;
            Unmarshaller u = null;
            try {
                StringWriter writer = new StringWriter();
                xmlWriter = StaxUtils.createXMLStreamWriter(writer);
                StaxUtils.copy(data, xmlWriter);
                xmlWriter.flush();

                BeanDefinitionBuilder jaxbbean
                    = BeanDefinitionBuilder.rootBeanDefinition(JAXBBeanFactory.class);
                jaxbbean.getRawBeanDefinition().setFactoryMethodName("createJAXBBean");
                jaxbbean.addConstructorArgValue(getContext(c));
                jaxbbean.addConstructorArgValue(writer.toString());
                jaxbbean.addConstructorArgValue(c);
                bean.addPropertyValue(propertyName, jaxbbean.getBeanDefinition());
            } catch (Exception ex) {
                u = getContext(c).createUnmarshaller();
                u.setEventHandler(null);
                Object obj;
                if (c != null) {
                    obj = u.unmarshal(data, c);
                } else {
                    obj = u.unmarshal(data);
                }
                if (obj instanceof JAXBElement<?>) {
                    JAXBElement<?> el = (JAXBElement<?>)obj;
                    obj = el.getValue();
                }
                if (obj != null) {
                    bean.addPropertyValue(propertyName, obj);
                }
            } finally {
                StaxUtils.close(xmlWriter);
                JAXBUtils.closeUnmarshaller(u);
            }
        } catch (JAXBException e) {
            throw new RuntimeException("Could not parse configuration.", e);
        }
    }


    public void mapElementToJaxbPropertyFactory(Element data,
                                                BeanDefinitionBuilder bean,
                                                String propertyName,
                                                Class<?> type,
                                                Class<?> factory,
                                                String method,
                                                Object ... args) {
        bean.addPropertyValue(propertyName, mapElementToJaxbBean(data,
                                                                 factory,
                                                                 null, type, method, args));
    }
    public AbstractBeanDefinition mapElementToJaxbBean(Element data,
                                                       Class<?> cls,
                                                      Class<?> factory,
                                                      String method,
                                                      Object ... args) {
        return mapElementToJaxbBean(data, cls, factory, cls, method, args);
    }

    public AbstractBeanDefinition mapElementToJaxbBean(Element data,
                                                       Class<?> cls,
                                                      Class<?> factory,
                                                      Class<?> jaxbClass,
                                                      String method,
                                                      Object ... args) {
        StringWriter writer = new StringWriter();
        XMLStreamWriter xmlWriter = StaxUtils.createXMLStreamWriter(writer);
        try {
            StaxUtils.copy(data, xmlWriter);
            xmlWriter.flush();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } finally {
            StaxUtils.close(xmlWriter);
        }

        BeanDefinitionBuilder jaxbbean
            = BeanDefinitionBuilder.rootBeanDefinition(cls);
        if (factory != null) {
            jaxbbean.getRawBeanDefinition().setFactoryBeanName(factory.getName());
        }
        jaxbbean.getRawBeanDefinition().setFactoryMethodName(method);
        jaxbbean.addConstructorArgValue(writer.toString());
        jaxbbean.addConstructorArgValue(getContext(jaxbClass));
        if (args != null) {
            for (Object o : args) {
                jaxbbean.addConstructorArgValue(o);
            }
        }
        return jaxbbean.getBeanDefinition();
    }

    protected static <T> T unmarshalFactoryString(String s, JAXBContext ctx, Class<T> cls) {
        StringReader reader = new StringReader(s);
        XMLStreamReader data = StaxUtils.createXMLStreamReader(reader);
        Unmarshaller u = null;
        try {
            u = ctx.createUnmarshaller();
            JAXBElement<?> obj = u.unmarshal(data, cls);
            return cls.cast(obj.getValue());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                StaxUtils.close(data);
            } catch (XMLStreamException ex) {
                throw new RuntimeException(ex);
            }
            JAXBUtils.closeUnmarshaller(u);
        }
    }

    protected String getJaxbPackage() {
        return "";
    }

    protected void mapToProperty(BeanDefinitionBuilder bean, String propertyName, String val) {
        if (ID_ATTRIBUTE.equals(propertyName)) {
            return;
        }

        if (!StringUtils.isEmpty(val)) {
            if (val.startsWith("#")) {
                bean.addPropertyReference(propertyName, val.substring(1));
            } else {
                bean.addPropertyValue(propertyName, val);
            }
        }
    }

    protected boolean isAttribute(String pre, String name) {
        return !"xmlns".equals(name) && (pre == null || !"xmlns".equals(pre))
            && !"abstract".equals(name) && !"lazy-init".equals(name) && !"id".equals(name);
    }

    protected QName parseQName(Element element, String t) {
        if (t.startsWith("{")) {
            int i = t.indexOf('}');
            if (i == -1) {
                throw new RuntimeException("Namespace bracket '{' must having a closing bracket '}'.");
            }

            t = t.substring(i + 1);
        }

        final String local;
        final String pre;
        final String ns;
        int colIdx = t.indexOf(':');
        if (colIdx == -1) {
            local = t;
            pre = "";

            ns = DOMUtils.getNamespace(element, "");
        } else {
            pre = t.substring(0, colIdx);
            local = t.substring(colIdx + 1);

            ns = DOMUtils.getNamespace(element, pre);
        }

        return new QName(ns, local, pre);
    }

    /* This id-or-name resolution logic follows that in Spring's
     * org.springframework.beans.factory.xml.BeanDefinitionParserDelegate object
     * Intent is to have resolution of CXF custom beans follow that of Spring beans
     */
    protected String getIdOrName(Element elem) {
        String id = elem.getAttribute(BeanDefinitionParserDelegate.ID_ATTRIBUTE);

        if (null == id || "".equals(id)) {
            String names = elem.getAttribute("name");
            if (null != names) {
                StringTokenizer st =
                    new StringTokenizer(names, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
                if (st.countTokens() > 0) {
                    id = st.nextToken();
                }
            }
        }
        return id;
    }

}
