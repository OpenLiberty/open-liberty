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

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;

import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;

/**
 * JavaURLName is a special case of {@link CompositeName} that provides
 * additional function for getting the java: namespace as a {@link NamingConstants.JavaColonNamespace} and for getting the prefixless
 * form of the JNDI Name.
 * 
 */
public class JavaURLName extends CompositeName {

    private static final long serialVersionUID = 3689428422441378449L;

    private JavaColonNamespace namespace = null;
    private String prefixlessName = null;

    /**
     * Constructor intended for use by the {@link JavaURLNameParser}.
     * 
     * @param nameString - the String form of the JNDI name
     * @throws InvalidNameException if the name does not meet the syntax requirements of a {@link CompositeName}
     * @throws NameNotFoundException if the name does not have the minimum
     *             required number of elements (i.e. at least java:namespace/name) or if the
     *             namespace does not match one of those defined in {@link NamingConstants.JavaColonNamespace}
     */
    JavaURLName(String nameString) throws InvalidNameException, NameNotFoundException {
        super(nameString);

        //if we are here the name must at least start with java: or
        //we wouldn't have got to this URL context handler

        // get the java:something part of the name
        String prefix = this.getPrefix(1).toString();

        if (this.size() > 1) {
            // get the next part of the name
            String extendedPrefix = this.getPrefix(2).toString();
            // loop through checking the prefixes to find the namespace
            for (JavaColonNamespace ns : JavaColonNamespace.values()) {
                // check for extended prefixes first
                // so we subsequently only match a prefix if it wasn't an extended
                // one
                if (ns.toString().equals(extendedPrefix)) {
                    namespace = ns;
                    prefixlessName = this.getSuffix(2).toString();
                    break;
                }
            }
        }
        if (namespace == null) {
            // namespace was still null, was not an extended prefix, try the
            // short ones
            for (JavaColonNamespace ns : JavaColonNamespace.values()) {
                if (ns.toString().equals(prefix)) {
                    namespace = ns;
                    //get the prefixless name if there is one, or set it to the empty string if there isn't
                    if (this.size() > 1)
                        prefixlessName = this.getSuffix(1).toString();
                    else
                        prefixlessName = "";
                    break;
                }
            }
        }
        //if we didn't match a namespace throw an NameNotFoundException
        if (namespace == null)
            throw new NameNotFoundException(nameString);
    }

    /**
     * 
     * @return the {@link NamingConstants.JavaColonNamespace} to which this Name
     *         belongs
     */
    public JavaColonNamespace getNamespace() {
        return this.namespace;
    }

    /**
     * This method returns the prefixless form of the Name. The prefixless form
     * is the remaining part of the Name after the known {@link NamingConstants.JavaColonNamespace} is removed. For example for
     * the name java:comp/env/jdbc/test the prefixless form is jdbc/test
     * 
     * @return the prefixless form of this Name
     */
    public String getPrefixlessName() {
        return this.prefixlessName;
    }

    /**
     * The equals method in CompositeName is suitable for use with JavaURLName
     * because we don't change any of the elements of the name.
     */
    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * The hashCode method in CompositeName is suitable for use with JavaURLName
     * because we don't change any of the elements of the name.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
