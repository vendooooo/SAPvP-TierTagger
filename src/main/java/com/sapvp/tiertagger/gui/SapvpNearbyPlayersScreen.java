package com.sapvp.tiertagger.gui;

import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import com.sapvp.tiertagger.model.SapvpPlayerProfile;
import com.sapvp.tiertagger.service.PlayerLookupTarget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class SapvpNearbyPlayersScreen extends Screen {
	private final Screen parent;
	private final List<ButtonWidget> playerButtons = new ArrayList<>();
	private final List<AbstractClientPlayerEntity> visiblePlayers = new ArrayList<>();
	private final List<AbstractClientPlayerEntity> players = new ArrayList<>();
	private int page;

	public SapvpNearbyPlayersScreen(Screen parent) {
		super(Text.literal("Jogadores proximos"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		for (int i = 0; i < 10; i++) {
			final int buttonIndex = i;
			visiblePlayers.add(null);
			ButtonWidget button = addDrawableChild(ButtonWidget.builder(Text.literal(""), pressed -> {
				AbstractClientPlayerEntity player = visiblePlayers.get(buttonIndex);
				if (player != null) {
					SAPVPTierTaggerClient.openProfile(PlayerLookupTarget.forUuid(player.getUuid(), player.getName().getString()));
				}
			}).dimensions(30, 46 + (i * 22), this.width - 60, 20).build());
			playerButtons.add(button);
		}

		addDrawableChild(ButtonWidget.builder(Text.literal("Anterior"), button -> {
			if (page > 0) {
				page--;
				refreshPlayers();
			}
		}).dimensions(this.width / 2 - 155, this.height - 28, 74, 20)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Proximo"), button -> {
			if ((page + 1) * 10 < players.size()) {
				page++;
				refreshPlayers();
			}
		}).dimensions(this.width / 2 - 77, this.height - 28, 74, 20)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Atualizar"), button -> refreshPlayers())
			.dimensions(this.width / 2 + 1, this.height - 28, 74, 20)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Voltar"), button -> close())
			.dimensions(this.width / 2 + 79, this.height - 28, 74, 20)
			.build());
		refreshPlayers();
	}

	@Override
	public void close() {
		this.client.setScreen(parent);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderSafeBackground(context);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Clique em um jogador para abrir o painel SAPVP."), this.width / 2, 28, 0xFF9CA3AF);

		if (players.isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Nenhum jogador encontrado por perto."), this.width / 2, 60, 0xFFFFD54A);
			super.render(context, mouseX, mouseY, delta);
			return;
		}

		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Pagina " + (page + 1)), this.width / 2, this.height - 40, 0xFFA6ADC8);

		super.render(context, mouseX, mouseY, delta);
	}

	private void renderSafeBackground(DrawContext context) {
		context.fillGradient(0, 0, this.width, this.height, 0xF010131A, 0xF0202B38);
		context.fill(20, 38, this.width - 20, this.height - 36, 0x55303A48);
	}

	private void refreshPlayers() {
		players.clear();
		if (this.client == null || this.client.world == null || this.client.player == null) {
			return;
		}
		players.addAll(this.client.world.getPlayers().stream()
			.filter(player -> player != this.client.player)
			.sorted(Comparator.comparingDouble(player -> player.distanceTo(this.client.player)))
			.toList());

		int firstIndex = page * 10;
		if (firstIndex >= players.size() && page > 0) {
			page = Math.max(0, (players.size() - 1) / 10);
			firstIndex = page * 10;
		}

		for (int index = 0; index < playerButtons.size(); index++) {
			ButtonWidget button = playerButtons.get(index);
			int playerIndex = firstIndex + index;
			if (playerIndex >= players.size()) {
				visiblePlayers.set(index, null);
				button.visible = false;
				button.active = false;
				continue;
			}

			AbstractClientPlayerEntity player = players.get(playerIndex);
			visiblePlayers.set(index, player);
			Optional<SapvpPlayerProfile> profile = SAPVPTierTaggerClient.profileService().getCached(player.getUuid());
			SAPVPTierTaggerClient.profileService().requestProfile(player.getUuid(), player.getName().getString());
			String label = player.getName().getString()
				+ "  |  "
				+ profile.map(SapvpPlayerProfile::bestTierShort).orElse("...")
				+ "  |  "
				+ String.format("%.1fm", Math.sqrt(player.distanceTo(this.client.player)))
				+ "  |  "
				+ profile.map(SapvpPlayerProfile::countryCodeOrFallback).orElse("--");

			button.setMessage(Text.literal(label));
			button.visible = true;
			button.active = true;
		}
	}
}
