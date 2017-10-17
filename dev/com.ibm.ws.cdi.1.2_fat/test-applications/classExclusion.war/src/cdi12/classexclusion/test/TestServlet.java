package cdi12.classexclusion.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cdi12.classexclusion.test.interfaces.IExcludedBean;
import cdi12.classexclusion.test.interfaces.IExcludedByComboBean;
import cdi12.classexclusion.test.interfaces.IExcludedByPropertyBean;
import cdi12.classexclusion.test.interfaces.IExcludedPackageBean;
import cdi12.classexclusion.test.interfaces.IExcludedPackageTreeBean;
import cdi12.classexclusion.test.interfaces.IIncludedBean;
import cdi12.classexclusion.test.interfaces.IProtectedByClassBean;
import cdi12.classexclusion.test.interfaces.IProtectedByHalfComboBean;
import cdi12.classexclusion.test.interfaces.IVetoedBean;

@WebServlet("/test")
public class TestServlet extends HttpServlet {

    @Inject
    IIncludedBean included;
    @Inject
    IExcludedBean excluded;
    @Inject
    IExcludedPackageBean excludedPackageBean;
    @Inject
    IExcludedPackageTreeBean excludedPackageTreeBean;
    @Inject
    IProtectedByClassBean protectedByClassBean;
    @Inject
    IExcludedByPropertyBean excludedByPropertyBean;
    @Inject
    IExcludedByComboBean excludedByComboBean;
    @Inject
    IProtectedByHalfComboBean proectedByHalfComboBean;
    @Inject
    IVetoedBean vetoedBean;

    private static final long serialVersionUID = 8549700799591343964L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter pw = response.getWriter();

        pw.write(included.getOutput() + System.getProperty("line.separator"));
        pw.write(excluded.getOutput() + System.getProperty("line.separator"));
        pw.write(excludedPackageBean.getOutput() + System.getProperty("line.separator"));
        pw.write(excludedPackageTreeBean.getOutput() + System.getProperty("line.separator"));
        pw.write(protectedByClassBean.getOutput() + System.getProperty("line.separator"));
        pw.write(excludedByPropertyBean.getOutput() + System.getProperty("line.separator"));
        pw.write(excludedByComboBean.getOutput() + System.getProperty("line.separator"));
        pw.write(proectedByHalfComboBean.getOutput() + System.getProperty("line.separator"));
        pw.write(vetoedBean.getOutput() + System.getProperty("line.separator"));

        pw.flush();
        pw.close();
    }

}
