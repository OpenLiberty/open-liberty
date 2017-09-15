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
package org.apache.myfaces.config;

import org.apache.myfaces.config.element.ConfigOthersSlot;
import org.apache.myfaces.config.element.FacesConfig;
import org.apache.myfaces.config.element.FacesConfigData;
import org.apache.myfaces.config.element.FacesConfigNameSlot;
import org.apache.myfaces.config.element.OrderSlot;
import org.apache.myfaces.config.element.Ordering;
import org.apache.myfaces.config.impl.digester.DigesterFacesConfigDispenserImpl;
import org.apache.myfaces.config.util.CyclicDependencyException;
import org.apache.myfaces.config.util.DirectedAcyclicGraphVerifier;
import org.apache.myfaces.config.util.Vertex;
import org.apache.myfaces.spi.FacesConfigurationMerger;
import org.apache.myfaces.spi.FacesConfigurationProvider;
import org.apache.myfaces.spi.FacesConfigurationProviderFactory;

import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default impl of the FacesConfigurationMerger-SPI.
 *
 * This impl gets all FacesConfig data from the current FacesConfigurationProvider SPI impl and merges
 * it into one FacesConfigData object using the ordering and sorting rules of the JSF spec.
 *
 * @author Jakob Korherr
 */
public class DefaultFacesConfigurationMerger extends FacesConfigurationMerger
{

    private static final Logger log = Logger.getLogger(DefaultFacesConfigurationMerger.class.getName());

    @Override
    public FacesConfigData getFacesConfigData(ExternalContext externalContext)
    {
        // get the FacesConfigProvider SPI impl in order to get the faces-config data for the merging process
        FacesConfigurationProvider facesConfigProvider = FacesConfigurationProviderFactory
                .getFacesConfigurationProviderFactory(externalContext).getFacesConfigurationProvider(externalContext);

        FacesConfigDispenser dispenser = new DigesterFacesConfigDispenserImpl();

        // standard-faces-config.xml
        dispenser.feed(facesConfigProvider.getStandardFacesConfig(externalContext));

        // META-INF/services/[factory name] factory definitions
        dispenser.feed(facesConfigProvider.getMetaInfServicesFacesConfig(externalContext));

        // WEB-INF/faces-config.xml
        FacesConfig webAppFacesConfig = facesConfigProvider.getWebAppFacesConfig(externalContext);

        //read metadata-complete attribute on WEB-INF/faces-config.xml
        boolean metadataComplete = false;
        if(webAppFacesConfig != null)
        {
            metadataComplete = Boolean.valueOf(webAppFacesConfig.getMetadataComplete());
        }
        else
        {
            //assume false if no faces-config.xml was found
            //metadata-complete can only be specified in faces-config.xml per the JSF 2.0 schema
            metadataComplete = false;
        }

        // faces-config data from Annotations
        FacesConfig annotationFacesConfig = facesConfigProvider
                .getAnnotationsFacesConfig(externalContext, metadataComplete);
        if (annotationFacesConfig != null)
        {
            dispenser.feed(annotationFacesConfig);
        }

        List<FacesConfig> appConfigResources = new ArrayList<FacesConfig>();

        // META-INF/faces-config.xml files
        appConfigResources.addAll(facesConfigProvider.getClassloaderFacesConfig(externalContext));
        
        // faces-config.xml files from javax.faces.CONFIG_FILES
        appConfigResources.addAll(facesConfigProvider.getContextSpecifiedFacesConfig(externalContext));
        
        // JSF 2.2 ApplicationConfigurationResourceDocumentPopulator FacesConfig
        appConfigResources.addAll(facesConfigProvider.
            getApplicationConfigurationResourceDocumentPopulatorFacesConfig(externalContext));

        // JSF 2.2 Faces Flow
        appConfigResources.addAll(facesConfigProvider.getFacesFlowFacesConfig(externalContext));
        
        // apply the ordering and sorting algorithm 
        orderAndFeedArtifacts(dispenser, appConfigResources, webAppFacesConfig);
        
        List<FacesConfig> faceletTagLibFacesConfig = 
            facesConfigProvider.getFaceletTaglibFacesConfig(externalContext);
        
        // at last feed facelet taglib faces config. These ones does not need
        // to be included in the ordering algorithm.
        if (faceletTagLibFacesConfig != null && !faceletTagLibFacesConfig.isEmpty())
        {
            for (FacesConfig fc : faceletTagLibFacesConfig)
            {
                dispenser.feed(fc);
            }
        }

        LogMetaInfUtils.logMetaInf();

        return dispenser;
    }

