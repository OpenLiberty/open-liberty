/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.ffdc.IncidentStream;

/**
 * A IntrospectionLevelMember holds the information about a single object to be
 * introspected.
 */
public final class IntrospectionLevelMember {

    /**
     * OBJECT_TYPE is the name for the root object we're going to introspect
     * deeper
     */
    private static final String OBJECT_TYPE = "Object type";

    /** A well-formed equals sign for FFDCSelfIntrospectable objects */
    private static final String EQUALS = " = ";

    /** Comment for <code>EQUALS_LENGTH</code> */
    private static final int EQUALS_LENGTH = EQUALS.length();

    /** The amount of space needed for ' = ' and a CRLF */
    private static final int EXTRA_SPACE = 5;

    /** The maximum number of primitive array elements we're prepared to print */
    private static final int MAX_ARRAY_LENGTH = 1024;

    /** The object that this IntrospectionLevelMember is wrapping */
    private final Object _member;

    /** The depth of this level member */
    private final int _level;

    /** The text to be used as the name for this member */
    private final String _name;

    /** The descriptive text for this member */
    private final String _description;

    /** The children of this IntrospectionLevelMember */
    private List<IntrospectionLevelMember> _children;

    /** All known IntrospectionLevelMembers starting from the root object */
    private final Set<IntrospectionLevelMember> _allKnownMembersInThisTree;

    /**
     * Determine if another object is the same as this IntrospectionLevelMember
     *
     * @see java.lang.Object#equals(java.lang.Object)
     * @param obj
     *                The other object to be tested
     * @return true if the other object is the same
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;

        if (obj instanceof IntrospectionLevelMember) {
            IntrospectionLevelMember other = (IntrospectionLevelMember) obj;
            return _member == other._member;
        }
        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     * @return The hash code for this IntrospectionLevelMember
     */
    @Override
    public int hashCode() {
        if (_member == null)
            return 0;
        else
            return System.identityHashCode(_member);
    }

    /**
     * Construct a new IntrospectionLevelMember for a root object
     *
     * @param member
     */
    public IntrospectionLevelMember(Object member) {
        this(0, OBJECT_TYPE, member.getClass().getName(), member, new HashSet<IntrospectionLevelMember>());
        _allKnownMembersInThisTree.add(this); // Add ourselves to the all known
                                              // members set
    }

    /**
     * Construct a new IntrospectionLevelMember. NOTE: It's the caller's
     * responsibility to add this new member to any relevant allKnownMembers set
     *
     * @param level
     *                            The introspection level for this member
     * @param name
     *                            The text used as the name of this level member
     * @param description
     *                            The text used as the description of this level member
     * @param member
     *                            The object being introspected
     * @param allKnownMembers
     *                            The set of all known level members
     */
    private IntrospectionLevelMember(int level, String name, String description, Object member, Set<IntrospectionLevelMember> allKnownMembers) {
        _level = level;
        _name = name;
        _description = description;
        _member = member;
        _allKnownMembersInThisTree = allKnownMembers;
    }

    /**
     * Return the size of the string that will generated if this member is
     * instructed to print itself
     *
     * @return the number of bytes needed to introspect this member
     */
    public int sizeOfIntrospection() {
        return _name.length() + _description.length() + EXTRA_SPACE + 2 * _level;
    }

    /**
     * @return The set of child level members for this level member (excluding
     *         level members for objects already added by other level members)
     */
    public List<IntrospectionLevelMember> getChildren() {
        if (_children == null) {
            _children = new ArrayList<IntrospectionLevelMember>();
            if (_member != null) {
                if (_member instanceof String)
                    addNewChild("String value", _member);
                else if (_member instanceof FFDCSelfIntrospectable)
                    introspectSelfIntrospectable();
                else
                    introspectViaReflection();
            }
        }
        return _children;
    }

    /**
     * Introspect the object via reflection
     */
    private void introspectViaReflection() {
        {
            Class<?> memberClass = _member.getClass();
            if (memberClass.isArray()) {
                int length = Array.getLength(_member);
                Class<?> componentType = memberClass.getComponentType();
                if (componentType.isPrimitive()) {
                    addNewChild(componentType + "[0.." + (length - 1) + "]", _member);
                } else {
                    String simpleName = componentType.getSimpleName();
                    for (int i = 0; i < length && i < MAX_ARRAY_LENGTH; i++) {
                        Object value = Array.get(_member, i);
                        addNewChild(simpleName + "[" + i + "]", value);
                    }
                    if (length > MAX_ARRAY_LENGTH) {
                        addNewChild(simpleName + "[...]", "/* array length = " + length + " */");
                    }
                }
            } else {
                /*
                 * Loop around the fields of the object (including fields of its
                 * superclass) adding them as children (and, it we haven't seen
                 * the child before, getting introspected at the next level if
                 * its worth doing so)
                 */
                Class<?> currentClass = _member.getClass();
                while (currentClass != Object.class) {
                    Field[] fields = getFields(currentClass);
                    for (int i = 0; i < fields.length && i < MAX_ARRAY_LENGTH; i++) {
                        final Field field = fields[i];
                        Object value = getFieldValue(field);
                        addNewChild(field.getName(), value);
                    }
                    if (fields.length > MAX_ARRAY_LENGTH) {
                        addNewChild("field...", "/* total # of fields = " + fields.length + " */");
                    }
                    currentClass = currentClass.getSuperclass();
                }
            }
        }
    }

