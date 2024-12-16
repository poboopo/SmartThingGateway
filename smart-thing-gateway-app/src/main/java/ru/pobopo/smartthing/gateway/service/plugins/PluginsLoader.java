package ru.pobopo.smartthing.gateway.service.plugins;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static ru.pobopo.smartthing.gateway.SmartThingGatewayApp.DEFAULT_APP_DIR;

@Service
public class PluginsLoader {

    public List<Class<?>> loadPlugins() throws IOException {
        List<Class<?>> loadedClasses = new ArrayList<>();
        Files.list(Path.of(DEFAULT_APP_DIR.toString(), "plugins")).forEach(path -> {
            try {
                loadedClasses.addAll(loadClassesFromJar(path.toString()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return loadedClasses;
    }

    private List<Class<?>> loadClassesFromJar(String pathToJar) throws IOException {
        try (JarFile jarFile = new JarFile(pathToJar)) {
            List<Class<?>> result = new ArrayList<>();
            Enumeration<JarEntry> e = jarFile.entries();

            URL[] urls = { new URL("jar:file:" + pathToJar +"!/") };
            URLClassLoader cl = URLClassLoader.newInstance(urls);

            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();
                if(je.isDirectory() || !je.getName().endsWith(".class")) {
                    continue;
                }
                // -6 because of .class
                String className = je.getName().substring(0,je.getName().length()-6);
                className = className.replace('/', '.');
                result.add(cl.loadClass(className));
            }
            return result;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