    protected void orderAndFeedArtifacts(FacesConfigDispenser dispenser,
                                         List<FacesConfig> appConfigResources,
                                         FacesConfig webAppConfig)
        throws FacesException
    {
        if (webAppConfig != null && webAppConfig.getAbsoluteOrdering() != null)
        {
            if (webAppConfig.getOrdering() != null)
            {
                if (log.isLoggable(Level.WARNING))
                {
                    log.warning("<ordering> element found in application faces config. " +
                            "This description will be ignored and the actions described " +
                            "in <absolute-ordering> element will be taken into account instead.");
                }
            }
            //Absolute ordering

            //1. Scan all appConfigResources and create a list
            //containing all resources not mentioned directly, preserving the
            //order founded
            List<FacesConfig> othersResources = new ArrayList<FacesConfig>();
            List<OrderSlot> slots = webAppConfig.getAbsoluteOrdering().getOrderList();
            for (FacesConfig resource : appConfigResources)
            {
                // First condition: if faces-config.xml does not have name it is
                // 1) pre-JSF-2.0 or
                // 2) has no <name> element,
                // -> in both cases cannot be ordered
                // Second condition : faces-config.xml has a name but <ordering>
                // element does not have slot with that name
                //  -> resource can be ordered, but will fit into <others /> element
                if ((resource.getName() == null) ||
                        (resource.getName() != null && !containsResourceInSlot(slots, resource.getName())))
                {
                    othersResources.add(resource);
                }
            }

            //2. Scan slot by slot and merge information according
            for (OrderSlot slot : webAppConfig.getAbsoluteOrdering().getOrderList())
            {
                if (slot instanceof ConfigOthersSlot)
                {
                    //Add all mentioned in othersResources
                    for (FacesConfig resource : othersResources)
                    {
                        dispenser.feed(resource);
                    }
                }
                else
                {
                    //Add it to the sorted list
                    FacesConfigNameSlot nameSlot = (FacesConfigNameSlot) slot;
                    // We need to check if the resource is on appConfigResources, otherwise we can
                    // ignore it safely.
                    FacesConfig targetFacesConfig = getFacesConfig(appConfigResources, nameSlot.getName());
                    if (targetFacesConfig != null)
                    {
                        dispenser.feed(targetFacesConfig);
                    }
                }
            }
        }
        else if (!appConfigResources.isEmpty())
        {
            //Relative ordering
            for (FacesConfig resource : appConfigResources)
            {
                if (resource.getAbsoluteOrdering() != null)
                {
                    if (log.isLoggable(Level.WARNING))
                    {
                        log.warning("<absolute-ordering> element found in application " +
                                "configuration resource "+resource.getName()+". " +
                                "This description will be ignored and the actions described " +
                                "in <ordering> elements will be taken into account instead.");
                    }
                }
            }

            List<FacesConfig> postOrderedList = getPostOrderedList(appConfigResources);

            List<FacesConfig> sortedList = sortRelativeOrderingList(postOrderedList);

            if (sortedList == null)
            {
                //The previous algorithm can't sort correctly, try this one
                sortedList = applySortingAlgorithm(appConfigResources);
            }

            for (FacesConfig resource : sortedList)
            {
                //Feed
                dispenser.feed(resource);
            }
        }

        //add null check for apps which don't have a faces-config.xml (e.g. tomahawk examples for 1.1/1.2)
        if(webAppConfig != null)
        {
            dispenser.feed(webAppConfig);
        }
    }

