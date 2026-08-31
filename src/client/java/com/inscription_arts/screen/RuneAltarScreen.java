package com.inscription_arts.screen;

import com.inscription_arts.network.ChooseEnchantPacket;
import com.inscription_arts.network.InscribePacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Optional;

/**
 * 符文祭坛界面。绘制一个简易面板与四个槽位（装备 + 3 个符文精华），提供「铭刻」按钮，
 * 点击后向服务端请求三选一候选。收到候选后<b>全屏覆盖</b>主界面，只显示三选一面板
 * （附魔名 + 当前等级 + 本次充能，按类别配色、悬停高亮），点选其一即把充能累加到该附魔。
 */
public class RuneAltarScreen extends AbstractContainerScreen<RuneAltarScreenHandler> {

    private Button inscribeButton;
    /** 三选一候选按钮（初始隐藏，收到候选后显示）；坐标在 init 中按面板居中计算并缓存 */
    private final Button[] choiceButtons = new Button[3];
    private final int[] choiceX = new int[3];
    private final int[] choiceY = new int[3];
    private static final int CHOICE_W = 140;
    private static final int CHOICE_H = 22;
    private Button cancelButton;
    private int cancelX, cancelY;
    private static final int CANCEL_W = 60, CANCEL_H = 20;

    /** 当前待选状态（由 S2C 包驱动） */
    private boolean offerOpen = false;
    private List<ResourceLocation> offerIds = List.of();
    private float offerCharge = 0f;

    public RuneAltarScreen(RuneAltarScreenHandler menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        this.inscribeButton = Button.builder(Component.literal("铭刻"),
                        b -> {
                            if (this.menu.inscriptionReady()) {
                                ClientPlayNetworking.send(new InscribePacket(this.menu.getPos()));
                            }
                        })
                .bounds(x + 70, y + 64, 36, 20)
                .build();
        this.addRenderableWidget(this.inscribeButton);

        // 三选一按钮：固定 3 个，初始隐藏；坐标缓存供覆盖层绘制
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            int bx = x + (this.imageWidth - CHOICE_W) / 2;
            int by = y + 44 + i * (CHOICE_H + 6);
            choiceX[i] = bx;
            choiceY[i] = by;
            Button btn = Button.builder(Component.literal(""),
                            b -> onChoose(idx))
                    .bounds(bx, by, CHOICE_W, CHOICE_H)
                    .build();
            btn.visible = false;
            this.choiceButtons[i] = btn;
            this.addRenderableWidget(btn);
        }

