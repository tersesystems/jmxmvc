package com.tersesystems.jmxmvc.examples.alphabet;

import com.tersesystems.jmxmvc.view.ViewInterceptor;
import com.tersesystems.jmxmvc.view.ViewInterceptorBuilder;

import java.io.IOException;
import java.lang.management.ManagementFactory;

public class Main {

    static {
        org.slf4j.bridge.SLF4JBridgeHandler.install();
    }

    public static void main(String[] args) {
        try {
            final AlphabetModel model = new AlphabetModel("alphabet");
            ViewInterceptor interceptor = new ViewInterceptorBuilder().withMBeanServer(ManagementFactory.getPlatformMBeanServer()).withModel(model).build();
            interceptor.start();

            System.out.println("\nPress <Enter> to stop the agent...");
            waitForEnterPressed();

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void waitForEnterPressed() throws IOException {
        boolean done = false;
        while (!done) {
            char ch = (char) System.in.read();
            if (ch < 0 || ch == '\n') {
                done = true;
            }
        }
    }
}
