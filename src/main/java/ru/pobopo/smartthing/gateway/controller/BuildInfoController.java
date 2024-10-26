package ru.pobopo.smartthing.gateway.controller;

import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.gateway.aspect.AcceptCloudRequest;
import ru.pobopo.smartthing.gateway.model.CustomPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/build")
public class BuildInfoController {

    private final Map<String, Object> info  = new HashMap<>();

    public BuildInfoController(
            BuildProperties buildProperties,
            ResourceLoader resourceLoader,
            List<CustomPlugin> plugins
    ) {
        Resource webResource = resourceLoader.getResource("classpath:static/index.html");
        info.put("withUi", webResource.exists());

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        info.put("buildTime", formatter.format(Date.from(buildProperties.getTime())));

        info.put("commonsVersion", buildProperties.get("commons.version"));
        info.put("javaVersion", buildProperties.get("java.version"));
        info.put("version", buildProperties.getVersion());
        info.put("plugins", plugins);
    }

    @AcceptCloudRequest
    @GetMapping
    public Map<String, Object> getBuildInfo() {
        return info;
    }

}
