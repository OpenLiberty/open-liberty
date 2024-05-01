/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.utils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class VersionlessTestCase {

    private List<String> featureInputs = new ArrayList<String>();
    private List<String> featureOutputs = new ArrayList<String>();
    private String[] platforms;


    public VersionlessTestCase(List<String> featureInputs, List<String> featureOutputs){
        this.featureInputs = featureInputs;
        this.featureOutputs = featureOutputs;
        platforms = null;
    }

    public VersionlessTestCase(List<String> featureInputs, List<String> featureOutputs, String[] platforms){
        this.featureInputs = featureInputs;
        this.featureOutputs = featureOutputs;
        this.platforms = platforms;
    }

    public List<String> getFeatureInputs(){
        return featureInputs;
    }

    public List<String> getFeatureOutputs(){
        return featureOutputs;
    }

    public String[] getPlatforms(){
        return platforms;
    }
}