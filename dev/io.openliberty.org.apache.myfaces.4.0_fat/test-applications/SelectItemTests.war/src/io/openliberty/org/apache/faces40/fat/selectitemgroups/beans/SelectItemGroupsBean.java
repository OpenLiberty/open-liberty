/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.org.apache.faces40.fat.selectitemgroups.beans;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Named
@RequestScoped
public class SelectItemGroupsBean {
    
    /* Each DestinationGroup is a group of countries */
    private List<DestinationGroup> destinationGroupList = new ArrayList<>();
    private Long selectedId;

    public SelectItemGroupsBean() {
        destinationGroupList.add(new DestinationGroup("Africa", 
                                        new Country(1, "Egypt"), 
                                        new Country(2, "Kenya")
                                    ));
        destinationGroupList.add(new DestinationGroup("South America",
                                        new Country(3, "Peru"),
                                        new Country(4, "Argentina")
                                        ));
    }

    public List<DestinationGroup> getDestinationGroupList() {
        return destinationGroupList;
    }

    public void setDestinationGroupList(List<DestinationGroup> destinationGroupList) {
        this.destinationGroupList = destinationGroupList;
    }

    public Long getSelectedId() {
        return selectedId;
    }

    public void setSelectedId(Long selectedId) {
        this.selectedId = selectedId;
    }


    // Think of this as the option
    public class Country {

        private Integer id;
        private String name;
    
        public Country(Integer id, String name){
            this.id = id;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
  
    }

    /* Think of this as the opgroup */
    public static class DestinationGroup {

        private String region;
        private List<Country> countries = new ArrayList<>();
    
        public DestinationGroup(String region, Country... countries){
            this.region = region;
            this.countries = Arrays.asList(countries);
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public List<Country> getCountries() {
            return countries;
        }

        public void setCountries(List<Country> countries) {
            this.countries = countries;
        }

        
        
    }
}
