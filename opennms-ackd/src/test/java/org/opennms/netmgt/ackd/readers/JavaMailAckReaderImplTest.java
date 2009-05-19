/*
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a derivative work, containing both original code, included code and modified
 * code that was published under the GNU General Public License. Copyrights for modified
 * and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * Modifications:
 * 
 * Created: January 27, 2009
 *
 * Copyright (C) 2009 The OpenNMS Group, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 *      OpenNMS Licensing       <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 */
package org.opennms.netmgt.ackd.readers;

import static org.junit.Assert.fail;
import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.netmgt.ackd.AckReader;
import org.opennms.netmgt.ackd.Ackd;
import org.opennms.netmgt.dao.JavaMailConfigurationDao;
import org.opennms.netmgt.dao.db.OpenNMSConfigurationExecutionListener;
import org.opennms.netmgt.dao.db.TemporaryDatabaseExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import org.opennms.javamail.JavaMailerException;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
    OpenNMSConfigurationExecutionListener.class,
    TemporaryDatabaseExecutionListener.class,
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class
})
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-daemon.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "classpath:/META-INF/opennms/applicationContext-ackd.xml" })

public class JavaMailAckReaderImplTest {

    @Autowired
    private AckReader m_ackReader;
    
    @Autowired
    private Ackd m_daemon;
    
    @Autowired
    private JavaMailConfigurationDao m_jmDao;

    @Test
    public void verifyWiring() {
        Assert.assertNotNull(m_daemon);
        Assert.assertNotNull(m_jmDao);
        Assert.assertNotNull(m_ackReader);
    }
    
    @Ignore
    public void detectAcks() throws JavaMailerException {
        fail("Not yet implemented");
    }

    @Ignore
    public void findAndProcessAcks() {
        fail("Not yet implemented");
    }

    @Ignore
    public void detectId() {
        fail("Not yet implemented");
    }

    @Ignore
    public void createAcknowledgment() {
        fail("Not yet implemented");
    }

    @Ignore
    public void determineAckAction() {
        fail("Not yet implemented");
    }

    @Ignore
    public void start() {
        fail("Not yet implemented");
    }

    @Ignore
    public void pause() {
        fail("Not yet implemented");
    }

    @Ignore
    public void resume() {
        fail("Not yet implemented");
    }

    @Ignore
    public void stop() {
        fail("Not yet implemented");
    }

}
