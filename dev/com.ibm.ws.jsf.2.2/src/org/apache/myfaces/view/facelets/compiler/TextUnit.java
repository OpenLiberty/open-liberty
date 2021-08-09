/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.compiler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.el.ELException;
import javax.faces.application.FacesMessage;
import javax.faces.view.facelets.CompositeFaceletHandler;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.Tag;
import javax.faces.view.facelets.TagAttribute;
import javax.faces.view.facelets.TagException;

import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.view.facelets.el.ELText;

/**
 * 
 * @author Jacob Hookom
 * @version $Id: TextUnit.java 1569412 2014-02-18 16:28:27Z lu4242 $
 */
final class TextUnit extends CompilationUnit
{

    private final StringBuffer buffer;

    private final StringBuffer textBuffer;

    private final List<Instruction> instructionBuffer;

    private final Stack<Tag> tags;

    private final List<Object> children;

    private boolean startTagOpen;

    private final String alias;

    private final String id;
    
    private final List<Object> messages;

    private final boolean escapeInlineText;

    private final boolean compressSpaces;

    public TextUnit(String alias, String id)
    {
        this(alias,id,true);
    }
    
    public TextUnit(String alias, String id, boolean escapeInlineText)
    {
        this(alias,id,escapeInlineText,false);
    }
    
    public TextUnit(String alias, String id, boolean escapeInlineText, boolean compressSpaces)
    {
        this.alias = alias;
        this.id = id;
        this.buffer = new StringBuffer();
        this.textBuffer = new StringBuffer();
        this.instructionBuffer = new ArrayList<Instruction>();
        this.tags = new Stack<Tag>();
        this.children = new ArrayList<Object>();
        this.startTagOpen = false;
        this.messages = new ArrayList<Object>(4);
        this.escapeInlineText = escapeInlineText;
        this.compressSpaces = compressSpaces;
    }

    public FaceletHandler createFaceletHandler()
    {
        this.flushBufferToConfig(true);

        if (this.children.size() == 0)
        {
            return LEAF;
        }

        FaceletHandler[] h = new FaceletHandler[this.children.size()];
        Object obj;
        for (int i = 0; i < h.length; i++)
        {
            obj = this.children.get(i);
            if (obj instanceof FaceletHandler)
            {
                h[i] = (FaceletHandler) obj;
            }
            else
            {
                h[i] = ((CompilationUnit) obj).createFaceletHandler();
            }
        }
        if (h.length == 1)
        {
            return h[0];
        }
        return new CompositeFaceletHandler(h);
    }

    private void addInstruction(Instruction instruction)
    {
        this.flushTextBuffer(false);
        this.instructionBuffer.add(instruction);
    }

    private void flushTextBuffer(boolean child)
    {
        if (this.textBuffer.length() > 0)
        {
            String s = this.textBuffer.toString();

            if (child)
            {
                s = trimRight(s);
            }
            if (s.length() > 0)
            {
                if (!compressSpaces)
                {
                    //Do it as usual.
                    ELText txt = ELText.parse(s);
                    if (txt != null)
                    {
                        if (txt.isLiteral())
                        {
                            if (escapeInlineText)
                            {
                                this.instructionBuffer.add(new LiteralTextInstruction(txt.toString()));
                            }
                            else
                            {
                                this.instructionBuffer.add(new LiteralNonExcapedTextInstruction(txt.toString()));
                            }
                        }
                        else
                        {
                            if (escapeInlineText)
                            {
                                this.instructionBuffer.add(new TextInstruction(this.alias, txt ));
                            }
                            else
                            {
                                // When escape inline text is disabled (jspx case) we have to split the EL and add
                                // separate instructions, so it can be properly escaped.
                                ELText[] splitText = ELText.parseAsArray(s);
                                if (splitText.length > 1)
                                {
                                    Instruction[] array = new Instruction[splitText.length];
                                    for (int i = 0; i < splitText.length; i++)
                                    {
                                        ELText selText = splitText[i];
                                        if (selText.isLiteral())
                                        {
                                            array[i] = new LiteralNonExcapedTextInstruction(selText.toString());
                                        }
                                        else
                                        {
                                            array[i] = new TextInstruction(this.alias, selText );
                                        }
                                    }
                                    this.instructionBuffer.add(new CompositeTextInstruction(array));
                                }
                                else
                                {
                                    this.instructionBuffer.add(new TextInstruction(this.alias, txt ));
                                }
                            }
                        }
                    }
                }
                else
                {
                    // First check if the text contains EL before build something, and if contains 
                    // an EL expression, compress it before build the ELText.
                    if (s != null && s.length() > 0)
                    {
                        if (ELText.isLiteral(s))
                        {
                            if (escapeInlineText)
                            {
                                this.instructionBuffer.add(new LiteralTextInstruction(s));
                            }
                            else
                            {
                                this.instructionBuffer.add(new LiteralNonExcapedTextInstruction(s));
                            }
                        }
                        else
                        {
                            if (instructionBuffer.size() > 0 && 
                                !(instructionBuffer.get(instructionBuffer.size()-1) instanceof LiteralXMLInstruction))
                            {
                                s = compressELText(s);
                            }
                            // When escape inline text is disabled (jspx case) we have to split the EL and add
                            // separate instructions, so it can be properly escaped.
                            ELText[] splitText = ELText.parseAsArray(s);
                            if (splitText.length > 1)
                            {
                                Instruction[] array = new Instruction[splitText.length];
                                for (int i = 0; i < splitText.length; i++)
                                {
                                    ELText selText = splitText[i];
                                    if (selText.isLiteral())
                                    {
                                        array[i] = new LiteralNonExcapedTextInstruction(selText.toString());
                                    }
                                    else
                                    {
                                        array[i] = new TextInstruction(this.alias, selText );
                                    }
                                }
                                this.instructionBuffer.add(new CompositeTextInstruction(array));
                            }
                            else
                            {
                                this.instructionBuffer.add(new TextInstruction(this.alias, ELText.parse(s)));
                            }
                        }
                    }
                }
            }

        }
        this.textBuffer.setLength(0);
    }

