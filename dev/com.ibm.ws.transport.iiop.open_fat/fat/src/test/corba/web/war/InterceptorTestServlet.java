package test.corba.web.war;

import java.rmi.AccessException;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import componenttest.app.FATServlet;
import shared.ClientUtil;

@WebServlet("/InterceptorTestServlet")
@SuppressWarnings("serial")
public class InterceptorTestServlet extends FATServlet {
	@Resource
    private ORB orb;

	@Test
	public void testInterceptorIsInstalledCorrectly() throws Throwable {
		try {
			ClientUtil.lookupBusinessBean(orb).takesString("This shouldn't work");
			Assert.fail("Invoking the bean should result in an exception");
		} catch (AccessException expected) {
			try {
				throw expected.getCause();
			} catch (NO_PERMISSION expectedToo) {
				Assert.assertEquals("Can't touch this.", expectedToo.getMessage());
			}
		}
	}
}
