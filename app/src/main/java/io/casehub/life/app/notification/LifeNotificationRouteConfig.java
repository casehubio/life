package io.casehub.life.app.notification;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.List;

@ConfigMapping(prefix = "casehub.life.notification")
public interface LifeNotificationRouteConfig {

    @WithDefault("household-admin")
    String defaultRecipient();

    @WithDefault("278776f9-e1b0-46fb-9032-8bddebdcf9ce")
    String tenancyId();

    @WithDefault("sms")
    List<String> channels();
}
