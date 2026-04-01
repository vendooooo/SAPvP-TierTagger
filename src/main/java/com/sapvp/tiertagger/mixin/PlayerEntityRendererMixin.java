package com.sapvp.tiertagger.mixin;

import com.sapvp.tiertagger.render.NametagTextFormatter;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "updateRenderState", at = @At("RETURN"), require = 0)
	private void sapvp$applyTierTagToPlayerLike(PlayerLikeEntity player, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
		sapvp$applyTierTag(player.getUuid(), player.getName().getString(), player.getName(), state);
	}

	@Unique
	private static void sapvp$applyTierTag(java.util.UUID uuid, String fallbackName, Text fallbackText, PlayerEntityRenderState state) {
		Text baseText = state.displayName;
		boolean usingCustomDisplayName = baseText != null;
		if (baseText == null) {
			baseText = state.playerName;
		}
		if (baseText == null) {
			baseText = fallbackText;
		}

		Text formatted = NametagTextFormatter.format(uuid, fallbackName, baseText);
		if (usingCustomDisplayName) {
			state.displayName = formatted;
			state.playerName = null;
			return;
		}

		state.playerName = formatted;
	}
}
