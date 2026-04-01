package com.sapvp.tiertagger.gui;

import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import com.sapvp.tiertagger.config.SapvpTierTaggerConfig;
import com.sapvp.tiertagger.util.ModeIconStyleOption;
import com.sapvp.tiertagger.util.NametagModeOption;
import com.sapvp.tiertagger.util.NametagSideOption;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class SapvpConfigScreen {
    private SapvpConfigScreen() {
    }

    public static Screen create(Screen parent) {
        return createAdvanced(parent);
    }

    public static Screen createAdvanced(Screen parent) {
        SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.empty()
                .append(Text.literal("SETTINGS.").formatted(Formatting.AQUA))
                .append(Text.literal("  Advanced editor").formatted(Formatting.GRAY)))
            .setTransparentBackground(true);

        builder.setSavingRunnable(() -> {
            config.normalize();
            SAPVPTierTaggerClient.saveConfig();
        });

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory visibility = builder.getOrCreateCategory(Text.empty()
            .append(Text.literal("Visibilidade").formatted(Formatting.WHITE))
            .append(Text.literal(config.enabled ? "  ON" : "  OFF")
                .formatted(config.enabled ? Formatting.GREEN : Formatting.RED)));
        visibility.addEntry(entries.startTextDescription(Text.literal(
            "Base principal do mod. Aqui ficam os controles que ligam ou desligam a experiencia inteira."
        ).formatted(Formatting.GRAY)).build());
        visibility.addEntry(entries.startBooleanToggle(Text.literal("Ativar mod"), config.enabled)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Liga ou desliga todo o SAPVPTierTagger."))
            .setSaveConsumer(value -> config.enabled = value)
            .build());
        visibility.addEntry(entries.startBooleanToggle(Text.literal("Apenas no multiplayer"), config.onlyInMultiplayer)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Evita rodar o mod em mundos singleplayer."))
            .setSaveConsumer(value -> config.onlyInMultiplayer = value)
            .build());
        visibility.addEntry(entries.startBooleanToggle(Text.literal("Mostrar tiers nas nametags"), config.renderNametags)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Prefixa o tier diretamente na nametag do jogador."))
            .setSaveConsumer(value -> config.renderNametags = value)
            .build());

        ConfigCategory nametag = builder.getOrCreateCategory(Text.literal("Tier Tag"));
        nametag.addEntry(entries.startTextDescription(Text.literal(
            "Configure o lado, o modo exibido e o estilo dos icones usados na nametag in-game."
        ).formatted(Formatting.GRAY)).build());
        nametag.addEntry(entries.startEnumSelector(Text.literal("Modo exibido"), NametagModeOption.class, NametagModeOption.fromModeId(config.nametagMode))
            .setDefaultValue(NametagModeOption.BEST)
            .setEnumNameProvider(value -> Text.literal(((NametagModeOption) value).displayLabel()))
            .setTooltip(Text.literal("BEST usa o melhor tier disponivel entre os 6 modos da SAPVP."))
            .setSaveConsumer(value -> config.nametagMode = value.modeId())
            .build());
        nametag.addEntry(entries.startEnumSelector(Text.literal("Lado da tag"), NametagSideOption.class, NametagSideOption.fromId(config.nametagSide))
            .setDefaultValue(NametagSideOption.LEFT)
            .setEnumNameProvider(value -> Text.literal(((NametagSideOption) value).label()))
            .setTooltip(Text.literal("Esquerda = tier antes do nick. Direita = tier depois do nick."))
            .setSaveConsumer(value -> config.nametagSide = value.id())
            .build());
        nametag.addEntry(entries.startEnumSelector(Text.literal("Estilo dos icones"), ModeIconStyleOption.class, ModeIconStyleOption.fromId(config.modeIconStyle))
            .setDefaultValue(ModeIconStyleOption.SA_ICONS)
            .setEnumNameProvider(value -> Text.literal(((ModeIconStyleOption) value).label()))
            .setTooltip(Text.literal("SA Icons usa os simbolos da tierlist. Emoji usa placeholders em texto."))
            .setSaveConsumer(value -> config.modeIconStyle = value.id())
            .build());
        nametag.addEntry(entries.startBooleanToggle(Text.literal("Mostrar icone do modo"), config.showModeIconInNametag)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Mostra o icone do modo antes do tier na tag."))
            .setSaveConsumer(value -> config.showModeIconInNametag = value)
            .build());
        nametag.addEntry(entries.startBooleanToggle(Text.literal("Usar icone alternativo"), config.showDiamondIcon)
            .setDefaultValue(true)
            .setTooltip(Text.literal("Quando o icone do modo estiver desligado, usa o simbolo padrao."))
            .setSaveConsumer(value -> config.showDiamondIcon = value)
            .build());
        nametag.addEntry(entries.startStrField(Text.literal("Formato do tier"), config.nametagFormat)
            .setDefaultValue("%tier%")
            .setTooltip(Text.literal("Use %tier% no texto. Exemplo: [%tier%] ou %tier%."))
            .setSaveConsumer(value -> config.nametagFormat = value == null || value.isBlank() ? "%tier%" : value.trim())
            .build());
        nametag.addEntry(entries.startIntField(Text.literal("Distancia maxima"), config.maxRenderDistance)
            .setDefaultValue(96)
            .setMin(16)
            .setMax(192)
            .setTooltip(Text.literal("Define ate onde os tiers aparecem nas nametags."))
            .setSaveConsumer(value -> config.maxRenderDistance = value)
            .build());

        ConfigCategory palette = builder.getOrCreateCategory(Text.literal("Paleta"));
        palette.addEntry(entries.startTextDescription(Text.literal(
            "Ajuste as cores dos tiers e dos icones. A paleta padrao agora segue a leitura visual do MCTiers e da UI da SAPVP."
        ).formatted(Formatting.GRAY)).build());
        palette.addEntry(alphaColor(entries, "HT1", "High Tier 1", config.ht1Color, SapvpTierTaggerConfig.DEFAULT_HT1_COLOR, value -> config.ht1Color = value));
        palette.addEntry(alphaColor(entries, "LT1", "Low Tier 1", config.lt1Color, SapvpTierTaggerConfig.DEFAULT_LT1_COLOR, value -> config.lt1Color = value));
        palette.addEntry(alphaColor(entries, "HT2", "High Tier 2", config.ht2Color, SapvpTierTaggerConfig.DEFAULT_HT2_COLOR, value -> config.ht2Color = value));
        palette.addEntry(alphaColor(entries, "LT2", "Low Tier 2", config.lt2Color, SapvpTierTaggerConfig.DEFAULT_LT2_COLOR, value -> config.lt2Color = value));
        palette.addEntry(alphaColor(entries, "HT3", "High Tier 3", config.ht3Color, SapvpTierTaggerConfig.DEFAULT_HT3_COLOR, value -> config.ht3Color = value));
        palette.addEntry(alphaColor(entries, "LT3", "Low Tier 3", config.lt3Color, SapvpTierTaggerConfig.DEFAULT_LT3_COLOR, value -> config.lt3Color = value));
        palette.addEntry(alphaColor(entries, "HT4", "High Tier 4", config.ht4Color, SapvpTierTaggerConfig.DEFAULT_HT4_COLOR, value -> config.ht4Color = value));
        palette.addEntry(alphaColor(entries, "LT4", "Low Tier 4", config.lt4Color, SapvpTierTaggerConfig.DEFAULT_LT4_COLOR, value -> config.lt4Color = value));
        palette.addEntry(alphaColor(entries, "HT5", "High Tier 5", config.ht5Color, SapvpTierTaggerConfig.DEFAULT_HT5_COLOR, value -> config.ht5Color = value));
        palette.addEntry(alphaColor(entries, "LT5", "Low Tier 5", config.lt5Color, SapvpTierTaggerConfig.DEFAULT_LT5_COLOR, value -> config.lt5Color = value));
        palette.addEntry(entries.startAlphaColorField(Text.literal("Cor do icone alternativo"), config.modeIconColor)
            .setDefaultValue(0xFFFFFFFF)
            .setTooltip(Text.literal("Usada pelo diamante/fallback quando nao houver cor especifica por modo."))
            .setSaveConsumer(value -> {
                config.modeIconColor = value;
                config.diamondColor = value;
            })
            .build());
        palette.addEntry(entries.startTextDescription(Text.literal(
            "Cores dos icones por modo. Essas cores aparecem no perfil e tambem na tier tag quando o icone do modo estiver ligado."
        ).formatted(Formatting.GRAY)).build());
        palette.addEntry(modeColor(entries, "Sword", config.swordIconColor, SapvpTierTaggerConfig.DEFAULT_SWORD_ICON_COLOR, value -> config.swordIconColor = value));
        palette.addEntry(modeColor(entries, "Crystal", config.crystalIconColor, SapvpTierTaggerConfig.DEFAULT_CRYSTAL_ICON_COLOR, value -> config.crystalIconColor = value));
        palette.addEntry(modeColor(entries, "Mace", config.maceIconColor, SapvpTierTaggerConfig.DEFAULT_MACE_ICON_COLOR, value -> config.maceIconColor = value));
        palette.addEntry(modeColor(entries, "SMP", config.smpIconColor, SapvpTierTaggerConfig.DEFAULT_SMP_ICON_COLOR, value -> config.smpIconColor = value));
        palette.addEntry(modeColor(entries, "Axe", config.axeIconColor, SapvpTierTaggerConfig.DEFAULT_AXE_ICON_COLOR, value -> config.axeIconColor = value));
        palette.addEntry(modeColor(entries, "NetheritePot", config.netheritePotIconColor, SapvpTierTaggerConfig.DEFAULT_NETHERITE_POT_ICON_COLOR, value -> config.netheritePotIconColor = value));
        palette.addEntry(entries.startAlphaColorField(Text.literal("Cor do separador"), config.separatorColor)
            .setDefaultValue(0xFFB7C0CC)
            .setTooltip(Text.literal("Cor do separador | entre tier e nick."))
            .setSaveConsumer(value -> config.separatorColor = value)
            .build());
        palette.addEntry(entries.startAlphaColorField(Text.literal("Cor do nome"), config.nameColor)
            .setDefaultValue(0xFFFFFFFF)
            .setTooltip(Text.literal("Cor do nick do jogador dentro da nametag."))
            .setSaveConsumer(value -> config.nameColor = value)
            .build());

        ConfigCategory performance = builder.getOrCreateCategory(Text.literal("Cache e API"));
        performance.addEntry(entries.startTextDescription(Text.literal(
            "Mais alto = menos requests. Mais baixo = atualizacao mais rapida."
        ).formatted(Formatting.GRAY)).build());
        performance.addEntry(entries.startIntField(Text.literal("Cooldown do cache (segundos)"), config.cacheCooldownSeconds)
            .setDefaultValue(20)
            .setMin(2)
            .setMax(120)
            .setTooltip(Text.literal("Tempo minimo antes de consultar o mesmo jogador novamente."))
            .setSaveConsumer(value -> config.cacheCooldownSeconds = value)
            .build());
        performance.addEntry(entries.startIntField(Text.literal("Atualizacao de proximos (ticks)"), config.nearbyRefreshTicks)
            .setDefaultValue(80)
            .setMin(20)
            .setMax(400)
            .setTooltip(Text.literal("Frequencia da busca em lote por jogadores proximos."))
            .setSaveConsumer(value -> config.nearbyRefreshTicks = value)
            .build());
        performance.addEntry(entries.startIntField(Text.literal("Raio de busca"), config.requestRadius)
            .setDefaultValue(64)
            .setMin(16)
            .setMax(256)
            .setTooltip(Text.literal("Distancia maxima para rastrear jogadores proximos automaticamente."))
            .setSaveConsumer(value -> config.requestRadius = value)
            .build());
        performance.addEntry(entries.startIntField(Text.literal("Maximo de jogadores rastreados"), config.maxTrackedPlayers)
            .setDefaultValue(32)
            .setMin(1)
            .setMax(128)
            .setTooltip(Text.literal("Limite de jogadores processados ao mesmo tempo."))
            .setSaveConsumer(value -> config.maxTrackedPlayers = value)
            .build());

        return builder.build();
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> alphaColor(
        ConfigEntryBuilder entries,
        String shortName,
        String fullName,
        int currentColor,
        int defaultColor,
        java.util.function.Consumer<Integer> saveConsumer
    ) {
        return entries.startAlphaColorField(tierLabel(shortName, fullName, currentColor), currentColor)
            .setDefaultValue(defaultColor)
            .setTooltip(Text.literal("Cor usada para " + shortName + "."))
            .setSaveConsumer(saveConsumer)
            .build();
    }

    private static Text tierLabel(String shortName, String fullName, int color) {
        return Text.empty()
            .append(Text.literal(shortName).setStyle(Style.EMPTY.withColor(color & 0x00FFFFFF)))
            .append(Text.literal(" - " + fullName).formatted(Formatting.GRAY));
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> modeColor(
        ConfigEntryBuilder entries,
        String label,
        int currentColor,
        int defaultColor,
        java.util.function.Consumer<Integer> saveConsumer
    ) {
        return entries.startAlphaColorField(Text.literal(label), currentColor)
            .setDefaultValue(defaultColor)
            .setTooltip(Text.literal("Cor padrao do icone do modo " + label + "."))
            .setSaveConsumer(saveConsumer)
            .build();
    }
}
