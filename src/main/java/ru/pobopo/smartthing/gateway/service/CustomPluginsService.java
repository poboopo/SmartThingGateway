package ru.pobopo.smartthing.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.consumers.DeviceLogsConsumer;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.gateway.model.CustomPlugin;
import ru.pobopo.smartthing.consumers.DashboardUpdatesConsumer;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static ru.pobopo.smartthing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Slf4j
@Service
public class CustomPluginsService {
    private static final Path PLUGINS_DIR_DEFAULT = Path.of(DEFAULT_APP_DIR.toString(), "plugins");

    private static final List<Class<?>> ALLOWED_INTERFACES = List.of(
            DeviceNotificationConsumer.class,
            DashboardUpdatesConsumer.class,
            DeviceLogsConsumer.class
    );

    private final ApplicationContext applicationContext;
    private final List<CustomPlugin> plugins = new ArrayList<>();

    public CustomPluginsService(
            @Value("${plugins.dir:}") String pluginsDir,
            ApplicationContext applicationContext
    ) throws IOException {
        this.applicationContext = applicationContext;

        this.loadPlugins(StringUtils.isEmpty(pluginsDir) ? PLUGINS_DIR_DEFAULT : Path.of(pluginsDir));
    }

    public void loadPlugin(Path path) {
        String pathToJar = path.toString();
        String pluginName = extractPluginName(pathToJar);

        try (JarFile jarFile = new JarFile(pathToJar)) {
            log.info("Loading plugin {} ({})", pluginName, pathToJar);

            Enumeration<JarEntry> e = jarFile.entries();
            URL[] urls = { new URL("jar:file:" + pathToJar + "!/") };
            URLClassLoader cl = URLClassLoader.newInstance(urls);

            List<Class<?>> loadedClasses = new ArrayList<>();
            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                if(je.isDirectory() || !je.getName().endsWith(".class")) {
                    continue;
                }
                String className = je.getName()
                        .substring(0, je.getName().length() - 6)
                        .replace('/', '.');
                Class<?> clazz = cl.loadClass(className);

                if (Arrays.stream(clazz.getInterfaces()).anyMatch(ALLOWED_INTERFACES::contains)) {
                    log.info("Obtained {}", clazz);
                    loadedClasses.add(clazz);
                } else {
                    log.info("{} ignored", clazz);
                }
            }

            if (loadedClasses.isEmpty()) {
                log.warn("Ignoring plugin {} because there is no allowed classes found", pluginName);
                return;
            }

            CustomPlugin plugin = new CustomPlugin(pluginName, loadedClasses);
            log.info("Finished loading plugin {}", plugin);
            plugins.add(plugin);
        } catch (ClassNotFoundException | IOException e) {
            log.error("Failed to load plugin name={}, path={}. Error message: {}", pluginName, pathToJar, e.getMessage());
        }
    }

    private void loadPlugins(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            Files.createDirectory(dirPath);
        } else if (!Files.isDirectory(dirPath)) {
            throw new IllegalStateException("Can't use " + dirPath + " as plugins directory path - it's a file, dummy");
        }

        log.info("Loading plugins from {}", dirPath);
        Files.list(dirPath).forEach(this::loadPlugin);
        log.info("Loaded plugins count: {}", plugins.size());
    }

    public <T> List<T> createBeansFromPlugins(Class<T> targetInterface) {
        List<T> loadedBeans = new ArrayList<>();
        for (CustomPlugin plugin: plugins) {
            for (Class<?> clazz: plugin.getClasses()) {
                if (Arrays.stream(clazz.getInterfaces()).anyMatch(i -> i == targetInterface)) {
                    try {
                        loadedBeans.add((T) applicationContext.getAutowireCapableBeanFactory().createBean(clazz));
                    } catch (Exception e) {
                        log.error("Failed to create bean {} of plugin {} (error message: {})", clazz, plugin, e.getMessage());
                    }
                }
            }
        }
        return loadedBeans;
    }

    private static String extractPluginName(String pathToJar) {
        String[] split = pathToJar.split("/");
        return split[split.length - 1].replace(".jar", "");
    }
}
