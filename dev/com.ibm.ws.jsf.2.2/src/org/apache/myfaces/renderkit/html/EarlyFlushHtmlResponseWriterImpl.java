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
package org.apache.myfaces.renderkit.html;

import java.io.IOException;
import java.io.Writer;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import org.apache.myfaces.shared.renderkit.html.HTML;
import org.apache.myfaces.shared.renderkit.html.HtmlResponseWriterImpl;

/**
 * This implementation is just the default html response writer with the early flush logic. The
 * idea is detect when the end "head" element is rendered and in that moment, when the flush call
 * is done, force the flush of the current underlying writer.
 *
 * @author Leonardo Uribe
 */
public class EarlyFlushHtmlResponseWriterImpl extends HtmlResponseWriterImpl
{
    /**
     * Check if a end head tag was recently done. The idea is 
     */
    private boolean _endHeadTag;

    public EarlyFlushHtmlResponseWriterImpl(Writer writer, String contentType, String characterEncoding)
    {
        super(writer, contentType, characterEncoding);
    }

    public EarlyFlushHtmlResponseWriterImpl(Writer writer, String contentType, String characterEncoding, 
            boolean wrapScriptContentWithXmlCommentTag)
    {
        super(writer, contentType, characterEncoding, wrapScriptContentWithXmlCommentTag);
    }

    public EarlyFlushHtmlResponseWriterImpl(Writer writer, String contentType, String characterEncoding,
            boolean wrapScriptContentWithXmlCommentTag, String writerContentTypeMode) throws FacesException
    {
        super(writer, contentType, characterEncoding, wrapScriptContentWithXmlCommentTag, writerContentTypeMode);
    }

    @Override
    public ResponseWriter cloneWithWriter(Writer writer)
    {
        EarlyFlushHtmlResponseWriterImpl newWriter
                = new EarlyFlushHtmlResponseWriterImpl(writer, getContentType(), getCharacterEncoding(), 
                        getWrapScriptContentWithXmlCommentTag(), getWriterContentTypeMode());
        return newWriter;        
    }

    @Override
    public void startElement(String name, UIComponent uiComponent) throws IOException
    {
        _endHeadTag = false;
        super.startElement(name, uiComponent);
    }

    @Override
    public void endElement(String name) throws IOException
    {
        super.endElement(name);
        if (HTML.HEAD_ELEM.equalsIgnoreCase(name))
        {
            _endHeadTag = true;
        }
    }
    
    @Override
    public void flush() throws IOException
    {
        super.flush();
        
        if (_endHeadTag)
        {
            FacesContext facesContext = getFacesContext();
            if (!facesContext.getPartialViewContext().isAjaxRequest() &&
                !facesContext.getPartialViewContext().isPartialRequest())
            {
                forceFlush();
            }
        }
    }
}
