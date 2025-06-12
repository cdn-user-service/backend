/**
 *
 *
 * https://www.cdn.com
 *
 * 版权所有，侵权必究！
 */

package io.ants;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.logging.Logger;

@SpringBootApplication
public class CdnSysApplication {
	private static final Logger logger = Logger.getLogger(CdnSysApplication.class.getName());

	public static void main(String[] args) {

		java.util.logging.LogManager.getLogManager().reset();
		// Install SLF4JBridgeHandler
		SLF4JBridgeHandler.install();
		// Log a test message
		logger.info("Application starting...");


		SpringApplication.run(CdnSysApplication.class, args);


	}

}