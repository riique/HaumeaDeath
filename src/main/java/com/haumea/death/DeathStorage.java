package com.haumea.death;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeathStorage {
    private static final Map<UUID, DeathRecord> LAST_DEATH = new ConcurrentHashMap<>();

    private DeathStorage() {}

    public static void put(DeathRecord record) {
        LAST_DEATH.put(record.playerId, record);
    }

    public static Optional<DeathRecord> get(UUID playerId) {
        return Optional.ofNullable(LAST_DEATH.get(playerId));
    }

    public static void remove(UUID playerId) {
        LAST_DEATH.remove(playerId);
    }
}
