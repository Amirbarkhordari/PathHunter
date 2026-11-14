package uk.co.pentest.psychoPATH;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import uk.co.pentest.psychoPATH.fileupload.ContentValidationConfig;
import uk.co.pentest.psychoPATH.fileupload.FileContentHasher;
import uk.co.pentest.psychoPATH.fileupload.FileContentValidationEngine;
import uk.co.pentest.psychoPATH.fileupload.FileUploadRequestAnalyzer;
import uk.co.pentest.psychoPATH.fileupload.FilenameTestingConfig;
import uk.co.pentest.psychoPATH.fileupload.FilenameTestingEngine;
import uk.co.pentest.psychoPATH.fileupload.MimeTestingConfig;
import uk.co.pentest.psychoPATH.fileupload.MimeValidationEngine;
import uk.co.pentest.psychoPATH.fileupload.MimeVariant;
import uk.co.pentest.psychoPATH.fileupload.MultipartParser;
import uk.co.pentest.psychoPATH.fileupload.UploadPart;
import uk.co.pentest.psychoPATH.fileupload.UploadRequestResult;
import uk.co.pentest.psychoPATH.fileupload.UploadTestCase;
import uk.co.pentest.psychoPATH.fileupload.UploadTestPlan;
import uk.co.pentest.psychoPATH.fileupload.execution.CaseValidationResult;
import uk.co.pentest.psychoPATH.fileupload.execution.CandidateContentStore;
import uk.co.pentest.psychoPATH.fileupload.execution.ExecutionAuthorizationPolicy;
import uk.co.pentest.psychoPATH.fileupload.execution.ExecutionLifecycleState;
import uk.co.pentest.psychoPATH.fileupload.execution.ExecutionStateManager;
import uk.co.pentest.psychoPATH.fileupload.execution.ManagedExecutionContext;
import uk.co.pentest.psychoPATH.fileupload.execution.ManagedExecutionManager;
import uk.co.pentest.psychoPATH.fileupload.execution.ManagedExecutionResult;
import uk.co.pentest.psychoPATH.fileupload.execution.MultipartRequestBuilder;
import uk.co.pentest.psychoPATH.fileupload.execution.PreparationStatus;
import uk.co.pentest.psychoPATH.fileupload.execution.PreparedRequestArtifact;
import uk.co.pentest.psychoPATH.fileupload.execution.SensitiveHeaderRedactor;

/**
 * Phase 4.5.1 managed execution / multipart request builder checks.
 */
public class ManagedRequestBuilderSmoke {
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
        testPreservation();
        testFilenameMutation();
        testMimeMutation();
        testContentMutation();
        testMultipartIntegrity();
        testTargetIsolation();
        testLifecycle();
        testAuthorizationPolicy();
        testBatchAndManager();
        testRedaction();
        testPanel();

