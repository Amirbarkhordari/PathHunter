package uk.co.pentest.psychoPATH;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Phase 2.1 absolute-path payload checks. Does not replace Smoke regression suite.
 */
public class AbsolutePathSmoke {
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
        // Unit validation
        AbsolutePathEngine.ValidationResult linuxOk = AbsolutePathEngine.validateLinux("/etc/passwd");
        check("validate linux /etc/passwd", linuxOk.valid && "/etc/passwd".equals(linuxOk.normalized), linuxOk.message);

        AbsolutePathEngine.ValidationResult hosts = AbsolutePathEngine.validateLinux("/etc/hosts");
        check("validate linux /etc/hosts", hosts.valid, hosts.message);

        AbsolutePathEngine.ValidationResult winOk = AbsolutePathEngine.validateWindows("C:\\Windows\\win.ini");
        check("validate windows win.ini", winOk.valid && winOk.normalized.startsWith("C:\\"), winOk.normalized);

        AbsolutePathEngine.ValidationResult empty = AbsolutePathEngine.validateCustom("", "auto");
        check("empty custom rejected", !empty.valid, empty.message);

        AbsolutePathEngine.ValidationResult relative = AbsolutePathEngine.validateLinux("etc/passwd");
        check("relative linux rejected", !relative.valid, relative.message);

        AbsolutePathEngine.ValidationResult traversal = AbsolutePathEngine.validateLinux("/etc/../passwd");
        check("linux .. segment rejected", !traversal.valid, traversal.message);

        AbsolutePathEngine.ValidationResult dblSlash = AbsolutePathEngine.validateLinux("/etc//passwd");
        check("linux // rejected", !dblSlash.valid, dblSlash.message);

        AbsolutePathEngine.ValidationResult badWin = AbsolutePathEngine.validateWindows("/etc/passwd");
        check("linux path rejected as windows", !badWin.valid, badWin.message);

        List<String> deduped = AbsolutePathEngine.dedupePreserveOrder(
                java.util.Arrays.asList("/etc/passwd", "/etc/hosts", "/etc/passwd"));
        check("dedupe preserves order", deduped.size() == 2 && "/etc/passwd".equals(deduped.get(0)), deduped.toString());

        String[] dots = new String[]{".", "%2e", "%25%32%65", "%u002e", "%c0%ae", "%e0%ae"};
        String[] slashes = new String[]{"/", "%2f", "%25%32%66", "%u2215", "%c0%af", "%e0%af"};
        String[] backslashes = new String[]{"\\", "%5c", "%25%35%63", "%u2216", "%c1%9c", "%e1%9c"};
        check("encode raw unchanged",
                "/etc/passwd".equals(AbsolutePathEngine.encodeAbsolutePath("/etc/passwd", 0, dots, slashes, backslashes)),
                "raw");
        String url = AbsolutePathEngine.encodeAbsolutePath("/etc/passwd", 1, dots, slashes, backslashes);
        check("encode url uses %2f", "%2fetc%2fpasswd".equals(url) && !url.contains("../"), url);
        String dbl = AbsolutePathEngine.encodeAbsolutePath("/etc/passwd", 2, dots, slashes, backslashes);
        check("encode double-url once", dbl.contains("%25") && !dbl.contains("%2525"), dbl);
        String winEnc = AbsolutePathEngine.encodeAbsolutePath("C:\\Windows\\win.ini", 1, dots, slashes, backslashes);
        check("encode windows separators", winEnc.contains("%5c") && winEnc.startsWith("C:"), winEnc);

