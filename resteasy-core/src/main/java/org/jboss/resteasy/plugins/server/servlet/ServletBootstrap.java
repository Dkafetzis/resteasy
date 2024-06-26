package org.jboss.resteasy.plugins.server.servlet;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.core.Application;

import org.jboss.resteasy.core.ApplicationDescription;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletBootstrap extends ListenerBootstrap {
    private final ServletConfig config;

    public ServletBootstrap(final ServletConfig config) {
        super(config.getServletContext());
        this.config = config;
    }

    @Override
    public ResteasyDeployment createDeployment() {
        ResteasyDeployment deployment = super.createDeployment();
        deployment.getDefaultContextObjects().put(ServletConfig.class, config);
        deployment.getDefaultContextObjects().put(ServletContext.class, config.getServletContext());
        // If the application has not been defined, then we will check the servlet name. If the servlet name is
        // jakarta.ws.rs.Application (per the spec) or jakarta.ws.rs.core.Application, we need to use the context
        // parameter name.
        String application = getParameter(Application.class.getName());
        if (application == null) {
            application = getParameter("jakarta.ws.rs.Application");
        }
        if (application == null && Application.class.getName().equals(config.getServletName())) {
            String servletMappingPrefix = getParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX);
            if (servletMappingPrefix == null) {
                servletMappingPrefix = "";
            }
            final ApplicationDescription description = ApplicationDescription.Builder.of(new Application())
                    .path(servletMappingPrefix.trim())
                    .build();
            deployment.getDefaultContextObjects().put(ApplicationDescription.class, description);
        }
        return deployment;
    }

    @Override
    public String getInitParameter(String name) {
        String value = config.getInitParameter(name);
        if (value == null) {
            value = super.getInitParameter(name);
        }
        return value;
    }

    @Override
    public Set<String> getParameterNames() {
        Set<String> set = super.getServletContextNames();
        Enumeration<String> en = config.getInitParameterNames();
        while (en.hasMoreElements())
            set.add(en.nextElement());
        return set;
    }

    @Override
    public String getParameter(final String name) {
        // First check the servlet context, not found delegate the super method
        String value = config.getServletContext().getInitParameter(name);
        if (value == null) {
            value = super.getParameter(name);
        }
        return value;
    }

    @Override
    public Set<String> getInitParameterNames() {
        Set<String> set = new HashSet<String>();
        Enumeration<String> en = config.getInitParameterNames();
        while (en.hasMoreElements())
            set.add(en.nextElement());
        return set;
    }

}
