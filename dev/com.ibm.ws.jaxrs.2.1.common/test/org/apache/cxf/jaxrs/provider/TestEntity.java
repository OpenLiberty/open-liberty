/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
package org.apache.cxf.jaxrs.provider;

public class TestEntity {

    private String data1;

    private Integer data2;

    private Boolean data3;

    public String getData1() {
        return data1;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }

    public Integer getData2() {
        return data2;
    }

    public void setData2(Integer data2) {
        this.data2 = data2;
    }

    public Boolean getData3() {
        return data3;
    }

    public void setData3(Boolean data3) {
        this.data3 = data3;
    }
}