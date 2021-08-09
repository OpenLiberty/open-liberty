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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletException;
import javax.faces.view.facelets.FaceletHandler;
import javax.faces.view.facelets.TagDecorator;

import org.apache.myfaces.config.element.FaceletsProcessing;
import org.apache.myfaces.view.facelets.tag.BaseMultipleTagDecorator;
import org.apache.myfaces.view.facelets.tag.BaseTagDecorator;
import org.apache.myfaces.view.facelets.tag.CompositeTagDecorator;
import org.apache.myfaces.view.facelets.tag.CompositeTagLibrary;
import org.apache.myfaces.view.facelets.tag.TagLibrary;
import org.apache.myfaces.view.facelets.tag.jsf.html.DefaultTagDecorator;
import org.apache.myfaces.view.facelets.util.ParameterCheck;
import org.apache.myfaces.view.facelets.util.ReflectionUtil;

/**
 * A Compiler instance may handle compiling multiple sources
 * 
 * @author Jacob Hookom
 * @version $Id: Compiler.java 1542444 2013-11-16 01:41:08Z lu4242 $
 */
public abstract class Compiler
{

    //protected final static Logger log = Logger.getLogger("facelets.compiler");
    protected final static Logger log = Logger.getLogger(Compiler.class.getName());

    public final static String EXPRESSION_FACTORY = "compiler.ExpressionFactory";

    private static final TagLibrary EMPTY_LIBRARY = new CompositeTagLibrary(new TagLibrary[0]);

    private boolean validating = false;

    private boolean trimmingWhitespace = false;

    private boolean trimmingComments = false;

    private final List<TagLibrary> libraries = new ArrayList<TagLibrary>();

    private final List<TagDecorator> decorators = new ArrayList<TagDecorator>();

    private final Map<String, String> features = new HashMap<String, String>();

    private boolean developmentProjectStage = false;

    private Collection<FaceletsProcessing> faceletsProcessingConfigurations;

    /**
     * 
     */
    public Compiler()
    {

    }

    public final FaceletHandler compile(URL src, String alias) throws IOException, FaceletException, ELException,
            FacesException
    {
        return this.doCompile(src, alias);
    }
    
    public final FaceletHandler compileViewMetadata(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException
    {
        return this.doCompileViewMetadata(src, alias);
    }
    
    public final FaceletHandler compileCompositeComponentMetadata(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException
    {
        return this.doCompileCompositeComponentMetadata(src, alias);
    }
    
    public final FaceletHandler compileComponent(
        String taglibURI, String tagName, Map<String,Object> attributes)
    {
        return this.doCompileComponent(taglibURI, tagName, attributes);
    }

    protected abstract FaceletHandler doCompile(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException;

    protected abstract FaceletHandler doCompileViewMetadata(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException;
    
    protected abstract FaceletHandler doCompileCompositeComponentMetadata(URL src, String alias)
            throws IOException, FaceletException, ELException, FacesException;
    
    protected abstract FaceletHandler doCompileComponent(
        String taglibURI, String tagName, Map<String,Object> attributes);

    public final TagDecorator createTagDecorator()
    {
        if (this.decorators.size() > 0)
        {
            return new BaseMultipleTagDecorator(new DefaultTagDecorator(), 
                new CompositeTagDecorator(this.decorators.toArray(
                    new TagDecorator[this.decorators.size()])));
        }
        // JSF 2.2 has always enabled the default tag decorator.
        return new BaseTagDecorator(new DefaultTagDecorator());
        //return EMPTY_DECORATOR;
    }

    public final void addTagDecorator(TagDecorator decorator)
    {
        ParameterCheck.notNull("decorator", decorator);
        if (!this.decorators.contains(decorator))
        {
            this.decorators.add(decorator);
        }
    }

    public final ExpressionFactory createExpressionFactory()
    {
        ExpressionFactory el = null;
        el = (ExpressionFactory) this.featureInstance(EXPRESSION_FACTORY);
        if (el == null)
        {
            try
            {
                el = FacesContext.getCurrentInstance().getApplication().getExpressionFactory();
                if (el == null)
                {
                    log.warning("No default ExpressionFactory from Faces Implementation, "
                                + "attempting to load from Feature["
                                + EXPRESSION_FACTORY + "]");
                }
            }
            catch (Exception e)
            {
                // do nothing
            }
        }
        
        return el;
    }

    private final Object featureInstance(String name)
    {
        String type = (String) this.features.get(name);
        if (type != null)
        {
            try
            {
                return ReflectionUtil.forName(type).newInstance();
            }
            catch (Throwable t)
            {
                throw new FaceletException("Could not instantiate feature[" + name + "]: " + type);
            }
        }
        return null;
    }

    public final TagLibrary createTagLibrary()
    {
        if (this.libraries.size() > 0)
        {
            return new CompositeTagLibrary(this.libraries.toArray(new TagLibrary[this.libraries.size()]));
        }
        return EMPTY_LIBRARY;
    }

    public final void addTagLibrary(TagLibrary library)
    {
        ParameterCheck.notNull("library", library);
        if (!this.libraries.contains(library))
        {
            this.libraries.add(library);
        }
    }

    public final void setFeature(String name, String value)
    {
        this.features.put(name, value);
    }

    public final String getFeature(String name)
    {
        return (String) this.features.get(name);
    }

    public final boolean isTrimmingComments()
    {
        return this.trimmingComments;
    }

    public final void setTrimmingComments(boolean trimmingComments)
    {
        this.trimmingComments = trimmingComments;
    }

    public final boolean isTrimmingWhitespace()
    {
        return this.trimmingWhitespace;
    }

    public final void setTrimmingWhitespace(boolean trimmingWhitespace)
    {
        this.trimmingWhitespace = trimmingWhitespace;
    }

    public final boolean isValidating()
    {
        return this.validating;
    }

    public final void setValidating(boolean validating)
    {
        this.validating = validating;
    }
    
    public final boolean isDevelopmentProjectStage()
    {
        return this.developmentProjectStage;
    }
    
    public final void setDevelopmentProjectStage(boolean developmentProjectStage)
    {
        this.developmentProjectStage = developmentProjectStage;
    }

    /**
     * 
     * @since 2.1.0
     * @return
     */
    public Collection<FaceletsProcessing> getFaceletsProcessingConfigurations()
    {
        return faceletsProcessingConfigurations;
    }

    /**
     * 
     * @since 2.1.0
     * @return
     */
    public void setFaceletsProcessingConfigurations(
            Collection<FaceletsProcessing> faceletsProcessingConfigurations)
    {
        this.faceletsProcessingConfigurations = faceletsProcessingConfigurations;
    }
}

