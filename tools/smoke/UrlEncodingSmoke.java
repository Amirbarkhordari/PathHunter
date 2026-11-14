package uk.co.pentest.psychoPATH;

import burp.IBurpExtenderCallbacks;
import burp.IExtensionHelpers;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2.2 URL encoding / decode-order bypass checks.
 */
public class UrlEncodingSmoke {
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
        // Unit: encoding
        check("single encode ../",
                "%2e%2e%2f".equals(UrlEncodingEngine.encodeStringDepth("../", 1, false)),
                UrlEncodingEngine.encodeStringDepth("../", 1, false));
        check("double encode ../",
                "%252e%252e%252f".equals(UrlEncodingEngine.encodeStringDepth("../", 2, false)),
                UrlEncodingEngine.encodeStringDepth("../", 2, false));
        check("triple encode ../",
                "%25252e%25252e%25252f".equals(UrlEncodingEngine.encodeStringDepth("../", 3, false)),
                UrlEncodingEngine.encodeStringDepth("../", 3, false));
        check("upper hex",
                "%2E%2E%2F".equals(UrlEncodingEngine.encodeStringDepth("../", 1, true)),
                UrlEncodingEngine.encodeStringDepth("../", 1, true));
        check("windows unit encode",
                "%2e%2e%5c".equals(UrlEncodingEngine.encodeStringDepth("..\\", 1, false)),
                UrlEncodingEngine.encodeStringDepth("..\\", 1, false));
        check("depth cap",
                UrlEncodingEngine.encodeStringDepth(".", UrlEncodingEngine.HARD_MAX_DEPTH + 3, false)
                        .equals(UrlEncodingEngine.encodeStringDepth(".", UrlEncodingEngine.HARD_MAX_DEPTH, false)),
                "capped");

        // Decode simulator
        List<String> stages1 = UrlEncodingEngine.simulateDecodeStages("%2e%2e%2f", 2);
        check("one decode to traversal",
                stages1.size() >= 2 && "../".equals(stages1.get(1)), stages1.toString());
        List<String> stages2 = UrlEncodingEngine.simulateDecodeStages("%252e%252e%252f", 3);
        check("two decode stages",
                stages2.size() >= 3 && "%2e%2e%2f".equals(stages2.get(1)) && "../".equals(stages2.get(2)),
                stages2.toString());
        check("becomes traversal after 2 decodes",
                UrlEncodingEngine.becomesLiteralTraversalAfterDecodes("%252e%252e%252fetc/passwd", 2),
                "yes");
        check("malformed percent safe",
                "%zz".equals(UrlEncodingEngine.urlDecodeOnce("%zz")),
                UrlEncodingEngine.urlDecodeOnce("%zz"));
        check("empty decode",
                "".equals(UrlEncodingEngine.urlDecodeOnce("")),
                "empty");

        // Config validation
        UrlEncodingEngine.Config bad = new UrlEncodingEngine.Config();
        bad.targetPath = "";
        bad.single = false;
        bad.doubleEnc = false;
        bad.triple = false;
        bad.mixed = false;
        bad.mixedSeparators = false;
        bad.caseVariants = false;
        UrlEncodingEngine.ValidationResult badVr = UrlEncodingEngine.validateConfig(bad);
        check("invalid empty strategies/path", !badVr.valid, badVr.message);

        UrlEncodingEngine.Config okCfg = new UrlEncodingEngine.Config();
        okCfg.enable = true;
        okCfg.maxDepth = 2;
        okCfg.traversalRepeat = 3;
        okCfg.targetPath = "/etc/passwd";
        okCfg.osTarget = "Linux";
        okCfg.single = true;
        okCfg.doubleEnc = true;
        okCfg.triple = false;
        okCfg.mixed = true;
        okCfg.mixedSeparators = true;
        okCfg.caseVariants = false;
        UrlEncodingEngine.ValidationResult okVr = UrlEncodingEngine.validateConfig(okCfg);
        check("valid config", okVr.valid, okVr.message);

