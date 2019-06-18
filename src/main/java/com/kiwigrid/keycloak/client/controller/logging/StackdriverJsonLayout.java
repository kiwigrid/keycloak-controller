package com.kiwigrid.keycloak.client.controller.logging;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import ch.qos.logback.contrib.json.classic.JsonLayout;

/**
 * Stackdriver layout with <b>serviceName</b> and <b>serviceVersion</b>.
 */
@lombok.Getter
@lombok.Setter
public class StackdriverJsonLayout extends JsonLayout {

	private static final String TIMESTAMP_SECONDS_ATTR_NAME = "timestampSeconds";
	private static final String TIMESTAMP_NANOS_ATTR_NAME = "timestampNanos";
	private static final String SEVERITY_ATTR_NAME = "severity";
	private static final String PROPERTY_IS_UNDEFINED = "_IS_UNDEFINED";

	private boolean includeExceptionInMessage;
	private String serviceName;
	private String serviceVersion;
	private Map<String, String> serviceContext;

	public StackdriverJsonLayout() {
		appendLineSeparator = true;
		includeExceptionInMessage = true;
		includeException = false;
		setJsonFormatter(new JacksonJsonFormatter());
	}

	@Override
	protected Map<String, Object> toJsonMap(ILoggingEvent event) {
		Map<String, Object> map = new LinkedHashMap<>();
		if (includeTimestamp) {
			map.put(TIMESTAMP_SECONDS_ATTR_NAME, TimeUnit.MILLISECONDS.toSeconds(event.getTimeStamp()));
			map.put(TIMESTAMP_NANOS_ATTR_NAME, TimeUnit.MILLISECONDS.toNanos(event.getTimeStamp() % 1_000));
		}
		add(SEVERITY_ATTR_NAME, includeLevel, String.valueOf(event.getLevel()), map);
		add(JsonLayout.THREAD_ATTR_NAME, includeThreadName, event.getThreadName(), map);
		add(JsonLayout.LOGGER_ATTR_NAME, includeLoggerName, event.getLoggerName(), map);
		if (includeFormattedMessage) {
			String message = event.getFormattedMessage();
			if (includeExceptionInMessage) {
				IThrowableProxy throwableProxy = event.getThrowableProxy();
				if (throwableProxy != null) {
					String stackTrace = getThrowableProxyConverter().convert(event);
					if (stackTrace != null && !stackTrace.equals("")) {
						message += "\n" + stackTrace;
					}
				}
			}
			map.put(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, message);
		}
		add(JsonLayout.MESSAGE_ATTR_NAME, includeMessage, event.getMessage(), map);
		add(JsonLayout.CONTEXT_ATTR_NAME, includeContextName, event.getLoggerContextVO().getName(), map);
		addThrowableInfo(JsonLayout.EXCEPTION_ATTR_NAME, includeException, event, map);
		addCustomDataToJsonMap(map, event);

		Map<String, String> serviceContext = getServiceContext();
		if (!serviceContext.isEmpty()) {
			map.put("serviceContext", serviceContext);
		}
		return map;
	}

	private Map<String, String> getServiceContext() {
		if (serviceContext == null) {
			serviceContext = new HashMap<>(2);
			if (serviceName != null && !serviceName.isEmpty() && !serviceName.endsWith(PROPERTY_IS_UNDEFINED)) {
				serviceContext.put("service", serviceName);
			}
			if (serviceVersion != null && !serviceVersion.isEmpty() && !serviceName.endsWith(PROPERTY_IS_UNDEFINED)) {
				serviceContext.put("version", serviceVersion);
			}
		}
		return serviceContext;
	}
}
