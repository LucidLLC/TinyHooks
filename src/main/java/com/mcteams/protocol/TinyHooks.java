package com.mcteams.protocol;

import com.comphenix.tinyprotocol.TinyProtocol;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
public final class TinyHooks extends TinyProtocol {


    enum Result {
        PASS, CANCEL
    }

    enum Side {
        // Packet<Stage>In
        IN,

        // Packet<Stage>Out
        OUT
    }

    enum Priority {
        LOWEST,
        LOW,
        DEFAULT,
        HIGH,
        HIGHEST
    }

    public final Map<Side, Map<Priority, Map<Class<?>, Set<BiFunction<Object, Player, Result>>>>> hooks = new ConcurrentHashMap<>();

    public TinyHooks(Plugin plugin) {
        super(plugin);
    }

    public <T> void addPacketHook(Side side, Priority priority, Class<? extends T> packet, BiFunction<T, Player, Result> on) {
        hooks.computeIfAbsent(side, (s) -> new ConcurrentHashMap<>())
                .computeIfAbsent(priority, (p) -> new ConcurrentHashMap<>())
                .computeIfAbsent(packet, (c) -> new HashSet<>()).add((BiFunction<Object, Player, Result>) on);

    }

    public <T> void addPacketHook(Side side, Class<? extends T> packet, BiFunction<T, Player, Result> on) {
        hooks.computeIfAbsent(side, (s) -> new ConcurrentHashMap<>())
                .computeIfAbsent(Priority.DEFAULT, (p) -> new ConcurrentHashMap<>())
                .computeIfAbsent(packet, (c) -> new HashSet<>()).add((BiFunction<Object, Player, Result>) on);
    }

    public <T> void addIncomingPacketHook(Priority priority, Class<? extends T> packet, BiFunction<T, Player, Result> on) {
        hooks.computeIfAbsent(Side.IN, (s) -> new ConcurrentHashMap<>())
                .computeIfAbsent(priority, (p) -> new ConcurrentHashMap<>())
                .computeIfAbsent(packet, (c) -> new HashSet<>()).add((BiFunction<Object, Player, Result>) on);
    }

    public <T> void addIncomingPacketHook(Class<? extends T> packet, BiFunction<T, Player, Result> on) {
        hooks.computeIfAbsent(Side.IN, (s) -> new ConcurrentHashMap<>())
                .computeIfAbsent(Priority.DEFAULT, (p) -> new ConcurrentHashMap<>())
                .computeIfAbsent(packet, (c) -> new HashSet<>()).add((BiFunction<Object, Player, Result>) on);
    }

    public <T> void addOutgoingPacketHook(Priority priority, Class<? extends T> packet, BiFunction<T, Player, Result> on) {
        hooks.computeIfAbsent(Side.OUT, (s) -> new ConcurrentHashMap<>())
                .computeIfAbsent(priority, (p) -> new ConcurrentHashMap<>())
                .computeIfAbsent(packet, (c) -> new HashSet<>()).add((BiFunction<Object, Player, Result>) on);
    }

    public <T> void addOutgoingPacketHook(Class<? extends T> packet, BiFunction<T, Player, Result> on) {
        hooks.computeIfAbsent(Side.OUT, (s) -> new ConcurrentHashMap<>())
                .computeIfAbsent(Priority.DEFAULT, (p) -> new ConcurrentHashMap<>())
                .computeIfAbsent(packet, (c) -> new HashSet<>()).add((BiFunction<Object, Player, Result>) on);
    }


    private Object processHooks(Side side, Player player, Object packet) {
        Map<Priority, Map<Class<?>, Set<BiFunction<Object, Player, Result>>>> packetInHooks =
                hooks.computeIfAbsent(side, (s) -> new ConcurrentHashMap<>());

        if (packetInHooks.isEmpty()) return packet;

        Result result = Result.PASS;

        for (Priority value : Priority.values()) {
            Map<Class<?>, Set<BiFunction<Object, Player, Result>>> prioritizedHooks = packetInHooks
                    .computeIfAbsent(value, priority -> new ConcurrentHashMap<>());

            if (prioritizedHooks.isEmpty()) continue;
            Set<BiFunction<Object, Player, Result>> hooks = prioritizedHooks.getOrDefault(packet.getClass(), new HashSet<>());
            if (hooks.isEmpty()) continue;

            for (BiFunction<Object, Player, Result> hook : hooks) {
                result = hook.apply(packet, player);
            }
        }

        if (result == Result.CANCEL) {
            return null;
        }

        return packet;
    }

    @Override
    public Object onPacketInAsync(Player sender, Channel channel, Object packet) {
        return processHooks(Side.IN, sender, packet);
    }

    @Override
    public Object onPacketOutAsync(Player receiver, Channel channel, Object packet) {
        return processHooks(Side.OUT, receiver, packet);
    }
}
