/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.jcdi.util.html;

import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * The representation of a Html Tag. A Lazy evaluation is used.
 * 
 * @author yingwang
 * 
 */
public class HtmlTag {

    /**
     * The original CharBuffer that stores the html tag.
     */
    protected CharBuffer buffer;

    /**
     * abs start of the tag in the buffer.
     */
    protected int start;

    /**
     * abs end of the tag in the buffer.
     */
    protected int end;

    /**
     * The tag name.
     */
    protected String name;

    /**
     * The abs index of next char after the tag name.
     */
    protected int tagNameEnd;

    /**
     * Tag attribute <name, value> pairs.
     */
    protected Map<String, String> attributes;

    /**
     * The constructor.
     * 
     * @param buffer0 input charbuffer that stores the tag.
     */
    public HtmlTag(CharBuffer buffer0) {
        this.buffer = buffer0;
        this.start = buffer.position();
        this.end = buffer.limit();

        int i = 0, ss = -1, ee = -1;
        char ch;
        name = null;
        tagNameEnd = -1;
        for (i = start; i < end; i++) {
            ch = buffer.get(i);
            switch (ch) {
                case '<':
                    ss = 0;
                    break;
                case ' ':
                case '\r':
                case '\n':
                case '\t':
                    if (ss > 0) {
                        ee = i;
                    }
                    break;
                case '>':
                    if (ss >= 0) {
                        ee = i;
                    }
                    break;
                default:
                    if (ss == 0) {
                        ss = i;
                    }
            }
            if (ee > 0)
                break;
        }
        tagNameEnd = ee;
        if (ss > 0 && ee >= ss) {
            name = CharBuffer.wrap(buffer, ss - start, ee - start).toString().toLowerCase();
        }
    }

    /**
     * get the tag name.
     * 
     * @return the tag name.
     */
    public String getName() {
        return name;
    }

    /**
     * Whether or not the current index is at the end of a tag.
     * 
     * @param input
     * @param index
     * @param end
     * 
     * @return true of at the end of tag.
     */
    protected boolean isEndOfTag(CharBuffer input, int index, int end) {
        if (index >= end - 1)
            return true;
        if (index == end - 2) {
            char ch = input.get(index);
            if (ch == '/' || ch == '?')
                return true;
        }
        return false;
    }

    /**
     * Skip the index until we see the char 'till' or at the end of a tag.
     * 
     * @param input
     * @param start
     * @param end
     * @param output
     * @param till
     * @return
     */
    protected int skipTill(CharBuffer input, int start, int end,
                           StringBuffer output, char till) {
        char ch;
        int i;
        for (i = start; i < end; i++) {
            ch = input.get(i);
            if (ch == till || isEndOfTag(input, i, end))
                return i;
            output.append(ch);
        }
        return i;
    }

    /**
     * Skip the index until we see the char 'till' or at the end of a tag.
     * 
     * @param input
     * @param start
     * @param end
     * @param till
     * @return
     */
    protected int skipTill(CharBuffer input, int start, int end,
                           char till) {
        char ch;
        int i;
        for (i = start; i < end; i++) {
            ch = input.get(i);
            if (ch == till || isEndOfTag(input, i, end))
                return i;
        }
        return i;
    }

    /**
     * Skip the index until we see the char 'till' or at the end of a tag.
     * Then contents between quote pair are also skipped.
     * 
     * @param input
     * @param start
     * @param end
     * @param output
     * @param till
     * @return
     */
    protected int skipTillAndQuote(CharBuffer input, int start, int end,
                                   StringBuffer output, char till) {
        char ch;
        int i;
        for (i = start; i < end; i++) {
            ch = input.get(i);
            if (ch == '"' || ch == '\'') {
                output.append(ch);
                i = skipTill(input, i + 1, end, output, ch);
                if (i < end) {
                    output.append(ch);
                }
                continue;
            }
            if (ch == till || isEndOfTag(input, i, end))
                return i;
            output.append(ch);
        }
        return i;
    }

