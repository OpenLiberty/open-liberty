/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Properties;

import com.ibm.ws.ffdc.FFDCFilter;

public class FieldInitializer {

    /*
     * initFromSystemProperties - initializes *static* fields in a Class based
     *    on system properties.  For example, given class com.company.Foo with
     *    static field name.  The following system property would change the field
     *      -Dcom.company.Foo.name=myName
     */
    static public void initFromSystemProperties(Class c) {
        updateFieldsFromSystemProperties(c, null);
    }

    /*
     * initFromSystemProperties - initializes fields (static and non-static) in an Object based
     *    on system properties.  For example, given Object o of class com.company.Foo
     *    with field cacheSize. The following system property would change the field
     *      -Dcom.company.Foo.cacheSize=100
     */
    static public void initFromSystemProperties(Object o) {
        updateFieldsFromSystemProperties(o.getClass(), o);
    }

    /*
     * initFromProperties - initializes *static* fields in a Class based
     *    on system properties.  For example, given class com.company.Foo with
     *    static field name.  The following system property would change the field
     *      -Dcom.company.Foo.name=myName
     */
    static public void initFromSystemProperties(Class c, Properties prop) {
        updateFieldsFromProperties(c, null, prop);
    }

    /*
     * initFromProperties - initializes fields (static and non-static) in an Object based
     *    on system properties.  For example, given Object o of class com.company.Foo
     *    with field cacheSize. The following system property would change the field
     *      -Dcom.company.Foo.cacheSize=100
     */
    static public void initFromSystemProperties(Object o, Properties prop) {
        updateFieldsFromProperties(o.getClass(), o, prop);
    }

    static private void updateFieldsFromSystemProperties(final Class c, final Object o) {
        updateFieldsFromProperties(c, o, System.getProperties());
    }

    static private void updateFieldsFromProperties(final Class c, final Object o, final Properties prop) {
        Enumeration propNames = prop.propertyNames();
        String prefix = c.getName();
        Field[] fields = c.getDeclaredFields();
        while ( propNames.hasMoreElements() ) {
            String propName = (String) propNames.nextElement();
            if ( isClassProperty( fields, propName, prefix ) ) {
                Field field = null;
                try {
                    String fieldName = propName.substring(propName.lastIndexOf('.') + 1);
                    field = getDeclaredField(c, fieldName);
                    // if we don't have an instance, make sure field is static
                    if ( field != null && o == null ) {
                        int modifiers = field.getModifiers();
                        if ( !Modifier.isStatic(modifiers) )
                            field = null;
                    }
                    if ( field != null ) {
                        field.setAccessible(true);
                        String val = (String) prop.get(propName);
                        Class fClass = field.getType();
                        if ( fClass == String.class )
                            field.set(o, val);
                        else if ( fClass == Integer.TYPE )
                            field.setInt(o, Integer.parseInt(val));
                        else if ( fClass == Boolean.TYPE )
                            field.setBoolean(o, Boolean.valueOf(val).booleanValue());
                        else if ( fClass == Long.TYPE )
                            field.setLong(o, Long.parseLong(val));
                        else if ( fClass == Byte.TYPE )
                            field.setByte(o, Byte.parseByte(val));
                        else if ( fClass == Character.TYPE )
                            field.setChar(o, val.charAt(0));
                        else if ( fClass == Float.TYPE )
                            field.setFloat(o, Float.parseFloat(val));
                        else if ( fClass == Double.TYPE )
                            field.setDouble(o, Double.parseDouble(val));
                        else if ( fClass == Short.TYPE )
                            field.setShort(o, Short.parseShort(val));
                        else {
                            // Tr.error unsupported type
                        }
                    } else {
                        // Tr.error unrecognized field
                    }
            // Start of lines changed for defect 491585
                } catch ( IllegalAccessException iae ) {
                      FFDCFilter.processException(iae, "com.ibm.ws.util.FieldInitializer", "110");
                } catch ( IllegalArgumentException iae ) {
                	FFDCFilter.processException(iae, "com.ibm.ws.util.FieldInitializer", "113");
                } catch ( NullPointerException npe ) {
                	FFDCFilter.processException(npe, "com.ibm.ws.util.FieldInitializer", "116");
                } catch ( ExceptionInInitializerError eiie ) {
                	FFDCFilter.processException(eiie, "com.ibm.ws.util.FieldInitializer", "119");
                }
            // End of lines changed for defect 491585
                if ( field != null )
                    field.setAccessible(false);
            }
        }
    }

    static private Field getDeclaredField(Class c, String fieldName) {
        Field f = null;
        do {
            try {
                f = c.getDeclaredField(fieldName);
         // Start of lines changed for defect 491585
            } catch ( NoSuchFieldException nsfe ) {
                // No FFDC code needed - we do not need to FFDC unless we've reached Object.class
                // (the field we want could be in the superclass!)
                if (c == Object.class) {
                   FFDCFilter.processException(nsfe, "com.ibm.ws.util.FieldInitializer", "133"); // 477704
                   c = null; // Stop the loop
                }
                else {
                  c = c.getSuperclass(); // Move to the superclass
                }
            } catch ( NullPointerException npe) {
                FFDCFilter.processException(npe,  "com.ibm.ws.util.FieldInitializer", "137");
                c = null; // Stop the loop
            } catch ( SecurityException se ) {
                FFDCFilter.processException(se,  "com.ibm.ws.util.FieldInitializer", "140");
                c = null; // Stop the loop
         // End of lines changed for defect 491585
            }
        } while ( f == null && c!=null );
        return f;
    }

    static private boolean isClassProperty( Field[] fields, String propName, String className ) {
        
        try {
        	if ( propName.startsWith(className) ) {
        		propName = propName.substring(propName.lastIndexOf('.') + 1);
        	}
        	for (Field field : fields) {
				if (field.getName().equals(propName)){
					return true;
				}
			}
		} catch (Exception e) {	}

		return false;
    }

}
