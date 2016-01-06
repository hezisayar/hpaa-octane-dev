package com.hp.octane.plugins.jenkins.actions;

import com.hp.octane.dto.general.AggregatedStatusInfo;
import com.hp.octane.dto.general.CIServerTypes;
import com.hp.octane.dto.general.PluginInfo;
import com.hp.octane.plugins.jenkins.OctanePlugin;
import com.hp.octane.plugins.jenkins.configuration.ConfigApi;
import com.hp.octane.plugins.jenkins.model.api.ParameterConfig;
import com.hp.octane.plugins.jenkins.model.processors.parameters.ParameterProcessors;
import com.hp.octane.plugins.jenkins.rest.ProjectsRESTResource;
import com.hp.octane.serialization.SerializationService;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.export.Flavor;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 8/10/14
 * Time: 12:47 PM
 * To change this template use File | Settings | File Templates.
 */

@Extension
public class PluginActions implements RootAction {
	private static final Logger logger = Logger.getLogger(PluginActions.class.getName());

	//  [YG] probably move the instance ID related things to Plugin Info, since it's not belongs to the Jenkins core
	@ExportedBean
	public static final class ServerInfo {
		private static final String type = "jenkins";
		private static final String version = Jenkins.VERSION;
		private String url;
		private String instanceId = Jenkins.getInstance().getPlugin(OctanePlugin.class).getIdentity();
		private Long instanceIdFrom = Jenkins.getInstance().getPlugin(OctanePlugin.class).getIdentityFrom();
		private Long sendingTime;

		public ServerInfo() {
			String serverUrl = Jenkins.getInstance().getRootUrl();
			if (serverUrl != null && serverUrl.endsWith("/"))
				serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
			this.url = serverUrl;
			this.sendingTime = System.currentTimeMillis();
		}

		@Exported(inline = true)
		public String getType() {
			return type;
		}

		@Exported(inline = true)
		public String getVersion() {
			return version;
		}

		@Exported(inline = true)
		public String getUrl() {
			return url;
		}

		@Exported(inline = true)
		public String getInstanceId() {
			return instanceId;
		}

		@Exported(inline = true)
		public Long getInstanceIdFrom() {
			return instanceIdFrom;
		}

		@Exported(inline = true)
		public Long getSendingTime() {
			return sendingTime;
		}
	}

	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return null;
	}

	public String getUrlName() {
		return "octane";
	}

	public void doStatus(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
		AggregatedStatusInfo statusInfo = new AggregatedStatusInfo();
		statusInfo.setPlugin(new PluginInfo(Jenkins.getInstance().getPlugin(OctanePlugin.class).getWrapper().getVersion()));
		statusInfo.setServer(new com.hp.octane.dto.general.ServerInfo(
				CIServerTypes.JENKINS,
				Jenkins.VERSION,
				getNormalizedSelfURL(),
				Jenkins.getInstance().getPlugin(OctanePlugin.class).getIdentity(),
				Jenkins.getInstance().getPlugin(OctanePlugin.class).getIdentityFrom()
		));
		res.getWriter().write(SerializationService.toJSON(statusInfo));
	}

	public void doProjects(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
		if (req.getRestOfPath().isEmpty() && req.getMethod().equals("GET")) {
			boolean areParametersNeeded = true;
			if (req.getParameter("parameters") != null && req.getParameter("parameters").toLowerCase().equals("false")) {
				areParametersNeeded = false;
			}
			res.serveExposedBean(req, new ProjectsList(areParametersNeeded), Flavor.JSON);
		} else {
			ProjectsRESTResource.instance.handle(req, res);
		}
	}

	//  TODO: refactor to adjust to standard REST APIs flavor
	public ConfigApi getConfiguration() {
		return new ConfigApi();
	}

	@Deprecated
	public void doJobs(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
		boolean areParametersNeeded = true;
		if (req.getParameter("parameters") != null && req.getParameter("parameters").toLowerCase().equals("false")) {
			areParametersNeeded = false;
		}
		res.serveExposedBean(req, new ProjectsList(areParametersNeeded), Flavor.JSON);
	}

	@ExportedBean
	public static final class ProjectConfig {
		private String name;
		private ParameterConfig[] parameters;

		public void setName(String value) {
			name = value;
		}

		@Exported(inline = true)
		public String getName() {
			return name;
		}

		public void setParameters(ParameterConfig[] parameters) {
			this.parameters = parameters;
		}

		@Exported(inline = true)
		public ParameterConfig[] getParameters() {
			return parameters;
		}
	}

	@ExportedBean
	public static final class ProjectsList {
		@Exported(inline = true)
		public ProjectConfig[] jobs;

		public ProjectsList(boolean areParametersNeeded) {
			ProjectConfig tmpConfig;
			AbstractProject tmpProject;
			List<ProjectConfig> list = new ArrayList<ProjectConfig>();
			List<String> itemNames = (List<String>) Jenkins.getInstance().getTopLevelItemNames();
			for (String name : itemNames) {
				tmpProject = (AbstractProject) Jenkins.getInstance().getItem(name);
				tmpConfig = new ProjectConfig();
				tmpConfig.setName(name);
				if (areParametersNeeded) {
					tmpConfig.setParameters(ParameterProcessors.getConfigs(tmpProject));
				}
				list.add(tmpConfig);
			}
			jobs = list.toArray(new ProjectConfig[list.size()]);
		}
	}

	private String getNormalizedSelfURL() {
		String selfUrl = Jenkins.getInstance().getRootUrl();
		if (selfUrl != null && selfUrl.endsWith("/"))
			selfUrl = selfUrl.substring(0, selfUrl.length() - 1);
		return selfUrl;
	}
}
