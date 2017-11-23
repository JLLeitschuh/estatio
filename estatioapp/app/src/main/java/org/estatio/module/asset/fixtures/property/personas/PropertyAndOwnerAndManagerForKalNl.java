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
package org.estatio.module.asset.fixtures.property.personas;

import org.incode.module.country.dom.impl.Country;

import org.estatio.module.asset.dom.PropertyType;
import org.estatio.module.asset.fixtures.property.PropertyAndUnitsAndOwnerAndManagerAbstract;
import org.estatio.module.asset.fixtures.person.personas.PersonAndRolesForJohnDoeNl;
import org.estatio.module.base.fixtures.security.apptenancy.personas.ApplicationTenancyForNl;
import org.estatio.module.country.fixtures.enums.Country_data;
import org.estatio.module.party.dom.Party;
import org.estatio.module.party.fixtures.organisation.personas.OrganisationForAcmeNl;

import static org.incode.module.base.integtests.VT.ld;

public class PropertyAndOwnerAndManagerForKalNl extends PropertyAndUnitsAndOwnerAndManagerAbstract {

    public static final String REF = "KAL";
    public static final String PARTY_REF_OWNER = OrganisationForAcmeNl.REF;
    public static final String PARTY_REF_MANAGER = PersonAndRolesForJohnDoeNl.REF;
    public static final String AT_PATH_COUNTRY = ApplicationTenancyForNl.PATH;

    public static String unitReference(String suffix) {
        return REF + "-" + suffix;
    }

    @Override
    protected void execute(final ExecutionContext executionContext) {

        // prereqs
        executionContext.executeChild(this, new OrganisationForAcmeNl());
        executionContext.executeChild(this, new PersonAndRolesForJohnDoeNl());

        // exec
        final Party owner = partyRepository.findPartyByReference(PARTY_REF_OWNER);
        final Party manager = partyRepository.findPartyByReference(PARTY_REF_MANAGER);

        final Country netherlands = Country_data.NLD.findUsing(serviceRegistry);

        createPropertyAndUnits(
                AT_PATH_COUNTRY,
                REF, "Kalvertoren", "Amsterdam", netherlands, PropertyType.SHOPPING_CENTER,
                40, ld(2003, 12, 1), ld(2003, 12, 1), owner, manager,
                "52.37597;4.90814", executionContext);
    }


}