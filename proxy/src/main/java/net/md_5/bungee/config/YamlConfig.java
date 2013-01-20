package net.md_5.bungee.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class YamlConfig implements ConfigurationAdapter
{

    private boolean loaded;
    private Yaml yaml;
    private Map config;
    File file = new File("config.yml");

    public void load()
    {
        try
        {
            file.createNewFile();
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            yaml = new Yaml(options);

            try (InputStream is = new FileInputStream(file))
            {
                config = (Map) yaml.load(is);
            }

            if (config == null)
            {
                config = new LinkedHashMap<>();
            }

            loaded = true;
        } catch (IOException ex)
        {
            throw new RuntimeException("Could not load configuration!", ex);
        }
    }

    private <T> T get(String path, T def)
    {
        if (!loaded)
        {
            load();
        }
        return get(path, def, config);
    }

    @SuppressWarnings("unchecked")
    private <T> T get(String path, T def, Map submap)
    {
        if (!loaded)
        {
            load();
        }

        int index = path.indexOf('.');
        if (index == -1)
        {
            Object val = submap.get(path);
            if (val == null)
            {
                val = def;
                submap.put(path, def);
                save();
            }
            return (T) val;
        } else
        {
            String first = path.substring(0, index);
            String second = path.substring(index, path.length());
            Map sub = (Map) submap.get(first);
            return (sub != null) ? get(second, def, sub) : def;
        }
    }

    private void save()
    {
        try
        {
            try (FileWriter wr = new FileWriter(file))
            {
                yaml.dump(config, wr);
            }
        } catch (IOException ex)
        {
            ProxyServer.getInstance().getLogger().log(Level.WARNING, "Could not save config", ex);
        }
    }

    @Override
    public int getInt(String path, int def)
    {
        return get(path, def);
    }

    @Override
    public String getString(String path, String def)
    {
        return get(path, def);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ServerInfo> getServers()
    {
        Map<String, Map<String, Object>> base = get("servers", (Map) Collections.singletonMap("lobby", new HashMap<>()));
        Map<String, ServerInfo> ret = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : base.entrySet())
        {
            Map<String, Object> val = entry.getValue();
            String name = entry.getKey();
            String addr = get("address", "localhost:25565", val);
            InetSocketAddress address = Util.getAddr(addr);
            ServerInfo info = new ServerInfo(name, address);
            ret.put(name, info);
        }

        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<ListenerInfo> getListeners()
    {
        Collection<Map<String, Object>> base = get("listeners", (Collection) Arrays.asList(new Map[]
                {
                    new HashMap()
                }));
        Collection<ListenerInfo> ret = new HashSet<>();

        for (Map<String, Object> val : base)
        {
            String motd = get("motd", "Another Bungee server", val);
            int maxPlayers = get("max_players", 1, val);
            String defaultServer = get("default_server", "lobby", val);
            boolean forceDefault = get("force_default_server", false, val);
            String host = get("host", "0.0.0.0:25577", val);
            InetSocketAddress address = Util.getAddr(host);
            ListenerInfo info = new ListenerInfo(address, motd, maxPlayers, defaultServer, forceDefault);
            ret.add(info);
        }

        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> getGroups(String player)
    {
        return get("groups." + player, Collections.EMPTY_SET);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> getPermissions(String group)
    {
        return get("permissions." + group, Collections.EMPTY_SET);
    }
}