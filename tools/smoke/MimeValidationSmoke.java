package uk.co.pentest.psychoPATH;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import uk.co.pentest.psychoPATH.fileupload.FileUploadRequestAnalyzer;
import uk.co.pentest.psychoPATH.fileupload.FilenameTestingConfig;
import uk.co.pentest.psychoPATH.fileupload.FilenameTestingEngine;
import uk.co.pentest.psychoPATH.fileupload.MimeAnalyzer;
import uk.co.pentest.psychoPATH.fileupload.MimeComparisonResult;
import uk.co.pentest.psychoPATH.fileupload.MimeExtensionMapper;
import uk.co.pentest.psychoPATH.fileupload.MimeParseResult;
import uk.co.pentest.psychoPATH.fileupload.MimeSignatureComparator;
import uk.co.pentest.psychoPATH.fileupload.MimeTestStrategy;
import uk.co.pentest.psychoPATH.fileupload.MimeTestingConfig;
import uk.co.pentest.psychoPATH.fileupload.MimeValidationEngine;
import uk.co.pentest.psychoPATH.fileupload.MimeVariant;
import uk.co.pentest.psychoPATH.fileupload.MimeVariantGenerator;
import uk.co.pentest.psychoPATH.fileupload.UploadRequestResult;
import uk.co.pentest.psychoPATH.fileupload.UploadTestCase;
import uk.co.pentest.psychoPATH.fileupload.UploadTestPlan;

/**
 * Phase 4.3 MIME Validation Testing Engine checks.
 */
public class MimeValidationSmoke {
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
        testMimeParser();
        testMimeMapping();
        testSignatureComparison();
        testVariantGeneration();
        testPlanGeneration();
        testCombinedCompatibility();
        testPanel();

