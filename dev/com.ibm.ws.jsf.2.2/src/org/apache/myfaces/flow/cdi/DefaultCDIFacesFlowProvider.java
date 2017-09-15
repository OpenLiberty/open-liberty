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
// PI46218 hwibell     Unambiguous bean name exception if the same bean name is used in multiple WARs
package org.apache.myfaces.flow.cdi;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.context.FacesContext;
import javax.faces.flow.Flow;
import org.apache.myfaces.cdi.util.CDIUtils;
import org.apache.myfaces.spi.FacesFlowProvider;

/**
 *
 * @author Leonardo Uribe
 */
public class DefaultCDIFacesFlowProvider extends FacesFlowProvider
{
    private BeanManager _beanManager;
    private boolean _initialized;
    
    private final static String CURRENT_FLOW_SCOPE_MAP = "oam.flow.SCOPE_MAP";
    
    static final char SEPARATOR_CHAR = '.';
    
    @Override
    public Iterator<Flow> getAnnotatedFlows(FacesContext facesContext)
    {
        BeanManager beanManager = getBeanManager(facesContext);
        if (beanManager != null)
        {
            FlowBuilderFactoryBean bean = CDIUtils.lookup(
                beanManager, FlowBuilderFactoryBean.class);

            // PI46218 start
            List<Flow> flows = bean.getFlowDefinitions();
            
            return flows.iterator();
            // PI46218 end
        }
        else
        {
            Logger.getLogger(DefaultCDIFacesFlowProvider.class.getName()).log(Level.INFO,
                "CDI BeanManager not found");
        }
        return null;
    }
    
    @Override
    public void doAfterEnterFlow(FacesContext context, Flow flow)
    {
        BeanManager beanManager = getBeanManager(context);
        if (beanManager != null)
        {
            FlowScopeBeanHolder beanHolder = CDIUtils.lookup(beanManager, FlowScopeBeanHolder.class);
            beanHolder.createCurrentFlowScope(context);
        }
        String mapKey = CURRENT_FLOW_SCOPE_MAP+SEPARATOR_CHAR+
                flow.getDefiningDocumentId()+SEPARATOR_CHAR+flow.getId();
        context.getAttributes().remove(mapKey);
    }
    
    @Override
    public void doBeforeExitFlow(FacesContext context, Flow flow)
    {
        BeanManager beanManager = getBeanManager(context);
        if (beanManager != null)
        {
            FlowScopeBeanHolder beanHolder = CDIUtils.lookup(beanManager, FlowScopeBeanHolder.class);
            beanHolder.destroyCurrentFlowScope(context);
        }
        String mapKey = CURRENT_FLOW_SCOPE_MAP+SEPARATOR_CHAR+
                flow.getDefiningDocumentId()+SEPARATOR_CHAR+flow.getId();
        context.getAttributes().remove(mapKey);
    }
    
    public Map<Object, Object> getCurrentFlowScope(FacesContext facesContext)
    {
        Flow flow = facesContext.getApplication().getFlowHandler().getCurrentFlow(facesContext);
        if (flow != null)
        {
            String mapKey = CURRENT_FLOW_SCOPE_MAP+SEPARATOR_CHAR+
                flow.getDefiningDocumentId()+SEPARATOR_CHAR+flow.getId();
            Map<Object, Object> map = (Map<Object, Object>) facesContext.getAttributes().get(
                mapKey);
            if (map == null)
            {
                map = new FlowScopeMap(getBeanManager(), flow.getClientWindowFlowId(
                    facesContext.getExternalContext().getClientWindow()));
                
                facesContext.getAttributes().put(mapKey, map);
            }
            return map;
        }
        return null;
    }

    @Override
    public void refreshClientWindow(FacesContext facesContext)
    {
        if (!facesContext.getApplication().getStateManager().isSavingStateInClient(facesContext))
        {
            Flow flow = facesContext.getApplication().getFlowHandler().getCurrentFlow(facesContext);
            if (flow != null)
            {
                BeanManager beanManager = getBeanManager(facesContext);
                if (beanManager != null)
                {
                    FlowScopeBeanHolder beanHolder = CDIUtils.lookup(beanManager, FlowScopeBeanHolder.class);

                    //Refresh client window for flow scope
                    beanHolder.refreshClientWindow(facesContext);
                }
            }
        }
    }
    
    public BeanManager getBeanManager()
    {
        if (_beanManager == null && !_initialized)
        {
            _beanManager = CDIUtils.getBeanManager(
                FacesContext.getCurrentInstance().getExternalContext());
            _initialized = true;
        }
        return _beanManager;
    }
    
    public BeanManager getBeanManager(FacesContext facesContext)
    {
        if (_beanManager == null && !_initialized)
        {
            _beanManager = CDIUtils.getBeanManager(
                facesContext.getExternalContext());
            _initialized = true;
        }
        return _beanManager;
    }

}
