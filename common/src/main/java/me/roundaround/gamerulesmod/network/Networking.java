package me.roundaround.gamerulesmod.network;

import java.util.List;
import java.util.Map;

import com.mojang.datafixers.util.Either;

import me.roundaround.gamerulesmod.client.network.ClientNetworking;
import me.roundaround.gamerulesmod.common.gamerule.RuleInfo;
import me.roundaround.gamerulesmod.generated.Constants;
import me.roundaround.gamerulesmod.server.network.ServerNetworking;
import me.roundaround.trove.network.TroveNetworking;
import me.roundaround.trove.network.TrovePacketCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public final class Networking {
  private Networking() {
  }

  public static final Identifier SET_C2S = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "set_c2s");
  public static final Identifier FETCH_C2S = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "fetch_c2s");
  public static final Identifier FETCH_S2C = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "fetch_s2c");

  public static void register() {
    // C2S — handled on the server thread (the loader bridge hops threads for us).
    TroveNetworking.registerC2S(SetC2S.ID, SetC2S.CODEC, ServerNetworking::handleSet);
    TroveNetworking.registerC2S(FetchC2S.ID, FetchC2S.CODEC, ServerNetworking::handleFetch);
    // S2C — handled on the client thread (the loader bridge hops threads for us).
    TroveNetworking.registerS2C(FetchS2C.ID, FetchS2C.CODEC, ClientNetworking::handleFetch);
  }

  public record SetC2S(Map<String, Either<Boolean, Integer>> values) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetC2S> ID = new CustomPacketPayload.Type<>(SET_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, SetC2S> CODEC = StreamCodec.composite(
        TrovePacketCodecs.forMap(
            ByteBufCodecs.STRING_UTF8,
            ByteBufCodecs.either(ByteBufCodecs.BOOL, ByteBufCodecs.INT)),
        SetC2S::values,
        SetC2S::new);

    @Override
    @NotNull
    public Type<SetC2S> type() {
      return ID;
    }
  }

  public record FetchC2S(int reqId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FetchC2S> ID = new CustomPacketPayload.Type<>(FETCH_C2S);
    public static final StreamCodec<RegistryFriendlyByteBuf, FetchC2S> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        FetchC2S::reqId,
        FetchC2S::new);

    @Override
    @NotNull
    public Type<FetchC2S> type() {
      return ID;
    }
  }

  public record FetchS2C(int reqId, List<RuleInfo> rules) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FetchS2C> ID = new CustomPacketPayload.Type<>(FETCH_S2C);
    public static final StreamCodec<RegistryFriendlyByteBuf, FetchS2C> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        FetchS2C::reqId,
        TrovePacketCodecs.forList(RuleInfo.PACKET_CODEC),
        FetchS2C::rules,
        FetchS2C::new);

    @Override
    @NotNull
    public Type<FetchS2C> type() {
      return ID;
    }
  }
}
