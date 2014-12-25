package com.empcraft.hybrid;

import java.util.HashMap;

public class PlotWrapper {
    public final int minX;
    public final int maxX;
    public final int minZ;
    public final int maxZ;
    
    public HashMap<BlockLoc, HashMap<Integer, Integer>> blocks;
    
    public PlotWrapper(int minX, int maxX, int minZ, int maxZ) {
        this.maxX = maxX;
        this.minX = minX;
        this.maxZ = maxZ;
        this.minZ = minZ;
    }
}
