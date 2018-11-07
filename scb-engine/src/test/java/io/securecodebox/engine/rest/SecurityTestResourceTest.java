/*
 *
 *  SecureCodeBox (SCB)
 *  Copyright 2015-2018 iteratec GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package io.securecodebox.engine.rest;

import io.securecodebox.engine.service.SecurityTestService;
import io.securecodebox.model.securitytest.SecurityTestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SecurityTestResourceTest {
    @InjectMocks
    SecurityTestResource classUnderTest;

    @Mock
    SecurityTestService securityTestServiceDummy;

    @Test
    public void shouldReturnAnErrorWhenNoSuchProcessIsAvailible() throws Exception {
        willThrow(new SecurityTestService.NonExistentSecurityTestDefinitionException()).given(securityTestServiceDummy).checkSecurityTestDefinitionExistence(any());

        SecurityTestConfiguration secTest = new SecurityTestConfiguration();
        secTest.setName("this-process-will-never-exist");

        ResponseEntity<List<UUID>> response = classUnderTest.startSecurityTests(Arrays.asList(secTest));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void shouldReturnAMultipleChoicesErrorIfThereAreMultipleProcessesForTheSecurityTestName() throws Exception {
        willThrow(new SecurityTestService.DuplicateSecurityTestDefinitionForKeyException()).given(securityTestServiceDummy).checkSecurityTestDefinitionExistence(any());

        SecurityTestConfiguration secTest = new SecurityTestConfiguration();
        secTest.setName("this-process-key-has-multiple-implementations");

        ResponseEntity<List<UUID>> response = classUnderTest.startSecurityTests(Arrays.asList(secTest));

        assertEquals(HttpStatus.MULTIPLE_CHOICES, response.getStatusCode());
    }

    @Test
    public void shouldStartAProcessAndReturnItsUUID() throws Exception {
        given(securityTestServiceDummy.startSecurityTest(any())).willReturn(UUID.fromString("47bd8786-84f2-49ed-9ca9-20ed22be532b"));

        SecurityTestConfiguration secTest = new SecurityTestConfiguration();
        secTest.setName("this-process-is-ok");

        ResponseEntity<List<UUID>> response = classUnderTest.startSecurityTests(Arrays.asList(secTest));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(Arrays.asList(UUID.fromString("47bd8786-84f2-49ed-9ca9-20ed22be532b")), response.getBody());

        verify(securityTestServiceDummy, times(1)).startSecurityTest(secTest);
    }
}