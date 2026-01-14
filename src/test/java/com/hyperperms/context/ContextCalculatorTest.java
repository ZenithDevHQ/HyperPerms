package com.hyperperms.context;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.context.calculators.GameModeContextCalculator;
import com.hyperperms.context.calculators.ServerContextCalculator;
import com.hyperperms.context.calculators.WorldContextCalculator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContextCalculatorTest {

    private static final UUID TEST_UUID = UUID.randomUUID();

    @Test
    void testWorldContextCalculator() {
        PlayerContextProvider provider = new TestContextProvider("overworld", "survival");
        WorldContextCalculator calculator = new WorldContextCalculator(provider);

        ContextSet.Builder builder = ContextSet.builder();
        calculator.calculate(TEST_UUID, builder);
        ContextSet contexts = builder.build();

        assertTrue(contexts.containsKey("world"));
        assertEquals("overworld", contexts.getValue("world"));
    }

    @Test
    void testWorldContextCalculatorNullWorld() {
        PlayerContextProvider provider = new TestContextProvider(null, "survival");
        WorldContextCalculator calculator = new WorldContextCalculator(provider);

        ContextSet.Builder builder = ContextSet.builder();
        calculator.calculate(TEST_UUID, builder);
        ContextSet contexts = builder.build();

        assertFalse(contexts.containsKey("world"));
        assertTrue(contexts.isEmpty());
    }

    @Test
    void testGameModeContextCalculator() {
        PlayerContextProvider provider = new TestContextProvider("overworld", "creative");
        GameModeContextCalculator calculator = new GameModeContextCalculator(provider);

        ContextSet.Builder builder = ContextSet.builder();
        calculator.calculate(TEST_UUID, builder);
        ContextSet contexts = builder.build();

        assertTrue(contexts.containsKey("gamemode"));
        assertEquals("creative", contexts.getValue("gamemode"));
    }

    @Test
    void testGameModeContextCalculatorNullMode() {
        PlayerContextProvider provider = new TestContextProvider("overworld", null);
        GameModeContextCalculator calculator = new GameModeContextCalculator(provider);

        ContextSet.Builder builder = ContextSet.builder();
        calculator.calculate(TEST_UUID, builder);
        ContextSet contexts = builder.build();

        assertFalse(contexts.containsKey("gamemode"));
        assertTrue(contexts.isEmpty());
    }

    @Test
    void testServerContextCalculator() {
        ServerContextCalculator calculator = new ServerContextCalculator("lobby");

        ContextSet.Builder builder = ContextSet.builder();
        calculator.calculate(TEST_UUID, builder);
        ContextSet contexts = builder.build();

        assertTrue(contexts.containsKey("server"));
        assertEquals("lobby", contexts.getValue("server"));
    }

    @Test
    void testServerContextCalculatorLowercase() {
        ServerContextCalculator calculator = new ServerContextCalculator("SURVIVAL");

        ContextSet.Builder builder = ContextSet.builder();
        calculator.calculate(TEST_UUID, builder);
        ContextSet contexts = builder.build();

        assertEquals("survival", contexts.getValue("server"));
    }

    @Test
    void testServerContextCalculatorEmpty() {
        ServerContextCalculator calculator = new ServerContextCalculator("");

        ContextSet.Builder builder = ContextSet.builder();
        calculator.calculate(TEST_UUID, builder);
        ContextSet contexts = builder.build();

        assertFalse(contexts.containsKey("server"));
    }

    @Test
    void testContextManagerRegistration() {
        ContextManager manager = new ContextManager();
        assertEquals(0, manager.getCalculatorCount());

        PlayerContextProvider provider = new TestContextProvider("nether", "adventure");
        manager.registerCalculator(new WorldContextCalculator(provider));
        manager.registerCalculator(new GameModeContextCalculator(provider));
        manager.registerCalculator(new ServerContextCalculator("hub"));

        assertEquals(3, manager.getCalculatorCount());

        ContextSet contexts = manager.getContexts(TEST_UUID);
        assertEquals("nether", contexts.getValue("world"));
        assertEquals("adventure", contexts.getValue("gamemode"));
        assertEquals("hub", contexts.getValue("server"));
    }

    @Test
    void testContextManagerClear() {
        ContextManager manager = new ContextManager();
        manager.registerCalculator(new ServerContextCalculator("test"));
        assertEquals(1, manager.getCalculatorCount());

        manager.clear();
        assertEquals(0, manager.getCalculatorCount());
    }

    @Test
    void testEmptyProvider() {
        PlayerContextProvider provider = PlayerContextProvider.EMPTY;

        assertNull(provider.getWorld(TEST_UUID));
        assertNull(provider.getGameMode(TEST_UUID));
        assertFalse(provider.isOnline(TEST_UUID));
    }

    /**
     * Test implementation of PlayerContextProvider.
     */
    private static class TestContextProvider implements PlayerContextProvider {
        private final String world;
        private final String gameMode;

        TestContextProvider(@Nullable String world, @Nullable String gameMode) {
            this.world = world;
            this.gameMode = gameMode;
        }

        @Override
        public @Nullable String getWorld(@NotNull UUID uuid) {
            return world;
        }

        @Override
        public @Nullable String getGameMode(@NotNull UUID uuid) {
            return gameMode;
        }

        @Override
        public boolean isOnline(@NotNull UUID uuid) {
            return world != null;
        }
    }
}
