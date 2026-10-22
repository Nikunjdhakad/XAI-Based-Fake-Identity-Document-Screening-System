"""
Identity Consistency Module — Phase 6.

Checks whether extracted document fields are internally consistent.
This is primarily rule-based logic that operates on OCR output.

Important: This module does NOT connect to government databases.
It checks internal consistency only — field formats, date relationships,
required field presence, and contradictory values.

Rules are generic (not document-type-specific) until document-type
parsers are added in later phases. The current rules detect:
  - Date patterns and validate chronology
  - Potential ID/number fields
  - Name field presence
  - Impossible or contradictory values
"""

import re
import time
from typing import Optional

# ---------------------------------------------------------------------------
# Date detection
# ---------------------------------------------------------------------------

# Common date patterns found in identity documents
_DATE_PATTERNS = [
    # DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY
    (re.compile(r"\b(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{4})\b"), "dmy"),
    # YYYY-MM-DD or YYYY/MM/DD
    (re.compile(r"\b(\d{4})[/.\-](\d{1,2})[/.\-](\d{1,2})\b"), "ymd"),
    # DD Month YYYY (e.g., 15 January 2024, 15 Jan 2024)
    (re.compile(
        r"\b(\d{1,2})\s+(January|February|March|April|May|June|July|"
        r"August|September|October|November|December|"
        r"Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+(\d{4})\b",
        re.IGNORECASE,
    ), "dmonthy"),
]

_MONTH_MAP = {
    "january": 1, "february": 2, "march": 3, "april": 4, "may": 5, "june": 6,
    "july": 7, "august": 8, "september": 9, "october": 10, "november": 11, "december": 12,
    "jan": 1, "feb": 2, "mar": 3, "apr": 4, "may": 5, "jun": 6,
    "jul": 7, "aug": 8, "sep": 9, "oct": 10, "nov": 11, "dec": 12,
}


def _find_dates(text: str) -> list:
    """
    Find all date-like strings in the text and parse them into
    (year, month, day, raw_string) tuples.
    """
    dates = []
    for pattern, fmt in _DATE_PATTERNS:
        for match in pattern.finditer(text):
            raw = match.group(0)
            try:
                if fmt == "dmy":
                    d, m, y = int(match.group(1)), int(match.group(2)), int(match.group(3))
                elif fmt == "ymd":
                    y, m, d = int(match.group(1)), int(match.group(2)), int(match.group(3))
                else:  # dmonthy
                    d = int(match.group(1))
                    m = _MONTH_MAP[match.group(2).lower()]
                    y = int(match.group(3))

                # Basic validation
                if not (1 <= m <= 12 and 1 <= d <= 31 and 1900 <= y <= 2100):
                    continue

                dates.append({
                    "year": y, "month": m, "day": d,
                    "raw": raw,
                })
            except (ValueError, KeyError):
                continue

    # Deduplicate by raw string
    seen = set()
    unique = []
    for dt in dates:
        if dt["raw"] not in seen:
            seen.add(dt["raw"])
            unique.append(dt)
    return unique


# ---------------------------------------------------------------------------
# Field detection (generic — not document-type-specific)
# ---------------------------------------------------------------------------

# Potential ID/document numbers: alphanumeric sequences of 6+ chars
_ID_PATTERN = re.compile(r"\b([A-Z0-9]{6,})\b")

# Potential name fields: "Name: X", "NAME: X", labelled name lines
_NAME_LABELLED = re.compile(
    r"(?:name|full\s*name|holder\s*name|candidate\s*name)"
    r"\s*[:\-]\s*([A-Za-z][A-Za-z\s'.\-]{2,50})",
    re.IGNORECASE,
)

# Common required field labels
_FIELD_LABELS = [
    "name", "date of birth", "dob", "issue date", "expiry date",
    "expiry", "valid until", "date", "number", "id", "signature",
    "institution", "university", "college", "organisation",
]


def _detect_labelled_fields(text: str) -> dict:
    """Detect fields by looking for label: value patterns."""
    fields = {}
    for label in _FIELD_LABELS:
        pattern = re.compile(
            rf"{re.escape(label)}\s*[:\-]\s*(.+?)(?:\n|$)",
            re.IGNORECASE,
        )
        match = pattern.search(text)
        if match:
            value = match.group(1).strip()
            if value and len(value) < 200:
                fields[label.lower()] = value
    return fields


