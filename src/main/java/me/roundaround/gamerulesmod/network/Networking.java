package me.roundaround.gamerulesmod.network;

import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Either;

import me.roundaround.gamerulesmod.common.gamerule.RuleInfo;
import me.roundaround.gamerulesmod.generated.Constants;
import me.roundaround.gamerulesmod.generated.Variant;
import me.roundaround.gamerulesmod.roundalib.network.RoundaLibPacketCodecs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class Networking {
  private Networking() {
  }

  public static final Identifier SET_C2S = Identifier.of(Constants.MOD_ID, "set_c2s");
  public static final Identifier FETCH_C2S = Identifier.of(Constants.MOD_ID, "fetch_c2s");
  public static final Identifier FETCH_S2C = Identifier.of(Constants.MOD_ID, "fetch_s2c");

  public static void registerC2SPayloads() {
    PayloadTypeRegistry.playC2S().register(SetC2S.ID, SetC2S.CODEC);
    PayloadTypeRegistry.playC2S().register(FetchC2S.ID, FetchC2S.CODEC);
  }

  public static void registerS2CPayloads() {
    PayloadTypeRegistry.playS2C().register(FetchS2C.ID, FetchS2C.CODEC);
  }

  public record SetC2S(Map<String, Either<Boolean, Integer>> values) implements CustomPayload {
    public static final CustomPayload.Id<SetC2S> ID = new CustomPayload.Id<>(SET_C2S);
    public static final PacketCodec<RegistryByteBuf, SetC2S> CODEC = PacketCodec.tuple(
        RoundaLibPacketCodecs.forMap(
            PacketCodecs.STRING,
            PacketCodecs.either(PacketCodecs.BOOLEAN, PacketCodecs.INTEGER)),
        SetC2S::values,
        SetC2S::new);

    @Override
    public Id<SetC2S> getId() {
      return ID;
    }
  }

  public record FetchC2S(int reqId, boolean includeImmutable) implements CustomPayload {
    public static final CustomPayload.Id<FetchC2S> ID = new CustomPayload.Id<>(FETCH_C2S);
    public static final PacketCodec<RegistryByteBuf, FetchC2S> CODEC = PacketCodec.tuple(
        PacketCodecs.INTEGER,
        FetchC2S::reqId,
        PacketCodecs.BOOLEAN,
        FetchC2S::includeImmutable,
        FetchC2S::new);

    @Override
    public Id<FetchC2S> getId() {
      return ID;
    }
  }

  public record FetchS2C(int reqId, Variant activeVariant, List<RuleInfo> rules) implements CustomPayload {
    // TODO: Auto-generate with plugin
    private static final PacketCodec<RegistryByteBuf, Variant> VARIANT_CODEC = PacketCodec.of(
        (variant, byeBuf) -> byeBuf.writeInt(
            variant.ordinal()),
        (byteBuf) -> Variant.values()[byteBuf.readInt() % Variant.values().length]);

    public static final CustomPayload.Id<FetchS2C> ID = new CustomPayload.Id<>(FETCH_S2C);
    public static final PacketCodec<RegistryByteBuf, FetchS2C> CODEC = PacketCodec.tuple(
        PacketCodecs.INTEGER,
        FetchS2C::reqId,
        VARIANT_CODEC,
        FetchS2C::activeVariant,
        RoundaLibPacketCodecs.forList(RuleInfo.PACKET_CODEC),
        FetchS2C::rules,
        FetchS2C::new);

    @Override
    public Id<FetchS2C> getId() {
      return ID;
    }
  }
}
