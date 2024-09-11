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
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.Doctype;
import jakarta.faces.view.Location;
import jakarta.faces.view.facelets.FaceletHandler;
import jakarta.faces.view.facelets.Tag;
import jakarta.faces.view.facelets.TagAttribute;
import jakarta.faces.view.facelets.TagAttributeException;
import jakarta.faces.view.facelets.TagDecorator;
import jakarta.faces.view.facelets.TagException;
import org.apache.myfaces.view.facelets.DoctypeImpl;

import org.apache.myfaces.view.facelets.tag.TagAttributesImpl;
import org.apache.myfaces.view.facelets.tag.TagLibrary;
import org.apache.myfaces.view.facelets.tag.composite.CompositeLibrary;
import org.apache.myfaces.view.facelets.tag.composite.ImplementationHandler;
import org.apache.myfaces.view.facelets.tag.composite.InterfaceHandler;
import org.apache.myfaces.view.facelets.tag.ui.ComponentRefHandler;
import org.apache.myfaces.view.facelets.tag.ui.CompositionHandler;
import org.apache.myfaces.view.facelets.tag.ui.UILibrary;

/**
 * Compilation unit for managing the creation of a single FaceletHandler based on events from an XML parser.
 * 
 * @see org.apache.myfaces.view.facelets.compiler.Compiler
 * 
 * @author Jacob Hookom
 * @version $Id$
 */
final class CompilationManager
{

    private final static Logger log = Logger.getLogger(CompilationManager.class.getName());

    private final Compiler compiler;

    private final TagLibrary tagLibrary;

    private final TagDecorator tagDecorator;

    private final NamespaceManager namespaceManager;

    private final Stack<CompilationUnit> units;

    private int tagId;

    private boolean finished;

    private final String alias;
    
    private CompilationUnit interfaceCompilationUnit;
    
    private final FaceletsProcessingInstructions faceletsProcessingInstructions;
    
    private Doctype doctype;

    public CompilationManager(String alias, Compiler compiler, FaceletsProcessingInstructions instructions)
    {

        // this is our alias
        this.alias = alias;

        // grab compiler state
        this.compiler = compiler;
        this.tagDecorator = compiler.createTagDecorator();
        this.tagLibrary = compiler.createTagLibrary();

        // namespace management
        this.namespaceManager = new NamespaceManager();

        // tag uids
        this.tagId = 0;

        // for composition use
        this.finished = false;

        // our compilationunit stack
        this.units = new Stack<CompilationUnit>();
        this.units.push(new CompilationUnit());
        
        this.interfaceCompilationUnit = null; 
        this.faceletsProcessingInstructions = instructions;
    }

    public void writeInstruction(String value, Location location)
    {
        if (this.finished)
        {
            return;
        }

        // don't carelessly add empty tags
        if (value.length() == 0)
        {
            return;
        }

        TextUnit unit;
        if (this.currentUnit() instanceof TextUnit)
        {
            unit = (TextUnit) this.currentUnit();
        }
        else
        {
            unit = new TextUnit(this.alias, this.nextTagId(), 
                    faceletsProcessingInstructions.isEscapeInlineText(),
                    faceletsProcessingInstructions.isCompressSpaces(),
                    location);
            this.startUnit(unit);
        }
        unit.writeInstruction(value);
    }
    
    public void writeDoctype(String name, String publicId, String systemId)
    {
        if (this.finished)
        {
            return;
        }

        DoctypeUnit unit = new DoctypeUnit(this.alias, this.nextTagId(),
            name, publicId, systemId, faceletsProcessingInstructions.isHtml5Doctype());
        this.doctype = new DoctypeImpl(name, publicId, systemId);
        this.startUnit(unit);
    }

    public void writeText(String value, Location location)
    {

        if (this.finished)
        {
            return;
        }

        // don't carelessly add empty tags
        if (value.length() == 0)
        {
            return;
        }

        TextUnit unit;
        if (this.currentUnit() instanceof TextUnit)
        {
            unit = (TextUnit) this.currentUnit();
        }
        else
        {
            unit = new TextUnit(this.alias, this.nextTagId(), 
                    faceletsProcessingInstructions.isEscapeInlineText(),
                    faceletsProcessingInstructions.isCompressSpaces(),
                    location);
            this.startUnit(unit);
        }
        unit.write(value);
    }