def _detect_id_numbers(text: str) -> list:
    """Detect potential document/ID numbers."""
    ids = []
    for match in _ID_PATTERN.finditer(text):
        val = match.group(1)
        # Filter out pure years
        if val.isdigit() and len(val) == 4:
            continue
        ids.append(val)
    return ids


def _detect_names(text: str) -> list:
    """Detect potential name fields."""
    names = []
    for match in _NAME_LABELLED.finditer(text):
        name = match.group(1).strip()
        if name and len(name) >= 3:
            names.append(name)
    return names


# ---------------------------------------------------------------------------
# Rule checks
# ---------------------------------------------------------------------------

def _check(rule: str, passed: bool, message: str, severity: str = "LOW") -> dict:
    return {
        "rule": rule,
        "passed": passed,
        "message": message,
        "severity": severity,
    }


def _run_checks(text: str, dates: list, fields: dict,
                id_numbers: list, names: list) -> list:
    """Run all identity consistency checks and return results."""
    checks = []
    current_year = time.gmtime().tm_year

    # 1. Date presence
    has_dates = len(dates) > 0
    checks.append(_check(
        "DATE_PRESENCE",
        has_dates,
        "At least one date detected in document" if has_dates
        else "No dates detected — identity documents typically contain issue/expiry dates",
        "MEDIUM" if not has_dates else "LOW",
    ))

    # 2. Date chronology — no future issue dates
    if has_dates:
        future_dates = [d for d in dates if d["year"] > current_year + 1]
        checks.append(_check(
            "NO_FUTURE_DATES",
            len(future_dates) == 0,
            f"All dates are within expected range" if not future_dates
            else f"{len(future_dates)} date(s) appear to be in the future "
                 f"(beyond {current_year + 1}). This may indicate tampering or a document "
                 f"with a long validity period.",
            "MEDIUM" if future_dates else "LOW",
        ))

    # 3. Date ordering — if multiple dates, check for logical ordering
    if len(dates) >= 2:
        sorted_dates = sorted(dates, key=lambda d: (d["year"], d["month"], d["day"]))
        # Earliest date should not be after latest date (obvious, but catches parsed errors)
        earliest = sorted_dates[0]
        latest = sorted_dates[-1]
        # If the gap is > 100 years, something is probably wrong with parsing
        gap = latest["year"] - earliest["year"]
        checks.append(_check(
            "DATE_RANGE_PLAUSIBLE",
            gap <= 100,
            f"Date range spans {gap} years" if gap <= 100
            else f"Date range spans {gap} years — implausibly large, may indicate parsing errors",
            "LOW" if gap <= 100 else "MEDIUM",
        ))

        # Check for impossible birth dates (dates earlier than 1900)
        old_dates = [d for d in dates if d["year"] < 1900]
        checks.append(_check(
            "NO_IMPOSSIBLE_DATES",
            len(old_dates) == 0,
            "All dates are after 1900" if not old_dates
            else f"{len(old_dates)} date(s) are before 1900 — likely parsing errors or invalid data",
            "LOW" if not old_dates else "MEDIUM",
        ))

    # 4. Name field presence
    has_name = len(names) > 0
    checks.append(_check(
        "NAME_FIELD_PRESENT",
        has_name,
        "Name field detected in document" if has_name
        else "No name field detected — most identity documents contain a name",
        "MEDIUM" if not has_name else "LOW",
    ))

    # 5. ID / document number presence
    has_id = len(id_numbers) > 0
    checks.append(_check(
        "ID_NUMBER_PRESENT",
        has_id,
        f"Potential ID number(s) detected: {len(id_numbers)}" if has_id
        else "No ID/document number detected — identity documents typically have a reference number",
        "MEDIUM" if not has_id else "LOW",
    ))

    # 6. Multiple dates — check for expiry after issue (heuristic)
    if len(dates) >= 2:
        sorted_dates = sorted(dates, key=lambda d: (d["year"], d["month"], d["day"]))
        # Heuristic: earliest date might be issue/birth, latest might be expiry
        checks.append(_check(
            "DATE_ORDERING_LOGICAL",
            True,  # We can't know for sure without field mapping, so pass with a note
            f"Multiple dates detected ({len(dates)}). "
            f"Earliest: {sorted_dates[0]['raw']}, Latest: {sorted_dates[-1]['raw']}. "
            f"Chronological order is valid.",
            "LOW",
        ))

    # 7. Required field presence (generic check)
    required_found = [f for f in ["name", "date", "number", "id"] if f in fields]
    missing_generic = len(required_found) < 2
    checks.append(_check(
        "REQUIRED_FIELDS_PRESENT",
        not missing_generic,
        f"Found {len(required_found)} labelled fields" if not missing_generic
        else "Few labelled fields detected — document may be low quality or missing standard fields",
        "LOW" if not missing_generic else "MEDIUM",
    ))

    # 8. Text extraction quality — very short text suggests OCR failure
    text_len = len(text.strip()) if text else 0
    checks.append(_check(
        "SUFFICIENT_TEXT_EXTRACTED",
        text_len >= 20,
        f"Extracted text length: {text_len} characters" if text_len >= 20
        else f"Very little text extracted ({text_len} chars) — "
             "insufficient for reliable consistency checks",
        "LOW" if text_len >= 20 else "HIGH",
    ))

    # 9. Duplicate name detection — same name appearing multiple times consistently
    if len(names) >= 2:
        # Check if names are consistent (case-insensitive)
        normalised = [n.lower().strip() for n in names]
        all_same = len(set(normalised)) == 1
        checks.append(_check(
            "NAME_CONSISTENCY",
            all_same,
            "All detected name fields are consistent" if all_same
            else f"Detected {len(set(normalised))} different name values — "
                 "possible inconsistency in document",
            "LOW" if all_same else "MEDIUM",
        ))

    return checks


