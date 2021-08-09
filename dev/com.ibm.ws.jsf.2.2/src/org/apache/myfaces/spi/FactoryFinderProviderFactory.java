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

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.myfaces.shared.util.ClassUtils;

/**
 * <p>{@link javax.faces.FactoryFinder} is a class with three methods:</p>
 * 
 * <code>
 * public final class FactoryFinder
 * {
 *    public static Object getFactory(String factoryName) throws FacesException {...}
 *    public static void setFactory(String factoryName, String implName) {...}
 *    public static void releaseFactories() throws FacesException {...}
 * }
 * </code>
 * 
 * <p>The javadoc describe the intention of FactoryFinder class:
 * </p>
 * 
 * <p>"... FactoryFinder implements the standard discovery algorithm for all factory 
 * objects specified in the JavaServer Faces APIs. For a given factory class name, a 
 * corresponding implementation class is searched for based on the following 
 * algorithm...."</p>
 * 
 * <p>In few words, this class allows to find JSF factory classes. The necessary 
 * information to create factory instances is loaded on initialization time, 
 * but which locations contains such information (for more information see 
 * JSF 2.0 spec section 11.4.2) (here the only interest is in jsf factories 
 * initialization information) ?</p>
 * 
 * <ul>
 * <li>Look factories on META-INF/services/[factoryClassName]</li>
 * <li>Look META-INF/faces-config.xml or META-INF/[prefix].faces-config.xml</li>
 * <li>Look the files pointed by javax.faces.CONFIG_FILES web config param 
 *     (note WEB-INF/web.xml is taken into consideration)</li>
 * <li>Look the applicationFacesConfig on WEB-INF/faces-config.xml</li>
 * </ul>
 * 
 * <p>Based on the previous facts, the first conclusion to take into account arise: 
 * Configuration information is gathered per "web context". What is a "web context"? 
 * In simple terms, is the "space" where a web application is deployed. 
 * Let's suppose an EAR file with two WAR files: a.war and b.war. 
 * Both contains different "web applications" and when are deployed has 
 * different "web context", so both can provide different factory configuration, 
 * because both has different WEB-INF/web.xml and WEB-INF/faces-config.xml files.</p>
 * 
 * <p>Now, given a request, how the web container identify a "web context"? 
 * At start, it receives the request information and based on that it decides 
 * which web application should process it. After that, it assign to a thread 
 * from is thread pool to be processed and the control is passed to the proper 
 * filters/servlets.</p> 
 * 
 * <p>So, if there is not a servlet context/portlet context/whatever context, 
 * how to identify a "web context"? The answer is using the thread, but the one 
 * who knows how to do that is the web container, not the jsf implementation.</p>
 * 
 * <p>The existing problem is caused by a "shortcut" taken to make things easier. 
 * Instead use the current "thread", it is taken as advantage the fact that each 
 * web application deployed has a different classloader. That is true for a lot 
 * of application servers, so the current implementation of FactoryFinder is based 
 * on that fact too and has worked well since the beginning.</p>
 * 
 * <p>Now let's examine in detail how a "single classloader per EAR" option could 
 * work. If the EAR has two WAR files (a.war and b.war), we have two web context, 
 * and the initialization code is executed twice. When all FactoryFinder methods 
 * are called?</p>
 * 
 * <ul>
 * <li>FactoryFinder.setFactory is called on initialization</li>
 * <li>FactoryFinder.releaseFactories is called on shutdown</li>
 * <li>FactoryFinder.getFactory is called after initialization configuration is 
 *     done but before shutdown call to FactoryFinder.setFactory </li>
 * </ul>
 * 
 * <p>Remember all methods of FactoryFinder are static.</p> 
 * 
 * <p>One possible solution could be:</p>
 * 
 * <ol>
 * <li>Create a class called FactoryFinderProvider, that has the same three method 
 *     but in a non static version.</li>
 * <li>A singleton component is provided that holds the information of the 
 *     FactoryFinderProviderFactory. This one works per classloader, so the 
 *     singleton is implemented using an static variable. To configure it, the 
 *     static method should be called when the "classloader realm" is initialized, 
 *     before any web context is started (the WAR is deployed). Usually the EAR is 
 *     started as a single entity, so this should occur when the EAR starts, but 
 *     before the WAR files are started (or the web context are created). 
 *     The singleton will be responsible to decide which FactoryFinderProvider 
 *     should be used, based on the current thread information.</li>
 * <li>Add utility methods to retrieve the required objects and call the methods 
 *     using reflection from javax.faces.FactoryFinder</li>
 * </ol>
 * 
 * <p>This class implements the proposed solution. Note by definition, this factory 
 * cannot be configured using SPI standard algorithm (look for 
 * META-INF/services/[factory_class_name]).</p>
 * 
 * @since 2.0.5
 * @author Leonardo Uribe
 *
 */
public abstract class FactoryFinderProviderFactory
{
    private static volatile FactoryFinderProviderFactory instance = null;
    
    /**
     * Set the instance to be used by {@link javax.faces.FactoryFinder} to resolve
     * factories. 
     * 
     * <p>This method should be called before any "web context" is initialized in the
     * current "classloader context". For example, if a EAR file contains two WAR files,
     * this method should be called before initialize any WAR, since each one requires
     * a different "web context"</p>
     * 
     * @param instance
     */
    public static void setInstance(FactoryFinderProviderFactory instance)
    {
        
        
        // Now we need to make sure the volatile var FactoryFinder._initialized is
        // set to false, to make sure the right factory is fetched after this method
        // exists. It is just a fail-safe, because after all if the conditions to make 
        // this call are met, _initialized should be false.
        try
        {
            Class clazz = ClassUtils.classForName("javax.faces.FactoryFinder");
            Field field = clazz.getDeclaredField("initialized");
            field.setAccessible(true);
            
            if (field.getBoolean(null))
            {
                Logger log = Logger.getLogger(FactoryFinderProviderFactory.class.getName());
                if (log.isLoggable(Level.WARNING))
                {
                    log.log(Level.WARNING,
                            "Called FactoryFinderProviderFactory.setFactory after " +
                                    "initialized FactoryFinder (first call to getFactory() or setFactory()). " +
                                    "This method should be called before " +
                                    "any 'web context' is initialized in the current 'classloader context'. " +
                                    "By that reason it will not be changed.");
                }
            }
            else
            {
                FactoryFinderProviderFactory.instance = instance;
            }
            
            field.setBoolean(null, false);
        }
        catch (Exception e)
        {
            // No Op
            Logger log = Logger.getLogger(FactoryFinderProviderFactory.class.getName());
            if (log.isLoggable(Level.FINE))
            {
                log.log(Level.FINE, "Cannot access field _initialized"
                        + "from FactoryFinder ", e);
            }
        }
    }
    
    /**
     * Retrieve the installed instance of this class to be used by 
     * {@link javax.faces.FactoryFinder}. If no factory is set, return null
     * 
     * @return
     */
    public static FactoryFinderProviderFactory getInstance()
    {
        return instance;
    }

    /**
     * Provide the FactoryFinderProvider to be used to resolve factories.
     * Subclasses must implement this method. 
     * 
     * @return
     */
    public abstract FactoryFinderProvider getFactoryFinderProvider();
}
