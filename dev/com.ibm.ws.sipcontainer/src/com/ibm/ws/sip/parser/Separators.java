/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.parser;

/**
 * Various separators used in the parser.
 * 
 * @author Assaf Azaria, Mar 2003.
 */
public interface Separators
{
    public static final char SEMICOLON = ';';
    public static final char COLON = ':';
    public static final char COMMA = ',';
    public static final char SLASH = '/';
    public static final char SP = ' ';
    public static final char EQUALS = '=';
    public static final char STAR = '*';
	public static final char RETURN = '\n';
    public static final char LESS_THAN = '<';
    public static final char GREATER_THAN = '>';
    public static final char AT = '@';
    public static final char DOT = '.';
    public static final char QUESTION = '?';
    public static final char POUND = '#';
    public static final char AND = '&';
    public static final char LPAREN = '(';
    public static final char RPAREN = ')';
    public static final char DOUBLE_QUOTE = '\"';
    public static final char QUOTE = '\'';
    public static final char ENDL = '\0';
    public static final char PLUS = '+';
    public static final char EXCLAMATION = '!';
	public static final char APOSTROPHE = '\'';
	public static final char PERCENT = '%';
	public static final char TILDA = '~';
	public static final char MINUS = '-';
	public static final char UNDERSCORE = '_';
	public static final char TAB = '\t';
    
	
	public static final String NEWLINE = "\r\n";
}
