package org.ecommerce.auth.service.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.ecommerce.auth.service.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class ExpiredRefreshTokenCleanupJob {

    private final TokenService tokenService;
    @Value("${expired.batch:100}")
    private int batchSize;

    @Scheduled(cron = "0 0 0 * * Sat,Sun")
    @SchedulerLock(name = "cleanExpiredRefreshTokensJob", lockAtMostFor = "8m",lockAtLeastFor = "2m")
    public void cleanExpiredRefreshTokensJob() {
        log.info("Expired Refresh Tokens Cleanup Job Started");
        tokenService.removeRefreshTokenByBatch(batchSize);
        log.info("Expired Refresh Tokens Cleanup Job Finished");
    }


}