    public void write(String text)
    {
        this.finishStartTag();
        this.textBuffer.append(text);
        this.buffer.append(text);
    }

    public void writeInstruction(String text)
    {
        this.finishStartTag();
        ELText el = ELText.parse(text);
        if (el.isLiteral())
        {
            this.addInstruction(new LiteralXMLInstruction(text));
        }
        else
        {
            this.addInstruction(new XMLInstruction(el));
        }
        this.buffer.append(text);
    }

    public void writeComment(String text)
    {
        this.finishStartTag();

        ELText el = ELText.parse(text);
        if (el.isLiteral())
        {
            this.addInstruction(new LiteralCommentInstruction(text));
        }
        else
        {
            this.addInstruction(new CommentInstruction(el));
        }

        this.buffer.append("<!--" + text + "-->");
    }

    public void startTag(Tag tag)
    {

        // finish any previously written tags
        this.finishStartTag();

        // push this tag onto the stack
        this.tags.push(tag);

        // write it out
        this.buffer.append('<');
        this.buffer.append(tag.getQName());

        this.addInstruction(new StartElementInstruction(tag.getQName()));

        TagAttribute[] attrs = tag.getAttributes().getAll();
        if (attrs.length > 0)
        {
            for (int i = 0; i < attrs.length; i++)
            {
                String qname = attrs[i].getQName();
                String value = attrs[i].getValue();
                this.buffer.append(' ').append(qname).append("=\"").append(value).append("\"");

                ELText txt = ELText.parse(value);
                if (txt != null)
                {
                    if (txt.isLiteral())
                    {
                        this.addInstruction(new LiteralAttributeInstruction(qname, txt.toString()));
                    }
                    else
                    {
                        this.addInstruction(new AttributeInstruction(this.alias, qname, txt));
                    }
                }
            }
        }
        
        if (!messages.isEmpty())
        {
            for (Iterator<Object> it = messages.iterator(); it.hasNext();)
            {
                Object[] message = (Object[])it.next();
                this.addInstruction(new AddFacesMessageInstruction((FacesMessage.Severity) message[0],
                                                                   (String)message[1], (String)message[2]));
                it.remove();
            }
        }

        // notify that we have an open tag
        this.startTagOpen = true;
    }

    private void finishStartTag()
    {
        if (this.tags.size() > 0 && this.startTagOpen)
        {
            this.buffer.append(">");
            this.startTagOpen = false;
        }
    }

    public void endTag()
    {
        Tag tag = (Tag) this.tags.pop();

        if (HTML.BODY_ELEM.equalsIgnoreCase(tag.getQName()))
        {
            this.addInstruction(new BodyEndElementInstruction(tag.getQName()));
        }
        else
        {
            this.addInstruction(new EndElementInstruction(tag.getQName()));            
        }

        if (this.startTagOpen)
        {
            this.buffer.append("/>");
            this.startTagOpen = false;
        }
        else
        {
            this.buffer.append("</").append(tag.getQName()).append('>');
        }
    }

