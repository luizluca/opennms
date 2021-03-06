/*******************************************************************************
 * This file is part of OpenNMS(R).
 * <p>
 * Copyright (C) 2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * <p>
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 * http://www.gnu.org/licenses/
 * <p>
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.jasper.helper;

import org.opennms.netmgt.measurements.api.ExpressionEngine;
import org.opennms.netmgt.measurements.api.FilterEngine;
import org.opennms.netmgt.measurements.api.MeasurementFetchStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringHelper implements ApplicationContextAware {

    private static Logger LOG = LoggerFactory.getLogger(SpringHelper.class);

    private ApplicationContext applicationContext;

    public MeasurementFetchStrategy getMeasurementFetchStrategy() {
        return getBean("measurementFetchStrategy", MeasurementFetchStrategy.class);
    }

    public ExpressionEngine getExpressionEngine() {
        return getBean("expressionEngine", ExpressionEngine.class);
    }

    public FilterEngine getFilterEngine() {
        return getBean("filterEngine", FilterEngine.class);
    }

    public <T> T getBean(String name, Class<T> clazz) {
        if (applicationContext == null) {
            LOG.error("Could not instantiate bean with name '{}' and type '{}'. ApplicationContext is '{}'", name, clazz, applicationContext);
            return null;
        }
        try {
            return applicationContext.getBean(name, clazz);
        } catch (Exception ex) {
            LOG.error("Could not instantiate bean with name '{}' and type '{}'", name, clazz, ex);
            return null;
        }
    }

    public ApplicationContext getSpringContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
