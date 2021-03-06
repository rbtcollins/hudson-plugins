package hudson.plugins.scm_sync_configuration.scms;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.DescriptorImpl.Credential;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.ScmProviderRepositoryWithHost;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.util.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSLAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public enum SCM {
	
	SUBVERSION("Subversion", "svn/config.jelly", "hudson.scm.SubversionSCM", "/hudson/plugins/scm_sync_configuration/ScmSyncConfigurationPlugin/scms/svn/url-help.jelly"){
		private static final String SCM_URL_PREFIX="scm:svn:";
		public String createScmUrlFromRequest(StaplerRequest req) {
			String repoURL = req.getParameter("repositoryUrl");
			if(repoURL == null){ return null; }
			else { return SCM_URL_PREFIX+repoURL; }
		}
		public String extractScmUrlFrom(String scmUrl) {
			return scmUrl.substring(SCM_URL_PREFIX.length());
		}
		protected SCMCredentialConfiguration extractScmCredentials(String scmUrl) {
			String realm = retrieveRealmFor(scmUrl);
			if(realm != null){
				try {
					SubversionSCM.DescriptorImpl subversionDescriptor = (SubversionSCM.DescriptorImpl)getSCMDescriptor();
					Field credentialField = SubversionSCM.DescriptorImpl.class.getDeclaredField("credentials");
					credentialField.setAccessible(true);
					Map<String,Credential> credentials = (Map<String,Credential>)credentialField.get(subversionDescriptor);
					Credential cred = credentials.get(realm);
					// FIXME : retrieve kind & realm somehow !
					String kind = "";
					return createSCMCredentialConfiguration(cred.createSVNAuthentication(kind));
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SVNException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}
		
		private String retrieveRealmFor(String scmURL){
			final String[] realms = new String[]{ null };
			
            SVNRepository repository;
			try {
				repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(scmURL));
	            repository.setTunnelProvider(SVNWCUtil.createDefaultOptions(true));
	            repository.setAuthenticationManager(new DefaultSVNAuthenticationManager(SVNWCUtil.getDefaultConfigurationDirectory(), true, "", "", null, "") {
                    @Override
                    public SVNAuthentication getFirstAuthentication(String kind, String realm, SVNURL url) throws SVNException {
                    	realms[0] = realm;
                    	return super.getFirstAuthentication(kind, realm, url);
                    }
                    @Override
                    public SVNAuthentication getNextAuthentication(String kind, String realm, SVNURL url) throws SVNException {
                    	realms[0] = realm;
                    	return super.getNextAuthentication(kind, realm, url);
                    }

                    @Override
                    public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication) throws SVNException {
                    	realms[0] = realm;
                    	super.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
                    }
	            });
	            repository.testConnection();
	            
			} catch (SVNException e) {
				// If a problem happens, don't do anything, it implies realm doesn't exist in current cache
			}

			return realms[0];
		}
		
		/**
		 * Ugly method to convert a SVN authentication into a SCMCredentialConfiguration
		 */
		public SCMCredentialConfiguration createSCMCredentialConfiguration(SVNAuthentication auth){
			if(auth instanceof SVNPasswordAuthentication){
				SVNPasswordAuthentication passAuth = (SVNPasswordAuthentication)auth;
				return new SCMCredentialConfiguration(passAuth.getUserName(), passAuth.getPassword());
			} else if(auth instanceof SVNSSHAuthentication){
				SVNSSHAuthentication sshAuth = (SVNSSHAuthentication)auth;
				return new SCMCredentialConfiguration(sshAuth.getUserName(), sshAuth.getPassword(), sshAuth.getPassphrase(), sshAuth.getPrivateKey());
			} else if(auth instanceof SVNSSLAuthentication){
				SVNSSLAuthentication sslAuth = (SVNSSLAuthentication)auth;
				return new SCMCredentialConfiguration(sslAuth.getUserName(), sslAuth.getPassword());
			} else if(auth instanceof SVNUserNameAuthentication){
				SVNUserNameAuthentication unameAuth = (SVNUserNameAuthentication)auth;
				return new SCMCredentialConfiguration(unameAuth.getUserName());
			}
			return null;
		}
	};

	private String title;
	private String scmClassName;
	private String configPage;
	private String repositoryUrlHelpPath;
	
	private SCM(String _title, String _configPage, String _scmClassName, String _repositoryUrlHelpPath){
		this.title = _title;
		this.configPage = _configPage;
		this.scmClassName = _scmClassName;
		this.repositoryUrlHelpPath = _repositoryUrlHelpPath;
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public String getConfigPage(){
		return this.configPage;
	}

	public String getSCMClassName() {
		return this.scmClassName;
	}
	
	public Descriptor<? extends hudson.scm.SCM> getSCMDescriptor(){
		return Hudson.getInstance().getDescriptorByName(getSCMClassName());
	}
	
	public String getRepositoryUrlHelpPath() {
		return this.repositoryUrlHelpPath;
	}

	public ScmRepository getConfiguredRepository(ScmManager scmManager, String scmRepositoryURL) 
				throws ScmRepositoryException, NoSuchScmProviderException {
		SCMCredentialConfiguration credentials = extractScmCredentials( extractScmUrlFrom(scmRepositoryURL) );
		if(credentials == null){
			return null;
		}

        ScmRepository repository = scmManager.makeScmRepository( scmRepositoryURL );

        ScmProviderRepository scmRepo = repository.getProviderRepository();

        // TODO: uncomment this ??? (MRELEASE-76)
        //scmRepo.setPersistCheckout( false );

        if ( repository.getProviderRepository() instanceof ScmProviderRepositoryWithHost )
        {
            ScmProviderRepositoryWithHost repositoryWithHost =
                (ScmProviderRepositoryWithHost) repository.getProviderRepository();
            String host = repositoryWithHost.getHost();

            int port = repositoryWithHost.getPort();

            if ( port > 0 )
            {
                host += ":" + port;
            }
        }

        if(credentials != null){
	        if ( !StringUtils.isEmpty( credentials.getUsername() ) )
	        {
	            scmRepo.setUser( credentials.getUsername() );
	        }
	        if ( !StringUtils.isEmpty( credentials.getPassword() ) )
	        {
	            scmRepo.setPassword( credentials.getPassword() );
	        }
	
	        if ( scmRepo instanceof ScmProviderRepositoryWithHost )
	        {
	            ScmProviderRepositoryWithHost repositoryWithHost = (ScmProviderRepositoryWithHost) scmRepo;
	            if ( !StringUtils.isEmpty( credentials.getPrivateKey() ) )
	            {
	                repositoryWithHost.setPrivateKey( credentials.getPrivateKey() );
	            }
	
	            if ( !StringUtils.isEmpty( credentials.getPassphrase() ) )
	            {
	                repositoryWithHost.setPassphrase( credentials.getPassphrase() );
	            }
	        }
        }
        
        return repository;
	}
	
	public abstract String createScmUrlFromRequest(StaplerRequest req);
	public abstract String extractScmUrlFrom(String scmUrl);
	protected abstract SCMCredentialConfiguration extractScmCredentials(String scmRepositoryURL);
}