        List<UrlEncodingEngine.EncodedPayload> gen = UrlEncodingEngine.generate(okVr.normalized);
        check("generates payloads", gen.size() > 0, "count=" + gen.size());
        check("contains double-encoded classic",
                containsPayload(gen, "%252e%252e%252f%252e%252e%252f%252e%252e%252fetc/passwd")
                        || containsSubstring(gen, "%252e%252e%252f"),
                sample(gen));
        check("contains single-encoded",
                containsSubstring(gen, "%2e%2e%2f"), sample(gen));
        check("no bare absolute without traversal from this engine",
                !containsExact(gen, "/etc/passwd"), sample(gen));
        check("payloads include etc/passwd target",
                containsSubstring(gen, "etc/passwd"), sample(gen));
        check("strategy metadata present",
                gen.get(0).strategy != null && gen.get(0).strategy.length() > 0, gen.get(0).strategy);
        check("decode stages present",
                gen.get(0).decodeStages != null && gen.get(0).decodeStages.size() >= 1,
                String.valueOf(gen.get(0).decodeStages == null ? 0 : gen.get(0).decodeStages.size()));
        check("dedupe by string",
                UrlEncodingEngine.payloadStrings(gen).size() == gen.size(), "unique");

        // Disabled => empty
        UrlEncodingEngine.Config off = okVr.normalized;
        off.enable = false;
        check("disabled yields empty", UrlEncodingEngine.generate(off).isEmpty(), "empty");

        // Depth bound
        UrlEncodingEngine.Config deep = okVr.normalized;
        deep.enable = true;
        deep.maxDepth = 99;
        UrlEncodingEngine.ValidationResult deepVr = UrlEncodingEngine.validateConfig(deep);
        check("max depth capped", deepVr.valid && deepVr.normalized.maxDepth == UrlEncodingEngine.HARD_MAX_DEPTH,
                "depth=" + (deepVr.normalized == null ? -1 : deepVr.normalized.maxDepth));

