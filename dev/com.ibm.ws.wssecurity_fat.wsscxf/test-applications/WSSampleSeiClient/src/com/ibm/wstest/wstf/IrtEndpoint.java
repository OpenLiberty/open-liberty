/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wstest.wstf;

public class IrtEndpoint {
    private String featureName;
    private String scenarioName;
    private String actorName;
    private String address;
    private String wsdlUri;

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWsdlUri() {
        return wsdlUri;
    }

    public void setWsdlUri(String wsdlUri) {
        this.wsdlUri = wsdlUri;
    }

    /**
     * Constructor
     * 
     * @param featureName
     * @param scenarioName
     * @param actorName
     * @param address
     * @param wsdlUri
     */
    public IrtEndpoint(String featureName, String scenarioName,
                       String actorName, String address, String wsdlUri) {
        super();
        this.featureName = featureName;
        this.scenarioName = scenarioName;
        this.actorName = actorName;
        this.address = address;
        this.wsdlUri = wsdlUri;
    }

}
