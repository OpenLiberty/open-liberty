package com.ibm.ws.cdi12.fat.injectparameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {

    private List<String> resourceList;

    @Inject
    private void testInject(@Named("resource1") String res1,
                            @Named("resource2") String res2,
                            @Named("resource3") String res3,
                            @Named("resource4") String res4,
                            @Named("resource5") String res5,
                            @Named("resource6") String res6,
                            @Named("resource7") String res7,
                            @Named("resource8") String res8,
                            @Named("resource9") String res9,
                            @Named("resource10") String res10,
                            @Named("resource11") String res11,
                            @Named("resource12") String res12,
                            @Named("resource13") String res13,
                            @Named("resource14") String res14,
                            @Named("resource15") String res15,
                            @Named("resource16") String res16
                    ) {
        resourceList = Arrays.asList(res1, res2, res3, res4, res5, res6, res7, res8, res9, res10, res11, res12, res13, res14, res15, res16);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        resp.getOutputStream().println(TestUtils.join(resourceList));
    }

}