    /**
     * Sort using topological ordering algorithm.
     *
     * @param appConfigResources
     * @return
     * @throws FacesException
     */
    protected List<FacesConfig> applySortingAlgorithm(List<FacesConfig> appConfigResources) throws FacesException
    {

        //0. Convert the references into a graph
        List<Vertex<FacesConfig>> vertexList = new ArrayList<Vertex<FacesConfig>>();
        for (FacesConfig config : appConfigResources)
        {
            Vertex<FacesConfig> v = null;
            if (config.getName() != null)
            {
                v = new Vertex<FacesConfig>(config.getName(), config);
            }
            else
            {
                v = new Vertex<FacesConfig>(config);
            }
            vertexList.add(v);
        }

        //1. Resolve dependencies (before-after rules) and mark referenced vertex
        boolean[] referencedVertex = new boolean[vertexList.size()];

        for (int i = 0; i < vertexList.size(); i++)
        {
            Vertex<FacesConfig> v = vertexList.get(i);
            FacesConfig f = (FacesConfig) v.getNode();

            if (f.getOrdering() != null)
            {
                for (OrderSlot slot : f.getOrdering().getBeforeList())
                {
                    if (slot instanceof FacesConfigNameSlot)
                    {
                        String name = ((FacesConfigNameSlot) slot).getName();
                        int j = DirectedAcyclicGraphVerifier.findVertex(vertexList, name);
                        Vertex<FacesConfig> v1 = vertexList.get(j);
                        if (v1 != null)
                        {
                            referencedVertex[i] = true;
                            referencedVertex[j] = true;
                            v1.addDependency(v);
                        }
                    }
                }
                for (OrderSlot slot : f.getOrdering().getAfterList())
                {
                    if (slot instanceof FacesConfigNameSlot)
                    {
                        String name = ((FacesConfigNameSlot) slot).getName();
                        int j = DirectedAcyclicGraphVerifier.findVertex(vertexList, name);
                        Vertex<FacesConfig> v1 = vertexList.get(j);
                        if (v1 != null)
                        {
                            referencedVertex[i] = true;
                            referencedVertex[j] = true;
                            v.addDependency(v1);
                        }
                    }
                }
            }
        }

        //2. Classify into categories
        List<Vertex<FacesConfig>> beforeAfterOthersList = new ArrayList<Vertex<FacesConfig>>();
        List<Vertex<FacesConfig>> othersList = new ArrayList<Vertex<FacesConfig>>();
        List<Vertex<FacesConfig>> referencedList = new ArrayList<Vertex<FacesConfig>>();

        for (int i = 0; i < vertexList.size(); i++)
        {
            if (!referencedVertex[i])
            {
                Vertex<FacesConfig> v = vertexList.get(i);
                FacesConfig f = (FacesConfig) v.getNode();
                boolean added = false;
                if (f.getOrdering() != null)
                {
                    if (!f.getOrdering().getBeforeList().isEmpty())
                    {
                        added = true;
                        beforeAfterOthersList.add(v);
                    }
                    else if (!f.getOrdering().getAfterList().isEmpty())
                    {
                        added = true;
                        beforeAfterOthersList.add(v);
                    }
                }
                if (!added)
                {
                    othersList.add(v);
                }
            }
            else
            {
                referencedList.add(vertexList.get(i));
            }
        }

        //3. Sort all referenced nodes
        try
        {
            DirectedAcyclicGraphVerifier.topologicalSort(referencedList);
        }
        catch (CyclicDependencyException e)
        {
            e.printStackTrace();
        }

        //4. Add referenced nodes
        List<FacesConfig> sortedList = new ArrayList<FacesConfig>();
        for (Vertex<FacesConfig> v : referencedList)
        {
            sortedList.add((FacesConfig)v.getNode());
        }

        //5. add nodes without instructions at the end
        for (Vertex<FacesConfig> v : othersList)
        {
            sortedList.add((FacesConfig)v.getNode());
        }

        //6. add before/after nodes
        for (Vertex<FacesConfig> v : beforeAfterOthersList)
        {
            FacesConfig f = (FacesConfig) v.getNode();
            boolean added = false;
            if (f.getOrdering() != null && !f.getOrdering().getBeforeList().isEmpty())
            {
                added = true;
                sortedList.add(0,f);
            }
            if (!added)
            {
                sortedList.add(f);
            }
        }

        //Check
        for (int i = 0; i < sortedList.size(); i++)
        {
            FacesConfig resource = sortedList.get(i);

            if (resource.getOrdering() != null)
            {
                for (OrderSlot slot : resource.getOrdering().getBeforeList())
                {
                    if (slot instanceof FacesConfigNameSlot)
                    {
                        String name = ((FacesConfigNameSlot) slot).getName();
                        if (name != null && !"".equals(name))
                        {
                            boolean founded = false;
                            for (int j = i-1; j >= 0; j--)
                            {
                                if (name.equals(sortedList.get(j).getName()))
                                {
                                    founded=true;
                                    break;
                                }
                            }
                            if (founded)
                            {
                                log.severe("Circular references detected when sorting " +
                                          "application config resources. Use absolute ordering instead.");
                                throw new FacesException("Circular references detected when sorting " +
                                        "application config resources. Use absolute ordering instead.");
                            }
                        }
                    }
                }
                for (OrderSlot slot : resource.getOrdering().getAfterList())
                {
                    if (slot instanceof FacesConfigNameSlot)
                    {
                        String name = ((FacesConfigNameSlot) slot).getName();
                        if (name != null && !"".equals(name))
                        {
                            boolean founded = false;
                            for (int j = i+1; j < sortedList.size(); j++)
                            {
                                if (name.equals(sortedList.get(j).getName()))
                                {
                                    founded=true;
                                    break;
                                }
                            }
                            if (founded)
                            {
                                log.severe("Circular references detected when sorting " +
                                    "application config resources. Use absolute ordering instead.");
                                throw new FacesException("Circular references detected when sorting " +
                                    "application config resources. Use absolute ordering instead.");
                            }
                        }
                    }
                }
            }
        }

        return sortedList;
    }