        this.cancelX = x + (this.imageWidth - CANCEL_W) / 2;
        this.cancelY = y + 44 + 3 * (CHOICE_H + 6);
        this.cancelButton = Button.builder(Component.literal("取消"),
                        b -> clearOffer())
                .bounds(this.cancelX, this.cancelY, CANCEL_W, CANCEL_H)
                .build();
        this.cancelButton.visible = false;
        this.addRenderableWidget(this.cancelButton);
    }

    /** 由客户端网络接收器调用：弹出三选一覆盖层 */
    public void showOffer(List<ResourceLocation> ids, float charge) {
        this.offerOpen = true;
        this.offerIds = ids;
        this.offerCharge = charge;
        ItemStack equipment = this.menu.getSlot(0).getItem();
        for (int i = 0; i < 3; i++) {
            Button btn = this.choiceButtons[i];
            if (i < ids.size()) {
                ResourceLocation id = ids.get(i);
                int level = levelOf(id, equipment);
                String name = Component.translatable(
                        "enchantment." + id.getNamespace() + "." + id.getPath()).getString();
                String lvlText = level <= 0 ? "未附魔" : ("Lv." + level);
                btn.setMessage(Component.literal(name + "  ·  " + lvlText
                        + "  ·  充能+" + (int) charge));
                btn.visible = true;
            } else {
                btn.visible = false;
            }
        }
        this.cancelButton.visible = true;
        this.inscribeButton.visible = false;
    }

    /** 关闭覆盖层（取消或已选择） */
    public void clearOffer() {
        this.offerOpen = false;
        this.offerIds = List.of();
        for (Button btn : this.choiceButtons) {
            btn.visible = false;
        }
        this.cancelButton.visible = false;
        this.inscribeButton.visible = true;
    }

    private void onChoose(int index) {
        if (!offerOpen || index < 0 || index >= offerIds.size()) {
            return;
        }
        ClientPlayNetworking.send(new ChooseEnchantPacket(this.menu.getPos(), index));
        clearOffer();
    }

    /** 读客户端装备上某附魔的当前等级（用于候选按钮展示） */
    private int levelOf(ResourceLocation id, ItemStack equipment) {
        if (equipment.isEmpty() || Minecraft.getInstance().level == null) {
            return 0;
        }
        RegistryAccess access = Minecraft.getInstance().level.registryAccess();
        Registry<Enchantment> reg = access.registryOrThrow(Registries.ENCHANTMENT);
        var holder = reg.getHolder(ResourceKey.create(Registries.ENCHANTMENT, id));
        if (holder.isEmpty()) {
            return 0;
        }
        return equipment.getEnchantments().getLevel(holder.get());
    }

    /** 按附魔 id 取分类配色（战斗=金 / 挖掘=青 / 弓=蓝） */
    private int colorOf(ResourceLocation id) {
        return switch (id.getPath()) {
            case "blazing", "venom", "siphon", "thunder", "frost" -> 0xFFE0A040;
            case "refine", "radiance", "magnetism", "prospect", "haste" -> 0xFF40C0E0;
            case "pierce", "precision", "velocity", "explosive", "incinerate" -> 0xFF6080E0;
            default -> 0xFFB0B0B0;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (offerOpen) {
            // 覆盖层模式下只处理按钮（三选一/取消），屏蔽槽位交互
            for (Button b : this.choiceButtons) {
                if (b.visible && b.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            if (this.cancelButton.visible && this.cancelButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float delta, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 面板背景
        gui.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF3B2F2F);
        // 铭刻区域底色
        gui.fill(x + 7, y + 16, x + 169, y + 72, 0xFF1B1414);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        if (offerOpen) {
            // —— 三选一覆盖层：隐藏主界面，只画候选面板 ——
            this.renderBackground(gui, mouseX, mouseY, delta);
            // 全屏暗化遮罩，盖住后面的槽位/背包
            gui.fill(0, 0, this.width, this.height, 0xCC0A0A0F);
            // 中央面板
            int px = this.leftPos + 8;
            int py = this.topPos + 20;
            int pw = this.imageWidth - 16;
            int ph = 44 + 3 * (CHOICE_H + 6) + CANCEL_H + 16;
            gui.fill(px, py, px + pw, py + ph, 0xFF161019);
            gui.fill(px, py, px + pw, py + 2, 0xFFE0B050);
            // 标题与提示
            gui.drawCenteredString(this.font,
                    Component.translatable("message.inscription_arts.offer_title"),
                    this.width / 2, py + 8, 0xFFE0B050);
            gui.drawCenteredString(this.font,
                    Component.translatable("message.inscription_arts.choose_hint"),
                    this.width / 2, py + 24, 0xFFA0E0FF);

            // 候选按钮（手动绘制，带分类配色 + 悬停高亮）
            for (int i = 0; i < 3; i++) {
                Button b = this.choiceButtons[i];
                if (!b.visible) {
                    continue;
                }
                int bx = choiceX[i], by = choiceY[i];
                boolean hover = mouseX >= bx && mouseX <= bx + CHOICE_W
                        && mouseY >= by && mouseY <= by + CHOICE_H;
                int accent = colorOf(offerIds.get(i));
                // 外框
                gui.fill(bx - 2, by - 2, bx + CHOICE_W + 2, by + CHOICE_H + 2,
                        hover ? 0xFF6A6A8A : 0xFF3A3A4A);
                // 面板
                gui.fill(bx, by, bx + CHOICE_W, by + CHOICE_H,
                        hover ? 0xFF2E2E44 : 0xFF1A1A26);
                // 左侧分类色条
                gui.fill(bx, by, bx + 3, by + CHOICE_H, accent);
                // 文本
                int tx = bx + 10;
                int ty = by + (CHOICE_H - this.font.lineHeight) / 2;
                gui.drawString(this.font, b.getMessage(), tx, ty,
                        hover ? 0xFFFFFFFF : 0xFFDCDCE6, false);
                if (hover) {
                    gui.renderTooltip(this.font, b.getMessage(), mouseX, mouseY);
                }
            }
            // 取消按钮
            boolean chover = mouseX >= cancelX && mouseX <= cancelX + CANCEL_W
                    && mouseY >= cancelY && mouseY <= cancelY + CANCEL_H;
            gui.fill(cancelX - 2, cancelY - 2, cancelX + CANCEL_W + 2, cancelY + CANCEL_H + 2,
                    chover ? 0xFF5A3A3A : 0xFF3A2A2A);
            gui.fill(cancelX, cancelY, cancelX + CANCEL_W, cancelY + CANCEL_H,
                    chover ? 0xFF3A2020 : 0xFF2A1616);
            gui.drawCenteredString(this.font, this.cancelButton.getMessage(),
                    cancelX + CANCEL_W / 2, cancelY + (CANCEL_H - this.font.lineHeight) / 2,
                    chover ? 0xFFFFFFFF : 0xFFDCDCE6);
            return;
        }

        // —— 正常模式：主界面 ——
        this.renderBackground(gui, mouseX, mouseY, delta);
        if (!this.menu.inscriptionReady()) {
            this.inscribeButton.active = false;
        } else {
            this.inscribeButton.active = true;
        }
        super.render(gui, mouseX, mouseY, delta);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xEEEEEE, false);
        gui.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                0xEEEEEE, false);

        // 槽位标注：左侧放装备，右侧放 3 个铭文精华
        Component equipLabel = Component.literal("装备（剑/斧/镐/弓）");
        gui.drawString(this.font, equipLabel, 8, 18, 0xFFE0B050, false);

        Component runeLabel = Component.literal("符文精华 ×3");
        gui.drawString(this.font, runeLabel, 70, 6, 0xFFC080E0, false);

        if (!offerOpen && !this.menu.inscriptionReady()) {
            Component hint = Component.translatable("message.inscription_arts.inscribe_hint");
            gui.drawCenteredString(this.font, hint, this.width / 2, this.topPos + 78, 0xFFE06060);
        }
    }
}
