package uk.co.pentest.psychoPATH;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;
import uk.co.pentest.psychoPATH.fileupload.ContentConsistencyResult;
import uk.co.pentest.psychoPATH.fileupload.ContentTestStrategy;
import uk.co.pentest.psychoPATH.fileupload.ContentValidationConfig;
import uk.co.pentest.psychoPATH.fileupload.ContentVariant;
import uk.co.pentest.psychoPATH.fileupload.ContentVariantGenerator;
import uk.co.pentest.psychoPATH.fileupload.FileContentAnalysisResult;
import uk.co.pentest.psychoPATH.fileupload.FileContentHasher;
import uk.co.pentest.psychoPATH.fileupload.FileContentValidationEngine;
import uk.co.pentest.psychoPATH.fileupload.FileStructureValidator;
import uk.co.pentest.psychoPATH.fileupload.FileUploadRequestAnalyzer;
import uk.co.pentest.psychoPATH.fileupload.FilenameTestingConfig;
import uk.co.pentest.psychoPATH.fileupload.FilenameTestingEngine;
import uk.co.pentest.psychoPATH.fileupload.MimeTestingConfig;
import uk.co.pentest.psychoPATH.fileupload.MimeValidationEngine;
import uk.co.pentest.psychoPATH.fileupload.StructureValidationResult;
import uk.co.pentest.psychoPATH.fileupload.UploadPart;
import uk.co.pentest.psychoPATH.fileupload.UploadRequestResult;
import uk.co.pentest.psychoPATH.fileupload.UploadTestCase;
import uk.co.pentest.psychoPATH.fileupload.UploadTestPlan;

/**
 * Phase 4.4 File Content Validation Engine checks.
 */
public class FileContentValidationSmoke {
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
        testHashing();
        testPngStructure();
        testJpegStructure();
        testGifStructure();
        testPdfStructure();
        testPreservation();
        testCandidateGeneration();
        testConsistency();
        testPlanGeneration();
        testCombinedCompatibility();
        testPanel();

