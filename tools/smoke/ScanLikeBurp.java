import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Mimic burp.Zojj.Zk discovery against an extension JAR using Burp's Zv_d. */
public class ScanLikeBurp {
    public static void main(String[] args) throws Exception {
        String extPath = args[0];
        URL ext = new java.io.File(extPath).toURI().toURL();
        Class<?> zv = Class.forName("burp.Zv_d");
        Constructor<?> c = zv.getConstructor(URL[].class);
        ClassLoader extLoader = (ClassLoader) c.newInstance((Object) new URL[] { ext });
        Class<?> iBurp = Class.forName("burp.IBurpExtender");
        Class<?> montoya = Class.forName("burp.api.montoya.BurpExtension");
        int candidates = 0;
        String found = null;
        try (JarFile jf = new JarFile(extPath)) {
            Enumeration<JarEntry> en = jf.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class")) continue;
                if (n.contains("module-info")) continue;
                if (n.contains("META-INF")) continue;
                String cn = n.substring(0, n.length() - 6).replace('/', '.');
                try {
                    Class<?> cls = Class.forName(cn, false, extLoader);
                    if (cls.isInterface()) continue;
                    if (java.lang.reflect.Modifier.isAbstract(cls.getModifiers())) continue;
                    boolean legacy = iBurp.isAssignableFrom(cls);
                    boolean mon = montoya.isAssignableFrom(cls);
                    if (legacy || mon) {
                        candidates++;
                        found = cn + " legacy=" + legacy + " montoya=" + mon;
                        System.out.println("CANDIDATE " + found);
                        cls.getDeclaredConstructor().newInstance();
                        System.out.println("INSTANTIATED " + cn);
                    }
                } catch (Throwable t) {
                    System.out.println("SKIP " + cn + " :: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }
        }
        System.out.println("SUMMARY candidates=" + candidates + " first=" + found);
        if (candidates < 1) System.exit(1);
    }
}