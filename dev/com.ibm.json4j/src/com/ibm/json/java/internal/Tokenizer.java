/*******************************************************************************
 * Copyright (c) 2006,2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.json.java.internal;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Parse JSON text.
 * 
 * See JSON RFC 8259.
 * 
 * This tokenizer provides several extensions:
 *
 * Single quotes are handled as string delimiters. Single quote delimited
 * strings are not valid JSON values.
 * 
 * A bare '-' is parsed as 0. A bare '-' is not a valid JSON number.
 * 
 * Hex and octal integer values are parsed, but are not valid JSON numbers.
 */
public class Tokenizer {
    // Character tests ...
    
    // TODO: Would these be faster with range checks?

    @Trivial    
    private static boolean isHexDigit(int c) {
        switch (c) {
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
            default:
                return false;
        }
    }

    @Trivial    
    private static boolean isOctalDigit(int c) {
        switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                return true;
            default:
                return false;
        }
    }

    @Trivial    
    private static boolean isDigitChar(int c) {
        switch (c) {
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
            default:
                return false;                
        }
    }    

    @Trivial    
    private static char asPunctuation(char c) {
        switch (c) {
            case 'b': return '\b';
            case 'f': return '\f';
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
            case '\'': return '\'';
            case '"': return '"';
            case '\\': return '\\';
            case '/': return '/';

            default: return (char) 0;
        }
    }
    
    // Length checks ... these are security related.

    // Cache these per tokenizer.
    // Do NOT make them static ... allow the configured values to change.
    
    private final int maxStringLength;
    private final int maxNumberLength;

    // Having the caller throw the exception eases the data flow analysis
    // that the compiler does.

    @Trivial    
    private IOException stringTooLong(int currentLength) {
        return new IOException("String length exceeds maximum allowed length of " +
            maxStringLength + " characters at line " + lineNo + ", column " + colNo +
                ". Configure with system property: com.ibm.json4j.max.string.length");
    }

    @Trivial    
    private IOException numberTooLong(int currentLength) {
        return new IOException("Number string length exceeds maximum allowed length of " +
            maxNumberLength + " characters at line " + lineNo + ", column " + colNo +
                ". Configure with system property: com.ibm.json4j.max.number.length");
    }

    // Reader and associated state ...

    private static Reader ensureBuffer(Reader reader) {
        //In-memory readers don't need to be buffered.  Also, skip PushbackReaders
        //because they probably already wrap a buffered stream.  And lastly, anything
        //that extends from a BufferedReader also doesn't need buffering!
        
        Class<?> readerClass = reader.getClass();        
        if ( StringReader.class.isAssignableFrom(readerClass) ||
             CharArrayReader.class.isAssignableFrom(readerClass) ||
             PushbackReader.class.isAssignableFrom(readerClass) ||
             BufferedReader.class.isAssignableFrom(readerClass) ) {
            return reader;

        } else {
            return new BufferedReader(reader);
        }
    }
    
    private final Reader reader;

    private int lineNo;
    private int colNo;

    private boolean isEOF;
    private char lastChar;

    @Trivial
    private static String onLineCol(int line, int col) {
        return "on line " + line + ", column " + col;
    }

    @Trivial    
    public String onLineCol() {
        return onLineCol(lineNo, colNo);
    }

    @Trivial    
    public int getLine() {
        return lineNo;
    }

    @Trivial    
    public int getCol() {
        return colNo;
    }

    /**
     * Read the next available character.
     * 
     * Do nothing if EOF has been reached.
     * 
     * Reset the column number and increment the line number
     * if the last read character was an end-of-line character.
     * 
     * If the read answers the end-of-file value (-1), mark the tokenizer
     * as having reached the end-of-file.
     * 
     * Otherwise, convert the read value into a character and increment
     * the column number.
     * 
     * TODO: The read pattern is to pre-read each character. This is problematic,
     * and might be better implemented as on-demand reads. In particular, the
     * constructor call will block until a character is available, and can throw
     * an IOException. Having IO only happen on the first call to 'next' is
     * preferable.
     * 
     * @throws IOException Thrown if the read fails.
     */
    private void readChar() throws IOException {
        // This test is present for safety, but should not be necessary.
        // The precondition of calling 'readChar' is that EOF has not been reached. 
        // (All current calls to 'readChar' are preceded by an EOF test..)
        if ( isEOF ) {
            return;
        }

        if ( '\n' == lastChar ) {
            colNo = 0;
            lineNo++;
        }

        int nextRead = reader.read();
        if ( nextRead == -1 ) {
            // Once 'isEOF' is true, 'lastChar' is not meaningful.
            // Assign a dummy 'nul' character.
            lastChar = '\0';
            isEOF = true;
        } else {
            lastChar = (char) nextRead;
            colNo++;
        }
    }

    // Text accumulators ...
    //
    // The flow through the tokenizer is single threaded and does not recurse.
    // More than one of any of these is not needed.

    private final StringBuilder stringBuilder;
    private final StringBuilder numberBuilder;

    // Public APIs ...
    
    public Tokenizer(Reader reader) throws IOException {
        super();

        this.maxStringLength = ParserConfig.getMaxStringLength();        
        this.maxNumberLength = ParserConfig.getMaxNumberLength();        

        this.reader = ensureBuffer(reader);
        this.lineNo = 0;
        this.colNo = 0;
        
        // TODO: See the comment on 'readChar' in regards to this
        // problematic pre-reading of the next available character.

        // Prime with a first character read that moves to line '1'.
        
        this.isEOF = false;
        this.lastChar = '\n';
        this.readChar(); // Move off the pseudo-end-of-line character.

        this.stringBuilder = new StringBuilder();
        this.numberBuilder = new StringBuilder();
    }

    public Token next() throws IOException {
        while ( !isEOF && Character.isWhitespace(lastChar)) {
            readChar(); // Move off of the whitespace character.
        }

        if ( isEOF ) {
            return Token.TokenEOF;
        }

        switch ( lastChar ) {
                // handle punctuation                
            case '{':
                readChar(); // Move off the punctuation character.
                return Token.TokenBraceL;
            case '}':
                readChar(); // Move off the punctuation character.
                return Token.TokenBraceR;
            case '[':
                readChar(); // Move off the punctuation character.
                return Token.TokenBrackL;
            case ']':
                readChar(); // Move off the punctuation character.
                return Token.TokenBrackR;
            case ':':
                readChar(); // Move off the punctuation character.
                return Token.TokenColon;
            case ',':
                readChar(); // Move off the punctuation character.
                return Token.TokenComma;

                // Handle double quotes (which is per the JSON RFC) and
                // handle single quotes (which is NOT valid per the RFC.)
                // See JSON RFC 8259.
            case '"':
            case '\'':
                return new Token( readString() );

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
                return new Token( readNumber() );

            case 'n':
            case 't':
            case 'f':
                return readIdentifier();

            default:
                throw new IOException("unexpected character '" + lastChar + "' " + onLineCol());
        }
    }

    // Token readers ... String, Number, and Identifier.

    /**
     * Method to read a string from the JSON string, converting escapes accordingly.
     *
     * @return The parsed JSON string with all escapes properly converted.
     *
     * @throws IOException Thrown if the JSON text does not contain a valid string.
     *     Thrown on unterminated strings, invalid characters, bad escapes, and so on.
     */
    private String readString() throws IOException {
        char delim = lastChar;
        int l = lineNo;
        int c = colNo;
        int currentLength = 0;

        // This can be removed using try/finally.                            
        stringBuilder.setLength(0);

        readChar(); // Move off the opening delimiter.
        
        while ( !isEOF && (delim != lastChar)) {
            if ( currentLength >= maxStringLength ) {
                throw stringTooLong(currentLength);
            }

            if ( lastChar != '\\' ) {
                stringBuilder.append(lastChar);
                currentLength++;
                readChar(); // Move off the string element.
                continue;
            }

            // From here on we are on an escaped character,
            // which will be either punctuation, a hex or unicode
            // encoded character, or an octal encoded character.
            
            readChar(); // Move off the escape character.

            if ( isEOF ) {
                throw new IOException("incomplete escape sequence " + onLineCol());
            }

            char leadingChar = lastChar;
            
            char punctuationChar = asPunctuation(leadingChar);
            if ( punctuationChar != 0 ) {
                readChar(); // Move off the punctuation character
                stringBuilder.append(punctuationChar);
                currentLength++;
                continue;
                
                // hex constant
                // unicode constant                
            } else if ( (leadingChar == 'x') || (leadingChar == 'u') ) {
                readChar();
                // Move off the hex/unicode signifier and onto the first
                // hex/unicode digit.

                int toRead = ( (leadingChar == 'u') ? 4 : 2 );

                // Do NOT do length checks on hex / unicode digits. There are at most four
                // of these. The contribution is the parsed encoded character, which is checked
                // by the common length test, above.
                
                // This can be removed using try/finally.                    
                numberBuilder.setLength(0);

                int digitNo = 0;
                while ( !isEOF && (digitNo < toRead) ) {
                    if ( !isHexDigit(lastChar) ) {
                        throw new IOException("non-hex digit '" + lastChar + "' " + onLineCol());
                    }
                    numberBuilder.append(lastChar);
                    readChar(); // Move off the hex digit.
                    
                    digitNo++;
                }
                if ( digitNo < toRead ) {
                    String msg;
                    if ( leadingChar == 'u' ) {
                        msg = "Incomplete unicode number; four digits are required; ";
                    } else {
                        msg = "Incomplete hex number; two digits are required; ";
                    }
                    throw new IOException(msg + onLineCol());
                }

                String numberText = numberBuilder.toString();
                numberBuilder.setLength(0);

                char hexDigit;
                try {
                    hexDigit = (char) Integer.parseInt(numberText, 16);
                } catch ( NumberFormatException e ) {
                    throw new IOException("non-hex number " + onLineCol());
                }
                stringBuilder.append(hexDigit);
                currentLength++;
                
            } else {
                // octal constant

                // This is the default escaped character case. The text must be
                // a one, two, or three character octal value.

                // Do NOT do length checks on the octal digits. There can be at most two of these.
                // The contribution is the parsed octal encoded character, which is checked
                // by the common length test, above.

                // This can be removed using try/finally.                    
                numberBuilder.setLength(0);

                // The first character MUST be an an octal digit.
                if ( !isOctalDigit(leadingChar) ) {
                    throw new IOException("non-octal digit '" + leadingChar + "' " + onLineCol());
                }

                numberBuilder.append(leadingChar);
                readChar(); // Move off the first digit.

                for ( int digitNo = 0; !isEOF && (digitNo < 2); digitNo++ ) {
                    // The second and third digits need not be octal digits.
                    if ( !isOctalDigit(lastChar) ) {
                        break;
                    }
                    numberBuilder.append(lastChar);
                    readChar(); // Move off the next digit.
                }
                
                // Unlike the hex case, octal values may have fewer than two digits.

                String octalText = numberBuilder.toString();
                numberBuilder.setLength(0);

                char octalDigit;
                try {
                    octalDigit = (char) Integer.parseInt(octalText, 8);
                } catch ( NumberFormatException e ) {
                    throw new IOException("non-octal number " + onLineCol());
                }
                stringBuilder.append(octalDigit);
                currentLength++;                    
            }
        }

        if ( isEOF ) {
            throw new IOException("no closing delimiter '" + delim + "' " + onLineCol(l, c));
        }
        readChar(); // Move off the closing delimiter.

        String string = stringBuilder.toString();
        stringBuilder.setLength(0);
        return string;
    }

    /**
     * Method to read a number from the JSON string.
     *
     * (-)(1-9)(0-9)* : decimal
     * (-)0(0-7)* : octal
     * (-)0(x|X)(0-9|a-f|A-F)* : hex
     * [digits][.digits][(E|e)[(+|-)]digits]
     *
     * @return The number as the wrapper Java Number type.
     *
     * @throws IOException Thrown in invalid numbers or unexpected end of JSON string
     */
    private Number readNumber() throws IOException {
        int l = lineNo;
        int c = colNo;
        int currentLength = 0;

        // TODO: This can be further optimized.

        // This can be removed using try/finally.
        numberBuilder.setLength(0);
        
        // TODO: This is somewhat improper. The call to 'readNumber' is
        // preconditioned on the last character being a digit character.

        while ( !isEOF && isDigitChar(lastChar) ) {
            if ( currentLength >= maxNumberLength ) {
                throw numberTooLong(currentLength);
            }
            numberBuilder.append(lastChar);
            currentLength++;
            readChar(); // Move off the digit character.
        }
        String numberText = numberBuilder.toString();
        numberBuilder.setLength(0);

        // 'length' cannot be 0: The call to 'readNumber' is preconditioned on
        // starting with a valid number character.

        int length = numberText.length();

        boolean isDecimal = false;
        boolean isExp = false;
        for ( int charNo = 0; charNo < length; charNo++ ) {
            char nextChar = numberText.charAt(charNo);
            if ( nextChar == '.' ) {
                isDecimal = true;
                break;
            } else if ( (nextChar == 'e') || (nextChar == 'E') ) {
                isExp = true;
                break;
            }
        }
        if ( isDecimal || isExp ) {
            try {
                return Double.valueOf(numberText);
            } catch ( NumberFormatException e ) {
                throw new IOException("non-valid number " + onLineCol(l, c), e);
            }
        }

        boolean isSigned;
        int adjustment;
        if ( numberText.charAt(0) == '-' ) {
            if ( length == 1 ) {
                // Return 0 for a bare '-'.
                // A bare '-' is not valid JSON. This behavior must be
                // retained for backwards compatibility.
                // Note that Long.valueOf("") answers zero.
                return 0;
            }
            isSigned = true;
            adjustment = 1;
        } else {
            isSigned = false;
            adjustment = 0;
        }

        int radix;
        if ( numberText.charAt(adjustment) == '0' ) {
            if ( length == 1 + adjustment ) {
                // Return 0 for "0" or "-0". Although it looks strange, '-0' is
                // a valid JSON number.
                return 0;
            } else {
                // A leading 0 means that either the value is octal ("033")
                // or hexadecimal ("0x33" or "0X33"). 
                char secondChar = numberText.charAt(1 + adjustment);
                if ( (secondChar == 'x') || (secondChar == 'X') ) {
                    // Remove the "0x" (or "0X") hexadecimal signifier.
                    // Remove the '-' as well. The sign will be put into
                    // the result after conversion.
                    adjustment += 2;
                    radix = 16;
                } else {
                    // The leading '0' could be removed. However, providing
                    // the leading 0 to Long.valueOf is harmless. Leaving the
                    // leading '0' avoids a substring operation.
                    // The leading '-' is retained, since an explicit
                    // radix is specified.
                    adjustment = 0;
                    radix = 8;
                }
            }
        } else {
            // No leading '0', '0x', or '0X'. The number is a usual base 10 number.
            // The leading '-' is retained, since an explicit radix is specified.
            adjustment = 0;
            radix = 10;
        }

        String adjustedText = (adjustment != 0) ? numberText.substring(adjustment) : numberText;
        long value;
        try {
            value = Long.valueOf(adjustedText, radix);
        } catch ( NumberFormatException e ) {
            throw new IOException("non-valid number " + onLineCol(l, c), e);
        }
        if ( (adjustment > 0) && isSigned ) {
            value *= -1;
        }
        return value;
    }
    
    private static final char[] nullChars = { 'n', 'u', 'l', 'l' };
    private static final char[] trueChars = { 't', 'r', 'u', 'e' };
    private static final char[] falseChars = { 'f', 'a', 'l', 's', 'e' };

    private Token readIdentifier() throws IOException {
        char initialChar = lastChar;
        readChar(); // Move past the initial identifier character.
        int charNo = 1;        

        // The first character is known to be valid ... setup the
        // parsing expectations.

        char[] testChars;
        Token expectedToken;
        if ( initialChar == 'n' ) {
            testChars = nullChars;
            expectedToken = Token.TokenNull;
        } else if ( initialChar == 't' ) {
            testChars = trueChars;
            expectedToken = Token.TokenTrue;
        } else if ( initialChar == 'f' ) {
            testChars = falseChars;
            expectedToken = Token.TokenFalse;
        } else {
            // Not possible: 'lastChar' must be one of 'n', 't', or 'f'.
            throw new IllegalStateException();
        }

        while ( !isEOF && (charNo < testChars.length) ) {
            if ( lastChar != testChars[charNo] ) {
                throw new IOException("unexpected identifier " + onLineCol());
            }
            readChar(); // Move past the next identifier character.
            charNo++;
        }

        if ( charNo < testChars.length ) {
            throw new IOException("truncated identifier " + onLineCol());
        } else if ( !isEOF && Character.isLetter(lastChar) ) {
            throw new IOException("identifier has extra characters " + onLineCol());
        } else {
            return expectedToken;
        }
    }    
}