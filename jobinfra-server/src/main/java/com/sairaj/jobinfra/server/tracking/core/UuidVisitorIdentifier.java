package com.sairaj.jobinfra.server.tracking.core;

import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * UUID v4 implementation of the VisitorIdentifier.
 */
@Component
public class UuidVisitorIdentifier implements VisitorIdentifier {

    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean isValid(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
