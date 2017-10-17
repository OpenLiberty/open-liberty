/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.implicit.archive.disabled;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.explicit.bean.MyBike;
import com.ibm.ws.cdi.implicit.bean.disabled.MyPlane;

/**
 *
 */
@WebServlet("/")
public class MyCarServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    MyCar myCar;
    @Inject
    MyBike myBike;
    @Inject
    BeanManager beanManager;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();
        pw.write(myCar.getMyCar());

        /** just in case the bean manager is mad */
        Set<Bean<?>> myCar = beanManager.getBeans(MyCar.class);
        if (myCar.isEmpty()) {
            pw.write(" No Car");
        }
        pw.write(myBike.getMyBike());
        /** just in case the bean manager is mad */
        Set<Bean<?>> myBike = beanManager.getBeans(MyBike.class);
        if (myBike.isEmpty()) {
            pw.write(" No Bike");
        }
        /** We know the bean manager is honest */
        Set<Bean<?>> myPlane = beanManager.getBeans(MyPlane.class);
        if (myPlane.isEmpty()) {
            pw.write(" No Plane!");
        }

        pw.flush();
        pw.close();
    }

}
