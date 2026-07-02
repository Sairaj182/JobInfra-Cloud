package com.sairaj.jobinfra.server.tracking.core;

/**
 * Interface for generating unique visitor identifiers.
 * This allows swapping out UUID generation with signed or encrypted IDs in the future.
 */
public interface VisitorIdentifier {
    
    /**
     * Generates a new unique identifier.
     * @return the newly generated identifier string
     */
    String generateId();
    
    /**
     * Validates if the given string is a valid identifier.
     * @param id the identifier to validate
     * @return true if valid, false otherwise
     */
    boolean isValid(String id);
}
