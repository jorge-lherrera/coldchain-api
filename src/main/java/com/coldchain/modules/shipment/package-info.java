/**
 * What moves, who holds it and what the custody log says about it. The module the whole product
 * exists to serve.
 */
@ApplicationModule(id = "shipment", allowedDependencies = {"catalog::api"})
package com.coldchain.modules.shipment;

import org.springframework.modulith.ApplicationModule;
