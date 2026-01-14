package com.hyperperms.api.events;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.resolver.PermissionTrace;
import com.hyperperms.resolver.WildcardMatcher.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired when a permission is checked.
 * <p>
 * This event can be used for debugging and verbose mode. It includes
 * detailed trace information about where the permission came from.
 */
public final class PermissionCheckEvent implements HyperPermsEvent {

    private final UUID uuid;
    private final String permission;
    private final ContextSet contexts;
    private final TriState result;
    private final String source;
    @Nullable
    private final PermissionTrace trace;

    /**
     * Creates a new permission check event.
     *
     * @param uuid       the user UUID
     * @param permission the permission checked
     * @param contexts   the contexts
     * @param result     the result
     * @param source     where the permission came from
     */
    public PermissionCheckEvent(@NotNull UUID uuid, @NotNull String permission,
                                 @NotNull ContextSet contexts, @NotNull TriState result,
                                 @NotNull String source) {
        this(uuid, permission, contexts, result, source, null);
    }

    /**
     * Creates a new permission check event with trace information.
     *
     * @param uuid       the user UUID
     * @param permission the permission checked
     * @param contexts   the contexts
     * @param result     the result
     * @param source     where the permission came from
     * @param trace      detailed trace information
     */
    public PermissionCheckEvent(@NotNull UUID uuid, @NotNull String permission,
                                 @NotNull ContextSet contexts, @NotNull TriState result,
                                 @NotNull String source, @Nullable PermissionTrace trace) {
        this.uuid = uuid;
        this.permission = permission;
        this.contexts = contexts;
        this.result = result;
        this.source = source;
        this.trace = trace;
    }

    @Override
    public EventType getType() {
        return EventType.PERMISSION_CHECK;
    }

    /**
     * Gets the user UUID.
     *
     * @return the UUID
     */
    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the permission that was checked.
     *
     * @return the permission
     */
    @NotNull
    public String getPermission() {
        return permission;
    }

    /**
     * Gets the contexts the check was performed in.
     *
     * @return the contexts
     */
    @NotNull
    public ContextSet getContexts() {
        return contexts;
    }

    /**
     * Gets the result of the check.
     *
     * @return the result
     */
    @NotNull
    public TriState getResult() {
        return result;
    }

    /**
     * Gets the source of the permission (e.g., group name, "user", etc.).
     *
     * @return the source
     */
    @NotNull
    public String getSource() {
        return source;
    }

    /**
     * Gets the detailed permission trace, if available.
     * <p>
     * The trace is only populated in verbose mode and contains detailed
     * information about how the permission was resolved.
     *
     * @return the trace, or null if not in verbose mode
     */
    @Nullable
    public PermissionTrace getTrace() {
        return trace;
    }

    /**
     * Checks if this event has trace information.
     *
     * @return true if trace is available
     */
    public boolean hasTrace() {
        return trace != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PermissionCheckEvent{uuid=");
        sb.append(uuid);
        sb.append(", permission='").append(permission).append('\'');
        sb.append(", result=").append(result);
        if (trace != null) {
            sb.append(", trace=").append(trace);
        }
        sb.append('}');
        return sb.toString();
    }
}
