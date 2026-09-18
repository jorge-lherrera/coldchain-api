package com.coldchain.modules.identity.internal.application;

import com.coldchain.modules.identity.api.ActorType;
import com.coldchain.shared.security.CurrentActor;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CurrentIdentityActor {

    private final CurrentActor actor;

    public CurrentIdentityActor(CurrentActor actor) {
        this.actor = actor;
    }

    public ActorType type() {
        return actor.actorType().map(ActorType::valueOf).orElse(ActorType.SYSTEM);
    }

    public UUID id() {
        return actor.requireId();
    }
}
