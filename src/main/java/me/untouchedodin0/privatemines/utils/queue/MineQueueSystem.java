package me.untouchedodin0.privatemines.utils.queue;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class MineQueueSystem {

    final Queue<UUID> mine_queue = new LinkedList<>();

    public void queueMineCreation(Player player) {
        mine_queue.add(player.getUniqueId());
    }

    public void removeFromQueue(Player player) {
        mine_queue.remove(player.getUniqueId());
    }

    public boolean queueContainsPlayer(Player player) {
        return mine_queue.contains(player.getUniqueId());
    }

    public Queue<UUID> getMine_queue() {
        return mine_queue;
    }

    public int getQueueSize() {
        return mine_queue.size();
    }

    /*
        Gets the current queue slot for the UUID specified.
     */

    public int getQueueSlot(UUID uuid) {
        int count = 0;
        for (UUID id : mine_queue) {
            count++;
            if (id == uuid) {
                Bukkit.broadcastMessage(String.format("%s is at position %d in the queue.",
                        Bukkit.getPlayer(uuid).getName(),
                        count));
                break;
            }
        }
        return count;
    }
}
