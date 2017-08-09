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

import de.codecentric.boot.admin.server.domain.entities.Application;
import de.codecentric.boot.admin.server.domain.events.ClientApplicationEndpointsDetectedEvent;
import de.codecentric.boot.admin.server.domain.events.ClientApplicationEvent;
import de.codecentric.boot.admin.server.domain.events.ClientApplicationRegisteredEvent;
import de.codecentric.boot.admin.server.domain.events.ClientApplicationStatusChangedEvent;
import de.codecentric.boot.admin.server.domain.values.ApplicationId;
import de.codecentric.boot.admin.server.domain.values.Endpoints;
import de.codecentric.boot.admin.server.domain.values.Registration;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import reactor.core.publisher.Mono;
import reactor.test.publisher.TestPublisher;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InfoUpdateTriggerTest {
    private final Application application = Application.create(ApplicationId.of("id-1"))
                                                       .register(Registration.create("foo", "http://health-1").build());

    @Test
    public void should_update_on_event() throws InterruptedException {
        //given
        InfoUpdater updater = mock(InfoUpdater.class);
        when(updater.updateInfo(any(ApplicationId.class))).thenReturn(Mono.empty());

        TestPublisher<ClientApplicationEvent> events = TestPublisher.create();
        InfoUpdateTrigger trigger = new InfoUpdateTrigger(updater, events.flux());
        trigger.start();
        Thread.sleep(50L); //wait for subscription

        //when some non-status-change event is emitted
        events.next(new ClientApplicationRegisteredEvent(application.getId(), application.getVersion(),
                application.getRegistration()));
        //then should not update
        verify(updater, never()).updateInfo(application.getId());

        //when status-change event is emitted
        events.next(new ClientApplicationStatusChangedEvent(application.getId(), application.getVersion(),
                StatusInfo.ofDown()));
        //then should update
        verify(updater, times(1)).updateInfo(application.getId());

        //when endpoints-detected event is emitted
        clearInvocations(updater);
        events.next(new ClientApplicationEndpointsDetectedEvent(application.getId(), application.getVersion(),
                Endpoints.empty()));
        //then should update
        verify(updater, times(1)).updateInfo(application.getId());

        //when registered event is emitted but the trigger has been stopped
        trigger.stop();
        clearInvocations(updater);
        events.next(new ClientApplicationRegisteredEvent(application.getId(), application.getVersion(),
                application.getRegistration()));
        //then should not update
        verify(updater, never()).updateInfo(application.getId());
    }

}
