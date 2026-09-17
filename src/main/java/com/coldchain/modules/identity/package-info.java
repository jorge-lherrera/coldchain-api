/**
 * Organizations, users, credentials and the audit trail. The module every other one authenticates against.
 */
@ApplicationModule(id = "identity", allowedDependencies = {})
package com.coldchain.modules.identity;

import org.springframework.modulith.ApplicationModule;
