package mapping;

import mapping.classify.MappingResult;

import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MappingProperties.class)
public class MappingApplication implements CommandLineRunner {
    private final MappingJob job;

    public MappingApplication(MappingJob job) {
        this.job = job;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(MappingApplication.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        MappingResult result = job.execute();
        System.out.print(result.summary().text());
        System.out.println("Wrote " + result.rows().size() + " rows to " + job.outputPath());
        System.out.println("Costliest holdout disagreements:");
        int shown = 0;
        for (MappingResult.ReportRow row : result.report()) {
            if (row.disagreedRevenue() <= 0) {
                break;
            }
            System.out.printf(Locale.US, "  %.2f  %s -> %s  %s%n",
                    row.disagreedRevenue(), row.normalizedCategory(), row.ourCategoryId(), row.weakReason());
            if (++shown == 12) {
                break;
            }
        }
    }
}
