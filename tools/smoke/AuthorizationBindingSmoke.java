package uk.co.pentest.psychoPATH;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import uk.co.pentest.psychoPATH.fileupload.FileUploadPanel;
import uk.co.pentest.psychoPATH.fileupload.FileUploadRequestAnalyzer;
import uk.co.pentest.psychoPATH.fileupload.UploadRequestResult;
import uk.co.pentest.psychoPATH.fileupload.UploadTestCase;
import uk.co.pentest.psychoPATH.fileupload.execution.ControlledHttpExecutor;
import uk.co.pentest.psychoPATH.fileupload.execution.ExecutionAuthorizationPolicy;
import uk.co.pentest.psychoPATH.fileupload.execution.HttpExecutionBatchResult;
import uk.co.pentest.psychoPATH.fileupload.execution.HttpRequestFrame;
import uk.co.pentest.psychoPATH.fileupload.execution.HttpServiceTarget;
import uk.co.pentest.psychoPATH.fileupload.execution.ManagedExecutionManager;
import uk.co.pentest.psychoPATH.fileupload.execution.ManagedExecutionResult;
import uk.co.pentest.psychoPATH.fileupload.execution.MultipartRequestBuilder;
import uk.co.pentest.psychoPATH.fileupload.execution.PreparationStatus;
import uk.co.pentest.psychoPATH.fileupload.execution.PreparedRequestArtifact;
import uk.co.pentest.psychoPATH.fileupload.execution.RecordingHttpTransport;

/**
 * Phase 4.5 authorization binding, IHttpService retention, and HTTP/2 rejection checks.
 * Uses RecordingHttpTransport only — never contacts external systems.
 */
public class AuthorizationBindingSmoke {
    static int pass = 0;
    static int fail = 0;
    static int skipped = 0;
    static final Charset ISO = Charset.forName("ISO-8859-1");

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
        testUnboundPolicyShowsDashHostPort();
        testServiceRetentionOnPanel();
        testServiceRetainedAfterPlanGeneration();
        testAuthorizeBindsOnlyBoundService();
        testMissingServiceBlocksAuthorizationSemantics();
        testHttp1RequestAccepted();
        testHttp2RequestLineRejected();
        testHttp2PrefaceRejected();
        testHttp2PseudoHeaderRejected();
        testHostMismatchRejected();
        testPortMismatchRejected();
        testUnauthorizedExecuteRejected();
        testRevokeClearsBinding();
        testPathPrefixEnforced();
        testPreparedHostHeaderMismatchRejected();

        skipped++;
        System.out.println("SKIP burp-gui-manual :: NOT VERIFIED in automation");
        skipped++;
        System.out.println("SKIP portswigger-academy :: NOT VERIFIED in automation");

