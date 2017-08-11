package com.fuze.coreuc.amiproxy.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

@Sources({"file:/Users/rforest/IdeaProjects/AMIProxy/src/main/resources/config.properties", "classpath:config.properties"})

public interface MyAppConfig extends Config {
    @Key("ami.port")
    int amiPort();

    @Key("ami.host")
    String host();

    @Key("ami.username")
    String user();

    @Key("ami.password")
    String password();

    @Key("tcc.port")
    int tccPort();

}
