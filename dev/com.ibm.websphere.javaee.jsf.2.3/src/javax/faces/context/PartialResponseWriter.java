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
package javax.faces.context;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * @since 2.0
 */
public class PartialResponseWriter extends ResponseWriterWrapper
{
    public static final String RENDER_ALL_MARKER = "javax.faces.ViewRoot";
    public static final String VIEW_STATE_MARKER = "javax.faces.ViewState";

    private boolean hasChanges;
    private String insertType;

   
    /**
     * 
     */
    public PartialResponseWriter(ResponseWriter writer)
    {
        super(writer);
    }

    public void delete(String targetId) throws IOException
    {
        startChanges();
        
        startElement ("delete", null);
        writeAttribute ("id", targetId, null);
        endElement ("delete");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws IOException
    {
        if (hasChanges)
        {
            // Close the <insert> element, if any.
            //error close the last op if any
            endInsert();
            
            endElement ("changes");
            
            hasChanges = false;
        }
        
        endElement ("partial-response");
    }

    public void endError() throws IOException
    {
        // Close open <error-message> element.
        
        endCDATA();
        endElement ("error-message");
        endElement ("error");
    }

    public void endEval() throws IOException
    {
        // Close open <eval> element.
        
        endCDATA();
        endElement ("eval");
    }

    public void endExtension() throws IOException
    {
        endElement ("extension");
    }

    public void endInsert() throws IOException
    {
        if (insertType == null)
        {
            // No insert started; ignore.
            
            return;
        }
        
        // Close open <insert> element.
        
        endCDATA();
        endElement (insertType);
        endElement ("insert");
        
        insertType = null;
    }

    public void endUpdate() throws IOException
    {
        endCDATA();
        endElement ("update");
    }

    public void redirect(String url) throws IOException
    {
        startElement ("redirect", null);
        writeAttribute ("url", url, null);
        endElement ("redirect");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startDocument() throws IOException
    {
        // JSF 2.2 section 2.2.6.1 Render Response Partial Processing
        // use writePreamble(...)
        //_wrapped.write ("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        
        startElement ("partial-response", null);
        
        // If by some reason the response has been reset, and the same
        // PartialResponseWriter is used, it is necessary to ensure any 
        // variable is initialized in a consistent state. To do that,
        // the best point is when the document is started.
        hasChanges = false;
        insertType = null;
    }

    public void startError(String errorName) throws IOException
    {
        startElement ("error", null);
        
        startElement ("error-name", null);
        write (errorName);
        endElement ("error-name");
        
        startElement ("error-message", null);
        startCDATA();
        
        // Leave open; caller will write message.
    }

    public void startEval() throws IOException
    {
        startChanges();
        
        startElement ("eval", null);
        startCDATA();
        
        // Leave open; caller will write statements.
    }

    public void startExtension(Map<String, String> attributes) throws IOException
    {
        Iterator<String> attrNames;
        
        startChanges();
        
        startElement ("extension", null);
        
        // Write out extension attributes.
        // TODO: schema mentions "id" attribute; not used?
        
        attrNames = attributes.keySet().iterator();
        
        while (attrNames.hasNext())
        {
            String attrName = attrNames.next();
            
            writeAttribute (attrName, attributes.get (attrName), null);
        }
        
        // Leave open; caller will write extension elements.
    }

    public void startInsertAfter(String targetId) throws IOException
    {
        startInsertCommon ("after", targetId);
    }

    public void startInsertBefore(String targetId) throws IOException
    {
        startInsertCommon ("before", targetId);
    }

    public void startUpdate(String targetId) throws IOException
    {
        startChanges();
        
        startElement ("update", null);
        writeAttribute ("id", targetId, null);
        startCDATA();
        
        // Leave open; caller will write content.
    }

    public void updateAttributes(String targetId, Map<String, String> attributes) throws IOException
    {
        Iterator<String> attrNames;
        
        startChanges();
        
        startElement ("attributes", null);
        writeAttribute ("id", targetId, null);
        
        attrNames = attributes.keySet().iterator();
        
        while (attrNames.hasNext())
        {
            String attrName = attrNames.next();
            
            startElement ("attribute", null);
            writeAttribute ("name", attrName, null);
            writeAttribute ("value", attributes.get (attrName), null);
            endElement ("attribute");
        }
        
        endElement ("attributes");
    }
    
    private void startChanges () throws IOException
    {
        if (!hasChanges)
        {
            startElement ("changes", null);
            
            hasChanges = true;
        }
    }
    
    private void startInsertCommon (String type, String targetId) throws IOException
    {
        if (insertType != null)
        {
            // An insert has already been started; ignore.
            
            return;
        }
        
        insertType = type;
        
        startChanges();
        
        startElement ("insert", null);
        startElement (insertType, null);
        writeAttribute ("id", targetId, null);
        startCDATA();
        
        // Leave open; caller will write content.
    }
}
