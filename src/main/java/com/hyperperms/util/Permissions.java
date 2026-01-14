package com.hyperperms.util;

/**
 * Constants for HyperPerms' own permission nodes.
 * <p>
 * These permissions control who can use HyperPerms commands and features.
 */
public final class Permissions {

    private Permissions() {}

    /**
     * Base permission for all HyperPerms commands.
     */
    public static final String BASE = "hyperperms.command";

    /**
     * Permission to use any HyperPerms command (admin wildcard).
     */
    public static final String ADMIN = "hyperperms.*";

    // ==================== User Commands ====================

    /**
     * Permission to view user information.
     */
    public static final String USER_INFO = "hyperperms.command.user.info";

    /**
     * Permission to modify user permissions.
     */
    public static final String USER_PERMISSION = "hyperperms.command.user.permission";

    /**
     * Permission to modify user groups.
     */
    public static final String USER_GROUP = "hyperperms.command.user.group";

    /**
     * Permission to promote users.
     */
    public static final String USER_PROMOTE = "hyperperms.command.user.promote";

    /**
     * Permission to demote users.
     */
    public static final String USER_DEMOTE = "hyperperms.command.user.demote";

    /**
     * Permission to clear user permissions.
     */
    public static final String USER_CLEAR = "hyperperms.command.user.clear";

    /**
     * Permission to clone user permissions.
     */
    public static final String USER_CLONE = "hyperperms.command.user.clone";

    // ==================== Group Commands ====================

    /**
     * Permission to view group information.
     */
    public static final String GROUP_INFO = "hyperperms.command.group.info";

    /**
     * Permission to create groups.
     */
    public static final String GROUP_CREATE = "hyperperms.command.group.create";

    /**
     * Permission to delete groups.
     */
    public static final String GROUP_DELETE = "hyperperms.command.group.delete";

    /**
     * Permission to modify group permissions.
     */
    public static final String GROUP_PERMISSION = "hyperperms.command.group.permission";

    /**
     * Permission to modify group parents.
     */
    public static final String GROUP_PARENT = "hyperperms.command.group.parent";

    /**
     * Permission to modify group settings.
     */
    public static final String GROUP_MODIFY = "hyperperms.command.group.modify";

    // ==================== Track Commands ====================

    /**
     * Permission to view track information.
     */
    public static final String TRACK_INFO = "hyperperms.command.track.info";

    /**
     * Permission to create tracks.
     */
    public static final String TRACK_CREATE = "hyperperms.command.track.create";

    /**
     * Permission to delete tracks.
     */
    public static final String TRACK_DELETE = "hyperperms.command.track.delete";

    /**
     * Permission to modify tracks.
     */
    public static final String TRACK_MODIFY = "hyperperms.command.track.modify";

    // ==================== Admin Commands ====================

    /**
     * Permission to reload the plugin.
     */
    public static final String RELOAD = "hyperperms.command.reload";

    /**
     * Permission to use verbose mode.
     */
    public static final String VERBOSE = "hyperperms.command.verbose";

    /**
     * Permission to clear the cache.
     */
    public static final String CACHE = "hyperperms.command.cache";

    /**
     * Permission to export data.
     */
    public static final String EXPORT = "hyperperms.command.export";

    /**
     * Permission to import data.
     */
    public static final String IMPORT = "hyperperms.command.import";

    /**
     * Permission to list all groups.
     */
    public static final String LIST_GROUPS = "hyperperms.command.listgroups";

    /**
     * Permission to list all tracks.
     */
    public static final String LIST_TRACKS = "hyperperms.command.listtracks";

    // ==================== GUI Permissions ====================

    /**
     * Permission to use the GUI.
     */
    public static final String GUI = "hyperperms.gui";

    /**
     * Permission to use the user editor GUI.
     */
    public static final String GUI_USER = "hyperperms.gui.user";

    /**
     * Permission to use the group editor GUI.
     */
    public static final String GUI_GROUP = "hyperperms.gui.group";
}
