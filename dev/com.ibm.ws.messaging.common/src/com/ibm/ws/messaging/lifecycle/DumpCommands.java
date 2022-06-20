package com.ibm.ws.messaging.lifecycle;

import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.logging.Introspector;

@Component(
        service = Object.class,
        configurationPolicy = ConfigurationPolicy.IGNORE,
        property = {
                "osgi.command.scope=liberty",
                "osgi.command.function=dump",
                "service.vendor=IBM"
        })
public class DumpCommands {
    private final Map<String, Introspector> introspectors = new TreeMap<>();
    
    @Activate
    public DumpCommands(@Reference(policyOption = GREEDY) List<Introspector> introspectors) {
        introspectors.stream().forEach(i -> this.introspectors.put(i.getIntrospectorName(), i));
    }
    
    public void dump(String...names) throws Exception {
        if (names.length == 0) {
            System.out.println("Available Introspectors:");
            introspectors.keySet().forEach(name -> System.out.println("\t" + name));            
        }
        for (String name: names) {
            Introspector introspector = introspectors.get(name);
            if (introspector == null) {
                System.err.println("Unknown introspector: '" + name + "'");
                System.err.println("To list available introspectors, run this command without any arguments");
                return;
            }
            System.out.println(introspector.getIntrospectorName());
            System.out.println(introspector.getIntrospectorDescription());
            try (PrintWriter pw = new PrintWriter(System.out)) {
                introspector.introspect(pw);
            }
        }
    }
}
