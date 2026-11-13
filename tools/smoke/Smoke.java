package uk.co.pentest.psychoPATH;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import burp.IHttpRequestResponse;
import burp.IIntruderPayloadGenerator;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import uk.co.pentest.psychoPATH.IntruderPayloadGenerator;
import uk.co.pentest.psychoPATH.IntruderPayloadGeneratorByte;
import uk.co.pentest.psychoPATH.PayloadFactory;
import uk.co.pentest.psychoPATH.PsychoPATH;
import uk.co.pentest.psychoPATH.PsychoPanel2;
import uk.co.pentest.psychoPATH.PsychoTab;

/**
 * Local smoke harness. Not part of the extension. Talks to the real classes
 * through a stand-in for the Burp callbacks so payload generation can be
 * exercised without starting Burp.
 */
public class Smoke {
    static int pass = 0;
    static int fail = 0;
    static final List<String> rows = new ArrayList<String>();

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            rows.add("PASS | " + name + " | " + detail);
        } else {
            fail++;
            rows.add("FAIL | " + name + " | " + detail);
        }
        System.out.println((ok ? "PASS" : "FAIL") + " " + name + " :: " + detail);
    }

    static Object proxy(Class<?> type, final InvocationHandler handler) {
        return Proxy.newProxyInstance(Smoke.class.getClassLoader(), new Class<?>[] { type }, handler);
    }

    public static void main(String[] args) throws Exception {
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final List<String> calls = new ArrayList<String>();
        final List<String> generatorNames = new ArrayList<String>();

        InvocationHandler helpersHandler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                String n = method.getName();
                if ("bytesToString".equals(n)) {
                    return new String((byte[]) args[0], StandardCharsets.ISO_8859_1);
                }
                if ("stringToBytes".equals(n)) {
                    return ((String) args[0]).getBytes(StandardCharsets.ISO_8859_1);
                }
                if ("getSiteMap".equals(n)) {
                    return new IHttpRequestResponse[0];
                }
                Class<?> rt = method.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return Integer.valueOf(0);
                if (rt == byte.class) return Byte.valueOf((byte) 0);
                if (rt == short.class) return Short.valueOf((short) 0);
                if (rt == long.class) return Long.valueOf(0L);
                return null;
            }
        };
        final Object helpers = proxy(IExtensionHelpers.class, helpersHandler);

        InvocationHandler callbacksHandler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) {
                String n = method.getName();
                calls.add(n);
                if ("getStdout".equals(n) || "getStderr".equals(n)) {
                    return stdout;
                }
                if ("getHelpers".equals(n)) {
                    return helpers;
                }
                if ("getSiteMap".equals(n)) {
                    return new IHttpRequestResponse[0];
                }
                if ("setExtensionName".equals(n)) {
                    calls.add("name=" + args[0]);
                    return null;
                }
                if ("registerIntruderPayloadGeneratorFactory".equals(n)) {
                    PayloadFactory factory = (PayloadFactory) args[0];
                    generatorNames.add(factory.getGeneratorName());
                    return null;
                }
                Class<?> rt = method.getReturnType();
                if (rt == boolean.class) return Boolean.FALSE;
                if (rt == int.class) return Integer.valueOf(0);
                if (rt == byte.class) return Byte.valueOf((byte) 0);
                return null;
            }
        };
        IBurpExtenderCallbacks callbacks = (IBurpExtenderCallbacks) proxy(IBurpExtenderCallbacks.class, callbacksHandler);
        PsychoPATH.callbacks = callbacks;

        BurpExtender extender = new BurpExtender();
        extender.registerExtenderCallbacks(callbacks);
        check("extension name", calls.contains("name=PathHunter"), "setExtensionName recorded");
        check("suite tab registered", calls.contains("addSuiteTab"), "addSuiteTab called");
        check("state listener", calls.contains("registerExtensionStateListener"), "registerExtensionStateListener called");
        check("scanner check", calls.contains("registerScannerCheck"), "registerScannerCheck called");
        check("context menu", calls.contains("registerContextMenuFactory"), "registerContextMenuFactory called");
        check("four generators",
                generatorNames.size() == 4
                        && generatorNames.contains("Byte")
                        && generatorNames.contains("Directory checker")
                        && generatorNames.contains("Path traversal")
                        && generatorNames.contains("Payload marker"),
                generatorNames.toString());

        PsychoTab tab = PsychoPATH.PsychoTab;
        PsychoPanel2 panel = tab.getUiComponent();
        check("tab caption", "PathHunter".equals(tab.getTabCaption()), tab.getTabCaption());
        check("ui component", panel != null, panel.getClass().getName());
        check("default LFI mode", panel.LFImode, "LFImode=" + panel.LFImode);
        check("default filename", "etc/passwd".equals(panel.fileNameField.getText()), panel.fileNameField.getText());
        check("scanner rules",
                panel.scannerMatchRules.getModel().getSize() == 3
                        && "root:x:".equals(panel.scannerMatchRules.getModel().getElementAt(0)),
                "size=" + panel.scannerMatchRules.getModel().getSize());
        check("scanner checkbox", panel.scannerChecksCheckbox.isSelected(), "selected=" + panel.scannerChecksCheckbox.isSelected());

        quiet(panel);
        panel.evasiveTechniques = false;
        panel.mixEncodingsCheckBox.setSelected(false);
        panel.useTerminatorsCheckBox.setSelected(false);
        panel.encodingsEnabled = new int[] {1, 0, 0, 0, 0, 0};
        panel.slashesToUse = "nix";
        panel.LFImode = true;
        panel.optimizeLFI = true;
        panel.maxTraversalsPerPayloadSpinner.setValue(Integer.valueOf(2));
        panel.fileNameField.setText("etc/passwd");

        List<String> raw = collect("path", tab, false);
        check("raw LFI payload count", raw.size() > 0, "count=" + raw.size() + " sample=" + preview(raw));
        check("raw traversal shape", contains(raw, "../../etc/passwd") || containsPrefix(raw, "../"), preview(raw));

        panel.encodingsEnabled = new int[] {1, 1, 0, 0, 0, 0};
        List<String> url = collect("path", tab, false);
        check("url encoding present", anyContains(url, "%2e") || anyContains(url, "%2f"), "count=" + url.size() + " sample=" + firstMatch(url, "%2"));

        panel.urlEncodeUtf.setSelected(false);
        panel.encodingsEnabled = new int[] {1, 0, 1, 0, 0, 0};
        List<String> dblLiteral = collect("path", tab, false);
        check("double url slot without UTF checkbox",
                !anyContains(dblLiteral, "%25") && (anyContains(dblLiteral, "%2e") || dblLiteral.size() > 0),
                "count=" + dblLiteral.size() + " sample=" + preview(dblLiteral));
        panel.urlEncodeUtf.setSelected(true);
        List<String> dbl = collect("path", tab, false);
        check("double url with UTF checkbox", anyContains(dbl, "%25"), "count=" + dbl.size() + " sample=" + firstMatch(dbl, "%25"));
        panel.urlEncodeUtf.setSelected(false);

        panel.encodingsEnabled = new int[] {1, 0, 0, 0, 0, 0};
        panel.useTerminatorsCheckBox.setSelected(true);
        panel.noLiteralNullbyte.setSelected(true);
        List<String> terminated = collect("path", tab, false);
        check("null terminator appended", anyContains(terminated, "%00"), "count=" + terminated.size() + " sample=" + firstMatch(terminated, "%00"));
        panel.useTerminatorsCheckBox.setSelected(false);

        List<String> marks = collect("mark", tab, false);
        check("marker length", marks.size() == raw.size() || marks.size() > 0, "marks=" + marks.size() + " first=" + (marks.isEmpty() ? "" : marks.get(0)));
        if (!marks.isEmpty()) {
            check("marker padding", marks.get(0).length() == 7 && marks.get(0).endsWith("0"), marks.get(0));
        }

        panel.byteGeneratorRange = "alpha";
        IntruderPayloadGeneratorByte bytes = new IntruderPayloadGeneratorByte(tab);
        int byteCount = 0;
        String firstByte = null;
        while (bytes.hasMorePayloads()) {
            byte[] p = bytes.getNextPayload(new byte[0]);
            if (byteCount == 0) firstByte = new String(p, StandardCharsets.ISO_8859_1);
            byteCount++;
        }
        check("byte generator alphanumeric", byteCount == 62, "count=" + byteCount + " first=" + firstByte);

        boolean npe = false;
        try {
            panel.byteGeneratorRange = "not-a-range";
            new IntruderPayloadGeneratorByte(tab);
        } catch (NullPointerException ex) {
            npe = true;
        }
        check("unknown byte range errors", npe, npe ? "NullPointerException" : "no exception");
        panel.byteGeneratorRange = "non-alpha";

        panel.genericSuffixes.clear();
        panel.genericSuffixes.add("/images");
        panel.genericSuffixes.add("/css");
        List<String> dirs = collect("check", tab, false);
        check("directory checker uses genericSuffixes", dirs.size() == 2 && dirs.contains("/images"), dirs.toString());

        panel.suffixesList.setListData(new String[] {"/should-not-appear"});
        List<String> dirsAfterUiEdit = collect("check", tab, false);
        check("directory checker ignores suffixesList edits",
                !dirsAfterUiEdit.contains("/should-not-appear") && dirsAfterUiEdit.contains("/images"),
                dirsAfterUiEdit.toString());

        boolean bounds = false;
        String boundsDetail = "no exception";
        try {
            panel.genericSuffixes.clear();
            IntruderPayloadGenerator empty = new IntruderPayloadGenerator("check", tab, false);
            if (empty.hasMorePayloads()) {
                boundsDetail = "unexpected payloads";
            } else {
                empty.getNextPayload(new byte[0]);
            }
        } catch (IndexOutOfBoundsException ex) {
            bounds = true;
            boundsDetail = "IndexOutOfBoundsException";
        } catch (Exception ex) {
            boundsDetail = ex.getClass().getName() + ": " + ex.getMessage();
        }
        check("empty directory checker throws", bounds, boundsDetail);
        panel.genericSuffixes.add("/images");

        panel.LFImode = false;
        panel.useAbsoluteWebroots = false;
        panel.optimizeDocroots = true;
        panel.evasiveTechniques = false;
        panel.mixEncodingsCheckBox.setSelected(false);
        panel.useTerminatorsCheckBox.setSelected(false);
        panel.encodingsEnabled = new int[] {1, 0, 0, 0, 0, 0};
        panel.slashesToUse = "nix";
        panel.maxTraversalsPerPayloadSpinner.setValue(Integer.valueOf(1));
        panel.fileNameField.setText("a.jpg");
        panel.docrootsList.setListData(new String[] {"/var/www/{TARGET}"});
        panel.targetsList.setListData(new String[] {"app"});
        panel.suffixesList.setListData(new String[0]);
        List<String> upload = collect("path", tab, false);
        check("webroot substitution", anyContains(upload, "var/www/app") && anyContains(upload, "a.jpg"),
                "count=" + upload.size() + " sample=" + firstMatch(upload, "var/www"));

        panel.slashesToUse = "win";
        panel.LFImode = true;
        panel.optimizeLFI = true;
        panel.fileNameField.setText("win.ini");
        panel.maxTraversalsPerPayloadSpinner.setValue(Integer.valueOf(1));
        List<String> win = collect("path", tab, false);
        check("windows traversal unit", anyContains(win, "..\\") || anyContains(win, "win.ini"), preview(win));

        panel.updateScope("https", "www.example.org");
        boolean sawHost = false;
        boolean sawLabel = false;
        javax.swing.ListModel model = panel.targetsList.getModel();
        StringBuilder targets = new StringBuilder();
        for (int i = 0; i < model.getSize(); i++) {
            String item = String.valueOf(model.getElementAt(i));
            targets.append(item).append(",");
            if ("www.example.org".equals(item)) sawHost = true;
            if ("www".equals(item) || "example".equals(item)) sawLabel = true;
        }
        check("scope keeps hostname", sawHost, targets.toString());
        check("scope does not split labels", !sawLabel, "labelsPresent=" + sawLabel + " targets=" + targets);

        System.out.println("SUMMARY pass=" + pass + " fail=" + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }

    static void quiet(PsychoPanel2 panel) {
        panel.stdout = new java.io.PrintWriter(new ByteArrayOutputStream(), true);
    }

    static List<String> collect(String type, PsychoTab tab, boolean scanner) {
        IntruderPayloadGenerator generator = new IntruderPayloadGenerator(type, tab, scanner);
        List<String> out = new ArrayList<String>();
        byte[] base = new byte[0];
        int guard = 0;
        while (generator.hasMorePayloads()) {
            byte[] payload = generator.getNextPayload(base);
            out.add(new String(payload, StandardCharsets.ISO_8859_1));
            guard++;
            if (guard > 20000) {
                out.add("TRUNCATED");
                break;
            }
        }
        return out;
    }

    static boolean contains(List<String> values, String exact) {
        return values.contains(exact);
    }

    static boolean containsPrefix(List<String> values, String prefix) {
        for (String value : values) {
            if (value.startsWith(prefix)) return true;
        }
        return false;
    }

    static boolean anyContains(List<String> values, String needle) {
        for (String value : values) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    static String firstMatch(List<String> values, String needle) {
        for (String value : values) {
            if (value.contains(needle)) return value;
        }
        return values.isEmpty() ? "" : values.get(0);
    }

    static String preview(List<String> values) {
        StringBuilder sb = new StringBuilder();
        int n = Math.min(5, values.size());
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(" | ");
            sb.append(values.get(i));
        }
        sb.append(" (n=").append(values.size()).append(")");
        return sb.toString();
    }
}
