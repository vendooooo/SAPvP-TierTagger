package com.sapvp.tiertagger.gui;

import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import com.sapvp.tiertagger.model.SapvpModeRanking;
import com.sapvp.tiertagger.model.SapvpPlayerProfile;
import com.sapvp.tiertagger.service.PlayerLookupTarget;
import com.sapvp.tiertagger.service.ProfileLookupState;
import com.sapvp.tiertagger.util.ModeVisuals;
import com.sapvp.tiertagger.util.TierVisuals;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.Comparator;
import java.util.List;

public final class SapvpInfoScreen extends Screen {
    private static final int RANKING_ROW_HEIGHT = 34;
    private static final int RANKING_ICON_BOX_SIZE = 24;
    private static final Comparator<SapvpModeRanking> RANKING_COMPARATOR = Comparator
        .comparingInt((SapvpModeRanking value) -> value.tier().raw()).reversed()
        .thenComparing(Comparator.comparingInt(SapvpModeRanking::points).reversed())
        .thenComparing(SapvpModeRanking::displayName, String.CASE_INSENSITIVE_ORDER);
    private static final int WHITE = 0xFFFFFFFF;
    private static final int TEXT = 0xFFD8DEEF;
    private static final int MUTED = 0xFF92A0B5;
    private static final int PANEL = 0xF0091018;
    private static final int SECTION = 0xD0101722;
    private static final int SURFACE = 0xB1121A27;
    private static final int BORDER = 0xBB243247;
    private static final int DIVIDER = 0x2A31435A;
    private static final int CYAN = 0xFF63D7D8;
    private static final int GREEN = 0xFFA6E3A1;
    private static final int GOLD = 0xFFFFD166;
    private static final int BLUE = 0xFF6288E4;
    private static final int SILVER = 0xFFD8E2F0;
    private static final int BRONZE = 0xFFCF9368;
    private static final int RED = 0xFFF38BA8;
    private static final int OVERLAY_TOP = 0x76030810;
    private static final int OVERLAY_BOTTOM = 0xB4050810;

    private final Screen parent;
    private final PlayerLookupTarget target;

    private int rankingScroll;
    private int rankingAreaX;
    private int rankingAreaY;
    private int rankingAreaWidth;
    private int rankingAreaHeight;

    public SapvpInfoScreen(Screen parent, PlayerLookupTarget target) {
        super(Text.literal("SAPVP Profile"));
        this.parent = parent;
        this.target = target;
    }

    @Override
    protected void init() {
        int x = panelX();
        int y = panelY();
        int footerY = y + panelHeight() - 30;
        int buttonWidth = 86;
        int gap = 8;
        int totalWidth = buttonWidth * 3 + gap * 2;
        int buttonsX = x + panelWidth() - 18 - totalWidth;

        addDrawableChild(ButtonWidget.builder(Text.literal("Voltar"), button -> close())
            .dimensions(buttonsX, footerY, buttonWidth, 20)
            .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Recarregar"), button -> forceRefresh())
            .dimensions(buttonsX + buttonWidth + gap, footerY, buttonWidth, 20)
            .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Config"), button -> this.client.setScreen(SapvpConfigScreen.create(this)))
            .dimensions(buttonsX + (buttonWidth + gap) * 2, footerY, buttonWidth, 20)
            .build());

        forceRefresh();
    }