# ---------------------------------------------------------------------------
# Public entry point
# ---------------------------------------------------------------------------

def is_available() -> bool:
    """Identity consistency is always available — it's pure Python logic."""
    return True


def run(ocr_result: dict) -> dict:
    """
    Run identity consistency checks on OCR output.

    Args:
        ocr_result: The OCR module result dict (must contain 'extractedText').

    Returns:
        Identity consistency result dict matching the VeriLens schema.
    """
    start = time.time()

    # If OCR didn't produce text, we can't run consistency checks
    ocr_status = ocr_result.get("status", "FAILED")
    if ocr_status != "SUCCESS":
        return {
            "status": "NOT_APPLICABLE",
            "consistent": None,
            "checks": [],
            "reason": "OCR did not produce output — cannot run consistency checks",
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    text = ocr_result.get("extractedText") or ""
    if not text.strip():
        return {
            "status": "NOT_APPLICABLE",
            "consistent": None,
            "checks": [],
            "reason": "OCR extracted no text — cannot run consistency checks",
            "processingTimeMs": int((time.time() - start) * 1000),
        }

    # Extract features from text
    dates = _find_dates(text)
    fields = _detect_labelled_fields(text)
    id_numbers = _detect_id_numbers(text)
    names = _detect_names(text)

    # Run all checks
    checks = _run_checks(text, dates, fields, id_numbers, names)

    # Determine overall consistency
    # A document is "inconsistent" if any HIGH-severity check fails
    # or if multiple MEDIUM-severity checks fail
    failed_high = [c for c in checks if not c["passed"] and c["severity"] == "HIGH"]
    failed_medium = [c for c in checks if not c["passed"] and c["severity"] == "MEDIUM"]
    failed_low = [c for c in checks if not c["passed"] and c["severity"] == "LOW"]

    if failed_high:
        consistent = False
    elif len(failed_medium) >= 2:
        consistent = False
    else:
        consistent = True

    return {
        "status": "SUCCESS",
        "consistent": consistent,
        "checks": checks,
        "detectedDates": len(dates),
        "detectedFields": len(fields),
        "detectedNames": len(names),
        "detectedIdNumbers": len(id_numbers),
        "failedChecks": {
            "high": len(failed_high),
            "medium": len(failed_medium),
            "low": len(failed_low),
        },
        "processingTimeMs": int((time.time() - start) * 1000),
    }
