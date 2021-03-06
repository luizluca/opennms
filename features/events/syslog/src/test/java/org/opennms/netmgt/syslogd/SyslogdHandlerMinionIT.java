/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.syslogd;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Component;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/META-INF/opennms/emptyContext.xml" })
public class SyslogdHandlerMinionIT extends CamelBlueprintTestSupport {

	private static final Logger LOG = LoggerFactory.getLogger(SyslogdHandlerMinionIT.class);

	private static BrokerService m_broker = null;

	/**
	 * Use Aries Blueprint synchronous mode to avoid a blueprint deadlock bug.
	 * 
	 * @see https://issues.apache.org/jira/browse/ARIES-1051
	 * @see https://access.redhat.com/site/solutions/640943
	 */
	@Override
	public void doPreSetup() throws Exception {
		System.setProperty("org.apache.aries.blueprint.synchronous", Boolean.TRUE.toString());
		System.setProperty("de.kalpatec.pojosr.framework.events.sync", Boolean.TRUE.toString());
	}

	@Override
	public boolean isUseAdviceWith() {
		return true;
	}

	@Override
	public boolean isUseDebugger() {
		// must enable debugger
		return true;
	}

	@Override
	public String isMockEndpoints() {
		return "*";
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
		// Register any mock OSGi services here
		ActiveMQComponent queueingservice = new ActiveMQComponent();
		queueingservice.setBrokerURL("tcp://127.0.0.1:61716");
		Properties props = new Properties();
		props.put("alias", "opennms.broker");
		services.put(Component.class.getName(), new KeyValueHolder<Object, Dictionary>(queueingservice, props));
	}

	// The location of our Blueprint XML files to be used for testing
	@Override
	protected String getBlueprintDescriptor() {
		return "file:blueprint-syslog-handler-minion.xml";
	}

	@BeforeClass
	public static void startActiveMQ() throws Exception {
		m_broker = new BrokerService();
		m_broker.addConnector("tcp://127.0.0.1:61716");
		m_broker.start();
	}

	@AfterClass
	public static void stopActiveMQ() throws Exception {
		if (m_broker != null) {
			m_broker.stop();
		}
	}

	@Test(timeout=60000)
	public void testSyslogd() throws Exception {
		// Expect one SyslogConnection message to be broadcast on the messaging channel
		MockEndpoint broadcastSyslog = getMockEndpoint("mock:queuingservice:broadcastSyslog", false);

		// Create a mock SyslogdConfig
		SyslogConfigBean config = new SyslogConfigBean();
		config.setSyslogPort(10514);
		config.setNewSuspectOnMessage(false);
		config.setParser("org.opennms.netmgt.syslogd.CustomSyslogParser");
		config.setForwardingRegexp("^.*\\s(19|20)\\d\\d([-/.])(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])(\\s+)(\\S+)(\\s)(\\S.+)");
		config.setMatchingGroupHost(6);
		config.setMatchingGroupMessage(8);
		config.setDiscardUei("DISCARD-MATCHING-MESSAGES");

		int numberOfMessages = 20;
		SyslogConnection[] conns = new SyslogConnection[numberOfMessages];
		for (int i = 0; i < numberOfMessages; i++) {
			conns[i] = new SyslogConnection(InetAddressUtils.ONE_TWENTY_SEVEN, 2000, ByteBuffer.wrap("<34>main: 2010-08-19 localhost foo0: load test 0 on tty1\0".getBytes("US-ASCII")), config);
		}

		broadcastSyslog.setExpectedMessageCount(numberOfMessages);

		// Warm up (open JMS connections, etc)
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < numberOfMessages; i++) {
			template.asyncSendBody("seda:handleMessage", conns[i]);
		}
		long endTime = System.currentTimeMillis();
		LOG.info("Warm-up messages took {}ms", endTime - startTime);
		assertMockEndpointsSatisfied();
		resetMocks();


		numberOfMessages = 5000;
		conns = new SyslogConnection[numberOfMessages];
		for (int i = 0; i < numberOfMessages; i++) {
			conns[i] = new SyslogConnection(InetAddressUtils.ONE_TWENTY_SEVEN, 2000, ByteBuffer.wrap("<34>main: 2010-08-19 localhost foo0: load test 0 on tty1\0".getBytes("US-ASCII")), config);
		}

		broadcastSyslog.setExpectedMessageCount(numberOfMessages);

		// Send a SyslogConnection to seda:handleMessage
		startTime = System.currentTimeMillis();
		for (int i = 0; i < numberOfMessages; i++) {
			template.asyncSendBody("seda:handleMessage", conns[i]);
		}
		endTime = System.currentTimeMillis();
		LOG.info("Test messages took {}ms", endTime - startTime);

		assertMockEndpointsSatisfied();

		endTime = System.currentTimeMillis();
		LOG.info(
			"Message transmission took {}ms ({} events per second)", 
			endTime - startTime, 
			Math.round((double)numberOfMessages / ((double)(endTime - startTime) / 1000.0))
		);
	}
}
