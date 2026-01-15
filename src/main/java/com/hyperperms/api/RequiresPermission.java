package com.hyperperms.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods or types that require specific permissions.
 * <p>
 * This annotation serves as documentation and can be processed at runtime
 * by frameworks or custom code to automatically enforce permission checks.
 * <p>
 * Example usage:
 * <pre>
 * &#64;RequiresPermission("admin.kick")
 * public void kickPlayer(Player player) {
 *     // Implementation
 * }
 *
 * &#64;RequiresPermission(value = "build.place", mode = Mode.ANY, contexts = {"world:creative"})
 * public void placeBlock(Location loc) {
 *     // Implementation
 * }
 *
 * &#64;RequiresPermission(value = {"mod.ban", "mod.kick"}, mode = Mode.ANY)
 * public void punishPlayer(Player player) {
 *     // Implementation - requires EITHER permission
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresPermission {

    /**
     * The permission(s) required for this method or type.
     * <p>
     * When multiple permissions are specified, the {@link #mode()} determines
     * whether ALL or ANY of them are required.
     *
     * @return the required permission nodes
     */
    String[] value();

    /**
     * The mode for evaluating multiple permissions.
     * <p>
     * Default is {@link Mode#ALL}, meaning all specified permissions must be held.
     *
     * @return the evaluation mode
     */
    Mode mode() default Mode.ALL;

    /**
     * Optional context requirements in "key:value" format.
     * <p>
     * Examples: "world:nether", "gamemode:survival", "server:lobby"
     *
     * @return context requirements
     */
    String[] contexts() default {};

    /**
     * Custom message to display when permission check fails.
     * <p>
     * If empty, a default message will be used.
     *
     * @return the denial message
     */
    String message() default "";

    /**
     * Whether to silently deny (no message) or inform the player.
     *
     * @return true to suppress denial message
     */
    boolean silent() default false;

    /**
     * Mode for evaluating multiple permission requirements.
     */
    enum Mode {
        /**
         * All specified permissions must be held.
         */
        ALL,

        /**
         * At least one of the specified permissions must be held.
         */
        ANY
    }
}
