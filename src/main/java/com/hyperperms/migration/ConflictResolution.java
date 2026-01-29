package com.hyperperms.migration;

/**
 * Strategies for handling conflicts during migration.
 */
public enum ConflictResolution {
    
    /**
     * Merge permissions from both sources.
     * For groups: combines permissions from both, HyperPerms values take precedence for same node.
     * For users: same behavior.
     */
    MERGE,
    
    /**
     * Skip conflicting items entirely.
     * If a group/user already exists in HyperPerms, don't import from source.
     */
    SKIP,
    
    /**
     * Overwrite HyperPerms data with source data.
     * Replaces existing groups/users with imported data.
     */
    OVERWRITE
}
