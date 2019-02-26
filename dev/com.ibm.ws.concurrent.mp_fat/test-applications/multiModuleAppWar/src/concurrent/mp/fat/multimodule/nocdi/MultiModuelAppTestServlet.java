package concurrent.mp.fat.multimodule.nocdi;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import concurrent.mp.fat.multimodule.withcdi.CDIEnabledResource;

@SuppressWarnings("serial")
@WebServlet("/MultiModuelAppTestServlet")
public class MultiModuelAppTestServlet extends FATServlet {

    @Test
    public void testWorking() throws Exception {
        CDIEnabledResource.sayHello();
    }

    @Test
    public void verifyCDIOff() throws Exception {
        System.out.println("@AGG bm=" + CDI.current().getBeanManager());
    }

    @Test
    public void verifyCDIOn() throws Exception {
        System.out.println("@AGG bm2=" + CDIEnabledResource.getBeanManager());
    }

}
