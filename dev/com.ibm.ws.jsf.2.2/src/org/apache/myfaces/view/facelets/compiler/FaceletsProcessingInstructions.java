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

/**
 * 
 * @author Leonardo Uribe
 * @since 2.1.0
 */
public final class FaceletsProcessingInstructions
{
    public static final String PROCESS_AS_HTML5 = "html5";
    public static final String PROCESS_AS_JSPX = "jspx";
    public static final String PROCESS_AS_XHTML = "xhtml";
    public static final String PROCESS_AS_XML = "xml";
    
    private static final FaceletsProcessingInstructions FACELETS_PROCESSING_HTML5 =
        new FaceletsProcessingInstructions(
                false, false, false, false, true, false, true, false, true);

    private static final FaceletsProcessingInstructions FACELETS_PROCESSING_XHTML =
        new FaceletsProcessingInstructions(
                false, false, false, false, true, false, true);

    private static final FaceletsProcessingInstructions FACELETS_PROCESSING_XML =
        new FaceletsProcessingInstructions(
                true, true, true, true, true, true, true);

    private static final FaceletsProcessingInstructions FACELETS_PROCESSING_JSPX =
        new FaceletsProcessingInstructions(
                true, true, true, true, false, true, false);
    
    private static final FaceletsProcessingInstructions FACELETS_PROCESSING_HTML5_COMPRESS_SPACES =
        new FaceletsProcessingInstructions(
                false, false, false, false, true, false, true, true, true);
    
    private static final FaceletsProcessingInstructions FACELETS_PROCESSING_XHTML_COMPRESS_SPACES =
        new FaceletsProcessingInstructions(
                false, false, false, false, true, false, true, true);

    private static final FaceletsProcessingInstructions FACELETS_PROCESSING_XML_COMPRESS_SPACES =
        new FaceletsProcessingInstructions(
                true, true, true, true, true, true, true, true);

    private static final FaceletsProcessingInstructions FACELETS_PROCESSING_JSPX_COMPRESS_SPACES =
        new FaceletsProcessingInstructions(
                true, true, true, true, false, true, false, true);

    private final boolean consumeXmlDocType;
    
    private final boolean consumeXmlDeclaration;
    
    private final boolean consumeProcessingInstructions;
    
    private final boolean consumeCDataSections;
    
    private final boolean escapeInlineText;
    
    private final boolean consumeXMLComments;
    
    private final boolean swallowCDataContent;
    
    private final boolean compressSpaces;
    
    private final boolean html5Doctype;
    
    public static FaceletsProcessingInstructions getProcessingInstructions(String processAs)
    {
        if (processAs == null)
        {
            return FACELETS_PROCESSING_HTML5;
        }
        else if (PROCESS_AS_HTML5.equals(processAs))
        {
            return FACELETS_PROCESSING_HTML5;
        }
        else if (PROCESS_AS_XHTML.equals(processAs))
        {
            return FACELETS_PROCESSING_XHTML;
        }
        else if (PROCESS_AS_XML.equals(processAs))
        {
            return FACELETS_PROCESSING_XML;
        }
        else if (PROCESS_AS_JSPX.equals(processAs))
        {
            return FACELETS_PROCESSING_JSPX;
        }
        else
        {
            return FACELETS_PROCESSING_XHTML;
        }
    }
    
    public static FaceletsProcessingInstructions getProcessingInstructions(
            String processAs, boolean compressSpaces)
    {
        if (!compressSpaces)
        {
            return getProcessingInstructions(processAs);
        }
        if (processAs == null)
        {
            return FACELETS_PROCESSING_HTML5_COMPRESS_SPACES;
        }
        else if (PROCESS_AS_HTML5.equals(processAs))
        {
            return FACELETS_PROCESSING_HTML5_COMPRESS_SPACES;
        }
        else if (PROCESS_AS_XHTML.equals(processAs))
        {
            return FACELETS_PROCESSING_XHTML_COMPRESS_SPACES;
        }
        else if (PROCESS_AS_XML.equals(processAs))
        {
            return FACELETS_PROCESSING_XML_COMPRESS_SPACES;
        }
        else if (PROCESS_AS_JSPX.equals(processAs))
        {
            return FACELETS_PROCESSING_JSPX_COMPRESS_SPACES;
        }
        else
        {
            return FACELETS_PROCESSING_XHTML_COMPRESS_SPACES;
        }
    }    
    
    public FaceletsProcessingInstructions(
            boolean consumeXmlDocType,
            boolean consumeXmlDeclaration,
            boolean consumeProcessingInstructions,
            boolean consumeCDataSections, 
            boolean escapeInlineText,
            boolean consumeXMLComments,
            boolean swallowCDataContent)
    {
        this(consumeXmlDocType, 
            consumeXmlDeclaration, 
            consumeProcessingInstructions, 
            consumeCDataSections, 
            escapeInlineText, 
            consumeXMLComments, 
            swallowCDataContent, 
            false);
    }
    
    public FaceletsProcessingInstructions(
            boolean consumeXmlDocType,
            boolean consumeXmlDeclaration,
            boolean consumeProcessingInstructions,
            boolean consumeCDataSections, 
            boolean escapeInlineText,
            boolean consumeXMLComments,
            boolean swallowCDataContent,
            boolean compressSpaces)
    {
        this(consumeXmlDocType, 
            consumeXmlDeclaration, 
            consumeProcessingInstructions, 
            consumeCDataSections, 
            escapeInlineText, 
            consumeXMLComments, 
            swallowCDataContent, 
            compressSpaces,
            false);
    }
    
    public FaceletsProcessingInstructions(
            boolean consumeXmlDocType,
            boolean consumeXmlDeclaration,
            boolean consumeProcessingInstructions,
            boolean consumeCDataSections, 
            boolean escapeInlineText,
            boolean consumeXMLComments,
            boolean swallowCDataContent,
            boolean compressSpaces,
            boolean html5Doctype)
    {
        super();
        this.consumeXmlDocType = consumeXmlDocType;
        this.consumeXmlDeclaration = consumeXmlDeclaration;
        this.consumeProcessingInstructions = consumeProcessingInstructions;
        this.consumeCDataSections = consumeCDataSections;
        this.escapeInlineText = escapeInlineText;
        this.consumeXMLComments = consumeXMLComments;
        this.swallowCDataContent = swallowCDataContent;
        this.compressSpaces = compressSpaces;
        this.html5Doctype = html5Doctype;
    }    

    public boolean isConsumeXmlDocType()
    {
        return consumeXmlDocType;
    }

    public boolean isConsumeXmlDeclaration()
    {
        return consumeXmlDeclaration;
    }

    public boolean isConsumeProcessingInstructions()
    {
        return consumeProcessingInstructions;
    }

    public boolean isConsumeCDataSections()
    {
        return consumeCDataSections;
    }

    public boolean isEscapeInlineText()
    {
        return escapeInlineText;
    }

    public boolean isConsumeXMLComments()
    {
        return consumeXMLComments;
    }

    public boolean isSwallowCDataContent()
    {
        return swallowCDataContent;
    }

    /**
     * @return the compressSpaces
     */
    public boolean isCompressSpaces()
    {
        return compressSpaces;
    }

    public boolean isHtml5Doctype()
    {
        return html5Doctype;
    }
}
