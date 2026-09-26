package com.hyeja.domain.notification.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyeja.domain.notification.service.NotificationGenerationService;
import com.hyeja.global.config.SchedulingConfig;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class NotificationSchedulerTest {

    @Test
    void runsGenerationServiceWithKoreaDate() {
        LocalDate baseDate = LocalDate.of(2026, 9, 26);
        ZoneId koreaZone = ZoneId.of(SchedulingConfig.KOREA_TIME_ZONE);
        Clock clock = Clock.fixed(
                ZonedDateTime.of(baseDate, LocalTime.of(0, 5), koreaZone).toInstant(),
                koreaZone
        );
        NotificationGenerationService service = mock(NotificationGenerationService.class);
        when(service.createDeadlineNotifications(baseDate)).thenReturn(2);
        NotificationScheduler scheduler = new NotificationScheduler(service, clock);

        scheduler.createDeadlineNotifications();

        verify(service).createDeadlineNotifications(baseDate);
    }

    @Test
    void isScheduledEveryDayAtFiveMinutesAfterMidnightInKorea() throws Exception {
        Method method = NotificationScheduler.class
                .getDeclaredMethod("createDeadlineNotifications");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).isEqualTo("0 5 0 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }
}
