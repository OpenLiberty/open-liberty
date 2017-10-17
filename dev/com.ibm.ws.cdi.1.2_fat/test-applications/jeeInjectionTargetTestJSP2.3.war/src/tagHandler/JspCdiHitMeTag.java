package tagHandler;

import javax.inject.Inject;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;

import beans.Pojo1;
import beans.TestTagInjectionDependentBean;
import beans.TestTagInjectionRequestBean;
import beans.TestTagInjectionSessionBean;

public class JspCdiHitMeTag extends BodyTagSupport {

    private static final long serialVersionUID = 7413920082866356670L;

    @Inject
    TestTagInjectionRequestBean cdiRequestBean;

    @Inject
    TestTagInjectionSessionBean cdiSessionBean;

    @Inject
    TestTagInjectionDependentBean cdiDependentBean;

    private final TestTagInjectionDependentBean x;

    @Inject
    public JspCdiHitMeTag(TestTagInjectionDependentBean bean) {
        //    public JspCdiHitMeTag() {

        //StringWriter sw = new StringWriter();
        //new Throwable("").printStackTrace(new PrintWriter(sw));
        //String stackTrace = sw.toString();
        // System.out.println("JspCdiHitMeTag constructor stack trace: \n " + stackTrace);

        this.x = bean;
        System.out.println("JspCdiHitMeTag and x is: " + x);
    }

    @Override
    public int doStartTag()
                    throws JspException {

        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag()
                    throws JspException {

        try {
            // print the message out
            String s1 = "x is null";
            if (x != null) {
                s1 = "constructor injection OK";
            }

            String s2 = "interceptor failed";
            if (Pojo1.counter == 1) {
                s2 = "interceptor OK";
            }

            JspWriter out = pageContext.getOut();
            out.println("Message: " + cdiDependentBean.getHitMe() + " " + cdiSessionBean.getHitMe() + " " + cdiRequestBean.getHitMe()
                        + " ..." + s1 + " ..." + s2);

            //System.out.println("HI from doEndTag().  cdiDependentBean.getHitMe() is: " + cdiDependentBean.getHitMe());
            //System.out.println("cdiSessionBean.getHitMe() is: " + cdiSessionBean.getHitMe());
            //System.out.println("cdiRequestBean.getHitMe() is: " + cdiRequestBean.getHitMe());
            //System.out.println("cdiRequestBean.getHitMe() is: " + cdiRequestBean.getHitMe());
            //System.out.println("Pojo1 counter: " + Pojo1.counter);
            //System.out.println("injected bean: " + x);

        } catch (Exception ex) {
            throw new JspException(ex);
        }

        return EVAL_PAGE;
    }

}