    @Override
    public void tick() {
        if (this.client == null) {
            return;
        }
        if (this.client.world == null) {
            close();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        ProfileLookupState state = SAPVPTierTaggerClient.profileService().lookupState(target);
        if (state.profile().isEmpty() || !contains(mouseX, mouseY, rankingAreaX, rankingAreaY, rankingAreaWidth, rankingAreaHeight)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        List<SapvpModeRanking> rankings = sortedRankings(state.profile().get().rankings());
        int maxScroll = maxRankingScroll(rankings, rankingAreaHeight);
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        rankingScroll = clamp(rankingScroll - (int) Math.signum(verticalAmount), 0, maxScroll);
        return true;
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
        int contentX = x + 18;
        int contentWidth = width - 36;

        ProfileLookupState state = SAPVPTierTaggerClient.profileService().lookupState(target);
        SapvpPlayerProfile profile = state.profile().orElse(null);
        int accent = profile != null ? TierVisuals.accentColor(profile.bestTier()) : CYAN;

        drawShell(context, x, y, width, height, accent);
        drawHeader(context, contentX, y + 16, contentWidth, profile, state);

        if (profile == null) {
            int footerTop = y + height - 50;
            drawLoadingPanel(context, contentX, y + 58, contentWidth, footerTop - (y + 58), state, accent);
            rankingAreaX = 0;
            rankingAreaY = 0;
            rankingAreaWidth = 0;
            rankingAreaHeight = 0;
            drawFooterRail(context, contentX, footerTop, contentWidth, 18);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        int footerTop = y + height - 50;
        drawStatsRow(context, contentX, y + 58, contentWidth, profile);
        drawRankingsBoard(context, contentX, y + 112, contentWidth, footerTop - (y + 112), sortedRankings(profile.rankings()));
        drawFooterRail(context, contentX, footerTop, contentWidth, 18);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBackdrop(DrawContext context) {
        context.fillGradient(0, 0, this.width, this.height, OVERLAY_TOP, OVERLAY_BOTTOM);
    }

    private void drawShell(DrawContext context, int x, int y, int width, int height, int accent) {
        context.fill(x - 10, y - 10, x + width + 10, y + height + 10, 0x22000000);
        context.fill(x - 2, y - 2, x + width + 2, y + height + 2, BORDER);
        context.fill(x, y, x + width, y + height, PANEL);
        context.fillGradient(x, y, x + width, y + 56, tint(accent, 0x30), 0x00000000);
        context.fill(x, y, x + 4, y + height, tint(accent, 0x6E));
        context.fill(x + 18, y + 58, x + width - 18, y + 59, DIVIDER);
        context.fillGradient(x, y + height - 46, x + width, y + height, 0x00000000, 0x28050A10);
    }

    private void drawHeader(DrawContext context, int x, int y, int width, @Nullable SapvpPlayerProfile profile, ProfileLookupState state) {
        int headSize = 34;
        drawPlayerHead(context, x, y, headSize, profile);

        int textX = x + headSize + 12;
        String title = profile != null ? profile.name() : fallbackTargetName();
        drawScaledText(context, title, textX, y + 1, 1.24F, WHITE);
        context.drawTextWithShadow(this.textRenderer, Text.literal("PROFILE."), textX, y + 22, MUTED);

        String status = profile != null ? "Loaded" : state.loading() ? "Consultando" : state.message() != null ? "Erro" : "Aguardando";
        int statusColor = profile != null ? GREEN : state.loading() ? GOLD : state.message() != null ? RED : CYAN;
        int badgeWidth = this.textRenderer.getWidth(status) + 20;
        int badgeX = x + width - badgeWidth;
        context.fill(badgeX, y + 2, badgeX + badgeWidth, y + 22, tint(statusColor, 0x24));
        context.fill(badgeX, y + 2, badgeX + 3, y + 22, statusColor);
        context.drawTextWithShadow(this.textRenderer, Text.literal(status), badgeX + 10, y + 8, statusColor);
    }

    private void drawStatsRow(DrawContext context, int x, int y, int width, SapvpPlayerProfile profile) {
        int gap = 10;
        int cardWidth = (width - gap) / 2;
        drawStatCard(context, x, y, cardWidth, 44, "PONTOS TOTAIS", Integer.toString(profile.points()), GOLD, 0x3017120B, null, 0);

        PodiumStyle podium = podiumStyle(profile.globalRank());
        String topValue = profile.globalRank() > 0 ? "#" + profile.globalRank() : "Unranked";
        drawStatCard(
            context,
            x + cardWidth + gap,
            y,
            cardWidth,
            44,
            "RANK GLOBAL",
            topValue,
            podium != null ? podium.accent() : CYAN,
            podium != null ? podium.fill() : 0x28111A24,
            null,
            0
        );
    }

    private void drawStatCard(DrawContext context, int x, int y, int width, int height, String label, String value, int valueColor, int fill, @Nullable String badge, int badgeColor) {
        context.fill(x, y, x + width, y + height, fill);
        context.fill(x, y, x + 4, y + height, tint(valueColor, 0xD0));
        context.drawTextWithShadow(this.textRenderer, Text.literal(label), x + 12, y + 8, MUTED);
        drawScaledText(context, value, x + 12, y + 22, 1.12F, valueColor);
        if (badge != null) {
            int badgeWidth = this.textRenderer.getWidth(badge) + 16;
            context.fill(x + width - badgeWidth - 10, y + 8, x + width - 10, y + 24, tint(badgeColor, 0x20));
            context.drawTextWithShadow(this.textRenderer, Text.literal(badge), x + width - badgeWidth + 1, y + 13, badgeColor);
        }
    }

    private void drawLoadingPanel(DrawContext context, int x, int y, int width, int height, ProfileLookupState state, int accent) {
        context.fill(x, y, x + width, y + height, SECTION);
        context.fillGradient(x, y, x + width, y + 34, tint(accent, 0x22), 0x00000000);
        drawScaledText(context, "TIERS.", x + 12, y + 10, 1.14F, WHITE);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Nick alvo: " + fallbackTargetName()), x + 12, y + 34, TEXT);

        String message = state.loading()
            ? "Consultando a API da SAPVP e preparando o perfil."
            : state.message() != null
                ? state.message()
                : "Aguardando retorno da API.";
        context.drawTextWithShadow(this.textRenderer, Text.literal(message), x + 12, y + 56, GOLD);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Assim que os dados chegarem, a lista completa de tiers aparece aqui."), x + 12, y + 78, MUTED);
    }

    private void drawRankingsBoard(DrawContext context, int x, int y, int width, int height, List<SapvpModeRanking> rankings) {
        context.fill(x, y, x + width, y + height, SECTION);
        context.fillGradient(x, y, x + width, y + 32, 0x241A2735, 0x00000000);
        drawScaledText(context, "TIERS.", x + 12, y + 8, 1.14F, WHITE);

        String helper = rankings.isEmpty() ? "Sem modos" : rankings.size() + " modos";
        int helperWidth = this.textRenderer.getWidth(helper) + 16;
        context.fill(x + width - helperWidth - 10, y + 7, x + width - 10, y + 23, SURFACE);
        context.drawTextWithShadow(this.textRenderer, Text.literal(helper), x + width - helperWidth + 1, y + 12, MUTED);

        int headerY = y + 30;
        int contentX = x + 10;
        int contentWidth = width - 24;
        RankingColumns columns = rankingColumns(contentX, contentWidth);
        context.fill(contentX, headerY, contentX + contentWidth, headerY + 19, 0x1D0E151F);
        context.fill(contentX, headerY + 18, contentX + contentWidth, headerY + 19, DIVIDER);
        int modeHeaderX = columns.iconCenterX() - (this.textRenderer.getWidth("Modo") / 2);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Modo"), modeHeaderX, headerY + 5, MUTED);

        int tierHeaderX = columns.tierCenterX() - (this.textRenderer.getWidth("Tier") / 2);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Tier"), tierHeaderX, headerY + 5, MUTED);

        int pointsHeaderX = columns.pointsCenterX() - (this.textRenderer.getWidth("Pts") / 2);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Pts"), pointsHeaderX, headerY + 5, MUTED);

        if (rankings.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.literal("Nenhum tier encontrado para este jogador."), x + 14, headerY + 30, MUTED);
            rankingAreaX = 0;
            rankingAreaY = 0;
            rankingAreaWidth = 0;
            rankingAreaHeight = 0;
            return;
        }

        int listTop = headerY + 20;
        int listBottom = y + height - 10;
        int listHeight = listBottom - listTop;
        rankingAreaX = contentX;
        rankingAreaY = listTop;
        rankingAreaWidth = contentWidth;
        rankingAreaHeight = listHeight;

        int visibleRows = visibleRows(listHeight);
        int maxScroll = maxRankingScroll(rankings, listHeight);
        rankingScroll = clamp(rankingScroll, 0, maxScroll);

        context.enableScissor(contentX, listTop, contentX + contentWidth, listBottom);
        int rowY = listTop;
        for (int index = rankingScroll; index < rankings.size() && index < rankingScroll + visibleRows; index++) {
            drawRankingRow(context, contentX, rowY, contentWidth, rankings.get(index), index - rankingScroll);
            rowY += RANKING_ROW_HEIGHT;
        }
        context.disableScissor();

        drawScrollBar(context, x + width - 10, listTop, 4, listHeight, rankings.size(), visibleRows);
    }

    private void drawRankingRow(DrawContext context, int x, int y, int width, SapvpModeRanking ranking, int slotIndex) {
        int rowHeight = RANKING_ROW_HEIGHT;
        int modeColor = ModeVisuals.accentColor(ranking.modeId());
        int tierColor = TierVisuals.accentColor(ranking.tier());
        int iconColor = SAPVPTierTaggerClient.config().modeIconColorFor(ranking.modeId());
        int background = slotIndex % 2 == 0 ? 0x2C111A25 : 0x24101922;
        RankingColumns columns = rankingColumns(x, width);

        context.fill(x, y, x + width, y + rowHeight, background);
        context.fill(x, y, x + 3, y + rowHeight, modeColor);
        context.fill(x + 6, y + rowHeight - 1, x + width - 6, y + rowHeight, 0x12000000);

        int iconBoxX = columns.iconCenterX() - (RANKING_ICON_BOX_SIZE / 2);
        context.fill(iconBoxX, y + 5, iconBoxX + RANKING_ICON_BOX_SIZE, y + 29, tint(modeColor, 0x22));
        context.drawCenteredTextWithShadow(this.textRenderer, modeIconText(ranking, iconColor), columns.iconCenterX(), y + 12, iconColor);

        int pointsRight = columns.pointsRightX();
        int nameX = columns.nameStartX();
        String tierText = ranking.tier().shortLabel();
        int tierTextWidth = this.textRenderer.getWidth(tierText);
        int tierBoxWidth = Math.max(48, tierTextWidth + 22);
        int tierBoxX = columns.tierCenterX() - (tierBoxWidth / 2);
        int nameMaxWidth = Math.max(40, tierBoxX - nameX - 14);
        String modeName = this.textRenderer.trimToWidth(ranking.displayName(), nameMaxWidth);

        context.drawTextWithShadow(this.textRenderer, Text.literal(modeName), nameX, y + 13, WHITE);

        context.fill(tierBoxX, y + 8, tierBoxX + tierBoxWidth, y + 26, TierVisuals.backgroundColor(ranking.tier()));
        context.fill(tierBoxX, y + 8, tierBoxX + 2, y + 26, tierColor);
        int tierTextX = columns.tierCenterX() - (tierTextWidth / 2);
        context.drawTextWithShadow(this.textRenderer, Text.literal(tierText), tierTextX, y + 13, tierColor);

        String points = ranking.points() + " pts";
        context.drawTextWithShadow(this.textRenderer, Text.literal(points), pointsRight - this.textRenderer.getWidth(points), y + 13, GREEN);
    }

    private void drawFooterRail(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, 0x160D141C);
        context.fill(x, y, x + width, y + 1, DIVIDER);
    }

    private void drawScrollBar(DrawContext context, int x, int y, int width, int height, int totalRows, int visibleRows) {
        context.fill(x, y, x + width, y + height, 0x220A0F15);
        if (totalRows <= visibleRows || totalRows <= 0) {
            return;
        }

        int maxScroll = Math.max(1, totalRows - visibleRows);
        int thumbHeight = Math.max(20, Math.round(height * (visibleRows / (float) totalRows)));
        int thumbTravel = Math.max(0, height - thumbHeight);
        int thumbY = y + Math.round((rankingScroll / (float) maxScroll) * thumbTravel);
        context.fill(x, thumbY, x + width, thumbY + thumbHeight, 0x88A6ADC8);
    }

    private void drawScaledText(DrawContext context, String text, int x, int y, float scale, int color) {
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        context.drawTextWithShadow(this.textRenderer, Text.literal(text), 0, 0, color);
        matrices.popMatrix();
    }

    private void forceRefresh() {
        rankingScroll = 0;
        if (target.uuid() != null) {
            SAPVPTierTaggerClient.profileService().requestProfile(target.uuid(), target.displayName());
        } else if (target.displayName() != null) {
            SAPVPTierTaggerClient.profileService().requestProfileByName(target.displayName());
        }
    }

    private List<SapvpModeRanking> sortedRankings(List<SapvpModeRanking> rankings) {
        return rankings.stream()
            .filter(ranking -> ModeVisuals.isSupportedTierlistMode(ranking.modeId()))
            .sorted(RANKING_COMPARATOR)
            .toList();
    }

    private int visibleRows(int contentHeight) {
        return Math.max(1, contentHeight / RANKING_ROW_HEIGHT);
    }

    private int maxRankingScroll(List<SapvpModeRanking> rankings, int contentHeight) {
        return Math.max(0, rankings.size() - visibleRows(contentHeight));
    }

    private String fallbackTargetName() {
        if (target.displayName() != null && !target.displayName().isBlank()) {
            return target.displayName();
        }
        return target.uuid() == null ? "Desconhecido" : target.uuid().toString();
    }

    private int panelWidth() {
        return Math.min(586, this.width - 48);
    }

    private int panelHeight() {
        return Math.min(398, this.height - 26);
    }

    private int panelX() {
        return (this.width - panelWidth()) / 2;
    }

    private int panelY() {
        return (this.height - panelHeight()) / 2;
    }

    private void drawPlayerHead(DrawContext context, int x, int y, int size, @Nullable SapvpPlayerProfile profile) {
        if (this.client == null) {
            return;
        }

        context.fill(x - 2, y - 2, x + size + 2, y + size + 2, 0x44222B38);
        context.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xAA0E141D);

        SkinTextures skinTextures = resolveLiveSkin(profile);
        if (skinTextures != null) {
            PlayerSkinDrawer.draw(context, skinTextures, x, y, size);
            return;
        }

        Identifier offlineSkin = SAPVPTierTaggerClient.avatarService().getOrRequestSkinTexture(this.client, target, profile);
        if (offlineSkin != null) {
            PlayerSkinDrawer.draw(context, offlineSkin, x, y, size, true, false, -1);
            return;
        }

        Identifier headTexture = SAPVPTierTaggerClient.avatarService().getOrRequestHeadTexture(this.client, target, profile);
        if (headTexture != null) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, headTexture, x, y, size, size);
            return;
        }

