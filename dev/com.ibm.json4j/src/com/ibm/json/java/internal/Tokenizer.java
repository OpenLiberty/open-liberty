/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.json.java.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.CharArrayReader;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Tokenizes a stream into JSON tokens.
 */
public class Tokenizer
{

    /**
     * The reader from which the JSON string is being read.
     */
    private Reader reader;

    /** 
     * The current line position in the JSON string.
     */
    private int     lineNo;

    /**
     * The current column position in the JSON string.
     */
    private int     colNo;

    /** 
     * The last character read from the JSON string.
     */
    private int     lastChar;

    /**
     * Whether large numbers should be supported
     */
    private boolean largeNumbers = false;

    /**
     * Constructor.
     * @param reader The reader from which the JSON string is read.
     * 
     * @throws IOException Thrown on IOErrors such as invalid JSON or sudden reader closures.
     */
    public Tokenizer(Reader reader, boolean largeNumbers) throws IOException {
        super();

        Class readerClass= reader.getClass();
        //In-memory readers don't need to be buffered.  Also, skip PushbackReaders
        //because they probably already wrap a buffered stream.  And lastly, anything
        //that extends from a BufferedReader also doesn't need buffering!
        if (!StringReader.class.isAssignableFrom(readerClass) && 
            !CharArrayReader.class.isAssignableFrom(readerClass) &&
            !PushbackReader.class.isAssignableFrom(readerClass) &&
            !BufferedReader.class.isAssignableFrom(readerClass)) {
            reader = new BufferedReader(reader);
        }
        this.reader    = reader;
        this.lineNo    = 0;
        this.colNo     = 0;
        this.lastChar  = '\n';
        this.largeNumbers = largeNumbers;

        readChar();
    }

    /**
     * Method to get the next JSON token from the JSON String
     * @return The next token in the stream, returning Token.TokenEOF when finished.
     *
     * @throws IOException Thrown if unexpected read error occurs or invalid character encountered in JSON string.
     */
    public Token next() throws IOException {

        // skip whitespace
        while (Character.isWhitespace((char)lastChar))
        {
            readChar();
        }

        // handle punctuation
        switch (lastChar)
        {
            case -1:  readChar(); return Token.TokenEOF;
            case '{': readChar(); return Token.TokenBraceL;
            case '}': readChar(); return Token.TokenBraceR;
            case '[': readChar(); return Token.TokenBrackL;
            case ']': readChar(); return Token.TokenBrackR;
            case ':': readChar(); return Token.TokenColon;
            case ',': readChar(); return Token.TokenComma;

            case '"':
            case '\'':
                String stringValue = readString();
                return new Token(stringValue);

            case '-':
            case '.':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                Number numberValue = readNumber();
                return new Token(numberValue);

            case 'n': 
            case 't':
            case 'f':
                String ident = readIdentifier();

                if (ident.equals("null"))  return Token.TokenNull;
                if (ident.equals("true"))  return Token.TokenTrue;
                if (ident.equals("false")) return Token.TokenFalse;

                throw new IOException("Unexpected identifier '" + ident + "' " + onLineCol());

            default:
                throw new IOException("Unexpected character '" + (char)lastChar + "' " + onLineCol());

        }

    }

    /**
     * Method to read a string from the JSON string, converting escapes accordingly.
     * @return The parsed JSON string with all escapes properly converyed.
     *
     * @throws IOException Thrown on unterminated strings, invalid characters, bad escapes, and so on.  Basically, invalid JSON.
     */
    private String readString() throws IOException {
        StringBuffer sb    = new StringBuffer();
        int          delim = lastChar;
        int          l = lineNo;
        int          c = colNo;

        readChar();
        while ((-1 != lastChar) && (delim != lastChar))
        {
            StringBuffer digitBuffer;

            if (lastChar != '\\')
            {
                sb.append((char)lastChar);
                readChar();
                continue;
            }

            readChar();

            switch (lastChar)
            {
                case 'b':  readChar(); sb.append('\b'); continue; 
                case 'f':  readChar(); sb.append('\f'); continue; 
                case 'n':  readChar(); sb.append('\n'); continue; 
                case 'r':  readChar(); sb.append('\r'); continue; 
                case 't':  readChar(); sb.append('\t'); continue; 
                case '\'': readChar(); sb.append('\''); continue; 
                case '"':  readChar(); sb.append('"');  continue; 
                case '\\': readChar(); sb.append('\\'); continue;
                case '/': readChar();  sb.append('/'); continue;

                    // hex constant
                    // unicode constant
                case 'x':
                case 'u':
                    digitBuffer = new StringBuffer();

                    int toRead = 2;
                    if (lastChar == 'u') toRead = 4;

                    for (int i=0; i<toRead; i++)
                    {
                        readChar();
                        if (!isHexDigit(lastChar)) throw new IOException("non-hex digit " + onLineCol());
                        digitBuffer.append((char) lastChar);
                    }
                    readChar();

                    try
                    {
                        int digitValue = Integer.parseInt(digitBuffer.toString(), 16);
                        sb.append((char) digitValue);
                    }
                    catch (NumberFormatException e)
                    {
                        throw new IOException("non-hex digit " + onLineCol());
                    }

                    break;

                    // octal constant
                default:
                    if (!isOctalDigit(lastChar)) throw new IOException("non-hex digit " + onLineCol());

                    digitBuffer = new StringBuffer();
                    digitBuffer.append((char) lastChar);

                    for (int i=0; i<2; i++)
                    {
                        readChar();
                        if (!isOctalDigit(lastChar)) break;

                        digitBuffer.append((char) lastChar);
                    }

                    try
                    {
                        int digitValue = Integer.parseInt(digitBuffer.toString(), 8);
                        sb.append((char) digitValue);
                    }
                    catch (NumberFormatException e)
                    {
                        throw new IOException("non-hex digit " + onLineCol());
                    }
            }
        }

        if (-1 == lastChar)
        {
            throw new IOException("String not terminated " + onLineCol(l,c));
        }

        readChar();

        return sb.toString();
    }

