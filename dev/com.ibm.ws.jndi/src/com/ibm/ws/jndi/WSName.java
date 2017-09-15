/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jndi;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.naming.CompoundName;
import javax.naming.InvalidNameException;
import javax.naming.Name;

import com.ibm.ws.jndi.internal.Messages;
import com.ibm.wsspi.kernel.service.utils.CompositeEnumeration;

public final class WSName extends CompoundName {
    private static final long serialVersionUID = 1L;
    public static final String SEPARATOR = "/";
    static final String ESCAPE = "\\";
    static final String DOUBLE_QUOTE = "\"";
    static final String SINGLE_QUOTE = "'";
    @SuppressWarnings("serial")
    private static final Properties SYNTAX = new Properties() {
        {
            // parse left to right.
            put("jndi.syntax.direction", "left_to_right");
            // Use backslash for escape.
            put("jndi.syntax.escape", ESCAPE);
            // Allow single and double quotes.
            put("jndi.syntax.beginquote", DOUBLE_QUOTE);
            put("jndi.syntax.beginquote2", SINGLE_QUOTE);
            // Use slash to separate components.
            put("jndi.syntax.separator", SEPARATOR);
            // Allow leading and trailing whitespace in names
            put("jndi.syntax.trimblanks", "false");
        }
    };

    private static final Enumeration<String> NO_STRINGS = new Vector<String>().elements();

    // this initialisation must come after that of anything it references, even indirectly
    public static final WSName EMPTY_NAME = new WSName();

    public WSName() {
        super(NO_STRINGS, SYNTAX);
    }

    public WSName(String name) throws InvalidNameException {
        super(name, SYNTAX);
        if (name == null)
            throw new InvalidNameException(Messages.formatMessage("null.name", "The name cannot be null"));
    }

    public WSName(Name name) throws InvalidNameException {
        super(NO_STRINGS, SYNTAX);
        if (name == null)
            throw new InvalidNameException(Messages.formatMessage("null.name", "The name cannot be null"));
        if (name.isEmpty())
            throw new InvalidNameException(Messages.formatMessage("empty.name", "The name cannot be empty"));
        addAll(name);
    }

    private WSName(Enumeration<String> elems) {
        super(elems, SYNTAX);
    }

    @Override
    public WSName clone() {
        return new WSName(this.getAll());
    }

    @Override
    public WSName addAll(Name name) throws InvalidNameException {
        return (WSName) super.addAll(name);
    }

    @Override
    public WSName add(String element) throws InvalidNameException {
        return (WSName) super.add(element);
    }

    public WSName plus(Name name) throws InvalidNameException {
        return clone().addAll(name);
    }

    public WSName plus(String name) throws InvalidNameException {
        return clone().add(name);
    }

    WSName plus(WSName that) {
        Enumeration<String> e1 = this.getAll();
        Enumeration<String> e2 = that.getAll();
        Enumeration<String> e3 = new CompositeEnumeration<String>(e1).add(e2);
        return new WSName(e3);
    }

    @Override
    public WSName getPrefix(int index) {
        if (index < 0)
            throw new ArrayIndexOutOfBoundsException(index);
        Vector<String> elems = new Vector<String>(index);
        for (int i = 0; i < index; i++)
            elems.add(get(i));
        return new WSName(elems.elements());
    }

    @Override
    public WSName getSuffix(int index) {
        if (index > size())
            throw new ArrayIndexOutOfBoundsException(index);
        Vector<String> elems = new Vector<String>(size() - index);
        for (int i = index; i < size(); i++)
            elems.add(get(i));
        return new WSName(elems.elements());
    }

    /** @throws InvalidNameException if <code>this.isEmpty()</code> */
    public WSName getParent() throws InvalidNameException {
        ensureNotEmpty();
        return getPrefix(size() - 1);
    }

    /** @throws InvalidNameException if <code>this.isEmpty()</code> */
    public void ensureNotEmpty() throws InvalidNameException {
        if (isEmpty())
            throw new InvalidNameException(Messages.formatMessage("empty.name", "The name cannot be empty"));
    }

    /**
     * @throws InvalidNameException if <code>this.isEmpty()</code>
     */
    public String getLast() throws InvalidNameException {
        ensureNotEmpty();
        return get(size() - 1);
    }
}