        // Integration with Burp extender stubs
        final List<String> calls = new ArrayList<String>();
        InvocationHandler helpersHandler = new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] a) {
                String n = method.getName();
                if ("stringToBytes".equals(n)) {
                    return ((String) a[0]).getBytes(StandardCharsets.ISO_8859_1);
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
                if ("getStdout".equals(n) || "getStderr".equals(n)) return new ByteArrayOutputStream();
                if ("setExtensionName".equals(n)) { calls.add("name=" + a[0]); return null; }
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
        new BurpExtender().registerExtenderCallbacks(callbacks);
        PsychoTab tab = PsychoPATH.PsychoTab;
        PsychoPanel2 panel = tab.getUiComponent();
        Smoke.quiet(panel);

        panel.evasiveTechniques = false;
        panel.mixEncodingsCheckBox.setSelected(false);
        panel.useTerminatorsCheckBox.setSelected(false);
        panel.encodingsEnabled = new int[] {1, 0, 0, 0, 0, 0};
        panel.slashesToUse = "nix";
        panel.LFImode = true;
        panel.optimizeLFI = true;
        panel.maxTraversalsPerPayloadSpinner.setValue(Integer.valueOf(2));
        panel.fileNameField.setText("etc/passwd");
        panel.enableAbsolutePathPayloads = false;
        panel.enableUrlEncodingPayloads = false;

        List<String> baseline = Smoke.collect("path", tab, false);
        check("url encoding off regression traversal",
                Smoke.contains(baseline, "../../etc/passwd") || Smoke.contains(baseline, "etc/passwd"),
                Smoke.preview(baseline));
        check("url encoding off no double-encoded unit",
                !Smoke.anyContains(baseline, "%252e%252e%252f"), Smoke.preview(baseline));

        // Enable URL encoding
        panel.enableUrlEncodingPayloads = true;
        if (panel.enableUrlEncodingPayloadsBox != null) {
            panel.enableUrlEncodingPayloadsBox.setSelected(true);
        }
        if (panel.urlEncDoubleBox != null) panel.urlEncDoubleBox.setSelected(true);
        if (panel.urlEncSingleBox != null) panel.urlEncSingleBox.setSelected(true);
        if (panel.urlEncMixedBox != null) panel.urlEncMixedBox.setSelected(true);
        if (panel.urlEncMixedSepBox != null) panel.urlEncMixedSepBox.setSelected(true);
        if (panel.urlEncCaseBox != null) panel.urlEncCaseBox.setSelected(false);
        if (panel.urlEncTripleBox != null) panel.urlEncTripleBox.setSelected(false);
        if (panel.urlEncDepthSpinner != null) panel.urlEncDepthSpinner.setValue(Integer.valueOf(2));
        if (panel.urlEncTraversalRepeatSpinner != null) panel.urlEncTraversalRepeatSpinner.setValue(Integer.valueOf(3));
        if (panel.urlEncTargetOsCombo != null) panel.urlEncTargetOsCombo.setSelectedItem("Linux");
        if (panel.urlEncTargetPathField != null) panel.urlEncTargetPathField.setText("/etc/passwd");

        List<String> encoded = Smoke.collect("path", tab, false);
        check("url encoding on adds encoded payloads",
                Smoke.anyContains(encoded, "%252e") || Smoke.anyContains(encoded, "%2e%2e%2f"),
                Smoke.preview(encoded));
        check("standard traversal still present",
                Smoke.contains(encoded, "etc/passwd") || Smoke.contains(encoded, "../../etc/passwd"),
                Smoke.preview(encoded));

        // Absolute path regression with URL encoding
        panel.enableAbsolutePathPayloads = true;
        panel.absolutePathTargetMode = "Linux";
        panel.reloadAbsolutePathsForTargetMode(false);
        List<String> both = Smoke.collect("path", tab, false);
        check("absolute path still works with url encoding",
                Smoke.contains(both, "/etc/passwd"), Smoke.preview(both));
        check("url encoding still present with absolute",
                Smoke.anyContains(both, "%252e") || Smoke.anyContains(both, "%2e%2e%2f"),
                Smoke.preview(both));

        // Metadata on psychoPayload via generator internals
        IntruderPayloadGenerator gen2 = new IntruderPayloadGenerator("path", tab, false);
        boolean sawMeta = false;
        if (gen2.psychoPATHs != null) {
            for (int i = 0; i < gen2.psychoPATHs.size(); i++) {
                psychoPayload p = gen2.psychoPATHs.get(i);
                if (p != null && p.strategy != null && p.strategy.contains("URL")) {
                    sawMeta = p.encodingDepth >= 1 || p.transformationDescription != null;
                    if (sawMeta) break;
                }
            }
        }
        check("strategy metadata on url payloads", sawMeta, "meta");

        // Intruder still returns payloads
        check("intruder path non-empty", both.size() > 0, "count=" + both.size());

        System.out.println("SUMMARY pass=" + pass + " fail=" + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }

    static boolean containsPayload(List<UrlEncodingEngine.EncodedPayload> list, String exact) {
        for (int i = 0; i < list.size(); i++) {
            if (exact.equals(list.get(i).payload)) return true;
        }
        return false;
    }

    static boolean containsExact(List<UrlEncodingEngine.EncodedPayload> list, String exact) {
        return containsPayload(list, exact);
    }

    static boolean containsSubstring(List<UrlEncodingEngine.EncodedPayload> list, String needle) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).payload != null && list.get(i).payload.contains(needle)) return true;
        }
        return false;
    }

    static String sample(List<UrlEncodingEngine.EncodedPayload> list) {
        StringBuilder sb = new StringBuilder();
        int n = Math.min(3, list.size());
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(" | ");
            sb.append(list.get(i).payload);
        }
        sb.append(" (n=").append(list.size()).append(")");
        return sb.toString();
    }
}
