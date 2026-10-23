package net.crimsoningot.dynlair.core;

import net.crimsoningot.dynlair.core.api.DynlairApi;
import net.crimsoningot.dynlair.core.api.DocSource;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public final class DynlairCorePlugin extends JavaPlugin {

    private DynlairService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        service = new DynlairService(this);
        getServer().getServicesManager().register(DynlairApi.class, service, this, ServicePriority.Normal);

        PluginCommand command = getCommand("dynlair");
        if (command != null) command.setExecutor(new DynlairCommand(this, service));

        if (getConfig().getBoolean("export.on-startup", false)) {
            long delay = Math.max(1L, getConfig().getLong("export.startup-delay-ticks", 1L));
            // Scheduled tasks first run once the server has finished enabling every plugin.
            getServer().getScheduler().runTaskLater(this, this::startupExport, delay);
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
    }

    private void startupExport() {
        List<String> ids = service.getSources().stream().map(DocSource::getId).toList();
        if (ids.isEmpty()) {
            getLogger().info("Startup export skipped: no documentation sources registered");
            return;
        }
        service.exportToFile(ids).whenComplete((result, error) -> {
            if (error != null) getLogger().log(Level.SEVERE, "Startup export failed", error);
            else getLogger().info("Startup export wrote " + result.file());
        });
    }
}
