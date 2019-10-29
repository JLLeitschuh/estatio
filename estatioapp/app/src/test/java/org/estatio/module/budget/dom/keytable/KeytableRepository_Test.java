/*
 * Copyright 2015 Yodo Int. Projects and Consultancy
 *
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.estatio.module.budget.dom.keytable;

import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;

import org.estatio.module.budget.dom.budget.Budget;

import static org.assertj.core.api.Assertions.assertThat;

public class KeytableRepository_Test {

    public static class NewKeyTable extends KeytableRepository_Test {

        @Rule
        public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

        @Mock
        private FactoryService mockFactoryService;

        @Mock
        private RepositoryService mockRepositoryService;

        KeyTableRepository keyTableRepository;

        @Before
        public void setup() {
            keyTableRepository = new KeyTableRepository();
            keyTableRepository.repositoryService = mockRepositoryService;
            keyTableRepository.factoryService = mockFactoryService;
        }

        @Test
        public void newKeyTable() {

            //given
            Budget budget = new Budget();
            final KeyTable keyTable = new KeyTable();

            // expect
            context.checking(new Expectations() {
                {
                    oneOf(mockFactoryService).instantiate(KeyTable.class);
                    will(returnValue(keyTable));
                    oneOf(mockRepositoryService).persist(keyTable);
                }

            });

            //when
            KeyTable newKeyTable = keyTableRepository.newKeyTable(
                    budget,
                    "new keyTable",
                    FoundationValueType.AREA,
                    KeyValueMethod.PERCENT,
                    6);

            //then
            assertThat(newKeyTable.getBudget()).isEqualTo(budget);
            assertThat(newKeyTable.getName()).isEqualTo("new keyTable");
            assertThat(newKeyTable.getFoundationValueType()).isEqualTo(FoundationValueType.AREA);
            assertThat(newKeyTable.getKeyValueMethod()).isEqualTo(KeyValueMethod.PERCENT);
            assertThat(newKeyTable.getPrecision()).isEqualTo(6);
        }

    }

}
