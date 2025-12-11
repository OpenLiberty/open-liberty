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
 * Instead, save all data into a StringBuffer until END string is received; 
 * then reply with "PASS" or "FAIL" for client to assert on.
 */
import java.util.ArrayList;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

public class TestReadListener implements ReadListener {
    private static final String CLASS_NAME = TestReadListener.class.getName();
    private ServletInputStream input = null;
    private ServletOutputStream output = null;
    private final String READ_SEPARATOR = " || ";                       // separate between the read which help seeing how many onDataAvailable has been triggered
    private StringBuilder totalData;                                    // track total read 
    private ArrayList<String> dataList = new ArrayList<String>();       // track all received data

    //If modify, also update client DATA_SIZE !
    private final int DATA_SIZE = 50;                                                                   

    TestReadListener(ServletInputStream in, ServletOutputStream out) {
        this.input = in;
        this.output = out;
        totalData = new StringBuilder(); 

        //Expecting client to send data in chunk, begin with "BEGIN" then ,Data_0,...,Data_$DATA_SIZE,END using comma as delimiter for parsing
        dataList.add("BEGIN");
        for(int i = 0; i <= DATA_SIZE ; i++) {
            dataList.add("Data_" + i);
        }
        dataList.add("END");

        LOG("constructor, dataList size [" + dataList.size() + "], this [" + this + "]");  //dataList size should be  (DATA_SIZE + 3) 
    }

    public void onDataAvailable() {
        LOG("onDataAvailable ENTER");

        try {
            StringBuilder sub = new StringBuilder();
            int len = -1;
            byte[] b = new byte[1024];
            LOG(" onDataAvailable, reading ...");

            while (this.input.isReady() && (len = this.input.read(b)) != -1) {
                String data = new String(b, 0, len);
                sub.append(data);
            } 
            LOG(" onDataAvailable, this read data [" + sub.toString() + "]");

            totalData.append(sub.toString() + READ_SEPARATOR); 

            LOG(" onDataAvailable, total read data [" + totalData.toString() + "]");

            //Remove received data from the main dataList
            String[] tmp = sub.toString().split(",");   
            for (String s : tmp) {
                dataList.remove(s);
            }

            LOG(" onDataAvailable, after remove, dataList size [" + dataList.size() + "]");

            if (totalData.toString().contains("END")) {
                if (dataList.size() == 0) {
                    LOG(" onDataAvailable, END string found and dataList size is 0. All data received. Test PASS");
                    output.println("Received [" + totalData.toString() + "] . Test PASS");
                }
                else {
                    LOG(" onDataAvailable, END string found BUT not all data has received.  Missing the following ");
                    for(String s : dataList) {
                        LOG(s);
                    }
                    output.println("NOT all data has received (check server message.log for missing data). Test FAIL");
                }
                output.flush();
            }
            LOG("onDataAvailable EXIT");
        } catch (Exception ex) {
            LOG(" onDataAvailable EXIT with exception [" + ex + "]");
            throw new IllegalStateException(ex);
        } 
    }

    public void onAllDataRead() {
        LOG("onAllDataRead ENTER");

        try {
            LOG(" onAllDataRead, close output stream. this [" + this + "]");
            this.output.close();
        } catch (Exception ex) {
            LOG(" onAllDataRead throws IllegalStateException for exception [" + ex + "]");
            throw new IllegalStateException(ex);
        } 
        LOG("onAllDataRead EXIT");
    }

    public void onError(Throwable t) {
        LOG("onError, encounted error");

        t.printStackTrace();
    }

    private static void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
