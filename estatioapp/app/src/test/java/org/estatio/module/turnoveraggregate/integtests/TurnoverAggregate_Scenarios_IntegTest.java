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
package org.estatio.module.turnoveraggregate.integtests;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.fixturescripts.FixtureScript;
import org.apache.isis.applib.services.registry.ServiceRegistry2;

import org.estatio.module.asset.dom.Property;
import org.estatio.module.asset.fixtures.property.enums.Property_enum;
import org.estatio.module.currency.dom.Currency;
import org.estatio.module.currency.fixtures.enums.Currency_enum;
import org.estatio.module.lease.dom.Lease;
import org.estatio.module.lease.dom.occupancy.Occupancy;
import org.estatio.module.lease.fixtures.lease.enums.Lease_enum;
import org.estatio.module.turnover.contributions.Occupancy_turnovers;
import org.estatio.module.turnover.dom.Frequency;
import org.estatio.module.turnover.dom.Turnover;
import org.estatio.module.turnover.dom.TurnoverReportingConfig;
import org.estatio.module.turnover.dom.TurnoverReportingConfigRepository;
import org.estatio.module.turnover.dom.TurnoverRepository;
import org.estatio.module.turnover.dom.Type;
import org.estatio.module.turnoveraggregate.contributions.Lease_aggregateTurnovers;
import org.estatio.module.turnoveraggregate.dom.TurnoverAggregation;
import org.estatio.module.turnoveraggregate.dom.TurnoverAggregationRepository;
import org.estatio.module.turnoveraggregate.fixtures.TurnoverImportXlsxFixtureForAggregated123;
import org.estatio.module.turnoveraggregate.fixtures.TurnoverImportXlsxFixtureForAggregatedMinute;

import static org.assertj.core.api.Assertions.assertThat;

public class TurnoverAggregate_Scenarios_IntegTest extends TurnoverAggregateModuleIntegTestAbstract {

