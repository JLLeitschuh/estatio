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
package org.estatio.module.lease.integtests.imports;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.fixturescripts.FixtureResult;
import org.apache.isis.applib.fixturescripts.FixtureScript;
import org.apache.isis.applib.services.registry.ServiceRegistry2;
import org.apache.isis.applib.services.xactn.TransactionService3;
import org.apache.isis.applib.value.Blob;

import org.estatio.module.asset.dom.Property;
import org.estatio.module.asset.fixtures.property.enums.Property_enum;
import org.estatio.module.lease.contributions.Property_maintainTurnOverRentSwe;
import org.estatio.module.lease.dom.Lease;
import org.estatio.module.lease.dom.LeaseItem;
import org.estatio.module.lease.dom.LeaseItemType;
import org.estatio.module.lease.dom.LeaseTerm;
import org.estatio.module.lease.dom.LeaseTermForTurnoverRent;
import org.estatio.module.lease.fixtures.imports.TurnOverRentFixedImportFixture;
import org.estatio.module.lease.fixtures.lease.enums.Lease_enum;
import org.estatio.module.lease.fixtures.leaseitems.enums.LeaseItemForTurnoverRentSwe_enum;
import org.estatio.module.lease.imports.LeaseTermForTurnOverRentSweImport;
import org.estatio.module.lease.imports.LeaseTermForTurnoverRentSweImportManager;
import org.estatio.module.lease.integtests.LeaseModuleIntegTestAbstract;

import static org.assertj.core.api.Assertions.assertThat;

public class LeasetermForTurnoverRentFixedImport_IntegTest extends LeaseModuleIntegTestAbstract {

