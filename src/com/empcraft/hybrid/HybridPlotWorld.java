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
import com.intellectualcrafters.plot.config.Configuration;
import com.intellectualcrafters.plot.config.ConfigurationNode;
import com.intellectualcrafters.plot.object.PlotBlock;
import com.intellectualcrafters.plot.object.PlotWorld;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;

public class HybridPlotWorld extends PlotWorld {

    /*
     * These variables are set to ensure fast access to config settings Strings
     * are used as little as possible to optimize math performance in many of
     * the functions/algorithms
     */

    /**
     * Default Road Height: 64
     */
    public final static int ROAD_HEIGHT_DEFAULT = 64;
    /**
     * Default plot height: 64
     */
    public final static int PLOT_HEIGHT_DEFAULT = 64;
    /**
     * Default Wall Height: 64
     */
    public final static int WALL_HEIGHT_DEFAULT = 64;
    /**
     * Default plot width: 32
     */
    public final static int PLOT_WIDTH_DEFAULT = 32;
    /**
     * Default road width: 7
     */
    public final static int ROAD_WIDTH_DEFAULT = 7;
    /**
     * Default main block: 1
     */
    public final static PlotBlock[] MAIN_BLOCK_DEFAULT = new PlotBlock[]{new PlotBlock((short) 1, (byte) 0)};
    /**
     * Default top blocks: {"2"}
     */
    public final static PlotBlock[] TOP_BLOCK_DEFAULT = new PlotBlock[]{new PlotBlock((short) 2, (byte) 0)};
    /**
     * Default wall block: 44
     */
    public final static PlotBlock WALL_BLOCK_DEFAULT = new PlotBlock((short) 44, (byte) 0);
    public final static PlotBlock CLAIMED_WALL_BLOCK_DEFAULT = new PlotBlock((short) 44, (byte) 1);
    /**
     * Default wall filling: 1
     */
    public final static PlotBlock WALL_FILLING_DEFAULT = new PlotBlock((short) 1, (byte) 0);
    /**
     * Default road stripes: 35
     */
    public final static PlotBlock ROAD_STRIPES_DEFAULT = new PlotBlock((short) 98, (byte) 0);
    public final static boolean ROAD_STRIPES_ENABLED_DEFAULT = false;
    /**
     * Default road block: 155
     */
    public final static PlotBlock ROAD_BLOCK_DEFAULT = new PlotBlock((short) 155, (byte) 0);
    /**
     * Road Height
     */
    public int ROAD_HEIGHT;
    /**
     * plot height
     */
    public int PLOT_HEIGHT;
    /**
     * Wall height
     */
    public int WALL_HEIGHT;
    /**
     * plot width
     */
    public int PLOT_WIDTH;
    /**
     * Road width
     */
    public int ROAD_WIDTH;
    /**
     * Plot main block
     */
    public PlotBlock[] MAIN_BLOCK;
    /**
     * Top blocks
     */
    public PlotBlock[] TOP_BLOCK;
    /**
     * Wall block
     */
    public PlotBlock WALL_BLOCK;
    public PlotBlock CLAIMED_WALL_BLOCK;
    /**
     * Wall filling
     */
    public PlotBlock WALL_FILLING;
    /**
     * Road stripes
     */
    public PlotBlock ROAD_STRIPES;
    /**
     * enable road stripes
     */
    public boolean ROAD_STRIPES_ENABLED;
    /**
     * Road block
     */
    public PlotBlock ROAD_BLOCK;

    /*
     * Here we are just calling the super method, nothing special
     */
    public HybridPlotWorld(final String worldname) {
        super(worldname);
    }

