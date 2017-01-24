/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.actions.throttler;

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.watcher.actions.ActionStatus;
import org.elasticsearch.xpack.watcher.execution.WatchExecutionContext;
import org.elasticsearch.xpack.watcher.watch.WatchStatus;
import org.joda.time.DateTime;
import org.joda.time.PeriodType;

import java.time.Clock;

import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.EMPTY_PAYLOAD;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.mockExecutionContext;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PeriodThrottlerTests extends ESTestCase {
    public void testBelowPeriodSuccessful() throws Exception {
        PeriodType periodType = randomFrom(PeriodType.millis(), PeriodType.seconds(), PeriodType.minutes());
        TimeValue period = TimeValue.timeValueSeconds(randomIntBetween(2, 5));
        PeriodThrottler throttler = new PeriodThrottler(Clock.systemUTC(), period, periodType);

        WatchExecutionContext ctx = mockExecutionContext("_name", EMPTY_PAYLOAD);
        ActionStatus actionStatus = mock(ActionStatus.class);
        DateTime now = new DateTime(Clock.systemUTC().millis());
        when(actionStatus.lastSuccessfulExecution())
                .thenReturn(ActionStatus.Execution.successful(now.minusSeconds((int) period.seconds() - 1)));
        WatchStatus status = mock(WatchStatus.class);
        when(status.actionStatus("_action")).thenReturn(actionStatus);
        when(ctx.watch().status()).thenReturn(status);

        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        assertThat(result.throttle(), is(true));
        assertThat(result.reason(), notNullValue());
        assertThat(result.reason(), startsWith("throttling interval is set to [" + period.format(periodType) + "]"));
        assertThat(result.type(), is(Throttler.Type.PERIOD));
    }

    public void testAbovePeriod() throws Exception {
        PeriodType periodType = randomFrom(PeriodType.millis(), PeriodType.seconds(), PeriodType.minutes());
        TimeValue period = TimeValue.timeValueSeconds(randomIntBetween(2, 5));
        PeriodThrottler throttler = new PeriodThrottler(Clock.systemUTC(), period, periodType);

        WatchExecutionContext ctx = mockExecutionContext("_name", EMPTY_PAYLOAD);
        ActionStatus actionStatus = mock(ActionStatus.class);
        DateTime now = new DateTime(Clock.systemUTC().millis());
        when(actionStatus.lastSuccessfulExecution())
                .thenReturn(ActionStatus.Execution.successful(now.minusSeconds((int) period.seconds() + 1)));
        WatchStatus status = mock(WatchStatus.class);
        when(status.actionStatus("_action")).thenReturn(actionStatus);
        when(ctx.watch().status()).thenReturn(status);

        Throttler.Result result = throttler.throttle("_action", ctx);
        assertThat(result, notNullValue());
        assertThat(result.throttle(), is(false));
        assertThat(result.reason(), nullValue());
    }
}
