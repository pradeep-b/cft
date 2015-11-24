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
package org.eclipse.cft.server.standalone.ui.internal.startcommand;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.standalone.ui.internal.Messages;
import org.eclipse.cft.server.standalone.ui.internal.SelectMainTypeWizard;
import org.eclipse.cft.server.ui.internal.CloudUiUtil;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * Helper methods for UI components that require Java type searching, given a
 * valid java project.
 */
public class JavaTypeResolver {

	private final IJavaProject project;

	private String serverID;

	public JavaTypeResolver(IJavaProject project, String serverID) {
		this.project = project;
		this.serverID = serverID;
	}

	protected IJavaProject getJavaProject() {
		return project;
	}

	public IType[] getMainTypes(IProgressMonitor monitor) {
		IJavaProject javaProject = getJavaProject();

		if (javaProject != null) {
			// Returns main method types
			boolean includeSubtypes = true;
			MainMethodSearchEngine engine = new MainMethodSearchEngine();
			int constraints = IJavaSearchScope.SOURCES;
			constraints |= IJavaSearchScope.APPLICATION_LIBRARIES;
			IJavaSearchScope scope = SearchEngine.createJavaSearchScope(
					new IJavaElement[] { javaProject }, constraints);
			return engine.searchMainMethods(monitor, scope, includeSubtypes);
		}
		return new IType[] {};

	}

	public IType getMainTypesFromSource(IProgressMonitor monitor) {
		if (project != null) {
			IType[] types = getMainTypes(monitor);
			// Enable when dependency to
			// org.springsource.ide.eclipse.commons.core is
			// added. This should be the common way to obtain main types
			// MainTypeFinder.guessMainTypes(project, monitor);

			if (types != null && types.length > 0) {

				final List<IType> typesFromSource = new ArrayList<IType>();

				for (IType type : types) {
					if (!type.isBinary() && !typesFromSource.contains(type)) {
						typesFromSource.add(type);
					}
				}

				if (typesFromSource.size() == 1) {
					return typesFromSource.get(0);
				} else {
					// Prompt user to select a main type

					final IType[] selectedType = new IType[1];
					Display.getDefault().syncExec(new Runnable() {

						@Override
						public void run() {

							final Shell shell = CloudUiUtil.getShell();

							if (shell != null && !shell.isDisposed()) {
								SelectMainTypeWizard wizard = new SelectMainTypeWizard(
										serverID, typesFromSource);
								WizardDialog dialog = new WizardDialog(shell,
										wizard);
								if (dialog.open() == Window.OK) {
									selectedType[0] = wizard
											.getSelectedMainType();
								}

							} else {
							
								CloudFoundryPlugin
										.getCallback()
										.displayAndLogError(
												CloudFoundryPlugin.getErrorStatus(NLS
														.bind(Messages.SelectMainTypeWizardPage_NO_SHELL,
																project.getProject()
																		.getName())));
								selectedType[0] = typesFromSource.get(0);
							}
						}
					});
					return selectedType[0];
				}
			}
		}
		return null;
	}
}
