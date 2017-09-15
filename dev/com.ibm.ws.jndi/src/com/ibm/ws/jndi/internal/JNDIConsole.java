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
package com.ibm.ws.jndi.internal;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jndi.WSName;
import com.ibm.ws.jndi.internal.literals.LiteralParser;

@Component(configurationPolicy = IGNORE, property = "service.vendor=IBM")
public class JNDIConsole implements CommandProvider {

    private Context ctx;
    private Object last;

    private Context getContext() throws NamingException {
        if (ctx == null)
            ctx = new InitialContext();
        return ctx;
    }

    private Object parseNextArgument(CommandInterpreter ci) {
        String arg = ci.nextArgument();
        return arg == null ? null : arg.equals("$_") ? last : LiteralParser.parse(arg);
    }

    private void announce(CommandInterpreter ci, String op, String name) throws NamingException {
        ci.println("[" + getContext().getNameInNamespace() + "]" + "." + op + "(\"" + name + "\")");
    }

    private void announce(CommandInterpreter ci, String op, String name, Object obj) throws NamingException {
        String str = obj instanceof String ? "" + '"' + obj + '"' : obj + " : " + obj.getClass();
        ci.println("[" + getContext().getNameInNamespace() + "]" + "." + op + "(\"" + name + "\", " + str + ")");
    }

    private void handleEx(CommandInterpreter ci, Exception e) {
        ci.println("\tfailed with exception: " + e.getClass());
        ci.println("\t" + e.getMessage());
        ci.printStackTrace(e);
    }

    public void _jndilist(CommandInterpreter ci) {
        int i = 0;
        for (String arg = ci.nextArgument(); arg != null; arg = ci.nextArgument(), i++)
            list(ci, arg);
        if (i == 0)
            list(ci, "");
    }

    @FFDCIgnore(Exception.class)
    private void list(CommandInterpreter ci, String name) {
        try {
            announce(ci, "listBindings", name);
            NamingEnumeration<Binding> bindings = name == null ? getContext().listBindings(WSName.EMPTY_NAME) : getContext().listBindings(name);
            Set<String> subcontexts = new TreeSet<String>();
            Map<String, Binding> noncontexts = new TreeMap<String, Binding>();
            while (bindings.hasMore()) {
                Binding b = bindings.next();
                if (b.getObject() instanceof WSContext)
                    subcontexts.add(b.getName() + "/");
                else
                    noncontexts.put(b.getName(), b);
            }
            for (String sc : subcontexts)
                ci.println("\t" + sc);
            for (Binding b : noncontexts.values())
                ci.println("\t" + b);
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    public void _jndilookup(CommandInterpreter ci) {
        try {
            String name = ci.nextArgument();
            announce(ci, "lookup", name);
            last = getContext().lookup(name);
            ci.println("\t" + (last instanceof WSContext ? name + "/" : last));
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    public void _jndibind(CommandInterpreter ci) {
        try {
            String name = ci.nextArgument();
            Object obj = parseNextArgument(ci);
            announce(ci, "bind", name, obj);
            getContext().bind(name, obj);
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    public void _jndirebind(CommandInterpreter ci) {
        try {
            String name = ci.nextArgument();
            Object obj = parseNextArgument(ci);
            announce(ci, "rebind", name, obj);
            getContext().rebind(name, obj);
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    public void _jndiunbind(CommandInterpreter ci) {
        try {
            String name = ci.nextArgument();
            announce(ci, "unbind", name);
            getContext().unbind(name);
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    public void _jndirename(CommandInterpreter ci) {
        try {
            String oldName = ci.nextArgument();
            String newName = ci.nextArgument();
            announce(ci, "rename", oldName, newName);
            getContext().rename(oldName, newName);
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    public void _jndicreatesc(CommandInterpreter ci) {
        try {
            String name = ci.nextArgument();
            announce(ci, "createSubcontext", name);
            getContext().createSubcontext(name);
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    public void _jndidestroysc(CommandInterpreter ci) {
        try {
            String name = ci.nextArgument();
            announce(ci, "destroySubcontext", name);
            getContext().destroySubcontext(name);
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    public void _jndireset(CommandInterpreter ci) {
        try {
            ci.println("context = null");
            ctx = null;
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    public void _jndicd(CommandInterpreter ci) {
        try {
            String name = ci.nextArgument();
            Object o = getContext().lookup(name);
            Context c = (Context) o;
            ci.println("context = " + c);
            ctx = c;
        } catch (Exception e) {
            handleEx(ci, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getHelp() {
        return String.format("---JNDI utilities---%n"
                             + "\tjndilist%n"
                             + "\tjndilist <name>...%n"
                             + "\tjndilookup <name>%n"
                             + "\tjndibind <name> <object>%n"
                             + "\tjndirebind <name> <object>%n"
                             + "\tjndiunbind <name>%n"
                             + "\tjndirename <name> <name>%n"
                             + "\tjndicreatesc <name>%n"
                             + "\tjndidestroysc <name>%n"
                             + "\tjndicd <name>%n"
                             + "\tjndireset%n");
    }
}
