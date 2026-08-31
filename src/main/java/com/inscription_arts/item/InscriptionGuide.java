package com.inscription_arts.item;

import com.inscription_arts.registry.ModRunes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/**
 * 铭刻手册（内置引导书）。
 * <p>
 * 参考匠魂「秘籍」：玩家在创造标签页拿到本书后右键即可翻阅，内含从「萃取 → 建坛 → 铭刻 → 修复」
 * 的完整可预期工作流说明，以及全部符文与自创附魔的速查表。继承 {@link WrittenBookItem}，
 * 因此右键原生打开书本界面，无需额外网络包或自定义界面。
 */
public class InscriptionGuide extends WrittenBookItem {

    public static final InscriptionGuide INSTANCE = new InscriptionGuide(new Properties().stacksTo(1).fireResistant());

    public InscriptionGuide(Properties props) {
        super(props);
    }

    /**
     * 缓存默认实例：8 页 {@link Component} 的构建代价不低，而创造标签页取物品时会反复调用
     * {@link #getDefaultInstance()}。此处懒构建一次并缓存，之后只返回副本，避免重复开销。
     * <p>
     * 关键：必须在「首次调用时」（类已完全加载、{@code PAGES} 已就绪）才构建，绝不能放在静态字段
     * 初始化器中——否则会因静态初始化顺序，在 {@code PAGES} 尚未赋值时就迭代它而触发 NPE。
     */
    private static volatile ItemStack cachedInstance;

    /** 默认实例即携带写好的书页内容，所以创造标签页拿到的就是一本可翻阅的手册 */
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack cached = cachedInstance;
        if (cached == null) {
            synchronized (InscriptionGuide.class) {
                cached = cachedInstance;
                if (cached == null) {
                    ItemStack stack = new ItemStack(this);
                    stack.set(DataComponents.WRITTEN_BOOK_CONTENT, buildContent());
                    cachedInstance = cached = stack;
                }
            }
        }
        return cached.copy();
    }

    private static WrittenBookContent buildContent() {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String text : PAGES) {
            pages.add(Filterable.passThrough(Component.literal(text)));
        }
        return new WrittenBookContent(
                Filterable.passThrough("铭刻手册"),
                "铭刻之艺",
                0,
                pages,
                true);
    }

    /** 书页内容（\n 为换行，§ 为颜色代码） */
    private static final String[] PAGES = {
            """
            §l《铭刻之艺》玩法手册§r
            
            这是一个「符文铭刻」装备强化模组：
            从材料中萃取§6符文精华§r，在§5符文祭坛§r上将其
            铭刻到工具与武器，获得§a固定、可预期§r的强化，
            彻底替代原版附魔台的随机 roll。
            
            右键翻页 →""",

            """
            §l第一步 · 萃取精华§r
            
            放置§7萃取台§r，手持可萃取材料右键：
            铁锭→铁锋 · 煤→煤痕 · 铜锭→铜辉
            金锭→金运 · 纸→纸纹 · 钻石→钻耀
            绿宝石→翡华 · 下界合金锭→冥金
            每次消耗 1 个材料，得到 1 枚§6符文精华§r。""",

            """
            §l第二步 · 搭建祭坛§r
            
            以§5符文祭坛核心§r为中心：
            · 正下方一层 3×3 §8黑石§r 作基座
            · 核心同一层四角放 §5黑曜石§r 立柱
            结构不完整时无法铭刻。""",

            """
            §l第三步 · 铭刻§r
            
            右键祭坛核心打开界面：
            · §e装备§r槽放剑 / 斧 / 镐 / 弓
            · §d符文精华§r槽放 §d3 枚§r精华
            点击「铭刻」→ 主界面隐藏，
            弹出§a全屏三选一§r面板（按类别配色、悬停高亮），
            选定 1 种附魔即把 3 枚充能之和写入。
            每次铭刻§a消耗全部 3 枚精华§r，
            可反复铭刻把附魔叠到更高等级。""",

    """
            §l符文附魔一览（按工具类别）§r
            
            §c战斗§r(剑/重锤/三叉戟/权杖)：
            烈焰·淬毒·汲取·雷霆·寒霜
            §e斧§r(兼具伤害与效率，共 10 种)：
            战斗5 + 挖掘5(精炼/辉光/磁力/勘探/疾速)
            §6挖掘§r(镐)：
            精炼·辉光·磁力·勘探·疾速
            §9弓§r：
            贯穿·精准·迅捷·爆裂·灼燃
            
            任意符文均可铭刻到任意工具，
            按工具类别自动给出对应候选池。""",

    """
            §l稀有度与充能§r
            
            每次铭刻的「充能」倍率：
            普通×1 · 少见×4
            稀有×10 · 冥金×25
            
            升级到 N+1 级所需煤等价充能
            随等级按二次曲线激增：
            0→1 需 5 · 6→7 需 545
            9→10 需 1020（块煤符文）
            
            低级符文需反复铭刻；
            高级符文（如钻石）1 次可刷 2 级。
            自创附魔最高 §cX (10级)§r。""",

            """
            §l修复与提示§r
            
            · 铭刻装备本质仍是普通物品，
              可用对应材料在§7萃取台§r修复。
            · 铭刻装备请勿放入原版§5附魔台§r，
              本模组已引导其走铭刻体系。
            · 攻击 / 挖掘时效果自动生效。""",

            """
            §l符文之兵（工具）§r
            
            5 件符文工具均以下界合金为基，可铭刻任意符文：
            · §e符文之锤§r：镐，左键 3×3 同层采矿
            · §e符文之铲§r：铲，左键 3×3 同层挖掘
            · §e符文之斧§r：斧，左键 3×3 同层伐木
            · §e符文之锄§r：锄，右键 3×3 耕地
            · §e符文之剑§r：剑，左键横扫周身 3×3 生物
            范围能力只破坏对应工具正确的方块。""",

            """
            §l结语§r
            
            像匠魂一样自由组合三段符文，
            打造属于你的专属神兵。
            
            祝旅途愉快，冒险者！§6✦§r"""
    };
}
