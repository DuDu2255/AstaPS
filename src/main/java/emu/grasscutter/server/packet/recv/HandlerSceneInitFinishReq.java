package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.config.ConfigContainer;
import emu.grasscutter.config.Configuration;
import emu.grasscutter.game.dungeons.DomainDungeonHelper;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.player.Player.SceneLoadState;
import emu.grasscutter.game.player.WidgetPetHelper;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.SceneInitFinishReqOuterClass.SceneInitFinishReq;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.*;
import emu.grasscutter.utils.LuaShell;
import emu.grasscutter.utils.WatermarkGradientHelper;

@Opcodes(PacketOpcodes.SceneInitFinishReq)
public class HandlerSceneInitFinishReq extends PacketHandler {

    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        SceneInitFinishReq req = SceneInitFinishReq.parseFrom(payload);

        var player = session.getPlayer();
        var world = player.getWorld();

        // Preload follower-pet embryos before team AbilityControlBlock is sent
        WidgetPetHelper.preloadEquippedWidget(player);

        try {
            // Scrub dive leftovers before enter-scene ability blocks (login / teleport).
            emu.grasscutter.game.player.DiveAbilityHelper.onTeamOrSceneRebuild(player);
        } catch (Throwable ignored) {
        }

        session.send(new PacketServerTimeNotify());
        session.send(new PacketWorldPlayerInfoNotify(world));
        session.send(new PacketWorldDataNotify(world));
        session.send(new PacketPlayerWorldSceneInfoListNotify(player));
        session.send(new PacketSceneForceUnlockNotify(1, true));
        session.send(new PacketHostPlayerNotify(world));
        session.send(new PacketSceneDataNotify(player));

        session.send(new PacketSceneTimeNotify(player));
        session.send(new PacketPlayerGameTimeNotify(player));
        session.send(new PacketPlayerEnterSceneInfoNotify(player));
        int moonPhaseCount = (int) player.getTeamManager().getActiveTeam().stream()
                .filter(e -> PacketPlayerEnterSceneInfoNotify.getMoonphaseIds().contains(e.getAvatar().getAvatarId()))
                .count();
        session.send(new PacketTeamMoonPhaseChangeNotify(moonPhaseCount));
        int hexenzirkelCount = (int) player.getTeamManager().getActiveTeam().stream()
                .filter(e -> PacketPlayerEnterSceneInfoNotify.getHexenzirkelIds().contains(e.getAvatar().getAvatarId()))
                .count();
        session.send(new PacketTeamHexenzirkelChangeNotify(hexenzirkelCount));
        if (!player.getQuestManager().restoreActiveQuestWeather(player.getSceneId())) {
            session.send(new PacketSceneAreaWeatherNotify(player));
        }
        session.send(new PacketScenePlayerInfoNotify(world));
        session.send(new PacketSceneTeamUpdateNotify(player));

        session.send(new PacketSyncTeamEntityNotify(player));
        session.send(new PacketSyncScenePlayTeamEntityNotify(player));

        // Follower pets need the attach-ability notify again after scene ability block is set
        WidgetPetHelper.syncEquippedWidget(player);

        session.send(new PacketSceneInitFinishRsp(player));
        session.send(buildWatermarkPacket(player));

        player.setSceneLoadState(SceneLoadState.INIT);

        player.getScene().playerSceneInitialized(player);
    }

    private static PacketWindSeedClientNotify buildWatermarkPacket(Player player) {
        ConfigContainer.GameOptions.WatermarkOptions options = Configuration.GAME_OPTIONS.watermark;
        if (!options.enabled || options.text == null || options.text.isBlank()) {
            return new PacketWindSeedClientNotify(LuaShell.getLuaShell());
        }

        String text = WatermarkGradientHelper.buildDisplayText(options, player);
        String colored = WatermarkGradientHelper.applyGradient(options, text);
        byte[] payload = UidWatermarkScript.forText(colored);
        if (payload != null) return new PacketWindSeedClientNotify(payload);

        Grasscutter.getLogger()
                .warn("Watermark text exceeds the 254-byte limit; using the login Lua shell instead.");
        return new PacketWindSeedClientNotify(LuaShell.getLuaShell());
    }
}