    public void writeComment(String text, Location location)
    {
        if (this.compiler.isTrimmingComments())
        {
            return;
        }

        if (this.finished)
        {
            return;
        }

        // don't carelessly add empty tags
        if (text.length() == 0)
        {
            return;
        }

        TextUnit unit;
        if (this.currentUnit() instanceof TextUnit)
        {
            unit = (TextUnit) this.currentUnit();
        }
        else
        {
            unit = new TextUnit(this.alias, this.nextTagId(), 
                    faceletsProcessingInstructions.isEscapeInlineText(),
                    faceletsProcessingInstructions.isCompressSpaces(),
                    location);
            this.startUnit(unit);
        }

        unit.writeComment(text);
    }

    public void writeWhitespace(String text, Location location)
    {
        if (!this.compiler.isTrimmingWhitespace())
        {
            this.writeText(text, location);
        }
    }

    private String nextTagId()
    {
        return Integer.toHexString(Math.abs(this.alias.hashCode() ^ 13 * this.tagId++));
    }

    public void pushTag(Tag orig)
    {

        if (this.finished)
        {
            return;
        }

        if (log.isLoggable(Level.FINE))
        {
            log.fine("Tag Pushed: " + orig);
        }

        Tag t = this.tagDecorator.decorate(orig);
        String[] qname = this.determineQName(t);
        t = this.trimAttributes(t);

        if (isTrimmed(qname[0], qname[1]))
        {
            log.fine("Composition Found, Popping Parent Tags");
            this.units.clear();
            NamespaceUnit nsUnit = this.namespaceManager.toNamespaceUnit(this.tagLibrary);
            this.units.push(nsUnit);
            this.startUnit(new TrimmedTagUnit(this.tagLibrary, qname[0], qname[1], t, this.nextTagId()));
            log.fine("New Namespace and [Trimmed] TagUnit pushed");
        }
        else if (isRemove(qname[0], qname[1]))
        {
            this.units.push(new RemoveUnit());
        }
        else if (isCompositeComponentInterface(qname[0], qname[1]))
        {
            // Here we have two cases when we found a <composite:interface> tag:
            //
            // -  If a page has a <composite:interface> tag and a <composite:implementation> tag.
            //   In this case, we need to trim all tags outside this two tags, otherwise these
            //   unwanted tags will be added when the composite component is applied.
            //   Unfortunately, this is the only point we can do it, because after the compiler,
            //   html tags are wrapped on facelets UIInstruction or UIText components as "list",
            //   losing the original structure required to trim.
            //
            // -  If a page has a <composite:interface> tag and not a <composite:implementation> tag.
            //   In this case, it is not necessary to trim, because we use the facelet only to create
            //   metadata and the component tree created is not used (see
            //   ViewDeclarationLanguage.getComponentMetadata() ). On InterfaceHandler, instead
            //   there is some code that found the right component in the temporal tree to add the
            //   generated BeanInfo, which it is retrieved later.
            //
            // After use Template Client API for composite components, it was found the need to
            // gather metadata information from 
            log.fine("Composite Component Interface Found, saving unit");
            CompositeComponentUnit compositeRootCompilationUnit = new CompositeComponentUnit();
            this.startUnit(compositeRootCompilationUnit);
            interfaceCompilationUnit = new TagUnit(this.tagLibrary, qname[0], qname[1], t, this.nextTagId());
            this.startUnit(interfaceCompilationUnit);
        }        
        else if (isCompositeComponentImplementation(qname[0], qname[1]))
        {
            log.fine("Composite component Found, Popping Parent Tags");
            this.units.clear();
            NamespaceUnit nsUnit = this.namespaceManager.toNamespaceUnit(this.tagLibrary);
            this.units.push(nsUnit);
            CompositeComponentUnit compositeRootCompilationUnit = new CompositeComponentUnit();
            this.startUnit(compositeRootCompilationUnit);
            if (interfaceCompilationUnit != null)
            {
                this.currentUnit().addChild(interfaceCompilationUnit);
                interfaceCompilationUnit = null;
            }
            this.startUnit(new TrimmedTagUnit(this.tagLibrary, qname[0], qname[1], t, this.nextTagId()));
            log.fine("New Namespace and TagUnit pushed");
        }        
        else if (this.tagLibrary.containsTagHandler(qname[0], qname[1]))
        {
            this.startUnit(new TagUnit(this.tagLibrary, qname[0], qname[1], t, this.nextTagId()));
        }
        else if (this.tagLibrary.containsNamespace(qname[0]))
        {
            throw new TagException(orig, "Tag Library supports namespace: " + qname[0]
                    + ", but no tag was defined for name: " + qname[1]);
        }
        else
        {
            TextUnit unit;
            if (this.currentUnit() instanceof TextUnit)
            {
                unit = (TextUnit) this.currentUnit();
            }
            else
            {
                unit = new TextUnit(this.alias, this.nextTagId(),
                        faceletsProcessingInstructions.isEscapeInlineText(),
                        faceletsProcessingInstructions.isCompressSpaces(),
                        orig.getLocation());
                this.startUnit(unit);
            }
            
            if (this.compiler.isDevelopmentProjectStage())
            {
                String qName = null;
                boolean isPrefixed = false;
                TagAttribute jsfc = t.getAttributes().get("jsfc");
                if (jsfc != null)
                {
                    qName = jsfc.getValue();
                    if (jsfc.getValue().indexOf(':') > 0)
                    {
                        isPrefixed = true;
                    }
                }
                else if (t.getQName().indexOf(':') > 0 )
                {
                    qName = t.getQName();
                    isPrefixed = true;
                }
                if (isPrefixed)
                {
                    unit.addMessage(FacesMessage.SEVERITY_WARN,
                            "Warning: The page "+alias+" declares namespace "+qname[0]+ 
                            " and uses the tag " + qName + " , but no TagLibrary associated to namespace.", 
                            "Warning: The page "+alias+" declares namespace "+qname[0]+ 
                            " and uses the tag " + qName + " , but no TagLibrary associated to namespace. "+
                            "Please check the namespace name and if it is correct, it is probably that your " +
                            "library .taglib.xml cannot be found on the current classpath, or if you are " +
                            "referencing a composite component library check your library folder match with the " +
                            "namespace and can be located by the installed ResourceHandler.");
                }
            }
            
            unit.startTag(t);
        }
    }

