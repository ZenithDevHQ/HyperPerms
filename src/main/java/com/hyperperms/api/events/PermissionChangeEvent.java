package com.hyperperms.api.events;

import com.hyperperms.api.PermissionHolder;
import com.hyperperms.model.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a permission is added or removed.
 */
public final class PermissionChangeEvent implements HyperPermsEvent {

    private final PermissionHolder holder;
    private final Node node;
    private final ChangeType changeType;

    /**
     * Creates a new permission change event.
     *
     * @param holder     the holder that was modified
     * @param node       the node that was added/removed
     * @param changeType the type of change
     */
    public PermissionChangeEvent(@NotNull PermissionHolder holder, @NotNull Node node,
                                  @NotNull ChangeType changeType) {
        this.holder = holder;
        this.node = node;
        this.changeType = changeType;
    }

    @Override
    public EventType getType() {
        return EventType.PERMISSION_CHANGE;
    }

    /**
     * Gets the permission holder that was modified.
     *
     * @return the holder
     */
    @NotNull
    public PermissionHolder getHolder() {
        return holder;
    }

    /**
     * Gets the node that was changed.
     *
     * @return the node
     */
    @NotNull
    public Node getNode() {
        return node;
    }

    /**
     * Gets the type of change.
     *
     * @return the change type
     */
    @NotNull
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * Types of permission changes.
     */
    public enum ChangeType {
        /**
         * A permission was added.
         */
        ADD,

        /**
         * A permission was removed.
         */
        REMOVE,

        /**
         * A permission was updated.
         */
        UPDATE,

        /**
         * All permissions were cleared.
         */
        CLEAR,

        /**
         * A permission expired and was automatically removed.
         */
        EXPIRE
    }

    @Override
    public String toString() {
        return "PermissionChangeEvent{holder=" + holder.getIdentifier() +
                ", node=" + node.getPermission() + ", changeType=" + changeType + "}";
    }
}
