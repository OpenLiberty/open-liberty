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
package org.apache.myfaces.spi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.faces.FacesWrapper;
import javax.faces.context.ExternalContext;

/**
 * This interface provide a way to override myfaces annotation scanning algorithm that
 * needs to be found at startup: 
 * 
 * <ul>
 * <li>{@link javax.faces.bean.ManagedBean}</li>
 * <li>{@link javax.faces.component.FacesComponent}</li>
 * <li>{@link javax.faces.component.behavior.FacesBehavior}</li>
 * <li>{@link javax.faces.convert.FacesConverter}</li>
 * <li>{@link javax.faces.event.NamedEvent}</li>
 * <li>{@link javax.faces.render.FacesRenderer}</li>
 * <li>{@link javax.faces.render.FacesBehaviorRenderer}</li>
 * <li>{@link javax.faces.validator.FacesValidator}</li>
 * </ul>
 * 
 * <p>This is provided to allow application containers solve the following points</p>
 * <ul>
 * <li>It is common application containers to have its own protocols to handle files. It is
 * not the same to scan annotation inside a jar than look on a directory.</li>
 * <li>If the application container has some optimization related to annotation scanning or
 * it already did that task, it is better to reuse that information instead do the same
 * thing twice.</li>
 * </ul>
 * 
 * <p>To override this class, create a file on a jar file with the following entry name:
 * /META-INF/services/org.apache.myfaces.spi.AnnotationProvider and put the desired class name of
 * the class that will override or extend the default AnnotationProvider.
 * </p>
 * 
 * <p>To wrap the default AnnotationProvider, use a constructor like 
 * CustomAnnotationProvider(AnnotationProvider ap)</p>
 * 
 * @since 2.0.2
 * @author Leonardo Uribe 
 */
public abstract class AnnotationProvider implements FacesWrapper<AnnotationProvider>
{

    /**
     * Retrieve a map containing the classes that contains annotations used by jsf implementation at
     * startup.
     * <p>The default implementation must comply with JSF 2.0 spec section 11.5.1 Requirements for scanning of 
     * classes for annotations. 
     * </p>
     * <p>This method could call getBaseUrls() to obtain a list of URL that could be used to indicate jar files of
     * annotations in the classpath.
     * </p>
     * <p>If the <faces-config> element in the WEB-INF/faces-config.xml file contains metadata-complete attribute 
     * whose value is "true", this method should not be called.
     * </p>
     * 
     * @param ctx The current ExternalContext
     * @return A map with all classes that could contain annotations.
     */
    public abstract Map<Class<? extends Annotation>,Set<Class<?>>> getAnnotatedClasses(ExternalContext ctx);

    /**
     * <p>The returned Set&lt;URL&gt; urls are calculated in this way
     * ( see JSF 2.0 spec section 11.4.2 for definitions )
     * </p>
     * <ol>
     * <li>All resources that match either "META-INF/faces-config.xml" or end with ".facesconfig.xml" directly in 
     * the "META-INF" directory (considered <code>applicationConfigurationResources)<code></li>
     * </ol>
     * 
     * @deprecated 
     * @return
     */
    @Deprecated
    public abstract Set<URL> getBaseUrls() throws IOException;
    
    /**
     * Same as getBaseUrls(), but with the ExternalContext reference.
     * By default it calls to getBaseUrls()
     * 
     * @since 2.1.9, 2.0.15
     * @param ctx
     * @return
     * @throws IOException 
     */
    public Set<URL> getBaseUrls(ExternalContext ctx) throws IOException
    {
        return getBaseUrls();
    }
    
    public AnnotationProvider getWrapped()
    {
        return null;
    }    
}