    public void popTag()
    {

        if (this.finished)
        {
            return;
        }

        CompilationUnit unit = this.currentUnit();

        if (unit instanceof TextUnit)
        {
            TextUnit t = (TextUnit) unit;
            if (t.isClosed())
            {
                this.finishUnit();
            }
            else
            {
                t.endTag();
                return;
            }
        }

        unit = this.currentUnit();
        if (unit instanceof TagUnit)
        {
            TagUnit t = (TagUnit) unit;
            if (t instanceof TrimmedTagUnit)
            {
                this.finished = true;
                return;
            }
        }
        else if (unit instanceof CompositeComponentUnit)
        {
            this.finished = true;
            return;
        }

        this.finishUnit();
    }

    public void popNamespace(String ns)
    {
        this.namespaceManager.popNamespace(ns);
        if (this.currentUnit() instanceof NamespaceUnit)
        {
            this.finishUnit();
        }
    }

    public void pushNamespace(String prefix, String uri)
    {

        if (log.isLoggable(Level.FINE))
        {
            log.fine("Namespace Pushed " + prefix + ": " + uri);
        }

        this.namespaceManager.pushNamespace(prefix, uri);
        NamespaceUnit unit;
        if (this.currentUnit() instanceof NamespaceUnit)
        {
            unit = (NamespaceUnit) this.currentUnit();
        }
        else
        {
            unit = new NamespaceUnit(this.tagLibrary);
            this.startUnit(unit);
        }
        unit.setNamespace(prefix, uri);
    }

    public FaceletHandler createFaceletHandler()
    {
        return this.units.get(0).createFaceletHandler();
    }

    private CompilationUnit currentUnit()
    {
        if (!this.units.isEmpty())
        {
            return this.units.peek();
        }
        return null;
    }

    private void finishUnit()
    {
        Object obj = this.units.pop();

        if (log.isLoggable(Level.FINE))
        {
            log.fine("Finished Unit: " + obj);
        }
    }

    private void startUnit(CompilationUnit unit)
    {

        if (log.isLoggable(Level.FINE))
        {
            log.fine("Starting Unit: " + unit + " and adding it to parent: " + this.currentUnit());
        }

        this.currentUnit().addChild(unit);
        this.units.push(unit);
    }

    private Tag trimAttributes(Tag tag)
    {
        Tag t = this.trimJSFCAttribute(tag);
        t = this.trimNSAttributes(t);
        return t;
    }

