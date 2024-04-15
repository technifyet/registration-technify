package io.mosip.registration.processor.data.verification.config;

import io.mosip.kernel.dataaccess.hibernate.config.HibernateDaoConfig;
import io.mosip.kernel.dataaccess.hibernate.repository.impl.HibernateRepositoryImpl;
import io.mosip.registration.processor.data.verification.entity.DataVerificationEntity;
import io.mosip.registration.processor.data.verification.repository.DataVerificationRepository;
import io.mosip.registration.processor.data.verification.service.DataVerificationService;
import io.mosip.registration.processor.data.verification.service.impl.DataVerificationServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.mosip.registration.processor.data.verification.stage.DataVerificationStage;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Import({ HibernateDaoConfig.class })
@EnableJpaRepositories(basePackages = "io.mosip.registration.processor", repositoryBaseClass = HibernateRepositoryImpl.class)
public class DataVerificationConfigBean {

	@Bean
	public DataVerificationStage getDataVerificationStage() {
		return new DataVerificationStage();
	}
	@Bean
	DataVerificationService getDataVerificationService() {

		return new DataVerificationServiceImpl();
	}
	@Bean
	DataVerificationEntity getDataVerificationEntity(){

		return new DataVerificationEntity();
	}
}