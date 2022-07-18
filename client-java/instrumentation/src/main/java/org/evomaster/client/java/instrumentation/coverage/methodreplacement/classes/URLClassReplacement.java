package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Objects;

public class URLClassReplacement implements MethodReplacementClass {

    /**
     * This is an experimental implementation, not perfect yet.
     * */

    @Override
    public Class<?> getTargetClass() {
        return URL.class;
    }

    @Replacement(
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.NET,
            id = "URL_openConnection_Replacement",
            replacingStatic = false,
            usageFilter = UsageFilter.ANY
    )
    public static URLConnection openConnection(URL caller) throws java.io.IOException {
        Objects.requireNonNull(caller);

        /*
          Add the external service hostname to the ExecutionTracer
          */
        if (caller.getProtocol().equals("http") || caller.getProtocol().equals("https")) {
            int port = caller.getPort();
            String protocol = caller.getProtocol();

            // Unless the port number is specified, the default will be -1.
            // Which indicates that the port should be assigned according to the
            // protocol. Since the URLConnection openConnection is an abstract, this
            // assignment will be handled under the respective implementation.
            // Here it's manually handled assuming these default will never change. :)
            if (port == -1) {
                switch (protocol) {
                    case "https":
                        port = 443;
                        break;
                    case "http":
                        port = 80;
                        break;
                }
            }
            ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo(protocol, caller.getHost(), port);
            ExecutionTracer.addExternalServiceHost(remoteHostInfo);

            // TODO: The following logic needs to be changed in future
            if (ExecutionTracer.hasExternalMapping(caller.getHost())) {
                String ip  = ExecutionTracer.getExternalMapping(caller.getHost());

                // Usage of ports below 1024 require root privileges to run
                String url = "http://" + ip + ":" + caller.getPort() + caller.getPath();

                URL newURL = new URL(url);
                return newURL.openConnection();
            } else {
                SimpleLogger.uniqueWarn("There is no WireMock initiated for this hostname");
//                throw new UnknownHostException("There is no WireMock initiated for this hostname");
            }
        }

        return caller.openConnection();
    }
}
