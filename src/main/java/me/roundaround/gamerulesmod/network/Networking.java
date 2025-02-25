package me.roundaround.gamerulesmod.network;

import com.mojang.datafixers.util.Either;
import me.roundaround.gamerulesmod.GameRulesMod;
import me.roundaround.gamerulesmod.util.RuleInfo;
import me.roundaround.roundalib.network.CustomCodecs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public final class Networking {
  private Networking() {
  }

  public static final Identifier SET_C2S = Identifier.of(GameRulesMod.MOD_ID, "set_c2s");
  public static final Identifier FETCH_C2S = Identifier.of(GameRulesMod.MOD_ID, "fetch_c2s");
  public static final Identifier FETCH_S2C = Identifier.of(GameRulesMod.MOD_ID, "fetch_s2c");

  public static void registerC2SPayloads() {
    PayloadTypeRegistry.playC2S().register(SetC2S.ID, SetC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(FetchC2S.ID, FetchC2S.CODEC);
  }

  public static void registerS2CPayloads() {
    PayloadTypeRegistry.playS2C().register(FetchS2C.ID, FetchS2C.CODEC);
  }

  public record SetC2S(Map<String, Either<Boolean, Integer>> values) implements CustomPayload {
    public static final CustomPayload.Id<SetC2S> ID = new CustomPayload.Id<>(SET_C2S);
    public static final PacketCodec<RegistryByteBuf, SetC2S> CODEC =
        PacketCodec.tuple(CustomCodecs.forMap(PacketCodecs.STRING,
            PacketCodecs.either(PacketCodecs.BOOL, PacketCodecs.INTEGER)
        ),
        SetC2S::values,
        SetC2S::new
    );

    @Override
    public Id<SetC2S> getId() {
      return ID;
    }
  }

  public record FetchC2S(int reqId, boolean includeImmutable) implements CustomPayload {
    public static final CustomPayload.Id<FetchC2S> ID = new CustomPayload.Id<>(FETCH_C2S);
    public static final PacketCodec<RegistryByteBuf, FetchC2S> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER,
        FetchC2S::reqId,
        PacketCodecs.BOOL,
        FetchC2S::includeImmutable,
        FetchC2S::new
    );

    @Override
    public Id<FetchC2S> getId() {
      return ID;
    }
  }

  public record FetchS2C(int reqId, List<RuleInfo> rules) implements CustomPayload {
    public static final CustomPayload.Id<FetchS2C> ID = new CustomPayload.Id<>(FETCH_S2C);
    public static final PacketCodec<RegistryByteBuf, FetchS2C> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER,
        FetchS2C::reqId,
        CustomCodecs.forList(RuleInfo.PACKET_CODEC),
        FetchS2C::rules,
        FetchS2C::new
    );

    @Override
    public Id<FetchS2C> getId() {
      return ID;
    }
  }
}