    /**
     * Sort a list of pre ordered elements. It scans one by one the elements
     * and apply the conditions mentioned by Ordering object if it is available.
     *
     * The preOrderedList ensures that application config resources referenced by
     * other resources are processed first, making more easier the sort procedure.
     *
     * @param preOrderedList
     * @return
     */
    protected List<FacesConfig> sortRelativeOrderingList(List<FacesConfig> preOrderedList)
    {
        List<FacesConfig> sortedList = new ArrayList<FacesConfig>();

        for (int i=0; i < preOrderedList.size(); i++)
        {
            FacesConfig resource = preOrderedList.get(i);
            if (resource.getOrdering() != null)
            {
                if (resource.getOrdering().getBeforeList().isEmpty() &&
                    resource.getOrdering().getAfterList().isEmpty())
                {
                    //No order rules, just put it as is
                    sortedList.add(resource);
                }
                else if (resource.getOrdering().getBeforeList().isEmpty())
                {
                    //Only after rules
                    applyAfterRule(sortedList, resource);
                }
                else if (resource.getOrdering().getAfterList().isEmpty())
                {
                    //Only before rules

                    //Resolve if there is a later reference to this node before
                    //apply it
                    boolean referenceNode = false;

                    for (int j = i+1; j < preOrderedList.size(); j++)
                    {
                        FacesConfig pointingResource = preOrderedList.get(j);
                        for (OrderSlot slot : pointingResource.getOrdering().getBeforeList())
                        {
                            if (slot instanceof FacesConfigNameSlot &&
                                    resource.getName().equals(((FacesConfigNameSlot)slot).getName()) )
                            {
                                referenceNode = true;
                            }
                            if (slot instanceof ConfigOthersSlot)
                            {
                                //No matter if there is a reference, because this rule
                                //is not strict and before other ordering is unpredictable.
                                //
                                referenceNode = false;
                                break;
                            }
                        }
                        if (referenceNode)
                        {
                            break;
                        }
                        for (OrderSlot slot : pointingResource.getOrdering().getAfterList())
                        {
                            if (slot instanceof FacesConfigNameSlot &&
                                resource.getName().equals(((FacesConfigNameSlot)slot).getName()) )
                            {
                                referenceNode = true;
                                break;
                            }
                        }
                    }

                    applyBeforeRule(sortedList, resource, referenceNode);
                }
                else
                {
                    //Both before and after rules
                    //In this case we should compare before and after rules
                    //and the one with names takes precedence over the other one.
                    //It both have names references, before rules takes
                    //precedence over after
                    //after some action is applied a check of the condition is made.
                    int beforeWeight = 0;
                    int afterWeight = 0;
                    for (OrderSlot slot : resource.getOrdering()
                            .getBeforeList())
                    {
                        if (slot instanceof FacesConfigNameSlot)
                        {
                            beforeWeight++;
                        }
                    }
                    for (OrderSlot slot : resource.getOrdering()
                            .getAfterList())
                    {
                        if (slot instanceof FacesConfigNameSlot)
                        {
                            afterWeight++;
                        }
                    }

                    if (beforeWeight >= afterWeight)
                    {
                        applyBeforeRule(sortedList, resource,false);
                    }
                    else
                    {
                        applyAfterRule(sortedList, resource);
                    }


                }
            }
            else
            {
                //No order rules, just put it as is
                sortedList.add(resource);
            }
        }

        //Check
        for (int i = 0; i < sortedList.size(); i++)
        {
            FacesConfig resource = sortedList.get(i);

            if (resource.getOrdering() != null)
            {
                for (OrderSlot slot : resource.getOrdering().getBeforeList())
                {
                    if (slot instanceof FacesConfigNameSlot)
                    {
                        String name = ((FacesConfigNameSlot) slot).getName();
                        if (name != null && !"".equals(name))
                        {
                            boolean founded = false;
                            for (int j = i-1; j >= 0; j--)
                            {
                                if (name.equals(sortedList.get(j).getName()))
                                {
                                    founded=true;
                                    break;
                                }
                            }
                            if (founded)
                            {
                                //Cyclic reference
                                return null;
                            }
                        }
                    }
                }
                for (OrderSlot slot : resource.getOrdering().getAfterList())
                {
                    if (slot instanceof FacesConfigNameSlot)
                    {
                        String name = ((FacesConfigNameSlot) slot).getName();
                        if (name != null && !"".equals(name))
                        {
                            boolean founded = false;
                            for (int j = i+1; j < sortedList.size(); j++)
                            {
                                if (name.equals(sortedList.get(j).getName()))
                                {
                                    founded=true;
                                    break;
                                }
                            }
                            if (founded)
                            {
                                //Cyclic reference
                                return null;
                            }
                        }
                    }
                }
            }
        }

        return sortedList;
    }

