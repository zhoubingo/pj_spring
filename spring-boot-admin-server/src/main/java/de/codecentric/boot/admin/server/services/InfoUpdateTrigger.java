/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.boot.admin.server.services;

import de.codecentric.boot.admin.server.domain.events.ClientApplicationEndpointsDetectedEvent;
import de.codecentric.boot.admin.server.domain.events.ClientApplicationEvent;
import de.codecentric.boot.admin.server.domain.events.ClientApplicationStatusChangedEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.reactivestreams.Publisher;

public class InfoUpdateTrigger extends ResubscribingEventHandler<ClientApplicationEvent> {
    private final InfoUpdater infoUpdater;

    public InfoUpdateTrigger(InfoUpdater infoUpdater, Publisher<ClientApplicationEvent> publisher) {
        super(publisher, ClientApplicationEvent.class);
        this.infoUpdater = infoUpdater;
    }

    @Override
    protected Publisher<?> handle(Flux<ClientApplicationEvent> publisher) {
        return publisher.subscribeOn(Schedulers.newSingle("info-updater"))
                        .filter(event -> event instanceof ClientApplicationEndpointsDetectedEvent ||
                                         event instanceof ClientApplicationStatusChangedEvent)
                        .flatMap(this::updateInfo);
    }

    protected Mono<Void> updateInfo(ClientApplicationEvent event) {
        return infoUpdater.updateInfo(event.getApplication());
    }
}
