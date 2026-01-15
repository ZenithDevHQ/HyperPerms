package com.hyperperms.api.events;

import com.hyperperms.registry.PermissionRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a permission is registered with the {@link PermissionRegistry}.
 * <p>
 * This event can be used to:
 * <ul>
 *   <li>Track which permissions are being registered by plugins</li>
 *   <li>Log permission registrations for debugging</li>
 *   <li>Update permission auto-completion caches</li>
 * </ul>
 */
public final class PermissionRegisterEvent implements HyperPermsEvent {

    private final String permission;
    private final String description;
    private final String category;
    private final String plugin;

    /**
     * Creates a new permission register event.
     *
     * @param permission  the permission node that was registered
     * @param description the permission description
     * @param category    the permission category
     * @param plugin      the plugin that registered the permission
     */
    public PermissionRegisterEvent(@NotNull String permission, @NotNull String description,
                                    @NotNull String category, @NotNull String plugin) {
        this.permission = permission;
        this.description = description;
        this.category = category;
        this.plugin = plugin;
    }

    @Override
    public EventType getType() {
        return EventType.PERMISSION_REGISTER;
    }

    /**
     * Gets the permission node that was registered.
     *
     * @return the permission node
     */
    @NotNull
    public String getPermission() {
        return permission;
    }

    /**
     * Gets the description of the registered permission.
     *
     * @return the description
     */
    @NotNull
    public String getDescription() {
        return description;
    }

    /**
     * Gets the category of the registered permission.
     *
     * @return the category
     */
    @NotNull
    public String getCategory() {
        return category;
    }

    /**
     * Gets the plugin that registered the permission.
     *
     * @return the plugin name
     */
    @NotNull
    public String getPlugin() {
        return plugin;
    }

    @Override
    public String toString() {
        return "PermissionRegisterEvent{permission=" + permission +
                ", category=" + category + ", plugin=" + plugin + "}";
    }
}
