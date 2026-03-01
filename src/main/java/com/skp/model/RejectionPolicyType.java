package com.skp.model;

public enum RejectionPolicyType {
    ABORT,
    CALLER_RUNS,
    DISCARD_OLDEST;

    public static RejectionPolicyType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ABORT;
        }
        String normalized = value.trim().toUpperCase();
        for (RejectionPolicyType policy : values()) {
            if (policy.name().equals(normalized)) {
                return policy;
            }
        }
        throw new IllegalArgumentException("Unsupported rejection policy: " + value);
    }
}
