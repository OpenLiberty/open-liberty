/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.uirepeat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * A simple request scoped bean for use with the <ui:repeat> constraint test.
 */
@Named
@RequestScoped
public class IterationBean implements Serializable {

    /**  */
    private static final long serialVersionUID = 1L;

    private int begin = 0;
    private int step = 1;
    private int end = 9;
    private List<Integer> testList = new ArrayList<Integer>(Arrays.asList(new Integer(0),
                                                                          new Integer(1),
                                                                          new Integer(2),
                                                                          new Integer(3),
                                                                          new Integer(4),
                                                                          new Integer(5),
                                                                          new Integer(6),
                                                                          new Integer(7),
                                                                          new Integer(8),
                                                                          new Integer(9)));

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public List<Integer> getTestList() {
        return testList;
    }

    public void setTestList(List<Integer> testList) {
        this.testList = testList;
    }
}
