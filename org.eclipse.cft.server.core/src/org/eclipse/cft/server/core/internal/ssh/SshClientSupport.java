/*******************************************************************************
 * Copied from Spring Tool Suite. Original license:
 * 
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.cft.server.core.internal.ssh;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.eclipse.cft.server.core.internal.client.AuthorizationHeaderProvider;
import org.eclipse.cft.server.core.internal.client.RestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

/**
 * @author Kris De Volder
 */
public class SshClientSupport {

	private AuthorizationHeaderProvider oauth;
	private RestTemplate restTemplate;
	private CloudInfoV2 cloudInfo;
	private String authorizationUrl;
	private String sshClientId;

	public SshClientSupport(AuthorizationHeaderProvider oauth, CloudInfoV2 cloudInfo, boolean trustSelfSigned, HttpProxyConfiguration httpProxyConfiguration) {
		this.cloudInfo = cloudInfo;
		this.oauth = oauth;
		
		this.restTemplate = RestUtils.createRestTemplate(httpProxyConfiguration, trustSelfSigned, true);
		ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
		restTemplate.setRequestFactory(authorize(requestFactory));
		
		this.authorizationUrl = cloudInfo.getAuthorizationUrl();
		this.sshClientId = cloudInfo.getSshClientId();
	}

	private ClientHttpRequestFactory authorize(final ClientHttpRequestFactory delegate) {
		return new ClientHttpRequestFactory() {

			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				ClientHttpRequest request = delegate.createRequest(uri, httpMethod);
				request.getHeaders().add("Authorization", oauth.getAuthorizationHeader()); //$NON-NLS-1$
				return request;
			}
		};
	}

	public String getSshCode() {
		try {
			URIBuilder builder = new URIBuilder(authorizationUrl + "/oauth/authorize"); //$NON-NLS-1$
			
			builder.addParameter("response_type" //$NON-NLS-1$
					, "code"); //$NON-NLS-1$
			builder.addParameter("grant_type", //$NON-NLS-1$
					"authorization_code"); //$NON-NLS-1$
			builder.addParameter("client_id", sshClientId); //$NON-NLS-1$
			
			URI url = new URI(builder.toString());
			
			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			HttpStatus statusCode = response.getStatusCode();
			if (statusCode!=HttpStatus.FOUND) {
				throw new CloudFoundryException(statusCode);
			}
			
			String loc = response.getHeaders().getFirst("Location"); //$NON-NLS-1$
			if (loc==null) {
				throw new CloudOperationException("No 'Location' header in redirect response"); //$NON-NLS-1$
			}
			List<NameValuePair> qparams = URLEncodedUtils.parse(new URI(loc), "utf8"); //$NON-NLS-1$
			for (NameValuePair pair : qparams) {
				String name = pair.getName();
				if (name.equals("code")) { //$NON-NLS-1$
					return pair.getValue();
				}
			}
			throw new CloudOperationException("No 'code' param in redirect Location: "+loc); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			throw new CloudOperationException(e);
		}
	}

	public SshHost getSshHost() {
		return cloudInfo.getSshHost();
	}

	public static SshClientSupport create(final CloudFoundryOperations client, CloudCredentials creds, HttpProxyConfiguration proxyConf, boolean selfSigned) {
		AuthorizationHeaderProvider oauth = new AuthorizationHeaderProvider() {
			public String getAuthorizationHeader() {
				OAuth2AccessToken token = client.login();
				return token.getTokenType()+" "+token.getValue();
			}
		};
		
		CloudInfoV2 cloudInfo = new CloudInfoV2(
				creds,
				client.getCloudControllerUrl(),
				proxyConf,
				selfSigned
		);

		return new SshClientSupport(oauth, cloudInfo, selfSigned, proxyConf);
	}

}
