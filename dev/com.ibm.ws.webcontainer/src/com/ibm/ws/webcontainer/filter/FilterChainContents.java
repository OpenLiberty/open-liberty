/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.filter;

import java.util.ArrayList;
import java.util.logging.Level; //PM93069
import java.util.logging.Logger; //PM93069
import com.ibm.wsspi.webcontainer.WCCustomProperties; //PM93069
import com.ibm.wsspi.webcontainer.logging.LoggerFactory; //PM93069

/**
 * Represents the entities that are contained in a filter chain.
 *
 */
@SuppressWarnings("unchecked")
class FilterChainContents
{
    private ArrayList _filterNames = new ArrayList();
    protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.filter");//PM93069
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.filter.FilterChainContents";//PM93069

    boolean _hasFilters = false;

    FilterChainContents()
    {
    }

    /** public interface **/

    /**
     * Gets the filter name array list for this chain
     *
     * @return the filter names for this chain
     */
    public ArrayList getFilterNames()
    {
        return _filterNames;
    }

    /**
     * Adds a filter name to the name vector for this chain
     *
     * @param the filter name to add
     */
    public void addFilter(String filterName)
    {
        _hasFilters = true;
        //PM93069 Start
        if(!WCCustomProperties.DENY_DUPLICATE_FILTER_IN_CHAIN){
            _filterNames.add(filterName); 
        }
        else { 

            if(! _filterNames.contains(filterName)){ // if unique then add in the chain
                _filterNames.add(filterName);
            }
            else{
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME,"init", "filter already in chain ->"+ filterName);

            }
        }    //PM93069 End
    }

}
