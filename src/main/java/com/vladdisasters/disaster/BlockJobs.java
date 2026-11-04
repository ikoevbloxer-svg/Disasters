package com.vladdisasters.disaster;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Очередь массовых изменений блоков (кратеры, выжженная земля).
 * Растягивает работу на несколько тиков, чтобы сервер не зависал.
 */
public final class BlockJobs {
    private static final int BUDGET_PER_TICK = 9000;
    private static final List<Job> JOBS = new ArrayList<>();

    public interface Job {
        /** @return true, когда работа закончена */
        boolean step(ServerLevel level, int budget);
    }

    public static void add(Job job) {
        JOBS.add(job);
    }

    public static void clear() {
        JOBS.clear();
    }

    public static boolean busy() {
        return !JOBS.isEmpty();
    }

    public static void tick(ServerLevel level) {
        if (JOBS.isEmpty()) return;
        int per = Math.max(500, BUDGET_PER_TICK / JOBS.size());
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            if (it.next().step(level, per)) it.remove();
        }
    }

    // ------------------------------------------------------------------

    /** Эллипсоидный кратер. Внутри — воздух, на дне — магма/лава, по краю — обожжённый камень. */
    public static class CraterJob implements Job {
        private final BlockPos c;
        private final int rx, ry, rz;
        private final boolean molten;
        private int x, y, z;

        public CraterJob(BlockPos center, int rx, int ry, int rz, boolean molten) {
            this.c = center;
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            this.molten = molten;
            this.x = -rx;
            this.y = ry;
            this.z = -rz;
        }

        @Override
        public boolean step(ServerLevel level, int budget) {
            RandomSource r = level.random;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            while (budget-- > 0) {
                if (y < -ry - 3) return true;
                double nx = (double) x / rx, ny = (double) y / ry, nz = (double) z / rz;
                double d = nx * nx + ny * ny + nz * nz;
                p.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                if (!level.isOutsideBuildHeight(p) && level.isLoaded(p)) {
                    BlockState st = level.getBlockState(p);
                    if (d <= 1.0) {
                        if (!st.isAir() && DisasterUtil.breakable(level, p, st)) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                        }
                    } else if (d <= 1.35 && y <= 0) {
                        // Стенки кратера: оплавленная порода
                        if (!st.isAir() && DisasterUtil.breakable(level, p, st)) {
                            BlockState shell;
                            if (molten) {
                                int roll = r.nextInt(10);
                                shell = roll < 3 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                                        : roll < 6 ? Blocks.BLACKSTONE.defaultBlockState()
                                        : roll < 8 ? Blocks.BASALT.defaultBlockState()
                                        : Blocks.NETHERRACK.defaultBlockState();
                            } else {
                                shell = r.nextInt(3) == 0 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.TUFF.defaultBlockState();
                            }
                            level.setBlock(p, shell, 2 | 16);
                        }
                    }
                    // Немного лавы на самом дне
                    if (molten && d <= 1.0 && y == -ry + 1 && r.nextInt(5) == 0) {
                        level.setBlock(p, Blocks.LAVA.defaultBlockState(), 3);
                    }
                }
                // следующий блок (сверху вниз, чтобы ничего не падало)
                z++;
                if (z > rz) {
                    z = -rz;
                    x++;
                    if (x > rx) {
                        x = -rx;
                        y--;
                    }
                }
            }
            return false;
        }
    }

    /** Выжигание поверхности в кольце: листва и деревья сгорают, трава превращается в пепел и землю, огонь. */
    public static class ScorchJob implements Job {
        private final BlockPos c;
        private final int inner, outer;
        private int x, z;

        public ScorchJob(BlockPos center, int inner, int outer) {
            this.c = center;
            this.inner = inner;
            this.outer = outer;
            this.x = -outer;
            this.z = -outer;
        }

        @Override
        public boolean step(ServerLevel level, int budget) {
            RandomSource r = level.random;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            while (budget > 0) {
                if (x > outer) return true;
                double d = Math.sqrt((double) x * x + (double) z * z);
                if (d >= inner && d <= outer) {
                    int wx = c.getX() + x, wz = c.getZ() + z;
                    p.set(wx, c.getY(), wz);
                    if (level.isLoaded(p)) {
                        int top = DisasterUtil.topY(level, wx, wz);
                        double power = 1.0 - (d - inner) / (double) (outer - inner); // 1 у центра, 0 на краю
                        // Сносим деревья и растения сверху вниз
                        for (int yy = top; yy > top - 14 && yy > level.getMinBuildHeight(); yy--) {
                            p.set(wx, yy, wz);
                            BlockState st = level.getBlockState(p);
                            budget--;
                            if (st.isAir()) continue;
                            if (st.is(BlockTags.LEAVES) || st.canBeReplaced() && st.getFluidState().isEmpty()) {
                                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                            } else if (st.is(BlockTags.LOGS)) {
                                level.setBlock(p, r.nextFloat() < power ? Blocks.AIR.defaultBlockState() : Blocks.BASALT.defaultBlockState(), 2 | 16);
                            } else {
                                // Поверхность
                                if (st.is(Blocks.GRASS_BLOCK) || st.is(BlockTags.DIRT) || st.is(Blocks.SAND)) {
                                    BlockState rep = r.nextFloat() < power * 0.5 ? Blocks.NETHERRACK.defaultBlockState()
                                            : Blocks.COARSE_DIRT.defaultBlockState();
                                    level.setBlock(p, rep, 2 | 16);
                                } else if (st.is(Blocks.WATER) && power > 0.6) {
                                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                                }
                                BlockPos above = p.above();
                                if (r.nextFloat() < 0.18 * power + 0.03 && level.getBlockState(above).isAir()) {
                                    level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
                                }
                                break;
                            }
                        }
                    }
                }
                budget -= 4;
                z++;
                if (z > outer) {
                    z = -outer;
                    x++;
                }
            }
            return false;
        }
    }

    private BlockJobs() {}
}
