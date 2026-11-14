package uk.co.pentest.psychoPATH;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import uk.co.pentest.psychoPATH.fileupload.FileUploadRequestAnalyzer;
import uk.co.pentest.psychoPATH.fileupload.FilenameAnalysisResult;
import uk.co.pentest.psychoPATH.fileupload.FilenameAnalyzer;
import uk.co.pentest.psychoPATH.fileupload.FilenameTestStrategy;
import uk.co.pentest.psychoPATH.fileupload.FilenameTestingConfig;
import uk.co.pentest.psychoPATH.fileupload.FilenameTestingEngine;
import uk.co.pentest.psychoPATH.fileupload.FilenameVariant;
import uk.co.pentest.psychoPATH.fileupload.FilenameVariantGenerator;
import uk.co.pentest.psychoPATH.fileupload.UploadRequestClassification;
import uk.co.pentest.psychoPATH.fileupload.UploadRequestResult;
import uk.co.pentest.psychoPATH.fileupload.UploadTestCase;
import uk.co.pentest.psychoPATH.fileupload.UploadTestPlan;

/**
 * Phase 4.2 Filename & Extension Testing Engine checks.
 */
public class FilenameTestingSmoke {
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
        testFilenameAnalyzer();
        testExtensionExtraction();
        testVariantGeneration();
        testPlanGeneration();
        testIntegrationWithAnalyzer();
        testPanelConstruct();

