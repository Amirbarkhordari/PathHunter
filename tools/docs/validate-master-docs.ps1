# Lightweight PathHunter documentation validator (no external deps).
# Exit 0 on success, 1 on failure.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path (Join-Path $root 'docs\PATHHUNTER_MASTER_DOCUMENTATION.md'))) {
    $root = (Get-Location).Path
}
$fail = 0
function Check([string]$name, [bool]$ok, [string]$detail) {
    if ($ok) {
        Write-Output "PASS $name :: $detail"
    } else {
        Write-Output "FAIL $name :: $detail"
        $script:fail++
    }
}

$master = Join-Path $root 'docs\PATHHUNTER_MASTER_DOCUMENTATION.md'
$readme = Join-Path $root 'README.md'
$changelog = Join-Path $root 'CHANGELOG.md'
$phasesDir = Join-Path $root 'docs\phases'

Check 'master-exists' (Test-Path $master) $master
Check 'changelog-exists' (Test-Path $changelog) $changelog
Check 'readme-exists' (Test-Path $readme) $readme

$masterText = Get-Content -Raw $master
$readmeText = Get-Content -Raw $readme

Check 'readme-links-master' ($readmeText -match 'PATHHUNTER_MASTER_DOCUMENTATION\.md') 'README -> master'
Check 'master-has-overview' ($masterText -match '## 1\. Project Overview') '§1'
Check 'master-has-architecture' ($masterText -match '## 2\. Project Architecture') '§2'
Check 'master-has-install' ($masterText -match '## 3\. Installation') '§3'
Check 'master-has-features' ($masterText -match '## 4\. Core Features') '§4'
Check 'master-has-upload' ($masterText -match '## 5\. File Upload') '§5'
Check 'master-has-testcase' ($masterText -match '## 6\. File Upload Test Case') '§6'
Check 'master-has-managed' ($masterText -match '## 7\. Managed Execution') '§7'
Check 'master-has-security' ($masterText -match '## 8\. Security') '§8'
Check 'master-has-testing' ($masterText -match '## 9\. Testing') '§9'
Check 'master-has-roadmap' ($masterText -match '## 10\. Development Roadmap') '§10'
Check 'master-has-history' ($masterText -match '## 11\. Development History') '§11'
Check 'master-has-limitations' ($masterText -match '## 12\. Known Limitations') '§12'
Check 'master-has-workflow' ($masterText -match '## 13\. Contribution') '§13'

Check 'phase-451-not-marked-http' (-not ($masterText -match '(?i)Phase 4\.5\.1[^\n]{0,80}sends HTTP')) '4.5.1 no HTTP claim'
Check 'phase-452-planned' ($masterText -match '4\.5\.2' -and $masterText -match 'NOT IMPLEMENTED') '4.5.2 planned'
Check 'commit-ref-present' ($masterText -match 'a4ebc82') 'latest commit referenced'

# No remaining phase-specific docs
$phaseFiles = @()
if (Test-Path $phasesDir) {
    $phaseFiles = @(Get-ChildItem $phasesDir -Filter 'phase-*.md' -File -ErrorAction SilentlyContinue)
}
Check 'no-phase-md-files' ($phaseFiles.Count -eq 0) ("count=" + $phaseFiles.Count)

# Broken links to deleted phase docs from key files
$scanFiles = @($readme, $changelog, $master) + @(Get-ChildItem (Join-Path $root 'docs') -Filter '*.md' -File)
$broken = @()
foreach ($f in $scanFiles) {
    if (-not (Test-Path $f)) { continue }
    $t = Get-Content -Raw $f
    if ($t -match 'docs/phases/phase-') {
        $broken += $f
    }
}
Check 'no-stale-phase-links' ($broken.Count -eq 0) ("files=" + ($broken -join ','))

# Required headings for completed phases mentioned
foreach ($p in @('Phase 2\.1', 'Phase 2\.2', 'Phase 4\.1', 'Phase 4\.2', 'Phase 4\.3', 'Phase 4\.4', 'Phase 4\.5\.1')) {
    Check ("mentions-$p") ($masterText -match $p) $p
}

Write-Output ("SUMMARY pass_checks={0} fail={1}" -f (22 + 7 - $fail), $fail)
if ($fail -gt 0) { exit 1 } else { exit 0 }
