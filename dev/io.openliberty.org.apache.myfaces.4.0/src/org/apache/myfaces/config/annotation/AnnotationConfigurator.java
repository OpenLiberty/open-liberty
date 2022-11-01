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
package org.apache.myfaces.config.annotation;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.faces.FacesException;
import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.behavior.FacesBehavior;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.NamedEvent;
import jakarta.faces.render.FacesBehaviorRenderer;
import jakarta.faces.render.FacesRenderer;
import jakarta.faces.render.RenderKitFactory;
import jakarta.faces.validator.FacesValidator;

import org.apache.myfaces.config.impl.element.ApplicationImpl;
import org.apache.myfaces.config.impl.element.BehaviorImpl;
import org.apache.myfaces.config.impl.element.ComponentImpl;
import org.apache.myfaces.config.impl.element.ComponentTagDeclarationImpl;
import org.apache.myfaces.config.impl.element.ConverterImpl;
import org.apache.myfaces.config.impl.element.FacesConfigImpl;
import org.apache.myfaces.spi.AnnotationProvider;
import org.apache.myfaces.spi.AnnotationProviderFactory;
import org.apache.myfaces.util.lang.StringUtils;

/**
 * Configure all annotations that needs to be defined at startup.
 *
 * <ul>
 * <li>{@link jakarta.faces.component.FacesComponent}</li>
 * <li>{@link jakarta.faces.convert.FacesConverter}</li>
 * <li>{@link jakarta.faces.validator.FacesValidator}</li>
 * <li>{@link jakarta.faces.render.FacesRenderer}</li>
 * <li>{@link jakarta.faces.render.FacesBehaviorRenderer}</li>
 * </ul>
 * <p>
 * Some parts copied from org.apache.shale.tiger.view.faces.LifecycleListener2
 * </p>
 *
 * @since 2.0
 * @author Leonardo Uribe (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class AnnotationConfigurator
{
    private static final Logger log = Logger.getLogger(AnnotationConfigurator.class.getName());

    public AnnotationConfigurator()
    {
    }

    public FacesConfigImpl createFacesConfig(ExternalContext externalcontext, boolean metadataComplete)
    {
        if (!metadataComplete)
        {
            AnnotationProvider provider = AnnotationProviderFactory.getAnnotationProviderFactory(externalcontext).
                    getAnnotationProvider(externalcontext);
            Map<Class<? extends Annotation>, Set<Class<?>>> map = provider.getAnnotatedClasses(externalcontext);
            return createFacesConfig(map);
        }
        return null;
    }

    /**
     * TODO: Implement strategy pattern over this method.
     * 
     * @param map
     * @return
     */
    protected FacesConfigImpl createFacesConfig(Map<Class<? extends Annotation>, Set<Class<?>>> map)
    {
        FacesConfigImpl facesConfig = new FacesConfigImpl();

        Set<Class<?>> classes = map.get(FacesComponent.class);

        if (classes != null && !classes.isEmpty())
        {
            for (Class<?> clazz : classes)
            {
                FacesComponent comp = (FacesComponent) clazz.getAnnotation(FacesComponent.class);
                if (comp != null)
                {
                    if (log.isLoggable(Level.FINEST))
                    {
                        log.finest("addComponent(" + comp.value() + ',' + clazz.getName() + ')');
                    }

                    String value = comp.value();
                    if (StringUtils.isEmpty(value))
                    {
                        String simpleName = clazz.getSimpleName();
                        value = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
                    }
                    facesConfig.addComponent(new ComponentImpl(value, clazz.getName()));
                    
                    if (comp.createTag())
                    {
                        String tagName = comp.tagName();
                        if (StringUtils.isEmpty(tagName))
                        {
                            tagName = clazz.getSimpleName();
                            tagName = Character.toLowerCase(tagName.charAt(0)) + tagName.substring(1);
                        }

                        facesConfig.addComponentTagDeclaration(value, 
                                new ComponentTagDeclarationImpl(value, comp.namespace(), tagName));
                    }
                }
            }
        }

        classes = map.get(FacesConverter.class);
        if (classes != null && !classes.isEmpty())
        {
            for (Class<?> clazz : classes)
            {
                FacesConverter conv = (FacesConverter) clazz.getAnnotation(FacesConverter.class);
                if (conv != null)
                {
                    if (log.isLoggable(Level.FINEST))
                    {
                        log.finest("addConverter(" + conv.value() + ',' + clazz.getName() + ')');
                    }

                    //If there is a previous entry on Application Configuration Resources,
                    //the entry there takes precedence
                    boolean hasForClass = !Object.class.equals(conv.forClass());
                    boolean hasValue = conv.value().length() > 0;
                    if (hasForClass || hasValue)
                    {
                        ConverterImpl converter = new ConverterImpl();
                        if (hasForClass)
                        {
                            converter.setForClass(conv.forClass().getName());
                        }
                        if (hasValue)
                        {
                            converter.setConverterId(conv.value());
                        }
                        converter.setConverterClass(clazz.getName());
                        facesConfig.addConverter(converter);
                    }
                    else
                    {
                        throw new FacesException("@FacesConverter must have value, forClass or both. Check annotation "
                                                 + "@FacesConverter on class: " + clazz.getName());
                    }
                }
            }
        }

        classes = map.get(FacesValidator.class);
        if (classes != null && !classes.isEmpty())
        {
            for (Class<?> clazz : classes)
            {
                FacesValidator val = (FacesValidator) clazz.getAnnotation(FacesValidator.class);
                if (val != null)
                {
                    if (log.isLoggable(Level.FINEST))
                    {
                        log.finest("addValidator(" + val.value() + ',' + clazz.getName() + ')');
                    }

                    String value = val.value();
                    if (value == null || value.isEmpty())
                    {
                        String simpleName = clazz.getSimpleName();
                        value = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
                    }
                    facesConfig.addValidator(value, clazz.getName());
                    if (val.isDefault())
                    {
                        ApplicationImpl app = null;
                        if (facesConfig.getApplications().isEmpty())
                        {
                            app = new ApplicationImpl();
                        }
                        else
                        {
                            app = (ApplicationImpl) facesConfig.getApplications().get(0);
                        }
                        app.addDefaultValidatorId(value);
                    }
                }
            }
        }

        classes = map.get(FacesRenderer.class);
        if (classes != null && !classes.isEmpty())
        {
            for (Class<?> clazz : classes)
            {
                FacesRenderer rend = (FacesRenderer) clazz.getAnnotation(FacesRenderer.class);
                if (rend != null)
                {
                    String renderKitId = rend.renderKitId();
                    if (renderKitId == null)
                    {
                        renderKitId = RenderKitFactory.HTML_BASIC_RENDER_KIT;
                    }
                    if (log.isLoggable(Level.FINEST))
                    {
                        log.finest("addRenderer(" + renderKitId + ", "
                                + rend.componentFamily() + ", " + rend.rendererType()
                                + ", " + clazz.getName() + ')');
                    }

                    org.apache.myfaces.config.impl.element.RenderKitImpl renderKit =
                            (org.apache.myfaces.config.impl.element.RenderKitImpl)
                                    facesConfig.getRenderKit(renderKitId);
                    if (renderKit == null)
                    {
                        renderKit = new org.apache.myfaces.config.impl.element.RenderKitImpl();
                        renderKit.setId(renderKitId);
                        facesConfig.addRenderKit(renderKit);
                    }

                    org.apache.myfaces.config.impl.element.RendererImpl renderer =
                            new org.apache.myfaces.config.impl.element.RendererImpl();
                    renderer.setComponentFamily(rend.componentFamily());
                    renderer.setRendererClass(clazz.getName());
                    renderer.setRendererType(rend.rendererType());
                    renderKit.addRenderer(renderer);
                }
            }
        }

        classes = map.get(NamedEvent.class);
        if (classes != null && !classes.isEmpty())
        {
            handleNamedEvent(facesConfig, classes);
        }

        classes = map.get(FacesBehavior.class);
        if (classes != null && !classes.isEmpty())
        {
            handleFacesBehavior(facesConfig, classes);
        }

        classes = map.get(FacesBehaviorRenderer.class);
        if (classes != null && !classes.isEmpty())
        {
            handleFacesBehaviorRenderer(facesConfig, classes);
        }

        return facesConfig;
    }

    private void handleNamedEvent(FacesConfigImpl facesConfig, Set<Class<?>> classes)
    {
        for (Class<?> clazz : classes)
        {
            NamedEvent namedEvent = (NamedEvent) clazz.getAnnotation(NamedEvent.class);

            if (namedEvent != null)
            {
                // Can only apply @NamedEvent to ComponentSystemEvent subclasses.
                if (!ComponentSystemEvent.class.isAssignableFrom(clazz))
                {
                    // Just log this.  We'll catch it later in the runtime.
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning(clazz.getName() + " is annotated with @jakarta.faces.event.NamedEvent, but does "
                                    + "not extend jakarta.faces.event.ComponentSystemEvent");
                    }
                }
                // Have to register @NamedEvent annotations with the NamedEventManager class since
                // we need to get access to this info later and can't from the dispenser (it's not a
                // singleton).
                org.apache.myfaces.config.impl.element.NamedEventImpl namedEventConfig =
                        new org.apache.myfaces.config.impl.element.NamedEventImpl();
                namedEventConfig.setEventClass(clazz.getName());
                namedEventConfig.setShortName(namedEvent.shortName());
                facesConfig.addNamedEvent(namedEventConfig);
            }
        }
    }

    private void handleFacesBehavior(FacesConfigImpl facesConfig, Set<Class<?>> classes)
    {
        for (Class<?> clazz : classes)
        {
            FacesBehavior facesBehavior = (FacesBehavior) clazz.getAnnotation(FacesBehavior.class);

            if (facesBehavior != null)
            {
                // Can only apply @FacesBehavior to Behavior implementors.
                if (!jakarta.faces.component.behavior.Behavior.class.isAssignableFrom(clazz))
                {
                    // Just log this.  We'll catch it later in the runtime.
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning(clazz.getName()
                                    + " is annotated with @jakarta.faces.component.behavior.FacesBehavior, "
                                    + "but does not implement jakarta.faces.component.behavior.Behavior");
                    }
                }

                if (log.isLoggable(Level.FINEST))
                {
                    log.finest("addBehavior(" + facesBehavior.value() + ", " + clazz.getName() + ')');
                }

                BehaviorImpl behavior = new BehaviorImpl();
                behavior.setBehaviorId(facesBehavior.value());
                behavior.setBehaviorClass(clazz.getName());
                facesConfig.addBehavior(behavior);
            }

        }
    }
    
    private void handleFacesBehaviorRenderer(FacesConfigImpl facesConfig, Set<Class<?>> classes)
    {
        for (Class<?> clazz : classes)
        {
            FacesBehaviorRenderer facesBehaviorRenderer
                    = (FacesBehaviorRenderer) clazz.getAnnotation(FacesBehaviorRenderer.class);

            if (facesBehaviorRenderer != null)
            {
                String renderKitId = facesBehaviorRenderer.renderKitId();
                if (log.isLoggable(Level.FINEST))
                {
                    log.finest("addClientBehaviorRenderer(" + renderKitId + ", "
                               + facesBehaviorRenderer.rendererType() + ", "
                               + clazz.getName() + ')');
                }

                org.apache.myfaces.config.impl.element.RenderKitImpl renderKit =
                        (org.apache.myfaces.config.impl.element.RenderKitImpl)
                                facesConfig.getRenderKit(renderKitId);
                if (renderKit == null)
                {
                    renderKit = new org.apache.myfaces.config.impl.element.RenderKitImpl();
                    renderKit.setId(renderKitId);
                    facesConfig.addRenderKit(renderKit);
                }

                org.apache.myfaces.config.impl.element.ClientBehaviorRendererImpl cbr =
                        new org.apache.myfaces.config.impl.element.ClientBehaviorRendererImpl();
                cbr.setRendererType(facesBehaviorRenderer.rendererType());
                cbr.setRendererClass(clazz.getName());
                renderKit.addClientBehaviorRenderer(cbr);
            }
        }
    }
}