    /**
     * Skip the index until we see the char 'till' or at the end of a tag.
     * Then contents between quote pair are also skipped.
     * 
     * @param input
     * @param start
     * @param end
     * @param till
     * @return
     */
    protected int skipTillAndQuote(CharBuffer input, int start, int end,
                                   char till) {
        char ch;
        int i;
        for (i = start; i < end; i++) {
            ch = input.get(i);
            if (ch == '"' || ch == '\'') {
                i = skipTill(input, i + 1, end, ch);
                continue;
            }
            if (ch == till || isEndOfTag(input, i, end))
                return i;
        }
        return i;
    }

    /**
     * Parse the attribute of a tag.
     * 
     */
    public void parseAttributes() {
        char ch;
        int index = tagNameEnd, attrNameStart, attrNameEnd, valueStart, valueEnd;
        String attributeName, attributeValue;
        attributes = new HashMap<String, String>();

        while (index < end) {

            // skip space between attribute.
            ch = buffer.get(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            } else if (isEndOfTag(buffer, index, end)) {
                index = end;
                continue;
            }

            //We are here to start to handle new attribute
            attrNameStart = index;
            attrNameEnd = skipTill(buffer, attrNameStart, end, '=');
            if (attrNameEnd <= attrNameStart) {
                index = this.skipTillAndQuote(buffer, attrNameEnd, end, ' ');
                continue;
            }
            attributeName = CharBuffer.wrap(buffer, attrNameStart - start, attrNameEnd - start).toString();

            index = attrNameEnd;
            ch = buffer.get(index);
            if (ch != '=') {
                index = this.skipTillAndQuote(buffer, attrNameEnd, end, ' ');
                continue;
            }

            index++;
            ch = buffer.get(index);
            if (ch == '"' || ch == '\'') {
                valueStart = index + 1;
                valueEnd = this.skipTill(buffer, valueStart, end, ch);
                attributeValue = CharBuffer.wrap(buffer, valueStart - start, valueEnd - start).toString();
                attributes.put(attributeName.toLowerCase(), attributeValue);
                index = valueEnd + 1;
                continue;
            } else {
                valueStart = index;
                valueEnd = this.skipTillAndQuote(buffer, valueStart, end, ' ');
                attributeValue = CharBuffer.wrap(buffer, valueStart - start, valueEnd - start).toString();
                attributes.put(attributeName.toLowerCase(), attributeValue);
                index = valueEnd;
                continue;
            }
        }
    }

    /**
     * get the original char buffer.
     * 
     * @return
     */
    public CharBuffer getOriginalBuffer() {
        return this.buffer;
    }

    /**
     * get the value of a attribute.
     * 
     * @param attributeName
     * @return
     */
    public String getAttributeValue(String attributeName) {
        return attributes.get(attributeName);
    }

    // unit test cases.
    public static void main(String[] args) {
        CharBuffer[] testcases = {
                                  CharBuffer.wrap("  <>"),
                                  CharBuffer.wrap("  </>"),
                                  CharBuffer.wrap("  <abc>"),
                                  CharBuffer.wrap("  < abc>"),
                                  CharBuffer.wrap("  < abc >"),
                                  CharBuffer.wrap("  <?xml aaa=\"nnn\" bbb=mmm encoding=\"utf-8\">"),
                                  CharBuffer.wrap("  <meta http-equiv=\"Content-Type\" Content=\"text/html; charset=gb2312\">")
        };
        HtmlTag tag;
        for (int i = 0; i < testcases.length; i++) {
            testcases[i].position(2);
            tag = new HtmlTag(testcases[i]);
            tag.parseAttributes();
            System.out.println("buffer=" + tag.buffer);
            System.out.println("name=" + tag.getName());
            System.out.println("start=" + tag.start);
            System.out.println("end=" + tag.end);
            System.out.println("tagNameEnd=" + tag.tagNameEnd);
            System.out.println("attributes=" + tag.attributes);
            System.out.println("************");
        }
    }
}