    public void addChild(CompilationUnit unit)
    {
        // if we are adding some other kind of unit
        // then we need to capture our buffer into a UITextHandler
        this.finishStartTag();
        this.flushBufferToConfig(true);
        this.children.add(unit);
    }

    protected void flushBufferToConfig(boolean child)
    {
        this.flushTextBuffer(child);

        int size = this.instructionBuffer.size();
        if (size > 0)
        {
            try
            {
                String s = this.buffer.toString();
                if (child)
                {
                    s = trimRight(s);
                }
                ELText txt = ELText.parse(s);
                if (txt != null)
                {
                    if (compressSpaces)
                    {
                        // Use the logic behind the instructions to remove unnecessary instructions
                        // containing only spaces, or recreating new ones containing only the necessary
                        // spaces.
                        size = compressSpaces(instructionBuffer, size);
                    }
                    Instruction[] instructions = (Instruction[]) this.instructionBuffer
                            .toArray(new Instruction[size]);
                    this.children.add(new UIInstructionHandler(this.alias, this.id, instructions, txt));
                    this.instructionBuffer.clear();
                }

            }
            catch (ELException e)
            {
                if (this.tags.size() > 0)
                {
                    throw new TagException((Tag) this.tags.peek(), e.getMessage());
                }
                else
                {
                    throw new ELException(this.alias + ": " + e.getMessage(), e.getCause());
                }
            }
        }

        // ALWAYS CLEAR FOR BOTH IMPL
        this.buffer.setLength(0);
    }

    public boolean isClosed()
    {
        return this.tags.empty();
    }