    private void applyBeforeRule(List<FacesConfig> sortedList, FacesConfig resource, boolean referenced)
            throws FacesException
    {
        //Only before rules
        boolean configOthers = false;
        List<String> names = new ArrayList<String>();

        for (OrderSlot slot : resource.getOrdering().getBeforeList())
        {
            if (slot instanceof ConfigOthersSlot)
            {
                configOthers = true;
                break;
            }
            else
            {
                FacesConfigNameSlot nameSlot = (FacesConfigNameSlot) slot;
                names.add(nameSlot.getName());
            }
        }

        if (configOthers)
        {
            //<before>....<others/></before> case
            //other reference where already considered when
            //pre ordered list was calculated, so just add to the end.

            //There is one very special case, and it is when there
            //is another resource with a reference on it. In this case,
            //it is better do not apply this rule and add it to the end
            //to give the chance to the other one to be applied.
            if (resource.getOrdering().getBeforeList().size() > 1)
            {
                //If there is a reference apply it
                sortedList.add(0,resource);
            }
            else if (!referenced)
            {
                //If it is not referenced apply it
                sortedList.add(0,resource);
            }
            else
            {
                //if it is referenced bypass the rule and add it to the end
                sortedList.add(resource);
            }
        }
        else
        {
            //Scan the nearest reference and add it after
            boolean founded = false;
            for (int i = 0; i < sortedList.size() ; i++)
            {
                if (names.contains(sortedList.get(i).getName()))
                {
                    sortedList.add(i,resource);
                    founded = true;
                    break;
                }
            }
            if (!founded)
            {
                //just add it to the end
                sortedList.add(resource);
            }
        }
    }

