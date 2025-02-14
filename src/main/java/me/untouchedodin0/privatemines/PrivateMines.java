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

package me.untouchedodin0.privatemines;

import me.byteful.lib.blockedit.BlockEditAPI;
import me.byteful.lib.blockedit.BlockEditOption;
import me.untouchedodin0.privatemines.commands.PrivateMinesCmd;
import me.untouchedodin0.privatemines.factory.MineFactory;
import me.untouchedodin0.privatemines.listener.FallInVoidListener;
import me.untouchedodin0.privatemines.structure.StructureLoader;
import me.untouchedodin0.privatemines.utils.Metrics;
import me.untouchedodin0.privatemines.utils.Util;
import me.untouchedodin0.privatemines.utils.filling.MineFillManager;
import me.untouchedodin0.privatemines.utils.mine.MineType;
import me.untouchedodin0.privatemines.utils.mine.loop.MineLoopUtil;
import me.untouchedodin0.privatemines.utils.mine.util.PrivateMineResetUtil;
import me.untouchedodin0.privatemines.utils.placeholderapi.PrivateMinesExpansion;
import me.untouchedodin0.privatemines.utils.storage.MineStorage;
import me.untouchedodin0.privatemines.world.MineWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.commandmanager.CommandParser;
import redempt.redlib.commandmanager.Messages;
import redempt.redlib.configmanager.ConfigManager;
import redempt.redlib.configmanager.annotations.ConfigValue;
import redempt.redlib.multiblock.MultiBlockStructure;

import java.io.File;
import java.util.*;

/*
    Thanks to Redempt for creating RedLib what helped me with a lot of the
    creation of this plugin

    RedLib: https://github.com/Redempt/RedLib
    Redempt: https://github.com/Redempt
 */

public class PrivateMines extends JavaPlugin {

    @ConfigValue
    private static String mineFillSpeed = "BUKKIT";

    int minesCount;

    File structureFolder = new File("plugins/PrivateMinesRewrite/structures/");
    File minesFolder = new File("plugins/PrivateMinesRewrite/mines/");
    File configFile;
    MultiBlockStructure multiBlockStructure;

    List<MineType> mineTypes = new ArrayList<>();

    List<MultiBlockStructure> multiBlockStructures = new ArrayList<>();

    @ConfigValue
    Map<String, MineType> mineTypeMap = ConfigManager.map(MineType.class);

    private PrivateMines privateMine;
    private MineWorldManager mineManager;
    private StructureLoader structureLoader;
    private ConfigManager configManager;

    @ConfigValue
    private int resetDelay = 5;

    @ConfigValue
    private Map<Material, Double> materials = ConfigManager.map(Material.class, Double.class);

    public static String fileNameWithOutExt(String fileName) {
        return Optional.of(fileName.lastIndexOf(".")).filter(i -> i >= 0)
                .map(i -> fileName.substring(0, i)).orElse(fileName);
    }

    @Override
    public void onEnable() {
        Bukkit.getLogger().info("Loading PrivateMinesRewrite...");
        materials.put(Material.STONE, 1.0);
        Util util = new Util();
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        configManager = new ConfigManager(this)
                .addConverter(MultiBlockStructure.class, util::loadStructure, MultiBlockStructure::getName)
                .addConverter(UUID.class, UUID::fromString, UUID::toString)
                .register(this).load();

        saveResource("messages.txt", false);

        Bukkit.getLogger().info("Setting up the Private Mines World...");
        mineManager = new MineWorldManager();
        Bukkit.getLogger().info("Private Mines World has been setup!");
        MineFillManager fillManager = new MineFillManager(this);

        Bukkit.getLogger().info("Setting up the Private Mines Storage and Factory...");
        MineStorage mineStorage = new MineStorage();
        PrivateMineResetUtil privateMineResetUtil = new PrivateMineResetUtil(this);
        MineLoopUtil mineLoopUtil = new MineLoopUtil();
        structureLoader = new StructureLoader(mineLoopUtil);
        MineFactory mineFactory = new MineFactory(
                this,
                mineStorage,
                fillManager,
                privateMineResetUtil,
                mineLoopUtil,
                util,
                structureLoader);

        createMinesFolder();
        createStructureFolder();
        privateMine = this;

        // This populates the mineTypeMap
        loadStructureList(util, structureFolder.listFiles());

        /*
        mineTypeMap.forEach((s, mineType) -> {
            Bukkit.getLogger().info(" Mine Type " + mineType.getMineTypeName()
                    + " is at order " + mineType.getMineOrder());
        });
        mineLevels.addAll(mineTypeMap.keySet());
        Bukkit.getLogger().info("mine levels: " + mineLevels);
        Collections.sort(mineLevels);
        Bukkit.getLogger().info("mine levels sorted: " + mineLevels);
        mineLevelsSorted = mineLevels;
         */

        Bukkit.getLogger().info("Loading mines...");
        if (!minesFolder.exists()) {
            createMinesFolder();
        } else {
            minesCount = Objects.requireNonNull(minesFolder.list()).length;
        }

        String minesCountString = String.valueOf(minesCount);
        String mineCountLoggerMessage = String.format("Found a total of %s mines!", minesCountString);
        Bukkit.getLogger().info(mineCountLoggerMessage);
        Bukkit.getLogger().info("Registering the command...");

        new CommandParser(this.getResource("command.rdcml")).parse().register("privatemines",
                new PrivateMinesCmd(mineStorage, mineFactory));

        Bukkit.getLogger().info("Command registered!");

        Bukkit.getLogger().info("Setting up the private mine util...");

        resetDelay = getConfig().getInt("resetDelay");

        Bukkit.getLogger().info("Starting the private mine reset task...");

        if (!mineStorage.getMineFolder().exists()) {
            boolean success = mineStorage.getMineFolder().mkdir();
            if (success) {
                Bukkit.getLogger().info("Mines folder was successfully created!");
            }
        }
        String mineSpeedMessage = String.format("Setting the mine fill speed to %s", mineFillSpeed);
        Bukkit.getLogger().info(mineSpeedMessage);
        setMineFillSpeed(mineFillSpeed);

        if (mineStorage.getMineFiles() == null) {
            Bukkit.getLogger().info("No mine files to load!");
        } else {
            for (File file : mineStorage.getMineFiles()) {
                UUID uuid = UUID.fromString(fileNameWithOutExt(file.getName()));
                if (getConfig().getBoolean("autoResets")) {
                    Bukkit.getLogger().info("Private Mines will auto-reset!");
                    privateMineResetUtil.startResetTask(uuid, resetDelay);
                }
            }
        }

        Bukkit.getLogger().info("Loading messages...");
        Messages.load(this);
        Bukkit.getLogger().info("Loaded messages!");

        // Metrics ID.
        int pluginId = 11413;
        Metrics metrics = new Metrics(this, pluginId);
        Bukkit.getLogger().info("Loaded metrics!");

        // Register listeners
        getServer().getPluginManager().registerEvents(new FallInVoidListener(this, mineStorage), this);
        registerPlaceholderAPI(mineStorage);
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("Disabling PrivateMinesRewrite v" + getDescription().getVersion());
    }

