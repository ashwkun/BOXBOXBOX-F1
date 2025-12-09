#!/bin/bash
# F1 Codebase Optimization Audit Script
# Run from project root: ./audit_optimizations.sh

echo "═══════════════════════════════════════════════════════════════════"
echo "F1 CODEBASE OPTIMIZATION AUDIT"
echo "═══════════════════════════════════════════════════════════════════"
echo ""

SRC_DIR="app/src/main/java/com/f1tracker"

# Check 1: Hardcoded year values
echo "🔍 [1/7] Hardcoded Year Values (should use SeasonConfig)"
echo "─────────────────────────────────────────────────────────"
grep -rn '"2025"' "$SRC_DIR" --include="*.kt" | grep -v "SeasonConfig" | grep -v "// OK:" | head -20
echo ""

# Check 2: Hardcoded team colors
echo "🔍 [2/7] Hardcoded Team Colors (should use F1DataProvider)"
echo "─────────────────────────────────────────────────────────"
grep -rn 'Color(0xFF' "$SRC_DIR" --include="*.kt" | grep -iE "(red.*bull|ferrari|mercedes|mclaren|alpine|williams|haas|sauber|aston)" | head -20
echo ""

# Check 3: Hardcoded team name mappings (when expressions)
echo "🔍 [3/7] Hardcoded Team Mappings"
echo "─────────────────────────────────────────────────────────"
grep -rn 'when.*{' "$SRC_DIR" --include="*.kt" -A5 | grep -E '"(red_bull|ferrari|mercedes|mclaren|alpine)"' | head -10
echo ""

# Check 4: Hardcoded driver codes/names
echo "🔍 [4/7] Hardcoded Driver Codes/Names"
echo "─────────────────────────────────────────────────────────"
grep -rn '"VER"\|"HAM"\|"LEC"\|"NOR"\|"SAI"' "$SRC_DIR" --include="*.kt" | grep -v "test" | head -10
echo ""

# Check 5: R.drawable references (should be URLs from JSON)
echo "🔍 [5/7] Hardcoded Drawable References"
echo "─────────────────────────────────────────────────────────"
grep -rn 'R.drawable\.' "$SRC_DIR" --include="*.kt" | grep -v "ic_" | grep -v "notification" | head -20
echo ""

# Check 6: Duplicate function patterns
echo "🔍 [6/7] Potential Duplicate Functions (getTeamColor, getDriverCode)"
echo "─────────────────────────────────────────────────────────"
grep -rn 'fun getTeamColor\|fun getDriverCode\|fun generateDriverCode' "$SRC_DIR" --include="*.kt"
echo ""

# Check 7: Files not using F1DataProvider
echo "🔍 [7/7] UI Files Potentially Missing F1DataProvider Usage"
echo "─────────────────────────────────────────────────────────"
for file in $(find "$SRC_DIR/ui" -name "*.kt" -type f); do
    if ! grep -q "F1DataProvider" "$file"; then
        basename "$file"
    fi
done
echo ""

echo "═══════════════════════════════════════════════════════════════════"
echo "SUMMARY"
echo "═══════════════════════════════════════════════════════════════════"
echo ""
echo "Files with hardcoded '2025': $(grep -rn '"2025"' "$SRC_DIR" --include="*.kt" | grep -v SeasonConfig | wc -l)"
echo "Files with R.drawable (non-icon): $(grep -rn 'R.drawable\.' "$SRC_DIR" --include="*.kt" | grep -v "ic_" | wc -l)"
echo "Duplicate getTeamColor functions: $(grep -rn 'fun getTeamColor' "$SRC_DIR" --include="*.kt" | wc -l)"
echo ""
echo "Run with | grep -v '// OK' to filter known-good lines"
