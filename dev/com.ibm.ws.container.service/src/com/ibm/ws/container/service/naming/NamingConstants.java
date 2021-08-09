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
package com.ibm.ws.container.service.naming;

import java.util.Arrays;
import java.util.Comparator;

import com.ibm.websphere.ras.annotation.Trivial;

public interface NamingConstants {

    // **************************************************
    // START KEY CONSTANTS
    // The following constants are for the StringRefAddr
    // objects containing environment attributes for the
    // Reference objects constructed by helpers
    // **************************************************

    /**
     * A string representing the key used for the JNDI resource name when
     * building a {@link Reference} or searching the SR for a resource.
     */
    public static final String JNDI_NAME_KEY = "jndi-name";
    /**
     * Constant for the resource-ref/res-auth entry
     */
    public static final String RES_REF_AUTH = "res-ref-auth";
    /**
     * Constant for the resource-ref:res-sharing-scope entry
     */
    public static final String RES_REF_SHARING = "res-ref-sharing";
    // **************************************************
    // END KEY CONSTANTS
    // **************************************************

    /**
     * A String representing the prefix of the java: namespace
     */
    public static final String JAVA_NS = "java:";

    /**
     * This enum represents the different namespaces of java:
     * 
     * <UL>
     * <LI>COMP = java:comp
     * <LI>COMP_ENV = java:comp/env
     * <LI>COMP_WS = java:comp/websphere
     * <LI>GLOBAL = java:global
     * <LI>APP = java:app
     * <LI>MODULE = java:module
     * </UL>
     * 
     */
    public enum JavaColonNamespace {
        COMP("comp"), COMP_ENV("comp/env"), COMP_WS("comp/websphere"), GLOBAL("global"), APP("app"), MODULE("module");

        private static final JavaColonNamespace[] VALUES_SORTED_BY_PREFIX = values();

        static {
            Arrays.sort(VALUES_SORTED_BY_PREFIX, new Comparator<JavaColonNamespace>() {
                @Override
                public int compare(JavaColonNamespace namespace1, JavaColonNamespace namespace2) {
                    String prefix1 = namespace1.prefix();
                    String prefix2 = namespace2.prefix();
                    if (prefix1.startsWith(prefix2)) {
                        return -1;
                    }
                    if (prefix2.startsWith(prefix1)) {
                        return 1;
                    }
                    return prefix1.compareTo(prefix2);
                }
            });
        }

        public static JavaColonNamespace match(String name) {
            for (JavaColonNamespace namespace : VALUES_SORTED_BY_PREFIX) {
                if (name.startsWith(namespace.prefix())) {
                    return namespace;
                }
            }
            return null;
        }

        /**
         * This method returns an instance of the enum based on the
         * passed-in string name. It must match the qualified name
         * of the namespace - i.e. "java:comp" - without the trailing
         * slash. This differs from the <code>match</code> method
         * in that it requires the passed-in string to be exactly
         * the same as the qualified name.
         * 
         * @param name - qualified name of the JCN enum requested
         * @return - the JavaColonNamespace enum instance or null
         *         if the passed-in string does not match any JCN instances.
         */
        public static JavaColonNamespace fromName(String name) {
            for (JavaColonNamespace namespace : VALUES_SORTED_BY_PREFIX) {
                if (namespace.qualifiedName().equals(name)) {
                    return namespace;
                }
            }
            return null;
        }

        private final String name;
        private final String prefix;

        private JavaColonNamespace(String name) {
            this.name = JAVA_NS + name;
            this.prefix = this.name + '/';
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Trivial
        public String qualifiedName() {
            return this.name;
        }

        @Trivial
        public String prefix() {
            return this.prefix;
        }

        public String unprefix(String name) {
            if (!name.startsWith(this.prefix)) {
                throw new IllegalArgumentException("name=" + name);
            }
            return name.substring(this.prefix.length());
        }

        /**
         * Returns true if the namespace belongs to java:comp.
         */
        public boolean isComp() {
            return this == COMP || this == COMP_ENV || this == COMP_WS;
        }
    }

}
