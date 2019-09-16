/*
 * Copyright (c)  2016  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.PI64718;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Named
@RequestScoped
public class IndexBeanPI64718 {

    private List<String> data1;

    public List<String> getData1() {
        return data1;
    }

    @Size(min = 2, max = 2)
    private List<String> selectedData1;

    public void setSelectedData1(List<String> s) {
        selectedData1 = s;
    }

    public List<String> getSelectedData1() {
        return selectedData1;
    }

    private String[] data2;

    public String[] getData2() {
        return data2;
    }

    @NotNull
    @Size(min = 2, max = 2)
    private String[] selectedData2;

    public String[] getSelectedData2() {
        return selectedData2;
    }

    public void setSelectedData2(String[] s) {
        selectedData2 = s;
    }

    private List<String> data3;

    public List<String> getData3() {
        return data3;
    }

    @ListSizeValidator
    private List<String> selectedData3;

    public List<String> getSelectedData3() {
        return selectedData3;
    }

    public void setSelectedData3(List<String> s) {
        selectedData3 = s;
    }

    private String[] data4;

    public String[] getdata4() {
        return data4;
    }

    @ArraySizeValidator
    private String[] selectedData4;

    public String[] getSelectedData4() {
        return selectedData4;
    }

    public void setSelectedData4(String[] s) {
        selectedData4 = s;
    }

    @PostConstruct
    public void postConstruct() {
        data1 = new ArrayList();
        data1.add("DATA1-1");
        data1.add("DATA1-2");
        data1.add("DATA1-3");

        data2 = new String[] { "DATA2-1", "DATA2-2", "DATA2-3" };

        data3 = new ArrayList();
        data3.add("DATA3-1");
        data3.add("DATA3-2");
        data3.add("DATA3-3");

        data4 = new String[] { "DATA4-1", "DATA4-2", "DATA4-3" };
    }

    public String execute() {
        return "index";
    }

}