    @Before
    public void setupData() {
        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(ExecutionContext executionContext) {
                executionContext.executeChild(this, Currency_enum.EUR.builder());
                executionContext.executeChild(this, Lease_enum.Oxf123.builder());
                executionContext.executeChild(this, Lease_enum.Oxfmin2.builder());
                executionContext.executeChild(this, Lease_enum.Oxfmin3.builder());

            }
        });
    }

    LocalDate endDateOcc1;
    Lease oxf123Lease;
    Occupancy occ1_123;
    TurnoverReportingConfig occ1Cfg_123;
    Occupancy occ2_123;
    TurnoverReportingConfig occ2Cfg_123;
    Property oxf;
    Currency euro;

    Lease oxfMin2Lease;
    Lease oxfMin3Lease;
    Occupancy occ1_min2;
    TurnoverReportingConfig occ1Cfg_min2;
    Occupancy occ2_min2;
    TurnoverReportingConfig occ2Cfg_min2;
    Occupancy occ_min3;
    TurnoverReportingConfig occCfg_min3;

    void setupScenario_and_validate_import_123() {

        oxf = Property_enum.OxfGb.findUsing(serviceRegistry2);
        euro = Currency_enum.EUR.findUsing(serviceRegistry2);
        oxf123Lease = Lease_enum.Oxf123.findUsing(serviceRegistry2);
        oxf123Lease.setTenancyStartDate(new LocalDate(2005, 10, 28));
        occ1_123 = oxf123Lease.getOccupancies().last();
        occ1Cfg_123 = turnoverReportingConfigRepository.findOrCreate(occ1_123, Type.PRELIMINARY,null, occ1_123.getStartDate(), Frequency.MONTHLY, euro);
        occ2_123 = oxf123Lease.getOccupancies().first();
        occ2Cfg_123 = turnoverReportingConfigRepository.findOrCreate(occ2_123, Type.PRELIMINARY,null, occ2_123.getStartDate(), Frequency.MONTHLY, euro);

        assertThat(occ1_123.getUnit().getName()).isEqualTo("Unit 1");
        assertThat(occ2_123.getUnit().getName()).isEqualTo("Unit 2");

        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(ExecutionContext executionContext) {
                executionContext.executeChild(this, new TurnoverImportXlsxFixtureForAggregated123());
            }
        });

        final List<Turnover> turnovers = turnoverRepository.listAll();
        assertThat(turnovers).hasSize(119);
        final List<Turnover> turnoversOnOcc1 = mixin(Occupancy_turnovers.class, occ1_123).turnovers();
        final List<Turnover> turnoversOnOcc2 = mixin(Occupancy_turnovers.class, occ2_123).turnovers();

        assertThat(turnoversOnOcc1).hasSize(112);
        final LocalDate maxDateOcc1 = turnoversOnOcc1.stream().max(Comparator.comparing(Turnover::getDate))
                .map(Turnover::getDate).get();
        assertThat(maxDateOcc1).isEqualTo(new LocalDate(2019, 4,1));

        assertThat(turnoversOnOcc2).hasSize(7);
        final LocalDate minDateOcc2 = turnoversOnOcc2.stream().min(Comparator.comparing(Turnover::getDate))
                .map(Turnover::getDate).get();
        assertThat(minDateOcc2).isEqualTo(new LocalDate(2019, 5,1));

    }

    @Test
    public void calculate_turnover_aggregations_works() throws Exception {

        // given
        setFixtureClockDate(new LocalDate(2020,2,16));
        setupScenario_and_validate_import_123();
        assertThat(turnoverAggregationRepository.listAll()).isEmpty();

        // when
        final LocalDate startDate = new LocalDate(2018, 2, 1);
        final LocalDate endDate = new LocalDate(2020, 1, 1);
        mixin(Lease_aggregateTurnovers.class, oxf123Lease).$$(startDate, endDate,false);
        transactionService.nextTransaction();

        // then
        final List<TurnoverAggregation> aggregations = turnoverAggregationRepository.listAll();
        final List<TurnoverAggregation> aggsOcc2 = turnoverAggregationRepository
                .findByTurnoverReportingConfig(occ2Cfg_123);
        final TurnoverAggregation agg20200101 = turnoverAggregationRepository.findUnique(occ2Cfg_123, endDate);
        assertThat(agg20200101).isNotNull();

        assertThat(agg20200101.getAggregate1Month().getGrossAmountPreviousYear()).isEqualTo(new BigDecimal("93836.00"));
        assertThat(agg20200101.getAggregate1Month().getTurnoverCountPreviousYear()).isEqualTo(1);
        assertThat(agg20200101.getAggregate1Month().getNetAmountPreviousYear()).isEqualTo(new BigDecimal("0.00"));
        assertThat(agg20200101.getAggregate1Month().getTurnoverCount()).isNull();
        assertThat(agg20200101.getAggregate1Month().getNonComparableThisYear()).isNull();
        assertThat(agg20200101.getAggregate1Month().getNonComparablePreviousYear()).isEqualTo(false);
        assertThat(agg20200101.getAggregate2Month().getGrossAmount()).isNull();
        assertThat(agg20200101.getAggregate2Month().getNonComparableThisYear()).isNull();
        assertThat(agg20200101.getAggregate2Month().getGrossAmountPreviousYear()).isEqualTo(new BigDecimal("176524.00"));
        assertThat(agg20200101.getAggregate2Month().getTurnoverCountPreviousYear()).isEqualTo(2);
        assertThat(agg20200101.getAggregate3Month().getNonComparableThisYear()).isFalse();
        assertThat(agg20200101.getAggregate3Month().getGrossAmountPreviousYear()).isEqualTo(new BigDecimal("276566.00"));
        assertThat(agg20200101.getAggregate3Month().getTurnoverCountPreviousYear()).isEqualTo(3);
        assertThat(agg20200101.getAggregate12Month().getGrossAmountPreviousYear()).isEqualTo(new BigDecimal("900140.00"));
        assertThat(agg20200101.getAggregate12Month().getTurnoverCountPreviousYear()).isEqualTo(12);
        assertThat(agg20200101.getGrossAmount1MCY_2()).isEqualTo(new BigDecimal("65264.00"));


        final TurnoverAggregation agg20191101 = turnoverAggregationRepository.findUnique(
                occ2Cfg_123, new LocalDate(2019,11,1));
        assertThat(agg20191101.getPurchaseCountAggregate1Month().isComparable()).isEqualTo(true);
        // TODO: find out why in current situation non-comparable ...
//        assertThat(agg20191101.getPurchaseCountAggregate3Month().isComparable()).isEqualTo(false);
//        assertThat(agg20191101.getPurchaseCountAggregate6Month().isComparable()).isEqualTo(false);
//        assertThat(agg20191101.getPurchaseCountAggregate12Month().isComparable()).isEqualTo(false);

        // when
        final LocalDate startDate1 = new LocalDate(2020, 1, 1);
        mixin(Lease_aggregateTurnovers.class, oxf123Lease).$$(startDate1, startDate1.plusMonths(23), false);
        transactionService.nextTransaction();
        // then still
        final TurnoverAggregation agg20200101v2 = turnoverAggregationRepository.findUnique(
                occ2Cfg_123, new LocalDate(2020,1,1));
        assertThat(agg20200101v2.getAggregate1Month().getGrossAmountPreviousYear()).isEqualTo(new BigDecimal("93836.00"));
        assertThat(agg20200101v2.getAggregate12Month().getGrossAmountPreviousYear()).isEqualTo(new BigDecimal("900140.00"));
        assertThat(agg20200101v2.getAggregate12Month().getTurnoverCountPreviousYear()).isEqualTo(12);
        assertThat(agg20200101v2.getGrossAmount1MCY_2()).isEqualTo(new BigDecimal("65264.00"));

        //when
        final TurnoverAggregation agg20100101 = turnoverAggregationRepository.findUnique(
                occ1Cfg_123, new LocalDate(2010,1,1));
        // then
        assertThat(agg20100101.getPurchaseCountAggregate1Month().getCount()).isEqualTo(null);
//        assertThat(agg20100101.getPurchaseCountAggregate1Month().getCountPreviousYear()).isEqualTo(new BigInteger("0")); // TODO: find out why in current situation 0 ...
        assertThat(agg20100101.getPurchaseCountAggregate3Month().getCount()).isEqualTo(null);
//        assertThat(agg20100101.getPurchaseCountAggregate3Month().getCountPreviousYear()).isEqualTo(new BigInteger("0"));
        assertThat(agg20100101.getPurchaseCountAggregate6Month().getCount()).isEqualTo(null);
//        assertThat(agg20100101.getPurchaseCountAggregate6Month().getCountPreviousYear()).isEqualTo(new BigInteger("0"));
        assertThat(agg20100101.getPurchaseCountAggregate12Month().getCount()).isEqualTo(null);
//        assertThat(agg20100101.getPurchaseCountAggregate12Month().getCountPreviousYear()).isEqualTo(new BigInteger("0"));

    }


    void setupScenario_and_validate_import_minute() {

        oxf = Property_enum.OxfGb.findUsing(serviceRegistry2);
        euro = Currency_enum.EUR.findUsing(serviceRegistry2);
        oxfMin2Lease = Lease_enum.Oxfmin2.findUsing(serviceRegistry2);
        oxfMin2Lease.setTenancyStartDate(new LocalDate(2016, 6, 1));
        oxfMin2Lease.setTenancyEndDate(new LocalDate(2019,8,11));
        oxfMin3Lease = Lease_enum.Oxfmin3.findUsing(serviceRegistry2);
        oxfMin3Lease.setTenancyStartDate(new LocalDate(2019, 8, 12));
        oxfMin2Lease.setNext(oxfMin3Lease);
        oxfMin3Lease.setPrevious(oxfMin2Lease);

        occ1_min2 = oxfMin2Lease.getOccupancies().first();
        occ1Cfg_min2 = turnoverReportingConfigRepository.findOrCreate(occ1_min2, Type.PRELIMINARY,null, occ1_min2.getStartDate(), Frequency.MONTHLY, euro);
        occ2_min2 = oxfMin2Lease.getOccupancies().last();
        occ2Cfg_min2 = turnoverReportingConfigRepository.findOrCreate(occ2_min2, Type.PRELIMINARY,null, occ2_min2.getStartDate(), Frequency.MONTHLY, euro);
        occ_min3 = oxfMin3Lease.getOccupancies().first();
        occCfg_min3 = turnoverReportingConfigRepository.findOrCreate(occ_min3, Type.PRELIMINARY,null, occ_min3.getStartDate(), Frequency.MONTHLY, euro);

        assertThat(occ1_min2.getUnit().getName()).isEqualTo("Unit 1");
        assertThat(occ2_min2.getUnit().getName()).isEqualTo("Unit 2");
        assertThat(occ_min3.getUnit().getName()).isEqualTo("Unit 1");

        runFixtureScript(new FixtureScript() {
            @Override
            protected void execute(ExecutionContext executionContext) {
                executionContext.executeChild(this, new TurnoverImportXlsxFixtureForAggregatedMinute());
            }
        });

        final List<Turnover> turnovers = turnoverRepository.listAll();
        assertThat(turnovers).hasSize(70);
        final List<Turnover> turnoversOnOcc1Min2 = mixin(Occupancy_turnovers.class, occ1_min2).turnovers();
        final List<Turnover> turnoversOnOcc2Min2 = mixin(Occupancy_turnovers.class, occ2_min2).turnovers();
        final List<Turnover> turnoversOnOccMin3 = mixin(Occupancy_turnovers.class, occ_min3).turnovers();

        assertThat(turnoversOnOcc1Min2).hasSize(21);
        final LocalDate maxDateOcc1 = turnoversOnOcc1Min2.stream().max(Comparator.comparing(Turnover::getDate))
                .map(Turnover::getDate).get();
        assertThat(maxDateOcc1).isEqualTo(new LocalDate(2019, 8,1));

        assertThat(turnoversOnOcc2Min2).hasSize(46);
        final LocalDate minDateOcc2 = turnoversOnOcc2Min2.stream().min(Comparator.comparing(Turnover::getDate))
                .map(Turnover::getDate).get();
        assertThat(minDateOcc2).isEqualTo(new LocalDate(2014, 7,1));
        final LocalDate maxDateOcc2 = turnoversOnOcc2Min2.stream().max(Comparator.comparing(Turnover::getDate))
                .map(Turnover::getDate).get();
        assertThat(maxDateOcc2).isEqualTo(new LocalDate(2019, 8,1));

        assertThat(turnoversOnOccMin3).hasSize(3);
        final LocalDate minDateOccMin3 = turnoversOnOccMin3.stream().min(Comparator.comparing(Turnover::getDate))
                .map(Turnover::getDate).get();
        assertThat(minDateOccMin3).isEqualTo(new LocalDate(2019, 9,1));

    }

    @Test
    public void xxx() throws Exception {

        // given
        setFixtureClockDate(new LocalDate(2020,2,16));
        setupScenario_and_validate_import_minute();

        // when
        mixin(Lease_aggregateTurnovers.class, oxfMin2Lease).$$(new LocalDate(2010,1,1), new LocalDate(2020,2,1), false);
        transactionService.nextTransaction();

        // then
        final TurnoverAggregation agg20190801Min2 = turnoverAggregationRepository.findUnique(
                occ1Cfg_min2, new LocalDate(2019,8,1));

        final TurnoverAggregation agg20190801Min3 = turnoverAggregationRepository.findUnique(
                occCfg_min3, new LocalDate(2019,8,1));

        assertThat(agg20190801Min2).isNotNull();
        assertThat(agg20190801Min3).isNull();


        final TurnoverAggregation agg20200101 = turnoverAggregationRepository.findUnique(
                occCfg_min3, new LocalDate(2020,1,1));

//        assertThat(agg20200101.getAggregate6Month().getTurnoverCount()).isEqualTo(4); //TODO: check if current aggregation through agent is OK??

    }




    @Inject TurnoverAggregationRepository turnoverAggregationRepository;

    @Inject TurnoverRepository turnoverRepository;

    @Inject ServiceRegistry2 serviceRegistry2;

    @Inject TurnoverReportingConfigRepository turnoverReportingConfigRepository;
}