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
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.DesignatedXMLInputFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public abstract class DDParser {
    protected static final TraceComponent tc = Tr.register(DDParser.class);

    protected void warning(String message) {
        Tr.warning(tc, message);
    }
    
    //
    
    public static final String NAMESPACE_SUN_J2EE =
        "http://java.sun.com/xml/ns/j2ee";
    public static final String NAMESPACE_SUN_JAVAEE =
        "http://java.sun.com/xml/ns/javaee";
    public static final String NAMESPACE_JCP_JAVAEE =
        "http://xmlns.jcp.org/xml/ns/javaee";
    public static final String NAMESPACE_JAKARTA =
        "https://jakarta.ee/xml/ns/jakartaee";    

    //

    public static String getDottedVersionText(int version) {
        if ( version == 0 ) {
            return null;

        } else {
            byte[] versionBytes = {
                (byte) ('0' + (version / 10)),
                '.',
                (byte) ('0' + (version % 10)) };
            return new String(versionBytes);
        }
    }

    public static String getVersionText(int version) {
        switch ( version ) {
            case 0: return null;
            case 12: return "1.2";
            case 13: return "1.3";
            case 14: return "1.4";
            case 50: return "5";
            case 60: return "6";
            case 70: return "7";
            case 80: return "8";
            case 90: return "9";
            default: throw new IllegalArgumentException("Unknown schema version");
        }
    }
    
    //

    public static class ParseException extends Exception {
        private static final long serialVersionUID = -2437543890927284677L;

        public ParseException(String translatedMessage) {
            super(translatedMessage);
        }

        public ParseException(String translatedMessage, Throwable t) {
            super(translatedMessage, t);
        }
    }

    //

    public interface RootParsable {
        void describe(StringBuilder sb);
    }

    public interface Parsable {
        void describe(Diagnostics diag);
    }

    public interface ParsableElement extends Parsable {
        void setNil(boolean nilled);
        boolean isNil();

        boolean isIdAllowed();

        boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException;
        boolean handleChild(DDParser parser, String localName) throws ParseException;
        boolean handleContent(DDParser parser) throws ParseException;

        void finish(DDParser parser) throws ParseException;
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

    public static class ParsableListImplements<T extends ParsableElement, I>
        extends ParsableList<T> {

        @Trivial
        @SuppressWarnings("unchecked")
        public List<I> getList() {
            return (List<I>) list;
        }
    }

    public static class ComponentIDMap {
        public static final Object DUPLICATE = new Object();

        private final Map<String, Object> idToComponentMap =
            new HashMap<String, Object>();

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
        private String dtdPublicId;

        /**
         * Override to answer an empty input stream.
         *
         * As a side effect, store the public ID.  This is used
         * when processing the XML header.
         *
         * @return The resolved entity.  This implementation
         *     always answers an empty input stream.
         */
        @Override
        public Object resolveEntity(
            String publicID, String systemID,
            String baseURI, String namespace) throws XMLStreamException {

            dtdPublicId = publicID;

            return new ByteArrayInputStream(new byte[0]);
        }
    }

    //

    public DDParser(Container ddRootContainer, Entry ddEntry, String expectedRootName)
        throws ParseException {

        this(ddRootContainer, ddEntry,
             UNUSED_MAX_SCHEMA_VERSION,
             expectedRootName);
    }    
    
    protected static final int UNUSED_MAX_SCHEMA_VERSION = -1;

    /**
     * Construct a parser for a specified container and container entry.
     * 
     * 
     * @param ddRootContainer The root container containing the entry which is to be parsed.
     * @param ddEntry The container entry which is to be parsed.
     * @param maxSchemaVersion The maximum schema version which will be
     *     parsed.
     * @param expectedRootName The expected root element name.
     *
     * @throws ParseException Thrown in case of a parse error.  Not currently thrown.
     *    Declared for future use.
     */
    public DDParser(Container ddRootContainer, Entry ddEntry,
                    int maxSchemaVersion,
                    String expectedRootName) throws ParseException {

        this.maxVersion = maxSchemaVersion;

        this.adaptableEntry = ddEntry;
        this.ddEntryPath = ddEntry.getPath();
        this.rootContainer = ddRootContainer;

        this.expectedRootName = expectedRootName;
    }

    /**
     * Answer the type of the linked descriptor.
     * 
     * The presence of this API on {@link DDParser} is
     * a historical artifact.  Cross component types are
     * only used for BND and EXT documents, and the API
     * should have only been exposed to {@link DDParserBndExt}.
     * Rewiring the many parse types is too big of a change,
     * so the API has been left here.
     *
     * @return The type of the linked descriptor.
     */
    public Class<?> getCrossComponentType() {
        throw new UnsupportedOperationException("Cross components require a BND or EXT parser");
    }
    
    // Control parameters ... 

    /** The current maximum supported schema version. */
    public final int maxVersion;

    // What will be parsed ...

    protected final Entry adaptableEntry;
    protected final String ddEntryPath;

    protected final Container rootContainer;

    private final Map<Class<?>, Object> adaptCache =
        new HashMap<Class<?>, Object>();

    @Trivial
    public Entry getAdaptableEntry() {
        return adaptableEntry;
    }

    protected InputStream openEntry() throws ParseException {
        try {
            return adaptableEntry.adapt(InputStream.class);
        } catch ( UnableToAdaptException e ) {
            throw new ParseException( xmlError(e), e );
        }
    }

    @Trivial
    public String getPath() {
        return ddEntryPath;
    }

    @Trivial
    public String getDeploymentDescriptorPath() {
        return ddEntryPath.substring(1);
    }

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

    public String getModuleName() {
        ModuleInfo moduleInfo = cacheGet(ModuleInfo.class);
        if ( moduleInfo != null ) {
            return moduleInfo.getName();
        } else {
            return rootContainer.getName();
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T cacheGet(Class<T> targetClass) {
        NonPersistentCache cache = null;
        try {
            cache = rootContainer.adapt(NonPersistentCache.class);
        } catch ( UnableToAdaptException e ) {
            // FFDC
        }
        return ( (cache == null) ? null : (T) cache.getFromCache(targetClass) );
    }    
    
    // Parse parameterization ...
    
    private final String expectedRootName;
    
    public String getExpectedRootName() {
        return expectedRootName;
    }
    
    protected void validateRootElementName() throws ParseException {
        String useExpectedName = getExpectedRootName();
        if ( !useExpectedName.equals(rootElementLocalName)) {
            throw new ParseException( unexpectedRootElement(useExpectedName) );
        }
    }    
    
    // XML header values ...

    public int version; // Schema version
    public int eePlatformVersion; // Schema version turned into a platform version.

    public String dtdPublicId;
    public String namespace;

    public String idNamespace;

    public String getDottedVersionText() {
        return getDottedVersionText(version);
    }
    
    public String getVersionText() {
        return getVersionText(version);
    }

    // Root parse objects ...
    
    protected ParsableElement rootParsable;    
    public String rootElementLocalName;
    
    public ComponentIDMap idMap = new ComponentIDMap();

    protected String currentElementLocalName;

    protected final Object describeRootParsable = new Object() {
        @Trivial
        @Override
        public String toString() {
            DDParser.Diagnostics diag = new DDParser.Diagnostics(DDParser.this.idMap);
            diag.describe(rootParsable != null ? rootParsable.toString() : "DDParser", rootParsable);
            return diag.getDescription();
        }
    };

    @Trivial
    public ParsableElement getRootParsable() {
        return rootParsable;
    }

    // Reading primitives ...
    
    private XMLStreamReader xsr;
    
    @Trivial
    public String getNamespaceURI(String prefix) {
        return xsr.getNamespaceURI(prefix);
    }

    @Trivial
    public int getLineNumber() {
        return ( (xsr == null) ? -1 : xsr.getLocation().getLineNumber() );
    }

    @Trivial
    public boolean isWhiteSpace() {
        return xsr.isWhiteSpace();
    }
    
    /**
     * Retrieve the value of an attribute.
     * 
     * Match on the target local name and optionally on a target namespace.
     *
     * Match only on the target local name if the target namespace is null.
     *
     * Match an empty target namespace against empty attribute namespaces
     * and against null attribute namespaces.
     *
     * @param targetNS The namespace of the target attribute.
     * @param targetLocalName The local name of the target attribute.
     *
     * @return The value of the target attribute.  Null if the target
     *     attribute is not found.
     */
    @Trivial
    public String getAttributeValue(String targetNS, String targetLocalName) {
        int attrCount = xsr.getAttributeCount();
        for ( int attrNo = 0; attrNo < attrCount; attrNo++ ) {
            String attrLocalName = xsr.getAttributeLocalName(attrNo);

            // First check is against the attribute local name.
            // This must match the target local name.

            if ( !targetLocalName.equals(attrLocalName) ) {
                continue;
            }

            // Second check is against the attribute namespace.
            // Ignore this check if a null target namespace was provided.

            if ( targetNS == null ) {
                return xsr.getAttributeValue(attrNo);
            }

            // When matching namespaces, an empty target namespace
            // matches a null attribute namespace as well as an
            // empty attribute namespace.  A non-null target namespace
            // must exactly match the attribute namespace.
            
            String attrNS = xsr.getAttributeNamespace(attrNo);
            if ( attrNS == null ) {
                if ( targetNS.isEmpty() ) {
                    return xsr.getAttributeValue(attrNo);
                } else {
                    // Keep looking: The target namespace is
                    // not null and is not empty, and this
                    // attribute namespace is null.
                }
            } else {
                if ( targetNS.equals(attrNS) ) {
                    // This test works if either namespace is empty.
                    return xsr.getAttributeValue(attrNo);
                } else {
                    // Keep looking.
                }
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

    // Element content primitives ...

    protected StringBuilder contentBuilder = new StringBuilder();
    
    @Trivial
    public void appendTextToContent() {
        contentBuilder.append(xsr.getText());
    }

    /**
     * Answer the accumulated content, conditionally
     * trimming whitespace.  Reset the content accumulator.
     *
     * This default implementation never trims whitespace.
     * See {@link DDParserSpec#getContentString(boolean)}.
     * 
     * @param untrimmed Control parameter: Tell if trimming
     *    (if enabled for this type), is to be performed.
     * @return The accumulated content.
     */    
    @Trivial
    public String getContentString(boolean untrimmed) {
        return getContentString();
    }

    /**
     * Answer the accumulated content, including
     * whitespace.  Reset the content accumulator.
     *
     * @return The accumulated content.
     */
    @Trivial
    public String getContentString() {
        String content = contentBuilder.toString();
        contentBuilder.setLength(0);
        return content;
    }

    // Top level parsing ...

    public ParsableElement parse() throws ParseException {
        parseRootElement();
        return rootParsable;
    }

    protected void parseRootElement() throws ParseException {
        InputStream stream = null;
        try {
            stream = openEntry();

            DTDPublicIDResolver resolver = new DTDPublicIDResolver();
            
            try {
                xsr = createXMLStreamReader(resolver, stream);

                parseToRootElement();

                dtdPublicId = resolver.dtdPublicId;
                namespace = xsr.getNamespaceURI();
                rootElementLocalName = xsr.getLocalName();

                currentElementLocalName = rootElementLocalName;
                rootParsable = createRootParsable();

                if ( rootParsable != null ) {
                    parse(rootParsable);
                }

            } finally {
                if ( xsr != null ) {
                    try {
                        xsr.close();
                    } catch ( XMLStreamException xse ) {
                        // FFDC
                    }
                }
            }

        } finally {
            if ( stream != null ) {
                try {
                    stream.close();
                } catch ( IOException ioe ) {
                    //FFDC
                }
            }
        }
    }

    @FFDCIgnore({ IllegalArgumentException.class, XMLStreamException.class })
    private XMLStreamReader createXMLStreamReader(XMLResolver resolver, InputStream stream) throws ParseException {
        try {
            XMLInputFactory inputFactory = DesignatedXMLInputFactory.newInstance();
            // IBM XML parser requires a special property to enable line numbers.
            try {
                inputFactory.setProperty("javax.xml.stream.isSupportingLocationCoordinates", true);
            } catch ( IllegalArgumentException e ) {
                // FFDC
            }
            inputFactory.setXMLResolver(resolver);
            return inputFactory.createXMLStreamReader(stream);

        } catch ( XMLStreamException e ) {
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
        } catch ( XMLStreamException e ) {
            throw new ParseException(xmlError(e), e);
        }
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

    protected abstract ParsableElement createRootParsable() throws ParseException;
    
    // Body parsing ...

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
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
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

    // Enum handling ...

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

    // Version handling ...

    protected static class VersionData {
        public final String versionAttr;
        public final String namespace;
        public final String publicId;
        public final int version;
        public final int platformVersion;

        public VersionData(
            String versionAttr, String publicId, String namespace, int version, int platformVersion) {

            this.versionAttr = versionAttr;
            this.namespace = namespace;
            this.publicId = publicId;
            this.version = version;
            this.platformVersion = platformVersion;
        }
        
        public boolean accept(String ddVersionAttr, String ddPublicId, String ddNamespace) {
            if ( ddVersionAttr == null ) {
                return ( ((publicId != null) && publicId.equals(ddPublicId)) ||
                         ((namespace != null) && namespace.equals(ddNamespace)) );
            } else {
                return versionAttr.equals(ddVersionAttr);
            }
        }
        
        public String toString() {
            return super.toString() +
                "(" + versionAttr +
                ", " + namespace +
                ", " + publicId +
                ", " + version +
                ", " + platformVersion +
                ")";
        }
    }

    /**
     * Select the last version that accepts the specified descriptor header
     * values.
     *
     * The table of versions must be in ascending order.
     *
     * When no version attribute is specified, match the highest provisioned
     * version that matches the schema.
     *
     * Schema based matching is inexact: Multiple versions may exist which
     * use the same schema.
     *
     * Ignore mismatches between the version attribute and the namespace.
     * The version value takes precedence.
     * 
     * @param versions The versions from which to select.
     * @param versionAttr The version attribute value from an XML header.
     *     Will be null if parsing a DTD based descriptor.  May be null,
     *     in which case the public ID or namespace will be used for
     *     matching.
     * @param publicId The public ID from an XML header.  Will be null
     *     unless parsing a DTD based descriptor.
     * @param namespace The namespace value from an XML header.  Will
     *     be null if parsing a DTD based descriptor.  May be null,
     *     in which case the version or public ID will be used for matching.
     * @param maxSchemaVersion The current maximum provisioned
     *     schema version.
     *     
     * @return The selected version.  Null if no matching version was
     *     selected.
     */
    protected static VersionData selectVersion(
        VersionData[] versions,
        String versionAttr, String publicId, String namespace,
        int maxSchemaVersion) {

        // There are two selection rules which interact to
        // give rise to the loop termination test:
        //
        // First, look for any first match, even if that match
        // is above the maximum provisioned version.
        //
        // Second, after finding the first match, disregard any
        // subsequent versions which are higher than the maximum
        // provisioned version.
        //
        // The selection will be either the first version which
        // matches and which is above the maximum provisioned
        // version, or will be the last version which matches and
        // which is provisioned.
        VersionData lastSelected = null;
        for ( VersionData version : versions ) {
            if ( (lastSelected != null) && (version.version > maxSchemaVersion) ) {
                break;
            }

            if ( version.accept(versionAttr, publicId, namespace) ) {
                lastSelected = version;
            }
        }
        return lastSelected;
    }
    
    // Error handling ...

    /**
     * Describe an entry, providing paths to parent archives
     * back to the root-of-roots container.
     * 
     * If possible, display the simple name of the physical path
     * of the root-of-roots container.  Do not display the full
     * physical path, as that would leak information about the
     * server location on disk.
     *
     * For example:
     * <code>
     *     WEB-INF/web.xml
     *     webModule.war : WEB-INF/web.xml
     *     myEar.ear : webModule.war : WEB-INF/web.xml
     *     WEB-INF/lib/fragment1.jar : META-INF/web-fragment.xml
     * </code>
     *
     * @return A description of the target entry, including
     *     relative paths of enclosing entries.
     */
    protected String describeEntry() {
        StringBuilder builder = new StringBuilder();

        Entry nextEntry = adaptableEntry;
        while ( nextEntry != null ) {
            if ( builder.length() > 0 ) {
                builder.insert(0, " : ");
            }
            String nextPath = nextEntry.getPath();
            if ( (nextPath.length() > 1) && (nextPath.charAt(0) == '/') ) {
                nextPath = nextPath.substring(1); // Strip leading '/'
            }
            builder.insert(0, nextPath);

            Container nextRoot = nextEntry.getRoot();
            try {
                nextEntry = nextRoot.adapt(Entry.class);
            } catch ( UnableToAdaptException e ) {
                break; // Unexpected
            }

            if ( nextEntry == null ) {
                // We have reached the root-of-roots ...
                //
                // Do our best to display information about the root-of-roots
                // container.  If this has a physical path, display the simple
                // name from the path.  Don't display more, as that would leak
                // information about the physical location of server files.
                // Don't display anything if the root has just '/' as its path.

                @SuppressWarnings("deprecation")
                String path = nextRoot.getPhysicalPath();
                if ( path != null ) {
                    path = path.replace('\\', '/');
                    int slashOffset = path.lastIndexOf('/');
                    if ( slashOffset != -1 ) {
                        path = path.substring(slashOffset + 1);
                    }
                    if ( !path.isEmpty() ) {
                        builder.insert(0, " : ");
                        builder.insert(0, path);
                    }
                }
            }
        }

        String description = builder.toString();
        System.out.println("Description [ " + description + " ]"); // Temp for debugging.
        return description;
    }
    
    // Static: Invoked from descriptor element parsers, which
    //         which do not have a convenient reference to a parser.
    // TODO: Add the reference?  The warning is supplied without
    //       a descriptor reference and without a line number.

    public static void unresolvedReference(String elementName, String href) {
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Unable to resolve reference from element {0} to {1}.", elementName, href);
        }
    }

    public String requiredAttributeMissing(String attrLocal) {
        return Tr.formatMessage(tc, "required.attribute.missing", describeEntry(), getLineNumber(), currentElementLocalName, attrLocal);
    }

    private String rootElementNotFound() {
        return Tr.formatMessage(tc, "root.element.not.found", describeEntry(), getLineNumber());
    }

    private String endElementNotFound() {
        return Tr.formatMessage(tc, "end.element.not.found", describeEntry(), getLineNumber(), currentElementLocalName);
    }

    private String incorrectIDAttrNamespace(String attrNS) {
        return Tr.formatMessage(tc, "incorrect.id.attr.namespace", describeEntry(), getLineNumber(), currentElementLocalName, attrNS, idNamespace);
    }

    private String unexpectedAttribute(String attrLocal) {
        return Tr.formatMessage(tc, "unexpected.attribute", describeEntry(), getLineNumber(), currentElementLocalName, attrLocal);
    }

    public String unexpectedContent() {
        return Tr.formatMessage(tc, "unexpected.content", describeEntry(), getLineNumber(), currentElementLocalName);
    }

    private String incorrectChildElementNamespace(String elementNS, String elementLocal) {
        return Tr.formatMessage(tc, "incorrect.child.element.namespace", describeEntry(), getLineNumber(), currentElementLocalName, elementLocal, elementNS, namespace);
    }

    private String unexpectedChildElement(String elementLocal) {
        return Tr.formatMessage(tc, "unexpected.child.element", describeEntry(), getLineNumber(), currentElementLocalName, elementLocal);
    }

    public String invalidHRefPrefix(String hrefElementName, String hrefPrefix) {
        return Tr.formatMessage(tc, "invalid.href.prefix", describeEntry(), getLineNumber(), hrefElementName, hrefPrefix);
    }

    public String tooManyElements(String element) {
        return Tr.formatMessage(tc, "at.most.one.occurrence", describeEntry(), getLineNumber(), currentElementLocalName, element);
    }

    public String missingElement(String element) {
        return Tr.formatMessage(tc, "required.method.element.missing", describeEntry(), getLineNumber(), currentElementLocalName, element);
    }

    private String xmlError(XMLStreamException e) {
        return Tr.formatMessage(tc, "xml.error", describeEntry(), getLineNumber(), e.getMessage());
    }

    private String xmlError(Throwable e) {
        return Tr.formatMessage(tc, "xml.error", describeEntry(), getLineNumber(), e.toString());
    }

    public String invalidEnumValue(String value, Object... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                builder.append(", ");
            }
            builder.append(values[i]);
        }
        return Tr.formatMessage(tc, "invalid.enum.value", describeEntry(), getLineNumber(), value, builder);
    }

    public String invalidIntValue(String value) {
        return Tr.formatMessage(tc, "invalid.int.value", describeEntry(), getLineNumber(), value);
    }

    public String invalidLongValue(String value) {
        return Tr.formatMessage(tc, "invalid.long.value", describeEntry(), getLineNumber(), value);
    }

    // New messages ...

    // protected String missingDeploymentDescriptorVersion() {
    //     return Tr.formatMessage(tc, "missing.deployment.descriptor.version", describeEntry(), getLineNumber());
    // }
    
    protected String missingDescriptorVersion() {
        // The deployment descriptor {0} specifies neither a version, a PUBLIC ID, or a schema.
        return Tr.formatMessage(tc, "missing.descriptor.version", describeEntry());
    }

    // protected String invalidDeploymentDescriptorVersion(String useVersion) {
    //     return Tr.formatMessage(tc, "invalid.deployment.descriptor.version", describeEntry(), getLineNumber(), useVersion);
    // }
    
    // protected String unknownDeploymentDescriptorVersion() {
    //     return Tr.formatMessage(tc, "unknown.deployment.descriptor.version", describeEntry());
    // }

    protected String unsupportedDescriptorVersion(String ddVersion) {
        // The deployment descriptor {0}, at line {1}, specifies unsupported version {2}.
        return Tr.formatMessage(tc, "unsupported.descriptor.version", describeEntry(), getLineNumber(), ddVersion);
    }

    protected String unprovisionedDescriptorVersion(int schemaVersion, int maxSchemaVersion) {
        // The deployment descriptor {0}, at line {1}, specifies version {2}, which
        // is higher than the current provisioned version {3}.
        return Tr.formatMessage(tc, "unprovisioned.descriptor.version", describeEntry(), getLineNumber(), schemaVersion, maxSchemaVersion);
    }
    
    // protected String missingDeploymentDescriptorNamespace() {
    //     return Tr.formatMessage(tc, "missing.deployment.descriptor.namespace", describeEntry(), getLineNumber());
    // }
    
    protected String unsupportedDescriptorNamespace(String ddNamespace) {
        // The deployment descriptor {0}, at line {1}, specifies unsupported namespace {2}.        
        return Tr.formatMessage(tc, "unsupported.descriptor.namespace", describeEntry(), getLineNumber(), ddNamespace);
    }

    // protected String invalidDeploymentDescriptorNamespace(String useVersion) {
    //     return Tr.formatMessage(tc, "invalid.deployment.descriptor.namespace", describeEntry(), getLineNumber(), namespace, useVersion);
    // }
    
    protected String incorrectDescriptorNamespace(String ddVersion, String ddNamespace, String expectedNamespace) {
        // The deployment descriptor {0}, at line {1}, specifies version {2} and namespace {3},
        // but should have namespace {4}.
        return Tr.formatMessage(tc, "incorrect.descriptor.namespace.for.version", describeEntry(), getLineNumber(), ddVersion, ddNamespace, expectedNamespace);        
    }
    
    protected String incorrectDescriptorNamespace(String ddNamespace, String expectedNamespace) {
        // The deployment descriptor {0}, at line {1}, specifies namespace {2},
        // but should have namespace {3}.
        return Tr.formatMessage(tc, "incorrect.descriptor.namespace", describeEntry(), getLineNumber(), ddNamespace, expectedNamespace);        
    }    

    // protected String invalidDeploymentDescriptorPublicId(String usePublicId) {
    //     return Tr.formatMessage(tc, "invalid.deployment.descriptor.public.id", describeEntry(), getLineNumber(), usePublicId);
    // }    
    
    protected String unsupportedDescriptorPublicId(String ddPublicId) {
        // The deployment descriptor {0}, at line {1}, specifies unsupported public ID {2}.
        return Tr.formatMessage(tc, "unsupported.descriptor.public.id", describeEntry(), getLineNumber(), ddPublicId);
    }

    // protected String invalidRootElement() {
    //     return Tr.formatMessage(tc, "invalid.root.element", describeEntry(), getLineNumber(), rootElementLocalName);
    // }

    protected String unexpectedRootElement(String expectedRootElementName) {
        // The deployment descriptor {0}, at line {1}, has root element {2},
        // but requires root element {3}. 
        return Tr.formatMessage(tc, "unexpected.root.element", describeEntry(), getLineNumber(), rootElementLocalName, expectedRootElementName);
    }    
}
