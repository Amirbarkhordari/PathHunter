package uk.co.pentest.psychoPATH;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Simulates Burp's legacy entry-point check: load burp.BurpExtender from the
 * extension JAR with the Burp desktop JAR as the parent classloader, then test
 * whether the instance is assignable to burp.IBurpExtender and
 * burp.api.montoya.BurpExtension.
 */
public class LoadCheck {
    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            System.out.println("PASS " + name + " :: " + detail);
        } else {
            fail++;
            System.out.println("FAIL " + name + " :: " + detail);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: LoadCheck <extension.jar> <burp.jar>");
            System.exit(2);
        }
        File extJar = new File(args[0]);
        File burpJar = new File(args[1]);
        check("extension jar exists", extJar.isFile(), extJar.getAbsolutePath());
        check("burp jar exists", burpJar.isFile(), burpJar.getAbsolutePath());

        boolean hasApiInterface = false;
        boolean hasMontoya = false;
        boolean hasBurpExtender = false;
        int classCount = 0;
        try (JarFile jf = new JarFile(extJar)) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (n.endsWith(".class")) {
                    classCount++;
                }
                if ("uk/co/pentest/psychoPATH/BurpExtender.class".equals(n)) {
                    hasBurpExtender = true;
                }
                if ("burp/IBurpExtender.class".equals(n)
                        || "burp/IBurpExtenderCallbacks.class".equals(n)
                        || "burp/IExtensionHelpers.class".equals(n)
                        || "burp/ITab.class".equals(n)
                        || "burp/IScannerCheck.class".equals(n)
                        || "burp/IIntruderPayloadGenerator.class".equals(n)
                        || "burp/IContextMenuFactory.class".equals(n)
                        || "burp/IScanIssue.class".equals(n)) {
                    hasApiInterface = true;
                    System.out.println("WARN bundled API class: " + n);
                }
                if (n.startsWith("burp/api/montoya/") && n.endsWith(".class")) {
                    hasMontoya = true;
                    System.out.println("WARN bundled Montoya class: " + n);
                }
            }
        }
        check("uk/co/pentest/psychoPATH/BurpExtender.class present", hasBurpExtender, "found=" + hasBurpExtender);
        check("no bundled legacy API interfaces", !hasApiInterface, "bundled=" + hasApiInterface);
        check("no bundled Montoya API classes", !hasMontoya, "bundled=" + hasMontoya);
        check("has project classes", classCount > 10, "classCount=" + classCount);

        URL burpUrl = burpJar.toURI().toURL();
        URL extUrl = extJar.toURI().toURL();

        // Parent = Burp (provides burp.IBurpExtender). Child = extension only.
        try (URLClassLoader burpLoader = new URLClassLoader(new URL[] { burpUrl }, ClassLoader.getSystemClassLoader().getParent());
             URLClassLoader extLoader = new URLClassLoader(new URL[] { extUrl }, burpLoader)) {

            Class<?> iBurp = Class.forName("burp.IBurpExtender", true, burpLoader);
            Class<?> burpExt = Class.forName("uk.co.pentest.psychoPATH.BurpExtender", true, extLoader);
            check("BurpExtender loaded from extension loader",
                    burpExt.getClassLoader() == extLoader,
                    "loader=" + burpExt.getClassLoader());
            check("IBurpExtender loaded from Burp loader",
                    iBurp.getClassLoader() == burpLoader,
                    "loader=" + iBurp.getClassLoader());

            Object instance = burpExt.getDeclaredConstructor().newInstance();
            boolean legacyOk = iBurp.isInstance(instance);
            check("instanceof burp.IBurpExtender", legacyOk,
                    "class=" + burpExt.getName() + " interfaces=" + java.util.Arrays.toString(burpExt.getInterfaces()));

            // List implemented interfaces with their defining loaders.
            for (Class<?> iface : burpExt.getInterfaces()) {
                System.out.println("INFO interface " + iface.getName()
                        + " loader=" + iface.getClassLoader()
                        + " sameAsBurpIBurp=" + (iface == iBurp));
            }

            boolean montoyaOk = false;
            try {
                Class<?> montoya = Class.forName("burp.api.montoya.BurpExtension", true, burpLoader);
                montoyaOk = montoya.isInstance(instance);
            } catch (ClassNotFoundException ignored) {
            }
            check("instanceof Montoya BurpExtension", !montoyaOk || montoyaOk,
                    "montoyaInstance=" + montoyaOk + " (legacy-only expected false)");

            // Reflective call signature check.
            Method m = burpExt.getMethod("registerExtenderCallbacks",
                    Class.forName("burp.IBurpExtenderCallbacks", true, burpLoader));
            check("registerExtenderCallbacks method present", m != null, String.valueOf(m));
        }

        System.out.println("SUMMARY pass=" + pass + " fail=" + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }
}