    private void applyAfterRule(List<FacesConfig> sortedList, FacesConfig resource) throws FacesException
    {
        boolean configOthers = false;
        List<String> names = new ArrayList<String>();

        for (OrderSlot slot : resource.getOrdering().getAfterList())
        {
            if (slot instanceof ConfigOthersSlot)
            {
                configOthers = true;
                break;
            }
            else
            {
                FacesConfigNameSlot nameSlot = (FacesConfigNameSlot) slot;
                names.add(nameSlot.getName());
            }
        }

        if (configOthers)
        {
            //<after>....<others/></after> case
            //other reference where already considered when
            //pre ordered list was calculated, so just add to the end.
            sortedList.add(resource);
        }
        else
        {
            //Scan the nearest reference and add it after
            boolean founded = false;
            for (int i = sortedList.size()-1 ; i >=0 ; i--)
            {
                if (names.contains(sortedList.get(i).getName()))
                {
                    if (i+1 < sortedList.size())
                    {
                        sortedList.add(i+1,resource);
                    }
                    else
                    {
                        sortedList.add(resource);
                    }
                    founded = true;
                    break;
                }
            }
            if (!founded)
            {
                //just add it to the end
                sortedList.add(resource);
            }
        }
    }


    /**
     * Pre Sort the appConfigResources, detecting cyclic references, so when sort process
     * start, it is just necessary to traverse the preOrderedList once. To do that, we just
     * scan "before" and "after" lists for references, and then those references are traversed
     * again, so the first elements of the pre ordered list does not have references and
     * the next elements has references to the already added ones.
     *
     * The elements on the preOrderedList looks like this:
     *
     * [ no ordering elements , referenced elements ... more referenced elements,
     *  before others / after others non referenced elements]
     *
     * @param appConfigResources
     * @return
     */
    protected List<FacesConfig> getPostOrderedList(final List<FacesConfig> appConfigResources) throws FacesException
    {

        //0. Clean up: remove all not found resource references from the ordering
        //descriptions.
        List<String> availableReferences = new ArrayList<String>();
        for (FacesConfig resource : appConfigResources)
        {
            String name = resource.getName();
            if (name != null && !"".equals(name))
            {
                availableReferences.add(name);
            }
        }

        for (FacesConfig resource : appConfigResources)
        {
            Ordering ordering = resource.getOrdering();
            if (ordering != null)
            {
                for (Iterator<OrderSlot> it =  resource.getOrdering().getBeforeList().iterator();it.hasNext();)
                {
                    OrderSlot slot = it.next();
                    if (slot instanceof FacesConfigNameSlot)
                    {
                        String name = ((FacesConfigNameSlot) slot).getName();
                        if (!availableReferences.contains(name))
                        {
                            it.remove();
                        }
                    }
                }
                for (Iterator<OrderSlot> it =  resource.getOrdering().getAfterList().iterator();it.hasNext();)
                {
                    OrderSlot slot = it.next();
                    if (slot instanceof FacesConfigNameSlot)
                    {
                        String name = ((FacesConfigNameSlot) slot).getName();
                        if (!availableReferences.contains(name))
                        {
                            it.remove();
                        }
                    }
                }
            }
        }

        List<FacesConfig> appFilteredConfigResources = null;

        //1. Pre filtering: Sort nodes according to its weight. The weight is the number of named
        //nodes containing in both before and after lists. The sort is done from the more complex
        //to the most simple
        if (appConfigResources instanceof ArrayList)
        {
            appFilteredConfigResources = (List<FacesConfig>)
                ((ArrayList<FacesConfig>)appConfigResources).clone();
        }
        else
        {
            appFilteredConfigResources = new ArrayList<FacesConfig>();
            appFilteredConfigResources.addAll(appConfigResources);
        }
        Collections.sort(appFilteredConfigResources,
                new Comparator<FacesConfig>()
                {
                    public int compare(FacesConfig o1, FacesConfig o2)
                    {
                        int o1Weight = 0;
                        int o2Weight = 0;
                        if (o1.getOrdering() != null)
                        {
                            for (OrderSlot slot : o1.getOrdering()
                                    .getBeforeList())
                            {
                                if (slot instanceof FacesConfigNameSlot)
                                {
                                    o1Weight++;
                                }
                            }
                            for (OrderSlot slot : o1.getOrdering()
                                    .getAfterList())
                            {
                                if (slot instanceof FacesConfigNameSlot)
                                {
                                    o1Weight++;
                                }
                            }
                        }
                        if (o2.getOrdering() != null)
                        {
                            for (OrderSlot slot : o2.getOrdering()
                                    .getBeforeList())
                            {
                                if (slot instanceof FacesConfigNameSlot)
                                {
                                    o2Weight++;
                                }
                            }
                            for (OrderSlot slot : o2.getOrdering()
                                    .getAfterList())
                            {
                                if (slot instanceof FacesConfigNameSlot)
                                {
                                    o2Weight++;
                                }
                            }
                        }
                        return o2Weight - o1Weight;
                    }
                });

        List<FacesConfig> postOrderedList = new LinkedList<FacesConfig>();
        List<FacesConfig> othersList = new ArrayList<FacesConfig>();

        List<String> nameBeforeStack = new ArrayList<String>();
        List<String> nameAfterStack = new ArrayList<String>();

        boolean[] visitedSlots = new boolean[appFilteredConfigResources.size()];

        //2. Scan and resolve conflicts
        for (int i = 0; i < appFilteredConfigResources.size(); i++)
        {
            if (!visitedSlots[i])
            {
                resolveConflicts(appFilteredConfigResources, i, visitedSlots,
                        nameBeforeStack, nameAfterStack, postOrderedList, othersList, false);
            }
        }

        //Add othersList to postOrderedList so <before><others/></before> and <after><others/></after>
        //ordering conditions are handled at last if there are not referenced by anyone
        postOrderedList.addAll(othersList);

        return postOrderedList;
    }

