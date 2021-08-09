/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

/**
 * InjectionBindings are used for all scopes of Java name spaces. The
 * Injection Scope is a new attribute used by those bindings so that a given
 * instance knows which naming scope it's a member of. <p>
 *
 * This enumeration is similar to the enumeration provided on
 * com.ibm.ws.naming.util.C, however it has been replicated here to avoid
 * dependencies on traditional WAS naming, and also provide an extension,
 * match(), which allows for easier identification of the context based on
 * the reference name. <p>
 */
public enum InjectionScope // d660700
{
    COMP("comp"),
    MODULE("module"),
    APP("app"),
    GLOBAL("global");

    private static final InjectionScope[] VALUES = values();

    /**
     * Returns the number of values for the enumeration.
     */
    public static int size()
    {
        return VALUES.length;
    }

    public static InjectionScope match(String refName)
    {
        for (InjectionScope scope : VALUES)
        {
            if (refName.startsWith(scope.ivPrefix))
            {
                return scope;
            }
        }

        return null;
    }

    private static final String JAVA_COLON = "java:"; // d726563
    private static final String JAVA_COLON_COMP_ENV_PREFIX = "java:comp/env/"; // d726563

    /**
     * Convert a reference name to the normal form used in binding maps.
     *
     * @param refName the reference name
     * @return the normalized reference name
     */
    // d726563
    public static String normalize(String refName)
    {
        return refName.startsWith(JAVA_COLON_COMP_ENV_PREFIX) ?
                        refName.substring(JAVA_COLON_COMP_ENV_PREFIX.length()) :
                        refName;
    }

    /**
     * Denormalize a reference name by prepending "java:comp/env/" if necessary.
     *
     * @param refName the reference name
     * @return the denormalized reference name
     */
    // d726563
    public static String denormalize(String refName)
    {
        // Note, this also handles "java:comp" without "env".
        return refName.startsWith(JAVA_COLON) ?
                        refName :
                        JAVA_COLON_COMP_ENV_PREFIX + refName;
    }

    /**
     * The scope name (e.g., "global").
     */
    private final String ivName;

    /**
     * The scheme and scope (e.g., "java:global")
     */
    private final String ivQualifiedName;

    /**
     * The scheme, scope, and a slash (e.g., "java:global/").
     */
    private final String ivPrefix;

    InjectionScope(String name)
    {
        ivName = name;
        ivQualifiedName = JAVA_COLON + name;
        ivPrefix = JAVA_COLON + name + '/';
    }

    /**
     * Returns the name space context name (e.g., "global").
     */
    public String contextName()
    {
        return ivName;
    }

    /**
     * Returns the name space qualified name; scheme and scope
     * (e.g., "java:global").
     */
    public String qualifiedName()
    {
        return ivQualifiedName;
    }

    /**
     * Returns the name space prefix; scheme, scope, and a slash
     * (e.g., "java:global/").
     */
    public String prefix()
    {
        return ivPrefix;
    }

}
