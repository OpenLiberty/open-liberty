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


package com.ibm.ws.sib.api.jms.impl;

/*
 * This class is used to format name/Object pairs into strings.
 * Objects must be of type Boolean, Byte, Short, Character, Integer, Long, Float,
 * Double, String, or Byte[].
 */
public class TypeFormatter {

    private static final String TYPE_BOOLEAN="boolean";
    private static final String TYPE_BYTE="byte";
    private static final String TYPE_SHORT="short";
    private static final String TYPE_CHAR="char";
    private static final String TYPE_INT="int";
    private static final String TYPE_LONG="long";
    private static final String TYPE_FLOAT="float";
    private static final String TYPE_DOUBLE="double";
    private static final String TYPE_STRING="String";
    private static final String TYPE_BYTEARRAY="byte[]";
    private static final String TYPE_UNKNOWN="unknown";
		
    private final static char Base16[] = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F' 
    };
	
    private static String getType(Object o) {
        if (o instanceof Boolean) 	return TYPE_BOOLEAN;
        if (o instanceof Byte) 		return TYPE_BYTE;
        if (o instanceof Short) 	return TYPE_SHORT;
        if (o instanceof Character)	return TYPE_CHAR;
        if (o instanceof Integer) 	return TYPE_INT;
        if (o instanceof Long) 		return TYPE_LONG;
        if (o instanceof Float) 	return TYPE_FLOAT;
        if (o instanceof Double) 	return TYPE_DOUBLE;
        if (o instanceof String) 	return TYPE_STRING;
        if (o instanceof Byte[])	return TYPE_BYTEARRAY;
        return TYPE_UNKNOWN;
    }
	
    private static String formatValue(Object o) {
        // the only type we handle specially is byte arrays
        if (o instanceof Byte[]) {
            Byte[] bytes = (Byte[])o;
            StringBuilder sb = new StringBuilder();
			
            for (int i=0; i<bytes.length; i++) {
                byte b = bytes[i];
                if (i>0) sb.append(' ');
                sb.append(Base16[(b >> 4) & (0xF)]);
                sb.append(Base16[b & 0xF]);
            }
            return sb.toString();
        }
        
        // for all other types, just return their toString
        return o.toString();
    }
	
    /*
     * Formats a name/object pair using the following template:
     * 
     * name(value-type)=value
     * 
     * where vale-type is a String representation of the object type of the
     * value passed in, and value is a String representation of the value
     * passed in.  Binary arrays are formatted in hex, all other types are
     * formatted with toString().  
     */
     public static String formatNameTypeValue(String name, Object value) {
        StringBuilder sb = new StringBuilder(name);
        sb.append('(');
        sb.append(getType(value));
        sb.append(')');
        sb.append('=');
        sb.append(formatValue(value));
		
        return sb.toString();
    }
	
}
