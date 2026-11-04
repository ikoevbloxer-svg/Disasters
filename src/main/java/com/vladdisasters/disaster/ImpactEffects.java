package com.vladdisasters.disaster;

import com.vladdisasters.network.ModNetwork;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Общие эффекты огромных взрывов: вспышка, ударная волна, кратер, выжженная земля. */
public final class ImpactEffects {

    public static void bigImpact(ServerLevel level, BlockPos center, int craterR, int craterDepth, int blastR, float maxDamage, boolean molten) {
        Vec3 c = Vec3.atCenterOf(center);
        // Вспышка и звук для всех в округе
        for (ServerPlayer p : DisasterUtil.players(level)) {
            double d = Math.sqrt(p.distanceToSqr(c));
            if (d < blastR * 6) {
                int flash = d < blastR * 2 ? 70 : 35;
                ModNetwork.send(p, new ModNetwork.FlashPayload(flash));
                p.playNotifySound(ModSounds.NUKE_BOOM.get(), SoundSource.MASTER, 1.0F, 0.9F);
            }
        }
        // Настоящие взрывы для эффекта и первичных разрушений
        for (int i = 0; i < 4; i++) {
            Vec3 o = c.add((level.random.nextDouble() - 0.5) * craterR, 0, (level.random.nextDouble() - 0.5) * craterR);
            level.explode(null, o.x, o.y, o.z, 9.0F, true, Level.ExplosionInteraction.TNT);
        }
        // Кратер и выжженная земля — по чуть-чуть каждый тик
        BlockJobs.add(new BlockJobs.CraterJob(center, craterR, craterDepth, craterR, molten));
        BlockJobs.add(new BlockJobs.ScorchJob(center, (int) (craterR * 0.9), blastR));

        // Ударная волна
        AABB box = new AABB(center).inflate(blastR * 1.5, 80, blastR * 1.5);
        for (Entity e : level.getEntities((Entity) null, box, e -> true)) {
            if (e instanceof Player pl && (pl.isCreative() || pl.isSpectator())) continue;
            Vec3 diff = e.position().subtract(c);
            double d = diff.length();
            double reach = blastR * 1.5;
            if (d > reach) continue;
            double k = 1.0 - d / reach;
            boolean sheltered = !level.canSeeSky(e.blockPosition().above());
            if (e instanceof LivingEntity le) {
                float dmg = (float) (maxDamage * k * k);
                if (sheltered) dmg *= 0.35F;
                if (dmg > 0.5F) le.hurt(level.damageSources().explosion(null, null), dmg);
                if (!sheltered) le.igniteForSeconds((float) (8 * k));
            }
            Vec3 dirv = d < 0.01 ? new Vec3(0, 1, 0) : diff.normalize();
            DisasterUtil.push(e, dirv.scale(3.5 * k).add(0, 1.2 * k, 0));
        }
        // Огненный шар
        for (ServerPlayer p : DisasterUtil.players(level)) {
            level.sendParticles(p, ParticleTypes.EXPLOSION_EMITTER, true, c.x, c.y + 4, c.z, 12, craterR * 0.6, 6, craterR * 0.6, 0);
            level.sendParticles(p, ParticleTypes.FLAME, true, c.x, c.y + 6, c.z, 400, craterR, 10, craterR, 0.3);
            level.sendParticles(p, ParticleTypes.LAVA, true, c.x, c.y + 2, c.z, 120, craterR * 0.5, 2, craterR * 0.5, 1);
        }
    }

    /** Грибовидное облако: вызывать каждый тик несколько секунд после взрыва. */
    public static void mushroomCloud(ServerLevel level, BlockPos center, int age, float scale) {
        double x = center.getX() + 0.5, y = center.getY(), z = center.getZ() + 0.5;
        double stemTop = Math.min(70, age * 0.9) * scale;
        double capR = 10 + Math.min(18, age * 0.25) * scale;
        for (ServerPlayer p : DisasterUtil.players(level)) {
            if (p.distanceToSqr(x, y, z) > 600 * 600) continue;
            // Ножка
            for (int i = 0; i < 8; i++) {
                double h = level.random.nextDouble() * stemTop;
                double r = 3 + h * 0.05;
                double a = level.random.nextDouble() * Math.PI * 2;
                level.sendParticles(p, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, x + Math.cos(a) * r, y + h, z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.02);
            }
            // Шляпка
            for (int i = 0; i < 14; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double r = level.random.nextDouble() * capR;
                double h = stemTop + (1 - r / capR) * 10 * scale + level.random.nextDouble() * 4;
                level.sendParticles(p, i % 4 == 0 ? ParticleTypes.FLAME : ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true,
                        x + Math.cos(a) * r, y + h, z + Math.sin(a) * r, 1, 0, 0, 0, 0.01);
            }
            if (age < 60 && age % 5 == 0) {
                level.sendParticles(p, ParticleTypes.EXPLOSION_EMITTER, true, x, y + stemTop, z, 2, capR * 0.5, 3, capR * 0.5, 0);
            }
        }
    }

    private ImpactEffects() {}
}
