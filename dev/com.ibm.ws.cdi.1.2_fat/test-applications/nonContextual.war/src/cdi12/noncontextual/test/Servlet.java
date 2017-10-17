package cdi12.noncontextual.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.inject.spi.Unmanaged;
import javax.enterprise.inject.spi.Unmanaged.UnmanagedInstance;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cdi12.noncontextual.test.NonContextualBean;

@WebServlet("/")
public class Servlet extends HttpServlet {

    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();

        Unmanaged<NonContextualBean> unmanagedBean = new Unmanaged<NonContextualBean>(NonContextualBean.class);
        UnmanagedInstance<NonContextualBean> beanInstance = unmanagedBean.newInstance();
        NonContextualBean bean = beanInstance.produce().inject().postConstruct().get();

        pw.append(bean.hello());

        beanInstance.preDestroy().dispose();

        pw.flush();
        pw.close();
    }

}
