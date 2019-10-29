/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.module.charge.dom;

import org.assertj.core.api.Assertions;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.isis.applib.services.registry.ServiceRegistry2;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.core.unittestsupport.jmocking.JUnitRuleMockery2;

import org.isisaddons.module.security.dom.tenancy.ApplicationTenancy;

import org.estatio.module.tax.dom.Tax;

public class ChargeRepository_Test {

    ChargeRepository chargeRepository;

    public static class NewCharge extends ChargeRepository_Test {

        @Rule
        public JUnitRuleMockery2 context = JUnitRuleMockery2.createFor(JUnitRuleMockery2.Mode.INTERFACES_AND_CLASSES);

        @Mock
        private ServiceRegistry2 mockServiceRegistry2;

        @Mock
        private RepositoryService mockRepositoryService;

        private Charge existingCharge;

        private ChargeGroup chargeGroup;
        private Tax tax;

        @Before
        public void setup() {

            chargeGroup = new ChargeGroup();
            tax = new Tax();

            chargeRepository = new ChargeRepository() {
                @Override
                public Charge findByReference(String reference) {
                    return existingCharge;
                }
            };
            chargeRepository.serviceRegistry2 = mockServiceRegistry2;
            chargeRepository.repositoryService = mockRepositoryService;
        }

        @Test
        public void newCharge_whenDoesNotExist() {

            existingCharge = null;

            context.checking(new Expectations() {
                {
                    oneOf(mockServiceRegistry2).injectServicesInto(with(any(Charge.class)));
                    oneOf(mockRepositoryService).persistAndFlush(with(any(Charge.class)));
                }
            });

            final Charge newCharge = chargeRepository.upsert("CG-REF", "CG-Name", "CG-Description",
                    newApplicationTenancy("/it"), Applicability.IN_AND_OUT, tax, chargeGroup
            );
            Assertions.assertThat(newCharge.getReference()).isEqualTo("CG-REF");
            Assertions.assertThat(newCharge.getName()).isEqualTo("CG-Name");
            Assertions.assertThat(newCharge.getDescription()).isEqualTo("CG-Description");
            Assertions.assertThat(newCharge.getTax()).isEqualTo(tax);
            Assertions.assertThat(newCharge.getGroup()).isEqualTo(chargeGroup);
        }

        @Test
        public void newCharge_whenDoesExist() {
            existingCharge = new Charge();

            final Charge newCharge = chargeRepository.upsert("CG-REF", "Some other description", "Some other code",
                    newApplicationTenancy("/it"), Applicability.IN_AND_OUT, null, null
            );
            Assertions.assertThat(newCharge).isEqualTo(existingCharge);
        }

        private ApplicationTenancy newApplicationTenancy(final String path) {
            ApplicationTenancy applicationTenancy = new ApplicationTenancy();
            applicationTenancy.setPath(path);
            return applicationTenancy;
        }

    }

}