    /**
     * Method to read a number from the JSON string.
     * 
     * (-)(1-9)(0-9)*            : decimal
     * (-)0(0-7)*               : octal
     * (-)0(x|X)(0-9|a-f|A-F)*  : hex
     * [digits][.digits][(E|e)[(+|-)]digits]         
     *
     * @returns The number as the wrapper Java Number type.
     * 
     * @throws IOException Thrown in invalid numbers or unexpected end of JSON string
     * */
    private Number readNumber() throws IOException {
        StringBuffer sb = new StringBuffer();
        int          l    = lineNo;
        int          c    = colNo;

        while (isDigitChar(lastChar))
        {
            sb.append((char)lastChar);
            readChar();
        }

        // convert it!
        String string = sb.toString();

        try
        {
            if (-1 != string.indexOf('.'))
            {
                if (largeNumbers)
                {
                    return new BigDecimal(string);
                }
                else
                {
                    return Double.valueOf(string);
                }
            }

            String sign = "";
            if (string.startsWith("-"))
            {
                sign = "-";
                string = string.substring(1);
            }

            if (string.toUpperCase().startsWith("0X"))
            {
                if (largeNumbers)
                {
                    return new BigInteger(sign + string.substring(2), 16);
                }
                else
                {
                    return Long.valueOf(sign + string.substring(2), 16);
                }
            }

            if (string.equals("0"))
            {
                if (largeNumbers)
                {
                    return BigInteger.ZERO;
                }
                else
                {
                    return new Long(0);
                }
            }
            else if (string.startsWith("0") && string.length() > 1)
            {
                if (largeNumbers)
                {
                    return new BigInteger(sign + string.substring(1), 8);
                }
                else
                {
                    return Long.valueOf(sign+string.substring(1), 8);
                }
            }

            /**
             * We have to check for the exponential and treat appropriately
             * Exponentials should be treated as Doubles.
             */
            if (string.indexOf("e") != -1 || string.indexOf("E") != -1)
            {
                if (largeNumbers)
                {
                    return new BigDecimal(sign + string);
                }
                else
                {
                    return Double.valueOf(sign + string);
                }
            }
            else
            {
                if (largeNumbers) {
                    return new BigInteger(sign + string, 10);
                }
                else
                {
                    return Long.valueOf(sign + string, 10);
                }
            }
        }
        catch (NumberFormatException e)
        {
            IOException iox = new IOException("Invalid number literal " + onLineCol(l,c));
            iox.initCause(e);
            throw iox;
        }
    }

    /**
     * Method to indicate if the character read is a HEX digit or not. 
     * @param c The character to check for being a HEX digit.
     */
    private boolean isHexDigit(int c)
    {
        switch (c)
        {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4': 
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
                return true;
        }

        return false;
    }

    /**
     * Method to indicate if the character read is an OCTAL digit or not. 
     * @param c The character to check for being a OCTAL digit.
     */
    private boolean isOctalDigit(int c)
    {
        switch (c)
        {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4': 
            case '5':
            case '6':
            case '7': 
                return true;
        }

        return false;
    }

    /**
     * Method to indicate if the character read is a digit or not.  
     * @param c The character to check for being a digit.
     */
    private boolean isDigitChar(int c)
    {
        switch (c)
        {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4': 
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '.':
            case 'e':
            case 'E':
            case 'x':
            case 'X':
            case '+':
            case '-':
                return true;
        }

        return false;
    }

    /**
     * Method to read a partular character string.
     * only really need to handle 'null', 'true', and 'false' 
     */
    private String readIdentifier() throws IOException {
        StringBuffer sb = new StringBuffer();

        while ((-1 != lastChar) && Character.isLetter((char)lastChar))
        {
            sb.append((char)lastChar);
            readChar();
        }

        return sb.toString();
    }

    /**
     * Method to read the next character from the string, keeping track of line/column position.
     * 
     * @throws IOException Thrown when underlying reader throws an error.
     */
    private void readChar() throws IOException {
        if ('\n' == lastChar)
        {
            this.colNo = 0;
            this.lineNo++;
        }

        lastChar = reader.read();
        if (-1 == lastChar) return ;

        colNo++;
    }

    /**
     * Method to generate a String indicationg the current line and column position in the JSON string.
     */
    private String onLineCol(int line, int col)
    {
        return "on line " + line + ", column " + col;
    }

    /**
     * Method to generate a String indicationg the current line and column position in the JSON string.
     */
    public String onLineCol()
    {
        return onLineCol(lineNo,colNo);
    }
}