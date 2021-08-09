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
package com.ibm.ws.jndi.url.contexts.javacolon.internal;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingException;

import com.ibm.ws.container.service.naming.NamingConstants;

/**
 * A {@link NameParser} for {@link JavaURLName}s. Includes utility methods for
 * retrieving the Name without the java: namespace prefix and retrieving the
 * JavaColonNamespace.
 * 
 */
public class JavaURLNameParser implements NameParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public JavaURLName parse(String nameString) throws NamingException {
        if (nameString == null || nameString.equals(""))
            throw new InvalidNameException();

        JavaURLName name;
        try {
            name = new JavaURLName(nameString);
        } catch (InvalidNameException ine) {
            // For parity with tWAS a composite name that is mis-constructed should throw a
            // NameNotFoundException rather than an InvalidNameException.
            // See test.jndi.url.context.servlet.JndiServlet.testInvalidName() for more details.
            NameNotFoundException nnfe = new NameNotFoundException(nameString);
            nnfe.initCause(ine);
            throw nnfe;
        }

        return name;
    }

    /**
     * An extra parse method for {@link Name} parameters instead of Strings.
     * 
     * @param name
     * @return
     * @throws NamingException
     */
    JavaURLName parse(Name name) throws NamingException {
        return parse(name.toString());
    }

    /**
     * 
     * @param name
     * @return the prefixless JNDI name for the specified Name, @see JavaURLName
     * @throws NamingException
     *             if the Name cannot be coerced to a {@link JavaURLName} or
     *             does not satisfy the parser.
     */
    String getStringNameWithoutPrefix(Name name) throws NamingException {
        if (name instanceof JavaURLName)
            return ((JavaURLName) name).getPrefixlessName();
        else
            return parse(name).getPrefixlessName();
    }

    /**
     * 
     * @param name
     * @return {@link NamingConstants.JavaColonNamespace} for the specified Name
     * @throws NamingException
     *             if the Name cannot be coerced to a {@link JavaURLName} or
     *             does not satisfy the parser.
     */
    NamingConstants.JavaColonNamespace getJavaNamespaceFromName(Name name) throws NamingException {
        if (name instanceof JavaURLName)
            return ((JavaURLName) name).getNamespace();
        else
            return parse(name).getNamespace();
    }
}
