/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * 
 * The Eclipse Public License is available at 
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * and the Apache License v2.0 is available at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * You may elect to redistribute this code under either of these licenses.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.Servlet;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.LaunchableAdapterDelegate;
import org.eclipse.wst.server.core.util.WebResource;



/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryLaunchableAdapterDelegate extends LaunchableAdapterDelegate {

	@Override
	public Object getLaunchable(IServer server, IModuleArtifact moduleArtifact) throws CoreException {
		if (server.getAdapter(CloudFoundryServer.class) == null) {
			return null;
		}
		if (!(moduleArtifact instanceof Servlet) && !(moduleArtifact instanceof WebResource)) {
			return null;
		}
		if (moduleArtifact.getModule().loadAdapter(IWebModule.class, null) == null) {
			return null;
		}
		return new CloudFoundryLaunchable(server, moduleArtifact);
	}

}
