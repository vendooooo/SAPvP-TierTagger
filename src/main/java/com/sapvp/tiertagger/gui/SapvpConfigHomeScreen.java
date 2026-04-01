package com.sapvp.tiertagger.gui;

import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import com.sapvp.tiertagger.config.SapvpTierTaggerConfig;
import com.sapvp.tiertagger.util.ModeIconStyleOption;
import com.sapvp.tiertagger.util.ModeVisuals;
import com.sapvp.tiertagger.util.NametagModeOption;
import com.sapvp.tiertagger.util.NametagSideOption;
import com.sapvp.tiertagger.util.TierLabel;
import com.sapvp.tiertagger.util.TierLabels;
import com.sapvp.tiertagger.util.TierVisuals;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix3x2fStack;

public final class SapvpConfigHomeScreen extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int TEXT = 0xFFD7DCEF;
    private static final int MUTED = 0xFF92A0B5;
    private static final int PANEL = 0xF00D141D;
    private static final int SECTION = 0xCC121B27;
    private static final int SURFACE = 0xB1161E2A;
    private static final int BORDER = 0xB92A3648;
    private static final int DIVIDER = 0x26334255;
    private static final int CYAN = 0xFF6FE6E1;
    private static final int GREEN = 0xFFA6E3A1;
    private static final int GOLD = 0xFFFFD166;
    private static final int RED = 0xFFF38BA8;
    private static final int BLUE = 0xFF89B4FA;
    private static final int OVERLAY_TOP = 0x74030A11;
    private static final int OVERLAY_BOTTOM = 0xB0050810;

    private final Screen parent;

    private ButtonWidget enabledButton;
    private ButtonWidget nametagButton;
    private ButtonWidget modeIconButton;
    private ButtonWidget sideButton;
    private ButtonWidget modeButton;
    private ButtonWidget styleButton;

    public SapvpConfigHomeScreen(Screen parent) {
        super(Text.literal("SAPVP Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        int controlsX = x + 18;
        int controlsY = y + 214;
        int controlsWidth = panelWidth() - 36;
        int columnGap = 12;
        int columnWidth = (controlsWidth - columnGap) / 2;
        int rowGap = 6;
        int rowHeight = 42;
        int quickButtonWidth = 118;

        enabledButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> toggleEnabled())
            .dimensions(controlsX + columnWidth - quickButtonWidth - 10, controlsY + 11, quickButtonWidth, 20)
            .build());
        nametagButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> toggleNametag())
            .dimensions(controlsX + columnWidth + columnGap + columnWidth - quickButtonWidth - 10, controlsY + 11, quickButtonWidth, 20)
            .build());
        modeIconButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> toggleModeIcon())
            .dimensions(controlsX + columnWidth - quickButtonWidth - 10, controlsY + rowHeight + rowGap + 11, quickButtonWidth, 20)
            .build());

        sideButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> cycleSide())
            .dimensions(controlsX + columnWidth + columnGap + columnWidth - quickButtonWidth - 10, controlsY + rowHeight + rowGap + 11, quickButtonWidth, 20)
            .build());
        modeButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> cycleMode())
            .dimensions(controlsX + columnWidth - quickButtonWidth - 10, controlsY + (rowHeight + rowGap) * 2 + 11, quickButtonWidth, 20)
            .build());
        styleButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> cycleStyle())
            .dimensions(controlsX + columnWidth + columnGap + columnWidth - quickButtonWidth - 10, controlsY + (rowHeight + rowGap) * 2 + 11, quickButtonWidth, 20)
            .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Editor completo"), button -> this.client.setScreen(SapvpConfigScreen.createAdvanced(this)))
            .dimensions(x + panelWidth() - 216, y + panelHeight() - 30, 128, 20)
            .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Voltar"), button -> close())
            .dimensions(x + panelWidth() - 80, y + panelHeight() - 30, 62, 20)
            .build());

        refreshButtons();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackdrop(context);

        int x = panelX();
        int y = panelY();
        int width = panelWidth();
        int height = panelHeight();
        int accent = SAPVPTierTaggerClient.config().enabled ? CYAN : RED;

        drawShell(context, x, y, width, height, accent);
        drawHeader(context, x + 18, y + 16, width - 36);
        drawPreviewCard(context, x + 18, y + 72, 244, 120);
        drawStatusCard(context, x + 274, y + 72, width - 292, 120);
        drawControlsBoard(context, x + 18, y + 204, width - 36, 140);
        drawFooterRail(context, x + 18, y + height - 38, width - 36, 26);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBackdrop(DrawContext context) {
        context.fillGradient(0, 0, this.width, this.height, OVERLAY_TOP, OVERLAY_BOTTOM);
    }

    private void drawShell(DrawContext context, int x, int y, int width, int height, int accent) {
        context.fill(x - 10, y - 10, x + width + 10, y + height + 10, 0x24000000);
        context.fill(x - 2, y - 2, x + width + 2, y + height + 2, BORDER);
        context.fill(x, y, x + width, y + height, PANEL);
        context.fillGradient(x, y, x + width, y + 54, tint(accent, 0x30), 0x00000000);
        context.fill(x, y, x + 4, y + height, tint(accent, 0x64));
        context.fill(x + 18, y + 56, x + width - 18, y + 57, DIVIDER);
    }

    private void drawHeader(DrawContext context, int x, int y, int width) {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        drawScaledText(context, "SETTINGS.", x, y + 1, 1.34F, WHITE);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Ajuste a nametag e o visual sem entrar no editor completo."), x, y + 23, MUTED);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Modo atual: " + NametagModeOption.fromModeId(config.nametagMode).displayLabel() + "  |  Lado: " + NametagSideOption.fromId(config.nametagSide).label()), x, y + 38, TEXT);

        String status = config.enabled ? "Ativo" : "Desativado";
        int statusColor = config.enabled ? GREEN : RED;
        int badgeWidth = this.textRenderer.getWidth(status) + 18;
        int badgeX = x + width - badgeWidth;
        context.fill(badgeX, y + 2, badgeX + badgeWidth, y + 20, tint(statusColor, 0x24));
        context.fill(badgeX, y + 2, badgeX + 3, y + 20, statusColor);
        context.drawTextWithShadow(this.textRenderer, Text.literal(status), badgeX + 9, y + 7, statusColor);
    }

    private void drawPreviewCard(DrawContext context, int x, int y, int width, int height) {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        context.fill(x, y, x + width, y + height, SECTION);
        context.fillGradient(x, y, x + width, y + 28, 0x221D3042, 0x00000000);
        drawScaledText(context, "NAMETAG.", x + 12, y + 8, 1.10F, WHITE);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Preview instantaneo da tag no jogo."), x + 12, y + 24, MUTED);

        int previewX = x + 12;
        int previewY = y + 46;
        int previewWidth = width - 24;
        context.fill(previewX, previewY, previewX + previewWidth, previewY + 38, SURFACE);
        context.fill(previewX, previewY, previewX + 4, previewY + 38, tint(CYAN, 0xC0));

        MutableText preview = buildPreviewNametag();
        int textWidth = this.textRenderer.getWidth(preview);
        int centeredX = previewX + Math.max(10, (previewWidth - textWidth) / 2);
        context.drawTextWithShadow(this.textRenderer, preview, centeredX, previewY + 14, WHITE);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Distancia: " + config.maxRenderDistance + " blocos"), x + 12, y + 94, TEXT);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Formato: " + config.nametagFormat), x + 12, y + 108, TEXT);
    }

    private void drawStatusCard(DrawContext context, int x, int y, int width, int height) {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        context.fill(x, y, x + width, y + height, SECTION);
        context.fillGradient(x, y, x + width, y + 28, 0x22162B3B, 0x00000000);
        drawScaledText(context, "OVERVIEW.", x + 12, y + 8, 1.10F, WHITE);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Resumo rapido do estado atual."), x + 12, y + 24, MUTED);

        int metricY = y + 48;
        int colGap = 10;
        int metricWidth = Math.max(92, (width - 36) / 3);
        drawMiniMetric(context, x + 12, metricY, metricWidth, 34, "Cache", config.cacheCooldownSeconds + "s", CYAN);
        drawMiniMetric(context, x + 12 + metricWidth + colGap, metricY, metricWidth, 34, "Raio", config.requestRadius + "m", BLUE);
        drawMiniMetric(context, x + 12 + (metricWidth + colGap) * 2, metricY, width - (metricWidth + colGap) * 2 - 24, 34, "Track", Integer.toString(config.maxTrackedPlayers), GOLD);

        context.drawTextWithShadow(this.textRenderer, Text.literal("Paleta dos tiers"), x + 12, y + 92, MUTED);
        drawTierPalette(context, x + 12, y + 106);
    }

    private void drawControlsBoard(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, SECTION);
        context.fillGradient(x, y, x + width, y + 24, 0x201A2636, 0x00000000);
        drawScaledText(context, "QUICK ACTIONS.", x + 12, y + 7, 1.06F, WHITE);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Cada linha controla uma parte do mod sem sobrepor a interface."), x + 126, y + 10, MUTED);

        int columnGap = 12;
        int columnWidth = (width - columnGap) / 2;
        int rowHeight = 42;
        int rowGap = 6;

        drawSettingRow(context, x, y + 26, columnWidth, "Modulo", "Liga o mod inteiro.", CYAN);
        drawSettingRow(context, x + columnWidth + columnGap, y + 26, columnWidth, "Nametag", "Mostra ou oculta as tags.", CYAN);
        drawSettingRow(context, x, y + 26 + rowHeight + rowGap, columnWidth, "Icone do modo", "Exibe o icone antes do tier.", BLUE);
        drawSettingRow(context, x + columnWidth + columnGap, y + 26 + rowHeight + rowGap, columnWidth, "Lado", "Antes ou depois do nick.", BLUE);
        drawSettingRow(context, x, y + 26 + (rowHeight + rowGap) * 2, columnWidth, "Modo", "Escolhe qual tier mostrar.", GOLD);
        drawSettingRow(context, x + columnWidth + columnGap, y + 26 + (rowHeight + rowGap) * 2, columnWidth, "Estilo", "SA Icons ou emoji.", GOLD);
    }

    private void drawSettingRow(DrawContext context, int x, int y, int width, String title, String subtitle, int accent) {
        context.fill(x, y, x + width, y + 42, 0x1E101722);
        context.fill(x, y, x + 3, y + 42, accent);
        context.drawTextWithShadow(this.textRenderer, Text.literal(title), x + 10, y + 6, WHITE);
        context.drawTextWithShadow(this.textRenderer, Text.literal(subtitle), x + 10, y + 18, MUTED);
    }

    private void drawMiniMetric(DrawContext context, int x, int y, int width, int height, String label, String value, int accent) {
        context.fill(x, y, x + width, y + height, SURFACE);
        context.fill(x, y, x + 4, y + height, tint(accent, 0xC0));
        context.drawTextWithShadow(this.textRenderer, Text.literal(label), x + 10, y + 8, MUTED);
        context.drawTextWithShadow(this.textRenderer, Text.literal(value), x + 10, y + 20, accent);
    }

    private void drawTierPalette(DrawContext context, int x, int y) {
        int swatchWidth = 18;
        int gap = 6;
        int[] raws = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        int drawX = x;
        for (int raw : raws) {
            TierLabel tier = TierLabels.fromRaw(raw);
            int color = TierVisuals.accentColor(tier);
            context.fill(drawX, y, drawX + swatchWidth, y + 10, TierVisuals.backgroundColor(tier));
            context.fill(drawX, y, drawX + 3, y + 10, color);
            drawX += swatchWidth + gap;
        }
    }

    private void drawFooterRail(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0x22111A24);
        context.fill(x, y, x + width, y + 1, DIVIDER);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Use o editor completo para cores, cache e ajustes avancados."), x + 10, y + 8, MUTED);
    }

    private MutableText buildPreviewNametag() {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        String previewMode = config.nametagMode == null ? "BEST" : config.nametagMode;
        previewMode = ModeVisuals.normalizeNametagMode(previewMode);
        if ("BEST".equals(previewMode)) {
            previewMode = "SWORD";
        }

        TierLabel tier = TierLabels.fromRaw(6);
        MutableText prefix = Text.empty();
        if (config.showModeIconInNametag) {
            prefix.append(previewIcon(previewMode));
            prefix.append(Text.literal(" "));
        }
        prefix.append(Text.literal(config.nametagFormat.replace("%tier%", tier.shortLabel()))
            .setStyle(Style.EMPTY.withColor(config.tierColorFor(tier) & 0x00FFFFFF)));

        MutableText nick = Text.literal("Vendouzz")
            .setStyle(Style.EMPTY.withColor(config.nameColor & 0x00FFFFFF));
        MutableText separator = Text.literal(" | ")
            .setStyle(Style.EMPTY.withColor(config.separatorColor & 0x00FFFFFF));

        if (NametagSideOption.fromId(config.nametagSide) == NametagSideOption.RIGHT) {
            return Text.empty().append(nick).append(separator).append(prefix);
        }
        return Text.empty().append(prefix).append(separator).append(nick);
    }

    private Text previewIcon(String modeId) {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        if (ModeIconStyleOption.fromId(config.modeIconStyle) == ModeIconStyleOption.EMOJI) {
            return Text.literal(ModeVisuals.emoji(modeId)).setStyle(Style.EMPTY.withColor(config.modeIconColor & 0x00FFFFFF));
        }
        return Text.literal(ModeVisuals.iconGlyph(modeId)).setStyle(ModeVisuals.saIconStyle(config.modeIconColor));
    }

    private void toggleEnabled() {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        config.enabled = !config.enabled;
        commitChanges();
    }

    private void toggleNametag() {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        config.renderNametags = !config.renderNametags;
        commitChanges();
    }

    private void toggleModeIcon() {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        config.showModeIconInNametag = !config.showModeIconInNametag;
        commitChanges();
    }

    private void cycleSide() {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        config.nametagSide = NametagSideOption.fromId(config.nametagSide) == NametagSideOption.LEFT ? NametagSideOption.RIGHT.id() : NametagSideOption.LEFT.id();
        commitChanges();
    }

    private void cycleMode() {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        NametagModeOption[] values = NametagModeOption.values();
        NametagModeOption current = NametagModeOption.fromModeId(config.nametagMode);
        int next = (current.ordinal() + 1) % values.length;
        config.nametagMode = values[next].modeId();
        commitChanges();
    }

    private void cycleStyle() {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        config.modeIconStyle = ModeIconStyleOption.fromId(config.modeIconStyle) == ModeIconStyleOption.SA_ICONS
            ? ModeIconStyleOption.EMOJI.id()
            : ModeIconStyleOption.SA_ICONS.id();
        commitChanges();
    }

    private void commitChanges() {
        SAPVPTierTaggerClient.config().normalize();
        SAPVPTierTaggerClient.saveConfig();
        refreshButtons();
    }

    private void refreshButtons() {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        enabledButton.setMessage(toggleText("Modulo", config.enabled));
        nametagButton.setMessage(toggleText("Nametag", config.renderNametags));
        modeIconButton.setMessage(toggleText("Icone do modo", config.showModeIconInNametag));
        sideButton.setMessage(cycleText("Lado", NametagSideOption.fromId(config.nametagSide).label(), CYAN));
        modeButton.setMessage(cycleText("Modo", NametagModeOption.fromModeId(config.nametagMode).displayLabel(), GOLD));
        styleButton.setMessage(cycleText("Estilo", ModeIconStyleOption.fromId(config.modeIconStyle).label(), BLUE));
    }

    private Text toggleText(String label, boolean enabled) {
        return Text.empty()
            .append(Text.literal(label + ": ").formatted(Formatting.GRAY))
            .append(Text.literal(enabled ? "ON" : "OFF").formatted(enabled ? Formatting.GREEN : Formatting.RED));
    }

    private Text cycleText(String label, String value, int color) {
        return Text.empty()
            .append(Text.literal(label + ": ").formatted(Formatting.GRAY))
            .append(Text.literal(value).setStyle(Style.EMPTY.withColor(color & 0x00FFFFFF)));
    }

    private void drawScaledText(DrawContext context, String text, int x, int y, float scale, int color) {
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        context.drawTextWithShadow(this.textRenderer, Text.literal(text), 0, 0, color);
        matrices.popMatrix();
    }

    private int panelWidth() {
        return Math.min(630, this.width - 44);
    }

    private int panelHeight() {
        return Math.min(404, this.height - 32);
    }

    private int panelX() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelY() {
        return (this.height - panelHeight()) / 2;
    }

    private static int tint(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