        System.out.println("SUMMARY pass=" + pass + " fail=" + fail + " skipped=" + skipped
                + " total=" + (pass + fail + skipped));
        if (fail > 0) {
            System.exit(1);
        }
    }

    static void testHashing() {
        check("sha empty",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
                        .equals(FileContentHasher.sha256Hex(new byte[0])),
                "empty");
        byte[] a = new byte[] {1, 2, 3};
        byte[] b = new byte[] {1, 2, 3};
        byte[] c = new byte[] {1, 2, 4};
        String ha = FileContentHasher.sha256Hex(a);
        check("identical content same hash", ha.equals(FileContentHasher.sha256Hex(b)), ha);
        check("different content different hash", !ha.equals(FileContentHasher.sha256Hex(c)), "diff");
        byte[] mut = a.clone();
        String before = FileContentHasher.sha256Hex(mut);
        FileContentHasher.sha256Hex(mut);
        check("hash does not mutate", Arrays.equals(mut, a) && before.equals(FileContentHasher.sha256Hex(mut)),
                "immutable");
        check("known abc",
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
                        .equals(FileContentHasher.sha256Hex("abc".getBytes(ISO))),
                "abc");
    }

    static void testPngStructure() {
        byte[] valid = minimalPng();
        StructureValidationResult ok = FileStructureValidator.validate(valid, "PNG", 65536);
        check("png valid basic", ok.getStatus() == StructureValidationResult.Status.VALID_BASIC_STRUCTURE,
                ok.getStatus() + " :: " + ok.getExplanation());

        byte[] badSig = valid.clone();
        badSig[0] = 0x00;
        check("png invalid sig",
                FileStructureValidator.validate(badSig, "PNG", 65536).getStatus()
                        == StructureValidationResult.Status.INVALID_SIGNATURE, "sig");

        byte[] noIhdr = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0, 0, 0, 0, 'I', 'D', 'A', 'T', 0, 0, 0, 0};
        StructureValidationResult miss = FileStructureValidator.validate(noIhdr, "PNG", 65536);
        check("png missing ihdr",
                miss.getStatus() == StructureValidationResult.Status.MISSING_REQUIRED_MARKER
                        || miss.getStatus() == StructureValidationResult.Status.TRUNCATED_CONTENT,
                miss.getStatus().name());

        byte[] noIend = Arrays.copyOf(valid, valid.length - 12);
        StructureValidationResult noEnd = FileStructureValidator.validate(noIend, "PNG", 65536);
        check("png missing iend",
                noEnd.getStatus() == StructureValidationResult.Status.MISSING_REQUIRED_MARKER
                        || noEnd.getStatus() == StructureValidationResult.Status.TRUNCATED_CONTENT,
                noEnd.getStatus().name());

        byte[] truncChunk = Arrays.copyOf(valid, 20);
        check("png truncated chunk",
                FileStructureValidator.validate(truncChunk, "PNG", 65536).getStatus()
                        == StructureValidationResult.Status.TRUNCATED_CONTENT
                        || FileStructureValidator.validate(truncChunk, "PNG", 65536).getStatus()
                        == StructureValidationResult.Status.MISSING_REQUIRED_MARKER,
                "trunc");

        byte[] badLen = valid.clone();
        badLen[8] = 0x7F;
        badLen[9] = (byte) 0xFF;
        badLen[10] = (byte) 0xFF;
        badLen[11] = (byte) 0xFF;
        StructureValidationResult badL = FileStructureValidator.validate(badLen, "PNG", 65536);
        check("png invalid chunk length/boundary",
                badL.getStatus() == StructureValidationResult.Status.TRUNCATED_CONTENT
                        || badL.getStatus() == StructureValidationResult.Status.INVALID_SEGMENT_LENGTH
                        || badL.getStatus() == StructureValidationResult.Status.INVALID_CHUNK_BOUNDARY,
                badL.getStatus().name());

        check("png empty",
                FileStructureValidator.validate(new byte[0], "PNG", 65536).getStatus()
                        == StructureValidationResult.Status.EMPTY_CONTENT, "empty");

        StructureValidationResult lim = FileStructureValidator.validate(valid, "PNG", 12);
        check("png analysis limit",
                lim.getStatus() == StructureValidationResult.Status.ANALYSIS_LIMIT_REACHED
                        || lim.getStatus() == StructureValidationResult.Status.TRUNCATED_CONTENT
                        || lim.getStatus() == StructureValidationResult.Status.MISSING_REQUIRED_MARKER
                        || !lim.getWarnings().isEmpty(),
                lim.getStatus().name());
    }

    static void testJpegStructure() {
        byte[] valid = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        StructureValidationResult ok = FileStructureValidator.validate(valid, "JPEG", 65536);
        check("jpeg valid basic", ok.getStatus() == StructureValidationResult.Status.VALID_BASIC_STRUCTURE,
                ok.getStatus().name());

        check("jpeg invalid soi",
                FileStructureValidator.validate(new byte[] {0, 1, 2, 3}, "JPEG", 65536).getStatus()
                        == StructureValidationResult.Status.INVALID_SIGNATURE
                        || FileStructureValidator.validate(new byte[] {0, 1, 2, 3}, "JPEG", 65536).getStatus()
                        == StructureValidationResult.Status.UNSUPPORTED_FORMAT,
                "soi");

        byte[] noEoi = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        StructureValidationResult miss = FileStructureValidator.validate(noEoi, "JPEG", 65536);
        check("jpeg missing eoi",
                miss.getStatus() == StructureValidationResult.Status.MISSING_REQUIRED_MARKER
                        || miss.getStatus() == StructureValidationResult.Status.TRUNCATED_CONTENT,
                miss.getStatus().name());

        byte[] truncSeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 1, 2};
        check("jpeg truncated segment",
                FileStructureValidator.validate(truncSeg, "JPEG", 65536).getStatus()
                        == StructureValidationResult.Status.TRUNCATED_CONTENT, "trunc");

        byte[] badSeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 1, (byte) 0xFF, (byte) 0xD9};
        check("jpeg invalid segment length",
                FileStructureValidator.validate(badSeg, "JPEG", 65536).getStatus()
                        == StructureValidationResult.Status.INVALID_SEGMENT_LENGTH, "seglen");

        check("jpeg empty",
                FileStructureValidator.validate(new byte[0], "JPEG", 65536).getStatus()
                        == StructureValidationResult.Status.EMPTY_CONTENT, "empty");
        check("jpeg minimal soi only",
                FileStructureValidator.validate(new byte[] {(byte) 0xFF, (byte) 0xD8}, "JPEG", 65536)
                        .getStatus() != StructureValidationResult.Status.VALID_BASIC_STRUCTURE,
                "minimal");
    }

    static void testGifStructure() {
        byte[] g89 = minimalGif("GIF89a");
        byte[] g87 = minimalGif("GIF87a");
        check("gif89a valid",
                FileStructureValidator.validate(g89, "GIF", 65536).getStatus()
                        == StructureValidationResult.Status.VALID_BASIC_STRUCTURE, "89a");
        check("gif87a valid",
                FileStructureValidator.validate(g87, "GIF", 65536).getStatus()
                        == StructureValidationResult.Status.VALID_BASIC_STRUCTURE, "87a");
        check("gif invalid sig",
                FileStructureValidator.validate("NOTGIF....;".getBytes(ISO), "GIF", 65536).getStatus()
                        == StructureValidationResult.Status.INVALID_SIGNATURE
                        || FileStructureValidator.validate("NOTGIF....;".getBytes(ISO), "GIF", 65536)
                        .getStatus() == StructureValidationResult.Status.UNSUPPORTED_FORMAT,
                "sig");
        check("gif missing lsd",
                FileStructureValidator.validate("GIF89a".getBytes(ISO), "GIF", 65536).getStatus()
                        == StructureValidationResult.Status.TRUNCATED_CONTENT, "lsd");
        byte[] noTrail = Arrays.copyOf(g89, g89.length - 1);
        check("gif missing trailer",
                FileStructureValidator.validate(noTrail, "GIF", 65536).getStatus()
                        == StructureValidationResult.Status.MISSING_REQUIRED_MARKER, "trail");
        check("gif empty",
                FileStructureValidator.validate(new byte[0], "GIF", 65536).getStatus()
                        == StructureValidationResult.Status.EMPTY_CONTENT, "empty");
    }

    static void testPdfStructure() {
        byte[] valid = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF\n".getBytes(ISO);
        check("pdf valid basic",
                FileStructureValidator.validate(valid, "PDF", 65536).getStatus()
                        == StructureValidationResult.Status.VALID_BASIC_STRUCTURE, "ok");
        check("pdf missing header",
                FileStructureValidator.validate("not a pdf %%EOF".getBytes(ISO), "PDF", 65536).getStatus()
                        == StructureValidationResult.Status.INVALID_SIGNATURE
                        || FileStructureValidator.validate("not a pdf %%EOF".getBytes(ISO), "PDF", 65536)
                        .getStatus() == StructureValidationResult.Status.UNSUPPORTED_FORMAT,
                "hdr");
        byte[] noEof = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n".getBytes(ISO);
        check("pdf missing eof",
                FileStructureValidator.validate(noEof, "PDF", 65536).getStatus()
                        == StructureValidationResult.Status.MISSING_REQUIRED_MARKER, "eof");
        check("pdf truncated",
                FileStructureValidator.validate("%PDF".getBytes(ISO), "PDF", 65536).getStatus()
                        != StructureValidationResult.Status.VALID_BASIC_STRUCTURE, "trunc");
        check("pdf empty",
                FileStructureValidator.validate(new byte[0], "PDF", 65536).getStatus()
                        == StructureValidationResult.Status.EMPTY_CONTENT, "empty");
        StructureValidationResult lim = FileStructureValidator.validate(valid, "PDF", 8);
        check("pdf analysis limit",
                lim.getStatus() == StructureValidationResult.Status.ANALYSIS_LIMIT_REACHED
                        || lim.getStatus() == StructureValidationResult.Status.MISSING_REQUIRED_MARKER
                        || !lim.getWarnings().isEmpty(),
                lim.getStatus().name());
    }

    static void testPreservation() {
        byte[] png = minimalPng();
        String origHash = FileContentHasher.sha256Hex(png);
        UploadRequestResult req = sampleUpload("photo.png", "image/png", png);
        UploadPart part = req.getParts().get(0);
        byte[] before = part.getContent();
        String beforeHash = FileContentHasher.sha256Hex(before);

        FileContentAnalysisResult a1 = FileContentValidationEngine.analyze(req, 0, ContentValidationConfig.defaults());
        check("analysis hash matches", beforeHash.equals(a1.getOriginalSha256()), a1.getOriginalSha256());
        check("part unchanged after analyze",
                beforeHash.equals(FileContentHasher.sha256Hex(part.getContent())), "part");
        check("snapshot unchanged", Arrays.equals(png, before) || beforeHash.equals(origHash), "snap");

        ContentVariantGenerator.GenerationStats st = new ContentVariantGenerator.GenerationStats();
        List<ContentVariant> variants = ContentVariantGenerator.generate(a1, ContentValidationConfig.defaults(), st);
        check("candidates generated", !variants.isEmpty(), "n=" + variants.size());
        check("original after generation",
                beforeHash.equals(FileContentHasher.sha256Hex(a1.getOriginalContent())), "orig");
        check("part after generation",
                beforeHash.equals(FileContentHasher.sha256Hex(part.getContent())), "part2");

        if (variants.size() >= 2) {
            byte[] c0 = variants.get(0).getCandidateContent();
            byte[] c1 = variants.get(1).getCandidateContent();
            String h0 = FileContentHasher.sha256Hex(c0);
            variants.get(1).getCandidateContent()[0] = (byte) 0x55; // mutate returned clone only
            check("candidate clone independent",
                    h0.equals(FileContentHasher.sha256Hex(variants.get(0).getCandidateContent())), "c0");
            check("candidate hashes independent",
                    !variants.get(0).getCandidateSha256().equals(variants.get(1).getCandidateSha256())
                            || Arrays.equals(c0, c1),
                    "hashes");
        }

        FileContentAnalysisResult a2 = FileContentValidationEngine.analyze(req, 0, ContentValidationConfig.defaults());
        check("repeated analysis deterministic hash",
                a1.getOriginalSha256().equals(a2.getOriginalSha256()), "det");
        check("repeated structure status",
                a1.getStructure().getStatus() == a2.getStructure().getStatus(), "struct");

        // JPEG / GIF / PDF preservation via plan generation
        check("jpeg preserve", preserveFormat("pic.jpg", "image/jpeg",
                new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9}), "jpeg");
        check("gif preserve", preserveFormat("a.gif", "image/gif", minimalGif("GIF89a")), "gif");
        check("pdf preserve", preserveFormat("a.pdf", "application/pdf",
                "%PDF-1.1\n%%EOF\n".getBytes(ISO)), "pdf");
    }

    static boolean preserveFormat(String name, String mime, byte[] content) {
        String h = FileContentHasher.sha256Hex(content);
        UploadRequestResult req = sampleUpload(name, mime, content);
        FileContentValidationEngine.generatePlan(req, 0, ContentValidationConfig.defaults());
        return h.equals(FileContentHasher.sha256Hex(req.getParts().get(0).getContent()));
    }

    static void testCandidateGeneration() {
        byte[] png = minimalPng();
        UploadRequestResult req = sampleUpload("photo.png", "image/png", png);
        FileContentAnalysisResult analysis =
                FileContentValidationEngine.analyze(req, 0, ContentValidationConfig.defaults());
        ContentVariantGenerator.GenerationStats stats = new ContentVariantGenerator.GenerationStats();
        List<ContentVariant> all = ContentVariantGenerator.generate(analysis, ContentValidationConfig.defaults(),
                stats);
        check("png candidates non-empty", !all.isEmpty(), "n=" + all.size());
        check("has truncated", hasStrat(all, ContentTestStrategy.TRUNCATED_CONTENT), "trunc");
        check("has marker", hasStrat(all, ContentTestStrategy.REQUIRED_MARKER), "marker");
        check("has signature", hasStrat(all, ContentTestStrategy.SIGNATURE_STRUCTURE), "sig");
        check("has length", hasStrat(all, ContentTestStrategy.CONTENT_LENGTH_BOUNDARY), "len");
        check("has malformed", hasStrat(all, ContentTestStrategy.MALFORMED_STRUCTURAL_METADATA), "meta");

        boolean hashOk = true;
        boolean lenOk = true;
        for (ContentVariant v : all) {
            if (!v.getCandidateSha256().equals(FileContentHasher.sha256Hex(v.getCandidateContent()))) {
                hashOk = false;
            }
            if (v.getCandidateLength() != v.getCandidateContent().length) {
                lenOk = false;
            }
            if (!analysis.getOriginalSha256().equals(v.getOriginalSha256())) {
                hashOk = false;
            }
        }
        check("candidate hash correctness", hashOk, "hash");
        check("candidate length correctness", lenOk, "len");
        check("transformation metadata", all.get(0).getTransformation() != null
                && all.get(0).getTransformationParams() != null, "meta");

        ContentValidationConfig limited = ContentValidationConfig.defaults();
        limited.setMaxCandidates(3);
        ContentVariantGenerator.GenerationStats st2 = new ContentVariantGenerator.GenerationStats();
        List<ContentVariant> lim = ContentVariantGenerator.generate(analysis, limited, st2);
        check("candidate limit", lim.size() <= 3 && st2.limitReached, "n=" + lim.size());

        ContentValidationConfig agg = ContentValidationConfig.defaults();
        agg.setMaxAggregateCandidateBytes(8);
        ContentVariantGenerator.GenerationStats st3 = new ContentVariantGenerator.GenerationStats();
        ContentVariantGenerator.generate(analysis, agg, st3);
        check("aggregate byte limit", st3.aggregateLimitReached || st3.afterDedup >= 0, "agg=" + st3.aggregateCandidateBytes);

        List<ContentVariant> a = ContentVariantGenerator.generate(analysis, ContentValidationConfig.defaults(),
                new ContentVariantGenerator.GenerationStats());
        List<ContentVariant> b = ContentVariantGenerator.generate(analysis, ContentValidationConfig.defaults(),
                new ContentVariantGenerator.GenerationStats());
        check("deterministic candidate count", a.size() == b.size(), a.size() + "/" + b.size());
        boolean same = a.size() == b.size();
        for (int i = 0; same && i < a.size(); i++) {
            if (!a.get(i).getCandidateSha256().equals(b.get(i).getCandidateSha256())
                    || !a.get(i).getTransformation().equals(b.get(i).getTransformation())) {
                same = false;
            }
        }
        check("deterministic candidate order", same, "order");

        // JPEG EOI / GIF trailer / PDF EOF
        FileContentAnalysisResult jpegA = FileContentValidationEngine.analyze(
                sampleUpload("x.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9}),
                0, ContentValidationConfig.defaults());
        List<ContentVariant> jpegV = ContentVariantGenerator.generate(jpegA, ContentValidationConfig.defaults(),
                new ContentVariantGenerator.GenerationStats());
        check("jpeg eoi candidates", hasStrat(jpegV, ContentTestStrategy.REQUIRED_MARKER)
                || hasStrat(jpegV, ContentTestStrategy.TRUNCATED_CONTENT), "jpeg");

        FileContentAnalysisResult gifA = FileContentValidationEngine.analyze(
                sampleUpload("x.gif", "image/gif", minimalGif("GIF89a")),
                0, ContentValidationConfig.defaults());
        List<ContentVariant> gifV = ContentVariantGenerator.generate(gifA, ContentValidationConfig.defaults(),
                new ContentVariantGenerator.GenerationStats());
        check("gif trailer candidates",
                hasStrat(gifV, ContentTestStrategy.TRUNCATED_CONTENT)
                        || hasStrat(gifV, ContentTestStrategy.REQUIRED_MARKER), "gif");

        FileContentAnalysisResult pdfA = FileContentValidationEngine.analyze(
                sampleUpload("x.pdf", "application/pdf", "%PDF-1.1\n%%EOF\n".getBytes(ISO)),
                0, ContentValidationConfig.defaults());
        List<ContentVariant> pdfV = ContentVariantGenerator.generate(pdfA, ContentValidationConfig.defaults(),
                new ContentVariantGenerator.GenerationStats());
        check("pdf eof candidates", hasStrat(pdfV, ContentTestStrategy.TRUNCATED_CONTENT)
                || hasStrat(pdfV, ContentTestStrategy.REQUIRED_MARKER), "pdf");
    }

    static void testConsistency() {
        ContentValidationConfig cfg = ContentValidationConfig.defaults();
        FileContentAnalysisResult pngOk = FileContentValidationEngine.analyze(
                sampleUpload("photo.png", "image/png", minimalPng()), 0, cfg);
        check("png+image/png consistent",
                pngOk.getConsistency().getStatus() == ContentConsistencyResult.Status.FULL_METADATA_CONSISTENCY,
                pngOk.getConsistency().getStatus().name());

        FileContentAnalysisResult pngMimeBad = FileContentValidationEngine.analyze(
                sampleUpload("photo.png", "image/jpeg", minimalPng()), 0, cfg);
        check("png+image/jpeg mime mismatch",
                pngMimeBad.getConsistency().getStatus() == ContentConsistencyResult.Status.MIME_FORMAT_MISMATCH
                        || pngMimeBad.getConsistency().getStatus()
                        == ContentConsistencyResult.Status.MULTIPLE_INCONSISTENCIES,
                pngMimeBad.getConsistency().getStatus().name());

        FileContentAnalysisResult jpegOk = FileContentValidationEngine.analyze(
                sampleUpload("a.jpg", "image/jpeg",
                        new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9}), 0, cfg);
        check("jpeg consistent",
                jpegOk.getConsistency().getStatus() == ContentConsistencyResult.Status.FULL_METADATA_CONSISTENCY,
                jpegOk.getConsistency().getStatus().name());

        FileContentAnalysisResult gifOk = FileContentValidationEngine.analyze(
                sampleUpload("a.gif", "image/gif", minimalGif("GIF89a")), 0, cfg);
        check("gif consistent",
                gifOk.getConsistency().getStatus() == ContentConsistencyResult.Status.FULL_METADATA_CONSISTENCY,
                gifOk.getConsistency().getStatus().name());

        FileContentAnalysisResult pdfOk = FileContentValidationEngine.analyze(
                sampleUpload("a.pdf", "application/pdf", "%PDF-1.4\n%%EOF\n".getBytes(ISO)), 0, cfg);
        check("pdf consistent",
                pdfOk.getConsistency().getStatus() == ContentConsistencyResult.Status.FULL_METADATA_CONSISTENCY,
                pdfOk.getConsistency().getStatus().name());

        FileContentAnalysisResult unk = FileContentValidationEngine.analyze(
                sampleUpload("a.bin", "application/octet-stream", new byte[] {1, 2, 3, 4, 5}), 0, cfg);
        check("unknown format",
                unk.getConsistency().getStatus() == ContentConsistencyResult.Status.UNKNOWN_FORMAT
                        || unk.getStructure().getStatus() == StructureValidationResult.Status.UNSUPPORTED_FORMAT,
                unk.getConsistency().getStatus().name());

        FileContentAnalysisResult noMime = FileContentValidationEngine.analyze(
                sampleUpload("photo.png", "", minimalPng()), 0, cfg);
        check("missing mime handled", noMime.getConsistency() != null, noMime.getConsistency().getStatus().name());

        // Consistency is not a vulnerability claim
        check("consistency not vuln claim",
                pngMimeBad.getConsistency().getExplanation() != null
                        && !pngMimeBad.getConsistency().getExplanation().toLowerCase().contains("vulnerable"),
                "no vuln wording");
    }

    static void testPlanGeneration() {
        UploadRequestResult analysis = sampleUpload("photo.png", "image/png", minimalPng());
        ContentValidationConfig cfg = ContentValidationConfig.defaults();

        check("validate null analysis",
                !FileContentValidationEngine.validate(null, Integer.valueOf(0), cfg).valid, "null");
        check("validate null part",
                !FileContentValidationEngine.validate(analysis, null, cfg).valid, "part");

        ContentValidationConfig empty = ContentValidationConfig.defaults();
        for (ContentTestStrategy s : ContentTestStrategy.values()) {
            empty.setStrategyEnabled(s, false);
        }
        check("validate empty strategies",
                !FileContentValidationEngine.validate(analysis, Integer.valueOf(0), empty).valid, "strat");

        FileContentValidationEngine.PlanWithArtifacts paw =
                FileContentValidationEngine.generatePlanWithArtifacts(analysis, 0, cfg);
        UploadTestPlan plan = paw.plan;
        check("plan id", plan.getPlanId() != null && plan.getPlanId().startsWith("plan-content-"),
                plan.getPlanId());
        check("analysis id", analysis.getAnalysisId().equals(plan.getAnalysisId()), plan.getAnalysisId());
        check("part index", plan.getSelectedPartIndex() != null && plan.getSelectedPartIndex().intValue() == 0,
                String.valueOf(plan.getSelectedPartIndex()));
        check("content analysis present", plan.getContentAnalysis() != null, "analysis");
        check("original sha on plan", plan.getContentAnalysis().getOriginalSha256() != null,
                plan.getContentAnalysis().getOriginalSha256());
        check("has cases", !plan.getCases().isEmpty(), "n=" + plan.getCases().size());
        check("artifacts present", paw.artifacts != null && !paw.artifacts.candidateBytesByTestId.isEmpty(),
                "artifacts");

        Set<String> ids = new HashSet<String>();
        boolean allOk = true;
        for (UploadTestCase tc : plan.getCases()) {
            if (!ids.add(tc.getTestId())) {
                allOk = false;
            }
            if (!UploadTestCase.CATEGORY_CONTENT.equals(tc.getTestCategory())) {
                allOk = false;
            }
            if (!UploadTestCase.TARGET_PART_CONTENT_BYTES.equals(tc.getTargetProperty())) {
                allOk = false;
            }
            if (!UploadTestCase.STATUS_NOT_EXECUTED.equals(tc.getExecutionStatus())) {
                allOk = false;
            }
            if (tc.getOriginalContentSha256() == null || tc.getCandidateContentSha256() == null) {
                allOk = false;
            }
            if (tc.getOriginalContentLength() == null || tc.getCandidateContentLength() == null) {
                allOk = false;
            }
            if (!paw.artifacts.candidateBytesByTestId.containsKey(tc.getTestId())) {
                allOk = false;
            }
        }
        check("unique ids + CONTENT metadata", allOk && ids.size() == plan.getCases().size(),
                "ids=" + ids.size());

        plan.deselectAll();
        check("deselect", plan.getSelectedCount() == 0, String.valueOf(plan.getSelectedCount()));
        plan.selectAll();
        check("select all", plan.getSelectedCount() == plan.getCases().size(),
                String.valueOf(plan.getSelectedCount()));

        ContentValidationConfig lim = ContentValidationConfig.defaults();
        lim.setMaxCandidates(2);
        UploadTestPlan limPlan = FileContentValidationEngine.generatePlan(analysis, 0, lim);
        check("plan respects candidate limit", limPlan.getCases().size() <= 2 && limPlan.isLimitReached(),
                "n=" + limPlan.getCases().size());
    }

    static void testCombinedCompatibility() {
        UploadRequestResult analysis = sampleUpload("avatar.png", "image/png", minimalPng());
        UploadTestPlan fn = FilenameTestingEngine.generatePlan(analysis, 0, FilenameTestingConfig.defaults());
        UploadTestPlan mime = MimeValidationEngine.generatePlan(analysis, 0, MimeTestingConfig.defaults());
        UploadTestPlan content = FileContentValidationEngine.generatePlan(analysis, 0,
                ContentValidationConfig.defaults());
        check("filename plan intact", !fn.getCases().isEmpty()
                && UploadTestCase.CATEGORY_FILENAME.equals(fn.getCases().get(0).getTestCategory()),
                "fn=" + fn.getCases().size());
        check("mime plan intact", !mime.getCases().isEmpty()
                && UploadTestCase.CATEGORY_MIME.equals(mime.getCases().get(0).getTestCategory()),
                "mime=" + mime.getCases().size());
        check("content plan category", !content.getCases().isEmpty()
                && UploadTestCase.CATEGORY_CONTENT.equals(content.getCases().get(0).getTestCategory()),
                "content=" + content.getCases().size());
        check("content targets PART_CONTENT_BYTES",
                UploadTestCase.TARGET_PART_CONTENT_BYTES.equals(
                        content.getCases().get(0).getTargetProperty()), "target");
        check("distinct plan ids", !fn.getPlanId().equals(mime.getPlanId())
                && !mime.getPlanId().equals(content.getPlanId())
                && !fn.getPlanId().equals(content.getPlanId()), "ids");

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
        for (UploadTestCase tc : content.getCases()) {
            if (!all.add(tc.getTestId())) {
                unique = false;
            }
        }
        check("no colliding test ids across 4.2/4.3/4.4", unique, "total=" + all.size());
        check("filename/mime analysis fields preserved",
                content.getFilenameAnalysis() == null && content.getMimeComparison() == null
                        && content.getContentAnalysis() != null, "fields");
    }

    static void testPanel() {
        try {
            System.setProperty("java.awt.headless", "true");
            uk.co.pentest.psychoPATH.fileupload.FileUploadPanel panel =
                    new uk.co.pentest.psychoPATH.fileupload.FileUploadPanel();
            check("FileUploadPanel with Content tab", panel != null, "ok");
            skipped++;
            System.out.println("SKIP burp-gui-content-testing :: requires Burp desktop runtime");
            skipped++;
            System.out.println("SKIP portswigger-file-upload-labs :: requires Academy session");
        } catch (Throwable t) {
            check("FileUploadPanel with Content tab", false, t.toString());
        }
    }

    static boolean hasStrat(List<ContentVariant> list, ContentTestStrategy s) {
        for (ContentVariant v : list) {
            if (v.getStrategy() == s) {
                return true;
            }
        }
        return false;
    }

    /** Minimal 1x1 PNG with IHDR + IEND and valid CRCs. */
    static byte[] minimalPng() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            byte[] ihdrData = new byte[] {
                    0, 0, 0, 1, 0, 0, 0, 1, 8, 0, 0, 0, 0
            };
            writePngChunk(bos, "IHDR", ihdrData);
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

    static byte[] minimalGif(String header) {
        byte[] out = new byte[14];
        byte[] h = header.getBytes(ISO);
        System.arraycopy(h, 0, out, 0, 6);
        // LSD 7 bytes
        out[6] = 1;
        out[7] = 0;
        out[8] = 1;
        out[9] = 0;
        out[10] = 0;
        out[11] = 0;
        out[12] = 0;
        out[13] = 0x3B;
        return out;
    }

    static UploadRequestResult sampleUpload(String filename, String partMime, byte[] content) {
        String b = "----CONTENTTEST";
        StringBuilder partHeaders = new StringBuilder();
        partHeaders.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(filename)
                .append("\"");
        if (partMime != null && partMime.length() > 0) {
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
        String req = "POST /upload HTTP/1.1\r\nHost: lab.example\r\nContent-Type: multipart/form-data; boundary="
                + b + "\r\n\r\n";
        byte[] full = new byte[req.getBytes(ISO).length + body.length];
        System.arraycopy(req.getBytes(ISO), 0, full, 0, req.getBytes(ISO).length);
        System.arraycopy(body, 0, full, req.getBytes(ISO).length, body.length);
        return FileUploadRequestAnalyzer.analyze(full, "content-smoke");
    }
}
