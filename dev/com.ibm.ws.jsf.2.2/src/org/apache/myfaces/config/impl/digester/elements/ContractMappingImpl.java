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
package org.apache.myfaces.config.impl.digester.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.myfaces.config.element.ContractMapping;

/**
 *
 * @author lu4242
 */
public class ContractMappingImpl extends ContractMapping
{
    private List<String> urlPatternList;
    private List<String> contractsList;

    private transient List <String> unmodifiableUrlPatternList;
    private transient List <String> unmodifiableContractsList;

    /**
     * @return the urlPattern
     */
    public List<String> getUrlPatternList()
    {
        if (urlPatternList == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableUrlPatternList == null)
        {
            unmodifiableUrlPatternList = 
                            Collections.unmodifiableList(urlPatternList);
        }
        return unmodifiableUrlPatternList;
    }

    /**
     * @return the contracts
     */
    public List<String> getContractList()
    {
        if (contractsList == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableContractsList == null)
        {
            unmodifiableContractsList = 
                            Collections.unmodifiableList(contractsList);
        }
        return unmodifiableContractsList;
    }

    public void addContract(String contract)
    {
        if (contractsList == null)
        {
            contractsList = new ArrayList<String>();
        }
        contractsList.add(contract);
    }

    public void addUrlPattern(String urlPattern)
    {
        if (urlPatternList == null)
        {
            urlPatternList = new ArrayList<String>();
        }
        urlPatternList.add(urlPattern);
    }

    @Override
    public String getUrlPattern()
    {
        return null;
    }

    @Override
    public String getContracts()
    {
        return null;
    }

}