    /**
     * Perform the introspection by calling the object's introspectSelf() method
     */
    private void introspectSelfIntrospectable() {
        try {
            String[] strings = ((FFDCSelfIntrospectable) _member).introspectSelf();
            if (strings != null) {
                for (int i = 0; i < strings.length; i++) {
                    // If the string has an equals sign in it, split it there
                    if (strings[i] != null) {
                        int equalsLoc = strings[i].indexOf(EQUALS);
                        if (equalsLoc > 0 && equalsLoc < strings[i].length() - EQUALS_LENGTH) {
                            // Of the form "x = y"
                            addNewChild(strings[i].substring(0, equalsLoc), strings[i].substring(equalsLoc + EQUALS_LENGTH));
                        } else if (equalsLoc > 0) {
                            // Of the form "X = "
                            addNewChild(strings[i].substring(0, equalsLoc), "");
                        } else {
                            // There's a string (but it's not got a = in it)
                            addNewChild("strings[" + i + "]", strings[i]);
                        }
                    } else {
                        // The string reference is null
                        addNewChild("strings[" + i + "]", null);
                    }
                }
            }
        } catch (RuntimeException e) {
            // No FFDC code needed - we're doing an FFDC already
            // Do nothing - we don't want to cause even more trouble :-)
        }
    }

    /**
     * Add a new IntrospectionLevelMember to _children List (Checking to see if
     * the new child should be introspected further
     *
     * @param name
     *                  The name of the new child
     * @param value
     *                  The value of the new child
     */
    private void addNewChild(String name, Object value) {
        IntrospectionLevelMember prospectiveMember = new IntrospectionLevelMember(_level + 1, name, makeDescription(value), makeMember(value), _allKnownMembersInThisTree);
        if (makeMember(value) != null) {
            // OK, we'd like to introspect the object further - have we seen it?
            if (_allKnownMembersInThisTree.contains(prospectiveMember)) {
                // Already seen it, so ensure we don't reexamine it
                prospectiveMember = new IntrospectionLevelMember(_level + 1, name, makeDescription(value), null, _allKnownMembersInThisTree);
            } else {
                // Ensure we don't reexamine it if we see it again!
                _allKnownMembersInThisTree.add(prospectiveMember);
            }
        }
        _children.add(prospectiveMember);
    }

