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

package org.apache.cxf.helpers;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.message.Message;

public final class ServiceUtils {
    
    private ServiceUtils() {
    }
    
    /**
     * A short cut method to be able to test for if Schema Validation should be enabled
     * for IN or OUT without having to check BOTH and IN or OUT.
     * 
     * @param message
     * @param type
     */
    public static boolean isSchemaValidationEnabled(SchemaValidationType type, Message message) {
        SchemaValidationType messageType = getSchemaValidationType(message);
        
        return messageType.equals(type) 
            || ((SchemaValidationType.IN.equals(type) || SchemaValidationType.OUT.equals(type))
                && SchemaValidationType.BOTH.equals(messageType));
    }
    
    /**
     * Determines the appropriate SchemaValidationType to return based on either
     * a Boolean (for backwards compatibility) or the selected Schema Validation Type.
     * 
     * Package private as the isSchemaValidationEnabled method should be used instead.  Only
     * visible for easier testing
     * 
     * @param message
     */
    static SchemaValidationType getSchemaValidationType(Message message) {
        Object obj = message.getContextualProperty(Message.SCHEMA_VALIDATION_ENABLED);
        if (obj instanceof SchemaValidationType) {
            return (SchemaValidationType)obj;
        } else if (obj != null) { 
            String value = obj.toString().toUpperCase(); // handle boolean values as well
            if ("TRUE".equals(value)) {
                return SchemaValidationType.BOTH;
            } else if ("FALSE".equals(value)) {
                return SchemaValidationType.NONE;
            } else if (value.length() > 0) {
                return SchemaValidationType.valueOf(value);
            }
        }
        
        // fall through default value
        return SchemaValidationType.NONE;
    }
    
    /**
     * Generates a suitable service name from a given class. The returned name
     * is the simple name of the class, i.e. without the package name.
     * 
     * @param clazz the class.
     * @return the name.
     */
    public static String makeServiceNameFromClassName(Class<?> clazz) {
        String name = clazz.getName();
        int last = name.lastIndexOf(".");
        if (last != -1) {
            name = name.substring(last + 1);
        }

        int inner = name.lastIndexOf("$");
        if (inner != -1) {
            name = name.substring(inner + 1);
        }

        return name;
    }

    public static QName makeQualifiedNameFromClass(Class<?> clazz) {
        String namespace = makeNamespaceFromClassName(clazz.getName(), "http");
        String localPart = makeServiceNameFromClassName(clazz);
        return new QName(namespace, localPart);
    }

    public static String getMethodName(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getDeclaringClass().getName());
        sb.append('.');
        sb.append(m.getName());
        sb.append('(');
        Class<?>[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            Class<?> param = params[i];
            sb.append(param.getName());
            if (i < params.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Generates the name of a XML namespace from a given class name and
     * protocol. The returned namespace will take the form
     * <code>protocol://domain</code>, where <code>protocol</code> is the
     * given protocol, and <code>domain</code> the inversed package name of
     * the given class name. <p/> For instance, if the given class name is
     * <code>org.codehaus.xfire.services.Echo</code>, and the protocol is
     * <code>http</code>, the resulting namespace would be
     * <code>http://services.xfire.codehaus.org</code>.
     * 
     * @param className the class name
     * @param protocol the protocol (eg. <code>http</code>)
     * @return the namespace
     */
    public static String makeNamespaceFromClassName(String className, String protocol) {
        int index = className.lastIndexOf(".");

        if (index == -1) {
            return protocol + "://" + "DefaultNamespace";
        }

        String packageName = className.substring(0, index);

        StringTokenizer st = new StringTokenizer(packageName, ".");
        String[] words = new String[st.countTokens()];

        for (int i = 0; i < words.length; ++i) {
            words[i] = st.nextToken();
        }

        StringBuilder sb = new StringBuilder(80);

        for (int i = words.length - 1; i >= 0; --i) {
            String word = words[i];

            // seperate with dot
            if (i != words.length - 1) {
                sb.append('.');
            }

            sb.append(word);
        }

        return protocol + "://" + sb.toString() + "/";
    }

    /**
     * Method makePackageName
     * 
     * @param namespace
     */
    public static String makePackageName(String namespace) {

        String hostname = null;
        String path = "";

        // get the target namespace of the document
        try {
            URL u = new URL(namespace);

            hostname = u.getHost();
            path = u.getPath();
        } catch (MalformedURLException e) {
            if (namespace.indexOf(":") > -1) {
                hostname = namespace.substring(namespace.indexOf(":") + 1);

                if (hostname.indexOf("/") > -1) {
                    hostname = hostname.substring(0, hostname.indexOf("/"));
                }
            } else {
                hostname = namespace;
            }
        }

        // if we didn't file a hostname, bail
        if (hostname == null) {
            return null;
        }

        // convert illegal java identifier
        hostname = hostname.replace('-', '_');
        path = path.replace('-', '_');

        // chomp off last forward slash in path, if necessary
        if ((path.length() > 0) && (path.charAt(path.length() - 1) == '/')) {
            path = path.substring(0, path.length() - 1);
        }

        // tokenize the hostname and reverse it
        StringTokenizer st = new StringTokenizer(hostname, ".:");
        String[] words = new String[st.countTokens()];

        for (int i = 0; i < words.length; ++i) {
            words[i] = st.nextToken();
        }

        StringBuilder sb = new StringBuilder(namespace.length());

        for (int i = words.length - 1; i >= 0; --i) {
            addWordToPackageBuffer(sb, words[i], i == words.length - 1);
        }

        // tokenize the path
        StringTokenizer st2 = new StringTokenizer(path, "/");

        while (st2.hasMoreTokens()) {
            addWordToPackageBuffer(sb, st2.nextToken(), false);
        }

        return sb.toString();
    }

    /**
     * Massage <tt>word</tt> into a form suitable for use in a Java package
     * name. Append it to the target string buffer with a <tt>.</tt> delimiter
     * iff <tt>word</tt> is not the first word in the package name.
     * 
     * @param sb the buffer to append to
     * @param word the word to append
     * @param firstWord a flag indicating whether this is the first word
     */
    private static void addWordToPackageBuffer(StringBuilder sb, String word, boolean firstWord) {

        if (JavaUtils.isJavaKeyword(word)) {
            word = JavaUtils.makeNonJavaKeyword(word);
        }

        // separate with dot after the first word
        if (!firstWord) {
            sb.append('.');
        }

        // prefix digits with underscores
        if (Character.isDigit(word.charAt(0))) {
            sb.append('_');
        }

        // replace periods with underscores
        if (word.indexOf('.') != -1) {
            char[] buf = word.toCharArray();

            for (int i = 0; i < word.length(); i++) {
                if (buf[i] == '.') {
                    buf[i] = '_';
                }
            }

            word = new String(buf);
        }

        sb.append(word);
    }

}