    /**
     * CONFIG NODE | DEFAULT VALUE | DESCRIPTION | CONFIGURATION TYPE | REQUIRED FOR INITIAL SETUP
     * <p/>
     * Set the last boolean to false if you do not require a specific config node to be set while using the setup
     * command - this may be useful if a config value can be changed at a later date, and has no impact on the actual
     * world generation
     */
    @Override
    public ConfigurationNode[] getSettingNodes() {
        // TODO return a set of configuration nodes (used for setup command)
        return new ConfigurationNode[]{new ConfigurationNode("plot.height", HybridPlotWorld.PLOT_HEIGHT_DEFAULT, "Plot height", Configuration.INTEGER, true), new ConfigurationNode("plot.size", HybridPlotWorld.PLOT_WIDTH_DEFAULT, "Plot width", Configuration.INTEGER, true), new ConfigurationNode("plot.filling", HybridPlotWorld.MAIN_BLOCK_DEFAULT, "Plot block", Configuration.BLOCKLIST, true), new ConfigurationNode("plot.floor", HybridPlotWorld.TOP_BLOCK_DEFAULT, "Plot floor block", Configuration.BLOCKLIST, true), new ConfigurationNode("wall.block", HybridPlotWorld.WALL_BLOCK_DEFAULT, "Top wall block", Configuration.BLOCK, true), new ConfigurationNode("wall.block_claimed", HybridPlotWorld.CLAIMED_WALL_BLOCK_DEFAULT, "Wall block (claimed)", Configuration.BLOCK, true), new ConfigurationNode("road.width", HybridPlotWorld.ROAD_WIDTH_DEFAULT, "Road width", Configuration.INTEGER, true), new ConfigurationNode("road.height", HybridPlotWorld.ROAD_HEIGHT_DEFAULT, "Road height", Configuration.INTEGER, true), new ConfigurationNode("road.enable_stripes", HybridPlotWorld.ROAD_STRIPES_ENABLED_DEFAULT, "Enable road stripes", Configuration.BOOLEAN, true), new ConfigurationNode("road.block", HybridPlotWorld.ROAD_BLOCK_DEFAULT, "Road block", Configuration.BLOCK, true), new ConfigurationNode("road.stripes", HybridPlotWorld.ROAD_STRIPES_DEFAULT, "Road stripe block", Configuration.BLOCK, true), new ConfigurationNode("wall.filling", HybridPlotWorld.WALL_FILLING_DEFAULT, "Wall filling block", Configuration.BLOCK, true), new ConfigurationNode("wall.height", HybridPlotWorld.WALL_HEIGHT_DEFAULT, "Wall height", Configuration.INTEGER, true),};
    }

    /**
     * This method is called when a world loads. Make sure you set all your constants here. You are provided with the
     * configuration section for that specific world.
     */
    @Override
    public void loadConfiguration(final ConfigurationSection config) {
        if (!config.contains("plot.height")) {
            PlotMain.sendConsoleSenderMessage(" - &cConfiguration is null? (" + config.getCurrentPath() + ")");
        }
        
        this.PLOT_HEIGHT = config.getInt("plot.height");
        this.PLOT_WIDTH = config.getInt("plot.size");
        this.MAIN_BLOCK = (PlotBlock[]) Configuration.BLOCKLIST.parseString(StringUtils.join(config.getStringList("plot.filling"), ','));
        this.TOP_BLOCK = (PlotBlock[]) Configuration.BLOCKLIST.parseString(StringUtils.join(config.getStringList("plot.floor"), ','));
        this.WALL_BLOCK = (PlotBlock) Configuration.BLOCK.parseString(config.getString("wall.block"));
        this.ROAD_WIDTH = config.getInt("road.width");
        this.ROAD_HEIGHT = config.getInt("road.height");
        this.ROAD_STRIPES_ENABLED = config.getBoolean("road.enable_stripes");
        this.ROAD_BLOCK = (PlotBlock) Configuration.BLOCK.parseString(config.getString("road.block"));
        this.ROAD_STRIPES = (PlotBlock) Configuration.BLOCK.parseString(config.getString("road.stripes"));
        this.WALL_FILLING = (PlotBlock) Configuration.BLOCK.parseString(config.getString("wall.filling"));
        this.WALL_HEIGHT = config.getInt("wall.height");
        this.CLAIMED_WALL_BLOCK = (PlotBlock) Configuration.BLOCK.parseString(config.getString("wall.block_claimed"));
    }
}
