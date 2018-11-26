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

package org.apache.cxf.common.jaxb;


import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.ASMHelper.ClassWriter;
import org.apache.cxf.common.util.ASMHelper.FieldVisitor;
import org.apache.cxf.common.util.ASMHelper.Label;
import org.apache.cxf.common.util.ASMHelper.MethodVisitor;
import org.apache.cxf.common.util.ASMHelper.Opcodes;
import org.apache.cxf.common.util.CachedClass;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.common.util.ReflectionInvokationHandler.WrapReturn;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.helpers.JavaUtils;

public final class JAXBUtils {
    public static final String JAXB_URI = "http://java.sun.com/xml/ns/jaxb";

    private static final Logger LOG = LogUtils.getL7dLogger(JAXBUtils.class);
    
    public enum IdentifierType {
        CLASS,
        INTERFACE,
        GETTER,
        SETTER,
        VARIABLE,
        CONSTANT
    };

    private static final char[] XML_NAME_PUNCTUATION_CHARS = new char[] {
        /* hyphen                       */ '\u002D',
        /* period                       */ '\u002E',
        /* colon                        */'\u003A',
        /* dot                          */ '\u00B7',
        /* greek ano teleia             */ '\u0387',
        /* arabic end of ayah           */ '\u06DD',
        /* arabic start of rub el hizb  */'\u06DE',
        /* underscore                   */ '\u005F',
    };

    private static final String XML_NAME_PUNCTUATION_STRING = new String(XML_NAME_PUNCTUATION_CHARS);

    private static final Map<String, String> BUILTIN_DATATYPES_MAP;
    private static final Map<String, Class<?>> HOLDER_TYPES_MAP;
    private static ClassLoader jaxbXjcLoader;
    private static volatile Optional<Object> jaxbMinimumEscapeHandler;
    private static volatile Optional<Object> jaxbNoEscapeHandler;

