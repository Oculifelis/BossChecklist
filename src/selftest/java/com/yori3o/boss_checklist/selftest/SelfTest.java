package com.yori3o.boss_checklist.selftest;

import com.yori3o.boss_checklist.common.BossChecklist;
import com.yori3o.boss_checklist.common.client.boss.BossDefinition;
import com.yori3o.boss_checklist.common.client.data.BossProgressStorage;
import com.yori3o.boss_checklist.common.client.data.BossRegistry;
import com.yori3o.boss_checklist.common.client.gui.BossChecklistScreen;
import com.yori3o.boss_checklist.common.server.data.ServerBossIdsLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game check, compiled only with -Pselftest: makes a creative world, opens the checklist, lists the bosses,
 * summons and kills bosses on the integrated server, checks the client marks them, takes screenshots and quits.
 * Every line it logs starts with BCTEST.
 */
@EventBusSubscriber(modid = BossChecklist.MOD_ID, value = Dist.CLIENT)
public final class SelfTest {
    private static final Logger LOG = LoggerFactory.getLogger("BCTEST");
    private static final boolean ON = Boolean.getBoolean("boss_checklist.selftest");

    private static boolean started;
    private static int waitTicks;
    private static int tick;
    private static int fails;
    private static final List<String> KILLED = new ArrayList<>();
    private static final List<String> BOSS_IDS = new ArrayList<>();

    static {
        if (ON) {
            // a hung run ends itself (this JVM only) instead of waiting for someone to kill it
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(12 * 60 * 1000L);
                } catch (InterruptedException e) {
                    return;
                }
                LOG.error("BCTEST RESULT FAIL (timed out after 12 minutes)");
                Runtime.getRuntime().halt(3);
            }, "bctest-watchdog");
            t.setDaemon(true);
            t.start();
        }
    }

    private SelfTest() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ON) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!started && mc.gui.screen() != null && !(mc.gui.screen() instanceof TitleScreen) && mc.gui.overlay() == null) {
                if (++waitTicks > 200) {
                    LOG.info("BCTEST dismissing {}", mc.gui.screen().getClass().getName());
                    mc.gui.setScreen(new TitleScreen());
                    waitTicks = 0;
                }
            }
            if (!started && mc.gui.screen() instanceof TitleScreen title) {
                started = true;
                LOG.info("BCTEST creating world");
                mc.options.pauseOnLostFocus = false;
                mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
                mc.options.renderDistance().set(6);
                mc.options.simulationDistance().set(6);
                String name = "bctest" + (System.currentTimeMillis() / 1000);
                LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(20261014L, true, false), WorldPresets::createNormalWorldDimensions, title);
            }
            return;
        }
        if (mc.gui.screen() instanceof PauseScreen) {
            mc.gui.setScreen(null);
        }
        tick++;
        try {
            step(mc);
        } catch (Throwable t) {
            fails++;
            LOG.error("BCTEST FAIL exception at tick {}", tick, t);
            tick = 1000;
        }
    }

    private static void step(Minecraft mc) {
        switch (tick) {
            case 100 -> {
                List<String> all = new ArrayList<>();
                for (BossDefinition d : BossRegistry.all()) {
                    all.add(d.id());
                }
                long vanilla = all.stream().filter(s -> s.startsWith("minecraft:")).count();
                long ur = all.stream().filter(s -> s.startsWith("ultimaratio:")).count();
                LOG.info("BCTEST client registry: {} bosses, {} vanilla, {} ultimaratio", all.size(), vanilla, ur);
                LOG.info("BCTEST client ids: {}", all);
                check("vanilla bosses listed", vanilla >= 3);
                // Ultima Ratio's checks run only when its Boss Checklist data is loaded beside it
                boolean urData = net.neoforged.fml.ModList.get().isLoaded("ultimaratio");
                LOG.info("BCTEST Ultima Ratio loaded: {}", urData);
                if (urData) {
                    check("Ultima Ratio bosses listed", ur >= 9);
                }
                List<String> serverIds = new ArrayList<>(List.of("minecraft:wither", "minecraft:ender_dragon", "minecraft:elder_guardian"));
                if (urData) {
                    serverIds.addAll(List.of("ultimaratio:ultrum", "ultimaratio:hollow_sun"));
                }
                for (String id : serverIds) {
                    check("server counts " + id + " as a boss", ServerBossIdsLoader.isBoss(id));
                }
                check("server does not count minecraft:zombie", !ServerBossIdsLoader.isBoss("minecraft:zombie"));
                for (String id : all) {
                    if (id.startsWith("ultimaratio:") || id.equals("minecraft:wither") || id.equals("minecraft:elder_guardian")) {
                        BOSS_IDS.add(id);
                    }
                }
                for (String id : BOSS_IDS) {
                    check(id + " not defeated before the test", !BossProgressStorage.isDefeated(id));
                }
            }
            case 110 -> {
                mc.gui.setScreen(new BossChecklistScreen(null));
                LOG.info("BCTEST opened screen: {}", mc.gui.screen() == null ? "null" : mc.gui.screen().getClass().getName());
                check("checklist screen open", mc.gui.screen() instanceof BossChecklistScreen);
            }
            case 160 -> shot(mc, "bctest_screen_before.png");
            case 170 -> {
                mc.gui.setScreen(null);
                command(mc, "summon minecraft:zombie ~ ~ ~5");
                command(mc, "kill @e[type=minecraft:zombie]");
            }
            default -> {
                // one boss every 20 ticks: summon it a few blocks away and kill it on the integrated server
                int k = (tick - 180) / 20;
                if (tick >= 180 && (tick - 180) % 20 == 0 && k < BOSS_IDS.size()) {
                    String id = BOSS_IDS.get(k);
                    command(mc, "summon " + id + " ~ ~ ~8");
                    command(mc, "kill @e[type=" + id + "]");
                    KILLED.add(id);
                }
                int end = 180 + BOSS_IDS.size() * 20 + 60;
                if (tick == end) {
                    for (String id : KILLED) {
                        check(id + " marked defeated after kill", BossProgressStorage.isDefeated(id));
                    }
                    check("minecraft:zombie not marked (not a boss)", !BossProgressStorage.isDefeated("minecraft:zombie"));
                    LOG.info("BCTEST defeated on client: {}", BossProgressStorage.defeatedBosses());
                    mc.gui.setScreen(new BossChecklistScreen(null));
                }
                if (tick == end + 50) {
                    shot(mc, "bctest_screen_after.png");
                }
                if (tick == end + 60 || tick == 1000) {
                    LOG.info("BCTEST RESULT {} ({} failed checks)", fails == 0 ? "PASS" : "FAIL", fails);
                    tick = 2000;
                }
                if (tick == 2040) {
                    LOG.info("BCTEST exiting");
                    mc.stop();
                }
            }
        }
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            fails++;
        }
        LOG.info("BCTEST {} {}", ok ? "ok  " : "FAIL", what);
    }

    private static void command(Minecraft mc, String cmd) {
        MinecraftServer server = mc.getSingleplayerServer();
        Vec3 at = mc.player.position();
        server.execute(() -> {
            CommandSourceStack src = server.createCommandSourceStack().withLevel(server.overworld()).withPosition(at);
            server.getCommands().performPrefixedCommand(src, cmd);
            LOG.info("BCTEST ran /{}", cmd);
        });
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, c -> {
        });
        LOG.info("BCTEST screenshot {}", name);
    }
}
