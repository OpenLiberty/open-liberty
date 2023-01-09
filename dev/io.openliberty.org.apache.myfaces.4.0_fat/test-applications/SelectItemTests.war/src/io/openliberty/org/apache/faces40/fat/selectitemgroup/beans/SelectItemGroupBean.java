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
package io.openliberty.org.apache.faces40.fat.selectitemgroup.beans;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Named
@RequestScoped
public class SelectItemGroupBean {

    //List is the "opgroup" and each destination is the option
    private List<Destination> destinationList;

    private Long selectedId;

    public SelectItemGroupBean() {
        this.destinationList = new ArrayList<Destination>();
        destinationList.add(new Destination(1, "Germany"));
        destinationList.add(new Destination(1, "France"));
    }

    public List<Destination> getDestinationList() {
        return destinationList;
    }
    public void setDestinationList(List<Destination> destinationList) {
        this.destinationList = destinationList;
    }
    public Long getSelectedId() {
        return selectedId;
    }
    public void setSelectedId(Long selectedId) {
        this.selectedId = selectedId;
    }

    public static class Destination {

        private Integer id;
        private String country;
    
        public Destination(Integer id, String country){
            this.id = id;
            this.country = country;
        }
    
        public Integer getId() {
            return id;
        }
    
        public void setId(Integer id) {
            this.id = id;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
        
    }
}
