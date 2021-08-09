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

import org.apache.myfaces.config.element.FacesConfigData;

import javax.faces.context.ExternalContext;

/**
 * SPI that uses the FacesConfigurationProvider-SPI to get all FacesConfig data
 * and then it combines it into one FacesConfigData instance. For this merging
 * process the ordering and sorting rules of the JSF spec must be applied.
 *
 * With this SPI it is possible to store the result of the complex ordering and
 * sorting algorithm in order to skip it if no configuration changes are applied
 * upon redeploy.
 *
 * Implementations of this SPI can take advantage of the decorator pattern in
 * order to use the default SPI-impl.
 *
 * @author Jakob Korherr
 * @since 2.0.3
 */
public abstract class FacesConfigurationMerger
{

    /**
     * Returns an object that collect all config information used by MyFaces
     * to initialize the web application.
     *
     * @param ectx
     * @return
     */
    public abstract FacesConfigData getFacesConfigData(ExternalContext ectx);

}