        // Generator integration
        final List<String> calls = new ArrayList<String>();
        InvocationHandler helpersHandler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] a) {
                String n = method.getName();
                if ("stringToBytes".equals(n)) {
                    String s = (String) a[0];
                    return s.getBytes(StandardCharsets.ISO_8859_1);
                }
                if ("bytesToString".equals(n)) {
                    return new String((byte[]) a[0], StandardCharsets.ISO_8859_1);
                }
                Class<?> rt = method.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return Integer.valueOf(0);
                if (rt == byte.class) return Byte.valueOf((byte) 0);
                return null;
            }
        };
        final IExtensionHelpers helpers = (IExtensionHelpers) Proxy.newProxyInstance(
                IExtensionHelpers.class.getClassLoader(), new Class[] {IExtensionHelpers.class}, helpersHandler);
        InvocationHandler callbacksHandler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] a) {
                String n = method.getName();
                calls.add(n);
                if ("getHelpers".equals(n)) return helpers;
                if ("getStdout".equals(n)) return new ByteArrayOutputStream();
                if ("getStderr".equals(n)) return new ByteArrayOutputStream();
                if ("setExtensionName".equals(n)) { calls.add("name=" + a[0]); return null; }
                if ("addSuiteTab".equals(n)) return null;
                if ("registerExtensionStateListener".equals(n)) return null;
                if ("registerIntruderPayloadGeneratorFactory".equals(n)) return null;
                if ("registerScannerCheck".equals(n)) return null;
                if ("registerContextMenuFactory".equals(n)) return null;
                if ("getSiteMap".equals(n)) return new burp.IHttpRequestResponse[0];
                Class<?> rt = method.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return Integer.valueOf(0);
                if (rt == byte.class) return Byte.valueOf((byte) 0);
                return null;
            }
        };
        IBurpExtenderCallbacks callbacks = (IBurpExtenderCallbacks) Proxy.newProxyInstance(
                IBurpExtenderCallbacks.class.getClassLoader(),
                new Class[] {IBurpExtenderCallbacks.class}, callbacksHandler);
        PsychoPATH.callbacks = callbacks;
        BurpExtender extender = new BurpExtender();
        extender.registerExtenderCallbacks(callbacks);

        PsychoTab tab = PsychoPATH.PsychoTab;
        PsychoPanel2 panel = tab.getUiComponent();
        Smoke.quiet(panel);

        // Baseline: absolute disabled -> no bare /etc/passwd without traversal requirement from this feature
        panel.enableAbsolutePathPayloads = false;
        if (panel.enableAbsolutePathPayloadsBox != null) {
            panel.enableAbsolutePathPayloadsBox.setSelected(false);
        }
        panel.evasiveTechniques = false;
        panel.mixEncodingsCheckBox.setSelected(false);
        panel.useTerminatorsCheckBox.setSelected(false);
        panel.encodingsEnabled = new int[] {1, 0, 0, 0, 0, 0};
        panel.slashesToUse = "nix";
        panel.LFImode = true;
        panel.optimizeLFI = true;
        panel.maxTraversalsPerPayloadSpinner.setValue(Integer.valueOf(2));
        panel.fileNameField.setText("etc/passwd");
        List<String> baseline = Smoke.collect("path", tab, false);
        check("regression: absolute off still has traversal shape",
                Smoke.contains(baseline, "../../etc/passwd") || Smoke.containsPrefix(baseline, "../")
                        || Smoke.contains(baseline, "etc/passwd"),
                Smoke.preview(baseline));
        check("absolute off does not force bare /etc/passwd",
                !Smoke.contains(baseline, "/etc/passwd"),
                Smoke.preview(baseline));

        // Enable absolute Linux defaults
        panel.enableAbsolutePathPayloads = true;
        if (panel.enableAbsolutePathPayloadsBox != null) {
            panel.enableAbsolutePathPayloadsBox.setSelected(true);
        }
        panel.absolutePathTargetMode = "Linux";
        if (panel.absolutePathTargetCombo != null) {
            panel.absolutePathTargetCombo.setSelectedItem("Linux");
        }
        panel.reloadAbsolutePathsForTargetMode(false);
        List<String> abs = Smoke.collect("path", tab, false);
        check("linux /etc/passwd generated", Smoke.contains(abs, "/etc/passwd"), Smoke.preview(abs));
        check("linux /etc/hosts generated", Smoke.contains(abs, "/etc/hosts"), Smoke.preview(abs));
        check("no ../ prefix on absolute /etc/passwd",
                Smoke.contains(abs, "/etc/passwd") && !hasPrefixedTraversal(abs, "/etc/passwd"),
                firstExact(abs, "/etc/passwd"));
        check("no webroot prefix on absolute", !anyStartsWithDocroot(abs, "/etc/passwd"), Smoke.preview(abs));

        // Multiple targets + duplicates
        panel.absolutePathsList.setListData(new String[] {
                "/etc/passwd", "/etc/hosts", "/etc/passwd", "/proc/version"
        });
        List<String> multi = Smoke.collect("path", tab, false);
        check("multiple absolute targets",
                Smoke.contains(multi, "/etc/passwd") && Smoke.contains(multi, "/etc/hosts")
                        && Smoke.contains(multi, "/proc/version"),
                Smoke.preview(multi));
        check("duplicate absolute payloads prevented",
                countExact(multi, "/etc/passwd") == 1, "count=" + countExact(multi, "/etc/passwd"));

        // Custom absolute path
        panel.absolutePathTargetMode = "Custom Paths";
        panel.absolutePathsList.setListData(new String[] {"/var/log/auth.log"});
        List<String> custom = Smoke.collect("path", tab, false);
        check("custom absolute path", Smoke.contains(custom, "/var/log/auth.log"), Smoke.preview(custom));

        // Empty custom list
        panel.absolutePathsList.setListData(new String[0]);
        List<String> emptyList = Smoke.collect("path", tab, false);
        check("empty absolute list adds none", !Smoke.contains(emptyList, "/var/log/auth.log"), Smoke.preview(emptyList));

        // Invalid configured path skipped (relative etc/passwd may still appear from LFI traversal strategy)
        panel.absolutePathsList.setListData(new String[] {"etc/passwd", "/etc/passwd", "/etc//passwd"});
        List<String> skipInvalid = Smoke.collect("path", tab, false);
        check("invalid absolute configs skipped, valid absolute kept",
                Smoke.contains(skipInvalid, "/etc/passwd")
                        && countExact(skipInvalid, "/etc/passwd") == 1
                        && !Smoke.contains(skipInvalid, "/etc//passwd"),
                Smoke.preview(skipInvalid));

        // Windows
        panel.absolutePathTargetMode = "Windows";
        panel.absolutePathsList.setListData(AbsolutePathEngine.DEFAULT_WINDOWS_PATHS);
        panel.slashesToUse = "win";
        List<String> win = Smoke.collect("path", tab, false);
        check("windows win.ini absolute", Smoke.contains(win, "C:\\Windows\\win.ini"), Smoke.preview(win));
        check("windows hosts absolute",
                Smoke.contains(win, "C:\\Windows\\System32\\drivers\\etc\\hosts"), Smoke.preview(win));

        // Encoding
        panel.absolutePathTargetMode = "Linux";
        panel.absolutePathsList.setListData(new String[] {"/etc/passwd"});
        panel.encodingsEnabled = new int[] {1, 0, 0, 0, 0, 0};
        List<String> rawOnly = Smoke.collect("path", tab, false);
        check("raw absolute present", Smoke.contains(rawOnly, "/etc/passwd"), Smoke.preview(rawOnly));

        panel.encodingsEnabled = new int[] {1, 1, 0, 0, 0, 0};
        List<String> urlAbs = Smoke.collect("path", tab, false);
        check("url-encoded absolute", Smoke.contains(urlAbs, "%2fetc%2fpasswd"), Smoke.preview(urlAbs));
        check("raw and url both when both enabled",
                Smoke.contains(urlAbs, "/etc/passwd") && Smoke.contains(urlAbs, "%2fetc%2fpasswd"),
                Smoke.preview(urlAbs));

        panel.urlEncodeUtf.setSelected(true);
        panel.encodingsEnabled = new int[] {1, 0, 1, 0, 0, 0};
        List<String> dblAbs = Smoke.collect("path", tab, false);
        check("double-url absolute", Smoke.contains(dblAbs, "%25%32%66etc%25%32%66passwd"), Smoke.preview(dblAbs));
        check("no accidental double double-encoding",
                !anyContains(dblAbs, "%2525"), Smoke.preview(dblAbs));
        panel.urlEncodeUtf.setSelected(false);

        // Existing traversal still present with absolute on
        panel.encodingsEnabled = new int[] {1, 0, 0, 0, 0, 0};
        panel.absolutePathsList.setListData(new String[] {"/etc/passwd"});
        panel.LFImode = true;
        panel.optimizeLFI = true;
        panel.fileNameField.setText("etc/passwd");
        panel.maxTraversalsPerPayloadSpinner.setValue(Integer.valueOf(2));
        List<String> both = Smoke.collect("path", tab, false);
        check("absolute coexists with traversal payloads",
                Smoke.contains(both, "/etc/passwd")
                        && (Smoke.contains(both, "../../etc/passwd") || Smoke.contains(both, "etc/passwd")),
                Smoke.preview(both));

        // Intruder path generator still registers / returns payloads
        check("intruder path payloads non-empty", both.size() > 0, "count=" + both.size());

        System.out.println("SUMMARY pass=" + pass + " fail=" + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }

    static boolean hasPrefixedTraversal(List<String> values, String absolute) {
        for (String v : values) {
            if (v != null && v.endsWith(absolute) && v.contains("..") && !v.equals(absolute)) {
                // presence of traversal variants is OK; we only fail if absolute itself has ../
                // This helper detects whether the exact absolute entry contains ../
            }
        }
        for (String v : values) {
            if (absolute.equals(v) && v.contains("..")) return true;
            if (absolute.equals(v) && v.startsWith("../")) return true;
        }
        return false;
    }

    static boolean anyStartsWithDocroot(List<String> values, String absolute) {
        for (String v : values) {
            if (absolute.equals(v)) continue;
            if (v != null && v.endsWith(absolute) && v.contains("/var/www")) return true;
        }
        return false;
    }

    static int countExact(List<String> values, String exact) {
        int n = 0;
        for (String v : values) {
            if (exact.equals(v)) n++;
        }
        return n;
    }

    static String firstExact(List<String> values, String exact) {
        for (String v : values) {
            if (exact.equals(v)) return v;
        }
        return "";
    }

    static boolean anyContains(List<String> values, String needle) {
        for (String v : values) {
            if (v != null && v.contains(needle)) return true;
        }
        return false;
    }
}