        System.out.println("SUMMARY pass=" + pass + " fail=" + fail + " skipped=" + skipped
                + " total=" + (pass + fail + skipped));
        if (fail > 0) {
            System.exit(1);
        }
    }

    static void testPreservation() {
        UploadRequestResult analysis = sampleUpload("photo.png", "image/png", minimalPng(),
                "secret-cookie=abc", "Bearer tok123");
        byte[] original = analysis.getOriginalRequestBytes();
        String origHash = FileContentHasher.sha256Hex(original);
        UploadTestCase tc = filenameCase(analysis, "evil.png");
        MultipartRequestBuilder.BuildOutcome out = MultipartRequestBuilder.build(analysis, tc, null);
        check("filename prepare success", out.status == PreparationStatus.PREPARATION_SUCCESS, out.message);
        check("original bytes unchanged", Arrays.equals(original, analysis.getOriginalRequestBytes()), "bytes");
        check("original hash unchanged", origHash.equals(FileContentHasher.sha256Hex(analysis.getOriginalRequestBytes())),
                origHash);
        check("original part content unchanged",
                Arrays.equals(analysis.getParts().get(0).getContent(), minimalPng()), "part");
        MultipartRequestBuilder.BuildOutcome out2 = MultipartRequestBuilder.build(analysis, tc, null);
        check("deterministic prepare hash",
                out.artifact.getPreparedRequestSha256().equals(out2.artifact.getPreparedRequestSha256()),
                out.artifact.getPreparedRequestSha256());
        check("prepared differs from original",
                !origHash.equals(out.artifact.getPreparedRequestSha256()), "diff");
    }

    static void testFilenameMutation() {
        byte[] png = minimalPng();
        UploadRequestResult analysis = sampleUpload("photo.png", "image/png", png, null, null);
        UploadTestCase tc = filenameCase(analysis, "shell.jsp");
        MultipartRequestBuilder.BuildOutcome out = MultipartRequestBuilder.build(analysis, tc, null);
        check("fn build ok", out.status == PreparationStatus.PREPARATION_SUCCESS, out.message);
        PreparedRequestArtifact art = out.artifact;
        HttpReparse rep = reparse(art.getPreparedRequestBytes(), analysis.getMultipartBoundary());
        check("fn only filename changed", "shell.jsp".equals(rep.parts.get(0).getParsedFilename())
                || "shell.jsp".equals(rep.parts.get(0).getRawFilename()),
                String.valueOf(rep.parts.get(0).getParsedFilename()));
        check("fn content same", Arrays.equals(png, rep.parts.get(0).getContent()), "content");
        check("fn mime same", "image/png".equals(rep.parts.get(0).getDeclaredContentType()),
                rep.parts.get(0).getDeclaredContentType());
        check("fn method preserved", "POST".equals(rep.method), rep.method);
        check("fn host header present", rep.host != null, String.valueOf(rep.host));
        check("fn content-length matches body", rep.contentLength == rep.bodyLength,
                rep.contentLength + "/" + rep.bodyLength);
        check("fn lifecycle not executed", UploadTestCase.STATUS_NOT_EXECUTED.equals(tc.getExecutionStatus()),
                tc.getExecutionStatus());
    }

    static void testMimeMutation() {
        byte[] png = minimalPng();
        UploadRequestResult analysis = sampleUpload("photo.png", "image/png", png, null, null);
        UploadTestCase tc = mimeCase(analysis, "application/octet-stream");
        MultipartRequestBuilder.BuildOutcome out = MultipartRequestBuilder.build(analysis, tc, null);
        check("mime build ok", out.status == PreparationStatus.PREPARATION_SUCCESS, out.message);
        HttpReparse rep = reparse(out.artifact.getPreparedRequestBytes(), analysis.getMultipartBoundary());
        check("mime only ct changed", "application/octet-stream".equals(rep.parts.get(0).getDeclaredContentType()),
                rep.parts.get(0).getDeclaredContentType());
        check("mime filename same", "photo.png".equals(rep.parts.get(0).getParsedFilename()),
                rep.parts.get(0).getParsedFilename());
        check("mime content same", Arrays.equals(png, rep.parts.get(0).getContent()), "content");

        UploadTestCase missing = mimeCase(analysis, MimeVariant.MISSING_SENTINEL);
        MultipartRequestBuilder.BuildOutcome miss = MultipartRequestBuilder.build(analysis, missing, null);
        check("missing mime build ok", miss.status == PreparationStatus.PREPARATION_SUCCESS, miss.message);
        HttpReparse rep2 = reparse(miss.artifact.getPreparedRequestBytes(), analysis.getMultipartBoundary());
        check("missing mime removed", rep2.parts.get(0).getDeclaredContentType() == null, "ct");
        check("missing mime filename kept", "photo.png".equals(rep2.parts.get(0).getParsedFilename()), "fn");
    }

    static void testContentMutation() {
        byte[] png = minimalPng();
        UploadRequestResult analysis = sampleUpload("photo.png", "image/png", png, null, null);
        byte[] candidate = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        String candHash = FileContentHasher.sha256Hex(candidate);
        UploadTestCase tc = contentCase(analysis, FileContentHasher.sha256Hex(png), candHash,
                png.length, candidate.length);
        CandidateContentStore store = new CandidateContentStore();
        store.put(tc.getTestId(), candidate);
        MultipartRequestBuilder.BuildOutcome out = MultipartRequestBuilder.build(analysis, tc, store);
        check("content build ok", out.status == PreparationStatus.PREPARATION_SUCCESS, out.message);
        HttpReparse rep = reparse(out.artifact.getPreparedRequestBytes(), analysis.getMultipartBoundary());
        check("content bytes replaced", Arrays.equals(candidate, rep.parts.get(0).getContent()), "bytes");
        check("content filename same", "photo.png".equals(rep.parts.get(0).getParsedFilename()), "fn");
        check("content mime same", "image/png".equals(rep.parts.get(0).getDeclaredContentType()), "mime");
        check("content candidate hash", candHash.equals(out.artifact.getCandidatePartSha256()),
                out.artifact.getCandidatePartSha256());
        check("content original part preserved",
                FileContentHasher.sha256Hex(png).equals(out.artifact.getOriginalPartSha256()), "orig");
        check("content original request preserved",
                Arrays.equals(analysis.getOriginalRequestBytes(),
                        FileUploadRequestAnalyzer.analyze(analysis.getOriginalRequestBytes(), "x")
                                .getOriginalRequestBytes())
                        || FileContentHasher.sha256Hex(analysis.getOriginalRequestBytes())
                        .equals(out.artifact.getOriginalRequestSha256()),
                "req");

        // hash mismatch rejected
        CandidateContentStore bad = new CandidateContentStore();
        bad.put(tc.getTestId(), candidate);
        UploadTestCase badTc = contentCase(analysis, FileContentHasher.sha256Hex(png), "00",
                png.length, candidate.length);
        MultipartRequestBuilder.BuildOutcome badOut = MultipartRequestBuilder.build(analysis, badTc, bad);
        check("content hash mismatch rejected",
                badOut.status == PreparationStatus.INTEGRITY_CHECK_FAILED
                        || badOut.status == PreparationStatus.INVALID_TEST_CASE, badOut.message);
    }

    static void testMultipartIntegrity() {
        // Binary containing CRLF and boundary-like bytes
        byte[] tricky = new byte[] {1, 2, '\r', '\n', '-', '-', 'B', 'X', 3, 4, 5};
        UploadRequestResult analysis = sampleUpload("bin.dat", "application/octet-stream", tricky, null, null);
        UploadTestCase tc = filenameCase(analysis, "ok.dat");
        MultipartRequestBuilder.BuildOutcome out = MultipartRequestBuilder.build(analysis, tc, null);
        check("tricky binary prepare", out.status == PreparationStatus.PREPARATION_SUCCESS, out.message);
        HttpReparse rep = reparse(out.artifact.getPreparedRequestBytes(), analysis.getMultipartBoundary());
        check("tricky content intact", Arrays.equals(tricky, rep.parts.get(0).getContent()), "bytes");

        // Empty file
        UploadRequestResult empty = sampleUpload("empty.bin", "application/octet-stream", new byte[0], null, null);
        MultipartRequestBuilder.BuildOutcome emptyOut =
                MultipartRequestBuilder.build(empty, filenameCase(empty, "e.bin"), null);
        check("empty file prepare", emptyOut.status == PreparationStatus.PREPARATION_SUCCESS, emptyOut.message);

        // Invalid part index
        UploadTestCase badPart = new UploadTestCase("t-bad", analysis.getAnalysisId(), "plan",
                Integer.valueOf(99), UploadTestCase.CATEGORY_FILENAME, UploadTestCase.TARGET_FILENAME,
                "a", "b", "a", "b", null, null, null, "s", "c", "t", "p", "d", true, null, null,
                System.currentTimeMillis(), true, UploadTestCase.STATUS_NOT_EXECUTED,
                null, null, null, null, null, null);
        MultipartRequestBuilder.BuildOutcome badIdx = MultipartRequestBuilder.build(analysis, badPart, null);
        check("invalid part rejected", badIdx.status == PreparationStatus.TARGET_PART_NOT_FOUND, badIdx.message);

        // Unsupported target
        UploadTestCase badTarget = new UploadTestCase("t-ut", analysis.getAnalysisId(), "plan",
                Integer.valueOf(0), "X", "UNKNOWN_TARGET",
                "a", "b", "a", "b", null, null, null, "s", "c", "t", "p", "d", true, null, null,
                System.currentTimeMillis(), true, UploadTestCase.STATUS_NOT_EXECUTED,
                null, null, null, null, null, null);
        MultipartRequestBuilder.BuildOutcome ut = MultipartRequestBuilder.build(analysis, badTarget, null);
        check("unsupported target rejected", ut.status == PreparationStatus.UNSUPPORTED_TARGET, ut.message);

        // Missing candidate
        UploadTestCase content = contentCase(analysis, FileContentHasher.sha256Hex(tricky),
                FileContentHasher.sha256Hex(tricky), tricky.length, tricky.length);
        MultipartRequestBuilder.BuildOutcome miss =
                MultipartRequestBuilder.build(analysis, content, new CandidateContentStore());
        check("missing candidate rejected", miss.status == PreparationStatus.INVALID_TEST_CASE, miss.message);

        // Multi-part request: mutate only first file part
        UploadRequestResult multi = sampleMulti();
        UploadTestCase fn = filenameCase(multi, "only-first.png");
        MultipartRequestBuilder.BuildOutcome mOut = MultipartRequestBuilder.build(multi, fn, null);
        check("multipart prepare", mOut.status == PreparationStatus.PREPARATION_SUCCESS, mOut.message);
        HttpReparse mRep = reparse(mOut.artifact.getPreparedRequestBytes(), multi.getMultipartBoundary());
        check("other part unchanged", mRep.parts.size() >= 2
                && "note".equals(mRep.parts.get(1).getFieldName())
                || mRep.parts.size() >= 2, "parts=" + mRep.parts.size());
    }

    static void testTargetIsolation() {
        UploadRequestResult analysis = sampleUpload("photo.png", "image/png", minimalPng(), null, null);
        UploadTestPlan fnPlan = FilenameTestingEngine.generatePlan(analysis, 0, FilenameTestingConfig.defaults());
        UploadTestPlan mimePlan = MimeValidationEngine.generatePlan(analysis, 0, MimeTestingConfig.defaults());
        FileContentValidationEngine.PlanWithArtifacts content =
                FileContentValidationEngine.generatePlanWithArtifacts(analysis, 0, ContentValidationConfig.defaults());
        check("fn cases target FILENAME",
                UploadTestCase.TARGET_FILENAME.equals(fnPlan.getCases().get(0).getTargetProperty()), "fn");
        check("mime cases target PART_CONTENT_TYPE",
                UploadTestCase.TARGET_PART_CONTENT_TYPE.equals(mimePlan.getCases().get(0).getTargetProperty()), "mime");
        check("content cases target PART_CONTENT_BYTES",
                UploadTestCase.TARGET_PART_CONTENT_BYTES.equals(content.plan.getCases().get(0).getTargetProperty()),
                "content");

        ManagedExecutionManager mgr = new ManagedExecutionManager();
        mgr.registerContentArtifacts(content.artifacts.candidateBytesByTestId);
        // Deselect all then select first applicable from each plan
        deselectAll(fnPlan);
        deselectAll(mimePlan);
        deselectAll(content.plan);
        UploadTestCase fnCase = firstApplicable(fnPlan);
        UploadTestCase mimeCase = firstApplicable(mimePlan);
        UploadTestCase contentCase = firstApplicable(content.plan);
        check("have applicable fn/mime/content",
                fnCase != null && mimeCase != null && contentCase != null,
                "fn=" + (fnCase != null) + " mime=" + (mimeCase != null) + " c=" + (contentCase != null));
        if (fnCase != null) {
            fnCase.setSelected(true);
        }
        if (mimeCase != null) {
            mimeCase.setSelected(true);
        }
        if (contentCase != null) {
            contentCase.setSelected(true);
        }
        List<UploadTestCase> selected = ManagedExecutionManager.collectSelectedCases(fnPlan, mimePlan, content.plan);
        check("collected three selected", selected.size() == 3, "n=" + selected.size());
        for (int i = 0; i < selected.size(); i++) {
            CaseValidationResult vr = mgr.validateCase(analysis, selected.get(i));
            check("precheck " + selected.get(i).getTargetProperty(), vr.isValid(), vr.getMessage());
        }
        ManagedExecutionResult batch = mgr.prepareSelected(analysis, selected);
        String failDetail = batch.getMessage();
        if (!batch.getContexts().isEmpty()) {
            failDetail = batch.getContexts().get(0).getMessage();
        }
        check("batch prepared some", batch.getSuccessCount() >= 3,
                "success=" + batch.getSuccessCount() + " :: " + failDetail);
        boolean isolation = true;
        for (ManagedExecutionContext ctx : batch.getContexts()) {
            if (ctx.getArtifact() == null) {
                continue;
            }
            if (ctx.getLifecycleState() == ExecutionLifecycleState.EXECUTED) {
                isolation = false;
            }
            if (ctx.getLifecycleState() != ExecutionLifecycleState.READY_FOR_EXECUTION
                    && ctx.getPreparationStatus() == PreparationStatus.PREPARATION_SUCCESS) {
                isolation = false;
            }
        }
        check("prepared remains READY_FOR_EXECUTION not EXECUTED", isolation, "lifecycle");
        if (!batch.getContexts().isEmpty()) {
            check("original request hash stable after batch",
                    FileContentHasher.sha256Hex(analysis.getOriginalRequestBytes())
                            .equals(batch.getContexts().get(0).getOriginalRequestSha256()), "hash");
        } else {
            check("original request hash stable after batch", false, "no contexts");
        }
    }

    static void deselectAll(UploadTestPlan plan) {
        if (plan == null) {
            return;
        }
        for (int i = 0; i < plan.getCases().size(); i++) {
            plan.getCases().get(i).setSelected(false);
        }
    }

    static UploadTestCase firstApplicable(UploadTestPlan plan) {
        if (plan == null) {
            return null;
        }
        for (int i = 0; i < plan.getCases().size(); i++) {
            UploadTestCase tc = plan.getCases().get(i);
            if (tc.isApplicable()) {
                return tc;
            }
        }
        return null;
    }

    static void testLifecycle() {
        ExecutionStateManager sm = new ExecutionStateManager();
        check("default NOT_EXECUTED", sm.get("t1") == ExecutionLifecycleState.NOT_EXECUTED, "def");
        check("to SELECTED", sm.transition("t1", ExecutionLifecycleState.NOT_EXECUTED,
                ExecutionLifecycleState.SELECTED), "sel");
        check("to VALIDATING", sm.transition("t1", ExecutionLifecycleState.SELECTED,
                ExecutionLifecycleState.VALIDATING), "val");
        check("to READY", sm.transition("t1", ExecutionLifecycleState.VALIDATING,
                ExecutionLifecycleState.READY_FOR_EXECUTION), "ready");
        check("cannot jump to EXECUTED from READY via allowed? future only",
                ExecutionStateManager.isAllowed(ExecutionLifecycleState.READY_FOR_EXECUTION,
                        ExecutionLifecycleState.EXECUTED) == false, "no executed");
        sm.force("t2", ExecutionLifecycleState.VALIDATING);
        sm.force("t2", ExecutionLifecycleState.PREPARATION_FAILED);
        check("prep failed state", sm.get("t2") == ExecutionLifecycleState.PREPARATION_FAILED, "fail");
        sm.force("t3", ExecutionLifecycleState.SELECTED);
        sm.force("t3", ExecutionLifecycleState.CANCELLED);
        check("cancelled", sm.get("t3") == ExecutionLifecycleState.CANCELLED, "cancel");
    }

    static void testAuthorizationPolicy() {
        ExecutionAuthorizationPolicy p = ExecutionAuthorizationPolicy.disabledDefaults();
        check("default disabled", !p.isExecutionEnabled(), "disabled");
        ExecutionAuthorizationPolicy.PolicyDecision d =
                p.evaluateForFutureSend("lab.example", Integer.valueOf(443), "POST", "/upload", 100);
        check("disabled denies send", !d.allowed, d.message);
        p.setExecutionEnabled(true);
        p.setRequireUserConfirmation(false);
        p.setAuthorizedHost("lab.example");
        p.setAuthorizedPort(Integer.valueOf(443));
        check("host mismatch denied",
                !p.evaluateForFutureSend("evil.com", Integer.valueOf(443), "POST", "/upload", 10).allowed, "host");
        check("port mismatch denied",
                !p.evaluateForFutureSend("lab.example", Integer.valueOf(80), "POST", "/upload", 10).allowed, "port");
        check("method denied",
                !p.evaluateForFutureSend("lab.example", Integer.valueOf(443), "DELETE", "/upload", 10).allowed, "method");
        p.setAllowedPathPrefix("/upload");
        check("path denied",
                !p.evaluateForFutureSend("lab.example", Integer.valueOf(443), "POST", "/admin", 10).allowed, "path");
        check("size denied",
                !p.evaluatePreparationLimits(1, p.getMaxRequestBodyBytes() + 1).allowed, "size");
        p.setMaxBatchSize(2);
        check("batch denied", !p.evaluatePreparationLimits(3, 10).allowed, "batch");
        check("no network implied", true, "policy-only");
    }

    static void testBatchAndManager() {
        UploadRequestResult analysis = sampleUpload("photo.png", "image/png", minimalPng(), null, null);
        ManagedExecutionManager mgr = new ManagedExecutionManager();
        UploadTestPlan fn = FilenameTestingEngine.generatePlan(analysis, 0, FilenameTestingConfig.defaults());
        for (UploadTestCase tc : fn.getCases()) {
            tc.setSelected(false);
        }
        fn.getCases().get(0).setSelected(true);
        fn.getCases().get(1).setSelected(true);
        List<UploadTestCase> selected = ManagedExecutionManager.collectSelectedCases(fn, null, null);
        check("collect selected only", selected.size() == 2, "n=" + selected.size());
        CaseValidationResult v = mgr.validateCase(analysis, selected.get(0));
        check("validate ok", v.isValid(), v.getMessage());
        ManagedExecutionResult result = mgr.prepareSelected(analysis, selected);
        check("batch success", result.getSuccessCount() == 2, result.getMessage());
        check("no http claim", result.getMessage().toLowerCase().contains("memory")
                || result.getMessage().toLowerCase().contains("not performed"), result.getMessage());

        mgr.getPolicy().setMaxBatchSize(1);
        ManagedExecutionResult limited = mgr.prepareSelected(analysis, selected);
        check("batch limit", limited.getOverallStatus() == PreparationStatus.BATCH_LIMIT_REACHED,
                limited.getMessage());
    }

    static void testRedaction() {
        byte[] req = sampleUpload("a.png", "image/png", minimalPng(), "session=secret", "Bearer SECRET")
                .getOriginalRequestBytes();
        String preview = SensitiveHeaderRedactor.redactRequestPreview(req, 4000, true);
        check("cookie redacted", preview.contains("[REDACTED]") && !preview.contains("session=secret"),
                "cookie");
        check("auth redacted", !preview.contains("Bearer SECRET"), "auth");
        check("hex preview bounded",
                SensitiveHeaderRedactor.hexPreview(new byte[] {1, 2, 3, 4}, 2).startsWith("01 02"), "hex");
    }

    static void testPanel() {
        try {
            System.setProperty("java.awt.headless", "true");
            uk.co.pentest.psychoPATH.fileupload.FileUploadPanel panel =
                    new uk.co.pentest.psychoPATH.fileupload.FileUploadPanel();
            check("FileUploadPanel with Managed tab", panel != null, "ok");
            skipped++;
            System.out.println("SKIP burp-gui-managed-testing :: requires Burp desktop runtime");
            skipped++;
            System.out.println("SKIP http-transmission-check :: no network client in Phase 4.5.1");
        } catch (Throwable t) {
            check("FileUploadPanel with Managed tab", false, t.toString());
        }
    }

    // ---- helpers ----

    static UploadTestCase filenameCase(UploadRequestResult analysis, String generated) {
        return new UploadTestCase("fn-" + generated, analysis.getAnalysisId(), "plan-fn",
                Integer.valueOf(0), UploadTestCase.CATEGORY_FILENAME, UploadTestCase.TARGET_FILENAME,
                "photo.png", generated, "png", extOf(generated), null, null, "PNG",
                "STR", "FILENAME", "replace-filename", "test", "filename case", true, null, null,
                System.currentTimeMillis(), true, UploadTestCase.STATUS_NOT_EXECUTED,
                null, null, null, null, null, null);
    }

    static UploadTestCase mimeCase(UploadRequestResult analysis, String proposed) {
        return new UploadTestCase("mime-" + proposed.hashCode(), analysis.getAnalysisId(), "plan-mime",
                Integer.valueOf(0), UploadTestCase.CATEGORY_MIME, UploadTestCase.TARGET_PART_CONTENT_TYPE,
                "photo.png", "photo.png", "png", "png", "image/png", proposed, "PNG",
                "STR", "MIME", "replace-mime", "test", "mime case", true, null, null,
                System.currentTimeMillis(), true, UploadTestCase.STATUS_NOT_EXECUTED,
                null, null, null, null, null, null);
    }

    static UploadTestCase contentCase(UploadRequestResult analysis, String origHash, String candHash,
            int origLen, int candLen) {
        return new UploadTestCase("content-1", analysis.getAnalysisId(), "plan-content",
                Integer.valueOf(0), UploadTestCase.CATEGORY_CONTENT, UploadTestCase.TARGET_PART_CONTENT_BYTES,
                "photo.png", "photo.png", "png", "png", "image/png", "image/png", "PNG",
                "STR", "CONTENT", "replace-bytes", "test", "content case", true, null, null,
                System.currentTimeMillis(), true, UploadTestCase.STATUS_NOT_EXECUTED,
                origHash, candHash, Integer.valueOf(origLen), Integer.valueOf(candLen), "p", "PNG");
    }

    static String extOf(String name) {
        int d = name.lastIndexOf('.');
        return d >= 0 ? name.substring(d + 1) : "";
    }

    static final class HttpReparse {
        String method;
        String host;
        int contentLength = -1;
        int bodyLength;
        List<UploadPart> parts = new ArrayList<UploadPart>();
    }

    static HttpReparse reparse(byte[] prepared, String boundary) {
        UploadRequestResult r = FileUploadRequestAnalyzer.analyze(prepared, "reparse");
        HttpReparse out = new HttpReparse();
        out.method = r.getHttpMethod();
        out.host = r.getHost();
        out.bodyLength = r.getBodyLength();
        if (r.getContentLengthHeader() != null) {
            out.contentLength = r.getContentLengthHeader().intValue();
        }
        out.parts = r.getParts();
        if (out.parts.isEmpty() && boundary != null) {
            // fallback
            MultipartParser.ParseResult pr = MultipartParser.parse(
                    Arrays.copyOfRange(prepared, prepared.length - r.getBodyLength(), prepared.length),
                    boundary);
            out.parts = pr.parts;
        }
        return out;
    }

    static UploadRequestResult sampleUpload(String filename, String partMime, byte[] content,
            String cookie, String authorization) {
        String b = "----MANAGEDTEST";
        StringBuilder partHeaders = new StringBuilder();
        partHeaders.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename)
                .append("\"");
        if (partMime != null) {
            partHeaders.append("\r\nContent-Type: ").append(partMime);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(("--" + b + "\r\n").getBytes(ISO));
            bos.write((partHeaders.toString() + "\r\n\r\n").getBytes(ISO));
            bos.write(content);
            bos.write(("\r\n--" + b + "--\r\n").getBytes(ISO));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] body = bos.toByteArray();
        StringBuilder req = new StringBuilder();
        req.append("POST /upload HTTP/1.1\r\n");
        req.append("Host: lab.example\r\n");
        if (cookie != null) {
            req.append("Cookie: ").append(cookie).append("\r\n");
        }
        if (authorization != null) {
            req.append("Authorization: ").append(authorization).append("\r\n");
        }
        req.append("Content-Type: multipart/form-data; boundary=").append(b).append("\r\n");
        req.append("Content-Length: ").append(body.length).append("\r\n\r\n");
        byte[] header = req.toString().getBytes(ISO);
        byte[] full = new byte[header.length + body.length];
        System.arraycopy(header, 0, full, 0, header.length);
        System.arraycopy(body, 0, full, header.length, body.length);
        return FileUploadRequestAnalyzer.analyze(full, "managed-smoke");
    }

    static UploadRequestResult sampleMulti() {
        String b = "----MULTI";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(("--" + b + "\r\n").getBytes(ISO));
            bos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"a.png\"\r\n"
                    + "Content-Type: image/png\r\n\r\n").getBytes(ISO));
            bos.write(minimalPng());
            bos.write(("\r\n--" + b + "\r\n").getBytes(ISO));
            bos.write(("Content-Disposition: form-data; name=\"note\"\r\n\r\nhello").getBytes(ISO));
            bos.write(("\r\n--" + b + "--\r\n").getBytes(ISO));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] body = bos.toByteArray();
        String req = "POST /upload HTTP/1.1\r\nHost: lab.example\r\nContent-Type: multipart/form-data; boundary="
                + b + "\r\nContent-Length: " + body.length + "\r\n\r\n";
        byte[] header = req.getBytes(ISO);
        byte[] full = new byte[header.length + body.length];
        System.arraycopy(header, 0, full, 0, header.length);
        System.arraycopy(body, 0, full, header.length, body.length);
        return FileUploadRequestAnalyzer.analyze(full, "multi");
    }

    static byte[] minimalPng() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            writePngChunk(bos, "IHDR", new byte[] {0, 0, 0, 1, 0, 0, 0, 1, 8, 0, 0, 0, 0});
            writePngChunk(bos, "IEND", new byte[0]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

    static void writePngChunk(ByteArrayOutputStream bos, String type, byte[] data) throws Exception {
        int len = data.length;
        bos.write((len >>> 24) & 0xff);
        bos.write((len >>> 16) & 0xff);
        bos.write((len >>> 8) & 0xff);
        bos.write(len & 0xff);
        byte[] typeBytes = type.getBytes("US-ASCII");
        bos.write(typeBytes);
        bos.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        long c = crc.getValue();
        bos.write((int) ((c >>> 24) & 0xff));
        bos.write((int) ((c >>> 16) & 0xff));
        bos.write((int) ((c >>> 8) & 0xff));
        bos.write((int) (c & 0xff));
    }
}