    public PrivateMines getPrivateMines() {
        return privateMine;
    }

    public String getStructureFileName() {
        return getConfig().getString("structureFile");
    }

    public MineWorldManager getMineWorldManagerManager() {
        return mineManager;
    }

    public StructureLoader getStructureLoader() {
        return structureLoader;
    }

    public List<MineType> getMineTypes() {
        return mineTypes;
    }

    public Map<String, MineType> getMineTypeMap() {
        return mineTypeMap;
    }

    public void loadStructureList(Util util, File[] structuresList) {
        Bukkit.getLogger().info("Loading structure list from method");
        if (structuresList != null) {
            for (File file : structuresList) {
                String name = file.getName().replace(".dat", "");
                multiBlockStructure = util.loadStructure(file.getName(), file);
                MineType mineType = new MineType(
                        name,
                        multiBlockStructure,
                        this);
                mineTypeMap.putIfAbsent(name, mineType);
                multiBlockStructures.add(multiBlockStructure);
            }
        }
    }

    public void createMinesFolder() {
        if (!minesFolder.exists()) {
            boolean minesFolderSuccessfully = minesFolder.mkdir();
            if (minesFolderSuccessfully) {
                Bukkit.getLogger().info("The mines directory was successfully created!");
            }
        }
    }

    public void createStructureFolder() {
        if (!structureFolder.exists()) {
            boolean createdStructureFolderSuccessfully = structureFolder.mkdir();
            if (createdStructureFolderSuccessfully) {
                Bukkit.getLogger().info("The structures directory was successfully created!");
            }
        }
    }

    public void registerPlaceholderAPI(MineStorage storage) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getLogger().info("Found PlaceholderAPI registering expansion!");
            new PrivateMinesExpansion(storage).register();
            Bukkit.getLogger().info("Registered the expansion!");
        } else {
            Bukkit.getLogger().info("Couldn't find PlaceholderAPI so couldn't register placeholders!");
        }
    }

    public Map<Material, Double> getBlocks() {
        return materials;
    }

    public void setMineFillSpeed(String speed) {
        if (speed.equalsIgnoreCase("NMS_SAFE")) {
            BlockEditAPI.initialize(BlockEditOption.NMS_SAFE);
        } else if (speed.equalsIgnoreCase("NMS_FAST")) {
            BlockEditAPI.initialize(BlockEditOption.NMS_FAST);
        } else if (speed.equalsIgnoreCase("NMS_UNSAFE")) {
            BlockEditAPI.initialize(BlockEditOption.NMS_UNSAFE);
        } else if (speed.equalsIgnoreCase("BUKKIT")) {
            BlockEditAPI.initialize(BlockEditOption.BUKKIT);
        } else {
            Bukkit.getLogger().info("Couldn't find the specified the specified mine fill speed.");
        }
    }

    @SuppressWarnings("unused")
    public ConfigManager getConfigManager() {
        return configManager;
    }
}