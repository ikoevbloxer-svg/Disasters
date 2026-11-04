package com.vladdisasters.entity;

import com.vladdisasters.disaster.DisasterUtil;
import com.vladdisasters.registry.ModItems;
import com.vladdisasters.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Мама Влада — огромное толстое существо с рыжими волосами.
 * <ul>
 *     <li>DISASTER — бедствие: пожирает блоки кусками 7x7, орёт, очень сильная.</li>
 *     <li>HOME — живёт в своём доме: слабее, блоки не ест.</li>
 *     <li>ALLY — призвана колбасой на 120 секунд, сражается за игрока.</li>
 * </ul>
 */
public class MamaVladEntity extends Monster {
    public static final int VARIANT_DISASTER = 0;
    public static final int VARIANT_HOME = 1;
    public static final int VARIANT_ALLY = 2;
    public static final int ALLY_LIFETIME = 120 * 20;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(MamaVladEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EATING = SynchedEntityData.defineId(MamaVladEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SCREAMING = SynchedEntityData.defineId(MamaVladEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Мамка Влада").withStyle(ChatFormatting.DARK_RED),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    @Nullable
    private UUID ownerId;
    private int lifeTicks = -1;
    private int eatCooldown = 50;
    private int screamCooldown = 40;

    public MamaVladEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 80;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.STEP_HEIGHT, 1.6D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, VARIANT_DISASTER);
        builder.define(EATING, 0);
        builder.define(SCREAMING, 0);
    }

    // ------------------------------------------------------------ варианты

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public boolean isAlly() {
        return getVariant() == VARIANT_ALLY;
    }

    public boolean isDisaster() {
        return getVariant() == VARIANT_DISASTER;
    }

    public int eatingTicks() {
        return this.entityData.get(EATING);
    }

    public int screamingTicks() {
        return this.entityData.get(SCREAMING);
    }

    public void setLifeTicks(int ticks) {
        this.lifeTicks = ticks;
    }

    public void setOwner(UUID owner) {
        this.ownerId = owner;
    }

    private void base(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        AttributeInstance inst = getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    public void setupVariant(int variant) {
        this.entityData.set(VARIANT, variant);
        switch (variant) {
            case VARIANT_HOME -> {
                base(Attributes.MAX_HEALTH, 100);
                base(Attributes.ATTACK_DAMAGE, 5);
                base(Attributes.MOVEMENT_SPEED, 0.23);
                base(Attributes.FOLLOW_RANGE, 18);
                base(Attributes.ARMOR, 2);
                base(Attributes.SCALE, 0.72);
                this.xpReward = 30;
            }
            case VARIANT_ALLY -> {
                base(Attributes.MAX_HEALTH, 220);
                base(Attributes.ATTACK_DAMAGE, 18);
                base(Attributes.MOVEMENT_SPEED, 0.33);
                base(Attributes.FOLLOW_RANGE, 32);
                base(Attributes.SCALE, 0.8);
                this.xpReward = 0;
                this.lifeTicks = ALLY_LIFETIME;
                bossEvent.setName(Component.literal("Мама Влада (на твоей стороне)").withStyle(ChatFormatting.GREEN));
                bossEvent.setColor(BossEvent.BossBarColor.GREEN);
            }
            default -> {
            }
        }
        setHealth(getMaxHealth());
    }

    @Nullable
    public Player getOwner() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    // ------------------------------------------------------------ ИИ

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 20.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> !isAlly()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
                e -> isAlly() && e instanceof Enemy && !(e instanceof MamaVladEntity m && m.isAlly())));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (isAlly() && target != null) {
            if (target instanceof Player) return;
            if (target instanceof MamaVladEntity m && m.isAlly()) return;
        }
        super.setTarget(target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;

        int eat = eatingTicks();
        if (eat > 0) this.entityData.set(EATING, eat - 1);
        int scr = screamingTicks();
        if (scr > 0) this.entityData.set(SCREAMING, scr - 1);

        if (lifeTicks > 0) {
            lifeTicks--;
            if (lifeTicks == 0) {
                leave();
                return;
            }
        }

        switch (getVariant()) {
            case VARIANT_DISASTER -> {
                bossEvent.setProgress(getHealth() / getMaxHealth());
                if (sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                    eatCooldown--;
                    boolean stuck = horizontalCollision || (getTarget() != null && getNavigation().isDone() && distanceToSqr(getTarget()) > 16);
                    if (eatCooldown <= 0 || (stuck && eatCooldown < 25)) {
                        eatAhead(sl);
                        eatCooldown = 45 + random.nextInt(25);
                    }
                }
            }
            case VARIANT_ALLY -> {
                bossEvent.setProgress(Math.max(0F, lifeTicks / (float) ALLY_LIFETIME));
                allyLogic();
            }
            default -> {
            }
        }

        if (--screamCooldown <= 0) {
            scream();
            screamCooldown = isDisaster() ? 90 + random.nextInt(140) : 260 + random.nextInt(300);
        }
    }

    private void allyLogic() {
        Player owner = getOwner();
        if (owner == null) return;
        LivingEntity tgt = getTarget();
        if (tgt == null || !tgt.isAlive()) {
            LivingEntity cand = owner.getLastHurtByMob();
            if (cand == null || !cand.isAlive()) cand = owner.getLastHurtMob();
            if (cand != null && cand.isAlive() && cand != this && !(cand instanceof Player)) setTarget(cand);
        }
        double d = distanceToSqr(owner);
        if (getTarget() == null && d > 10 * 10) getNavigation().moveTo(owner, 1.25D);
        if (d > 40 * 40) teleportTo(owner.getX(), owner.getY(), owner.getZ());
    }

