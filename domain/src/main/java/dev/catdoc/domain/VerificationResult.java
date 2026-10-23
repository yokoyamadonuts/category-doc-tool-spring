package dev.catdoc.domain;

import java.util.List;

/** 検証の結果。誤りがなければ正しい。警告は正しさに影響しない。 */
public record VerificationResult(List<String> errors, List<String> warnings) {

    public VerificationResult {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
