package com.hyperperms.api.events;

import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple event bus for HyperPerms events.
 */
public final class EventBus {

    private final Map<Class<? extends HyperPermsEvent>, List<Consumer<? extends HyperPermsEvent>>> handlers;

    public EventBus() {
        this.handlers = new ConcurrentHashMap<>();
    }

    /**
     * Subscribes to an event type.
     *
     * @param eventClass the event class
     * @param handler    the handler
     * @param <T>        the event type
     * @return a subscription that can be used to unsubscribe
     */
    @NotNull
    public <T extends HyperPermsEvent> Subscription subscribe(@NotNull Class<T> eventClass,
                                                               @NotNull Consumer<T> handler) {
        handlers.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(handler);
        return () -> unsubscribe(eventClass, handler);
    }

    /**
     * Unsubscribes a handler from an event type.
     *
     * @param eventClass the event class
     * @param handler    the handler
     * @param <T>        the event type
     */
    public <T extends HyperPermsEvent> void unsubscribe(@NotNull Class<T> eventClass,
                                                         @NotNull Consumer<T> handler) {
        List<Consumer<? extends HyperPermsEvent>> list = handlers.get(eventClass);
        if (list != null) {
            list.remove(handler);
        }
    }

    /**
     * Fires an event to all subscribers.
     *
     * @param event the event to fire
     * @param <T>   the event type
     */
    @SuppressWarnings("unchecked")
    public <T extends HyperPermsEvent> void fire(@NotNull T event) {
        List<Consumer<? extends HyperPermsEvent>> list = handlers.get(event.getClass());
        if (list != null) {
            for (Consumer<? extends HyperPermsEvent> handler : list) {
                try {
                    ((Consumer<T>) handler).accept(event);
                } catch (Exception e) {
                    // Log but don't propagate to avoid disrupting other handlers
                    Logger.severe("Exception in event handler for " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * Clears all subscriptions.
     */
    public void clear() {
        handlers.clear();
    }

    /**
     * Represents a subscription that can be cancelled.
     */
    @FunctionalInterface
    public interface Subscription {
        /**
         * Cancels this subscription.
         */
        void unsubscribe();
    }
}
