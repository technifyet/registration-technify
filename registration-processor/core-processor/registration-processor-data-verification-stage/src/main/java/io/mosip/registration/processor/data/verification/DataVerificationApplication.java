package io.mosip.registration.processor.data.verification;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.processor.core.constant.LoggerFileConstant;
import io.mosip.registration.processor.core.logger.RegProcessorLogger;
import io.mosip.registration.processor.data.verification.stage.DataVerificationStage;
/**
 * Data VerificationMain class .
 *
 * @author Yared Taye
 * @since 0.0.1
 */
public class DataVerificationApplication {
	private static Logger regProcLogger = RegProcessorLogger.getLogger(DataVerificationApplication.class);
	/**
	 * Main method to instantiate the spring boot application.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		AnnotationConfigApplicationContext configApplicationContext = new AnnotationConfigApplicationContext();
		configApplicationContext.scan("io.mosip.registration.processor.core.config",
				"io.mosip.registration.processor.data.verification.config",
				"io.mosip.registration.processor.packet.receiver.config",
				"io.mosip.registration.processor.packet.manager.config",
				"io.mosip.kernel.packetmanager.config",
				"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.registration.processor.packet.storage.config",
				"io.mosip.registration.processor.data.verification.validators");
		configApplicationContext.refresh();
		DataVerificationStage dataVerificationStage = configApplicationContext
				.getBean(DataVerificationStage.class);
		dataVerificationStage.deployStage();

	}
}