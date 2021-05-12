/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.DesignatedXMLInputFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public abstract class DDParser {
    private static final TraceComponent tc = Tr.register(DDParser.class);

    public static class ParseException extends Exception {
        /**  */
        private static final long serialVersionUID = -2437543890927284677L;

        public ParseException(String translatedMessage) {
            super(translatedMessage);
        }

        public ParseException(String translatedMessage, Throwable t) {
            super(translatedMessage, t);
        }
    }

    public interface RootParsable {
        public void describe(StringBuilder sb);
    }

    public interface Parsable {
        public void describe(Diagnostics diag);
    }

    public interface ParsableElement extends Parsable {

        public void setNil(boolean nilled);

        public boolean isNil();

        public boolean isIdAllowed();

        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException;

        public boolean handleChild(DDParser parser, String localName) throws ParseException;

        public boolean handleContent(DDParser parser) throws ParseException;

        public void finish(DDParser parser) throws ParseException;
    }

    public static abstract class ElementContentParsable implements ParsableElement {
        private boolean nilled;

        @Trivial
        @Override
        public final void setNil(boolean nilled) {
            this.nilled = nilled;
        }

        @Trivial
        @Override
        public final boolean isNil() {
            return nilled;
        }

        @Trivial
        @Override
        public boolean isIdAllowed() {
            return false;
        }

        @Trivial
        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
            return false;
        }

        @Trivial
        @Override
        public boolean handleContent(DDParser parser) throws ParseException {
            return parser.isWhiteSpace();
        }

        @Trivial
        @Override
        public void finish(DDParser parser) throws ParseException {
            return; // nothing to do for element content
        }

        @Trivial
        protected String toTracingSafeString() {
            return super.toString();
        }

        @Trivial
        @Override
        public final String toString() {
            return toTracingSafeString();
        }
    }

    public static class ParsableList<T extends ParsableElement> implements Parsable, Iterable<T> {
        protected List<T> list = new ArrayList<T>();

        public T newInstance(DDParser parser) {
        	throw new UnsupportedOperationException();
        }

        @Trivial
        @SuppressWarnings("unchecked")
        public void addElement(ParsableElement element) {
            add((T) element);
        }

        @Trivial
        public void add(T t) {
            list.add(t);
        }

        @Trivial
        @Override
        public Iterator<T> iterator() {
            return list.iterator();
        }

        @Trivial
        @Override
        public void describe(Diagnostics diag) {
            String prefix = "";
            for (T t : list) {
                diag.sb.append(prefix);
                diag.describeWithIdIfSet(t);
                prefix = ",";
            }
        }
    }

    public static class ParsableListImplements<T extends ParsableElement, I> extends ParsableList<T> {
        @Trivial
        @SuppressWarnings("unchecked")
        public List<I> getList() {
            return (List<I>) list;
        }
    }

    public static class ComponentIDMap {
        public static final Object DUPLICATE = new Object();

        private final Map<String, Object> idToComponentMap = new HashMap<String, Object>();

        @Trivial
        Object get(String id) {
            return idToComponentMap.get(id);
        }

        @Trivial
        Object put(String id, Object ddComponent) {
            return idToComponentMap.put(id, ddComponent);
        }

        @Trivial
        public Object getComponentForId(String id) {
            Object comp = idToComponentMap.get(id);
            return comp != DUPLICATE ? comp : null;
        }

        // Mark as trivial to turn off logging as a fix for defect 53155
        @Trivial
        public String getIdForComponent(Object ddComponent) {
            for (Map.Entry<String, Object> entry : idToComponentMap.entrySet()) {
                if (entry.getValue() == ddComponent) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }

    public static class Diagnostics {
        private final ComponentIDMap idMap;
        private final StringBuilder sb;

        @Trivial
        public Diagnostics(ComponentIDMap idMap) {
            this(idMap, new StringBuilder());
        }

        @Trivial
        public Diagnostics(ComponentIDMap idMap, StringBuilder sb) {
            this.idMap = idMap;
            this.sb = sb;
        }

        @Trivial
        void describeWithIdIfSet(Parsable parsable) {
            String id = idMap != null ? idMap.getIdForComponent(parsable) : null;
            if (id != null) {
                sb.append("[id<\"" + id + "\">]");
            }
            parsable.describe(this);
        }

        @Trivial
        public <T> void describeEnum(T enumValue) {
            if (enumValue != null) {
                sb.append(enumValue);
            }
        }

        @Trivial
        public <T> void describeEnum(String name, T enumValue) {
            sb.append(name + "<");
            if (enumValue != null) {
                sb.append(enumValue);
            } else {
                sb.append("null");
            }
            sb.append(">");
        }

        @Trivial
        public <T> void describeEnumIfSet(String name, T enumValue) {
            if (enumValue != null) {
                sb.append("[" + name + "<");
                sb.append(enumValue);
                sb.append(">]");
            }
        }

        @Trivial
        public void describe(String name, ParsableElement parsable) {
            sb.append(name + "<");
            if (parsable != null) {
                describeWithIdIfSet(parsable);
            } else {
                sb.append("null");
            }
            sb.append(">");
        }

        @Trivial
        public void describe(String name, ParsableList<? extends ParsableElement> parsableList) {
            sb.append(name + "(");
            if (parsableList != null) {
                parsableList.describe(this);
            } else {
                sb.append("null");
            }
            sb.append(")");
        }

        @Trivial
        public void describeIfSet(String name, ParsableElement parsable) {
            if (parsable != null) {
                sb.append("[" + name + "<");
                describeWithIdIfSet(parsable);
                sb.append(">]");
            }
        }

        @Trivial
        public void describeIfSet(String name, ParsableList<? extends ParsableElement> parsableList) {
            if (parsableList != null) {
                sb.append("[" + name + "(");
                parsableList.describe(this);
                sb.append(")]");
            }
        }

        @Trivial
        public void append(String string) {
            sb.append(string);
        }

        @Trivial
        public String getDescription() {
            return sb.toString();
        }
    }

    private static final class DTDPublicIDResolver implements XMLResolver {
        String dtdPublicId;

        @Override
        public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) throws XMLStreamException {
            dtdPublicId = publicID;
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    private XMLStreamReader xsr = null;
    public String namespace;
    public String idNamespace;
    public String dtdPublicId;
    /**
     * The version specified in the deployment descriptor (e.g., 30 for servlet 3.0)
     */
    public int version;
    /**
     * The EE platform version implied by the version of the deployment descriptor
     * (e.g., 60 for EE 6).
     */
    public int eePlatformVersion;

    /**
     * Distinct from the version implied by the deployment descriptor, the Liberty runtime
     * has rules of function, dictated by the specification, that act on multiple descriptor
     * versions. Ex: an EE6 descriptor level running in an EE7 runtime.
     */
    public int runtimeVersion;

    public ComponentIDMap idMap = new ComponentIDMap();

    protected final Container rootContainer;
    protected final Entry adaptableEntry;
    protected final String ddEntryPath;
    public final Class<?> crossComponentDocumentType;
    protected ParsableElement rootParsable;
    public String rootElementLocalName;
    protected String currentElementLocalName;
    protected StringBuilder contentBuilder = new StringBuilder();
    protected boolean trimSimpleContentAsRequiredByServletSpec;

    protected final Object describeRootParsable = new Object() {
        @Trivial
        @Override
        public String toString() {
            DDParser.Diagnostics diag = new DDParser.Diagnostics(DDParser.this.idMap);
            diag.describe(rootParsable != null ? rootParsable.toString() : "DDParser", rootParsable);
            return diag.getDescription();
        }
    };

    protected abstract ParsableElement createRootParsable() throws ParseException;

    public DDParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
        this(ddRootContainer, ddEntry, null);
    }

    /**
     * Construct a parser for a specified container and container entry.  The container entry contains
     * descriptor text which is to be parsed.
     * 
     * @param rootContainer The root container containing the entry which is to be parsed.
     * @param adaptableEntry The container entry which is to be parsed.
     * @param crossComponentDocumentType Unknown.
     *
     * @throws ParseException Thrown in case of a parse error.  Not currently thrown.
     *    Declared for future use.
     */
    public DDParser(Container rootContainer, Entry adaptableEntry, Class<?> crossComponentDocumentType) throws ParseException {
        this.rootContainer = rootContainer;
        this.adaptableEntry = adaptableEntry;
        this.ddEntryPath = adaptableEntry.getPath();
        this.crossComponentDocumentType = crossComponentDocumentType;
    }

    public String getDeploymentDescriptorPath() {
        return ddEntryPath.substring(1);
    }

    /**
     * Allow specific implementing parsers to manipulate this default behavior.
     */
    protected void failInvalidRootElement() throws ParseException {
        if (rootParsable == null) {
            throw new ParseException(invalidRootElement());
        }
    }

    @FFDCIgnore({ IllegalArgumentException.class, XMLStreamException.class })
    private XMLStreamReader createXMLStreamReader(XMLResolver resolver, InputStream stream) throws ParseException {
        try {
            XMLInputFactory inputFactory = DesignatedXMLInputFactory.newInstance();
            // IBM XML parser requires a special property to enable line numbers.
            try {
                inputFactory.setProperty("javax.xml.stream.isSupportingLocationCoordinates", true);
            } catch (IllegalArgumentException e) {
                e.getClass(); // findbugs
            }
            inputFactory.setXMLResolver(resolver);
            return inputFactory.createXMLStreamReader(stream);
        } catch (XMLStreamException e) {
            throw new ParseException(xmlError(e), e);
        }
    }

    @FFDCIgnore(XMLStreamException.class)
    private void parseToRootElement() throws ParseException {
        try {
            while (!xsr.isStartElement()) {
                if (!xsr.hasNext()) {
                    throw new ParseException(rootElementNotFound());
                }
                xsr.next();
            }
        } catch (XMLStreamException e) {
            throw new ParseException(xmlError(e), e);
        }
    }

    @Trivial
    public Entry getAdaptableEntry() {
        return adaptableEntry;
    }

    private final Map<Class<?>, Object> adaptCache = new HashMap<Class<?>, Object>();

    @Trivial
    public <T> T adaptRootContainer(Class<T> adaptTarget) throws ParseException {
        Object cachedObject = adaptCache.get(adaptTarget);
        if (cachedObject != null) {
            return adaptTarget.cast(cachedObject);
        }
        try {
            T result = rootContainer.adapt(adaptTarget);
            adaptCache.put(adaptTarget, result);
            return result;
        } catch (UnableToAdaptException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ParseException) {
                throw (ParseException) cause;
            }
            throw new ParseException(xmlError(e), e);
        }
    }

    @Trivial
    public ParsableElement getRootParsable() {
        return rootParsable;
    }

    @Trivial
    public String getNamespaceURI(String prefix) {
        return xsr.getNamespaceURI(prefix);
    }

    // TODO: Need to rewrite this!
    @Trivial
    public String getAttributeValue(String namespaceURI, String localName) {
        boolean checkNS = namespaceURI != null;
        if (checkNS && namespaceURI.length() == 0) {
            namespaceURI = null;
        }
        final int attrCount = xsr.getAttributeCount();
        for (int i = 0; i < attrCount; ++i) {
            if (localName.equals(xsr.getAttributeLocalName(i)) &&
                (!checkNS || ("".equals(xsr.getAttributeNamespace(i)) ? null : xsr.getAttributeNamespace(i)) == namespaceURI)) {
                return xsr.getAttributeValue(i);
            }
        }
        return null;
    }

    @Trivial
    public String getAttributeValue(int index) {
        return xsr.getAttributeValue(index);
    }

    public BooleanType parseBooleanAttributeValue(int index) throws ParseException {
        return parseBoolean(getAttributeValue(index));
    }

    public IDType parseIDAttributeValue(int index) throws ParseException {
        return parseID(getAttributeValue(index));
    }

    public IntegerType parseIntegerAttributeValue(int index) throws ParseException {
        return parseInteger(getAttributeValue(index));
    }

    public LongType parseLongAttributeValue(int index) throws ParseException {
        return parseLong(getAttributeValue(index));
    }

    public QNameType parseQNameAttributeValue(int index) throws ParseException {
        return parseQName(getAttributeValue(index));
    }

    public StringType parseStringAttributeValue(int index) throws ParseException {
        return parseString(getAttributeValue(index));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ParsableListImplements<StringType, String> parseStringListAttributeValue(int index) throws ParseException {
        return (ParsableListImplements) parseTokenAttributeValue(index).split(this, " ");
    }

    public ProtectedStringType parseProtectedStringAttributeValue(int index) throws ParseException {
        return parseProtectedString(getAttributeValue(index));
    }

    public TokenType parseTokenAttributeValue(int index) throws ParseException {
        return parseToken(getAttributeValue(index));
    }

    public <T extends Enum<T>> T parseEnumAttributeValue(int index, Class<T> valueClass) throws ParseException {
        return parseEnum(getAttributeValue(index), valueClass);
    }

    @Trivial
    public void appendTextToContent() {
        contentBuilder.append(xsr.getText());
    }

    @Trivial
    public String getContentString(boolean untrimmed) {
        String lexical = contentBuilder.toString();
        if (!untrimmed && trimSimpleContentAsRequiredByServletSpec) {
            lexical = lexical.trim();
        }
        contentBuilder.setLength(0);
        return lexical;
    }

    @Trivial
    public String getContentString() {
        return getContentString(false);
    }

    @Trivial
    public boolean isWhiteSpace() {
        return xsr.isWhiteSpace();
    }

    @Trivial
    @FFDCIgnore(XMLStreamException.class)
    public void skipSubtree() throws ParseException {
        try {
            int depth = 0;
            while (xsr.hasNext()) {
                if (xsr.isStartElement()) {
                    depth++;
                } else if (xsr.isEndElement()) {
                    if (--depth == 0) {
                        return;
                    }
                }
                xsr.next();
            }
            throw new ParseException(endElementNotFound());
        } catch (XMLStreamException e) {
            throw new ParseException(xmlError(e), e);
        }
    }

    protected void parseRootElement() throws ParseException {
        InputStream stream = null;
        try {
            stream = adaptableEntry.adapt(InputStream.class);
            DTDPublicIDResolver resolver = new DTDPublicIDResolver();
            xsr = createXMLStreamReader(resolver, stream);
            parseToRootElement();
            dtdPublicId = resolver.dtdPublicId;
            resolver = null;
            namespace = xsr.getNamespaceURI();
            rootElementLocalName = xsr.getLocalName();
            currentElementLocalName = rootElementLocalName;
            rootParsable = createRootParsable();
            failInvalidRootElement();
            if (rootParsable != null) {
                parse(rootParsable);
            }
        } catch (UnableToAdaptException e) {
            throw new ParseException(xmlError(e), e);
        } finally {
            try {
                if (xsr != null) {
                    xsr.close();
                }
            } catch (XMLStreamException xse) {
                // FFDCs
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException ioe) {
                    //FFDCs
                }
            }
        }
    }

    /**
     * If you are wanting to parse from the root element you should
     * call parseRootElement() which then invokes this parse.
     */
    @FFDCIgnore(XMLStreamException.class)
    public void parse(ParsableElement parsable) throws ParseException {
        QName elementName = xsr.getName();
        String elementLocalName = xsr.getLocalName();
        currentElementLocalName = elementLocalName;
        int attrCount = xsr.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrNS = xsr.getAttributeNamespace(i);
            String attrLocal = xsr.getAttributeLocalName(i);
            if (parsable.isIdAllowed() && "id".equals(attrLocal)) {
                if (idNamespace != null) {
                    if (!idNamespace.equals(attrNS)) {
                        throw new ParseException(incorrectIDAttrNamespace(attrNS));
                    }
                } else if (attrNS != null) {
                    throw new ParseException(incorrectIDAttrNamespace(attrNS));
                }
                IDType idKey = parseIDAttributeValue(i);
                String key = idKey.getValue();
                Object oldValue = idMap.get(key);
                if (oldValue == null) {
                    oldValue = idMap.put(key, parsable);
                }
                if (oldValue != null && oldValue != parsable) {
                    if (oldValue != ComponentIDMap.DUPLICATE) {
                        idMap.put(key, ComponentIDMap.DUPLICATE);
                    }
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "The element {0} has an id {1} that is not unique.", currentElementLocalName, key);
                    }
                }
                continue;
            }
            if (XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(attrNS)) {
                if ("nil".equals(attrLocal)) {
                    parsable.setNil(parseBooleanAttributeValue(i).getBooleanValue());
                    continue;
                }
                if ("schemaLocation".equals(attrLocal)) {
                    // no action needed
                    continue;
                }
            }
            if (parsable.handleAttribute(this, attrNS, attrLocal, i)) {
                continue;
            }
            throw new ParseException(unexpectedAttribute(attrLocal));
        }
        try {
            while (xsr.hasNext()) {
                switch (xsr.next()) {
                    case XMLStreamConstants.CHARACTERS:
                    case XMLStreamConstants.CDATA:
                        if (parsable.handleContent(this)) {
                            break;
                        }
                        throw new ParseException(unexpectedContent());
                    case XMLStreamConstants.END_ELEMENT:
                        parsable.finish(this);
                        if (parsable == rootParsable) {
                            // check that the document is well-formed after the end of the root element
                            while (xsr.hasNext()) {
                                xsr.next();
                            }
                            xsr.close();
                        }
                        return;
                    case XMLStreamConstants.START_ELEMENT:
                        String localName = xsr.getLocalName();
                        if (namespace != null) {
                            if (!namespace.equals(xsr.getNamespaceURI())) {
                                throw new ParseException(incorrectChildElementNamespace(xsr.getNamespaceURI(), localName));
                            }
                        } else if (xsr.getNamespaceURI() != null) {
                            throw new ParseException(incorrectChildElementNamespace(xsr.getNamespaceURI(), localName));
                        }
                        final boolean handledChild = parsable.handleChild(this, localName);
                        currentElementLocalName = elementLocalName;
                        if (!handledChild) {
                            throw new ParseException(unexpectedChildElement(localName));
                        }
                        break;
                    case XMLStreamConstants.COMMENT:
                    case XMLStreamConstants.PROCESSING_INSTRUCTION:
                        // ignored
                        break;
                    default:
                        int eventType = xsr.getEventType();
                        RuntimeException re = new RuntimeException("unexpected event " + eventType + " while processing element \"" + elementName + "\".");
                        FFDCFilter.processException(re, "com.ibm.ws.javaee.ddmodel.DDParser", "410", this);
                        break;
                }
            }
        } catch (XMLStreamException e) {
            throw new ParseException(xmlError(e), e);
        }
        throw new ParseException(endElementNotFound());
    }

    public BooleanType parseBoolean(String lexical) throws ParseException {
        return BooleanType.wrap(this, lexical);
    }

    public IDType parseID(String lexical) throws ParseException {
        return IDType.wrap(this, lexical);
    }

    public IntegerType parseInteger(String lexical) throws ParseException {
        return IntegerType.wrap(this, lexical);
    }

    public LongType parseLong(String lexical) throws ParseException {
        return LongType.wrap(this, lexical);
    }

    public QNameType parseQName(String lexical) throws ParseException {
        return QNameType.wrap(this, lexical);
    }

    public StringType parseString(String lexical) throws ParseException {
        return StringType.wrap(this, lexical);
    }

    public ProtectedStringType parseProtectedString(@Sensitive String lexical) throws ParseException {
        return ProtectedStringType.wrap(lexical);
    }

    public TokenType parseToken(String lexical) throws ParseException {
        return TokenType.wrap(this, lexical);
    }

    /**
     * A mix-in interface for enums that allows values to be used only if
     * the parser version is at the correct level.
     */
    public interface VersionedEnum {
        /**
         * The minimum value for {@link DDParser#version} that is required for
         * this constant to be valid.
         */
        int getMinVersion();
    }

    /**
     * Return an array of the constants for the enum that are valid based on the
     * version of this parser.
     */
    private <T extends Enum<T>> Object[] getValidEnumConstants(Class<T> valueClass) {
        T[] constants = valueClass.getEnumConstants();
        if (!VersionedEnum.class.isAssignableFrom(valueClass)) {
            return constants;
        }

        List<T> valid = new ArrayList<T>(constants.length);
        for (T value : constants) {
            VersionedEnum versionedValue = (VersionedEnum) value;
            if (version >= versionedValue.getMinVersion()) {
                valid.add(value);
            }
        }

        return valid.toArray();
    }

    @FFDCIgnore({ IllegalArgumentException.class, IncompatibleClassChangeError.class })
    public <T extends Enum<T>> T parseEnum(String value, Class<T> valueClass) throws ParseException {
        T constant;
        try {
            try {
                constant = Enum.valueOf(valueClass, value);
            } catch (IncompatibleClassChangeError e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "rethrowing IncompatibleClassChangeError as IllegalArgumentException", e);
                // FIXME: IBM Java 7 Enum.valueOf uses Class.getCanonicalName,
                // but the JVM has a bug in the processing of the InnerClasses
                // class attribute that causes that method to to fail erroneously.
                throw new IllegalArgumentException(e);
            }
        } catch (IllegalArgumentException e) {
            throw new ParseException(invalidEnumValue(value, getValidEnumConstants(valueClass)), e);
        }

        if (constant instanceof VersionedEnum) {
            VersionedEnum versionedConstant = (VersionedEnum) constant;
            if (version < versionedConstant.getMinVersion()) {
                throw new ParseException(invalidEnumValue(value, getValidEnumConstants(valueClass)));
            }
        }

        return constant;
    }

    public static void unresolvedReference(String elementName, String href) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Unable to resolve reference from element {0} to {1}.", elementName, href);
        }
    }

    public String getPath() {
        return ddEntryPath;
    }

    public int getLineNumber() {
        return xsr == null ? -1 : xsr.getLocation().getLineNumber();
    }

    public String requiredAttributeMissing(String attrLocal) {
        return Tr.formatMessage(tc, "required.attribute.missing", ddEntryPath, getLineNumber(), currentElementLocalName, attrLocal);
    }

    protected String invalidRootElement() {
        return Tr.formatMessage(tc, "invalid.root.element", ddEntryPath, getLineNumber(), rootElementLocalName);
    }

    private String rootElementNotFound() {
        return Tr.formatMessage(tc, "root.element.not.found", ddEntryPath, getLineNumber());
    }

    private String endElementNotFound() {
        return Tr.formatMessage(tc, "end.element.not.found", ddEntryPath, getLineNumber(), currentElementLocalName);
    }

    private String incorrectIDAttrNamespace(String attrNS) {
        return Tr.formatMessage(tc, "incorrect.id.attr.namespace", ddEntryPath, getLineNumber(), currentElementLocalName, attrNS, idNamespace);
    }

    private String unexpectedAttribute(String attrLocal) {
        return Tr.formatMessage(tc, "unexpected.attribute", ddEntryPath, getLineNumber(), currentElementLocalName, attrLocal);
    }

    public String unexpectedContent() {
        return Tr.formatMessage(tc, "unexpected.content", ddEntryPath, getLineNumber(), currentElementLocalName);
    }

    private String incorrectChildElementNamespace(String elementNS, String elementLocal) {
        return Tr.formatMessage(tc, "incorrect.child.element.namespace", ddEntryPath, getLineNumber(), currentElementLocalName, elementLocal, elementNS, namespace);
    }

    private String unexpectedChildElement(String elementLocal) {
        return Tr.formatMessage(tc, "unexpected.child.element", ddEntryPath, getLineNumber(), currentElementLocalName, elementLocal);
    }

    public String invalidHRefPrefix(String hrefElementName, String hrefPrefix) {
        return Tr.formatMessage(tc, "invalid.href.prefix", ddEntryPath, getLineNumber(), hrefElementName, hrefPrefix);
    }

    protected String unknownDeploymentDescriptorVersion() {
        return Tr.formatMessage(tc, "unknown.deployment.descriptor.version", ddEntryPath);
    }

    protected String invalidDeploymentDescriptorNamespace(String useVersion) {
        return Tr.formatMessage(tc, "invalid.deployment.descriptor.namespace", ddEntryPath, getLineNumber(), namespace, useVersion);
    }

    protected String invalidDeploymentDescriptorVersion(String useVersion) {
        return Tr.formatMessage(tc, "invalid.deployment.descriptor.version", ddEntryPath, getLineNumber(), useVersion);
    }

    protected String missingDeploymentDescriptorNamespace() {
        return Tr.formatMessage(tc, "missing.deployment.descriptor.namespace", ddEntryPath, getLineNumber());
    }

    protected String missingDeploymentDescriptorVersion() {
        return Tr.formatMessage(tc, "missing.deployment.descriptor.version", ddEntryPath, getLineNumber());
    }

    public String tooManyElements(String element) {
        return Tr.formatMessage(tc, "at.most.one.occurrence", ddEntryPath, getLineNumber(), currentElementLocalName, element);
    }

    public String missingElement(String element) {
        return Tr.formatMessage(tc, "required.method.element.missing", ddEntryPath, getLineNumber(), currentElementLocalName, element);
    }

    private String xmlError(XMLStreamException e) {
        return Tr.formatMessage(tc, "xml.error", ddEntryPath, getLineNumber(), e.getMessage());
    }

    private String xmlError(Throwable e) {
        return Tr.formatMessage(tc, "xml.error", ddEntryPath, getLineNumber(), e.toString());
    }

    public String invalidEnumValue(String value, Object... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                builder.append(", ");
            }
            builder.append(values[i]);
        }
        return Tr.formatMessage(tc, "invalid.enum.value", ddEntryPath, getLineNumber(), value, builder);
    }

    public String invalidIntValue(String value) {
        return Tr.formatMessage(tc, "invalid.int.value", ddEntryPath, getLineNumber(), value);
    }

    public String invalidLongValue(String value) {
        return Tr.formatMessage(tc, "invalid.long.value", ddEntryPath, getLineNumber(), value);
    }
}