    static {
        BUILTIN_DATATYPES_MAP = new HashMap<>();
        BUILTIN_DATATYPES_MAP.put("string", "java.lang.String");
        BUILTIN_DATATYPES_MAP.put("integer", "java.math.BigInteger");
        BUILTIN_DATATYPES_MAP.put("int", "int");
        BUILTIN_DATATYPES_MAP.put("long", "long");
        BUILTIN_DATATYPES_MAP.put("short", "short");
        BUILTIN_DATATYPES_MAP.put("decimal", "java.math.BigDecimal");
        BUILTIN_DATATYPES_MAP.put("float", "float");
        BUILTIN_DATATYPES_MAP.put("double", "double");
        BUILTIN_DATATYPES_MAP.put("boolean", "boolean");
        BUILTIN_DATATYPES_MAP.put("byte", "byte");
        BUILTIN_DATATYPES_MAP.put("QName", "javax.xml.namespace.QName");
        BUILTIN_DATATYPES_MAP.put("dateTime", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("base64Binary", "byte[]");
        BUILTIN_DATATYPES_MAP.put("hexBinary", "byte[]");
        BUILTIN_DATATYPES_MAP.put("unsignedInt", "long");
        BUILTIN_DATATYPES_MAP.put("unsignedShort", "short");
        BUILTIN_DATATYPES_MAP.put("unsignedByte", "byte");
        BUILTIN_DATATYPES_MAP.put("time", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("date", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gYear", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gYearMonth", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gMonth", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gMonthDay", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("gDay", "javax.xml.datatype.XMLGregorianCalendar");
        BUILTIN_DATATYPES_MAP.put("duration", "javax.xml.datatype.Duration");
        BUILTIN_DATATYPES_MAP.put("NOTATION", "javax.xml.namespace.QName");
        BUILTIN_DATATYPES_MAP.put("string", "java.lang.String");

        HOLDER_TYPES_MAP = new HashMap<>();
        HOLDER_TYPES_MAP.put("int", java.lang.Integer.class);
        HOLDER_TYPES_MAP.put("long", java.lang.Long.class);
        HOLDER_TYPES_MAP.put("short", java.lang.Short.class);
        HOLDER_TYPES_MAP.put("float", java.lang.Float.class);
        HOLDER_TYPES_MAP.put("double", java.lang.Double.class);
        HOLDER_TYPES_MAP.put("boolean", java.lang.Boolean.class);
        HOLDER_TYPES_MAP.put("byte", java.lang.Byte.class);
    }


    /**
     * prevents instantiation
     *
     */
    private JAXBUtils() {
    }

    @FFDCIgnore(IOException.class)
    public static void closeUnmarshaller(Unmarshaller u) {
        if (u instanceof Closeable) {
            //need to do this to clear the ThreadLocal cache
            //see https://java.net/jira/browse/JAXB-1000

            try {
                ((Closeable)u).close();
            } catch (IOException e) {
                //ignore
            }
        }
    }
    public static Object unmarshall(JAXBContext c, Element e) throws JAXBException {
        Unmarshaller u = c.createUnmarshaller();
        try {
            u.setEventHandler(null);
            return u.unmarshal(e);
        } finally {
            closeUnmarshaller(u);
        }
    }
    public static <T> JAXBElement<T> unmarshall(JAXBContext c, Element e, Class<T> cls) throws JAXBException {
        Unmarshaller u = c.createUnmarshaller();
        try {
            u.setEventHandler(null);
            return u.unmarshal(e, cls);
        } finally {
            closeUnmarshaller(u);
        }
    }
    public static Object unmarshall(JAXBContext c, Source s) throws JAXBException {
        Unmarshaller u = c.createUnmarshaller();
        try {
            u.setEventHandler(null);
            return u.unmarshal(s);
        } finally {
            closeUnmarshaller(u);
        }
    }
    public static <T> JAXBElement<T> unmarshall(JAXBContext c,
                                                XMLStreamReader reader,
                                                Class<T> cls) throws JAXBException {
        Unmarshaller u = c.createUnmarshaller();
        try {
            u.setEventHandler(null);
            return u.unmarshal(reader, cls);
        } finally {
            closeUnmarshaller(u);
        }
    }
    public static Object unmarshall(JAXBContext c,
                                    XMLStreamReader reader) throws JAXBException {
        Unmarshaller u = c.createUnmarshaller();
        try {
            u.setEventHandler(null);
            return u.unmarshal(reader);
        } finally {
            closeUnmarshaller(u);
        }
    }

    public static String builtInTypeToJavaType(String type) {
        return BUILTIN_DATATYPES_MAP.get(type);
    }

    public static Class<?> holderClass(String type) {
        return HOLDER_TYPES_MAP.get(type);
    }

    /**
     * Checks if the specified word is a Java keyword (as defined in JavaUtils).
     *
     * @param word the word to check.
     * @return true if the word is a keyword.
     * @see org.apache.cxf.helpers.JavaUtils
     */
    protected static boolean isJavaKeyword(String word) {
        return JavaUtils.isJavaKeyword(word);
    }

    /**
     * Generates a Java package name from a URI according to the
     * algorithm outlined in JAXB 2.0.
     *
     * @param namespaceURI the namespace URI.
     * @return the package name.
     */
    @FFDCIgnore(URISyntaxException.class)
    public static String namespaceURIToPackage(String namespaceURI) {
        try {
            return nameSpaceURIToPackage(new URI(namespaceURI));
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /**
     * Generates a Java package name from a URI according to the
     * algorithm outlined in Appendix D of JAXB (2.0+).
     *
     * @param uri the namespace URI.
     * @return the package name.
     */
    public static String nameSpaceURIToPackage(URI uri) {

        StringBuilder packageName = new StringBuilder();
        String authority = uri.getAuthority();
        String scheme = uri.getScheme();
        if (authority == null && "urn".equals(scheme)) {
            authority = uri.getSchemeSpecificPart();
        }

        if (null != authority && !"".equals(authority)) {
            if ("urn".equals(scheme)) {
                packageName.append(authority);
                /* JAXB 2.2 D.5.1, Rule #5 */
                for (int i = 0; i < packageName.length(); i++) {
                    if (packageName.charAt(i) == '-') {
                        packageName.setCharAt(i, '.');
                    }
                }
                authority = packageName.toString();
                packageName.setLength(0);

                StringTokenizer st = new StringTokenizer(authority, ":");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (packageName.length() > 0) {
                        packageName.insert(0, ".");
                        packageName.insert(0, normalizePackageNamePart(token));
                    } else {
                        packageName.insert(0, token);
                    }
                }
                authority = packageName.toString();
                packageName.setLength(0);

            }

            StringTokenizer st = new StringTokenizer(authority, ".");
            if (st.hasMoreTokens()) {
                String token = null;
                while (st.hasMoreTokens()) {
                    token = st.nextToken();
                    if (packageName.length() == 0) {
                        if ("www".equals(token)) {
                            continue;
                        }
                    } else {
                        packageName.insert(0, ".");
                    }
                    packageName.insert(0, normalizePackageNamePart(token));
                }
            }

            if (!("http".equalsIgnoreCase(scheme) || "urn".equalsIgnoreCase(scheme))) {
                packageName.insert(0, ".");
                packageName.insert(0, normalizePackageNamePart(scheme));
            }

        }

        String path = uri.getPath();
        if (path == null) {
            path = "";
        }
        /* JAXB 2.2 D.5.1 Rule 2 - remove trailing .??, .???, or .html only. */
        int index = path.lastIndexOf('.');
        if (index < 0) {
            index = path.length();
        } else {
            String ending = path.substring(index + 1);
            if (ending.length() < 2 || (ending.length() > 3
                && !"html".equalsIgnoreCase(ending))) {
                index = path.length();
            }
        }
        StringTokenizer st = new StringTokenizer(path.substring(0, index), "/");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (packageName.length() > 0) {
                packageName.append('.');
            }
            packageName.append(normalizePackageNamePart(token));
        }
        return packageName.toString();
    }

    private static String normalizePackageNamePart(String name) {
        StringBuilder sname = new StringBuilder(name.toLowerCase());

        for (int i = 0; i < sname.length(); i++) {
            sname.setCharAt(i, Character.toLowerCase(sname.charAt(i)));
        }

        for (int i = 0; i < sname.length(); i++) {
            if (!Character.isJavaIdentifierPart(sname.charAt(i))) {
                sname.setCharAt(i, '_');
            }
        }

        if (isJavaKeyword(sname.toString())) {
            sname.insert(0, '_');
        }

        if (!Character.isJavaIdentifierStart(sname.charAt(0))) {
            sname.insert(0, '_');
        }

        return sname.toString();
    }


    /**
     * Converts an XML name to a Java identifier according to the mapping
     * algorithm outlined in the JAXB specification
     *
     * @param name the XML name
     * @return the Java identifier
     */
    public static String nameToIdentifier(String name, IdentifierType type) {

        if (null == name || name.length() == 0) {
            return name;
        }

        // algorithm will not change an XML name that is already a legal and
        // conventional (!) Java class, method, or constant identifier

        boolean legalIdentifier = false;
        StringBuilder buf = new StringBuilder(name);
        boolean hasUnderscore = false;
        legalIdentifier = Character.isJavaIdentifierStart(buf.charAt(0));

        for (int i = 1; i < name.length() && legalIdentifier; i++) {
            legalIdentifier &= Character.isJavaIdentifierPart(buf.charAt(i));
            hasUnderscore |= '_' == buf.charAt(i);
        }

        boolean conventionalIdentifier = isConventionalIdentifier(buf, type);
        if (legalIdentifier && conventionalIdentifier) {
            if (JAXBUtils.isJavaKeyword(name) && type == IdentifierType.VARIABLE) {
                name = normalizePackageNamePart(name);
            }
            if (!hasUnderscore || IdentifierType.CLASS != type) {
                return name;
            }
        }

        // split into words

        List<String> words = new ArrayList<>();

        StringTokenizer st = new StringTokenizer(name, XML_NAME_PUNCTUATION_STRING);
        while (st.hasMoreTokens()) {
            words.add(st.nextToken());
        }

        for (int i = 0; i < words.size(); i++) {
            splitWord(words, i);
        }

        return makeConventionalIdentifier(words, type);
    }

    private static void splitWord(List<String> words, int listIndex) {
        String word = words.get(listIndex);
        if (word.length() <= 1) {
            return;
        }
        int index = listIndex + 1;
        StringBuilder sword = new StringBuilder(word);
        int first = 0;
        char firstChar = sword.charAt(first);
        if (Character.isLowerCase(firstChar)) {
            sword.setCharAt(first, Character.toUpperCase(firstChar));
        }
        int i = 1;

        while (i < sword.length()) {
            if (Character.isDigit(firstChar)) {
                while (i < sword.length() && Character.isDigit(sword.charAt(i))) {
                    i++;
                }
            } else if (isCasedLetter(firstChar)) {
                boolean previousIsLower = Character.isLowerCase(firstChar);
                while (i < sword.length() && isCasedLetter(sword.charAt(i))) {
                    if (Character.isUpperCase(sword.charAt(i)) && previousIsLower) {
                        break;
                    }
                    previousIsLower = Character.isLowerCase(sword.charAt(i));
                    i++;
                }
            } else {
                // first must be a mark or an uncased letter
                while (i < sword.length() && (isMark(sword.charAt(i)) || !isCasedLetter(sword.charAt(i)))) {
                    i++;
                }
            }

            // characters from first to i are all either
            // * digits
            // * upper or lower case letters, with only the first one an upper
            // * uncased letters or marks


            String newWord = sword.substring(first, i);
            words.add(index, newWord);
            index++;
            if (i >= sword.length()) {
                break;
            }
            first = i;
            firstChar = sword.charAt(first);
        }

        if (index > (listIndex + 1)) {
            words.remove(listIndex);
        }
    }

    private static boolean isMark(char c) {
        return Character.isJavaIdentifierPart(c) && !Character.isLetter(c) && !Character.isDigit(c);
    }

    private static boolean isCasedLetter(char c) {
        return Character.isUpperCase(c) || Character.isLowerCase(c);
    }

    private static boolean isConventionalIdentifier(StringBuilder buf, IdentifierType type) {
        if (null == buf || buf.length() == 0) {
            return false;
        }
        boolean result = false;
        if (IdentifierType.CONSTANT == type) {
            for (int i = 0; i < buf.length(); i++) {
                if (Character.isLowerCase(buf.charAt(i))) {
                    return false;
                }
            }
            result = true;
        } else if (IdentifierType.VARIABLE == type) {
            result = Character.isLowerCase(buf.charAt(0));
        } else {
            int pos = 3;
            if (IdentifierType.GETTER == type
                && !(buf.length() >= pos
                    && "get".equals(buf.subSequence(0, 3)))) {
                return false;
            } else if (IdentifierType.SETTER == type
                && !(buf.length() >= pos && "set".equals(buf.subSequence(0, 3)))) {
                return false;
            } else {
                pos = 0;
            }
            result = Character.isUpperCase(buf.charAt(pos));
        }
        return result;
    }

    private static String makeConventionalIdentifier(List<String> words, IdentifierType type) {
        StringBuilder buf = new StringBuilder();
        boolean firstWord = true;
        if (IdentifierType.GETTER == type) {
            buf.append("get");
        } else if (IdentifierType.SETTER == type) {
            buf.append("set");
        }
        for (String w : words) {
            int l = buf.length();
            if (l > 0 && IdentifierType.CONSTANT == type) {
                buf.append('_');
                l++;
            }
            buf.append(w);
            if (IdentifierType.CONSTANT == type) {
                for (int i = l; i < buf.length(); i++) {
                    if (Character.isLowerCase(buf.charAt(i))) {
                        buf.setCharAt(i, Character.toUpperCase(buf.charAt(i)));
                    }
                }
            } else if (IdentifierType.VARIABLE == type) {
                if (firstWord && Character.isUpperCase(buf.charAt(l))) {
                    buf.setCharAt(l, Character.toLowerCase(buf.charAt(l)));
                }
            } else {
                if (firstWord && Character.isLowerCase(buf.charAt(l))) {
                    buf.setCharAt(l, Character.toUpperCase(buf.charAt(l)));
                }
            }
            firstWord = false;
        }
        return buf.toString();
    }

    public static Class<?> getValidClass(Class<?> cls) {
        if (cls.isEnum()) {
            return cls;
        }
        if (cls.isArray()) {
            return cls;
        }

        if (cls == Object.class || cls == String.class
            || "javax.xml.ws.Holder".equals(cls.getName())) {
            cls = null;
        } else if (cls.isPrimitive() || cls.isAnnotation()) {
            cls = null;
        } else if (cls.isInterface()) {
            return cls;
        }
        if (cls != null) {
            if (cls.getName().equals("javax.xml.ws.wsaddressing.W3CEndpointReference")) {
                return cls;
            }
            Constructor<?> cons = ReflectionUtil.getDeclaredConstructor(cls);
            if (cons == null) {
                cons = ReflectionUtil.getConstructor(cls);
            }
            if (cons == null) {
                cls = null;
            }
        }
        return cls;
    }

    @FFDCIgnore({Exception.class, Exception.class})
    private static synchronized ClassLoader getXJCClassLoader() {
        if (jaxbXjcLoader == null) {
            try {
                Class.forName("com.sun.tools.internal.xjc.api.XJC");
                jaxbXjcLoader = ClassLoader.getSystemClassLoader();
            } catch (Exception t2) {
                //couldn't find either, probably cause tools.jar isn't on
                //the classpath.   Let's see if we can find the tools jar
                String s = SystemPropertyAction.getProperty("java.home");
                if (!StringUtils.isEmpty(s)) {
                    File home = new File(s);
                    File jar = new File(home, "lib/tools.jar");
                    if (!jar.exists()) {
                        jar = new File(home, "../lib/tools.jar");
                    }
                    if (jar.exists()) {
                        try {
                            jaxbXjcLoader = new URLClassLoader(new URL[] {jar.toURI().toURL()});
                            Class.forName("com.sun.tools.internal.xjc.api.XJC", false, jaxbXjcLoader);
                        } catch (Exception e) {
                            jaxbXjcLoader = null;
                        }
                    }
                }
            }
        }
        return jaxbXjcLoader;
    }

    public static Object setNamespaceMapper(final Map<String, String> nspref,
                                           Marshaller marshaller) throws PropertyException {
        Object mapper = createNamespaceWrapper(marshaller.getClass(), nspref);
        if (mapper != null) {
            if (marshaller.getClass().getName().contains(".internal.")) {
                marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper",
                                       mapper);
            } else if (marshaller.getClass().getName().contains("com.sun")) {
                marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
                                       mapper);
            } else if (marshaller.getClass().getName().contains("eclipse")) {
                marshaller.setProperty("eclipselink.namespace-prefix-mapper",
                                       mapper);
            }
        }
        return mapper;
    }
    @FFDCIgnore({ClassNotFoundException.class, Exception.class})
    public static BridgeWrapper createBridge(Set<Class<?>> ctxClasses,
                                      QName qname,
                                      Class<?> refcls,
                                      Annotation anns[]) throws JAXBException {
        try {
            Class<?> cls;
            Class<?> refClass;
            String pkg = "com.sun.xml.bind.";
            try {
                cls = Class.forName("com.sun.xml.bind.api.JAXBRIContext");
                refClass = Class.forName(pkg + "api.TypeReference");
            } catch (ClassNotFoundException e) {
                cls = Class.forName("com.sun.xml.internal.bind.api.JAXBRIContext", true, getXJCClassLoader());
                pkg = "com.sun.xml.internal.bind.";
                refClass = Class.forName(pkg + "api.TypeReference", true, getXJCClassLoader());
            }
            Object ref = refClass.getConstructor(QName.class,
                                                 Type.class,
                                                 anns.getClass()).newInstance(qname, refcls, anns);
            List<Object> typeRefs = new ArrayList<>();
            typeRefs.add(ref);
            List<Class<?>> clses = new ArrayList<>(ctxClasses);
            clses.add(refClass.getField("type").get(ref).getClass());
            if (!refcls.isInterface()) {
                clses.add(refcls);
            }

            Object ctx = null;
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals("newInstance")
                    && m.getParameterTypes().length == 6) {
                    ctx = m.invoke(null, clses.toArray(new Class<?>[0]),
                                         typeRefs, null, null, true, null);

                }
            }

            if (ctx == null) {
                throw new JAXBException("No ctx found");
            }

            Object bridge = ctx.getClass().getMethod("createBridge", refClass).invoke(ctx, ref);
            return ReflectionInvokationHandler.createProxyWrapper(bridge,
                                                                  BridgeWrapper.class);
        } catch (Exception ex) {
            throw new JAXBException(ex);
        }
    }
    public interface BridgeWrapper {

        Object unmarshal(XMLStreamReader source, AttachmentUnmarshaller am) throws JAXBException;

        Object unmarshal(InputStream source) throws JAXBException;

        Object unmarshal(Node source, AttachmentUnmarshaller am) throws JAXBException;

        void marshal(Object elValue, XMLStreamWriter source, AttachmentMarshaller m) throws JAXBException;

        void marshal(Object elValue, StreamResult s1) throws JAXBException;

        void marshal(Object elValue, Node source, AttachmentMarshaller am) throws JAXBException;
    }


    @FFDCIgnore({Throwable.class, Exception.class})
    public static SchemaCompiler createSchemaCompiler() throws JAXBException {
        try {
            Class<?> cls;
            Object sc = null;
            try {
                cls = Class.forName("com.sun.tools.xjc.api.XJC");
                sc = cls.getMethod("createSchemaCompiler").invoke(null);
            } catch (Throwable e) {
                cls = Class.forName("com.sun.tools.internal.xjc.api.XJC", true, getXJCClassLoader());
                sc = cls.getMethod("createSchemaCompiler").invoke(null);
            }

            return ReflectionInvokationHandler.createProxyWrapper(sc,
                                                                  SchemaCompiler.class);
        } catch (Exception ex) {
            throw new JAXBException(ex);
        }
    }

    @FFDCIgnore(JAXBException.class)
    public static SchemaCompiler createSchemaCompilerWithDefaultAllocator(Set<String> allocatorSet) {

        try {
            SchemaCompiler compiler = JAXBUtils.createSchemaCompiler();
            Object allocator = ReflectionInvokationHandler
                .createProxyWrapper(new DefaultClassNameAllocator(allocatorSet),
                                JAXBUtils.getParamClass(compiler, "setClassNameAllocator"));

            compiler.setClassNameAllocator(allocator);
            return compiler;
        } catch (JAXBException e1) {
            throw new IllegalStateException("Unable to create schema compiler", e1);
        }

    }

    public static void logGeneratedClassNames(Logger logger, JCodeModel codeModel) {
        if (!logger.isLoggable(Level.FINE)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Iterator<JPackage> itr = codeModel.packages(); itr.hasNext();) {
            JPackage package1 = itr.next();

            for (Iterator<JDefinedClass> citr = package1.classes(); citr.hasNext();) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(citr.next().fullName());
            }
        }

        logger.log(Level.FINE, "Created classes: " + sb.toString());
    }

    public static List<String> getGeneratedClassNames(JCodeModel codeModel) {
        List<String> classes = new ArrayList<>();
        for (Iterator<JPackage> itr = codeModel.packages(); itr.hasNext();) {
            JPackage package1 = itr.next();

            for (Iterator<JDefinedClass> citr = package1.classes(); citr.hasNext();) {
                classes.add(citr.next().fullName());
            }
        }
        return classes;
    }
    public static Object createFileCodeWriter(File f) throws JAXBException {
        return createFileCodeWriter(f, StandardCharsets.UTF_8.name());
    }
    @FFDCIgnore({ClassNotFoundException.class, Exception.class, Exception.class})
    public static Object createFileCodeWriter(File f, String encoding) throws JAXBException {
        try {
            Class<?> cls;
            try {
                cls = Class.forName("com.sun.codemodel.writer.FileCodeWriter");
            } catch (ClassNotFoundException e) {
                cls = Class.forName("com.sun.codemodel.internal.writer.FileCodeWriter",
                                    true, getXJCClassLoader());
            }
            if (encoding != null) {
                try {
                    return cls.getConstructor(File.class, String.class)
                              .newInstance(f, encoding);
                } catch (Exception ex) {
                    // try a single argument constructor
                }
            }
            return cls.getConstructor(File.class).newInstance(f);
        } catch (Exception ex) {
            throw new JAXBException(ex);
        }
    }

    public static Class<?> getParamClass(SchemaCompiler sc, String method) {
        Object o = ((ReflectionInvokationHandler)Proxy.getInvocationHandler(sc)).getTarget();
        for (Method m : o.getClass().getMethods()) {
            if (m.getName().equals(method) && m.getParameterTypes().length == 1) {
                return m.getParameterTypes()[0];
            }
        }
        return null;
    }


    public static List<DOMResult> generateJaxbSchemas(
        JAXBContext context, final Map<String, DOMResult> builtIns) throws IOException {
        final List<DOMResult> results = new ArrayList<>();

        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String ns, String file) throws IOException {
                DOMResult result = new DOMResult();

                if (builtIns.containsKey(ns)) {
                    DOMResult dr = builtIns.get(ns);
                    result.setSystemId(dr.getSystemId());
                    results.add(dr);
                    return result;
                }
                result.setSystemId(file);
                results.add(result);
                return result;
            }
        });
        return results;
    }

    public static String getPackageNamespace(Class<?> cls) {
        Package p = cls.getPackage();
        if (p != null) {
            javax.xml.bind.annotation.XmlSchema schemaAnn =
                p.getAnnotation(javax.xml.bind.annotation.XmlSchema.class);
            if (schemaAnn != null) {
                return schemaAnn.namespace();
            }
        }
        return null;
    }

    public static void scanPackages(Set<Class<?>> classes,
                                    Map<Package, CachedClass> objectFactoryCache) {
        scanPackages(classes, null, objectFactoryCache);
    }
    @FFDCIgnore({ClassNotFoundException.class, Exception.class, Exception.class, Exception.class})
    public static void scanPackages(Set<Class<?>> classes,
                                    Class<?>[] extraClass,
                                    Map<Package, CachedClass> objectFactoryCache) {

        // add user extra class into jaxb context
        if (extraClass != null && extraClass.length > 0) {
            for (Class<?> clz : extraClass) {
                classes.add(clz);
            }
        }

        // try and read any jaxb.index files that are with the other classes.
        // This should
        // allow loading of extra classes (such as subclasses for inheritance
        // reasons)
        // that are in the same package. Also check for ObjectFactory classes
        Map<String, InputStream> packages = new HashMap<>();
        Map<String, ClassLoader> packageLoaders = new HashMap<>();
        Set<Class<?>> objectFactories = new HashSet<>();
        for (Class<?> jcls : classes) {
            String pkgName = PackageUtils.getPackageName(jcls);
            if (!packages.containsKey(pkgName)) {
                Package pkg = jcls.getPackage();

                packages.put(pkgName, jcls.getResourceAsStream("jaxb.index"));
                packageLoaders.put(pkgName, getClassLoader(jcls));
                String objectFactoryClassName = pkgName + "." + "ObjectFactory";
                Class<?> ofactory = null;
                CachedClass cachedFactory = null;
                if (pkg != null && objectFactoryCache != null) {
                    synchronized (objectFactoryCache) {
                        cachedFactory = objectFactoryCache.get(pkg);
                    }
                }
                if (cachedFactory != null) {
                    ofactory = cachedFactory.getCachedClass();
                }
                if (ofactory == null) {
                    try {
                        ofactory = Class.forName(objectFactoryClassName, false, getClassLoader(jcls));
                        objectFactories.add(ofactory);
                        addToObjectFactoryCache(pkg, ofactory, objectFactoryCache);
                    } catch (ClassNotFoundException e) {
                        addToObjectFactoryCache(pkg, null, objectFactoryCache);
                    }
                } else {
                    objectFactories.add(ofactory);
                }
            }
        }
        for (Map.Entry<String, InputStream> entry : packages.entrySet()) {
            if (entry.getValue() != null) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(entry.getValue(), StandardCharsets.UTF_8));
                    String pkg = entry.getKey();
                    ClassLoader loader = packageLoaders.get(pkg);
                    if (!StringUtils.isEmpty(pkg)) {
                        pkg += ".";
                    }

                    String line = reader.readLine();
                    while (line != null) {
                        line = line.trim();
                        if (line.indexOf("#") != -1) {
                            line = line.substring(0, line.indexOf("#"));
                        }
                        if (!StringUtils.isEmpty(line)) {
                            try {
                                Class<?> ncls = Class.forName(pkg + line, false, loader);
                                classes.add(ncls);
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                        line = reader.readLine();
                    }
                } catch (Exception e) {
                    // ignore
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                            entry.getValue().close();
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        classes.addAll(objectFactories);
    }

    private static ClassLoader getClassLoader(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        }
        return clazz.getClassLoader();
    }
       
    private static void addToObjectFactoryCache(Package objectFactoryPkg, 
                                         Class<?> ofactory,
                                         Map<Package, CachedClass> objectFactoryCache) {
        if (objectFactoryPkg == null || objectFactoryCache == null) {
            return;
        }
        synchronized (objectFactoryCache) {
            objectFactoryCache.put(objectFactoryPkg,
                                     new CachedClass(ofactory));
        }
    }

    public static class DefaultClassNameAllocator {
        private final Set<String> typesClassNames;

        public DefaultClassNameAllocator() {
            this(new HashSet<>());
        }

        public DefaultClassNameAllocator(Set<String> set) {
            typesClassNames = set;
        }

        public String assignClassName(String packageName, String className) {
            String fullClassName = className;
            String fullPckClass = packageName + "." + fullClassName;
            int cnt = 0;
            while (typesClassNames.contains(fullPckClass)) {
                cnt++;
                fullClassName = className + cnt;
                fullPckClass = packageName + "." + fullClassName;
            }
            typesClassNames.add(fullPckClass);
            return fullClassName;
        }

    }

    public interface SchemaCompiler {
        void setEntityResolver(EntityResolver entityResolver);

        void setErrorListener(Object elForRun);

        void setClassNameAllocator(Object allocator);

        @WrapReturn(S2JJAXBModel.class)
        S2JJAXBModel bind();

        void parseSchema(InputSource source);

        void parseSchema(String key, Element el);
        void parseSchema(String key, XMLStreamReader el);

        @WrapReturn(Options.class)
        Options getOptions();
    }
    public interface S2JJAXBModel {

        @WrapReturn(JCodeModel.class)
        JCodeModel generateCode(Object object, Object elForRun);

        @WrapReturn(Mapping.class)
        Mapping get(QName qn);

        @WrapReturn(TypeAndAnnotation.class)
        TypeAndAnnotation getJavaType(QName typeQName);
    }
    public interface Mapping {
        @WrapReturn(TypeAndAnnotation.class)
        TypeAndAnnotation getType();
    }
    public interface TypeAndAnnotation {
        @WrapReturn(JType.class)
        JType getTypeClass();
    }
    public interface JType {
        boolean isArray();

        @WrapReturn(JType.class)
        JType elementType();

        boolean isPrimitive();

        String binaryName();

        String fullName();

        String name();

        @WrapReturn(value = JType.class, iterator = true)
        Iterator<JType> classes();
    }
    public interface Options {

        void addGrammar(InputSource is);

        void addBindFile(InputSource is);

        void parseArguments(String[] args);

        String getBuildID();
    }
    public interface JCodeModel {

        void build(Object writer) throws IOException;

        @WrapReturn(value = JPackage.class, iterator = true)
        Iterator<JPackage> packages();
    }
    public interface JPackage {

        String name();

        @WrapReturn(value = JDefinedClass.class, iterator = true)
        Iterator<JDefinedClass> classes();
    }
    public interface JDefinedClass {
        String name();

        String fullName();
    }

    public static boolean isJAXB22() {
        Target t = XmlElement.class.getAnnotation(Target.class);
        //JAXB 2.2 allows XmlElement on params.
        for (ElementType et : t.value()) {
            if (et == ElementType.PARAMETER) {
                return true;
            }
        }
        return false;
    }

    @FFDCIgnore({RuntimeException.class, Throwable.class, Exception.class})
    private static synchronized Object createNamespaceWrapper(Class<?> mcls, Map<String, String> map) {
        String postFix = "";
        if (mcls.getName().contains("eclipse")) {
            return createEclipseNamespaceMapper(mcls, map);
        } else if (mcls.getName().contains(".internal")) {
            postFix = "Internal";
        } else if (mcls.getName().contains("com.sun")) {
            postFix = "RI";
        }
        ASMHelper helper = new ASMHelper();
        String className = "org.apache.cxf.jaxb.NamespaceMapper";
        className += postFix;
        Class<?> cls = helper.findClass(className, JAXBUtils.class);
        Throwable t = null;
        if (cls == null) {
            try {
                ClassWriter cw = helper.createClassWriter();
                if (cw != null) {
                    cls = createNamespaceWrapperInternal(helper, cw, postFix, mcls);
                }
            } catch (RuntimeException ex) {
                // continue
                t = ex;
            }
        }
        if (cls == null
            && (!mcls.getName().contains(".internal.") && mcls.getName().contains("com.sun"))) {
            try {
                cls = ClassLoaderUtils.loadClass("org.apache.cxf.common.jaxb.NamespaceMapper",
                                                 JAXBUtils.class);
            } catch (Throwable ex2) {
                // ignore
                t = ex2;
            }
        }
        if (cls != null) {
            try {
                return cls.getConstructor(Map.class).newInstance(map);
            } catch (Exception e) {
                // ignore
                t = e;
            }
        }
        LOG.log(Level.INFO, "Could not create a NamespaceMapper compatible with Marshaller class " + mcls.getName(), t);
        return null;
    }
    /*
    // This is the "prototype" for the ASM generated class below
    public static class MapNamespacePrefixMapper2
        extends org.eclipse.persistence.internal.oxm.record.namespaces.MapNamespacePrefixMapper {

        String[] nsctxt;

        public MapNamespacePrefixMapper2(Map<String, String> foo) {
            super(foo);
        }
        public String[] getPreDeclaredNamespaceUris() {
            String[] sup = super.getPreDeclaredNamespaceUris();
            if (nsctxt == null) {
                return sup;
            }
            List<String> s = new ArrayList<>(Arrays.asList(sup));
            for (int x = 1; x < nsctxt.length; x = x + 2) {
                s.remove(nsctxt[x]);
            }
            return s.toArray(new String[s.size()]);
        }
        public void setContextualNamespaceDecls(String[] f) {
            nsctxt = f;
        }
        public String[] getContextualNamespaceDecls() {
            return nsctxt;
        }
    }
    */
    //CHECKSTYLE:OFF
    //bunch of really long ASM based methods that cannot be shortened easily
    @FFDCIgnore(Throwable.class)
    private static Object createEclipseNamespaceMapper(Class<?> mcls, Map<String, String> map) {
        ASMHelper helper = new ASMHelper();
        String className = "org.apache.cxf.jaxb.EclipseNamespaceMapper";
        String slashedName = "org/apache/cxf/jaxb/EclipseNamespaceMapper";
        Class<?> cls = helper.findClass(className, JAXBUtils.class);

        if (cls == null) {
            ClassWriter cw = helper.createClassWriter();
            if (cw == null) {
                return null;
            }
            String superName = "org/eclipse/persistence/internal/oxm/record/namespaces/MapNamespacePrefixMapper";
            FieldVisitor fv;
            MethodVisitor mv;
            cw.visit(Opcodes.V1_6,
                     Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                     slashedName, null,
                     superName, null);

            cw.visitSource("EclipseNamespaceMapper.java", null);

            fv = cw.visitField(Opcodes.ACC_PRIVATE, "nsctxt", "[Ljava/lang/String;", null, null);
            fv.visitEnd();


            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/util/Map;)V",
                                "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;)V", null);
            mv.visitCode();
            Label l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                               superName, "<init>", "(Ljava/util/Map;)V", false);
            Label l1 = helper.createLabel();
            mv.visitLabel(l1);
            mv.visitInsn(Opcodes.RETURN);
            Label l2 = helper.createLabel();
            mv.visitLabel(l2);
            mv.visitMaxs(2, 2);
            mv.visitEnd();


            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "setContextualNamespaceDecls", "([Ljava/lang/String;)V",
                                null, null);
            mv.visitCode();
            l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(47, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
            l1 = helper.createLabel();
            mv.visitLabel(l1);
            mv.visitLineNumber(48, l1);
            mv.visitInsn(Opcodes.RETURN);
            l2 = helper.createLabel();
            mv.visitLabel(l2);
            mv.visitLocalVariable("this", "L" + slashedName + ";", null, l0, l2, 0);
            mv.visitLocalVariable("contextualNamespaceDecls", "[Ljava/lang/String;", null, l0, l2, 1);
            mv.visitMaxs(2, 2);
            mv.visitEnd();

            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getContextualNamespaceDecls", "()[Ljava/lang/String;", null, null);
            mv.visitCode();
            l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(51, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
            mv.visitInsn(Opcodes.ARETURN);
            l1 = helper.createLabel();

            mv.visitLabel(l1);
            mv.visitLocalVariable("this", "L" + slashedName + ";", null, l0, l1, 0);

            mv.visitMaxs(1, 1);
            mv.visitEnd();


            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getPreDeclaredNamespaceUris", "()[Ljava/lang/String;", null, null);
            mv.visitCode();
            l0 = helper.createLabel();
            mv.visitLabel(l0);
            mv.visitLineNumber(1036, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                               superName,
                               "getPreDeclaredNamespaceUris", "()[Ljava/lang/String;", false);
            mv.visitVarInsn(Opcodes.ASTORE, 1);
            l1 = helper.createLabel();
            mv.visitLabel(l1);
            mv.visitLineNumber(1037, l1);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
            l2 = helper.createLabel();
            mv.visitJumpInsn(Opcodes.IFNONNULL, l2);
            Label l3 = helper.createLabel();
            mv.visitLabel(l3);
            mv.visitLineNumber(1038, l3);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(l2);
            mv.visitLineNumber(1040, l2);
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] {"[Ljava/lang/String;"}, 0, null);
            mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Arrays", "asList",
                               "([Ljava/lang/Object;)Ljava/util/List;", false);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "(Ljava/util/Collection;)V", false);
            mv.visitVarInsn(Opcodes.ASTORE, 2);
            Label l4 = helper.createLabel();
            mv.visitLabel(l4);
            mv.visitLineNumber(1041, l4);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitVarInsn(Opcodes.ISTORE, 3);
            Label l5 = helper.createLabel();
            mv.visitLabel(l5);
            Label l6 = helper.createLabel();
            mv.visitJumpInsn(Opcodes.GOTO, l6);
            Label l7 = helper.createLabel();
            mv.visitLabel(l7);
            mv.visitLineNumber(1042, l7);
            mv.visitFrame(Opcodes.F_APPEND, 2, new Object[] {"java/util/List", Opcodes.INTEGER}, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, slashedName, "nsctxt", "[Ljava/lang/String;");
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitInsn(Opcodes.AALOAD);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "remove", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(Opcodes.POP);
            Label l8 = helper.createLabel();
            mv.visitLabel(l8);
            mv.visitLineNumber(1041, l8);
            mv.visitIincInsn(3, 2);
            mv.visitLabel(l6);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD,
                              slashedName,
                              "nsctxt", "[Ljava/lang/String;");
            mv.visitInsn(Opcodes.ARRAYLENGTH);
            mv.visitJumpInsn(Opcodes.IF_ICMPLT, l7);
            Label l9 = helper.createLabel();
            mv.visitLabel(l9);
            mv.visitLineNumber(1044, l9);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true);
            mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List",
                               "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", true);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "[Ljava/lang/String;");
            mv.visitInsn(Opcodes.ARETURN);
            Label l10 = helper.createLabel();
            mv.visitLabel(l10);
            mv.visitLocalVariable("this", "L" + slashedName + ";",
                                  null, l0, l10, 0);
            mv.visitLocalVariable("sup", "[Ljava/lang/String;", null, l1, l10, 1);
            mv.visitLocalVariable("s", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/String;>;", l4, l10, 2);
            mv.visitLocalVariable("x", "I", null, l5, l9, 3);
            mv.visitMaxs(3, 4);
            mv.visitEnd();

            cw.visitEnd();

            byte[] bts = cw.toByteArray();
            cls = helper.loadClass(className,
                                   mcls, bts);
        }
        try {
            return cls.getConstructor(Map.class).newInstance(map);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Class<?> createNamespaceWrapperInternal(ASMHelper helper, ClassWriter cw,
                                                           String postFix, Class<?> ref) {
        String className = "org.apache.cxf.jaxb.NamespaceMapper" + postFix;
        boolean useJdkJaxb = isJdkJaxbAvailable() && !"RI".equals(postFix); //Liberty change
        String superName = "com/sun/xml/"
            + (useJdkJaxb ? "internal/" : "")
            + "bind/marshaller/NamespacePrefixMapper";
        String postFixedName = "org/apache/cxf/jaxb/NamespaceMapper" + postFix;

        FieldVisitor fv;
        MethodVisitor mv;

        cw.visit(Opcodes.V1_6,
                 Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                 postFixedName, null,
                 superName, null);

        cw.visitSource("NamespaceMapper.java", null);

        fv = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL,
                           "nspref", "Ljava/util/Map;",
                           "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;", null);
        fv.visitEnd();

        fv = cw.visitField(Opcodes.ACC_PRIVATE, "nsctxt", "[Ljava/lang/String;", null, null);
        fv.visitEnd();

        fv = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC,
                           "EMPTY_STRING", "[Ljava/lang/String;", null, null);
        fv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        Label l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(30, l0);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
        mv.visitFieldInsn(Opcodes.PUTSTATIC, postFixedName, "EMPTY_STRING", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                            "(Ljava/util/Map;)V",
                            "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;)V", null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(32, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        Label l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(29, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETSTATIC, postFixedName, "EMPTY_STRING", "[Ljava/lang/String;");
        mv.visitFieldInsn(Opcodes.PUTFIELD, postFixedName, "nsctxt", "[Ljava/lang/String;");
        Label l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLineNumber(33, l2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, postFixedName, "nspref", "Ljava/util/Map;");
        Label l3 = helper.createLabel();
        mv.visitLabel(l3);
        mv.visitLineNumber(34, l3);
        mv.visitInsn(Opcodes.RETURN);
        Label l4 = helper.createLabel();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l4, 0);
        mv.visitLocalVariable("nspref",
                              "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
                              l0, l4, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getPreferredPrefix",
                            "(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;",
                            null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(39, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, postFixedName, "nspref", "Ljava/util/Map;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map",
                           "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(40, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        l2 = helper.createLabel();
        mv.visitJumpInsn(Opcodes.IFNULL, l2);
        l3 = helper.createLabel();
        mv.visitLabel(l3);
        mv.visitLineNumber(41, l3);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(l2);
        mv.visitLineNumber(43, l2);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] {"java/lang/String"}, 0, null);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
        l4 = helper.createLabel();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l4, 0);
        mv.visitLocalVariable("namespaceUri", "Ljava/lang/String;", null, l0, l4, 1);
        mv.visitLocalVariable("suggestion", "Ljava/lang/String;", null, l0, l4, 2);
        mv.visitLocalVariable("requirePrefix", "Z", null, l0, l4, 3);
        mv.visitLocalVariable("prefix", "Ljava/lang/String;", null, l1, l4, 4);
        mv.visitMaxs(2, 5);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "setContextualNamespaceDecls", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(47, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, postFixedName, "nsctxt", "[Ljava/lang/String;");
        l1 = helper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(48, l1);
        mv.visitInsn(Opcodes.RETURN);
        l2 = helper.createLabel();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l2, 0);
        mv.visitLocalVariable("contextualNamespaceDecls", "[Ljava/lang/String;", null, l0, l2, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "getContextualNamespaceDecls", "()[Ljava/lang/String;", null, null);
        mv.visitCode();
        l0 = helper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(51, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, postFixedName, "nsctxt", "[Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARETURN);
        l1 = helper.createLabel();

        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + postFixedName + ";", null, l0, l1, 0);

        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();

        byte[] bts = cw.toByteArray();
        return helper.loadClass(className,
                                ref, bts);
    }
    //CHECKSTYLE:ON

    public static JAXBContextProxy createJAXBContextProxy(final JAXBContext ctx) {
        return createJAXBContextProxy(ctx, null, null);
    }
    public static JAXBContextProxy createJAXBContextProxy(final JAXBContext ctx,
                                                          final SchemaCollection collection,
                                                          final String defaultNs) {
        if (ctx.getClass().getName().contains("com.sun.")
            || collection == null) {
            return ReflectionInvokationHandler.createProxyWrapper(ctx, JAXBContextProxy.class);
        }
        return new SchemaCollectionContextProxy(ctx, collection, defaultNs);
    }
    public static JAXBBeanInfo getBeanInfo(JAXBContextProxy context, Class<?> cls) {
        Object o = context.getBeanInfo(cls);
        if (o == null) {
            return null;
        }
        if (o instanceof JAXBBeanInfo) {
            return (JAXBBeanInfo)o;
        }
        return ReflectionInvokationHandler.createProxyWrapper(o, JAXBBeanInfo.class);
    }

    private static String getPostfix(Class<?> cls) {
        String className = cls.getName();
        if (!isJdkJaxbAvailable() && 
            (className.contains("com.sun.xml.internal")
             || className.contains("eclipse"))) {
            //eclipse moxy accepts sun package CharacterEscapeHandler 
            return ".internal";
        } else if (className.contains("com.sun.xml.bind")
                   || className.startsWith("com.ibm.xml")) { //Liberty change) {
            return "";
        }
        return null;
    }

    public static void setMinimumEscapeHandler(Marshaller marshaller) {
        if (jaxbMinimumEscapeHandler == null) {
            jaxbMinimumEscapeHandler = Optional.ofNullable(createMininumEscapeHandler(marshaller.getClass()));
        }
        jaxbMinimumEscapeHandler.ifPresent(p -> setEscapeHandler(marshaller, p));
    }

    public static void setNoEscapeHandler(final Marshaller marshaller) {
        if (jaxbNoEscapeHandler == null) {
            jaxbNoEscapeHandler = Optional.ofNullable(createNoEscapeHandler(marshaller.getClass()));
        }
        jaxbNoEscapeHandler.ifPresent(p -> setEscapeHandler(marshaller, p));
    }

    @FFDCIgnore(PropertyException.class)
    public static void setEscapeHandler(Marshaller marshaller, Object escapeHandler) {
        try {
            String postFix = getPostfix(marshaller.getClass());
            if (postFix != null) {
                marshaller.setProperty("com.sun.xml" + postFix + ".bind.characterEscapeHandler", escapeHandler);
            }
        } catch (PropertyException e) {
            LOG.log(Level.INFO, "Failed to set MinumEscapeHandler to jaxb marshaller", e);
        }
    }

    public static Object createMininumEscapeHandler(Class<?> cls) {
        return createEscapeHandler(cls, "MinimumEscapeHandler");
    }

    public static Object createNoEscapeHandler(Class<?> cls) {
        return createEscapeHandler(cls, "NoEscapeHandler");
    }

    @FFDCIgnore(Exception.class)
    private static Object createEscapeHandler(Class<?> cls, String simpleClassName) {
        try {
            String postFix = getPostfix(cls);
            if (postFix == null) {
                LOG.log(Level.WARNING, "Failed to create" + simpleClassName + " for unknown jaxb class:"
                    + cls);
                return null;
            }
            Class<?> handlerClass = ClassLoaderUtils.loadClass("com.sun.xml" + postFix
                                                                   + ".bind.marshaller." + simpleClassName,
                                                               cls);
            Class<?> handlerInterface = ClassLoaderUtils
                .loadClass("com.sun.xml" + postFix + ".bind.marshaller.CharacterEscapeHandler",
                           cls);
            Object targetHandler = ReflectionUtil.getDeclaredField(handlerClass, "theInstance").get(null);
            return ProxyHelper.getProxy(cls.getClassLoader(),
                                        new Class[] {handlerInterface},
                                        new EscapeHandlerInvocationHandler(targetHandler));
        } catch (Exception e) {
            if ("NoEscapeHandler".equals(simpleClassName)) {
                //this class doesn't exist in JAXB 2.2 so expected
                LOG.log(Level.FINER, "Failed to create " + simpleClassName);
            } else {
                LOG.log(Level.INFO, "Failed to create " + simpleClassName);
            }
        }
        return null;
    }
    
    private static boolean isJdkJaxbAvailable() {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                // JAX-B is only available in the JDK in JDK 1.7 and 1.8, but not in JDK 9+
                return System.getProperty("java.specification.version").startsWith("1.");
            }
        });
    }

}

