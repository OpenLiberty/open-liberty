package test.corba.web.war;

import java.rmi.Remote;
import java.util.stream.Stream;

import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;

import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;

import shared.Business;
import shared.TestRemote;

/**
 * Place the IIOP-specific logic in here so the Servlet class can be safely introspected by the JUnit test.
 * (The JUnit test does not have all the Liberty and IIOP classes on its classpath.)
 */
enum Beans {
    ;

    static TestRemote lookupTestBean(ORB orb) throws Exception {
	return lookup(orb, "ejb/global/test.corba/test.corba.bean/TestBean!shared.TestRemote", TestRemote.class);
    }

    static Business lookupBusinessBean(ORB orb) throws Exception {
	return lookup(orb, "ejb/global/test.corba/test.corba.bean/BusinessBean!shared.Business", Business.class);
    }

	private static <T extends Remote> T lookup(ORB orb, String s, Class<T> type) throws Exception {
        System.out.println("### Performing naming lookup ###");
		NamingContext nameService = NamingContextHelper.narrow(orb.resolve_initial_references("NameService"));
        NameComponent[] nameComponents = Stream.of(s.split("/"))
			.map(n->new NameComponent(n,""))
			.toArray(NameComponent[]::new);
        Object o = nameService.resolve(nameComponents);
        T bean = type.cast(PortableRemoteObject.narrow(o, type));
        System.out.println("### bean = " + bean + " ###");
        return bean;
	}

    static int getNumProfiles(Stub ejb, ORB orb) {
        OutputStream os = orb.create_output_stream();
        os.write_Object(ejb);
        InputStream is = os.create_input_stream();
        IOR ior = IORHelper.read(is);
        return ior.profiles.length;
    }
}