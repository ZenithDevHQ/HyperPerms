package com.hyperperms.api.events;

/**
 * Base interface for all HyperPerms events.
 */
public interface HyperPermsEvent {

    /**
     * Gets the event type.
     *
     * @return the type
     */
    EventType getType();

    /**
     * Types of events fired by HyperPerms.
     */
    enum EventType {
        /**
         * Fired when a permission is checked.
         */
        PERMISSION_CHECK,

        /**
         * Fired when a permission is changed.
         */
        PERMISSION_CHANGE,

        /**
         * Fired when a permission is registered with the registry.
         */
        PERMISSION_REGISTER,

        /**
         * Fired when a group is created.
         */
        GROUP_CREATE,

        /**
         * Fired when a group is deleted.
         */
        GROUP_DELETE,

        /**
         * Fired when a user's group membership changes.
         */
        USER_GROUP_CHANGE,

        /**
         * Fired when a player's context changes (world, gamemode, etc.).
         */
        CONTEXT_CHANGE,

        /**
         * Fired when data is reloaded.
         */
        DATA_RELOAD
    }
}