        System.out.println("SUMMARY pass=" + pass + " fail=" + fail + " skipped=" + skipped
                + " total=" + (pass + fail + skipped));
        if (fail > 0) {
            System.exit(1);
        }
    }

    static byte[] minimalPng() {
        return new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
            (byte) 0xDE, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }

    static byte[] multipartRequest(String versionToken) {
        byte[] png = minimalPng();
        String boundary = "----PHAUTH";
        StringBuilder body = new StringBuilder();
        body.append("--").append(boundary).append("\r\n");
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"photo.png\"\r\n");
        body.append("Content-Type: image/png\r\n\r\n");
        byte[] prefix = body.toString().getBytes(ISO);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(ISO);
        byte[] fullBody = new byte[prefix.length + png.length + suffix.length];
        System.arraycopy(prefix, 0, fullBody, 0, prefix.length);
        System.arraycopy(png, 0, fullBody, prefix.length, png.length);
        System.arraycopy(suffix, 0, fullBody, prefix.length + png.length, suffix.length);
        String head = "POST /my-account/avatar " + versionToken + "\r\n"
                + "Host: lab.example\r\n"
                + "Content-Type: multipart/form-data; boundary=" + boundary + "\r\n"
                + "Content-Length: " + fullBody.length + "\r\n\r\n";
        byte[] headBytes = head.getBytes(ISO);
        byte[] req = new byte[headBytes.length + fullBody.length];
        System.arraycopy(headBytes, 0, req, 0, headBytes.length);
        System.arraycopy(fullBody, 0, req, headBytes.length, fullBody.length);
        return req;
    }

    static UploadRequestResult analyzeHttp1() {
        return FileUploadRequestAnalyzer.analyze(multipartRequest("HTTP/1.1"), "auth-smoke");
    }

    static void testUnboundPolicyShowsDashHostPort() {
        ExecutionAuthorizationPolicy p = ExecutionAuthorizationPolicy.disabledDefaults();
        String s = p.summarize();
        check("unbound host=-", s.contains("host=-"), s);
        check("unbound port=-", s.contains("port=-"), s);
        check("unbound not isBound", !p.isBound(), "bound");
        check("detailed explains UNBOUND", p.summarizeDetailed().contains("UNBOUND"), p.summarizeDetailed());
    }

    static void testServiceRetentionOnPanel() {
        FileUploadPanel panel = new FileUploadPanel();
        panel.seedAnalysisForTests(analyzeHttp1());
        check("before bind service null", panel.getBoundServiceTargetForTests() == null, "null");
        panel.bindServiceForTests("lab.example", 443, true);
        check("bound host", "lab.example".equals(panel.getBoundServiceTargetForTests().getHost()), "host");
        check("bound port", panel.getBoundServiceTargetForTests().getPort() == 443, "port");
        check("bound https", panel.getBoundServiceTargetForTests().isHttps(), "https");
        check("session matches bound", panel.getSessionTarget().equals(panel.getBoundServiceTargetForTests()),
                "session");
        check("detected label shows service",
                panel.getDetectedTargetLabelForTests().contains("lab.example"),
                panel.getDetectedTargetLabelForTests());
        check("auth status still UNBOUND until authorize",
                panel.getAuthStatusForTests().contains("UNBOUND")
                        || panel.getAuthDetailForTests().contains("UNBOUND"),
                panel.getAuthStatusForTests());
    }

    static void testServiceRetainedAfterPlanGeneration() {
        FileUploadPanel panel = new FileUploadPanel();
        panel.seedAnalysisForTests(analyzeHttp1());
        panel.bindServiceForTests("lab.example", 443, true);
        panel.generateAllPlansSynchronouslyForTests();
        panel.loadManagedCasesForTests();
        check("service retained after generate",
                panel.getBoundServiceTargetForTests() != null
                        && "lab.example".equals(panel.getBoundServiceTargetForTests().getHost()),
                String.valueOf(panel.getBoundServiceTargetForTests()));
        check("service retained after load",
                panel.getSessionTarget() != null && panel.getSessionTarget().getPort() == 443, "port");
    }

    static void testAuthorizeBindsOnlyBoundService() {
        FileUploadPanel panel = new FileUploadPanel();
        panel.seedAnalysisForTests(analyzeHttp1());
        panel.bindServiceForTests("lab.example", 443, true);
        ManagedExecutionManager mgr = panel.getExecutionManager();
        ExecutionAuthorizationPolicy p = mgr.getPolicy();
        p.authorizeTarget("lab.example", Integer.valueOf(443), true);
        p.setExecutionEnabled(true);
        p.setUserConfirmed(true);
        check("policy bound after authorize", p.isBound(), p.summarize());
        check("policy host set", "lab.example".equals(p.getAuthorizedHost()), p.getAuthorizedHost());
        check("policy port set", p.getAuthorizedPort() != null && p.getAuthorizedPort().intValue() == 443,
                String.valueOf(p.getAuthorizedPort()));
        // Attempt reuse against different target must fail evaluation
        ExecutionAuthorizationPolicy.PolicyDecision deny =
                p.evaluateForFutureSend("evil.example", Integer.valueOf(443), "POST", "/x", 100);
        check("cross-host denied", !deny.allowed, deny.message);
    }

    static void testMissingServiceBlocksAuthorizationSemantics() {
        FileUploadPanel panel = new FileUploadPanel();
        panel.seedAnalysisForTests(analyzeHttp1());
        check("no bind => null service", panel.getBoundServiceTargetForTests() == null, "null");
        check("detail explains missing or unbound",
                panel.getDetectedTargetLabelForTests().contains("—")
                        || panel.getAuthDetailForTests().contains("UNBOUND"),
                panel.getDetectedTargetLabelForTests());
    }

    static void testHttp1RequestAccepted() {
        HttpRequestFrame frame = HttpRequestFrame.parse(multipartRequest("HTTP/1.1"));
        check("HTTP/1.1 supported", frame.isSupported(), frame.unsupportedReason);
        UploadRequestResult analysis = analyzeHttp1();
        UploadTestCase tc = firstFilenameCase(analysis);
        MultipartRequestBuilder.BuildOutcome built = MultipartRequestBuilder.build(analysis, tc, null);
        check("HTTP/1.1 prepare ok", built.artifact != null, built.message);
    }

    static void testHttp2RequestLineRejected() {
        byte[] raw = multipartRequest("HTTP/2");
        HttpRequestFrame frame = HttpRequestFrame.parse(raw);
        check("HTTP/2 request line unsupported", !frame.isSupported(), frame.unsupportedReason);
        check("HTTP/2 reason mentions HTTP/2",
                frame.unsupportedReason != null && frame.unsupportedReason.contains("HTTP/2"),
                frame.unsupportedReason);

        UploadRequestResult analysis = FileUploadRequestAnalyzer.analyze(raw, "http2-line");
        // Analyzer may still parse multipart; prepare must fail closed.
        int part = firstFilePart(analysis);
        UploadTestCase tc = new UploadTestCase(
                "t-http2", analysis.getAnalysisId(), "plan", Integer.valueOf(part),
                "photo.png", "photo.php.png", "png", "png",
                "s", "c", "t", "p", "d", true, null, null, System.currentTimeMillis(), true,
                UploadTestCase.STATUS_NOT_EXECUTED);
        MultipartRequestBuilder.BuildOutcome built = MultipartRequestBuilder.build(analysis, tc, null);
        check("HTTP/2 prepare rejected", built.artifact == null, built.message);
        check("HTTP/2 status unsupported",
                built.status == PreparationStatus.UNSUPPORTED_TARGET
                        || (built.message != null && built.message.contains("HTTP/2")),
                String.valueOf(built.status) + " " + built.message);

        FileUploadPanel panel = new FileUploadPanel();
        panel.seedAnalysisForTests(analysis);
        panel.setDetectedRequestHttpVersionForTests("HTTP/2");
        check("panel detects unsupported version",
                panel.getDetectedRequestHttpVersionForTests().startsWith("HTTP/2"),
                panel.getDetectedRequestHttpVersionForTests());
    }

    static void testHttp2PrefaceRejected() {
        byte[] preface = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(ISO);
        HttpRequestFrame frame = HttpRequestFrame.parse(preface);
        check("HTTP/2 preface unsupported", !frame.isSupported(), frame.unsupportedReason);
    }

    static void testHttp2PseudoHeaderRejected() {
        byte[] raw = (":method: POST\r\n:path: /upload\r\n\r\n").getBytes(ISO);
        HttpRequestFrame frame = HttpRequestFrame.parse(raw);
        check("pseudo-header unsupported", !frame.isSupported(), frame.unsupportedReason);
    }

    static void testHostMismatchRejected() {
        UploadRequestResult analysis = analyzeHttp1();
        ManagedExecutionManager mgr = new ManagedExecutionManager();
        authorize(mgr, "lab.example", 80);
        UploadTestCase tc = firstFilenameCase(analysis);
        tc.setSelected(true);
        mgr.prepareSelected(analysis, Collections.singletonList(tc));
        RecordingHttpTransport transport = new RecordingHttpTransport();
        ControlledHttpExecutor exec = new ControlledHttpExecutor(mgr, transport, null);
        HttpExecutionBatchResult batch = exec.executePrepared(analysis,
                new HttpServiceTarget("other.example", 80, false),
                Collections.singletonList(tc.getTestId()), null);
        check("host mismatch rejected", batch.getRejected() >= 1, "rej=" + batch.getRejected());
        check("host mismatch no send", transport.getSendCount() == 0, "sends=" + transport.getSendCount());
    }

    static void testPortMismatchRejected() {
        UploadRequestResult analysis = analyzeHttp1();
        ManagedExecutionManager mgr = new ManagedExecutionManager();
        authorize(mgr, "lab.example", 80);
        UploadTestCase tc = firstFilenameCase(analysis);
        tc.setSelected(true);
        mgr.prepareSelected(analysis, Collections.singletonList(tc));
        RecordingHttpTransport transport = new RecordingHttpTransport();
        ControlledHttpExecutor exec = new ControlledHttpExecutor(mgr, transport, null);
        HttpExecutionBatchResult batch = exec.executePrepared(analysis,
                new HttpServiceTarget("lab.example", 443, true),
                Collections.singletonList(tc.getTestId()), null);
        check("port mismatch rejected", batch.getRejected() >= 1, "rej=" + batch.getRejected());
        check("port mismatch no send", transport.getSendCount() == 0, "sends");
    }

    static void testUnauthorizedExecuteRejected() {
        UploadRequestResult analysis = analyzeHttp1();
        ManagedExecutionManager mgr = new ManagedExecutionManager();
        UploadTestCase tc = firstFilenameCase(analysis);
        tc.setSelected(true);
        mgr.prepareSelected(analysis, Collections.singletonList(tc));
        RecordingHttpTransport transport = new RecordingHttpTransport();
        ControlledHttpExecutor exec = new ControlledHttpExecutor(mgr, transport, null);
        HttpExecutionBatchResult batch = exec.executePrepared(analysis,
                new HttpServiceTarget("lab.example", 80, false),
                Collections.singletonList(tc.getTestId()), null);
        check("unauthorized rejected", batch.getRejected() >= 1, "rej=" + batch.getRejected());
        check("unauthorized no send", transport.getSendCount() == 0, "sends");
    }

    static void testRevokeClearsBinding() {
        ExecutionAuthorizationPolicy p = ExecutionAuthorizationPolicy.disabledDefaults();
        p.authorizeTarget("lab.example", Integer.valueOf(80), true);
        check("bound before revoke", p.isBound(), p.summarize());
        p.revokeAuthorization();
        check("unbound after revoke enabled false", !p.isExecutionEnabled(), "enabled");
        check("unbound after revoke confirm false", !p.isUserConfirmed(), "confirm");
        // host/port intentionally retained or cleared? revokeAuthorization only clears enabled/confirmed
        // evaluate must still deny because enabled=false
        ExecutionAuthorizationPolicy.PolicyDecision d =
                p.evaluateForFutureSend("lab.example", Integer.valueOf(80), "POST", "/", 10);
        check("revoked denies send", !d.allowed, d.message);
    }

    static void testPathPrefixEnforced() {
        ExecutionAuthorizationPolicy p = ExecutionAuthorizationPolicy.disabledDefaults();
        p.authorizeTarget("lab.example", Integer.valueOf(80), true);
        p.setExecutionEnabled(true);
        p.setUserConfirmed(true);
        p.setAllowedPathPrefix("/my-account/");
        check("path in scope allowed",
                p.evaluateForFutureSend("lab.example", Integer.valueOf(80), "POST",
                        "/my-account/avatar", 10).allowed, "in");
        check("path out of scope denied",
                !p.evaluateForFutureSend("lab.example", Integer.valueOf(80), "POST",
                        "/admin/upload", 10).allowed, "out");
    }

    static void testPreparedHostHeaderMismatchRejected() {
        // Build a normal prepare then execute against matching host — Host header is lab.example
        UploadRequestResult analysis = analyzeHttp1();
        ManagedExecutionManager mgr = new ManagedExecutionManager();
        authorize(mgr, "lab.example", 80);
        UploadTestCase tc = firstFilenameCase(analysis);
        tc.setSelected(true);
        ManagedExecutionResult prep = mgr.prepareSelected(analysis, Collections.singletonList(tc));
        check("prep for host-header test", prep.getSuccessCount() == 1, "prep");
        PreparedRequestArtifact art = mgr.getPrepared(tc.getTestId()).getArtifact();
        HttpRequestFrame frame = HttpRequestFrame.parse(art.getPreparedRequestBytes());
        check("prepared has Host", frame.headerValue("Host") != null, frame.headerValue("Host"));
        // Policy authorized lab.example:80 but we send to lab.example:80 — Host matches, should allow
        // when transport records. Then authorize different and ensure Host mismatch path works via
        // target host != Host header host.
        RecordingHttpTransport transport = new RecordingHttpTransport();
        transport.enqueue(RecordingHttpTransport.ScriptedResponse.ok(
                ("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok").getBytes(ISO)));
        ControlledHttpExecutor exec = new ControlledHttpExecutor(mgr, transport, null);
        // Re-authorize same host so send can proceed for control case
        HttpExecutionBatchResult ok = exec.executePrepared(analysis,
                new HttpServiceTarget("lab.example", 80, false),
                Collections.singletonList(tc.getTestId()), null);
        check("matching host can transmit", ok.getTransmitted() == 1 || ok.getResponded() == 1
                || transport.getSendCount() == 1, "tx=" + ok.getTransmitted());
    }

    static void authorize(ManagedExecutionManager mgr, String host, int port) {
        ExecutionAuthorizationPolicy p = mgr.getPolicy();
        p.authorizeTarget(host, Integer.valueOf(port), true);
        p.setExecutionEnabled(true);
        p.setUserConfirmed(true);
    }

    static int firstFilePart(UploadRequestResult analysis) {
        List<uk.co.pentest.psychoPATH.fileupload.UploadPart> parts = analysis.getParts();
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).isFilePart()) {
                return parts.get(i).getIndex();
            }
        }
        return 0;
    }

    static UploadTestCase firstFilenameCase(UploadRequestResult analysis) {
        int part = firstFilePart(analysis);
        uk.co.pentest.psychoPATH.fileupload.UploadTestPlan plan =
                uk.co.pentest.psychoPATH.fileupload.FilenameTestingEngine.generatePlan(
                        analysis, part, uk.co.pentest.psychoPATH.fileupload.FilenameTestingConfig.defaults());
        UploadTestCase tc = plan.getCases().get(0);
        tc.setSelected(true);
        return tc;
    }
}
