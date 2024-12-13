/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package readlistener;

/*
 * For test performance, do not reply/echo back on every received data.
 * Instead, save all data into the StringBuffer until END string is received; 
 * then reply PASS or FAIL depending on whether if all data has received (tracking via the dataList)
 */
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;

public class TestReadListener implements ReadListener {
    private static final String CLASS_NAME = TestReadListener.class.getName();
    private ServletInputStream input = null;
    private ServletOutputStream output = null;
    private final String READ_SEPARATOR = " || ";                       // separate between the read ...help to see how many onDataAvailable has triggered
    private static StringBuilder totalData = new StringBuilder();       // tracking total read - ONE instance for this whole test
    private ArrayList<String> dataList = new ArrayList<String>();       //This dataList track all data that should be received by the application
    private final int sizeList = 30;                                    //If modify, also change in the client sizeList otherwise test will fail.

    TestReadListener(ServletInputStream in, ServletOutputStream out) {
        LOG("constructor, ");
        this.input = in;
        this.output = out;

        //client sending data in chunk, begin with "BEGIN" then ,Data_0,... up to ,Data_30 with comma as delimiter for parsing
        //dataList tracks the received Data_ and remove from the list. Its size should be 0 when END string is received; otherwise data has missed.
        for(int i = 0; i <= sizeList ; i++) {
            dataList.add("Data_" + i);
        }
        LOG("initial dataList size [" + dataList.size() + "]");
    }

    public void onDataAvailable() {
        LOG("onDataAvailable ENTER");

        try {
            StringBuilder sub = new StringBuilder();
            int len = -1;
            byte[] b = new byte[1024];
            LOG("onDataAvailable, reading ...");
            while (this.input.isReady() && (len = this.input.read(b)) != -1) {
                String data = new String(b, 0, len);
                sub.append(data);
            } 
            totalData.append(sub.toString() + READ_SEPARATOR);         // || is added to original data for readability 

            LOG("onDataAvailable, this read data [" + sub.toString() + "]");
            LOG("onDataAvailable EXIT. Total read data [" + totalData.toString() + "]");

            //Remove received data from the main dataList
            String[] tmp = sub.toString().split(",");   
            for (String s : tmp) {
                dataList.remove(s);
            }

            LOG("onDataAvailable, after remove, dataList size [" + dataList.size() + "]");
            if (totalData.toString().contains("END")) {
                if (dataList.size() == 0) {
                    LOG("onDataAvailable, END string found. All data received.");
                    output.println("Received [" + totalData.toString() + "] . PASS");
                }
                else {
                    LOG("onDataAvailable, END string found BUT not all data has received.  Missing the following ");
                    for(String s : dataList) {
                        LOG(s);
                    }
                    output.println("NOT all data has received [" + totalData.toString() + "] . FAIL");
                }
                output.flush();
            }
        } catch (Exception ex) {
            LOG("onDataAvailable EXIT with exception [" + ex + "]");
            throw new IllegalStateException(ex);
        } 
    }

    public void onAllDataRead() {
        LOG("onAllDataRead ENTER");

        try {
            LOG("onAllDataRead, close output stream");

            this.output.close();

            LOG("onAllDataRead EXIT");
        } catch (Exception ex) {
            LOG("onAllDataRead throws IllegalStateException for exception [" + ex + "]");
            throw new IllegalStateException(ex);
        } 
    }

    public void onError(Throwable t) {
        LOG("onError, encounted error");

        t.printStackTrace();
    }

    private static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}