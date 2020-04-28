package com.example.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	public DataSource dataSource;

	public BatchConfiguration(
			JobBuilderFactory jobBuilderFactory, 
			StepBuilderFactory stepBuilderFactory,
			DataSource dataSource) {
		
		this.jobBuilderFactory = jobBuilderFactory;
		this.stepBuilderFactory = stepBuilderFactory;
		this.dataSource = dataSource;
	}

	@Bean
	public FlatFileItemReader<Autobot> reader() {
		FlatFileItemReader<Autobot> reader = new FlatFileItemReader<>();
		reader.setResource(new ClassPathResource("sample-data.csv"));
		reader.setLineMapper(new DefaultLineMapper<Autobot>() {
			{
				setLineTokenizer(new DelimitedLineTokenizer() {
					{
						setNames(new String[] { "name", "car" });
					}
				});
				setFieldSetMapper(new BeanWrapperFieldSetMapper<Autobot>() {
					{
						setTargetType(Autobot.class);
					}
				});
			}
		});
		return reader;
	}

	@Bean
	public AutobotItemProcessor processor() {
		return new AutobotItemProcessor();
	}

	@Bean
	public JdbcBatchItemWriter<Autobot> writer() {
		JdbcBatchItemWriter<Autobot> writer = new JdbcBatchItemWriter<>();
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
		writer.setSql("INSERT INTO autobot (name, car) VALUES (:name, :car)");
		writer.setDataSource(this.dataSource);
		return writer;
	}

	@Bean
	public Job importAutobotJob(JobCompletionNotificationListener listener) {
		return jobBuilderFactory.get("importAutobotJob").incrementer(new RunIdIncrementer()).listener(listener)
				.flow(step1()).end().build();
	}

	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1").<Autobot, Autobot>chunk(10).reader(reader()).processor(processor())
				.writer(writer()).build();
	}

}
