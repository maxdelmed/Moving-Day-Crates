package dev.wwwrld.movingcrates.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;

public class ConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_WIDTH = 120;
    private static final int TEXT = 0xFFFFFF;
    private static final int ERROR = 0xFF6060;

    private final Screen parent;
    private final ForgeConfigSpec spec;
    private final List<Setting> settings = new ArrayList<>();
    private Component notice = Component.empty();
    private boolean editable;

    private interface Setting {
        AbstractWidget widget();

        boolean valid();

        void apply();

        Component label();
    }

    public ConfigScreen(Screen parent, Component title, ForgeConfigSpec spec) {
        super(title);
        this.parent = parent;
        this.spec = spec;
    }

    public static void register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context, ForgeConfigSpec spec) {
        Component title = Component.translatable("movingcrates.config.title",
                context.getContainer().getModInfo().getDisplayName());
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent, title, spec)));
    }

    @Override
    protected void init() {
        settings.clear();
        editable = spec.isLoaded() && Minecraft.getInstance().getSingleplayerServer() != null;
        if (!spec.isLoaded()) {
            notice = Component.translatable("movingcrates.config.no_world");
        } else if (!editable) {
            notice = Component.translatable("movingcrates.config.read_only");
        }
        SettingList list = new SettingList();
        if (spec.isLoaded()) {
            collect(spec.getValues(), new ArrayList<>());
            settings.forEach(setting -> {
                setting.widget().active = editable;
                list.add(new Row(setting));
            });
        }
        addRenderableWidget(list);
        int buttonY = height - 28;
        if (editable) {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save()).bounds(width / 2 - 154, buttonY, 150, 20).build());
            addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(width / 2 + 4, buttonY, 150, 20).build());
        } else {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(width / 2 - 75, buttonY, 150, 20).build());
        }
    }

    private void collect(UnmodifiableConfig config, List<String> path) {
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            List<String> here = new ArrayList<>(path);
            here.add(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig nested) {
                collect(nested, here);
            } else if (value instanceof ForgeConfigSpec.ConfigValue<?> configValue) {
                ForgeConfigSpec.ValueSpec valueSpec = spec.getSpec().get(here);
                Setting setting = settingFor(configValue, valueSpec, label(entry.getKey()));
                if (setting != null) {
                    String comment = valueSpec == null ? null : valueSpec.getComment();
                    if (comment != null && !comment.isBlank()) {
                        setting.widget().setTooltip(Tooltip.create(Component.literal(comment.trim())));
                    }
                    settings.add(setting);
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Setting settingFor(ForgeConfigSpec.ConfigValue<?> value, ForgeConfigSpec.ValueSpec valueSpec, Component label) {
        Object current = value.get();
        if (current instanceof Boolean flag) {
            ForgeConfigSpec.ConfigValue<Boolean> typed = (ForgeConfigSpec.ConfigValue<Boolean>) value;
            CycleButton<Boolean> button = CycleButton.onOffBuilder(flag).displayOnlyValue().create(0, 0, WIDGET_WIDTH, 20, label);
            return simple(button, label, () -> true, () -> typed.set(button.getValue()));
        }
        if (current instanceof Enum<?> choice) {
            ForgeConfigSpec.ConfigValue raw = value;
            List<Enum<?>> options = Arrays.asList(choice.getDeclaringClass().getEnumConstants());
            CycleButton<Enum<?>> button = CycleButton.<Enum<?>>builder(option -> Component.literal(label(option.name().toLowerCase(Locale.ROOT)).getString()))
                    .withValues(options).withInitialValue(choice).displayOnlyValue().create(0, 0, WIDGET_WIDTH, 20, label);
            return simple(button, label, () -> true, () -> raw.set(button.getValue()));
        }
        if (current instanceof Number || current instanceof String || current instanceof List<?>) {
            EditBox box = new EditBox(font, 0, 0, WIDGET_WIDTH, 20, label);
            box.setMaxLength(4096);
            box.setValue(current instanceof List<?> items ? String.join(", ", items.stream().map(String::valueOf).toList()) : String.valueOf(current));
            Supplier<Object> parsed = () -> parse(current, box.getValue());
            Supplier<Boolean> valid = () -> {
                Object result = parsed.get();
                return result != null && (valueSpec == null || valueSpec.test(result));
            };
            box.setResponder(text -> box.setTextColor(valid.get() ? 0xE0E0E0 : ERROR));
            ForgeConfigSpec.ConfigValue raw = value;
            return simple(box, label, valid, () -> raw.set(parsed.get()));
        }
        return null;
    }

    private static Object parse(Object current, String text) {
        String trimmed = text.trim();
        try {
            if (current instanceof Integer) {
                return Integer.parseInt(trimmed);
            }
            if (current instanceof Long) {
                return Long.parseLong(trimmed);
            }
            if (current instanceof Double) {
                return Double.parseDouble(trimmed);
            }
        } catch (NumberFormatException exception) {
            return null;
        }
        if (current instanceof List<?>) {
            return trimmed.isEmpty() ? new ArrayList<String>() : new ArrayList<>(Arrays.stream(trimmed.split(",")).map(String::trim)
                    .filter(item -> !item.isEmpty()).toList());
        }
        return text;
    }

    private static Setting simple(AbstractWidget widget, Component label, Supplier<Boolean> valid, Runnable apply) {
        return new Setting() {
            @Override
            public AbstractWidget widget() {
                return widget;
            }

            @Override
            public boolean valid() {
                return valid.get();
            }

            @Override
            public void apply() {
                apply.run();
            }

            @Override
            public Component label() {
                return label;
            }
        };
    }

    private static Component label(String key) {
        String spaced = key.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').toLowerCase(Locale.ROOT);
        return Component.literal(Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1));
    }

    private void save() {
        if (settings.stream().anyMatch(setting -> !setting.valid())) {
            notice = Component.translatable("movingcrates.config.invalid");
            return;
        }
        settings.forEach(Setting::apply);
        spec.save();
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, TEXT);
        if (!notice.getString().isEmpty()) {
            graphics.drawCenteredString(font, notice, width / 2, height - 42, editable ? ERROR : 0xC0C0C0);
        }
    }

    private class SettingList extends ContainerObjectSelectionList<Row> {
        SettingList() {
            super(ConfigScreen.this.minecraft, ConfigScreen.this.width, ConfigScreen.this.height, 28, ConfigScreen.this.height - 48, ROW_HEIGHT);
        }

        void add(Row row) {
            addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, width - 20);
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + getRowWidth() / 2 + 6;
        }
    }

    private class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final Setting setting;

        Row(Setting setting) {
            this.setting = setting;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovered, float partialTick) {
            graphics.drawString(font, font.plainSubstrByWidth(setting.label().getString(), width - WIDGET_WIDTH - 8), left, top + 6,
                    setting.valid() ? TEXT : ERROR, false);
            AbstractWidget widget = setting.widget();
            widget.setX(left + width - WIDGET_WIDTH);
            widget.setY(top);
            widget.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(setting.widget());
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(setting.widget());
        }
    }
}
