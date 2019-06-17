package io.jenkins.plugins.aipconsole.extensions;

import com.castsoftware.uc.aip.console.tools.core.services.RestApiServiceImpl;
import hudson.Extension;
import hudson.ExtensionPoint;
import lombok.extern.java.Log;

/**
 * This class is a simple wrapper for the existing RestApiServiceImpl in the tools core
 */
@Extension
@Log
public class RestApiServiceExtension extends RestApiServiceImpl implements ExtensionPoint {

}
