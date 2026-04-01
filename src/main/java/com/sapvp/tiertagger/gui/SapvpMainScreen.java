package com.sapvp.tiertagger.gui;

import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class SapvpMainScreen extends Screen {
	private final Screen parent;
	private TextFieldWidget nameField;

	public SapvpMainScreen(Screen parent) {
		super(Text.literal("SAPVP Tier Tagger"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int startY = this.height / 2 - 70;

		nameField = new TextFieldWidget(this.textRenderer, centerX - 100, startY, 200, 20, Text.literal("Nick SAPVP"));
		nameField.setPlaceholder(Text.literal("Digite um nick da SAPVP"));
		addSelectableChild(nameField);
		setInitialFocus(nameField);

		addDrawableChild(ButtonWidget.builder(Text.literal("Buscar nick"), button -> searchByTypedName())
			.dimensions(centerX - 100, startY + 26, 200, 20)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Perfil do alvo"), button -> SAPVPTierTaggerClient.openLookTargetProfile(this.client, true))
			.dimensions(centerX - 100, startY + 52, 200, 20)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Jogadores proximos"), button -> this.client.setScreen(new SapvpNearbyPlayersScreen(this)))
			.dimensions(centerX - 100, startY + 78, 200, 20)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Configuracao"), button -> this.client.setScreen(SapvpConfigScreen.create(this)))
			.dimensions(centerX - 100, startY + 104, 200, 20)
			.build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Fechar"), button -> close())
			.dimensions(centerX - 100, startY + 130, 200, 20)
			.build());
	}

	@Override
	public void close() {
		this.client.setScreen(parent);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderSafeBackground(context);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 100, 0xFFFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Abra perfis por nick, alvo ou lista de players proximos."), this.width / 2, this.height / 2 - 86, 0xFF9CA3AF);
		nameField.render(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
	}

	private void renderSafeBackground(DrawContext context) {
		context.fillGradient(0, 0, this.width, this.height, 0xF010131A, 0xF0202B38);
		context.fill(this.width / 2 - 120, this.height / 2 - 110, this.width / 2 + 120, this.height / 2 + 92, 0x662A3442);
	}

	private void searchByTypedName() {
		String value = nameField.getText().trim();
		if (!value.isEmpty()) {
			SAPVPTierTaggerClient.openProfileByName(value);
		}
	}
}