        context.fill(x, y, x + size, y + size, 0xFF1B2330);
        context.fill(x + 7, y + 8, x + 13, y + 14, WHITE);
        context.fill(x + 19, y + 8, x + 25, y + 14, WHITE);
        context.fill(x + 10, y + 20, x + 22, y + 23, MUTED);
    }

    private @Nullable SkinTextures resolveLiveSkin(@Nullable SapvpPlayerProfile profile) {
        if (this.client == null || this.client.getNetworkHandler() == null) {
            return null;
        }

        if (profile != null) {
            PlayerListEntry byUuid = this.client.getNetworkHandler().getPlayerListEntry(profile.uuid());
            if (byUuid != null) {
                return byUuid.getSkinTextures();
            }
        }

        if (target.uuid() != null) {
            PlayerListEntry byTargetUuid = this.client.getNetworkHandler().getPlayerListEntry(target.uuid());
            if (byTargetUuid != null) {
                return byTargetUuid.getSkinTextures();
            }
        }

        if (target.displayName() != null) {
            PlayerListEntry byName = this.client.getNetworkHandler().getPlayerListEntry(target.displayName());
            if (byName != null) {
                return byName.getSkinTextures();
            }
        }

        return null;
    }

    private static String modeIcon(SapvpModeRanking ranking) {
        if ("EMOJI".equalsIgnoreCase(SAPVPTierTaggerClient.config().modeIconStyle)) {
            String emoji = ranking.emoji();
            if (emoji != null && !emoji.isBlank()) {
                return emoji;
            }
            return modeBadge(ranking.modeId());
        }
        return ModeVisuals.iconGlyph(ranking.modeId());
    }

    private static Text modeIconText(SapvpModeRanking ranking, int iconColor) {
        String icon = modeIcon(ranking);
        if ("EMOJI".equalsIgnoreCase(SAPVPTierTaggerClient.config().modeIconStyle)) {
            return Text.literal(icon);
        }
        return Text.literal(icon).setStyle(ModeVisuals.saIconStyle(iconColor));
    }

    private static String modeBadge(String rawMode) {
        String normalized = ModeVisuals.normalize(rawMode);
        return switch (normalized) {
            case "SMP" -> "SM";
            case "SWORD" -> "SW";
            case "AXE" -> "AX";
            case "NETHERITE_POT", "NETHERITE_OP", "NETHERITE", "NETH_OP", "NETH_POT" -> "NP";
            case "CRYSTAL" -> "CR";
            case "MACE" -> "MC";
            default -> "??";
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean contains(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static int tint(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static @Nullable PodiumStyle podiumStyle(int globalRank) {
        return switch (globalRank) {
            case 1 -> new PodiumStyle(GOLD, 0x3A2A1E08);
            case 2 -> new PodiumStyle(SILVER, 0x2E1A2028);
            case 3 -> new PodiumStyle(BRONZE, 0x34251610);
            default -> null;
        };
    }

    private static RankingColumns rankingColumns(int x, int width) {
        int iconCenterX = x + 20;
        int nameStartX = x + 42;
        int pointsRightX = x + width - 14;
        int pointsCenterX = x + width - 31;
        int tierCenterX = x + width - 115;
        return new RankingColumns(iconCenterX, nameStartX, tierCenterX, pointsCenterX, pointsRightX);
    }

    private record PodiumStyle(int accent, int fill) {
    }

    private record RankingColumns(int iconCenterX, int nameStartX, int tierCenterX, int pointsCenterX, int pointsRightX) {
    }
}
