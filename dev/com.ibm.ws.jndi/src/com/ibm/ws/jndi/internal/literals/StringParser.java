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
package com.ibm.ws.jndi.internal.literals;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@Trivial
enum StringParser {
    // These are the states of the parsing process
    UNSTARTED, UNESCAPED, ESCAPED, OCTAL2LEFT, OCTAL1LEFT, HEX4LEFT, HEX3LEFT, HEX2LEFT, HEX1LEFT, COMPLETE;

    private static final TraceComponent tc = Tr.register(StringParser.class);

    @FFDCIgnore({ UnterminatedEscapeSequence.class, StringNotStarted.class, EarlyTermination.class })
    static String parse(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        StringParser state = UNSTARTED;
        int i = 0;
        try {
            for (i = 0; i < s.length(); i++)
                state = state.accept(sb, s.charAt(i), '"');
            if (state == COMPLETE)
                return sb.toString();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "String not terminated with double quote.", s);
        } catch (UnterminatedEscapeSequence e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unexpected character in escape sequence.", s, createPointerString(s, i), state);
        } catch (StringNotStarted e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "String does not start with double quote.", state);
        } catch (EarlyTermination e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unexpected double quote before end of string.", s, createPointerString(s, i), state);
        }
        return s;
    }

    @FFDCIgnore({ UnterminatedEscapeSequence.class, StringNotStarted.class, EarlyTermination.class })
    static Object parseChar(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        StringParser state = UNSTARTED;
        int i = 0;
        try {
            for (i = 0; i < s.length(); i++)
                state = state.accept(sb, s.charAt(i), '\'');
            if (state == COMPLETE) {
                if (sb.length() == 1)
                    return sb.charAt(0);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "char expression contains too many characters", s, sb);
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "char not terminated with single quote.", s);
            }
        } catch (UnterminatedEscapeSequence e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unexpected character in escape sequence.", s, createPointerString(s, i), state);
        } catch (StringNotStarted e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "char does not start with single quote.", state);
        } catch (EarlyTermination e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unexpected single quote before end of input string.", s, createPointerString(s, i), state);
        }
        return s;
    }

    private static String createPointerString(String s, int i) {
        String pointer = s.replaceAll(".", " ");
        pointer = pointer.substring(0, i) + '^' + pointer.substring(i + 1, pointer.length());
        return pointer;
    }

    private StringParser accept(StringBuilder sb, char ch, char delim) throws UnterminatedEscapeSequence, StringNotStarted, EarlyTermination {
        // deal with delimiters first
        if (ch == delim && this != ESCAPED)
            return parseDelimiter();

        switch (this) {
            default:
                throw new IllegalStateException(this.toString());
            case UNSTARTED:
                throw new StringNotStarted();
            case UNESCAPED:
                if (ch == '\\')
                    return ESCAPED;
                sb.append(ch);
                return UNESCAPED;
            case ESCAPED:
                return parseEscape(sb, ch);
            case OCTAL2LEFT:
                return parseOctal(sb, ch, OCTAL1LEFT, delim);
            case OCTAL1LEFT:
                return parseOctal(sb, ch, UNESCAPED, delim);
            case HEX4LEFT:
                return parseHex(sb, ch, HEX3LEFT);
            case HEX3LEFT:
                return parseHex(sb, ch, HEX2LEFT);
            case HEX2LEFT:
                return parseHex(sb, ch, HEX1LEFT);
            case HEX1LEFT:
                return parseHex(sb, ch, UNESCAPED);
            case COMPLETE:
                // there should not be any more characters 
                throw new EarlyTermination();
        }
    }

    private StringParser parseDelimiter() throws UnterminatedEscapeSequence {
        switch (this) {
            case UNSTARTED:
                return UNESCAPED;
            case HEX4LEFT:
            case HEX3LEFT:
            case HEX2LEFT:
            case HEX1LEFT:
                throw new UnterminatedEscapeSequence();
            default:
                return COMPLETE;
        }
    }

    private StringParser parseOctal(StringBuilder sb, char ch, StringParser nextState, char delim) throws UnterminatedEscapeSequence, EarlyTermination, StringNotStarted {
        int val = Character.digit(ch, 1 << 3);
        if (val == -1)
            // Octal sequence ended early.
            // parse the current character as unescaped
            return UNESCAPED.accept(sb, ch, delim);
        int lastIndex = sb.length() - 1;
        char partial = sb.charAt(lastIndex);
        partial <<= 3;
        partial |= val;
        sb.setCharAt(lastIndex, partial);
        return nextState;
    }

    private StringParser parseHex(StringBuilder sb, char ch, StringParser nextState) throws UnterminatedEscapeSequence {
        int val = Character.digit(ch, 1 << 4);
        if (val == -1)
            throw new UnterminatedEscapeSequence();
        int lastIndex = sb.length() - 1;
        char partial = sb.charAt(lastIndex);
        partial <<= 4;
        partial |= val;
        sb.setCharAt(lastIndex, partial);
        return nextState;
    }

    private StringParser parseEscape(StringBuilder sb, char ch) throws UnterminatedEscapeSequence {
        switch (ch) {
            case 't':
                sb.append('\t');
                return UNESCAPED;
            case 'n':
                sb.append('\n');
                return UNESCAPED;
            case 'b':
                sb.append('\b');
                return UNESCAPED;
            case 'r':
                sb.append('\r');
                return UNESCAPED;
            case 'f':
                sb.append('\f');
                return UNESCAPED;
            case '"':
                sb.append('"');
                return UNESCAPED;
            case '\'':
                sb.append('\'');
                return UNESCAPED;
            case '\\':
                sb.append('\\');
                return UNESCAPED;
            case '0':
            case '1':
            case '2':
            case '3':
                sb.append((char) (ch - '0'));
                return OCTAL2LEFT;
            case '4':
            case '5':
            case '6':
            case '7':
                sb.append((char) (ch - '0'));
                return OCTAL1LEFT;
            case 'u':
                sb.append('u'); // 'u' will gradually be shifted off to the left by four successive nybbles
                return HEX4LEFT;
            default:
                throw new UnterminatedEscapeSequence();
        }
    }

    private static class StringNotStarted extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static class EarlyTermination extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static class UnterminatedEscapeSequence extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
