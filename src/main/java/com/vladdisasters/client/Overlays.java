package com.vladdisasters.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.vladdisasters.VladDisasters;
import com.vladdisasters.network.ClientDisasterState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Полноэкранные оверлеи: белая вспышка взрыва и скример. */
public final class Overlays {
    private static final ResourceLocation SCREAMER = VladDisasters.id("textures/gui/screamer.png");
    // Размер картинки скримера (см. tools/gen_assets.py)
    public static final int IMG_W = 489, IMG_H = 800;
    private static final RandomSource RAND = RandomSource.create();

    public static void renderFlash(GuiGraphics g, DeltaTracker delta) {
        int t = ClientDisasterState.flashTicks;
        if (t <= 0) return;
        float a = Math.min(1F, t / (float) Math.max(1, ClientDisasterState.flashMax) * 1.4F);
        int alpha = (int) (a * 255) & 0xFF;
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | 0xFFFFF0);
    }

    public static void renderScreamer(GuiGraphics g, DeltaTracker delta) {
        if (ClientDisasterState.screamerTicks <= 0) return;
        int w = g.guiWidth(), h = g.guiHeight();
        // Чёрный фон
        g.fill(0, 0, w, h, 0xFF000000);
        // Картинка во весь экран с тряской и рывками зума
        float zoom = 1.15F + RAND.nextFloat() * 0.25F;
        float scale = Math.max(w / (float) IMG_W, h / (float) IMG_H) * zoom;
        int dw = (int) (IMG_W * scale), dh = (int) (IMG_H * scale);
        int jx = RAND.nextInt(25) - 12, jy = RAND.nextInt(25) - 12;
        int x = (w - dw) / 2 + jx, y = (h - dh) / 2 + jy;
        RenderSystem.enableBlend();
        g.blit(SCREAMER, x, y, dw, dh, 0, 0, IMG_W, IMG_H, IMG_W, IMG_H);
        // Красные вспышки
        if (RAND.nextInt(3) == 0) g.fill(0, 0, w, h, 0x55FF0000);
        RenderSystem.disableBlend();
    }

    private Overlays() {}
}