    /** Пожирание блоков куском 7x7 (и 7 в высоту) прямо перед собой. */
    private void eatAhead(ServerLevel sl) {
        Vec3 dir;
        LivingEntity t = getTarget();
        if (t != null) {
            dir = new Vec3(t.getX() - getX(), 0, t.getZ() - getZ());
        } else {
            dir = Vec3.directionFromRotation(0, getYRot());
        }
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(1, 0, 0);
        dir = dir.normalize();
        BlockPos c = BlockPos.containing(getX() + dir.x * 3.5, getY(), getZ() + dir.z * 3.5);
        int yMin = c.getY();
        int yMax = c.getY() + 6;
        if (t != null && t.getY() < getY() - 2) yMin -= 3;  // жертва ниже — прогрызаем вниз
        if (t != null && t.getY() > getY() + 4) yMax += 3;  // жертва выше — прогрызаем вверх

        int eaten = 0;
        BlockState sample = null;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = yMin; y <= yMax; y++) {
                    BlockPos p = new BlockPos(c.getX() + x, y, c.getZ() + z);
                    if (!sl.isLoaded(p)) continue;
                    BlockState st = sl.getBlockState(p);
                    if (st.isAir() || !DisasterUtil.breakable(sl, p, st)) continue;
                    if (sample == null && st.getFluidState().isEmpty()) sample = st;
                    if (random.nextInt(10) == 0) sl.destroyBlock(p, false, this);
                    else sl.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    eaten++;
                }
            }
        }
        // Предметы на земле она тоже съедает
        for (ItemEntity item : sl.getEntitiesOfClass(ItemEntity.class, new AABB(c).inflate(4, 4, 4))) item.discard();

        if (eaten > 0) {
            this.entityData.set(EATING, 24);
            heal(Math.min(12F, eaten * 0.08F));
            playSound(ModSounds.MAMA_EAT.get(), 3.0F, 0.8F + random.nextFloat() * 0.3F);
            if (sample != null) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, sample),
                        getX() + dir.x * 2, getY() + getBbHeight() * 0.75, getZ() + dir.z * 2, 60, 1.2, 0.8, 1.2, 0.15);
            }
        }
    }

    public void scream() {
        this.entityData.set(SCREAMING, 30);
        playSound(random.nextInt(3) == 0 ? ModSounds.MAMA_ROAR.get() : ModSounds.MAMA_SCREAM.get(), getSoundVolume(), 0.9F + random.nextFloat() * 0.2F);
    }

    /** Мама уходит (конец бедствия / конец призыва). */
    public void leave() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 80, getBbWidth() / 2, getBbHeight() / 2, getBbWidth() / 2, 0.05);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 1, getZ(), 30, 1.5, 1.5, 1.5, 0.02);
            playSound(ModSounds.MAMA_ROAR.get(), getSoundVolume(), 0.8F);
            Player owner = getOwner();
            if (isAlly() && owner != null) {
                owner.displayClientMessage(Component.literal("Мама Влада доела колбасу и ушла домой.").withStyle(ChatFormatting.GOLD), false);
            } else if (isDisaster()) {
                DisasterUtil.broadcast(sl, Component.literal("Мамка Влада наелась и ушла... пока что.").withStyle(ChatFormatting.DARK_RED));
            }
        }
        discard();
    }

    // ------------------------------------------------------------ бой

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok) {
            this.entityData.set(EATING, 14);
            DisasterUtil.push(target, new Vec3(0, isDisaster() ? 0.6 : 0.35, 0));
            playSound(ModSounds.MAMA_ROAR.get(), getSoundVolume() * 0.7F, 1.1F);
        }
        return ok;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.DROWN)) return true;
        if (isDisaster() && source.is(DamageTypeTags.IS_EXPLOSION)) return true; // бомбы ей нипочём
        if (isAlly() && source.getEntity() instanceof Player) return true;    // своих не бьём
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (isAlly()) return;
        spawnAtLocation(new ItemStack(ModItems.KOLBASA.get(), isDisaster() ? 2 + random.nextInt(3) : 1));
        spawnAtLocation(new ItemStack(Items.COOKED_PORKCHOP, 3 + random.nextInt(4)));
        if (isDisaster()) spawnAtLocation(new ItemStack(Items.DIAMOND, 1 + random.nextInt(3)));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ------------------------------------------------------------ звуки

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextBoolean() ? ModSounds.MAMA_ROAR.get() : ModSounds.MAMA_SCREAM.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MAMA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MAMA_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return isDisaster() ? 4.0F : 2.0F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return isDisaster() ? 80 : 220;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    // ------------------------------------------------------------ босс-бар

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (getVariant() != VARIANT_HOME) bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    // ------------------------------------------------------------ сохранение

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("Life", lifeTicks);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int v = tag.getInt("Variant");
        this.entityData.set(VARIANT, v);
        if (tag.contains("Life")) this.lifeTicks = tag.getInt("Life");
        if (tag.hasUUID("Owner")) this.ownerId = tag.getUUID("Owner");
        if (v == VARIANT_ALLY) {
            bossEvent.setName(Component.literal("Мама Влада (на твоей стороне)").withStyle(ChatFormatting.GREEN));
            bossEvent.setColor(BossEvent.BossBarColor.GREEN);
        }
        // Бедствие после перезахода не должно жить вечно
        if (v == VARIANT_DISASTER && lifeTicks < 0 && tag.getBoolean("FromDisaster")) this.lifeTicks = 20 * 60;
    }
}
