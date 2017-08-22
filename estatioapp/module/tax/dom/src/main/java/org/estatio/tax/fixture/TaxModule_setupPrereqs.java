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
package org.estatio.tax.fixture;

import org.apache.isis.applib.fixturescripts.FixtureScript;

import org.estatio.apptenancy.fixture.data.ApplicationTenancy_data;
import org.estatio.tax.fixture.data.Country_data;

public class TaxModule_setupPrereqs extends FixtureScript {

    static boolean beenRun = false;

    @Override
    protected void execute(final ExecutionContext executionContext) {
        if(beenRun) return;
        executionContext.executeChild(this, new ApplicationTenancy_data.PersistScript());
        executionContext.executeChild(this, new Country_data.PersistScript());
        beenRun = true;
    }

}
