package fangchenmi;

import fangchenmi.util.SQLUtil;
import fangchenmi.util.TimeUtil;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class FangChenMi extends JavaPlugin implements Listener, Runnable {
    private final Map<UUID, PlayerStatus> playerBirthMap = new ConcurrentHashMap<>();
    private static final ExecutorService executors = Executors.newCachedThreadPool();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        SQLUtil.connection(getConfig().getString("mysql.host"), getConfig().getInt("mysql.port"), getConfig().getString("mysql.user"), getConfig().getString("mysql.password"), getConfig().getString("mysql.database"), getConfig().getString("mysql.table"));

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, this, 600, 600);
    }

    @Override
    public void run() {
        boolean allowMinor = TimeUtil.allowMinor();
        for (Player player : getServer().getOnlinePlayers()) {
            PlayerStatus status = playerBirthMap.get(player.getUniqueId());
            if (status == PlayerStatus.Minor && !allowMinor) {
                kickPlayerAsync(player, Message.WARNING_MESSAGE);
            } else if (status == PlayerStatus.UnLog) {
                player.sendMessage(Message.WARNING_MESSAGE);
                player.sendMessage(Message.USING_MESSAGE);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        executors.submit(() -> {
            try {
                Date birth = SQLUtil.queryPlayerBirth(player.getUniqueId());
                if (birth == null) {
                    playerBirthMap.put(player.getUniqueId(), PlayerStatus.UnLog);
                } if (!TimeUtil.checkAdult(birth)) {
                    if (!TimeUtil.allowMinor()) {
                        kickPlayerAsync(player, Message.WARNING_MESSAGE);
                        return;
                    }
                    playerBirthMap.put(player.getUniqueId(), PlayerStatus.Minor);
                } else {
                    playerBirthMap.put(player.getUniqueId(), PlayerStatus.Adult);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                playerBirthMap.put(player.getUniqueId(), PlayerStatus.Error);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerBirthMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (playerBirthMap.get(event.getPlayer().getUniqueId()) == PlayerStatus.UnLog) {
            event.getPlayer().sendMessage(Message.WARNING_MESSAGE);
            event.getPlayer().sendMessage(Message.USING_MESSAGE);
            event.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;

        if (args.length == 1) {
            Player player = (Player) sender;
            PlayerStatus status = playerBirthMap.get(player.getUniqueId());
            if (status == PlayerStatus.UnLog) {
                String[] split = args[0].split("\\.");
                if (split.length == 3) {
                    try {
                        Date birth = new Date(Integer.parseInt(split[0]) - 1900, Integer.parseInt(split[1]), Integer.parseInt(split[2]));
                        if (new Date().after(birth)) {
                            player.sendMessage(Message.SETTING_MESSAGE);
                            executors.submit(() -> {
                                try {
                                    if (SQLUtil.setPlayerBirth(player.getUniqueId(), birth)) {
                                        player.sendMessage(String.format(Message.SUCCESS_MESSAGE, args[0]));
                                    }
                                    playerBirthMap.put(player.getUniqueId(), TimeUtil.checkAdult(birth) ? PlayerStatus.Adult : PlayerStatus.Minor);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    player.sendMessage(e.toString());
                                }
                            });
                            return true;
                        }
                    } catch (Exception ignored) {}
                }
            } else if (status != PlayerStatus.Error) {
                player.sendMessage(Message.ALREADY_MESSAGE);
                return true;
            }
        }

        sender.sendMessage(Message.USING_MESSAGE);
        return false;
    }

    public void kickPlayerAsync(Player player, String reason) {
        if (!player.isOnline()) return;
        getServer().getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                player.kickPlayer(reason);
            }
        });
    }

    enum PlayerStatus {
        UnLog, Minor, Adult, Error;
    }
}