        System.out.println("SUMMARY pass=" + pass + " fail=" + fail + " skipped=" + skipped
                + " total=" + (pass + fail + skipped));
        if (fail > 0) {
            System.exit(1);
        }
    }

    static void testFilenameAnalyzer() {
        FilenameAnalysisResult simple = FilenameAnalyzer.analyze("image.jpg");
        check("simple filename kind", simple.getKind() == FilenameAnalysisResult.Kind.SINGLE_EXTENSION, simple.getKind().name());
        check("simple base", "image".equals(simple.getBaseName()), simple.getBaseName());
        check("simple final ext", "jpg".equals(simple.getFinalExtension()), simple.getFinalExtension());

        FilenameAnalysisResult noExt = FilenameAnalyzer.analyze("filename");
        check("no extension", noExt.getKind() == FilenameAnalysisResult.Kind.NO_EXTENSION, noExt.getKind().name());

        FilenameAnalysisResult hidden = FilenameAnalyzer.analyze(".htaccess");
        check("hidden filename", hidden.getKind() == FilenameAnalysisResult.Kind.HIDDEN, hidden.getKind().name());

        FilenameAnalysisResult multi = FilenameAnalyzer.analyze("test.php.jpg");
        check("multiple extensions kind", multi.getKind() == FilenameAnalysisResult.Kind.MULTIPLE_EXTENSIONS,
                multi.getExtensionSegments().toString());
        check("multiple segments php,jpg",
                multi.getExtensionSegments().equals(Arrays.asList("php", "jpg")),
                multi.getExtensionSegments().toString());

        FilenameAnalysisResult trailDot = FilenameAnalyzer.analyze("filename.");
        check("trailing dot", trailDot.isTrailingDot(), "dot");
        FilenameAnalysisResult trailSpace = FilenameAnalyzer.analyze("test.jpg ");
        check("trailing whitespace", trailSpace.isTrailingSpace(), "space");
        check("raw preserved with trailing space", "test.jpg ".equals(trailSpace.getRawFilename()),
                quote(trailSpace.getRawFilename()));
        FilenameAnalysisResult leadSpace = FilenameAnalyzer.analyze(" test.jpg");
        check("leading whitespace", leadSpace.isLeadingWhitespace(), "lead");

        FilenameAnalysisResult multiDot = FilenameAnalyzer.analyze("filename..jpg");
        check("multiple dots", multiDot.hasMultipleDots(), multiDot.getExtensionSegments().toString());

        FilenameAnalysisResult upper = FilenameAnalyzer.analyze("FILE.JPG");
        check("uppercase extension", upper.isAllUpperExtension(), upper.getFinalExtension());
        FilenameAnalysisResult mixed = FilenameAnalyzer.analyze("file.JpG");
        check("mixed-case extension", mixed.isMixedCaseExtension(), mixed.getFinalExtension());

        FilenameAnalysisResult nonAscii = FilenameAnalyzer.analyze("caf\u00e9.png");
        check("non-ascii filename", nonAscii.isNonAscii(), nonAscii.getRawFilename());

        FilenameAnalysisResult pct = FilenameAnalyzer.analyze("a%20b.jpg");
        check("percent-encoded", pct.isPercentEncoded(), pct.getRawFilename());

        FilenameAnalysisResult spaces = FilenameAnalyzer.analyze("file name.jpg");
        check("filename with spaces", spaces.hasWhitespace(), spaces.getRawFilename());

        FilenameAnalysisResult special = FilenameAnalyzer.analyze("file(1).jpg");
        check("special chars length", special.getLength() > 0, String.valueOf(special.getLength()));

        FilenameAnalysisResult empty = FilenameAnalyzer.analyze("");
        check("empty filename", empty.getKind() == FilenameAnalysisResult.Kind.EMPTY, empty.getKind().name());
        FilenameAnalysisResult missing = FilenameAnalyzer.analyze(null);
        check("missing filename", missing.getKind() == FilenameAnalysisResult.Kind.MISSING, missing.getKind().name());

        FilenameAnalysisResult pathSep = FilenameAnalyzer.analyze("../x.jpg");
        check("path separator /", pathSep.hasForwardSlash() || pathSep.hasPathTraversalPattern(),
                pathSep.getKind().name());
        FilenameAnalysisResult winSep = FilenameAnalyzer.analyze("a\\b.jpg");
        check("path separator \\", winSep.hasBackslash(), "backslash");

        // Original never silently trimmed
        check("no silent trim", " test.jpg".equals(leadSpace.getRawFilename()), quote(leadSpace.getRawFilename()));
    }

    static void testExtensionExtraction() {
        check("ext jpg", segs("image.jpg").equals(Arrays.asList("jpg")), segs("image.jpg").toString());
        check("ext jpeg", segs("image.jpeg").equals(Arrays.asList("jpeg")), segs("image.jpeg").toString());
        check("ext png", segs("a.png").equals(Arrays.asList("png")), segs("a.png").toString());
        check("ext gif", segs("a.gif").equals(Arrays.asList("gif")), segs("a.gif").toString());
        check("ext pdf", segs("d.pdf").equals(Arrays.asList("pdf")), segs("d.pdf").toString());
        check("ext tar.gz", segs("archive.tar.gz").equals(Arrays.asList("tar", "gz")),
                segs("archive.tar.gz").toString());
        check("ext php.jpg", segs("test.php.jpg").equals(Arrays.asList("php", "jpg")),
                segs("test.php.jpg").toString());
        check("ext jpg.php", segs("test.jpg.php").equals(Arrays.asList("jpg", "php")),
                segs("test.jpg.php").toString());
        check("ext htaccess empty segments", segs(".htaccess").isEmpty(), segs(".htaccess").toString());
        check("ext env empty", segs(".env").isEmpty(), segs(".env").toString());
        check("ext none", segs("filename").isEmpty(), "empty");
        check("ext empty trailing", segs("filename.").equals(Arrays.asList("")), segs("filename.").toString());
        check("ext multi segments count", FilenameAnalyzer.analyze("a.b.c.d").getExtensionCount() == 3,
                String.valueOf(FilenameAnalyzer.analyze("a.b.c.d").getExtensionCount()));

        FilenameTestingConfig cfg = FilenameTestingConfig.defaults();
        check("normalizeExt lower", "jpg".equals(FilenameTestingConfig.normalizeExt("JPG")), "jpg");
        check("normalizeExt strip dot", "png".equals(FilenameTestingConfig.normalizeExt(".png")), "png");
        check("case insensitive flag default", cfg.isCaseInsensitiveComparison(), "true");
        cfg.setCaseInsensitiveComparison(false);
        check("case sensitive mode", !cfg.isCaseInsensitiveComparison(), "false");
    }

    static List<String> segs(String name) {
        return FilenameAnalyzer.extractExtensionSegments(FilenameAnalyzer.stripPathPrefix(name));
    }

    static void testVariantGeneration() {
        FilenameAnalysisResult fa = FilenameAnalyzer.analyze("photo.jpg");
        FilenameTestingConfig cfg = FilenameTestingConfig.defaults();
        FilenameVariantGenerator.GenerationStats stats = new FilenameVariantGenerator.GenerationStats();
        List<FilenameVariant> all = FilenameVariantGenerator.generate(fa, cfg, stats);
        check("variants non-empty", !all.isEmpty(), "n=" + all.size());
        check("stats raw >= after", stats.rawGenerated >= stats.afterDedup, stats.rawGenerated + "/" + stats.afterDedup);

        check("has multiple extension strategy", hasStrategy(all, FilenameTestStrategy.MULTIPLE_EXTENSION), "multi");
        check("has case strategy", hasStrategy(all, FilenameTestStrategy.EXTENSION_CASE), "case");
        check("has trailing", hasStrategy(all, FilenameTestStrategy.TRAILING_CHARACTERS), "trail");
        check("has encoding", hasStrategy(all, FilenameTestStrategy.FILENAME_ENCODING), "enc");
        check("has whitespace", hasStrategy(all, FilenameTestStrategy.WHITESPACE_SPECIAL), "ws");
        check("has parameter", hasStrategy(all, FilenameTestStrategy.FILENAME_PARAMETER), "param");
        check("has path sep", hasStrategy(all, FilenameTestStrategy.PATH_SEPARATOR), "path");

        // Trailing space exact
        boolean trailSpace = false;
        for (FilenameVariant v : all) {
            if ("trailing-space".equals(v.getTransformation()) && v.getGeneratedFilename().endsWith(" ")) {
                trailSpace = true;
                check("trailing space exact char",
                        v.getGeneratedFilename().equals("photo.jpg "),
                        quote(v.getGeneratedFilename()));
            }
        }
        check("trailing space variant present", trailSpace, "found");

        // Encoding depth recorded
        boolean enc = false;
        for (FilenameVariant v : all) {
            if (v.getStrategy() == FilenameTestStrategy.FILENAME_ENCODING
                    && v.getTransformation().contains("encode-dot")) {
                enc = true;
            }
        }
        check("encoding variants", enc, "enc");

        // Limit
        FilenameTestingConfig limited = FilenameTestingConfig.defaults();
        limited.setMaxCandidates(5);
        FilenameVariantGenerator.GenerationStats st2 = new FilenameVariantGenerator.GenerationStats();
        List<FilenameVariant> lim = FilenameVariantGenerator.generate(fa, limited, st2);
        check("max candidate limit", lim.size() <= 5 && st2.limitReached, "n=" + lim.size() + " limit=" + st2.limitReached);

        // Deterministic
        List<FilenameVariant> a = FilenameVariantGenerator.generate(fa, cfg, new FilenameVariantGenerator.GenerationStats());
        List<FilenameVariant> b = FilenameVariantGenerator.generate(fa, cfg, new FilenameVariantGenerator.GenerationStats());
        check("deterministic count", a.size() == b.size(), a.size() + "/" + b.size());
        boolean sameOrder = true;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).getGeneratedFilename().equals(b.get(i).getGeneratedFilename())
                    || !a.get(i).getTransformation().equals(b.get(i).getTransformation())) {
                sameOrder = false;
                break;
            }
        }
        check("deterministic ordering", sameOrder, "order");

        // Only one strategy
        FilenameTestingConfig onlyCase = FilenameTestingConfig.defaults();
        for (FilenameTestStrategy s : FilenameTestStrategy.values()) {
            onlyCase.setStrategyEnabled(s, s == FilenameTestStrategy.EXTENSION_CASE);
        }
        List<FilenameVariant> only = FilenameVariantGenerator.generate(fa, onlyCase,
                new FilenameVariantGenerator.GenerationStats());
        boolean onlyCaseOk = !only.isEmpty();
        for (FilenameVariant v : only) {
            if (v.getStrategy() != FilenameTestStrategy.EXTENSION_CASE) {
                onlyCaseOk = false;
            }
        }
        check("single strategy filter", onlyCaseOk, "n=" + only.size());
    }

    static void testPlanGeneration() {
        UploadRequestResult analysis = sampleUploadAnalysis("avatar.png");
        check("sample analysis confirmed",
                analysis.getClassification() == UploadRequestClassification.CONFIRMED_MULTIPART_UPLOAD,
                analysis.getClassification().name());

        FilenameTestingConfig cfg = FilenameTestingConfig.defaults();
        FilenameTestingEngine.PlanValidation bad = FilenameTestingEngine.validate(null, Integer.valueOf(0), cfg);
        check("validate missing analysis", !bad.valid, bad.message);

        FilenameTestingEngine.PlanValidation noPart = FilenameTestingEngine.validate(analysis, null, cfg);
        check("validate missing part", !noPart.valid, noPart.message);

        FilenameTestingConfig emptyStrat = FilenameTestingConfig.defaults();
        for (FilenameTestStrategy s : FilenameTestStrategy.values()) {
            emptyStrat.setStrategyEnabled(s, false);
        }
        FilenameTestingEngine.PlanValidation noStrat =
                FilenameTestingEngine.validate(analysis, Integer.valueOf(0), emptyStrat);
        check("validate empty strategies", !noStrat.valid, noStrat.message);

        UploadTestPlan plan = FilenameTestingEngine.generatePlan(analysis, 0, cfg);
        check("plan id present", plan.getPlanId() != null && plan.getPlanId().startsWith("plan-fn-"), plan.getPlanId());
        check("plan analysis id", analysis.getAnalysisId().equals(plan.getAnalysisId()), plan.getAnalysisId());
        check("plan part index", plan.getSelectedPartIndex() != null && plan.getSelectedPartIndex().intValue() == 0,
                String.valueOf(plan.getSelectedPartIndex()));
        check("plan original filename", "avatar.png".equals(plan.getOriginalFilename()), plan.getOriginalFilename());
        check("plan original extension", "png".equals(plan.getOriginalExtension()), plan.getOriginalExtension());
        check("plan has cases", !plan.getCases().isEmpty(), "n=" + plan.getCases().size());
        check("plan config summary", plan.getConfigurationSummary() != null, plan.getConfigurationSummary());

        Set<String> ids = new HashSet<String>();
        boolean allNotExecuted = true;
        boolean allSelected = true;
        boolean metaOk = true;
        for (UploadTestCase tc : plan.getCases()) {
            if (!ids.add(tc.getTestId())) {
                metaOk = false;
            }
            if (!UploadTestCase.STATUS_NOT_EXECUTED.equals(tc.getExecutionStatus())) {
                allNotExecuted = false;
            }
            if (!tc.isSelected()) {
                allSelected = false;
            }
            if (tc.getStrategyId() == null || tc.getGeneratedFilename() == null || tc.getPlanId() == null) {
                metaOk = false;
            }
            if (!plan.getPlanId().equals(tc.getPlanId())) {
                metaOk = false;
            }
        }
        check("unique test ids", ids.size() == plan.getCases().size(), String.valueOf(ids.size()));
        check("all NOT_EXECUTED", allNotExecuted, "status");
        check("default selected", allSelected, "selected");
        check("case metadata", metaOk, "meta");

        plan.deselectAll();
        check("deselect all", plan.getSelectedCount() == 0, String.valueOf(plan.getSelectedCount()));
        plan.selectAll();
        check("select all", plan.getSelectedCount() == plan.getCases().size(),
                String.valueOf(plan.getSelectedCount()));

        // Dedup stats present
        check("dedup stats", plan.getAfterDedupCount() == plan.getCases().size(),
                plan.getRawGeneratedCount() + "->" + plan.getAfterDedupCount());

        // Empty plan shell still works
        UploadTestPlan empty = UploadTestPlan.emptyFromAnalysis(analysis);
        check("empty plan shell", empty.getCases().isEmpty(), empty.getNotes());
    }

    static void testIntegrationWithAnalyzer() {
        UploadRequestResult r1 = sampleUploadAnalysis("shell.php.jpg");
        UploadTestPlan p1 = FilenameTestingEngine.generatePlan(r1, 0, FilenameTestingConfig.defaults());
        check("integration multi-ext original", "shell.php.jpg".equals(p1.getOriginalFilename()),
                p1.getOriginalFilename());
        check("integration analysis kind multi",
                p1.getFilenameAnalysis() != null
                        && p1.getFilenameAnalysis().getKind() == FilenameAnalysisResult.Kind.MULTIPLE_EXTENSIONS,
                p1.getFilenameAnalysis() == null ? "null" : p1.getFilenameAnalysis().getKind().name());

        UploadRequestResult r2 = sampleUploadAnalysis("ok.jpg");
        UploadTestPlan a = FilenameTestingEngine.generatePlan(r2, 0, FilenameTestingConfig.defaults());
        UploadTestPlan b = FilenameTestingEngine.generatePlan(r2, 0, FilenameTestingConfig.defaults());
        // Plan IDs differ (timestamp/uuid) but case transformations should match in order
        check("integration case count stable", a.getCases().size() == b.getCases().size(),
                a.getCases().size() + "/" + b.getCases().size());
        boolean sameGen = true;
        for (int i = 0; i < a.getCases().size(); i++) {
            if (!a.getCases().get(i).getGeneratedFilename().equals(b.getCases().get(i).getGeneratedFilename())) {
                sameGen = false;
                break;
            }
        }
        check("integration deterministic filenames", sameGen, "filenames");
    }

    static void testPanelConstruct() {
        try {
            System.setProperty("java.awt.headless", "true");
            uk.co.pentest.psychoPATH.fileupload.FileUploadPanel panel =
                    new uk.co.pentest.psychoPATH.fileupload.FileUploadPanel();
            check("FileUploadPanel constructs with filename tab", panel != null, "ok");
            skipped++;
            System.out.println("SKIP burp-gui-filename-testing :: requires Burp desktop runtime");
            skipped++;
            System.out.println("SKIP portswigger-lab-manual :: requires Academy session");
        } catch (Throwable t) {
            check("FileUploadPanel constructs with filename tab", false, t.toString());
        }
    }

    static boolean hasStrategy(List<FilenameVariant> list, FilenameTestStrategy s) {
        for (FilenameVariant v : list) {
            if (v.getStrategy() == s) {
                return true;
            }
        }
        return false;
    }

    static UploadRequestResult sampleUploadAnalysis(String filename) {
        String b = "----FNTEST";
        String partHeaders = "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename
                + "\"\r\nContent-Type: application/octet-stream";
        byte[] content = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
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
        return FileUploadRequestAnalyzer.analyze(full, "filename-smoke");
    }

    static String quote(String s) {
        return s == null ? "null" : "'" + s.replace(" ", "·") + "'";
    }
}
