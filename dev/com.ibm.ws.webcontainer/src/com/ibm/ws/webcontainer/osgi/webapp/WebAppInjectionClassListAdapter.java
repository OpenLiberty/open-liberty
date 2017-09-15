/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.webapp;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.container.service.app.deploy.InjectionClassListProvider;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInjectionClassListCollaborator;

/**
 *
 */
@Component(service = {ContainerAdapter.class, InjectionClassListProvider.class},
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {"service.vendor=IBM",
                       "toType=com.ibm.ws.webcontainer.osgi.webapp.WebAppInjectionClassList" } )
public class WebAppInjectionClassListAdapter implements ContainerAdapter<WebAppInjectionClassList>, InjectionClassListProvider{
    
    private final ConcurrentServiceReferenceSet<WebAppInjectionClassListCollaborator> webAppInjectionClassListCollaborators = new ConcurrentServiceReferenceSet<WebAppInjectionClassListCollaborator>("webAppInjectionClassListCollaborators");
    
    public WebAppInjectionClassList adapt(Container root, OverlayContainer rootOverlay, ArtifactContainer artifactContainer, Container containerToAdapt) throws UnableToAdaptException {
        
        final List<String> listOfClassNames = getInjectionClasses(containerToAdapt);
        
        return new WebAppInjectionClassList() {
            @Override
            public List<String> getClassNames() {
                return listOfClassNames;
            }
        };
    }

    /**
     * DS method to activate this component.
     */
    @Activate
    protected void activate(ComponentContext context)
    {
        webAppInjectionClassListCollaborators.activate(context);
    }

    /**
     * DS method to deactivate this component.
     */
    @Deactivate
    protected void deactivate(ComponentContext context)
    {
        webAppInjectionClassListCollaborators.deactivate(context);
    }
    
    @Reference(name = "webAppInjectionClassListCollaborators",
               service = WebAppInjectionClassListCollaborator.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setWebAppInjectionClassListCollaborators(ServiceReference<WebAppInjectionClassListCollaborator> ref){
        webAppInjectionClassListCollaborators.addReference(ref);  
    }
    
    protected void unsetWebAppInjectionClassListCollaborators(ServiceReference<WebAppInjectionClassListCollaborator> ref){
        webAppInjectionClassListCollaborators.removeReference(ref);  
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.container.service.app.deploy.InjectionClassListProvider#getInjectionClasses(com.ibm.wsspi.adaptable.module.Container)
     */
    @Override
    public List<String> getInjectionClasses(Container moduleContainer) {
        final List<String> listOfClassNames = new ArrayList<String>();
        
        for(WebAppInjectionClassListCollaborator collab: webAppInjectionClassListCollaborators.services()){
            listOfClassNames.addAll(collab.getInjectionClasses(moduleContainer));
        }
        
        return listOfClassNames;
    }

}
