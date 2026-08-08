package com.cmsBackend.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;

@SpringBootApplication
public class WsApplication {

	public static void main(String[] args) {
		var application = new SpringApplication(WsApplication.class);
		var environment = new StandardEnvironment();
		boolean devtoolsPresent = ClassUtils.isPresent(
				"org.springframework.boot.devtools.restart.RestartLauncher", WsApplication.class.getClassLoader());
		if (shouldActivateLocalProfile(args, devtoolsPresent, environment.getProperty("spring.profiles.active"))) {
			application.setAdditionalProfiles("local");
		}
		application.run(args);
	}

	static boolean shouldActivateLocalProfile(String[] args, boolean devtoolsPresent, String configuredProfiles) {
		if (!devtoolsPresent || configuredProfiles != null && !configuredProfiles.isBlank()) return false;
		for (String argument : args) {
			if (argument.startsWith("--spring.profiles.active=")) return false;
		}
		return true;
	}

}