    List<FixtureResult> fixtureResults;

    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(ExecutionContext executionContext) {
                executionContext.executeChild(this, new TurnOverRentFixedImportFixture());
                executionContext.executeChild(this, LeaseItemForTurnoverRentSwe_enum.HanPoison001Se.builder());
                executionContext.executeChild(this, LeaseItemForTurnoverRentSwe_enum.HanTopModel002Se.builder());
                fixtureResults = executionContext.getResults();
            }
        });
    }

    Property han;

    @Test
    public void export_lines_test() throws Exception {

        // given
        han = Property_enum.HanSe.findUsing(serviceRegistry2);
        Property_maintainTurnOverRentSwe mixin = new Property_maintainTurnOverRentSwe(han);

        // when
        final LeaseTermForTurnoverRentSweImportManager manager = wrap(mixin).maintainTurnoverRent(2011);

        // then
        assertThat(manager.getTurnoverRentLines()).hasSize(2);
        final LeaseTermForTurnOverRentSweImport lineForPoison = manager.getTurnoverRentLines().get(0);
        assertThat(lineForPoison.getLeaseReference()).isEqualTo(Lease_enum.HanPoison001Se.findUsing(serviceRegistry2).getReference());
        assertThat(lineForPoison.getStartDate()).isEqualTo(new LocalDate(2011,1,1));
        assertThat(lineForPoison.getEndDate()).isEqualTo(new LocalDate(2011,12,31));
        assertThat(lineForPoison.getValuePreviousYear()).isNull();
        assertThat(lineForPoison.getPercentagePreviousYear()).isNull();
        assertThat(lineForPoison.getValue()).isEqualTo(new BigDecimal("20000.00"));
        assertThat(lineForPoison.getPercentage()).isEqualTo(new BigDecimal("7"));
        assertThat(lineForPoison.getYear()).isEqualTo(2011);
        final LeaseTermForTurnOverRentSweImport lineForTopModel = manager.getTurnoverRentLines().get(1);
        assertThat(lineForTopModel.getLeaseReference()).isEqualTo(Lease_enum.HanTopModel002Se.findUsing(serviceRegistry2).getReference());
        assertThat(lineForTopModel.getStartDatePreviousYear()).isEqualTo(new LocalDate(2010,7,15));
        assertThat(lineForTopModel.getValuePreviousYear()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(lineForTopModel.getPercentagePreviousYear()).isEqualTo(new BigDecimal("1.23"));
        assertThat(lineForTopModel.getValue()).isNull();
        assertThat(lineForTopModel.getPercentage()).isNull();
        assertThat(lineForTopModel.getYear()).isEqualTo(2011);
        
    }

    @Test
    public void import_test() throws Exception {

        // given
        Lease leaseForPoison = Lease_enum.HanPoison001Se.findUsing(serviceRegistry2);
        Lease leaseForTopmodel = Lease_enum.HanTopModel002Se.findUsing(serviceRegistry2);
        LeaseItem itemForPoison = leaseForPoison.findFirstItemOfType(LeaseItemType.TURNOVER_RENT);
        LeaseItem itemForTopmodel = leaseForTopmodel.findFirstItemOfType(LeaseItemType.TURNOVER_RENT);
        Blob excelSheet = (Blob) fixtureResults.get(0).getObject();

        LeaseTermForTurnoverRentSweImportManager manager = new LeaseTermForTurnoverRentSweImportManager();
        manager.setYear(2011);
        manager.setProperty(han = Property_enum.HanSe.findUsing(serviceRegistry2));

        // when
        wrap(manager).upload(excelSheet);
        transactionService2.nextTransaction();

        // then
        final LocalDate startDate2010 = new LocalDate(2010, 1, 1);
        final LocalDate endDate2010 = new LocalDate(2010, 12, 31);
        final LocalDate startDate2011 = new LocalDate(2011, 1, 1);
        final LocalDate endDate2011 = new LocalDate(2011, 12, 31);

        final LeaseTerm term1Poison = itemForPoison.findTerm(startDate2010);
        assertThat(term1Poison).isNull(); // previous terms, when not found, are no longer created since ECP-806
        final LeaseTerm term2Poison = itemForPoison.findTerm(startDate2011);
        assertThat(term2Poison.getEffectiveValue()).isEqualTo(new BigDecimal("21000.00"));
        assertThat(term2Poison.getEndDate()).isEqualTo(endDate2011);
        final LeaseTermForTurnoverRent castedTerm2Poison = (LeaseTermForTurnoverRent) term2Poison;
        assertThat(castedTerm2Poison.getManualTurnoverRent()).isEqualTo(new BigDecimal("21000.00"));
        assertThat(castedTerm2Poison.getTurnoverRentRule()).isEqualTo("6.25");

        final LeaseTerm term1Topmodel = itemForTopmodel.findTerm(new LocalDate(2010, 7,15));
        assertThat(term1Topmodel.getEffectiveValue()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(term1Topmodel.getEndDate()).isEqualTo(endDate2010);
        final LeaseTermForTurnoverRent castedTerm1Topmodel = (LeaseTermForTurnoverRent) term1Topmodel;
        assertThat(castedTerm1Topmodel.getManualTurnoverRent()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(castedTerm1Topmodel.getTurnoverRentRule()).isEqualTo("1.25");
        final LeaseTerm term2Topmodel = itemForTopmodel.findTerm(startDate2011);
        assertThat(term2Topmodel.getEffectiveValue()).isEqualTo(new BigDecimal("2100.00"));
        assertThat(term2Topmodel.getEndDate()).isEqualTo(endDate2011);
        final LeaseTermForTurnoverRent castedTerm2Topmodel = (LeaseTermForTurnoverRent) term2Topmodel;
        assertThat(castedTerm2Topmodel.getManualTurnoverRent()).isEqualTo(new BigDecimal("2100.00"));
        assertThat(castedTerm2Topmodel.getTurnoverRentRule()).isEqualTo("1.3");
    }

    @Inject
    ServiceRegistry2 serviceRegistry2;


    @Inject
    TransactionService3 transactionService2;


}