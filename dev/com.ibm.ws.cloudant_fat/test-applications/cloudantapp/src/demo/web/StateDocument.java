/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package demo.web;

/**
 * Cloudant document representing information about a US state.
 */
public class StateDocument {

    private long area;

    private String capital;

    private long population;

    private String name;

    private String _id;

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        _id = id;
    }

    public long getArea() {
        return area;
    }

    public String getCapital() {
        return capital;
    }

    public String getName() {
        return name;
    }

    public long getPopulation() {
        return population;
    }

    public void setArea(long area) {
        this.area = area;
    }

    public void setCapital(String capital) {
        this.capital = capital;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPopulation(long population) {
        this.population = population;
    }
}
