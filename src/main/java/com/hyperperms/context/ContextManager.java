package com.hyperperms.context;

import com.hyperperms.api.context.ContextSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages context calculators and provides context resolution.
 */
public final class ContextManager {

    private final List<ContextCalculator> calculators = new CopyOnWriteArrayList<>();

    /**
     * Registers a context calculator.
     *
     * @param calculator the calculator to register
     */
    public void registerCalculator(@NotNull ContextCalculator calculator) {
        calculators.add(calculator);
    }

    /**
     * Unregisters a context calculator.
     *
     * @param calculator the calculator to unregister
     */
    public void unregisterCalculator(@NotNull ContextCalculator calculator) {
        calculators.remove(calculator);
    }

    /**
     * Calculates the current context for a player.
     *
     * @param uuid the player's UUID
     * @return the player's current contexts
     */
    @NotNull
    public ContextSet getContexts(@NotNull UUID uuid) {
        ContextSet.Builder builder = ContextSet.builder();
        for (ContextCalculator calculator : calculators) {
            try {
                calculator.calculate(uuid, builder);
            } catch (Exception e) {
                // Don't let one calculator break others
                e.printStackTrace();
            }
        }
        return builder.build();
    }

    /**
     * Gets the number of registered calculators.
     *
     * @return the count
     */
    public int getCalculatorCount() {
        return calculators.size();
    }

    /**
     * Clears all registered calculators.
     */
    public void clear() {
        calculators.clear();
    }
}
