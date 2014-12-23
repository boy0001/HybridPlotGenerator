package com.empcraft.hybrid;

import com.intellectualcrafters.plot.object.PlotBlock;
import com.intellectualcrafters.plot.object.PlotWorld;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;

import java.util.Random;

/**
 * @author Citymonstret
 */
public class HybridPop extends BlockPopulator {

    /*
     * Sorry, this isn't well documented at the moment.
     * We advise you to take a look at a world generation tutorial for
     * information about how a BlockPopulator works.
     */

    final int plotsize;
    final int pathsize;
    final PlotBlock wall;
    final PlotBlock wallfilling;
    final PlotBlock floor1;
    final PlotBlock floor2;
    final int size;
    final int roadheight;
    final int wallheight;
    final int plotheight;
    final PlotBlock[] plotfloors;
    final PlotBlock[] filling;
    private final HybridPlotWorld plotworld;
    final private double pathWidthLower;
    Biome biome;
    private int X;
    private int Z;
    private long state;

    public HybridPop(final PlotWorld pw) {
        this.plotworld = (HybridPlotWorld) pw;

        // save configuration

        this.plotsize = this.plotworld.PLOT_WIDTH;
        this.pathsize = this.plotworld.ROAD_WIDTH;

        this.floor1 = this.plotworld.ROAD_BLOCK;
        this.floor2 = this.plotworld.ROAD_STRIPES;

        this.wallfilling = this.plotworld.WALL_FILLING;
        this.size = this.pathsize + this.plotsize;
        this.wall = this.plotworld.WALL_BLOCK;

        this.plotfloors = this.plotworld.TOP_BLOCK;
        this.filling = this.plotworld.MAIN_BLOCK;

        this.wallheight = this.plotworld.WALL_HEIGHT;
        this.roadheight = this.plotworld.ROAD_HEIGHT;
        this.plotheight = this.plotworld.PLOT_HEIGHT;

        if ((this.pathsize % 2) == 0) {
            this.pathWidthLower = Math.floor(this.pathsize / 2) - 1;
        } else {
            this.pathWidthLower = Math.floor(this.pathsize / 2);
        }
    }

    public final long nextLong() {
        final long a = this.state;
        this.state = xorShift64(a);
        return a;
    }

    public final long xorShift64(long a) {
        a ^= (a << 21);
        a ^= (a >>> 35);
        a ^= (a << 4);
        return a;
    }

    public final int random(final int n) {
        final long result = ((nextLong() >>> 32) * n) >> 32;
        return (int) result;
    }

    public void setCuboidRegion(final int x1, final int x2, final int y1, final int y2, final int z1, final int z2, final PlotBlock block, final World w) {
        if (block.data == 0) {
            return;
        }
        for (int x = x1; x < x2; x++) {
            for (int z = z1; z < z2; z++) {
                for (int y = y1; y < y2; y++) {
                    setBlock(w, x, y, z, block.id, block.data);
                }
            }
        }
    }

    private void setCuboidRegion(final int x1, final int x2, final int y1, final int y2, final int z1, final int z2, final PlotBlock[] blocks, final World w) {
        if (blocks.length == 1) {
            setCuboidRegion(x1, x2, y1, y2, z1, z2, blocks[0], w);
        } else {
            for (int x = x1; x < x2; x++) {
                for (int z = z1; z < z2; z++) {
                    for (int y = y1; y < y2; y++) {
                        final int i = random(blocks.length);
                        if (blocks[i].data != 0) {
                            setBlock(w, x, y, z, blocks[i].id, blocks[i].data);
                        }
                    }
                }
            }
        }

    }

    public short[] getBlock(final String block) {
        if (block.contains(":")) {
            final String[] split = block.split(":");
            return new short[]{Short.parseShort(split[0]), Short.parseShort(split[1])};
        }
        return new short[]{Short.parseShort(block), 0};
    }

    @Override
    public void populate(final World w, final Random r, final Chunk c) {
    }

    @SuppressWarnings("deprecation")
    private void setBlock(final World w, final int x, final int y, final int z, final short id, final byte val) {
        w.getBlockAt(this.X + x, y, this.Z + z).setData(val, false);
    }

}