    protected static boolean isRemove(String ns, String name)
    {
        return "remove".equals(name) && (UILibrary.NAMESPACE.equals(ns)
                || UILibrary.JCP_NAMESPACE.equals(ns)
                || UILibrary.SUN_NAMESPACE.equals(ns));
    }

    protected static boolean isTrimmed(String ns, String name)
    {
        return (CompositionHandler.NAME.equals(name) || ComponentRefHandler.NAME.equals(name)) &&
                (UILibrary.NAMESPACE.equals(ns)
                    || UILibrary.JCP_NAMESPACE.equals(ns)
                    || UILibrary.SUN_NAMESPACE.equals(ns));
    }
    
    protected static boolean isCompositeComponentInterface(String ns, String name)
    {
        return InterfaceHandler.NAME.equals(name) && (CompositeLibrary.NAMESPACE.equals(ns)
                || CompositeLibrary.JCP_NAMESPACE.equals(ns)
                || CompositeLibrary.SUN_NAMESPACE.equals(ns));
    }

    protected static boolean isCompositeComponentImplementation(String ns, String name)
    {
        return ImplementationHandler.NAME.equals(name) && (CompositeLibrary.NAMESPACE.equals(ns)
                || CompositeLibrary.JCP_NAMESPACE.equals(ns)
                || CompositeLibrary.SUN_NAMESPACE.equals(ns));
    }

    private String[] determineQName(Tag tag)
    {
        TagAttribute attr = tag.getAttributes().get("jsfc");
        if (attr != null)
        {
            if (log.isLoggable(Level.FINE))
            {
                log.fine(attr + " Faces Facelet Compile Directive found");
            }
            String value = attr.getValue();
            String namespace;
            String localName;
            int c = value.indexOf(':');
            if (c == -1)
            {
                namespace = this.namespaceManager.getNamespace("");
                localName = value;
            }
            else
            {
                String prefix = value.substring(0, c);
                namespace = this.namespaceManager.getNamespace(prefix);
                if (namespace == null)
                {
                    throw new TagAttributeException(tag, attr, "No Namespace matched for: " + prefix);
                }
                localName = value.substring(c + 1);
            }
            return new String[] { namespace, localName };
        }
        else
        {
            return new String[] { tag.getNamespace(), tag.getLocalName() };
        }
    }

    private Tag trimJSFCAttribute(Tag tag)
    {
        TagAttribute attr = tag.getAttributes().get("jsfc");
        if (attr != null)
        {
            TagAttribute[] oa = tag.getAttributes().getAll();
            TagAttribute[] na = new TagAttribute[oa.length - 1];
            int p = 0;
            for (int i = 0; i < oa.length; i++)
            {
                if (!"jsfc".equals(oa[i].getLocalName()))
                {
                    na[p++] = oa[i];
                }
            }
            return new Tag(tag, new TagAttributesImpl(na));
        }
        return tag;
    }

    private Tag trimNSAttributes(Tag tag)
    {
        TagAttribute[] attr = tag.getAttributes().getAll();
        int remove = 0;
        for (int i = 0; i < attr.length; i++)
        {
            if (attr[i].getQName().startsWith("xmlns") && this.tagLibrary.containsNamespace(attr[i].getValue()))
            {
                remove |= 1 << i;
                if (log.isLoggable(Level.FINE))
                {
                    log.fine(attr[i] + " Namespace Bound to TagLibrary");
                }
            }
        }
        if (remove == 0)
        {
            return tag;
        }
        else
        {
            List<TagAttribute> attrList = new ArrayList<TagAttribute>(attr.length);
            int p = 0;
            for (int i = 0; i < attr.length; i++)
            {
                p = 1 << i;
                if ((p & remove) == p)
                {
                    continue;
                }
                attrList.add(attr[i]);
            }
            attr = attrList.toArray(new TagAttribute[attrList.size()]);
            return new Tag(tag.getLocation(), tag.getNamespace(), tag.getLocalName(), tag.getQName(),
                           new TagAttributesImpl(attr));
        }
    }

    /**
     * 
     * @since 2.1.0
     * @return
     */
    public FaceletsProcessingInstructions getFaceletsProcessingInstructions()
    {
        return faceletsProcessingInstructions;
    }

    public Doctype getDoctype()
    {
        return doctype;
    }
}

