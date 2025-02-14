/*
 * MIT License
 *
 * Copyright (c) 2021 Kyle Hicks
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.untouchedodin0.privatemines.utils.mine;

import me.byteful.lib.blockedit.BlockEditAPI;
import me.untouchedodin0.privatemines.PrivateMines;
import me.untouchedodin0.privatemines.utils.mine.loop.MineLoopUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import redempt.redlib.RedLib;
import redempt.redlib.configmanager.ConfigManager;
import redempt.redlib.configmanager.annotations.ConfigMappable;
import redempt.redlib.configmanager.annotations.ConfigPath;
import redempt.redlib.configmanager.annotations.ConfigValue;
import redempt.redlib.misc.WeightedRandom;
import redempt.redlib.multiblock.MultiBlockStructure;
import redempt.redlib.multiblock.Structure;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@ConfigMappable
public class MineType {

    @ConfigPath
    private String mineTypeName;

    private final PrivateMines privateMines;
    private final MineLoopUtil mineLoopUtil;
    private final WeightedRandom<Material> weightedRandom;
    private final int[][] cornerLocations;
    private final int[] spawnLocation;
    private final int[] npcLocation;

    private static final Material wool = Material.valueOf(RedLib.MID_VERSION >= 13 ? "WHITE_WOOL" : "WOOL");
    private Structure structure;
    private int structureInt;
    private final Callback callback;

    @ConfigValue
    private MultiBlockStructure multiBlockStructure;

    @ConfigValue
    private Map<Material, Double> materials = ConfigManager.map(Material.class, Double.class);

    public MineType(String name,
                    MultiBlockStructure multiStructure,
                    PrivateMines privateMines) {
        this.mineTypeName = name;
        this.multiBlockStructure = multiStructure;
        this.mineLoopUtil = new MineLoopUtil();
        this.privateMines = privateMines;
        this.cornerLocations = mineLoopUtil.findCornerLocations(multiBlockStructure, Material.POWERED_RAIL);
        this.spawnLocation = mineLoopUtil.findLocation(multiBlockStructure, Material.CHEST);
        this.npcLocation = mineLoopUtil.findLocation(multiBlockStructure, wool);
        this.weightedRandom = new WeightedRandom<>();
        this.callback = mine -> null;
    }

    public MineType getMineType() {
        return this;
    }

    public String getStructureName() {
        return multiBlockStructure.getName();
    }

    public interface Callback {
        Consumer<Structure> mineBuilt(Mine mine);
    }

    // i don't think it's needed here is it?

    public Mine build(Location location, UUID owner) {
        long startTime = System.currentTimeMillis();

        multiBlockStructure.forEachBlock(location, blockState ->
                BlockEditAPI.setBlock(
                        location,
                        blockState.getType(),
                        blockState.getData(),
                        false));

        this.structure = multiBlockStructure.build(location);

        getPrivateMines().getBlocks().forEach(weightedRandom::set);

        Mine mine = new Mine(structure, this);

        mine.setMineLocation(location);
        mine.setMineOwner(owner);
        mine.setSpawnLocation(mine.getRelative(spawnLocation));
        mine.setNpcLocation(mine.getRelative(npcLocation));
        mine.setWeightedRandomMaterials(weightedRandom);
        mine.createNPC(Bukkit.getPlayer(owner), owner.toString());
        mine.teleportToMine(Bukkit.getPlayer(owner));

        multiBlockStructure.getAt(mine.getRelative(mine.getSpawnLocationRelative()));
        long endTime = System.currentTimeMillis();
        String timeString = String.valueOf(endTime - startTime);
        String loadingTime = "That took {} milliseconds to create the mine".replace("{}", timeString);
        Bukkit.getLogger().info(loadingTime);
        return mine;
    }

    public Mine buildAsync(Location location, UUID owner) {
        long startTime = System.currentTimeMillis();

        multiBlockStructure.forEachBlock(location, blockState ->
                BlockEditAPI.setBlock(
                        location,
                        blockState.getType(),
                        blockState.getData(),
                        false));

        Consumer<MultiBlockStructure> multiBlockStructureConsumer = (x) -> {
            multiBlockStructure.buildAsync(location,
                    5,
                    (Consumer<Structure>) x);
            {
                x.getName();
                this.structure = multiBlockStructure.assumeAt(location);
            }
        };
        multiBlockStructureConsumer.accept(multiBlockStructure);

        Mine mine = new Mine(structure, this);

        this.structureInt =
                multiBlockStructure.buildAsync(location, 5, callback.mineBuilt(mine)); //build(location);

        getPrivateMines().getBlocks().forEach(weightedRandom::set);

        mine.setMineLocation(location);
        mine.setMineOwner(owner);
        mine.setSpawnLocation(mine.getRelative(spawnLocation));
        mine.setNpcLocation(mine.getRelative(npcLocation));
        mine.setWeightedRandomMaterials(weightedRandom);
        mine.createNPC(Bukkit.getPlayer(owner), owner.toString());
        mine.teleportToMine(Bukkit.getPlayer(owner));

        multiBlockStructure.getAt(mine.getRelative(mine.getSpawnLocationRelative()));
        long endTime = System.currentTimeMillis();
        String timeString = String.valueOf(endTime - startTime);
        String loadingTime = "That took {} milliseconds to create the mine".replace("{}", timeString);
        Bukkit.getLogger().info(loadingTime);
        return mine;
    }

    public String getMineTypeName() {
        return mineTypeName;
    }

    public PrivateMines getPrivateMines() {
        return privateMines;
    }

    public MineLoopUtil getMineLoopUtil() {
        return mineLoopUtil;
    }

    public Structure getStructure() {
        return this.structure;
    }

    public MultiBlockStructure getMultiBlockStructure() {
        return this.multiBlockStructure;
    }

    public int[][] getCornerLocations() {
        return cornerLocations;
    }

    public int[] getNpcLocation() {
        return npcLocation;
    }

    public int[] getSpawnLocation() {
        return spawnLocation;
    }
}