    /**
     * Return the value of the member's field
     *
     * @param field
     *                  The field to be queried
     * @return The value of the field
     */
    private Object getFieldValue(final Field field) {
        Object field_value = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    Object value = field.get(_member);

                    // Don't dump sensitive data
                    boolean sensitive = _member.getClass().isAnnotationPresent(Sensitive.class) || field.isAnnotationPresent(Sensitive.class);
                    if (value != null && sensitive) {
                        value = DataFormatHelper.sensitiveToString(value);
                    }

                    return value;
                } catch (IllegalAccessException e) {
                    // No FFDC code needed - we're in the middle of FFDC'ing!
                    // Should not happen - if it does return a string :-)
                    return "/* Could not access " + field.getName() + " */";
                }
            }
        });
        return field_value;
    }

    /**
     * Return the fields in a particular class
     *
     * @param currentClass
     *                         The class to be introspected
     * @return the fields of the class
     */
    private Field[] getFields(final Class<?> currentClass) {
        final Field[] objectFields = AccessController.doPrivileged(new PrivilegedAction<Field[]>() {
            @Override
            public Field[] run() {
                try {
                    Field[] tempObjectFields = currentClass.getDeclaredFields();
                    if (tempObjectFields.length != 0) {
                        /*
                         * If we are running on Java 9 or later, call setAccessible only if module currentClass belongs to is open.
                         * Calling setAccessible when module is not open leads to a warning being displayed in the console by the JDK, which we want to avoid.
                         * If we are running on Java 8 or earlier, we can freely call setAccessible.
                         */
                        //Attempt Java 9 and above
                        try {
                            Class<?> moduleClass = Class.forName("java.lang.Module");
                            Method getModule = Class.class.getMethod("getModule");
                            Object module = getModule.invoke(currentClass);
                            Class<?>[] paramString = new Class[1];
                            paramString[0] = String.class;
                            Method isOpen = moduleClass.getMethod("isOpen", paramString);
                            Package currentPackage = currentClass.getPackage();
                            String packageName = currentPackage.getName();
                            boolean isOpenCheck = (boolean) isOpen.invoke(module, packageName);
                            if (isOpenCheck) {
                                AccessibleObject.setAccessible(tempObjectFields, true);
                            }
                        } catch (Exception e) { //Fallback to java 8 and below
                            AccessibleObject.setAccessible(tempObjectFields, true);
                        }
                    }
                    return tempObjectFields;
                } catch (Throwable t) {
                    // Introspection of field failed
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    t.printStackTrace(pw);
                    addNewChild("Failed to resolve fields for " + currentClass.getName(), sw.toString());
                }
                return new Field[0];
            }
        });
        return objectFields;
    }

    /**
     * Make a string that describes this object
     *
     * @param value
     *                  The value needing a description
     * @return The description
     */
    private String makeDescription(Object value) {
        String answer; // Not initialized, so the compiler tells if we miss a
                       // case
        if (value == null) {
            answer = "null";
        } else if (value instanceof String) {
            answer = "\"" + value + "\"";
        } else {
            Class<?> objClass = value.getClass();
            if ((objClass == Boolean.class) || (objClass == Character.class) || (objClass == Byte.class) || (objClass == Short.class) || (objClass == Integer.class)
                || (objClass == Long.class) || (objClass == Float.class) || (objClass == Double.class)) {
                answer = value.toString();
            } else if (objClass.isArray()) {
                if (objClass.getComponentType().isPrimitive()) {
                    answer = convertSimpleArrayToString(value);
                } else {
                    answer = objClass.getComponentType() + "[" + Array.getLength(value) + "]";
                }
            } else {
                answer = value.getClass().toString() + "@" + Integer.toHexString(System.identityHashCode(value));
            }
        }

        return answer;
    }

    /**
     * Convert an Object (which must be an array) to a string to be used as a
     * description
     *
     * @param value
     *                  The object to be converted
     * @return The converted string
     */
    private String convertSimpleArrayToString(Object value) {
        String answer;
        int length = Array.getLength(value);
        StringBuffer temp = new StringBuffer("{");
        for (int i = 0; i < length && i < MAX_ARRAY_LENGTH; i++) {
            Object element = Array.get(value, i);
            if ((i > 0) && !(element instanceof Character))
                temp.append(",");
            temp.append(element);
        }
        if (length > MAX_ARRAY_LENGTH) {
            if (!(Array.get(value, 0) instanceof Character))
                temp.append(",");
            temp.append("...");
        }
        temp.append("} /* array length = ");
        temp.append(Integer.toString(Array.getLength(value)));
        temp.append(" */");
        answer = temp.toString();
        return answer;
    }

    /**
     * Return either null (if the value doesn't need further introspection) or
     * the object reference if it does require further introspection
     *
     * @param value
     *                  The object that might require further introspection
     * @return null if the object doesn't require further introspection, value
     *         if it does
     */
    private Object makeMember(Object value) {
        Object answer = null; // Assume we don't require further introspection
                              // until proved otherwise

        if (value != null) {
            Class<?> objClass = value.getClass();

            if (objClass.isArray()) {
                // Array's need further introspection if they're bigger than
                // length 0
                // AND of non-primitive type
                if (Array.getLength(value) > 0 && !(objClass.getComponentType().isPrimitive())) {
                    answer = value;
                }
            } else if ((objClass != String.class) && (objClass != Boolean.class) && (objClass != Character.class) && (objClass != Byte.class) && (objClass != Short.class)
                       && (objClass != Integer.class) && (objClass != Long.class) && (objClass != Float.class) && (objClass != Double.class) && (objClass != String.class)) {
                answer = value;
            }
        }

        return answer;
    }

    /**
     * Print this IntrospectionLevelMember to an incident stream
     *
     * @param is
     *                     The incident stream
     * @param maxDepth
     *                     The maximum depth to descend
     */
    public void print(IncidentStream is, int maxDepth) {
        StringBuffer fullName = new StringBuffer();
        for (int i = 0; i < _level; i++)
            fullName.append("  ");
        fullName.append(_name);
        is.writeLine(fullName.toString(), _description);
        if (_level < maxDepth) {
            List<IntrospectionLevelMember> children = getChildren();
            for (IntrospectionLevelMember ilm : children) {
                ilm.print(is, maxDepth);
            }
        }
    }
}

// End of file
