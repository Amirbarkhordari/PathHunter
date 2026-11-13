import java.lang.reflect.Constructor;
import java.net.URL;

/**
 * Uses Burp on the JVM classpath (as Burp itself does) so Zzq3 can resolve
 * allowlisted burp.* API types from its parent, while extension classes load
 * from the extension URL via Zv_d.findClass.
 */
public class ProveBlock {
    public static void main(String[] args) throws Exception {
        URL ext = new java.io.File(args[0]).toURI().toURL();
        Class<?> zv = Class.forName("burp.Zv_d");
        Constructor<?> c = zv.getConstructor(URL[].class);
        ClassLoader extLoader = (ClassLoader) c.newInstance((Object) new URL[] { ext });
        Class<?> iBurp = Class.forName("burp.IBurpExtender");
        String[] names = {
            "burp.BurpExtender",
            "uk.co.pentest.psychoPATH.BurpExtender",
            "uk.co.pentest.psychoPATH.Menu",
            "uk.co.pentest.psychoPATH.PsychoPATH",
            "burp.IBurpExtender"
        };
        int fail = 0;
        for (String n : names) {
            try {
                Class<?> cls = Class.forName(n, false, extLoader);
                boolean legacy = iBurp.isAssignableFrom(cls);
                System.out.println("LOADED " + n + " legacyAssignable=" + legacy + " loader=" + cls.getClassLoader());
            } catch (Throwable t) {
                fail++;
                System.out.println("FAILED " + n + " :: " + t.getClass().getName() + ": " + t.getMessage());
            }
        }
        // Expectations for the fixed jar:
        // burp.BurpExtender must FAIL (blocked)
        // uk.co.pentest.psychoPATH.BurpExtender must LOAD and be assignable
        boolean entryOk;
        try {
            Class<?> entry = Class.forName("uk.co.pentest.psychoPATH.BurpExtender", false, extLoader);
            entryOk = iBurp.isAssignableFrom(entry);
        } catch (Throwable t) {
            entryOk = false;
        }
        boolean oldBlocked;
        try {
            Class.forName("burp.BurpExtender", false, extLoader);
            oldBlocked = false;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            oldBlocked = true;
        }
        System.out.println("CHECK old burp.BurpExtender blocked=" + oldBlocked);
        System.out.println("CHECK new entry assignable=" + entryOk);
        if (!(oldBlocked && entryOk)) {
            System.exit(1);
        }
    }
}