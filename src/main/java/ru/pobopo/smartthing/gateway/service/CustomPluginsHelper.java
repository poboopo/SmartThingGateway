package ru.pobopo.smartthing.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import ru.pobopo.smartthing.consumers.DeviceNotificationConsumer;
import ru.pobopo.smartthing.gateway.model.CustomPlugin;
import ru.pobopo.smartthing.consumers.DashboardUpdatesConsumer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class CustomPluginsHelper {
    private static final List<Class<?>> ALLOWED_INTERFACES = List.of(
            DeviceNotificationConsumer.class,
            DashboardUpdatesConsumer.class
    );

    public static <T> List<T> createBeansFromPlugins(ApplicationContext applicationContext, List<CustomPlugin> plugins, Class<T> targetInterface) {
        List<T> loadedBeans = new ArrayList<>();
        for (CustomPlugin plugin: plugins) {
            for (Class<?> clazz: plugin.getClasses()) {
                if (Arrays.stream(clazz.getInterfaces()).anyMatch(i -> i == targetInterface)) {
                    try {
                        // todo inject dependencies?
                        // ApplicationContext.AutowireCapableBeanFactory.createBean()
                        loadedBeans.add((T) applicationContext.getAutowireCapableBeanFactory().createBean(clazz));
                    } catch (Exception e) {
                        log.error("Failed to create bean {} of plugin {} (error message: {})", clazz, plugin, e.getMessage());
                    }
                }
            }
        }
        return loadedBeans;
    }

    public static Optional<CustomPlugin> loadPlugin(Path path) {
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
                return Optional.empty();
            }

            CustomPlugin plugin = new CustomPlugin(pluginName, loadedClasses);
            log.info("Finished loading plugin {}", plugin);
            return Optional.of(plugin);
        } catch (ClassNotFoundException | IOException e) {
            log.error("Failed to load plugin name={}, path={}. Error message: {}", pluginName, pathToJar, e.getMessage());
        }
        return Optional.empty();
    }

    private static String extractPluginName(String pathToJar) {
        String[] split = pathToJar.split("/");
        return split[split.length - 1].replace(".jar", "");
    }
}