        System.out.println("SUMMARY pass=" + pass + " fail=" + fail + " skipped=" + skipped
                + " total=" + (pass + fail + skipped));
        if (fail > 0) {
            System.exit(1);
        }
    }

    static void testMimeParser() {
        MimeParseResult png = MimeAnalyzer.analyze("image/png");
        check("parse image/png", png.isUsable() && "image/png".equals(png.getNormalizedTypeSubtype()),
                png.getNormalizedTypeSubtype());
        check("parse jpeg", "image/jpeg".equals(MimeAnalyzer.analyze("image/jpeg").getNormalizedTypeSubtype()),
                "jpeg");
        check("parse pdf", "application/pdf".equals(MimeAnalyzer.analyze("application/pdf").getNormalizedTypeSubtype()),
                "pdf");
        check("parse text/plain", "text/plain".equals(MimeAnalyzer.analyze("text/plain").getNormalizedTypeSubtype()),
                "plain");
        check("parse octet-stream",
                "application/octet-stream".equals(
                        MimeAnalyzer.analyze("application/octet-stream").getNormalizedTypeSubtype()),
                "octet");

        MimeParseResult withParam = MimeAnalyzer.analyze("text/plain; charset=UTF-8");
        check("params not in subtype", "text/plain".equals(withParam.getNormalizedTypeSubtype()),
                withParam.getNormalizedTypeSubtype());
        check("charset extracted", "UTF-8".equals(withParam.getCharset()), withParam.getCharset());

        MimeParseResult quoted = MimeAnalyzer.analyze("text/plain; charset=\"UTF-8\"");
        check("quoted charset", "UTF-8".equals(quoted.getCharset()), quoted.getCharset());

        MimeParseResult ws = MimeAnalyzer.analyze("  image/png  ");
        check("whitespace trim normalize", "image/png".equals(ws.getNormalizedTypeSubtype()),
                ws.getNormalizedTypeSubtype());
        check("raw preserved with spaces", "  image/png  ".equals(ws.getRawValue()), quote(ws.getRawValue()));

        MimeParseResult upper = MimeAnalyzer.analyze("IMAGE/PNG");
        check("uppercase normalize", "image/png".equals(upper.getNormalizedTypeSubtype()),
                upper.getNormalizedTypeSubtype());
        check("uppercase raw preserved", "IMAGE/PNG".equals(upper.getRawValue()), upper.getRawValue());

        MimeParseResult mixed = MimeAnalyzer.analyze("Image/Png");
        check("mixed-case normalize", "image/png".equals(mixed.getNormalizedTypeSubtype()),
                mixed.getNormalizedTypeSubtype());

        check("missing mime", MimeAnalyzer.analyze(null).getState() == MimeParseResult.State.MISSING, "missing");
        check("empty mime", MimeAnalyzer.analyze("").getState() == MimeParseResult.State.EMPTY, "empty");
        check("malformed no slash", MimeAnalyzer.analyze("imagepng").getState() == MimeParseResult.State.MALFORMED,
                "malformed");
        check("invalid separator", MimeAnalyzer.analyze("image/png;bad").getWarnings().size() > 0
                || MimeAnalyzer.analyze("image/png;bad").isUsable(), "param warn");

        MimeParseResult multi = MimeAnalyzer.analyze("text/plain; charset=UTF-8; format=flowed");
        check("multiple parameters", multi.getParameters().size() >= 2, multi.getParameters().toString());

        check("valid syntax helper", MimeAnalyzer.isValidMimeSyntax("image/gif"), "gif");
        check("invalid syntax helper", !MimeAnalyzer.isValidMimeSyntax("not-a-mime"), "bad");
    }

    static void testMimeMapping() {
        MimeExtensionMapper map = new MimeExtensionMapper();
        check("jpg → jpeg", map.lookup("jpg").mimeTypes.contains("image/jpeg"), map.lookup("jpg").mimeTypes.toString());
        check("jpeg → jpeg", map.lookup("JPEG").primaryMime.equals("image/jpeg"), map.lookup("JPEG").primaryMime);
        check("png", "image/png".equals(map.lookup("png").primaryMime), map.lookup("png").primaryMime);
        check("gif", "image/gif".equals(map.lookup("gif").primaryMime), map.lookup("gif").primaryMime);
        check("pdf", "application/pdf".equals(map.lookup("pdf").primaryMime), map.lookup("pdf").primaryMime);
        check("txt", "text/plain".equals(map.lookup("txt").primaryMime), map.lookup("txt").primaryMime);
        check("zip", "application/zip".equals(map.lookup("zip").primaryMime), map.lookup("zip").primaryMime);
        check("gz", "application/gzip".equals(map.lookup("gz").primaryMime), map.lookup("gz").primaryMime);

        check("unknown ext", map.lookup("xyzabc").kind == MimeExtensionMapper.AssociationKind.UNKNOWN, "unk");
        check("case insensitive", map.lookup("PnG").primaryMime.equals("image/png"), "png");

        check("custom mapping", map.addCustomMapping("foo", "application/x-foo"), "add");
        check("custom kind", map.lookup("foo").kind == MimeExtensionMapper.AssociationKind.CUSTOM
                || map.lookup("foo").mimeTypes.contains("application/x-foo"), map.lookup("foo").kind.name());
        check("invalid custom rejected", !map.addCustomMapping("bar", "bad"), "bad");

        check("ambiguous xml", map.lookup("xml").kind == MimeExtensionMapper.AssociationKind.AMBIGUOUS
                || map.lookup("xml").mimeTypes.size() > 1, map.lookup("xml").mimeTypes.toString());

        check("extensions for mime", map.extensionsForMime("image/jpeg").contains("jpg"),
                map.extensionsForMime("image/jpeg").toString());
    }

    static void testSignatureComparison() {
        MimeExtensionMapper map = new MimeExtensionMapper();
        MimeComparisonResult ok = MimeSignatureComparator.compare("png",
                MimeAnalyzer.analyze("image/png"), "PNG", map);
        check("PNG consistent", ok.getStatus() == MimeComparisonResult.Status.CONSISTENT, ok.getStatus().name());

        MimeComparisonResult jpeg = MimeSignatureComparator.compare("jpg",
                MimeAnalyzer.analyze("image/jpeg"), "JPEG", map);
        check("JPEG consistent", jpeg.getStatus() == MimeComparisonResult.Status.CONSISTENT, jpeg.getStatus().name());

        MimeComparisonResult gif = MimeSignatureComparator.compare("gif",
                MimeAnalyzer.analyze("image/gif"), "GIF", map);
        check("GIF consistent", gif.getStatus() == MimeComparisonResult.Status.CONSISTENT, gif.getStatus().name());

        MimeComparisonResult pdf = MimeSignatureComparator.compare("pdf",
                MimeAnalyzer.analyze("application/pdf"), "PDF", map);
        check("PDF consistent", pdf.getStatus() == MimeComparisonResult.Status.CONSISTENT, pdf.getStatus().name());

        MimeComparisonResult zip = MimeSignatureComparator.compare("zip",
                MimeAnalyzer.analyze("application/zip"), "ZIP", map);
        check("ZIP consistent", zip.getStatus() == MimeComparisonResult.Status.CONSISTENT, zip.getStatus().name());

        MimeComparisonResult mismatch = MimeSignatureComparator.compare("png",
                MimeAnalyzer.analyze("image/jpeg"), "PNG", map);
        check("png+jpeg MIME mismatch",
                mismatch.getStatus() == MimeComparisonResult.Status.MIME_EXTENSION_MISMATCH
                        || mismatch.getStatus() == MimeComparisonResult.Status.MIME_SIGNATURE_MISMATCH
                        || mismatch.getStatus() == MimeComparisonResult.Status.MULTIPLE_INCONSISTENCIES,
                mismatch.getStatus().name());

        MimeComparisonResult jpegPng = MimeSignatureComparator.compare("jpg",
                MimeAnalyzer.analyze("image/png"), "JPEG", map);
        check("jpg declared png mismatch",
                jpegPng.getStatus() != MimeComparisonResult.Status.CONSISTENT, jpegPng.getStatus().name());

        MimeComparisonResult unkSig = MimeSignatureComparator.compare("png",
                MimeAnalyzer.analyze("image/png"), "UNKNOWN", map);
        check("unknown signature",
                unkSig.getStatus() == MimeComparisonResult.Status.UNKNOWN_SIGNATURE
                        || unkSig.getStatus() == MimeComparisonResult.Status.CONSISTENT
                        || unkSig.getStatus() == MimeComparisonResult.Status.INCONCLUSIVE,
                unkSig.getStatus().name());

        MimeComparisonResult missing = MimeSignatureComparator.compare("png",
                MimeAnalyzer.analyze(null), "PNG", map);
        check("missing mime", missing.getStatus() == MimeComparisonResult.Status.MISSING_MIME,
                missing.getStatus().name());

        MimeComparisonResult octet = MimeSignatureComparator.compare("png",
                MimeAnalyzer.analyze("application/octet-stream"), "PNG", map);
        check("octet-stream special",
                octet.getStatus() == MimeComparisonResult.Status.CONSISTENT
                        || octet.getStatus() == MimeComparisonResult.Status.MIME_EXTENSION_MISMATCH,
                octet.getStatus().name() + " :: " + octet.getExplanation());

        check("sig preferred mime", "image/png".equals(MimeSignatureComparator.signaturePreferredMime("PNG")),
                MimeSignatureComparator.signaturePreferredMime("PNG"));
    }

    static void testVariantGeneration() {
        MimeParseResult mime = MimeAnalyzer.analyze("image/png");
        MimeTestingConfig cfg = MimeTestingConfig.defaults();
        MimeVariantGenerator.GenerationStats stats = new MimeVariantGenerator.GenerationStats();
        List<MimeVariant> all = MimeVariantGenerator.generate(mime, "png", cfg, stats);
        check("variants non-empty", !all.isEmpty(), "n=" + all.size());
        check("has declared", hasStrat(all, MimeTestStrategy.DECLARED_MIME_VARIATIONS), "decl");
        check("has case", hasStrat(all, MimeTestStrategy.MIME_CASE_VARIATIONS), "case");
        check("has params", hasStrat(all, MimeTestStrategy.MIME_PARAMETER_VARIATIONS), "params");
        check("has missing", hasStrat(all, MimeTestStrategy.MISSING_EMPTY_MIME), "miss");
        check("has mismatch", hasStrat(all, MimeTestStrategy.MIME_EXTENSION_MISMATCH), "mis");

        boolean missingSentinel = false;
        boolean emptyVal = false;
        for (MimeVariant v : all) {
            if (v.isMissingHeaderProposal()) {
                missingSentinel = true;
            }
            if ("".equals(v.getProposedMime()) && "empty-content-type".equals(v.getTransformation())) {
                emptyVal = true;
            }
        }
        check("missing sentinel distinct", missingSentinel, "missing");
        check("empty distinct from missing", emptyVal, "empty");

        cfg.addCustomMime("application/x-test");
        List<MimeVariant> withCustom = MimeVariantGenerator.generate(mime, "png", cfg,
                new MimeVariantGenerator.GenerationStats());
        check("custom strategy present", hasStrat(withCustom, MimeTestStrategy.CUSTOM_MIME), "custom");

        MimeTestingConfig limited = MimeTestingConfig.defaults();
        limited.setMaxCandidates(5);
        MimeVariantGenerator.GenerationStats st2 = new MimeVariantGenerator.GenerationStats();
        List<MimeVariant> lim = MimeVariantGenerator.generate(mime, "png", limited, st2);
        check("candidate limit", lim.size() <= 5 && st2.limitReached, "n=" + lim.size());

        List<MimeVariant> a = MimeVariantGenerator.generate(mime, "png", MimeTestingConfig.defaults(),
                new MimeVariantGenerator.GenerationStats());
        List<MimeVariant> b = MimeVariantGenerator.generate(mime, "png", MimeTestingConfig.defaults(),
                new MimeVariantGenerator.GenerationStats());
        check("deterministic count", a.size() == b.size(), a.size() + "/" + b.size());
        boolean same = true;
        for (int i = 0; i < a.size(); i++) {
            if (!String.valueOf(a.get(i).getProposedMime()).equals(String.valueOf(b.get(i).getProposedMime()))
                    || !a.get(i).getTransformation().equals(b.get(i).getTransformation())) {
                same = false;
                break;
            }
        }
        check("deterministic order", same, "order");
    }

    static void testPlanGeneration() {
        UploadRequestResult analysis = sampleUpload("photo.png", "image/png");
        MimeTestingConfig cfg = MimeTestingConfig.defaults();

        check("validate null analysis",
                !MimeValidationEngine.validate(null, Integer.valueOf(0), cfg).valid, "null");
        check("validate null part",
                !MimeValidationEngine.validate(analysis, null, cfg).valid, "part");

        MimeTestingConfig empty = MimeTestingConfig.defaults();
        for (MimeTestStrategy s : MimeTestStrategy.values()) {
            empty.setStrategyEnabled(s, false);
        }
        check("validate empty strategies",
                !MimeValidationEngine.validate(analysis, Integer.valueOf(0), empty).valid, "strat");

        UploadTestPlan plan = MimeValidationEngine.generatePlan(analysis, 0, cfg);
        check("plan id", plan.getPlanId() != null && plan.getPlanId().startsWith("plan-mime-"), plan.getPlanId());
        check("analysis id", analysis.getAnalysisId().equals(plan.getAnalysisId()), plan.getAnalysisId());
        check("part index", plan.getSelectedPartIndex() != null && plan.getSelectedPartIndex().intValue() == 0,
                String.valueOf(plan.getSelectedPartIndex()));
        check("original filename", "photo.png".equals(plan.getOriginalFilename()), plan.getOriginalFilename());
        check("has cases", !plan.getCases().isEmpty(), "n=" + plan.getCases().size());
        check("mime comparison present", plan.getMimeComparison() != null, "cmp");
        check("comparison consistent-ish",
                plan.getMimeComparison().getStatus() == MimeComparisonResult.Status.CONSISTENT,
                plan.getMimeComparison().getStatus().name());

        Set<String> ids = new HashSet<String>();
        boolean allOk = true;
        for (UploadTestCase tc : plan.getCases()) {
            if (!ids.add(tc.getTestId())) {
                allOk = false;
            }
            if (!UploadTestCase.CATEGORY_MIME.equals(tc.getTestCategory())) {
                allOk = false;
            }
            if (!UploadTestCase.TARGET_PART_CONTENT_TYPE.equals(tc.getTargetProperty())) {
                allOk = false;
            }
            if (!UploadTestCase.STATUS_NOT_EXECUTED.equals(tc.getExecutionStatus())) {
                allOk = false;
            }
            if (!"photo.png".equals(tc.getOriginalFilename())) {
                allOk = false;
            }
            if (tc.getDetectedSignature() == null) {
                allOk = false;
            }
        }
        check("unique ids + MIME metadata", allOk && ids.size() == plan.getCases().size(),
                "ids=" + ids.size());

        plan.deselectAll();
        check("deselect", plan.getSelectedCount() == 0, String.valueOf(plan.getSelectedCount()));
        plan.selectAll();
        check("select all", plan.getSelectedCount() == plan.getCases().size(),
                String.valueOf(plan.getSelectedCount()));
    }

    static void testCombinedCompatibility() {
        UploadRequestResult analysis = sampleUpload("avatar.jpg", "image/jpeg");
        UploadTestPlan fn = FilenameTestingEngine.generatePlan(analysis, 0, FilenameTestingConfig.defaults());
        UploadTestPlan mime = MimeValidationEngine.generatePlan(analysis, 0, MimeTestingConfig.defaults());
        check("filename plan still works", !fn.getCases().isEmpty()
                && UploadTestCase.CATEGORY_FILENAME.equals(fn.getCases().get(0).getTestCategory()),
                "fn=" + fn.getCases().size());
        check("mime plan category", !mime.getCases().isEmpty()
                && UploadTestCase.CATEGORY_MIME.equals(mime.getCases().get(0).getTestCategory()),
                "mime=" + mime.getCases().size());
        check("distinct plan ids", !fn.getPlanId().equals(mime.getPlanId()), fn.getPlanId() + "/" + mime.getPlanId());
        check("filename targets FILENAME",
                UploadTestCase.TARGET_FILENAME.equals(fn.getCases().get(0).getTargetProperty()), "fn target");
        check("mime targets PART_CONTENT_TYPE",
                UploadTestCase.TARGET_PART_CONTENT_TYPE.equals(mime.getCases().get(0).getTargetProperty()),
                "mime target");

        // No shared test IDs across engines for same analysis
        Set<String> all = new HashSet<String>();
        boolean unique = true;
        for (UploadTestCase tc : fn.getCases()) {
            if (!all.add(tc.getTestId())) {
                unique = false;
            }
        }
        for (UploadTestCase tc : mime.getCases()) {
            if (!all.add(tc.getTestId())) {
                unique = false;
            }
        }
        check("no colliding test ids across plans", unique, "total=" + all.size());
    }

    static void testPanel() {
        try {
            System.setProperty("java.awt.headless", "true");
            uk.co.pentest.psychoPATH.fileupload.FileUploadPanel panel =
                    new uk.co.pentest.psychoPATH.fileupload.FileUploadPanel();
            check("FileUploadPanel with MIME tab", panel != null, "ok");
            skipped++;
            System.out.println("SKIP burp-gui-mime-testing :: requires Burp desktop runtime");
            skipped++;
            System.out.println("SKIP portswigger-content-type-lab :: requires Academy session");
        } catch (Throwable t) {
            check("FileUploadPanel with MIME tab", false, t.toString());
        }
    }

    static boolean hasStrat(List<MimeVariant> list, MimeTestStrategy s) {
        for (MimeVariant v : list) {
            if (v.getStrategy() == s) {
                return true;
            }
        }
        return false;
    }

    static UploadRequestResult sampleUpload(String filename, String partMime) {
        String b = "----MIMETEST";
        String partHeaders = "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename
                + "\"\r\nContent-Type: " + partMime;
        byte[] content;
        if (filename.toLowerCase().endsWith(".png") || "image/png".equals(partMime)) {
            content = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        } else {
            content = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        }
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try {
            bos.write(("--" + b + "\r\n").getBytes(ISO));
            bos.write((partHeaders + "\r\n\r\n").getBytes(ISO));
            bos.write(content);
            bos.write(("\r\n--" + b + "--\r\n").getBytes(ISO));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        byte[] body = bos.toByteArray();
        String req = "POST /upload HTTP/1.1\r\nHost: lab.example\r\nContent-Type: multipart/form-data; boundary="
                + b + "\r\n\r\n";
        byte[] full = new byte[req.getBytes(ISO).length + body.length];
        System.arraycopy(req.getBytes(ISO), 0, full, 0, req.getBytes(ISO).length);
        System.arraycopy(body, 0, full, req.getBytes(ISO).length, body.length);
        return FileUploadRequestAnalyzer.analyze(full, "mime-smoke");
    }

    static String quote(String s) {
        return s == null ? "null" : "'" + s.replace(" ", "·") + "'";
    }
}
