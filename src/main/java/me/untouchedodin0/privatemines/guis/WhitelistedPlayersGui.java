package me.untouchedodin0.privatemines.guis;

import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WhitelistedPlayersGui {

    File userFile;
    YamlConfiguration mineConfig;
    List<UUID> whitelistedPlayers = new ArrayList<>();
    List<String> whitelistedPlayersNames = new ArrayList<>();

    private static final String MINE_DIRECTORY = "plugins/PrivateMinesRewrite/mines/";

    public void openWhitelistedPlayersMenu(Player player) {
        userFile = new File(MINE_DIRECTORY + player.getUniqueId() + ".yml");
        mineConfig = YamlConfiguration.loadConfiguration(userFile);

        if (mineConfig.getList("whitelistedPlayers").isEmpty()) {
            player.sendMessage(ChatColor.RED + "There were no whitelisted players!");
        } else {
            for (String id : mineConfig.getStringList("whitelistedPlayers")) {
                whitelistedPlayers.add(UUID.fromString(id));
                whitelistedPlayersNames.add(Bukkit.getPlayer(UUID.fromString(id)).getName());
            }
        }

        for (String str : whitelistedPlayersNames) {
            BaseComponent[] component = new ComponentBuilder("- " )
                    .color(ChatColor.GRAY
                            .asBungee())
                    .append(str)
                    .color(ChatColor.GOLD
                            .asBungee()).append(str).create();

            TextComponent message = new TextComponent("- " + str);
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to un-whitelist " + str)));
            message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/pmine unwhitelist " + str));
            player.spigot().sendMessage(component);
        }

        player.sendMessage(whitelistedPlayers.toString());
    }
}
