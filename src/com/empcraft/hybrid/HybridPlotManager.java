////////////////////////////////////////////////////////////////////////////////////////////////////
// PlotSquared - A plot manager and world generator for the Bukkit API                             /
// Copyright (c) 2014 IntellectualSites/IntellectualCrafters                                       /
//                                                                                                 /
// This program is free software; you can redistribute it and/or modify                            /
// it under the terms of the GNU General Public License as published by                            /
// the Free Software Foundation; either version 3 of the License, or                               /
// (at your option) any later version.                                                             /
//                                                                                                 /
// This program is distributed in the hope that it will be useful,                                 /
// but WITHOUT ANY WARRANTY; without even the implied warranty of                                  /
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                   /
// GNU General Public License for more details.                                                    /
//                                                                                                 /
// You should have received a copy of the GNU General Public License                               /
// along with this program; if not, write to the Free Software Foundation,                         /
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA                               /
//                                                                                                 /
// You can contact us via: support@intellectualsites.com                                           /
////////////////////////////////////////////////////////////////////////////////////////////////////

package com.empcraft.hybrid;

import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.object.*;
import com.intellectualcrafters.plot.util.PlayerFunctions;
import com.intellectualcrafters.plot.util.PlotHelper;

import net.minecraft.server.v1_7_R3.ChunkRegionLoader;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("deprecation") public class HybridPlotManager extends PlotManager {

    public PlotWrapper currentPlotClear;
    
    /**
     * Default implementation of getting a plot at a given location For a simplified explanation of the math involved: -
     * Get the current coords - shift these numbers down to something relatable for a single plot (similar to reducing
     * trigonometric functions down to the first quadrant) - e.g. If the plot size is 20 blocks, and we are at x=25,
     * it's equivalent to x=5 for that specific plot From this, and knowing how thick the road is, we can say whether
     * x=5 is road, or plot. The number of shifts done, is also counted, and this number gives us the PlotId
     */
    @Override
    public PlotId getPlotIdAbs(final PlotWorld plotworld, final Location loc) {
        final HybridPlotWorld dpw = ((HybridPlotWorld) plotworld);

        // get x,z loc
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        // get plot size
        final int size = dpw.PLOT_WIDTH + dpw.ROAD_WIDTH;

        // get size of path on bottom part, and top part of plot
        // (As 0,0 is in the middle of a road, not the very start)
        int pathWidthLower;
        if ((dpw.ROAD_WIDTH % 2) == 0) {
            pathWidthLower = (int) (Math.floor(dpw.ROAD_WIDTH / 2) - 1);
        } else {
            pathWidthLower = (int) Math.floor(dpw.ROAD_WIDTH / 2);
        }

        // calulating how many shifts need to be done
        int dx = x / size;
        int dz = z / size;
        if (x < 0) {
            dx--;
            x += ((-dx) * size);
        }
        if (z < 0) {
            dz--;
            z += ((-dz) * size);
        }

        // reducing to first plot
        final int rx = (x) % size;
        final int rz = (z) % size;

        // checking if road (return null if so)
        final int end = pathWidthLower + dpw.PLOT_WIDTH;
        final boolean northSouth = (rz <= pathWidthLower) || (rz > end);
        final boolean eastWest = (rx <= pathWidthLower) || (rx > end);
        if (northSouth || eastWest) {
            return null;
        }
        // returning the plot id (based on the number of shifts required)
        return new PlotId(dx + 1, dz + 1);
    }

    /**
     * Some complex stuff for traversing mega plots (return getPlotIdAbs if you do not support mega plots)
     */
    @Override
    public PlotId getPlotId(final PlotWorld plotworld, final Location loc) {
        final HybridPlotWorld dpw = ((HybridPlotWorld) plotworld);

        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        if (plotworld == null) {
            return null;
        }
        final int size = dpw.PLOT_WIDTH + dpw.ROAD_WIDTH;
        int pathWidthLower;
        if ((dpw.ROAD_WIDTH % 2) == 0) {
            pathWidthLower = (int) (Math.floor(dpw.ROAD_WIDTH / 2) - 1);
        } else {
            pathWidthLower = (int) Math.floor(dpw.ROAD_WIDTH / 2);
        }

        int dx = x / size;
        int dz = z / size;

        if (x < 0) {
            dx--;
            x += ((-dx) * size);
        }
        if (z < 0) {
            dz--;
            z += ((-dz) * size);
        }

        final int rx = (x) % size;
        final int rz = (z) % size;

        final int end = pathWidthLower + dpw.PLOT_WIDTH;

        final boolean northSouth = (rz <= pathWidthLower) || (rz > end);
        final boolean eastWest = (rx <= pathWidthLower) || (rx > end);
        if (northSouth && eastWest) {
            // This means you are in the intersection
            final PlotId id = PlayerFunctions.getPlotAbs(loc.add(dpw.ROAD_WIDTH, 0, dpw.ROAD_WIDTH));
            final Plot plot = PlotMain.getPlots(loc.getWorld()).get(id);
            if (plot == null) {
                return null;
            }
            if ((plot.settings.getMerged(0) && plot.settings.getMerged(3))) {
                return PlayerFunctions.getBottomPlot(loc.getWorld(), plot).id;
            }
            return null;
        }
        if (northSouth) {
            // You are on a road running West to East (yeah, I named the var
            // poorly)
            final PlotId id = PlayerFunctions.getPlotAbs(loc.add(0, 0, dpw.ROAD_WIDTH));
            final Plot plot = PlotMain.getPlots(loc.getWorld()).get(id);
            if (plot == null) {
                return null;
            }
            if (plot.settings.getMerged(0)) {
                return PlayerFunctions.getBottomPlot(loc.getWorld(), plot).id;
            }
            return null;
        }
        if (eastWest) {
            // This is the road separating an Eastern and Western plot
            final PlotId id = PlayerFunctions.getPlotAbs(loc.add(dpw.ROAD_WIDTH, 0, 0));
            final Plot plot = PlotMain.getPlots(loc.getWorld()).get(id);
            if (plot == null) {
                return null;
            }
            if (plot.settings.getMerged(3)) {
                return PlayerFunctions.getBottomPlot(loc.getWorld(), plot).id;
            }
            return null;
        }
        final PlotId id = new PlotId(dx + 1, dz + 1);
        final Plot plot = PlotMain.getPlots(loc.getWorld()).get(id);
        if (plot == null) {
            return id;
        }
        return PlayerFunctions.getBottomPlot(loc.getWorld(), plot).id;
    }

    /**
     * Check if a location is inside a specific plot(non-Javadoc) - For this implementation, we don't need to do
     * anything fancier than referring to getPlotIdAbs(...)
     */
    @Override
    public boolean isInPlotAbs(final PlotWorld plotworld, final Location loc, final PlotId plotid) {
        final PlotId result = getPlotIdAbs(plotworld, loc);
        return (result != null) && (result == plotid);
    }

    /**
     * Get the bottom plot loc (some basic math)
     */
    @Override
    public Location getPlotBottomLocAbs(final PlotWorld plotworld, final PlotId plotid) {
        final HybridPlotWorld dpw = ((HybridPlotWorld) plotworld);

        final int px = plotid.x;
        final int pz = plotid.y;

        final int x = (px * (dpw.ROAD_WIDTH + dpw.PLOT_WIDTH)) - dpw.PLOT_WIDTH - ((int) Math.floor(dpw.ROAD_WIDTH / 2)) - 1;
        final int z = (pz * (dpw.ROAD_WIDTH + dpw.PLOT_WIDTH)) - dpw.PLOT_WIDTH - ((int) Math.floor(dpw.ROAD_WIDTH / 2)) - 1;

        return new Location(Bukkit.getWorld(plotworld.worldname), x, 1, z);
    }

    /**
     * Get the top plot loc (some basic math)
     */
    @Override
    public Location getPlotTopLocAbs(final PlotWorld plotworld, final PlotId plotid) {
        final HybridPlotWorld dpw = ((HybridPlotWorld) plotworld);

        final int px = plotid.x;
        final int pz = plotid.y;

        final int x = (px * (dpw.ROAD_WIDTH + dpw.PLOT_WIDTH)) - ((int) Math.floor(dpw.ROAD_WIDTH / 2)) - 1;
        final int z = (pz * (dpw.ROAD_WIDTH + dpw.PLOT_WIDTH)) - ((int) Math.floor(dpw.ROAD_WIDTH / 2)) - 1;

        return new Location(Bukkit.getWorld(plotworld.worldname), x, 256, z);
    }

    /**
     * Clearing the plot needs to only consider removing the blocks - This implementation has used the SetCuboid
     * function, as it is fast, and uses NMS code - It also makes use of the fact that deleting chunks is a lot faster
     * than block updates This code is very messy, but you don't need to do something quite as complex unless you happen
     * to have 512x512 sized plots
     */
    @Override
    public boolean clearPlot(final World world, final Plot plot, final boolean isDelete) {
        final Location pos1 = PlotHelper.getPlotBottomLoc(world, plot.id).add(1, 0, 1);
        final Location pos2 = PlotHelper.getPlotTopLoc(world, plot.id);
        
        Chunk c1 = world.getChunkAt(pos1);
        Chunk c2 = world.getChunkAt(pos2);

        this.currentPlotClear = new PlotWrapper(pos1.getBlockX(), pos2.getBlockX(), pos1.getBlockZ(), pos2.getBlockZ());
        
        int sx = pos1.getBlockX();
        int sz = pos1.getBlockZ();
        int ex = pos2.getBlockX();
        int ez = pos2.getBlockZ();
        
        int c1x = c1.getX();
        int c1z = c1.getZ();
        int c2x = c2.getX();
        int c2z = c2.getZ();
        
        int maxY = world.getMaxHeight();
        
        for (int x = c1x; x <= c2x; x ++) {
            for (int z = c1z; z <= c2z; z ++) {
                
                Chunk chunk = world.getChunkAt(x, z);
                
                boolean loaded = true;
                
                if (!chunk.isLoaded()) {
                    boolean result = chunk.load(false);
                    if (!result) {
                        loaded = false;;
                    }
                    if (!chunk.isLoaded()) {
                        loaded = false;
                    }
                }
                
                if (loaded) {
                    int absX = x << 4;
                    int absZ = z << 4;
                    
                    this.currentPlotClear.blocks = new HashMap<>();
                    
                    if (x == c1x || z == c1z) {
                        for (int X = 0; X < 16; X++) {
                            for (int Z = 0; Z < 16; Z++) {
                                if ((X + absX < sx || Z + absZ < sz) || (X + absX > ex || Z + absZ > ez)) {
                                    HashMap<Integer, Integer> ids = new HashMap<>();
                                    for (int y = 1; y < maxY; y++) {
                                        int id = world.getBlockTypeIdAt(X + absX, y, Z + absZ);
                                        if (id != 0) {
                                            ids.put(y, id);
                                        }
                                    }
                                    BlockLoc loc = new BlockLoc(X + absX, Z + absZ);
                                    this.currentPlotClear.blocks.put(loc, ids);
                                }
                            }
                        }
                    }
                    else if (x == c2x || z == c2z) {
                        for (int X = 0; X < 16; X++) {
                            for (int Z = 0; Z < 16; Z++) {
                                if ((X + absX > ex || Z + absZ > ez) || (X + absX < sx || Z + absZ < sz)) {
                                    HashMap<Integer, Integer> ids = new HashMap<>();
                                    for (int y = 1; y < maxY; y++) {
                                        int id = world.getBlockTypeIdAt(X + absX, y, Z + absZ);
                                        if (id != 0) {
                                            ids.put(y, id);
                                        }
                                    }
                                    BlockLoc loc = new BlockLoc(X + absX, Z + absZ);
                                    this.currentPlotClear.blocks.put(loc, ids);
                                }
                            }
                        }
                    }
                    world.regenerateChunk(x, z);
                }
            }
        }
        
        this.currentPlotClear = null;
        
        return true;
    }

    /**
     * Remove sign for a plot
     */
    @Override
    public Location getSignLoc(final World world, final PlotWorld plotworld, final Plot plot) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        return new Location(world, PlotHelper.getPlotBottomLoc(world, plot.id).getBlockX(), dpw.ROAD_HEIGHT + 1, PlotHelper.getPlotBottomLoc(world, plot.id).getBlockZ() - 1);
    }

    @Override
    public boolean setFloor(final World world, final PlotWorld plotworld, final PlotId plotid, final PlotBlock[] blocks) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        final Location pos1 = PlotHelper.getPlotBottomLoc(world, plotid).add(1, 0, 1);
        final Location pos2 = PlotHelper.getPlotTopLoc(world, plotid);
        PlotHelper.setCuboid(world, new Location(world, pos1.getX(), dpw.PLOT_HEIGHT, pos1.getZ()), new Location(world, pos2.getX() + 1, dpw.PLOT_HEIGHT + 1, pos2.getZ() + 1), blocks);
        return true;
    }

    @Override
    public boolean setWallFilling(final World w, final PlotWorld plotworld, final PlotId plotid, final PlotBlock plotblock) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        if (dpw.ROAD_WIDTH == 0) {
            return false;
        }
        final Location bottom = PlotHelper.getPlotBottomLoc(w, plotid);
        final Location top = PlotHelper.getPlotTopLoc(w, plotid);

        int x, z;

        Block block;

        z = bottom.getBlockZ();
        for (x = bottom.getBlockX(); x < (top.getBlockX() + 1); x++) {
            for (int y = 1; y <= dpw.WALL_HEIGHT; y++) {
                block = w.getBlockAt(x, y, z);
                PlotHelper.setBlock(block, plotblock);
            }
        }

        x = top.getBlockX() + 1;
        for (z = bottom.getBlockZ(); z < (top.getBlockZ() + 1); z++) {
            for (int y = 1; y <= dpw.WALL_HEIGHT; y++) {
                block = w.getBlockAt(x, y, z);
                PlotHelper.setBlock(block, plotblock);
            }
        }

        z = top.getBlockZ() + 1;
        for (x = top.getBlockX() + 1; x > (bottom.getBlockX() - 1); x--) {
            for (int y = 1; y <= dpw.WALL_HEIGHT; y++) {
                block = w.getBlockAt(x, y, z);
                PlotHelper.setBlock(block, plotblock);
            }
        }
        x = bottom.getBlockX();
        for (z = top.getBlockZ() + 1; z > (bottom.getBlockZ() - 1); z--) {
            for (int y = 1; y <= dpw.WALL_HEIGHT; y++) {
                block = w.getBlockAt(x, y, z);
                PlotHelper.setBlock(block, plotblock);
            }
        }
        return true;
    }

    @Override
    public boolean setWall(final World w, final PlotWorld plotworld, final PlotId plotid, final PlotBlock plotblock) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        if (dpw.ROAD_WIDTH == 0) {
            return false;
        }
        final Location bottom = PlotHelper.getPlotBottomLoc(w, plotid);
        final Location top = PlotHelper.getPlotTopLoc(w, plotid);

        int x, z;

        Block block;
        z = bottom.getBlockZ();
        for (x = bottom.getBlockX(); x < (top.getBlockX() + 1); x++) {
            block = w.getBlockAt(x, dpw.WALL_HEIGHT + 1, z);
            PlotHelper.setBlock(block, plotblock);
        }
        x = top.getBlockX() + 1;
        for (z = bottom.getBlockZ(); z < (top.getBlockZ() + 1); z++) {
            block = w.getBlockAt(x, dpw.WALL_HEIGHT + 1, z);
            PlotHelper.setBlock(block, plotblock);
        }
        z = top.getBlockZ() + 1;
        for (x = top.getBlockX() + 1; x > (bottom.getBlockX() - 1); x--) {
            block = w.getBlockAt(x, dpw.WALL_HEIGHT + 1, z);
            PlotHelper.setBlock(block, plotblock);
        }
        x = bottom.getBlockX();
        for (z = top.getBlockZ() + 1; z > (bottom.getBlockZ() - 1); z--) {
            block = w.getBlockAt(x, dpw.WALL_HEIGHT + 1, z);
            PlotHelper.setBlock(block, plotblock);
        }
        return true;
    }

    /**
     * Set a plot biome
     */
    @Override
    public boolean setBiome(final World world, final Plot plot, final Biome biome) {

        final int bottomX = PlotHelper.getPlotBottomLoc(world, plot.id).getBlockX() - 1;
        final int topX = PlotHelper.getPlotTopLoc(world, plot.id).getBlockX() + 1;
        final int bottomZ = PlotHelper.getPlotBottomLoc(world, plot.id).getBlockZ() - 1;
        final int topZ = PlotHelper.getPlotTopLoc(world, plot.id).getBlockZ() + 1;

        final Block block = world.getBlockAt(PlotHelper.getPlotBottomLoc(world, plot.id).add(1, 1, 1));
        final Biome current = block.getBiome();
        if (biome.equals(current)) {
            return false;
        }

        for (int x = bottomX; x <= topX; x++) {
            for (int z = bottomZ; z <= topZ; z++) {
                final Block blk = world.getBlockAt(x, 0, z);
                final Biome c = blk.getBiome();
                if (c.equals(biome)) {
                    x += 15;
                    continue;
                }
                blk.setBiome(biome);
            }
        }
        return true;
    }

    /**
     * PLOT MERGING
     */

    @Override
    public boolean createRoadEast(final PlotWorld plotworld, final Plot plot) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        final World w = Bukkit.getWorld(plot.world);

        final Location pos1 = getPlotBottomLocAbs(plotworld, plot.id);
        final Location pos2 = getPlotTopLocAbs(plotworld, plot.id);

        final int sx = pos2.getBlockX() + 1;
        final int ex = (sx + dpw.ROAD_WIDTH) - 1;
        final int sz = pos1.getBlockZ() - 1;
        final int ez = pos2.getBlockZ() + 2;

        PlotHelper.setSimpleCuboid(w, new Location(w, sx, Math.min(dpw.WALL_HEIGHT, dpw.ROAD_HEIGHT) + 1, sz + 1), new Location(w, ex + 1, 257 + 1, ez), new PlotBlock((short) 0, (byte) 0));

        PlotHelper.setCuboid(w, new Location(w, sx, 1, sz + 1), new Location(w, ex + 1, dpw.PLOT_HEIGHT, ez), new PlotBlock((short) 7, (byte) 0));

        PlotHelper.setCuboid(w, new Location(w, sx, 1, sz + 1), new Location(w, sx + 1, dpw.WALL_HEIGHT + 1, ez), dpw.WALL_FILLING);
        PlotHelper.setCuboid(w, new Location(w, sx, dpw.WALL_HEIGHT + 1, sz + 1), new Location(w, sx + 1, dpw.WALL_HEIGHT + 2, ez), dpw.WALL_BLOCK);

        PlotHelper.setCuboid(w, new Location(w, ex, 1, sz + 1), new Location(w, ex + 1, dpw.WALL_HEIGHT + 1, ez), dpw.WALL_FILLING);
        PlotHelper.setCuboid(w, new Location(w, ex, dpw.WALL_HEIGHT + 1, sz + 1), new Location(w, ex + 1, dpw.WALL_HEIGHT + 2, ez), dpw.WALL_BLOCK);

        PlotHelper.setCuboid(w, new Location(w, sx + 1, 1, sz + 1), new Location(w, ex, dpw.ROAD_HEIGHT + 1, ez), dpw.ROAD_BLOCK);

        return true;
    }

    @Override
    public boolean createRoadSouth(final PlotWorld plotworld, final Plot plot) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        final World w = Bukkit.getWorld(plot.world);

        final Location pos1 = getPlotBottomLocAbs(plotworld, plot.id);
        final Location pos2 = getPlotTopLocAbs(plotworld, plot.id);

        final int sz = pos2.getBlockZ() + 1;
        final int ez = (sz + dpw.ROAD_WIDTH) - 1;
        final int sx = pos1.getBlockX() - 1;
        final int ex = pos2.getBlockX() + 2;

        PlotHelper.setSimpleCuboid(w, new Location(w, sx, Math.min(dpw.WALL_HEIGHT, dpw.ROAD_HEIGHT) + 1, sz + 1), new Location(w, ex + 1, 257, ez), new PlotBlock((short) 0, (byte) 0));

        PlotHelper.setCuboid(w, new Location(w, sx + 1, 0, sz), new Location(w, ex, 1, ez + 1), new PlotBlock((short) 7, (byte) 0));

        PlotHelper.setCuboid(w, new Location(w, sx + 1, 1, sz), new Location(w, ex, dpw.WALL_HEIGHT + 1, sz + 1), dpw.WALL_FILLING);
        PlotHelper.setCuboid(w, new Location(w, sx + 1, dpw.WALL_HEIGHT + 1, sz), new Location(w, ex, dpw.WALL_HEIGHT + 2, sz + 1), dpw.WALL_BLOCK);

        PlotHelper.setCuboid(w, new Location(w, sx + 1, 1, ez), new Location(w, ex, dpw.WALL_HEIGHT + 1, ez + 1), dpw.WALL_FILLING);
        PlotHelper.setCuboid(w, new Location(w, sx + 1, dpw.WALL_HEIGHT + 1, ez), new Location(w, ex, dpw.WALL_HEIGHT + 2, ez + 1), dpw.WALL_BLOCK);

        PlotHelper.setCuboid(w, new Location(w, sx + 1, 1, sz + 1), new Location(w, ex, dpw.ROAD_HEIGHT + 1, ez), dpw.ROAD_BLOCK);

        return true;
    }

    @Override
    public boolean createRoadSouthEast(final PlotWorld plotworld, final Plot plot) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        final World w = Bukkit.getWorld(plot.world);

        final Location pos2 = getPlotTopLocAbs(plotworld, plot.id);

        final int sx = pos2.getBlockX() + 1;
        final int ex = (sx + dpw.ROAD_WIDTH) - 1;
        final int sz = pos2.getBlockZ() + 1;
        final int ez = (sz + dpw.ROAD_WIDTH) - 1;

        PlotHelper.setSimpleCuboid(w, new Location(w, sx, dpw.ROAD_HEIGHT + 1, sz + 1), new Location(w, ex + 1, 257, ez), new PlotBlock((short) 0, (byte) 0));
        PlotHelper.setCuboid(w, new Location(w, sx + 1, 0, sz + 1), new Location(w, ex, 1, ez), new PlotBlock((short) 7, (byte) 0));
        PlotHelper.setCuboid(w, new Location(w, sx + 1, 1, sz + 1), new Location(w, ex, dpw.ROAD_HEIGHT + 1, ez), dpw.ROAD_BLOCK);

        return true;
    }

    @Override
    public boolean removeRoadEast(final PlotWorld plotworld, final Plot plot) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        final World w = Bukkit.getWorld(plot.world);

        final Location pos1 = getPlotBottomLocAbs(plotworld, plot.id);
        final Location pos2 = getPlotTopLocAbs(plotworld, plot.id);

        final int sx = pos2.getBlockX() + 1;
        final int ex = (sx + dpw.ROAD_WIDTH) - 1;
        final int sz = pos1.getBlockZ();
        final int ez = pos2.getBlockZ() + 1;

        PlotHelper.setSimpleCuboid(w, new Location(w, sx, Math.min(dpw.PLOT_HEIGHT, dpw.ROAD_HEIGHT) + 1, sz), new Location(w, ex + 1, 257, ez + 1), new PlotBlock((short) 0, (byte) 0));

        PlotHelper.setCuboid(w, new Location(w, sx, 1, sz), new Location(w, ex + 1, dpw.PLOT_HEIGHT, ez + 1), dpw.MAIN_BLOCK);
        PlotHelper.setCuboid(w, new Location(w, sx, dpw.PLOT_HEIGHT, sz), new Location(w, ex + 1, dpw.PLOT_HEIGHT + 1, ez + 1), dpw.TOP_BLOCK);

        return true;
    }

    @Override
    public boolean removeRoadSouth(final PlotWorld plotworld, final Plot plot) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        final World w = Bukkit.getWorld(plot.world);

        final Location pos1 = getPlotBottomLocAbs(plotworld, plot.id);
        final Location pos2 = getPlotTopLocAbs(plotworld, plot.id);

        final int sz = pos2.getBlockZ() + 1;
        final int ez = (sz + dpw.ROAD_WIDTH) - 1;
        final int sx = pos1.getBlockX();
        final int ex = pos2.getBlockX() + 1;

        PlotHelper.setSimpleCuboid(w, new Location(w, sx, Math.min(dpw.PLOT_HEIGHT, dpw.ROAD_HEIGHT) + 1, sz), new Location(w, ex + 1, 257, ez + 1), new PlotBlock((short) 0, (byte) 0));

        PlotHelper.setCuboid(w, new Location(w, sx, 1, sz), new Location(w, ex + 1, dpw.PLOT_HEIGHT, ez + 1), dpw.MAIN_BLOCK);
        PlotHelper.setCuboid(w, new Location(w, sx, dpw.PLOT_HEIGHT, sz), new Location(w, ex + 1, dpw.PLOT_HEIGHT + 1, ez + 1), dpw.TOP_BLOCK);

        return true;
    }

    @Override
    public boolean removeRoadSouthEast(final PlotWorld plotworld, final Plot plot) {
        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;
        final World world = Bukkit.getWorld(plot.world);

        final Location loc = getPlotTopLocAbs(dpw, plot.id);

        final int sx = loc.getBlockX() + 1;
        final int ex = (sx + dpw.ROAD_WIDTH) - 1;
        final int sz = loc.getBlockZ() + 1;
        final int ez = (sz + dpw.ROAD_WIDTH) - 1;

        PlotHelper.setSimpleCuboid(world, new Location(world, sx, dpw.ROAD_HEIGHT + 1, sz), new Location(world, ex + 1, 257, ez + 1), new PlotBlock((short) 0, (byte) 0));

        PlotHelper.setCuboid(world, new Location(world, sx + 1, 1, sz + 1), new Location(world, ex, dpw.ROAD_HEIGHT, ez), dpw.MAIN_BLOCK);
        PlotHelper.setCuboid(world, new Location(world, sx + 1, dpw.ROAD_HEIGHT, sz + 1), new Location(world, ex, dpw.ROAD_HEIGHT + 1, ez), dpw.TOP_BLOCK);
        return true;
    }

    /**
     * Finishing off plot merging by adding in the walls surrounding the plot (OPTIONAL)(UNFINISHED)
     */
    @Override
    public boolean finishPlotMerge(final World world, final PlotWorld plotworld, final ArrayList<PlotId> plotIds) {

        // TODO set plot wall

        final HybridPlotWorld dpw = (HybridPlotWorld) plotworld;

        final PlotId pos1 = plotIds.get(0);
        final PlotId pos2 = plotIds.get(plotIds.size() - 1);

        final PlotBlock block = dpw.WALL_BLOCK;

        final Location megaPlotBot = PlotHelper.getPlotBottomLoc(world, pos1);
        final Location megaPlotTop = PlotHelper.getPlotTopLoc(world, pos2).add(1, 0, 1);
        for (int x = megaPlotBot.getBlockX(); x <= megaPlotTop.getBlockX(); x++) {
            for (int z = megaPlotBot.getBlockZ(); z <= megaPlotTop.getBlockZ(); z++) {
                if ((z == megaPlotBot.getBlockZ()) || (z == megaPlotTop.getBlockZ()) || (x == megaPlotBot.getBlockX()) || (x == megaPlotTop.getBlockX())) {
                    world.getBlockAt(x, dpw.WALL_HEIGHT + 1, z).setTypeIdAndData(block.id, block.data, false);
                }
            }
        }
        return true;
    }

    @Override
    public boolean finishPlotUnlink(final World world, final PlotWorld plotworld, final ArrayList<PlotId> plotIds) {
        return true;
    }

    @Override
    public boolean startPlotMerge(final World world, final PlotWorld plotworld, final ArrayList<PlotId> plotIds) {
        return true;
    }

    @Override
    public boolean startPlotUnlink(final World world, final PlotWorld plotworld, final ArrayList<PlotId> plotIds) {
        return true;
    }
}
