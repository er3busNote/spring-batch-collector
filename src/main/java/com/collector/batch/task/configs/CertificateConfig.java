package com.collector.batch.task.configs;

import com.collector.batch.domain.certificate.dto.NationalTechDto;
import com.collector.batch.domain.certificate.dto.Item;
import com.collector.batch.domain.certificate.service.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.validation.BindException;

import java.io.ByteArrayInputStream;
import java.util.List;

import static com.collector.batch.task.configs.CertificateConfig.JOB_NAME;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "job.name", havingValue = JOB_NAME)
public class CertificateConfig {

    public static final String JOB_NAME = "certificateJob";
    public static final String STEP_NAME = "certificateStep";
    private static final Integer CHUNK_SIZE = 10;

    @Autowired
    protected CertificateService certificateService;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    @StepScope
    public ItemReader<Item> nationalTechXmlItemReader() {
        return new ListItemReader(this.certificateService.getNationalTechnical());
    }

    private static class ListItemReader implements ItemReader<Item> {

        private final List<Item> itemList;
        private int index = 0;

        public ListItemReader(List<Item> itemList) {
            this.itemList = itemList;
        }

        @Override
        public Item read() throws Exception {
            if (index < itemList.size()) {
                return itemList.get(index++);
            } else {
                return null;
            }
        }
    }

    @Bean
    @StepScope
    public StaxEventItemReader<Item> nationalTechStaxItemReader() {
        return new StaxEventItemReaderBuilder<Item>()
                .name("nationalTechXmlItemReader")
                .resource(new InputStreamResource(new ByteArrayInputStream("".getBytes())))
                .addFragmentRootElements("response")
                .unmarshaller(jaxb2Marshaller())
                .build();
    }

    private Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(Item.class);
        return marshaller;
    }

    @Bean
    @StepScope
    public FlatFileItemReader<NationalTechDto> nationalTechCsvItemReader() {
        return new FlatFileItemReaderBuilder<NationalTechDto>()
                .name("nationalTechCsvItemReader")
                .encoding("EUC-KR")
                .resource(new ClassPathResource("data/한국산업인력공단_국가자격종목별 주무부처_20230316.csv"))
                .delimited()
                .delimiter(",")
                .names("name", "type", "agency")
                .linesToSkip(1)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<NationalTechDto>() {{
                    setTargetType(NationalTechDto.class);
                }})
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Item, Item> nationalTechXmlItemProcessor() {
        return item -> item;
    }

    @Bean
    @StepScope
    public ItemProcessor<NationalTechDto, NationalTechDto> nationalTechCsvItemProcessor() {
        return item -> item;
    }

    @Bean
    @StepScope
    public ItemWriter<Item> nationalTechXmlItemWriter() {
        return items -> items.forEach(item -> {
            try {
                this.certificateService.saveNationalTechnical(item);
            } catch (Exception e) {
                log.error("DB 에러 : " + e.getMessage());
            }
        });
    }

    @Bean
    @StepScope
    public ItemWriter<NationalTechDto> nationalTechCsvItemWriter() {
        return items -> items.forEach(item -> {
            try {
                this.certificateService.saveNationalTechnical(item);
            } catch (Exception e) {
                log.error("DB 에러 : " + e.getMessage());
            }
        });
    }

    @Bean
    public Job certificateJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(certificateReadyStep(null))
                .next(decider())
                .from(decider()).on("NATIONAL_TECHNICAL_API").to(nationalTechApiStep())
                .from(decider()).on("NATIONAL_TECHNICAL_CSV").to(nationalTechCsvStep())
                .end()
                .build();
    }

    @Bean
    @JobScope
    public Step certificateReadyStep(@Value("#{jobParameters[requestDate]}") String requestDate) {
        return stepBuilderFactory.get("certificateReadyStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("자격증 정보 수집 환경이 세팅되었습니다.");
                    log.info(">>>>> requestDate = {}", requestDate);
                    return RepeatStatus.FINISHED;
                }).build();
    }

    @Bean
    @JobScope
    public Step nationalTechApiStep() {
        return stepBuilderFactory.get(STEP_NAME)
                .<Item, Item>chunk(CHUNK_SIZE)
                .reader(nationalTechXmlItemReader())
                .processor(nationalTechXmlItemProcessor())
                .writer(nationalTechXmlItemWriter())
                .build();
    }

    @Bean
    @JobScope
    public Step nationalTechCsvStep() {
        return stepBuilderFactory.get(STEP_NAME)
                .<NationalTechDto, NationalTechDto>chunk(CHUNK_SIZE)
                .reader(nationalTechCsvItemReader())
                .processor(nationalTechCsvItemProcessor())
                .writer(nationalTechCsvItemWriter())
                .build();
    }

    @Bean
    public JobExecutionDecider decider() {
        return (jobExecution, stepExecution) -> {
            String condition = jobExecution.getJobParameters().getString("code");

            if (condition != null && condition.equals("NT_API")) {
                return new FlowExecutionStatus("NATIONAL_TECHNICAL_API");
            } else if (condition != null && condition.equals("NT_CSV")) {
                return new FlowExecutionStatus("NATIONAL_TECHNICAL_CSV");
            } else {
                throw new IllegalArgumentException("파라미터가 잘못 되었습니다.");
            }
        };
    }
}