    private final static String trimRight(String s)
    {
        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i)))
        {
            i--;
        }
        if (i >= 0)
        {
            return s;
        }
        else
        {
            return "";
        }
        /*
        if (i == s.length() - 1)
        {
            return s;
        }
        else
        {
            return s.substring(0, i + 1);
        }*/
    }
    
    final static String compressELText(String text)
    {
        //int firstCharLocation = getFirstTextCharLocationIgnoringSpacesTabsAndCarriageReturn(text);
        int firstCharLocation = -1;
        int leftChar = 0; // 0=first char on left 1=\n 2=\r 3=\r\n
        int lenght = text.length();
        String leftText = null;
        for (int j = 0; j < lenght; j++)
        {
            char c = text.charAt(j);
            if (leftChar == 0)
            {
                if (c == '\r')
                {
                    leftChar = 2;
                    if (j+1 < lenght)
                    {
                        if (text.charAt(j+1) == '\n')
                        {
                            leftChar = 3;
                        }
                    }
                }
                if (c == '\n')
                {
                    leftChar = 1;
                }
            }
            if (Character.isWhitespace(c))
            {
                continue;
            }
            else
            {
                firstCharLocation = j;
                break;
            }
        }
        if (firstCharLocation == -1)
        {
            firstCharLocation = lenght;
        }
        // Define the character on the left
        if (firstCharLocation > 0)
        {
            switch (leftChar)
            {
                case 1:
                    leftText = "\n";
                    break;
                case 2:
                    leftText = "\r";
                    break;
                case 3:
                    leftText = "\r\n";
                    break;
                default:
                    leftText = (lenght > 1) ? text.substring(0,1) : text;
                    break;
            }                
        }
        else
        {
            leftText = "";
        }
                
        int lastCharLocation = getLastTextCharLocationIgnoringSpacesTabsAndCarriageReturn(text);
        if (firstCharLocation == 0 && lastCharLocation == text.length()-1)
        {
            return text;
        }
        else
        {
            if (lastCharLocation+1 < text.length())
            {
                lastCharLocation = lastCharLocation+1;
            }
            if (firstCharLocation == 0)
            {
                return text.substring(firstCharLocation, lastCharLocation+1);
            }
            else
            {
                return leftText+text.substring(firstCharLocation, lastCharLocation+1);
            }
        }
    }
    
    /**
     * Compress spaces around a list of instructions, following these rules:
     * 
     * - The first instruction that is on the left usually make contact with a component.
     * 
     * @param instructionBuffer
     * @param size
     * @return 
     */
    final static int compressSpaces(List<Instruction> instructionBuffer, int size)
    {
        boolean addleftspace = true;
        boolean addrightspace = false;
        boolean skipnext = false;
        for (int i = 0; i < size; i++)
        {
            String text = null;
            String newText = null;
            int instructionType = 0;
            if (skipnext)
            {
                skipnext = false;
                continue;
            }
            Instruction ins = instructionBuffer.get(i);
            if (i+1 == size)
            {
                addrightspace = true;
            }
            
            if (ins instanceof LiteralTextInstruction)
            {
                text = ((LiteralTextInstruction)ins).getText();
                instructionType = 1;
            }
            else if (ins instanceof LiteralNonExcapedTextInstruction)
            {
                text = ((LiteralTextInstruction)ins).getText();
                instructionType = 2;
            }
            else if (ins instanceof LiteralXMLInstruction)
            {
                skipnext = true;
                continue;
            }
            
            if (text != null && text.length() > 0)
            {
                int firstCharLocation = -1;
                int leftChar = 0; // 0=first char on left 1=\n 2=\r 3=\r\n
                int lenght = text.length();
                String leftText = null;
                for (int j = 0; j < lenght; j++)
                {
                    char c = text.charAt(j);
                    if (leftChar == 0)
                    {
                        if (c == '\r')
                        {
                            leftChar = 2;
                            if (j+1 < lenght)
                            {
                                if (text.charAt(j+1) == '\n')
                                {
                                    leftChar = 3;
                                }
                            }
                        }
                        if (c == '\n')
                        {
                            leftChar = 1;
                        }
                    }
                    if (Character.isWhitespace(c))
                    {
                        continue;
                    }
                    else
                    {
                        firstCharLocation = j;
                        break;
                    }
                }
                if (firstCharLocation == -1)
                {
                    firstCharLocation = lenght;
                }
                // Define the character on the left
                if (firstCharLocation > 0)
                {
                    switch (leftChar)
                    {
                        case 1:
                            leftText = "\n";
                            break;
                        case 2:
                            leftText = "\r";
                            break;
                        case 3:
                            leftText = "\r\n";
                            break;
                        default:
                            leftText = (lenght > 1) ? text.substring(0,1) : text;
                            break;
                    }                
                }
                else
                {
                    leftText = "";
                }
                
                if (firstCharLocation == lenght && lenght > 1)
                {
                    // All the instruction is space, replace with an instruction 
                    // with only one space
                    if (addleftspace || addrightspace)
                    {
                        newText = leftText;
                    }
                    else
                    {
                        instructionBuffer.remove(i);
                        i--;
                        size--;
                    }
                }
                else
                {
                    int lastCharLocation = getLastTextCharLocationIgnoringSpacesTabsAndCarriageReturn(text);
                    // If right space, increment in 1
                    if (lastCharLocation+1 < text.length())
                    {
                        lastCharLocation = lastCharLocation+1;
                    }
                    if (firstCharLocation > 0)
                    {
                        newText = leftText+
                            text.substring(firstCharLocation, lastCharLocation+1);
                    }
                    else
                    {
                        newText = text.substring(firstCharLocation, lastCharLocation+1);
                    }
                }
                
                if (newText != null)
                {
                    if (instructionType == 1)
                    {
                        instructionBuffer.set(i, new LiteralTextInstruction(newText));
                    }
                    else if (instructionType == 2)
                    {
                        instructionBuffer.set(i, new LiteralNonExcapedTextInstruction(newText));
                    }
                }
            }
            addleftspace = false;
        }
        return size;
    }

    private static int getFirstTextCharLocationIgnoringSpacesTabsAndCarriageReturn(String text)
    {
        for (int i = 0; i < text.length(); i++)
        {
            if (Character.isWhitespace(text.charAt(i)))
            {
                continue;
            }
            else
            {
                return i;
            }
        }
        return text.length();
    }
    
    private static int getLastTextCharLocationIgnoringSpacesTabsAndCarriageReturn(String text)
    {
        for (int i = text.length()-1; i >= 0; i--)
        {
            if (Character.isWhitespace(text.charAt(i)))
            {
                continue;
            }
            else
            {
                return i;
            }
        }
        return 0;
    }

    public String toString()
    {
        return "TextUnit[" + this.children.size() + "]";
    }
    
    public void addMessage(FacesMessage.Severity severity, String summary, String detail)
    {
        this.messages.add(new Object[]{severity, summary, detail});
    }
}