    private void resolveConflicts(final List<FacesConfig> appConfigResources, int index, boolean[] visitedSlots,
            List<String> nameBeforeStack, List<String> nameAfterStack, List<FacesConfig> postOrderedList,
            List<FacesConfig> othersList, boolean indexReferenced) throws FacesException
    {
        FacesConfig facesConfig = appConfigResources.get(index);

        if (nameBeforeStack.contains(facesConfig.getName()))
        {
            //Already referenced, just return. Later if there exists a
            //circular reference, it will be detected and solved.
            return;
        }

        if (nameAfterStack.contains(facesConfig.getName()))
        {
            //Already referenced, just return. Later if there exists a
            //circular reference, it will be detected and solved.
            return;
        }

        if (facesConfig.getOrdering() != null)
        {
            boolean pointingResource = false;

            //Deal with before restrictions first
            for (OrderSlot slot : facesConfig.getOrdering().getBeforeList())
            {
                if (slot instanceof FacesConfigNameSlot)
                {
                    FacesConfigNameSlot nameSlot = (FacesConfigNameSlot) slot;
                    //The resource pointed is not added yet?
                    boolean alreadyAdded = false;
                    for (FacesConfig res : postOrderedList)
                    {
                        if (nameSlot.getName().equals(res.getName()))
                        {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded)
                    {
                        int indexSlot = -1;
                        //Find it
                        for (int i = 0; i < appConfigResources.size(); i++)
                        {
                            FacesConfig resource = appConfigResources.get(i);
                            if (resource.getName() != null && nameSlot.getName().equals(resource.getName()))
                            {
                                indexSlot = i;
                                break;
                            }
                        }

                        //Resource founded on appConfigResources
                        if (indexSlot != -1)
                        {
                            pointingResource = true;
                            //Add to nameStac
                            nameBeforeStack.add(facesConfig.getName());

                            resolveConflicts(appConfigResources, indexSlot, visitedSlots,
                                    nameBeforeStack, nameAfterStack, postOrderedList,
                                    othersList,true);

                            nameBeforeStack.remove(facesConfig.getName());
                        }
                    }
                    else
                    {
                        pointingResource = true;
                    }
                }
            }

            for (OrderSlot slot : facesConfig.getOrdering().getAfterList())
            {
                if (slot instanceof FacesConfigNameSlot)
                {
                    FacesConfigNameSlot nameSlot = (FacesConfigNameSlot) slot;
                    //The resource pointed is not added yet?
                    boolean alreadyAdded = false;
                    for (FacesConfig res : postOrderedList)
                    {
                        if (nameSlot.getName().equals(res.getName()))
                        {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded)
                    {
                        int indexSlot = -1;
                        //Find it
                        for (int i = 0; i < appConfigResources.size(); i++)
                        {
                            FacesConfig resource = appConfigResources.get(i);
                            if (resource.getName() != null && nameSlot.getName().equals(resource.getName()))
                            {
                                indexSlot = i;
                                break;
                            }
                        }

                        //Resource founded on appConfigResources
                        if (indexSlot != -1)
                        {
                            pointingResource = true;
                            //Add to nameStac
                            nameAfterStack.add(facesConfig.getName());

                            resolveConflicts(appConfigResources, indexSlot, visitedSlots,
                                    nameBeforeStack, nameAfterStack, postOrderedList,
                                    othersList,true);

                            nameAfterStack.remove(facesConfig.getName());
                        }
                    }
                    else
                    {
                        pointingResource = true;
                    }
                }
            }

            if (facesConfig.getOrdering().getBeforeList().isEmpty() &&
                facesConfig.getOrdering().getAfterList().isEmpty())
            {
                //Fits in the category "others", put at beginning
                postOrderedList.add(0,appConfigResources.get(index));
            }
            else if (pointingResource || indexReferenced)
            {
                //If the node points to other or is referenced from other,
                //add to the postOrderedList at the end
                postOrderedList.add(appConfigResources.get(index));
            }
            else
            {
                //Add to othersList
                othersList.add(appConfigResources.get(index));
            }
        }
        else
        {
            //Add at start of the list, since does not have any ordering
            //instructions and on the next step makes than "before others" and "after others"
            //works correctly
            postOrderedList.add(0,appConfigResources.get(index));
        }
        //Set the node as visited
        visitedSlots[index] = true;
    }

    private FacesConfig getFacesConfig(List<FacesConfig> appConfigResources, String name)
    {
        for (FacesConfig cfg: appConfigResources)
        {
            if (cfg.getName() != null && name.equals(cfg.getName()))
            {
                return cfg;
            }
        }
        return null;
    }

    private boolean containsResourceInSlot(List<OrderSlot> slots, String name)
    {
        for (OrderSlot slot: slots)
        {
            if (slot instanceof FacesConfigNameSlot)
            {
                FacesConfigNameSlot nameSlot = (FacesConfigNameSlot) slot;
                if (name.equals(nameSlot.getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

}
